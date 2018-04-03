
$(function(){
	/*镶嵌算法信息*/
	$(".liucheng").each(function () {
		var modelId = $(this).attr("modelId");
		var currentDiv = $(this);
		if(modelId != ""){
            $.ajax({
                type : 'get',
                url:manageHost + '/optimalModel/queryOptimalModelByModelId?modelId='+ modelId,
                success : function(data) {
                    if(data.code == 2001){
                        if (data.resultDataList != null){
                            for(i =0 ; i<data.resultDataList.length; i++){
                                currentDiv.append("<div class='pull-left line80'>→</div>")
                                    .append("<div class='round'></p><span>" + data.resultDataList[i].displayname +"</span></div>");
                            }
                        }
                    }else {
                        window.parent.showMsg(data.message);
                    }
                }
            });
		}

    });
	//显示云盘弹窗
	$(document).on("click",".chooseModal",function(){		
		$("#yunpan").modal("show");
		queryOnlyDirectory("1");		
	})
	//云盘提交
	$(document).on("click",".confirm-delete-btn",function(){		
		$("#yunpan").modal("hide");		
	})
	//数据来源按钮
	$(document).on("click",".btn-inpath",function(){		
		$("#yunpan").modal("show");
		$("#yunpan").addClass("yinpath").removeClass("youtpath");		
		queryOnlyDirectory("2");
	})
	
	
	//弹窗选择文件
	$(document).on("click",".outfile",function(){		
		$("#yunpan").modal({
			show:true,
			backdrop:false,						
		});
		$("#yunpan").css("z-index","10000");
		$("#yunpan").attr("flag","2");
		$("#yunpan").addClass("youtpath").removeClass("yinpath");
		queryOnlyDirectory("2");
		//$(".outPath:visible").val($("#pathFile").val());
	});
	
	/*******************查看监控*****************/
	$(document).on("click",".btn-watch",function(){
		watchInfo();
	})
	$(document).on("click","#tab1 .btn-xz",function(){
			$("#qiepianModal .errorMsg").text("");
	})
	//pl镶嵌选择
	$(document).on("click","#tab2 .btn-xz",function(){
		var urlType = $(this).attr("attr");
		if(urlType == "modis"){
            $(".errorinpath2").text("");
            $("#modis_modal").modal("show");
		}else if(urlType == "pl_quality"){
            $("#fileTypes").addClass("hide");
            $("#fileTypes2").removeClass("hide");
            $(".errorinpath2").text("");
            $("#xiangqian").modal("show");
            $("#mosaicForm input[name=outPath]").val("");
            $(".errorMsg").text("");
            $(".divgroup").hide();
            $(".submit-btn").hide();
            $(".submit-btn2").show();
		}else{
            $("#fileTypes").removeClass("hide");
            $("#fileTypes2").addClass("hide");
            //清空信息
            $(".errorinpath2").text("");
            $("#mosaicForm input[name=outPath]").val("");
            $(".errorMsg").text("");
            //赋值 使用同一个弹框，用数字却别请求接口
            $(".typeMosaic").val(urlType);
            $("#xiangqian").modal("show");//显示弹窗
            $(".divgroup").show();
            $(".submit-btn").show();
            $(".submit-btn2").hide();
		}

	});

	
	//pl，GF提交
	$(document).on("click",".submit-btn",function(){
		var type = $(".typeMosaic").val();
		mosaicSubmit(type);
	});
	//路径判断
	
	$(".disinpath").change(function(){ //文本
		var self = $(this).parent();
		var  path = $(this).val();
		pathFileReset(self,path,"1");
	})
	$("#mosaicForm .outPath").change(function(){//弹窗
		var  path = $(this).val();
		pathFileReset(null,path,"2");
	})
	
	$('.tab-nav a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
		$(".errorinpath,.errorMsg").text("");		
		$(".disinpath").val("");		
	})
	
})

/**
 * 路径判断
 * */
