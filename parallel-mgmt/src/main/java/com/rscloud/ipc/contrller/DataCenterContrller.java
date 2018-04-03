package com.rscloud.ipc.contrller;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class DataCenterContrller extends BaseContrller {
	
	@RequiresPermissions("image:dataCenter:list")
	@RequestMapping(value = "/dataCenter/list")
	public String dataCenterList(Model model){
		return "/dataCenter/dataCenterList";
	}
	
}
