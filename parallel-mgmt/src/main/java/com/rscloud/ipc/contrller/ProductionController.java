package com.rscloud.ipc.contrller;

import com.rscloud.ipc.rabbitmq.RabbitmqUtils;
import com.rscloud.ipc.rpc.api.dto.MapManageDto;
import com.rscloud.ipc.rpc.api.result.PageResultBean;
import com.rscloud.ipc.rpc.api.result.ResultBean;
import com.rscloud.ipc.rpc.api.service.*;
import com.rscloud.ipc.shiro.ShiroKit;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
* @ClassName: 生产管理  
* @Description: TODO
* @author lishun 
* @date 2017年7月21日 下午2:20:34  
*
 */
@Controller
public class ProductionController extends BaseContrller{
	@Autowired
	@Lazy
	private CutService cutService;
	@Autowired
	@Lazy
	private MosaicService mosaicService;

	@Autowired
	@Lazy
	private ProductlineService productlineService;

	@Autowired
	@Lazy
	public AiModelService aiModelService;

	@Autowired
	@Lazy
	public AiVmService aiVmService;
	
	@Value("#{fixparamProperty[queue_cut]}")
	protected String queueCut;
	
	@Value("#{fixparamProperty[queue_mosaic]}")
	protected String queueMosaic;
	
	@RequiresPermissions("production:job:mosaicDel")
	@ResponseBody
	@RequestMapping(value = "production/mosaicDel/{id}", method = RequestMethod.GET)
	public Map<String, Object> mosaicDel(@PathVariable("id")String id) {
		Map<String, Object> result=new HashMap<String, Object>();
		try {
			mosaicService.mosaicDel(id);
			result.put("code", "2001");
		} catch (Exception e) {
			result.put("code", "2002");
			result.put("errorMessage", " 删除失败" + e.getMessage());
			e.printStackTrace();
		}
		return result;
	}
	@RequiresPermissions("production:job:cutJobUpdate")
	@ResponseBody
	@RequestMapping(value = "production/cutJobUpdate", method = RequestMethod.POST)
	public Map<String, Object> cutJobUpdate(String id, Integer priority,Model model) {
		Map<String, Object> result = new HashMap<String, Object>();
		try {
			ResultBean<Boolean> res = cutService.cutJobChangePriority(id, priority);
			if(res.getResultData()){
				result.put("code", "2001");
			}else{
				result.put("code", "2002");
				result.put("errorMessage", " 修改优先级失败");
			}
			model.addAttribute("msg", "更新成功");
		} catch (Exception e) {
			result.put("code", "2002");
			result.put("errorMessage", " 更新失败;" + e.getMessage());
			e.printStackTrace();
		}
		return result;
	}
	@RequiresPermissions("production:job:cutJobDel")
	@ResponseBody
	@RequestMapping(value = "production/cutJobDel/{id}", method = RequestMethod.GET)
	public Map<String, Object> cutJobDel(@PathVariable("id")String id) {
		Map<String, Object> result=new HashMap<String, Object>();
		try {
			cutService.cutJobDel(id);
			result.put("code", "2001");
		} catch (Exception e) {
			result.put("code", "2002");
			result.put("errorMessage", " 删除失败" + e.getMessage());
			e.printStackTrace();
		}
		return result;
	}
	
	
	@RequiresPermissions("production:job:cutJobDel")
	@ResponseBody
	@RequestMapping(value = "production/cutJobDelMq/{jobid}", method = RequestMethod.GET)
	public Map<String, Object> cutJobDelMq(@PathVariable("jobid")String jobid) {
		Map<String, Object> result=new HashMap<String, Object>();
		try {
			RabbitmqUtils.removeQueueMsg(queueCut, jobid);
			result.put("code", "2001");
		} catch (Exception e) {
			result.put("code", "2002");
			result.put("errorMessage", " 删除失败" + e.getMessage());
			e.printStackTrace();
		}
		return result;
	}
	@RequiresPermissions("production:job:mosaicDel")
	@ResponseBody
	@RequestMapping(value = "production/mosaicDelMq/{jobid}", method = RequestMethod.GET)
	public Map<String, Object> mosaicDelMq(@PathVariable("jobid")String jobid) {
		Map<String, Object> result=new HashMap<String, Object>();
		try {
			RabbitmqUtils.removeQueueMsg(queueMosaic, jobid);
			result.put("code", "2001");
		} catch (Exception e) {
			result.put("code", "2002");
			result.put("errorMessage", " 删除失败" + e.getMessage());
			e.printStackTrace();
		}
		return result;
	}
	
	
	