function pathFileReset(self,path,flag){
	$("#waiting").show();
	$.getJSON(manageHost+"/userStorage/existPath",{path:path},function(data) {
		$("#waiting").hide();
		if(data == false){
			if(flag =="1"){				
				self.find(".errorinpath:visible").empty().text("路径不存在!");
			}else{
				$(".errorMsg").text("路径不存在!");
			}
			//self.find(".errorinpath2:visible").empty().text("路径不存在!");
		}else{
			if(flag =="1"){				
				$(".errorinpath:visible").empty().text("");
			}else{
				$(".errorMsg").text("");
			}
		}
	})
}

/**
 * 镶嵌接口
 * 
 * */
function mosaicSubmit(type){
	var form=$("#mosaicForm");
	var outPath=form.find("input[name=outPath]:visible").val(),//输出路径
		project =form.find("#project:visible option:selected").val(),//投影
		outBand=form.find("#outBand:visible option:selected").text(),	//	输出波段字节
		outImage=form.find("#outImage:visible option:selected").text(),//输出影像字节

		jobName  =$("#tab2 #firstname").val(),
		filesType  =$("#fileTypes").val(),
		errorinpath =$("#tab2 .errorpublic .errorinpath").text(),//路径不存在
		errorinpath2 = $("#xiangqian .errorMsg").text(),
		inPath  = $(".disinpath:visible").val();//数据来源
	if(inPath ==""){
		alert("请选择数据来源");
		$("#xiangqian").modal("hide");
		return;
	}else if(errorinpath !=""){
		alert("来源路径不存在!");			
		$("#xiangqian").modal("hide");
		return;	
	}else if(outPath ==""){
			alert("请选择输出路径");			
			return;		
	}else if(errorinpath2 !=""){
		alert("输出路径不存在!");			
		return;	
	}else if(jobName ==""){
		alert("请填写任务名称");
		$("#xiangqian").modal("hide");
		return;
	}else if(filesType ==""){
		alert("请填写格式");
		return;
	}

	var parmas ={
		"outPath":outPath+"/"+filesType,
		"jobName":jobName,
		"inPath":inPath,
		"project":project,
		"outBand":outBand,
		"outImage":outImage
	}
	
	var url = manageHost + "/mosaic/" + type;
	
	$("#waiting").show();
	$.ajax({
		url:url,
		type:"post",
		data:parmas,
		success:function(res){
			if(res.code==2001){
				$("#xiangqian").modal("hide");
				$("#waiting").hide();
				location.href=manageHost+"/production/jobMonitoring?pageType=mosaic&msg=提交成功";
			}else{
				$("#waiting").hide();
				$(".errorMsg_back").text(res.message);
			}
		},
		error:function(xhr,status,error)	{
			if(xhr.status == "401"){				
				$("#waiting").hide();
				$(".errorMsg_back").text("没有权限！");
			}
		}
	});
}
/*
 * @Description:modis镶嵌
 * @author lishun  
 * @date 2017/11/27
 * @param   
 * @return   
 */
