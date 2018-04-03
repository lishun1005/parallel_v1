package com.rscloud.ipc.contrller;

import com.rscloud.ipc.rpc.api.entity.AiModel;
import com.rscloud.ipc.rpc.api.entity.AiModelFile;
import com.rscloud.ipc.rpc.api.entity.AiModelParams;
import com.rscloud.ipc.rpc.api.result.ResultBean;
import com.rscloud.ipc.rpc.api.result.ResultCode;
import com.rscloud.ipc.rpc.api.service.AiModelService;
import com.rsclouds.common.utils.BeanMapper;
import com.rsclouds.common.utils.JsonUtil;
import com.rsclouds.common.utils.StringTool;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author lishun
 * @Description: TODO
 * @date 2018/1/30
 */
@Controller
public class AiModelContrlloer {
	@Autowired
	@Lazy
	public AiModelService aiModelService;

	@Value("#{fixparamProperty[aiModelFilePath]}")
	private String aiModelFilePath;

	@RequiresPermissions("ai:model:list")
	@RequestMapping(value = "ai/model/list", method = RequestMethod.GET)
	public String jobMonitoring(Integer rows, Integer pageNo, String keyword, Model model) {
		if (null == rows) {
			rows = 10;
		}
		if (null == pageNo) {
			pageNo = 1;
		}
		String userName = null;

		model.addAttribute("pageInfo", aiModelService.queryAll(keyword, pageNo, rows));
		return "/ai/modelList";
	}
	@RequiresPermissions("ai:model:list")
	@ResponseBody
	@RequestMapping(value = "ai/model/queryParmasByAiModelId", method = RequestMethod.GET)
	public ResultBean<AiModelParams> queryParmasByAiModelId(String aiModelId) {
		ResultBean<AiModelParams> resultBeanList = new ResultBean<AiModelParams>();
		resultBeanList.setCode(ResultCode.OK);
		resultBeanList.setResultDataList(aiModelService.queryParmasByAiModelId(aiModelId));
		return resultBeanList;
	}
	@RequiresPermissions("ai:model:add")
	@RequestMapping(value = "ai/model/add", method = RequestMethod.POST)
	@ResponseBody
	public ResultBean<String> save(String aiModelJson, String aiModelParamsJson) {
		ResultBean<String> resultBean = new ResultBean<>();
		if (StringUtils.isBlank(aiModelJson) || StringUtils.isBlank(aiModelParamsJson)) {
			resultBean.setResultBean(ResultCode.PARAMS_ERR, "json params is null");
		} else {
			AiModel aiModel = BeanMapper.map(JsonUtil.json2Map(aiModelJson), AiModel.class);
			List<AiModelParams> aiModelParams = BeanMapper.mapList(JsonUtil.toListMap(aiModelParamsJson),
					AiModelParams.class);
			if (StringUtils.isBlank(aiModel.getName())) {
				resultBean.setResultBean(ResultCode.PARAMS_ERR, "模型名不能为空");
			} else {
				if (aiModelService.findAiModelByName(aiModel.getName()) == null) {
					aiModel.setId(StringTool.getUUID());
					for (AiModelParams aiModelParam : aiModelParams) {
						if(StringUtils.isBlank(aiModelParam.getName()) ||
								aiModelParam.getType() == null){
							resultBean.setResultBean(ResultCode.PARAMS_ERR, "模型参数名称和类型不能为空");
							return resultBean;
						}
						aiModelParam.setId(StringTool.getUUID());
						aiModelParam.setAiModelId(aiModel.getId());
					}
					aiModelService.addModelAndParams(aiModel, aiModelParams);
					resultBean.setResultBean(ResultCode.OK, "添加成功");
				} else {
					resultBean.setResultBean(ResultCode.PARAMS_ERR, "模型名重复");
				}
			}
		}
		return resultBean;
	}

