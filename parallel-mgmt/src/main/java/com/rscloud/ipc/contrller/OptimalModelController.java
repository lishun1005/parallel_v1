package com.rscloud.ipc.contrller;

import com.rscloud.ipc.rpc.api.dto.OptimalModelDto;
import com.rscloud.ipc.rpc.api.result.ResultBean;
import com.rscloud.ipc.rpc.api.result.ResultCode;
import com.rscloud.ipc.rpc.api.service.AlgorithmService;
import com.rscloud.ipc.rpc.api.service.OptimalModelService;
import com.rsclouds.common.utils.StringTool;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
* @ClassName: 模型
* @Description: TODO
* @author lishun 
* @date 2017年7月21日 下午2:20:34  
*
 */
@Controller
public class OptimalModelController extends BaseContrller{
	@Autowired
	@Lazy
	private OptimalModelService optimalModelService;

	@Autowired
	@Lazy
	private AlgorithmService algorithmService;

	@RequiresPermissions("production:optimalModel:list")
	@RequestMapping(value = "optimalModel/list", method = RequestMethod.GET)
	public String queryAlgorithmAll(Integer rows, Integer pageNo, String keyword, Model model) {
		if (null == rows) {
			rows = 10;
		}
		if (null == pageNo) {
			pageNo = 1;
		}
		model.addAttribute("pageInfo", optimalModelService.queryAll(keyword, pageNo, rows));
		model.addAttribute("algorithmPageInfo", algorithmService.queryAlgorithmAll(null, 1, 10000000));

		return "/production/optimalModel";
	}
	@RequiresPermissions("production:optimalModel:del")
	@RequestMapping(value = "optimalModel/delete", method = RequestMethod.GET)
	public String delete(OptimalModelDto optimalModelDto, Model model) {
		try {
			if(StringUtils.isNotBlank(optimalModelDto.getId())){
				optimalModelDto.setIsDel(1);
				optimalModelService.update(optimalModelDto);
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

	@RequiresPermissions("production:optimalModel:save")
	@RequestMapping(value = "optimalModel/save", method = RequestMethod.POST)
	public String add(OptimalModelDto optimalModelDto, Model model) {
		try {
			optimalModelDto.setId(StringTool.getUUID());
			optimalModelService.insert(optimalModelDto);
			model.addAttribute("msg","编辑成功");
		}catch (Exception e){
			e.printStackTrace();
			model.addAttribute("msg","编辑失败");
		}
		return "redirect:list";
	}

	@RequiresPermissions(value ={"production:createTask:list"})
	@ResponseBody
	@RequestMapping(value = "optimalModel/queryOptimalModelByModelId", method = RequestMethod.GET)
	public ResultBean<Map<String,Object>> queryOptimalModelByModelId(String modelId) {
		ResultBean<Map<String,Object>> resultBean = new ResultBean<Map<String,Object>>();
		try {
			resultBean.setCode(ResultCode.OK);
			resultBean.setResultDataList(optimalModelService.queryOptimalModelByModelId(modelId));
		}catch (Exception e){
			e.printStackTrace();
			resultBean.setCode(ResultCode.FAILED);
			resultBean.setMessage(e.getMessage());
		}
		return resultBean;
	}

	@RequiresPermissions("production:optimalModel:save")
	@ResponseBody
	@RequestMapping(value = "optimalModel/saveModel", method = RequestMethod.POST)
	public ResultBean<String> saveModel(String id, String[] algorithmIds) {
		ResultBean<String> resultBean = new ResultBean<String>();
		try {
			if(algorithmIds.length == 0){
				resultBean.setResultBean(ResultCode.FAILED,"必须添加一个算法");
			}else{
				List<Map<String,Object>> list = new ArrayList<Map<String,Object>>();
				for (int i = 0 ; i <algorithmIds.length; i++) {
					Map<String,Object> map = new HashMap<String,Object>();
					map.put("modelId",id);
					map.put("algorithmId",algorithmIds[i]);
					map.put("ordering",i);
					list.add(map);
				}
				optimalModelService.insertBatch(list);
				resultBean.setCode(ResultCode.OK);
			}
		}catch (Exception e){
			e.printStackTrace();
			resultBean.setResultBean(ResultCode.FAILED,e.getMessage());
		}
		return resultBean;
	}
}