function modisSubmit() {
    var form = $("#modisForm");
    var outPath = form.find("input[name=outPath]:visible").val(),//输出路径
        jobName  =$("#tab2 #firstname").val(),
        filesType = form.find("input[name=fileTypes]:visible").val(),
        inPath  = $(".disinpath:visible").val(),
	    project =form.find("#project:visible option:selected").val();//投影
	if(checkFormValue("modisForm")){
		var is_submit = false;
		if(inPath ==""){
            alert("请选择数据来源");
            $("#xiangqian").modal("hide");
            return;
        }
        if(jobName == ""){
            $("#xiangqian").modal("hide");
            alert("请选择任务名称！");
            return;
		}
        $("#waiting").show();
        $.getJSON(manageHost + "/userStorage/existPath",{ path:outPath },function(data) {
            if(data == false){
                showPopover(form.find("input[name=outPath]:visible"),"目录不存在");
                $("#waiting").hide();
            }else {
                $("#waiting").show();
                $.getJSON(manageHost + "/userStorage/existPath",{ path:outPath + "/" + filesType },function(data) {
                    if(data == true){
                        $("#waiting").hide();
                        showPopover(form.find("input[name=fileTypes]:visible"),"该文件已存在")
                    }else {
                        var parmas ={
                            "outPath":outPath + "/" + filesType,
                            "inPath":inPath,
                            "jobName":jobName,
							"project":project
                        }
                        $("#waiting").show();
						$.ajax({
							url: manageHost+"/mosaic/modis",
							type: "post",
							data: parmas,
							success: function(res){
								if(res.code == 2001){
									$("#xiangqian").modal("hide")
									$("#waiting").hide();
									location.href = manageHost+"/production/jobMonitoring?pageType=mosaic&msg=提交任务成功";
								}else{
									$("#waiting").hide();
									$("#modis_modal").find(".error_message").text(res.message);
								}
							},
							error:function(xhr,status,error){
								if(xhr.status == "401"){
									$("#waiting").hide();
                                    $("#modis_modal").find(".error_message").text("没有权限！");
								}
							}
						})
                    }
                });
			}
        });



	}

}
/**
 * pl质量评估
 * */

function mosaicPlSubmit(){
	//$(".errorMsg").text("");
	
	var form=$("#mosaicForm");
	var	outPath=form.find("input[name=outPath]:visible").val(),//输出路径
		jobName  =$("#tab2 #firstname").val(),
		errorinpath =$("#tab2 .errorpublic .errorinpath").text(),//路径不存在
		errorinpath2 = $("#xiangqian .errorMsg").text(),
		filesType = $("#mosaicForm #fileTypes2").val(),
		inPath  = $(".disinpath:visible").val();

	if(inPath ==""){
		alert("请选择数据来源！");
		$("#xiangqian").modal("hide");
		return;
	}else if(errorinpath !=""){
		alert("来源路径不存在!");			
		$("#xiangqian").modal("hide");
		return;	
	}else if(errorinpath2 !=""){
		alert("输出路径不存在!");			
		return;	
	}else if(jobName ==""){
		$("#xiangqian").modal("hide");
		alert("请选择任务名称！");
		return;
	}else if(filesType ==""){
		alert("请填写格式类型！");
		return;
	}

	var parmas ={
		"outPath":outPath+"/"+filesType,
		"inPath":inPath,
		"jobName":jobName

	}
	$("#waiting").show();

	$.ajax({
		url:manageHost+"/mosaic/pl_quality",
		type:"post",
		data:parmas,
		success:function(res){
			if(res.code==2001){
				$("#xiangqian").modal("hide");
				//location.href="/production/oneMapCutJobList?msg=提交任务成功";
				$("#waiting").hide();
				location.href=manageHost+"/production/jobMonitoring?pageType=mosaic&msg=提交任务成功";
			}else{
				$("#waiting").hide();
				$(".errorMsg_back").text(res.message);
			}
		},
		error:function(xhr,status,error){
			if(xhr.status == "401"){				
				$("#waiting").hide();
				$(".errorMsg_back").text("没有权限！");
			}
		}
	})
}
/**
 * 切片接口
 * 
 * */