	@RequiresPermissions("production:job:cutjobDetail")
	@ResponseBody
	@RequestMapping(value = "production/oneMapCutJobDetail/{id}", method = RequestMethod.GET)
	public Map<String, Object> queryById(@PathVariable("id")String id) {
		ResultBean<Map<String, Object>> resultBean = cutService.queryById(id);
		return resultBean.getResultData();
	}
	
	@RequiresPermissions("production:job:mosaicDetail")
	@ResponseBody
	@RequestMapping(value = "production/mosaicDetail/{id}", method = RequestMethod.GET)
	public Map<String, Object> mosaicDetail(@PathVariable("id")String id) {
		return mosaicService.mosaicDetail(id).getResultData();
	}
	
	
	@RequiresPermissions("production:job:mosaicLog")
	@ResponseBody
	@RequestMapping(value = "production/mosaicLog/{id}", method = RequestMethod.GET)
	public List<Map<String, Object>> mosaicLog(@PathVariable("id")String id) {
		return mosaicService.querylog(id).getResultData();
	}
	
	@RequiresPermissions("production:job:cutJobLog")
	@ResponseBody
	@RequestMapping(value = "production/cutJobLog/{id}", method = RequestMethod.GET)
	public List<Map<String, Object>> cutJobLog(@PathVariable("id")String id) {
		return cutService.querylog(id).getResultData();
	}
	
	@RequiresPermissions("production:job:monitoring")
	@RequestMapping(value = "production/jobMonitoring", method = RequestMethod.GET)
	public String jobMonitoring(Integer rows, Integer pageNo, String keyword,String status,String algorithmType,
			@RequestParam(defaultValue="cut")String pageType,Model model) {
		if (null == rows) {
			rows = 10;
		}
		if (null == pageNo) {
			pageNo = 1;
		}
		String userName=null;
		if(ShiroKit.getSysUser().getUserType()==2){
			userName = ShiroKit.principal();//集市用户只能查询自身的切片记录
		}
		PageResultBean<Map<String, Object>> map = null;
		if("cut".equals(pageType)){
			 map = cutService.queryAll(keyword,status, pageNo, rows, userName);
		}else if("mosaic".equals(pageType)){
			 if(StringUtils.isBlank(algorithmType)){
				 algorithmType="pl";
			 }
			 map = mosaicService.queryAll(keyword,status, pageNo, rows, userName, algorithmType);
		}else{
			
		}
		model.addAttribute("productlinePage", productlineService.queryAll(null, 1, 100000));
		model.addAttribute("pageInfo", map);
		model.addAttribute("pageType", pageType);
		return "/production/jobMonitoringList";
	}

	/**
	 * 
	* Description: 图层列表
	*  @param rows
	*  @param pageNo
	*  @param keyword
	*  @param model
	*  @return 
	* @author lishun 
	* @date 2017年7月21日 
	* @return String
	 */
	@RequiresPermissions("production:mapManage:list")
	@RequestMapping(value = "production/mapManageList", method = RequestMethod.GET)
	public String mapManageList(Integer rows, Integer pageNo, String keyword,
			Model model) {
		if (null == rows) {
			rows = 10;
		}
		if (null == pageNo) {
			pageNo = 1;
		}
		String userName=null;
		if(ShiroKit.getSysUser().getUserType() == 2){
			userName=ShiroKit.principal();//集市用户只能查询自身的切片记录
		}

		model.addAttribute("pageInfo", cutService.queryMapManageAll(keyword, pageNo, rows,userName));
		
		return "/production/mapManageList";
	}
	@RequiresPermissions("production:mapManage:update")
	@RequestMapping(value = "production/mapManageUpdate", method = RequestMethod.POST)
	public String updateById(MapManageDto record, Model model){
		try {
			cutService.updateMapManageById(record);
			model.addAttribute("msg", "更新成功");
		} catch (Exception e) {
			e.printStackTrace();
			model.addAttribute("msg", "更新失败");
		}
		return "redirect:/production/mapManageList";
	}
	@RequiresPermissions("production:createTask:list")
	@RequestMapping(value = "/production/createTask", method = RequestMethod.GET)
	public String createTask(Model model){
		model.addAttribute("pageInfo", productlineService.queryAll(null, 1, 100000));
		/*model.addAttribute("aiModelInfo", aiModelService.queryAll(null, 1, 100000));
		model.addAttribute("aiVmInfoList", aiVmService.queryAll(null, 1, 100000));*/
		model.addAttribute("aiModelInfo", null);
		model.addAttribute("aiVmInfoList", null);
		return "/production/createTask";
	}

}
