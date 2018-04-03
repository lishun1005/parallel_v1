package com.rscloud.ipc.contrller;

import com.rscloud.ipc.rpc.api.dto.ProductLineDto;
import com.rscloud.ipc.rpc.api.result.ResultBean;
import com.rscloud.ipc.rpc.api.service.OptimalModelService;
import com.rscloud.ipc.rpc.api.service.ProductlineService;
import com.rsclouds.common.utils.StringTool;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * 
* @ClassName: 模型
* @Description: TODO
* @author lishun 
* @date 2017年7月21日 下午2:20:34  
*
 */
@Controller
public class ProductLineController extends BaseContrller{
	@Autowired
	@Lazy
	private ProductlineService productlineService;
	@Autowired
	@Lazy
	private OptimalModelService optimalModelService;


	@RequiresPermissions("production:productline:list")
	@RequestMapping(value = "productline/list", method = RequestMethod.GET)
	public String queryAlgorithmAll(Integer rows, Integer pageNo, String keyword, Model model) {
		if (null == rows) {
			rows = 10;
		}
		if (null == pageNo) {
			pageNo = 1;
		}
		model.addAttribute("pageInfo", productlineService.queryAll(keyword, pageNo, rows));
		model.addAttribute("optimalModelPageInfo", optimalModelService.queryAll(null, 1, 100000));
		return "/production/productLine";
	}
	@RequiresPermissions("production:productline:del")
	@RequestMapping(value = "productline/delete", method = RequestMethod.GET)
	public String delete(ProductLineDto productLineDto, Model model) {
		try {
			if(StringUtils.isNotBlank(productLineDto.getId())){
				productLineDto.setIsDel(1);
				productlineService.update(productLineDto);
				model.addAttribute("msg","删除成功");
			}else {
				model.addAttribute("msg","删除失败");
			}
		}catch (Exception e){
			e.printStackTrace();
			model.addAttribute("msg","删除失败");
		}
		return "redirect:list";
	}

	@RequiresPermissions("production:productline:save")
	@RequestMapping(value = "productline/save", method = RequestMethod.POST)
	public String add(ProductLineDto productLineDto, Model model) {
		try {
			ResultBean<ProductLineDto> resultBean =
					productlineService.findByIdOrName("", productLineDto.getName());
			if(StringUtils.isNotBlank(productLineDto.getId())){
				ResultBean<ProductLineDto> resultBeanById =
						productlineService.findByIdOrName(productLineDto.getId(), "");
				if(resultBeanById.getResultData() != null){
					if(resultBean.getResultData() != null){
						if(resultBean.getResultData().getName().equals(resultBeanById.getResultData().getName())){
							productlineService.update(productLineDto);
							model.addAttribute("msg","编辑成功");
						}else {
							model.addAttribute("msg","添加失败,名称已存在" + productLineDto.getName());
						}
					}else{
						productlineService.update(productLineDto);
						model.addAttribute("msg","编辑成功");
					}
				}else {
					model.addAttribute("msg","编辑失败,记录不存在");
				}
			}else{
				if(resultBean.getResultData() != null){
					model.addAttribute("msg","编辑失败,名称已存在" + productLineDto.getName());
				}else{
					productLineDto.setId(StringTool.getUUID());
					productLineDto.setEffectPicPath("/img/photo.png");
					productlineService.insert(productLineDto);
					model.addAttribute("msg","编辑成功");
				}
			}
		}catch (Exception e){
			e.printStackTrace();
			model.addAttribute("msg","编辑失败");
		}
		return "redirect:list";
	}

}