	@RequiresPermissions("ai:model:update")
	@RequestMapping(value = "ai/model/update", method = RequestMethod.POST)
	@ResponseBody
	public ResultBean<String>  update(String aiModelJson, String aiModelParamsJson) {
		ResultBean<String> resultBean = new ResultBean<>();

		if (StringUtils.isBlank(aiModelJson) || StringUtils.isBlank(aiModelParamsJson)) {
			resultBean.setResultBean(ResultCode.PARAMS_ERR, "json params is null");
		} else {
			AiModel aiModel = BeanMapper.map(JsonUtil.json2Map(aiModelJson), AiModel.class);
			List<AiModelParams> aiModelParams = BeanMapper.mapList(JsonUtil.toListMap(aiModelParamsJson),
					AiModelParams.class);
			if (StringUtils.isBlank(aiModel.getName())) {
				resultBean.setResultBean(ResultCode.PARAMS_ERR, "模型名不能为空");
			} else {
				AiModel aiModelById = aiModelService.findAiModelById(aiModel.getId());
				if(aiModelById == null){
					resultBean.setResultBean(ResultCode.PARAMS_ERR, "模型模型不存在");
				}else{
					if (aiModelService.findAiModelByName(aiModel.getName()) == null
							|| aiModelById.getName().equals(aiModel.getName())) {
						for (AiModelParams aiModelParam : aiModelParams) {
							if(StringUtils.isBlank(aiModelParam.getName()) ||
									aiModelParam.getType() == null){
								resultBean.setResultBean(ResultCode.PARAMS_ERR, "模型参数名称和类型不能为空");
								return resultBean;
							}
							aiModelParam.setId(StringTool.getUUID());
							aiModelParam.setAiModelId(aiModel.getId());
						}
						aiModelService.updateModelAndParams(aiModel, aiModelParams);
						resultBean.setResultBean(ResultCode.OK, "添加成功");
					} else {
						resultBean.setResultBean(ResultCode.PARAMS_ERR, "模型名重复");
					}
				}

			}
		}
		return resultBean;
	}

	@RequiresPermissions("ai:model:delete")
	@RequestMapping(value = "ai/model/delete", method = RequestMethod.GET)
	public String delete(AiModel aiModel, Model model) {
		if (StringUtils.isBlank(aiModel.getId())) {
			model.addAttribute("msg", "id is null");
		} else {
			aiModel.setIsDel((short) 1);
			aiModelService.update(aiModel);
			model.addAttribute("msg", "删除成功");
		}
		return "redirect:/ai/model/list";
	}

	@RequiresPermissions("ai:model:file")
	@RequestMapping(value = "ai/model/files", method = RequestMethod.POST)
	@ResponseBody
	public ResultBean<AiModelFile> queryFileByAiModelId(@RequestParam("aiModelId") String aiModelId){
		ResultBean<AiModelFile> resultBean = new ResultBean<AiModelFile>();
		resultBean.setResultDataList(aiModelService.queryFileByAiModelId(aiModelId));
		resultBean.setCode(ResultCode.OK);
		return resultBean;
	}
	@RequiresPermissions("ai:model:file")
	@RequestMapping(value = "ai/model/delfile", method = RequestMethod.POST)
	@ResponseBody
	public ResultBean<String> delfile(String id){
		ResultBean<String> resultBean = new ResultBean<String>();
		AiModelFile aiModelFile = aiModelService.delFileById(id);
		if(aiModelFile != null ){
			String filePath = aiModelFile.getPath();
			FileUtils.deleteQuietly(new File(filePath));
		}

		resultBean.setCode(ResultCode.OK);
		return resultBean;
	}
	@RequiresPermissions("ai:model:file")
	@RequestMapping(value = "ai/model/addfile", method = RequestMethod.POST)
	@ResponseBody
	public ResultBean<String> saveFile(MultipartFile modelFile, String type, String aiModelId) {
		ResultBean<String> resultBean = new ResultBean<>();

		if(modelFile == null || modelFile.isEmpty() || StringUtils.isBlank(type)){
			resultBean.setResultBean(ResultCode.PARAMS_ERR, "上传失败:文件和类型不能为空");
		}else {
			if(Short.valueOf(type) == 0){
				List<AiModelFile> aiModelFiles = aiModelService.queryFileByAiModelId(aiModelId);
				for (AiModelFile aiModelFile:aiModelFiles ) {
					if(aiModelFile.getType() == 0){
						resultBean.setResultBean(ResultCode.PARAMS_ERR, "已经存在主文件");
						return resultBean;
					}
				}
			}
			String fileName = modelFile.getOriginalFilename();
			String id = StringTool.getUUID();
			StringBuilder savePath = new StringBuilder(aiModelFilePath);
			savePath.append("/").append(id).append("_").append(fileName);
			try {
				FileUtils.copyInputStreamToFile(modelFile.getInputStream(), new File(savePath.toString()));

			} catch (IOException e) {
				resultBean.setResultBean(ResultCode.PARAMS_ERR, "上传失败:保存文件失败_" + e.getMessage());
				e.printStackTrace();
				return resultBean;
			}
			AiModelFile aiModelFile = new AiModelFile();
			aiModelFile.setId(id);
			aiModelFile.setType(Short.valueOf(type));

			aiModelFile.setFileName(fileName);
			aiModelFile.setPath(savePath.toString());
			aiModelFile.setAiModelId(aiModelId);
			aiModelService.addModelFile(aiModelFile);
			resultBean.setCode(ResultCode.OK);
		}
		return resultBean;
	}
}