function cuttingSubmit(){
	$("#qiepianModal .errorMsg").text("");
	var form=$("#cuttingForm");
	var cutType=form.find("select[name=cutType]:visible").val();
	var mapName=form.find("input[name=mapName]:visible").val();
	var minLayers=form.find("input[name=minLayers]:visible").val();
	var maxLayers=form.find("input[name=maxLayers]:visible").val();
	var outPath=form.find("input[name=outPath]:visible").val();
	var usage=form.find("input[name=usage]:visible").val();
	var inPath=form.find("input[name=inPath]").val();
	var isCover=form.find("select[name=isCover]:visible").val();
	var waterMark=form.find("select[name=waterMark]:visible").val();
	var jobName=$("#tab1 #firstname").val();
	var realTime=form.find("input[name=realTime]:visible").val();
	var priority=form.find("input[name=priority]:visible").val();
	
	var  regNum =/^[1-9]\d*|0$/;	   
	var re = new RegExp(regNum);
	//校验
	var file =$("#pathFile").val();;
	if(file == ""){		
		file = $("#tab1 .disinpath").val();		
	}
	/*if(file.indexOf(".tif") == -1){
		alert("请选择影像tif文件");
		$("#qiepianModal").modal("hide");
		return;
	}*/
	if(mapName == "" ){
		alert("请填写图层名!");
		form.find("input[name=mapName]").focus();
		return;
	}else if(jobName ==""){
		alert("请填写任务名称");
		$("#tab1 #firstname").focus();
		$("#qiepianModal").modal("hide");
		return;
	}else if(outPath ==""){
		alert("请填写输出路径");
		form.find("input[name=outPath]").focus();
		return;
	}else if(minLayers >20){
		alert("层级输入最大20");		
		form.find("input[name=minLayers]").focus();
		return;
	}else if(maxLayers >20){
		alert("层级输入最大20");		
		form.find("input[name=maxLayers]").focus();
		return;	
	}else if(maxLayers ==""){
		alert("请填写最大层级");
		form.find("input[name=maxLayers]").focus();
		return;
	}else if(re.test(maxLayers) == false){		
		alert("最大层级不能为负");
		form.find("input[name=maxLayers]").focus();
		return;
	}else if(minLayers == ""){
		alert("请填写最小层级");		
		form.find("input[name=minLayers]:visible").focus();
		return;
		
	}else if(parseFloat(minLayers) > parseFloat(maxLayers)){
		alert("最小层级不能大于最大层级");
		form.find("input[name=minLayers]:visible").val("");
		return;
	}
	var parmas ={
		//"cutType":cutType,
		"jobName":jobName,
		"mapName":mapName,
		"minLayers":minLayers,
		"isCover":isCover,
		"maxLayers":maxLayers,
		"outPath":outPath,
		"usage":usage,
		"inPath":file,
		"waterMark":waterMark,		
		"priority":priority
	}
	if(!form.find("input[name=minLayers]").parent().hasClass("hide")){		
		parmas.minLayers = minLayers
	}
	if(!form.find("input[name=realTime]").parent().hasClass("hide")){		
		if(realTime ==""){
			delete parmas.realTime;
		}else{
			parmas.realTime = realTime
			if(check(realTime) == false){
				return;
			};
		}		
	}
    $("#waiting").show();
	var urlAddr = "";
    if(cutType == 0){
        urlAddr = manageHost + "/dataCenter/cutAdd"
	}else{
        urlAddr = manageHost + "/dataCenter/cutUpt"
	}
    $.ajax({
        url: urlAddr,
        type:"post",
        contentType:"application/json",
        data:JSON.stringify(parmas),
        success:function(res){
            if(res.code==2001){
                $("#qiepianModal").modal("hide");
                $("#waiting").hide();
                window.location.href=manageHost+"/production/jobMonitoring?pageType=cut&msg=提交任务成功";
            }else{
                $("#waiting").hide();
                $("#qiepianModal .errorMsg").text(res.message);
            }
        },
        error: function(XMLHttpRequest, textStatus, errorThrown) {
            $("#waiting").hide();
            $(".errorMsg").text(XMLHttpRequest.responseJSON.message);
        }
    })

}
function check(str){
	//var a = /((?:19|20)\d{2}\/(?:0[1-9]|1[0-2])\/(?:0[1-9]|[12][0-9]|3[01]))/;
	var a  =/((^((1[8-9]\d{2})|([2-9]\d{3}))([-\/\._])(10|12|0?[13578])([-\/\._])(3[01]|[12][0-9]|0?[1-9])$)|(^((1[8-9]\d{2})|([2-9]\d{3}))([-\/\._])(11|0?[469])([-\/\._])(30|[12][0-9]|0?[1-9])$)|(^((1[8-9]\d{2})|([2-9]\d{3}))([-\/\._])(0?2)([-\/\._])(2[0-8]|1[0-9]|0?[1-9])$)|(^([2468][048]00)([-\/\._])(0?2)([-\/\._])(29)$)|(^([3579][26]00)([-\/\._])(0?2)([-\/\._])(29)$)|(^([1][89][0][48])([-\/\._])(0?2)([-\/\._])(29)$)|(^([2-9][0-9][0][48])([-\/\._])(0?2)([-\/\._])(29)$)|(^([1][89][2468][048])([-\/\._])(0?2)([-\/\._])(29)$)|(^([2-9][0-9][2468][048])([-\/\._])(0?2)([-\/\._])(29)$)|(^([1][89][13579][26])([-\/\._])(0?2)([-\/\._])(29)$)|(^([2-9][0-9][13579][26])([-\/\._])(0?2)([-\/\._])(29)$))/;
	if (!a.test(str)) { 
		alert("日期格式不正确!") 		
		return false;
	}
} 

