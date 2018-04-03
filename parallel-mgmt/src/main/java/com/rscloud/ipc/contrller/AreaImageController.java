package com.rscloud.ipc.contrller;



import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.rscloud.ipc.dto.AreaImageTotalVo;
import com.rscloud.ipc.rpc.api.service.AreaImageService;
import com.rscloud.ipc.utils.ShpFileOpUtil;


@Controller
public class AreaImageController {
	
	/*@Autowired
	public AreaImageService areaImageService;*/
	
	@Value("#{fixparamProperty[tmpShpFile]}")
	public String tmpShpFile;
	
	@Value("#{applicationProperty[imageDataQueryApi]}")
	public String imageDataQueryApi;
	
	@Value("#{applicationProperty[imageDataQueryDetailApi]}")
	public String imageDataQueryDetailApi;
	/**
	 * 
	* Description: 编目数据检索 若参数areaNo不为空，就以areaNo查询，不再查询geom
	*  @param areaImageTotalDto
	*  @return 
	* @author lishun 
	* @date 2017年7月27日 
	* @return Object
	 */
	@RequiresPermissions("image:dataCenter:areaImage.query")
	@RequestMapping(value = "/areaImage/query")
	@ResponseBody
	public Object getAreaImageList(AreaImageTotalVo areaImageTotalDto){
		Map<String,Object> map =new HashMap<String, Object>();
		try {
			areaImageTotalDto.setStartRow(0);
			areaImageTotalDto.setLimit(10000);
			RestTemplate rest = new RestTemplate();
			return rest.postForObject(imageDataQueryApi, areaImageTotalDto, String.class);//从数据中心获取编目数据
		} catch (Exception e) {
			e.printStackTrace();
			map.put("code", "2001");
			map.put("message", e.getMessage());
		}
		return map;
	}
		
	@RequiresPermissions("image:dataCenter:areaImage.queryDetail")
	@RequestMapping(value = "/areaImage/queryDetail/{id}")
	@ResponseBody
	public Object queryDetail(@PathVariable("id")String id){
		Map<String,Object> map =new HashMap<String, Object>();
		try {
			RestTemplate rest = new RestTemplate();
			return rest.getForObject(imageDataQueryDetailApi + id, String.class);//从数据中心获取编目数据
		} catch (Exception e) {
			e.printStackTrace();
			map.put("code", "2001");
			map.put("message", e.getMessage());
		}
		return map;
	}
	@RequiresPermissions("image:dataCenter:uploadShpFile")
	@RequestMapping(value="/uploadShpFile")
	@ResponseBody
	public Map<String, Object> Importshpfile(MultipartFile file,HttpServletRequest request) {
		Map<String,Object> map = new HashMap<String, Object>();
		try {
			if(file == null || file.isEmpty()){
				map.put("code", 0);
				map.put("message", "请上传文件！");
				return map;
			}
			if(!(file.getOriginalFilename().substring(file.getOriginalFilename().length()-4, file.getOriginalFilename().length()).toUpperCase()).equals(".ZIP")){
				map.put("code", 0);
				map.put("message", "请上传.zip 压缩文件！");
			}else{
				map = ShpFileOpUtil.readFileAndPareseByStream(file.getBytes(),file.getOriginalFilename(),tmpShpFile);
			}
		} catch (Exception e) {
			e.printStackTrace();
			map=new HashMap<String, Object>();
			map.put("code", 0);
			map.put("message", "后台发生异常，可能shp压缩包的文件格式不对，请重试！");
		}
		return map;
	}
	
}
