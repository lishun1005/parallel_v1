package com.rscloud.ipc.contrller;

import com.rscloud.ipc.rpc.api.dto.AlgorithmDto;
import com.rscloud.ipc.rpc.api.result.ResultBean;
import com.rscloud.ipc.rpc.api.service.AlgorithmService;
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
public class AlgorithmController extends BaseContrller{
	@Autowired
	@Lazy
	private AlgorithmService algorithmService;

	@RequiresPermissions("production:algorithm:list")
	@RequestMapping(value = "algorithm/list", method = RequestMethod.GET)
	public String queryAlgorithmAll(Integer rows, Integer pageNo, String keyword, Model model) {
		if (null == rows) {
			rows = 10;
		}
		if (null == pageNo) {
			pageNo = 1;
		}
		model.addAttribute("pageInfo", algorithmService.queryAlgorithmAll(keyword, pageNo, rows));
		return "/production/algorithm";
	}

	@RequiresPermissions("production:algorithm:del")
	@RequestMapping(value = "algorithm/delete", method = RequestMethod.GET)
	public String delete(AlgorithmDto algorithmDto, Model model) {
		try {
			if(StringUtils.isNotBlank(algorithmDto.getId())){
				algorithmDto.setIsDel(1);
				algorithmService.update(algorithmDto);
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
	@RequiresPermissions("production:algorithm:save")
	@RequestMapping(value = "algorithm/save", method = RequestMethod.POST)
	public String add(AlgorithmDto algorithmDto, Model model) {
		try {
			ResultBean<AlgorithmDto> resultBean =
					algorithmService.findByIdOrName(null, algorithmDto.getName());
			if(StringUtils.isNotBlank(algorithmDto.getId())){
				ResultBean<AlgorithmDto> resultBeanById =
						algorithmService.findByIdOrName(algorithmDto.getId(),"");
				if(resultBeanById.getResultData() != null){
					if(resultBean.getResultData() != null){
						if(resultBean.getResultData().getName().equals(resultBeanById.getResultData().getName())){
							algorithmService.update(algorithmDto);
							model.addAttribute("msg","编辑成功");
						}else {
							model.addAttribute("msg","添加失败,名称已存在" + algorithmDto.getName());
						}
					}else{
						algorithmService.update(algorithmDto);
						model.addAttribute("msg","编辑成功");
					}
				}else {
					model.addAttribute("msg","编辑失败,记录不存在");
				}
			}else{
				if(resultBean.getResultData() != null){
					model.addAttribute("msg","编辑失败,名称已存在" + algorithmDto.getName());
				}else{
					algorithmDto.setId(StringTool.getUUID());
					algorithmService.insert(algorithmDto);
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