function cutTypeChange(e){
	var form=$("#cuttingForm");
	var val=$(e).val();
	if(val==0){//新增，不需要填写最小层级
		form.find("input[name=minLayers]").parent().addClass("hide");
		form.find("input[name=realTime]").parent().addClass("hide");
		form.find("select[name=waterMark]").css("display","block").parent().css("display","block");
		form.find("input[name=usage]").css("display","block").parent().css("display","block");
		form.find("input[name=outPath]").css("display","block").parent().css("display","block");
		
	}else{//更新 不需要填写用途，水印方案，输出路径
		form.find("input[name=realTime]").parent().removeClass("hide");
		form.find("input[name=minLayers]").parent().removeClass("hide");
		
		form.find("select[name=waterMark]").val("").css("display","none").parent().css("display","none");
		form.find("input[name=usage]").val("").css("display","none").parent().css("display","none");
		form.find("input[name=outPath]").val("").css("display","none").parent().css("display","none");
	}
}

//点击选中某个文件夹的时候设置值
function selectTheNode(path,pPath,name)
{			
	if($("#yunpan").hasClass("youtpath")){		
		$(".outPath:visible").val(path);
	}else{		
		$(".disinpath:visible").val(path);
	}	
	$("#pathFile").val(path);	
}


/**
 * 云盘
 * */
var treeSetting,treeObj;
function ztreeOnAsyncSuccess(event,treeId,treeNode){
	treeObj.selectNode(treeNode);
}
var cacheData = "";
var cacheDataDir = "";

function queryOnlyDirectory(strObj){
	// 树的设置参数
	if(cacheDataDir != "" && strObj == 2){
        onloadTreeData(cacheDataDir);
	}else if(cacheData != "" && strObj == 1){
        onloadTreeData(cacheData);
	}else{
        var r = Math.floor(Math.random() * 9999 + 1);
        if(strObj =="2"){
            var url = manageHost+"/userStorage/queryDirectory?r=" + r;//文件类型
        }else{
            var url = manageHost+"/userStorage/queryDirectory?showFile=true&r="+ r;//文件夹
        }
        $("#waiting").show();
        $.ajax({
            url:url,
            type:"post",
            data:null,
            success:function(data){
                if(strObj =="2"){
                    cacheDataDir = data;
				}else{
                    cacheData = data;
				}
                $("#waiting").hide();
                onloadTreeData(data);
            }
        })
	}
}
function onloadTreeData(data){
    treeSetting = {
        async: {
            enable: true,
        },
        data: {
            simpleData: {
                enable: true
            }
        },
        edit: {
            enable: false,
            isCopy: false,
            isMove: false,
            showRenameBtn: false,
            showRemoveBtn: false
        },
        callback: {
            onClick:function(event,treeId,treeNode){
                treeNode = JSON.stringify(treeNode);
                sessionStorage.setItem("treeNode",treeNode);
            }
        }
    };
    var zNodes = data.list,str =[];
    for(var i in zNodes){
        if(zNodes[i].size == "-1"){//文件夹
            zNodes[i].iconSkin ="folder";
        }
        str[i]=zNodes[i];
    }
    $.fn.zTree.init($("#treeDemo"), treeSetting,str);
    treeObj = $.fn.zTree.getZTreeObj("treeDemo");
    var node = sessionStorage.getItem("treeNode");
    if(node !=""){
        node =JSON.parse(node);
        $("#"+node.tId+"_a").click();
    }
}
function filter(treeId, parentNode, childNodes) {
	if (!childNodes) return null;
	for (var i=0, l=childNodes.length; i<l; i++) {
		childNodes[i].name = childNodes[i].name.replace(/\.n/g, '.');
	}
	return childNodes;
}

