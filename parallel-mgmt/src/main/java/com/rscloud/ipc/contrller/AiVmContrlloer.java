package com.rscloud.ipc.contrller;

import com.rscloud.ipc.rpc.api.entity.AiVm;
import com.rscloud.ipc.rpc.api.service.AiVmService;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author lishun
 * @Description: TODO
 * @date 2018/1/30
 */
@Controller
public class AiVmContrlloer {
	@Autowired
	@Lazy
	public AiVmService aiVmService;

	@RequiresPermissions("ai:vm:list")
	@RequestMapping(value = "ai/vm/list", method = RequestMethod.GET)
	public String jobMonitoring(Integer rows, Integer pageNo, String keyword, Model model) {
		if (null == rows) {
			rows = 10;
		}
		if (null == pageNo) {
			pageNo = 1;
		}
		model.addAttribute("pageInfo", aiVmService.queryAll(keyword, pageNo, rows));
		return "/ai/vmList";
	}
	@RequiresPermissions("ai:vm:add")
	@RequestMapping(value = "ai/vm/add", method = RequestMethod.POST)
	public String save(AiVm aiVm, Model model) {
		aiVmService.add(aiVm);
		model.addAttribute("msg", "更新成功");
		return "redirect:/ai/vm/list";
	}
	@RequiresPermissions("ai:vm:update")
	@RequestMapping(value = "ai/vm/update", method = RequestMethod.POST)
	public String update(AiVm aiVm, Model model) {
		if(StringUtils.isBlank(aiVm.getId())){
			model.addAttribute("msg","id is null");
		}else{
			aiVmService.update(aiVm);
			model.addAttribute("msg", "更新成功");
		}
		return "redirect:/ai/vm/list";
	}
	@RequiresPermissions("ai:vm:delete")
	@RequestMapping(value = "ai/vm/delete", method = RequestMethod.GET)
	public String delete(AiVm aiVm, Model model) {
		if (StringUtils.isBlank(aiVm.getId())) {
			model.addAttribute("msg", "id is null");
		} else {
			aiVm.setIsDel((short) 1);
			aiVmService.update(aiVm);
			model.addAttribute("msg", "删除成功");
		}
		return "redirect:/ai/vm/list";
	}
}
