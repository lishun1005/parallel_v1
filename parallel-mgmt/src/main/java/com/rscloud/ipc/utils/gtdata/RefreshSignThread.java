package com.rscloud.ipc.utils.gtdata;



import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

/**
 * Description:gtdata定时刷新登录sign和time
 *
 * @author ljw 2014-10-11
 * 
 * @version v1.0
 *
 */
public class RefreshSignThread extends TimerTask
{
	public RefreshSignThread()
	{
		
	}
	
	public void run()
	{
		try
		{
			Map<String, Object> map = new HashMap<String, Object>();
			map = GtdataUtil.groupUserGetLoginedSign(GtdataUtil.getSign(), GtdataUtil.getSignTime());//更新
			if(1 == (Integer)map.get("result")){
				GtdataUtil.sign = (String) map.get("sign");
				GtdataUtil.signTime = (String) map.get("time");
			}else{
				GtdataUtil.updateGroupUserGetSignAndTime();//登录
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
}