//查看详情
function watchInfo(){
	var obj    =$(this).closest(".trId"),
	 	  objId = obj.attr("id"),
	 	  html  ="";					
	$("#msgModals").modal("show");
	$("#waiting").show();;
	var url =manageHost+"/production/oneMapCutJobDetail/"+objId;
	$.getJSON(url,null,function(data) {
		
			html +="<p>操作人："+data.operationname+"</p>";
			if(data.geowebcache_url == undefined){
				html +="<p>底图服务地址：</p>";								
			}else{
				html +="<p>底图服务地址："+data.geowebcache_url+"</p>";
			}
			html +="<p>最小层级："+data.min_layers+"</p>";
			html +="<p>最大层级："+data.max_layers+"</p>";
			html +="<p>图层名："+data.map_name+"</p>";
			html +="<p>状态："+data.status+"</p>";
			html +="<p>输出路径："+data.out_path+"</p>";
			html +="<p>输入路径："+data.in_path+"</p>";							
			html +="<p>日志："+data.log+"</p>";
			html +="<p>jobID："+data.jobid+"</p>";
			html +="<p>水印："+data.water_mark+"</p>";
			html +="<p>是否覆盖："+data.is_cover+"</p>";
			html +="<p>拥有人："+data.ownername+"</p>";
			if(data.cut_type == 0){							
				html +="<p>切片类型：新增</p>";
			}else{
				html +="<p>切片类型：更新</p>";
			}							
			html +="<p>用途："+data.usage+"</p>";
			if(data.accept_time == undefined){
				html +="<p>接收时间：</p>";	
			}else{								
				html +="<p>接收时间："+timeFormat(data.accept_time)+"</p>";		
			}
				
			if(data.real_time == undefined){
				html +="<p>实时时间：</p>";		
			}else{
				html +="<p>实时时间："+timeFormat(data.real_time)+"</p>";			
			}
	
			$(".modal-body").html(html);
			$("#waiting").hide();;
	})
}

function timeFormat(time) {     
	var timestamp = String(time);
	timestamp = timestamp.replace(/^\s+|\s+$/, '');
	
	if (/^\d{10}$/.test(timestamp)) {
		timestamp *= 1000;
	} else if (/^\d{13}$/.test(timestamp)) {
		timestamp = parseInt(timestamp);
	} else {
		alert('时间戳格式不正确！');
		return;
	}
	return format(timestamp);
};

function format(timestamp) {
	var time = new Date(timestamp);
	var year = time.getFullYear();
	var month = (time.getMonth() + 1) > 9 && (time.getMonth() + 1) || ('0' + (time.getMonth() + 1))
	var date = time.getDate() > 9 && time.getDate() || ('0' + time.getDate())
	var hour = time.getHours() > 9 && time.getHours() || ('0' + time.getHours())
	var minute = time.getMinutes() > 9 && time.getMinutes() || ('0' + time.getMinutes())
	var second = time.getSeconds() > 9 && time.getSeconds() || ('0' + time.getSeconds())
	var YmdHis = year + '-' + month + '-' + date
		+ ' ' + hour + ':' + minute + ':' + second;
	return YmdHis;
}

