package com.rscloud.ipc.contrller;

import com.rscloud.ipc.rpc.api.entity.AiVmInstancePort;
import com.rscloud.ipc.rpc.api.result.ResultBean;
import com.rscloud.ipc.rpc.api.service.AiVmService;
import com.rsclouds.common.utils.JsonUtil;
import com.rsclouds.common.utils.StringTool;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

/**
 * @author lishun
 * @Description: TODO
 * @date 2018/1/30
 */
@Controller
public class AiJobContrlloer {

	@Autowired
	@Lazy
	public AiVmService aiVmService;

	@RequiresPermissions("ai:inference:add")
	@RequestMapping(value = "/ai/inferenceAdd",method={RequestMethod.POST})
	@ResponseBody
	public ResultBean<String> addJob(String otherParms, String vmListJson){
		ResultBean<String> resultBean = new ResultBean<String>();
		if(StringUtils.isNotBlank(vmListJson)){
			List<Map<String, Object>> vmList = JsonUtil.toListMap(vmListJson);
			for (Map<String, Object> map : vmList){ //add 虚拟机端口使用表记录
				if(map.get("vmId") != null && map.get("vmPort") != null ) {
					String vmId = String.valueOf(map.get("vmId"));
					Integer vmPort = Integer.valueOf(map.get("vmPort").toString());
					AiVmInstancePort aiVmInstancePort = aiVmService.selectAiVmInstancePortByVmIdAndPort(vmId,vmPort);
					if(aiVmInstancePort == null ){
						String id = StringTool.getUUID();
						aiVmInstancePort = new AiVmInstancePort();
						aiVmInstancePort.setId(id);
						aiVmInstancePort.setVmId(vmId);
						aiVmInstancePort.setVmPort(vmPort);
						aiVmInstancePort.setIsUse(new Short("0"));
						aiVmService.aiVmInstancePortAdd(aiVmInstancePort);
						map.put("id",id);
					}else{
						map.put("id",aiVmInstancePort.getId());
					}
				}
			}
			//System.out.println(vmList.size());
			if(StringUtils.isNotBlank(otherParms)){
				Map<String,Object> other = JsonUtil.json2Map(otherParms);
				String msg = other.toString();
			}
		}

		return resultBean;
	}

}

