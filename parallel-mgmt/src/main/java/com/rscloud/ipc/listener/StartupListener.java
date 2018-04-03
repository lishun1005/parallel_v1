package com.rscloud.ipc.listener;


import com.rscloud.ipc.rpc.api.dto.SysMenuDto;
import com.rscloud.ipc.rpc.api.service.SysMenuService;
import com.rscloud.ipc.thread.MosaicStatusThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;

/**
 * 启动监听器
 * @author wugq
 */
@Service
public class StartupListener implements ApplicationListener<ContextRefreshedEvent> {
	public static Logger logger = LoggerFactory.getLogger(StartupListener.class);
	@Autowired
	@Lazy
	private SysMenuService sysMenuService;
	
	@Value("#{fixparamProperty[sysMgmtId]}")
	protected String sysMgmtId;
	
	@Value("#{fixparamProperty[queue_cut]}")
	protected String queueCut;

	@Value("#{fixparamProperty[queue_mosaic]}")
	protected String queueMosaic;
	
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (event.getApplicationContext().getParent() == null) {
			
			/** 类型 1:文件夹 2:页面 3:接口api **/
			//initSysPermission("数据查询","/dataCenter/list","image:dataCenter:*","2");
			
			initSysPermission("新增一张图切片_api","/api/v3/cut/addOnemapcut","v3.cut.addOnemapcut","3");
			initSysPermission("任务进度_api","/api/v3/cut/progress/","v3:cut:progress","3");
			initSysPermission("更新一张图切片_api","/api/v3/cut/uptOnemapcut","v3.cut.uptOnemapcut","3");
			initSysPermission("pl镶嵌_api","/api/v3/mosaic/pl","v3.mosaic.pl","3");
			initSysPermission("pl质量评估_api","/api/v3/mosaic/pl_quality","v3.mosaic.pl_quality","3");
			initSysPermission("GF1_08m镶嵌_api","/api/v3/mosaic/GF2_08m","v3.mosaic.GF2_08m","3");
			initSysPermission("GF1_2m镶嵌_api","/api/v3/mosaic/GF1_2m","v3.mosaic.GF1_2m","3");
			initSysPermission("GF1_16m镶嵌_api","/api/v3/mosaic/GF1_16m","v3.mosaic.GF1_16m","3");


			initSysPermission("镶嵌任务进度_api","/api/v3/mosaic/progress","v3.mosaic.progress","3");
			
			initSysPermission("一张图切片","/dataCenter/onemapCut","image:onemap:cut","3");
			initSysPermission("我的云盘","/userStorage/list","rsUser:userStorage:*","2");
			
			initSysPermission("图层管理","/production/mapManageList","production:mapManage:*","2");
			//initSysPermission("一张图切片任务","/production/oneMapCutJobList","production:oneMapCutJob:*","2");
			
			initSysPermission("创建任务","/production/createTask","production:createTask:list","2");
			
			initSysPermission("pl镶嵌接口","/mosaic/pl","image:mosaic:pl","3");
			initSysPermission("pl质量评估接口","/mosaic/pl_quality","image:mosaic:pl_quality","3");

			initSysPermission("GF2_08m镶嵌接口","/mosaic/GF2_08m","image:mosaic:GF2_08m","3");
			initSysPermission("GF1_2m镶嵌接口","/mosaic/GF1_2m","image:mosaic:GF1_2m","3");
			initSysPermission("GF1_16m镶嵌接口","/mosaic/GF1_16m","image:mosaic:GF1_16m","3");
			initSysPermission("modis镶嵌接口","/mosaic/modis","image:mosaic:modis","3");

			initSysPermission("任务监控","/production/jobMonitoring","production:job:*","2");
			initSysPermission("算法管理","/algorithm/list","production:algorithm:*","2");
			initSysPermission("模型管理","/optimalModel/list","production:optimalModel:*","2");
			initSysPermission("镶嵌生产线","/productline/list","production:productline:*","2");

			initSysPermission("集群虚拟机","/ai/vm/list","ai:vm:*","2");
			initSysPermission("人工智能模型","/ai/model/list","ai:model:*","2");

			initSysPermission("ai推理接口","/ai/inferenceAdd","ai:inference:*","3");

			new Thread(new MosaicStatusThread(queueMosaic),"_MosaicStatusThread").start();//查询镶嵌job进度线程
        }
	}
	
	/**
	 * 
	* Description: TODO
	*  @param name 名称
	*  @param url 
	*  @param permission
	*  @param menuType 类型 1:文件夹 2:页面 3:接口
	*  @return 
	* @author lishun 
	* @date 2017年7月12日 
	* @return boolean
	 */
	public boolean initSysPermission(String name,String url,String permission,String menuType){
		if(sysMenuService.checkPermission(permission, "")){
			logger.debug("已经存在名称[{}]的权限",permission);
		}else{
			SysMenuDto menu = new SysMenuDto();
			menu.setIsShow(true);
			menu.setPermission(permission);
			menu.setMenuType(menuType);
			menu.setName(name);
			menu.setUrl(url);
			menu.setParentId(sysMgmtId);//默认放在系统管理下面
			sysMenuService.editMenu(menu);
		}
		return false;
	}
	
	
	
	
	
}