function checkFilePth(e){
	var form = $("#mosaicForm");
	var outPath=form.find("input[name=outPath]:visible").val(),//输出路径
		filesType  =$(e).val();
	if("" != outPath && "" != filesType){
		$.getJSON(manageHost+"/userStorage/existPath",{path:outPath + "/" + filesType},function(data) {
			if(data == true){
				$("#xiangqian .errorMsg").empty().text( filesType + "该文件已存在");
			}else{
				$("#xiangqian .errorMsg").text("");
			}
		})
	}
}


function ai_inference_show(id) {

    $("#ai_inference_modal").modal("show");
    $("#ai_inference_modal").find('.newOtherParms').remove();
    $.ajax({
        url: manageHost + "/ai/model/queryParmasByAiModelId?aiModelId=" + id,
        type: "get",
        success:function (res) {
            if(res.resultDataList != null){
				var list = res.resultDataList;
				for(var i = 0; i < list.length; i++ ){
					var typeStr = "";
                    var pattern = "";
					if(res.resultDataList[i].type == 0){
                        typeStr = '小数';
                        pattern = "/^[+-]?(0|([1-9]\\d*))(\\.\\d+)?$/g";
					}else if (res.resultDataList[i].type == 1){
                        typeStr = '整数';
                        pattern = "/^([1-9][0-9]*)$/";
					}else{
                        typeStr = '字符串';
					}
					if(i == 0){
						var div =  $("#ai_inference_modal").find(".otherParms");
                        div.find("label").text(res.resultDataList[i].remark + ":")
                        div.find("input").attr("name", res.resultDataList[i].name).attr("placeholder",typeStr).attr("pattern",pattern);
					}else {
                        var clone = $("#ai_inference_modal").find(".otherParms").clone();
                        clone.find("label").text(res.resultDataList[i].remark + ":");
                        clone.find("input").attr("name", res.resultDataList[i].name).attr("placeholder",typeStr).attr("pattern","").attr("pattern",pattern);
                        clone.removeClass("otherParms").addClass("newOtherParms");

                        $("#ai_inference_modal").find(".otherParms").after(clone)
                    }
				}
			}
        }

    })
}
function addIpParams(e) {
    var clone = $(e).parent().parent().clone();
    clone.find("input[type=text]").val("");
    var btn = clone.find('.btn-default');
    btn.find(".glyphicon-plus").removeClass("glyphicon-plus").addClass("glyphicon-minus");
    btn.attr("onclick","delIpParams(this)")
    $(e).parent().parent().after(clone);
    return clone;
}
function delIpParams(e) {
    $(e).parent().parent().remove();
}
function aiInferenceAdd() {
    if (checkFormValue("ai_inference_form")) {
        var form = $('#ai_inference_form');
        var conditons = [];
        form.find(".paramDiv").each(function () {
            var vmListParams = new Object();
            vmListParams.vmId = $(this).find("select[name='vm']").val();
            vmListParams.type = $(this).find("input[name='vm_type']").val();
            vmListParams.vmPort = $(this).find("input[name='vm_port']").val();
            conditons.push(vmListParams);
        });
        var jsonStr = "";
        $("#ai_inference_modal").find('.otherVal').each(function () {
			var name = $(this).attr("name");
            var val = $(this).val();
            jsonStr = jsonStr +'"' + name + '": "' + val + '",';
        });
        if(jsonStr != ""){
            jsonStr = "{" + jsonStr.substring(0,jsonStr.length-1) + '}';
		}else{
        	jsonStr = {};
		}



        debugger
        $.ajax({
            url: manageHost + "/ai/inferenceAdd",
            type: "post",
            data:{"vmListJson":JSON.stringify(conditons),"otherParms":jsonStr},
            success:function (res) {
                if(res.code == 2001){
                    window.location = manageHost + "/ai/model/list?msg=添加成功"
                }else{
                    $('#ai_inference_modal').find("label[name='errorLab']").text(res.message);
                }
            }
        });
        debugger;
    }else{
    	debugger;
	}
}