
$(function(){
	//切片 查看详情
	$(document).on("click",".btn-watch",function(){
		var obj    =$(this).closest(".trId"),
		 	  objId = obj.attr("id"),
		 	  html  ="";					
		$("#msgModals").modal("show");
		Ajax(html,objId);
	});
	
	//生产线 查看详情
	$(document).on("click",".btn-watch2",function(){
		var obj    =$(this).closest(".trId"),
	 	  objId = obj.attr("id"),
	 	  html  ="";
		$("#msgModals").modal("show");
		Ajax2(html,objId);
	})
	
	//云盘提交
	$(document).on("click",".confirm-delete-btn",function(){		
		$("#yunpan").modal("hide");		
	})
	
	/**
	 * 修改优先级
	 */
	$(document).on("click","#tab1 .btn-update",function(){
		var obj =$(this).closest(".trId"),
			id = obj.attr("id");				
		var form = $("#cutUpdateForm");
		form.find("input[name=id]").val(id);
		$("#cutUpdateModals").modal("show");
	})
	//切片重启按钮
	$(document).on("click","#tab1 .btn-reset",function(){
		var obj    =$(this).closest(".trId"),
			  status = obj.find(".statusTd").text(),
			  val    = obj.find(".statusType").val(),			  
		 	  objId = obj.attr("id");				
		jobId =obj.find("td:eq(1)").text();
		$("#resetModal").modal("show");
		var mapName = obj.find("td:eq(2)").text();
		//0新增，1更新
		if(status == "FAILED"){
			if(val == "0"){
				cutTypeChange("0")
			}else{	
				cutTypeChange("1")
			}
		}else{				
			cutTypeChange("1");
		}
		var form=$("#cuttingForm");
		form.find("input[name=mapName]").val(mapName);
		form.find("input[name=id]").val(objId)
	})
	
	//生产线重启按钮
	$(document).on("click","#tab2 .btn-reset",function(){
		jobId = $(this).closest(".trId").find("td:eq(1)").text();
		$("#xiangqian").modal("show");
		
	})
	
	//切片日志按钮
	$(document).on("click","#tab1 .btn-log",function(){
		var id = $(this).closest(".trId").attr("id"),
			   url =manageHost+"/production/cutJobLog/"+id;	
		$("#logModals").modal("show");
		logDetail(url);
	})
	
	//切片删除按钮
	$(document).on("click","#tab1 .btn-del",function(){
		var obj =$(this).closest("tr");
		var id = $(this).closest(".trId").attr("id"),
			 url =manageHost+"/production/cutJobDel/"+id,
			 url2 =manageHost+"/production/jobMonitoring?&msg=删除成功";
		$("#delModals").modal("show");
		delParams.url =url
		delParams.obj =obj;
		delParams.url2 =url2;	
		
	})
	
	
	//生产线日志按钮
	$(document).on("click","#tab2 .btn-log",function(){
		var id = $(this).closest(".trId").attr("id"),
			 url =manageHost+"/production/mosaicLog/"+id;
		$("#logModals").modal("show");
		logDetail(url);
	})
	
	//生产线删除按钮
	$(document).on("click","#tab2 .btn-del",function(){
		var se  = sessionStorage.getItem("activeTab");
		var obj =$(this).closest("tr");		
		var id = $(this).closest(".trId").attr("id"),
			 url =manageHost+"/production/mosaicDel/"+id,
			 url2 =manageHost+"/production/jobMonitoring?pageType=mosaic&algorithmType="+se+"&msg=删除成功";
		$("#delModals").modal("show");
		delParams.url =url
		delParams.obj =obj;
		delParams.url2 =url2;		
	})
	
	//删除提交
	$(document).on("click","#delModals .btn-submit",function(){		
		$("#delModals").modal("hide");
		delData(delParams.url,delParams.obj,delParams.url2);		
	})
	
	//检索
	$(document).on("click",".btn-searchs",function(){
		var active = $(".tab-ul .active").attr("attrs");	
		$("#algorithmType").val(active);
		$("#pageForm").submit();			
	})
	
		//弹窗选择文件
	$(document).on("click",".outfile",function(){		
		$("#yunpan").modal({
			show:true,
			backdrop:false,						
		});
		$("#yunpan").css("z-index","10000");
		queryOnlyDirectory("2");
		//$(".outPath:visible").val($("#pathFile").val());
	});
	
	
	//模型切换请求
	$('.tab-ul a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
        // 获取已激活的标签页的名称
		$(".input-sm:visible").val("");
        var activeTab = $(e.target).parent("li").attr("attrs");        
        sessionStorage.setItem("activeTab",activeTab);
		$("#algorithmType").val(activeTab);
        $("#pageForm").submit();	

    });
	
	//tab请求
	$(document).on("click",".tab-nav li",function(){
		var _this = $(this).attr("attrs");
		$("#pageType").val(_this);
		$("#algorithmType").val("pl");
		$(".errorinpath").text("");
		$(".disinpath").val("");	
		$("#pageForm").submit();		
	})
	
	$("#mosaicForm .outPath").change(function(){
		var self =$(this),
			  path = self.val();
		$.getJSON(manageHost+"/userStorage/existPath",{path:path},function(data) {
			if(data == false){
				self.parent().parent().find(".errorinpath").empty().text("路径不存在!");
			}else{
				self.parent().parent().find(".errorinpath").text("");
			}
		})
	})
	

	
})


/**
 * 删除数据
 * 
 * .*/
var delParams ={	
		url:null,
		obj:null,
		url2:null	
}
function delData(url,obj,url2){
	
	$.getJSON(url,null,function(result){
/*		obj.remove();*/
		location.href = url2;
	})

}
/**
切片更新优先级
**/
function cutUpdateSubmit(){
	var  regNum =/^[1-9]\d*|0$/;	   
	var re = new RegExp(regNum);
	var form=$("#cutUpdateForm");
	var priority = form.find("input[name=priority]:visible").val();
	var id = form.find("input[name=id]").val();
	if("" == priority){
		alert("请填写优先级");
		form.find("input[name=priority]").focus();
		return;
	}
	if(re.test(priority) == false){		
		alert("优先级为正数字");
		form.find("input[name=priority]").focus();
		return;
	}
	var parmas = {
			"id":id,
			"priority":priority	
		}
	$("#waiting").show();
	$.ajax({
		url:manageHost + "/production/cutJobUpdate",
		type:"post",
		data:parmas,
		success:function(res){
			if(res.code==2001){
				$("#resetModal").modal("hide");
				$("#waiting").hide();
				location.href=manageHost+"/production/jobMonitoring?pageType=cut&msg=更新成功";
			}else{
				$("#waiting").hide();
				$(".upadateErrorMsg").text(res.errorMessage);
			}
		}
	})
}
/**
 * 日志详情
 * */

function logDetail(url){	
	$("#waiting").show();
	$.getJSON(url,null,function(result){
		var html='',len=result.length;
		for(var i=0;i<len;i++){			
			html +='<p style="margin-top: 10px;">jobid：'+result[i].jobid+'</p>';
			html +='<p>日志：'+result[i].log+'</p>';
			html +='<p>时间：'+timeFormat(result[i].accept_time)+'</p>';
			html +='<div class="underline"></div>';
		}
		$("#logModals .modal-body").html(html);
		$("#waiting").hide();
	})
}
/**
 * 切片重启接口
 * */
var jobId;
function cuttingSubmit(){

	var form=$("#cuttingForm");
	var cutType=form.find("input[name=cutType]").val();
	var mapName=form.find("input[name=mapName]:visible").val();
	var minLayers=form.find("input[name=minLayers]:visible").val();
	var maxLayers=form.find("input[name=maxLayers]:visible").val();
	var outPath=form.find("input[name=outPath]:visible").val();
	var usage=form.find("input[name=usage]:visible").val();
	var inPath=form.find("input[name=inPath]").val();
	var id = form.find("input[name=id]").val();
	var isCover=form.find("select[name=isCover]:visible").val();
	var waterMark=form.find("select[name=waterMark]:visible").val();
	
	var realTime=form.find("input[name=realTime]:visible").val();
	
	
	var  regNum =/^[1-9]\d*|0$/;	   
	var re = new RegExp(regNum);

	if(mapName == "" ){
		alert("请填写图层名!");
		form.find("input[name=mapName]").focus();
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
	}else if(minLayers =="") {
		alert("请填写最小层级");
		form.find("input[name=maxLayers]").focus();
		return;
	}else if(re.test(maxLayers) == false){
		alert("最大层级不能为负");
		form.find("input[name=maxLayers]").focus();
		return;
	}else if(parseFloat(minLayers) > parseFloat(maxLayers)){
		alert("最小层级不能大于最大层级");
		form.find("input[name=minLayers]:visible").val("");
		return;
	}
	//
	var parmas ={
		//"cutType":cutType,
		"mapName":mapName,
		"minLayers":minLayers,
		"isCover":isCover,
		"maxLayers":maxLayers,
		"outPath":outPath,
		"usage":usage,
		"isRestart":"1",	
		"id":id,
		/*"inPath":file,*/
		"waterMark":waterMark		
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
        urlAddr = manageHost + "/dataCenter/cutRestartAdd";
    }else{
        urlAddr = manageHost + "/dataCenter/cutRestartUpt";
    }

    $.ajax({
        url: urlAddr,
        type:"post",
        contentType:"application/json",
        data:JSON.stringify(parmas),
        success:function(res){
            if(res.code==2001){
                $("#resetModal").modal("hide");
                $("#waiting").hide();
                location.href = manageHost+"/production/jobMonitoring?pageType=cut&msg=提交任务成功";
            }else{
                $("#waiting").hide();
                $(".errorMsg").text(res.message);
            }
        },
        error: function(XMLHttpRequest, textStatus, errorThrown) {
            $("#waiting").hide();
            $(".errorMsg").text(XMLHttpRequest.responseJSON.message);
        }
    })

}
/**
 * 重启类型切换
 * params:val=0:新增,1:更新
 * */
function cutTypeChange(val){
	var form=$("#cuttingForm");	
	if(val==0){//新增，不需要填写最小层级
        form.find("input[name=cutType]").val("0");
		form.find("input[name=cutTypeStr]").val("新增");
		form.find("input[name=minLayers]").closest(".form-group").css("display","none");
		form.find("input[name=realTime]").closest(".form-group").addClass('hide');
		form.find("select[name=waterMark]").css("display","block").closest(".form-group").css("display","block");
		form.find("input[name=usage]").css("display","block").closest(".form-group").css("display","block");
		form.find("input[name=outPath]").css("display","block").closest(".form-group").css("display","block");
		
	}else{//更新 不需要填写用途，水印方案，输出路径
        form.find("input[name=cutType]").val("1");
		form.find("input[name=cutTypeStr]").val("更新");
		form.find("input[name=minLayers]").closest(".form-group").css("display","block");
		form.find("input[name=realTime]").closest(".form-group").removeClass('hide');
		form.find("select[name=waterMark]").css("display","none").closest(".form-group").css("display","none");
		form.find("input[name=usage]").css("display","none").closest(".form-group").css("display","none");
		form.find("input[name=outPath]").css("display","none").closest(".form-group").css("display","none");
	}
}


/**
 * 镶嵌重启接口
 * 
 * */
function mosaicSubmit(){

	var form=$("#mosaicForm2");

	var outPath=form.find("input[name=outPath]:visible").val(),//输出路径
		 project =form.find("#project:visible option:selected").val(),//投影
		 outBand=form.find("#outBand:visible option:selected").text(),	//	输出波段字节
		 outImage=form.find("#outImage:visible option:selected").text(),//输出影像字节
		 jobName  =$("#tab2 #firstname").val(),
		 filesType  =$("#fileTypes").val(),
		 inPath  = $(".inpath:visible").val();


	if(outPath ==""){
			alert("请选择输出路径");			
			return;
	}else if(inPath ==""){
		alert("请选择数据来源");
		$("#xiangqian").modal("hide");
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
		"project":project,
		"outBand":outBand,
		"outImage":outImage,
		"jobid":jobId,
		"isRestart":"1"
	}
	$("#waiting").show();
	$.ajax({
		url:manageHost+"/mosaic/pl",
		type:"post",
		data:parmas,
		success:function(res){
			if(res.code==2001){
				$("#waiting").hide();
				$("#xiangqian").modal("hide");
				location.href = manageHost+"/production/jobMonitoring?pageType=mosaic&msg=提交成功";
			}else{
				$("#waiting").hide();
				$(".errorMsg").text(res.message);
			}
		},
		error:function(xhr,status,error)	{
			if(xhr.status == "401"){				
				$("#waiting").hide();
				$(".errorMsg").text("没有权限！");
			}
		}
	})
}

/**
 * 切片查看详情
 * params:objId 当前dom
 * 
 * */
function Ajax(html,objId){
	$("#waiting").show();
	var url =manageHost+"/production/oneMapCutJobDetail/"+objId;
	$.getJSON(url,null,function(data) {
			
		html +="<p>操作人："+data.operationname+"</p>";
		
		if(data.geowebcache_url == undefined){
			html +="<p>底图服务地址：</p>";								
		}else{
			html +="<p>底图服务地址：<a  target='_blank'  href='"+data.geowebcache_url+"'>"+data.geowebcache_url+"</a></p>";
		}
		html +="<p>最小层级："+data.min_layers+"</p>";
		html +="<p>最大层级："+data.max_layers+"</p>";
		html +="<p>图层名："+data.map_name+"</p>";
		html +="<p>状态："+data.status+"</p>";
		html +="<p>输入路径："+data.in_path+"</p>";
		html +="<div style='width: auto;display:block;word-break: break-all;word-wrap: break-word;font-size: 16px'>输出路径："+data.out_path+"</div>";
		html +="<p></p>";
		html +="<p>日志："+data.log+"</p>";
		html +="<p>jobID："+data.jobid+"</p>";
		if(data.water_mark == "-1"){
			html +="<p>水印方案：无水印 </p>";
		}else if(data.water_mark == "0"){
			html +="<p>水印：全部有水印</p>";
		}else if(data.water_mark == "1"){
			html +="<p>水印：行列号和为奇数有水印</p>";
		}else{
			html +="<p>水印：行列号和为偶数有水印</p>";
		}
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

		$("#msgModals .modal-body").html(html);
		$("#waiting").hide();
	})
	
}

/**
 * 生产线查看详情
 * params:objId 当前dom
 * 
 * */
//mosaic/process/restart
function Ajax2(html,objId){
	var url =manageHost+"/production/mosaicDetail/"+objId;
	$("#waiting").show();
	$.getJSON(url,null,function(data) {
			
			html +="<p>操作人："+data.detail.operationname+"</p>";
			html +="<p>模型名称："+data.detail.algorithm_type+"</p>";
			html +="<p>是否重启："+data.detail.is_restart+"</p>";			
			html +="<p class='out_band'>输出波段："+data.detail.out_band+"</p>";
			html +="<p class='out_image'>输出影像字节："+data.detail.out_image+"</p>";
			html +="<p>状态："+data.detail.status+"</p>";
			html +="<p>输出路径："+data.detail.out_path+"</p>";
			html +="<p>输入路径："+data.detail.in_path+"</p>";		
			if(data.detail.log != undefined){				
				html +="<p class='out_log'>日志："+data.detail.log+"</p>";
			}else{
				html +="<p class='out_log'>日志：</p>";
			}
			html +="<p>jobID："+data.detail.jobid+"</p>";
			if(data.detail.project =="4326"){
				html +="<p class='out_project'>投影：WGS84</p>";
			}else{				
				html +="<p class='out_project'>投影：Web墨卡托</p>";			
			}
			html +="<p>拥有人："+data.detail.ownername+"</p>";

			if(data.detail.accept_time == undefined){
				html +="<p>接收时间：</p>";	
			}else{								
				html +="<p>接收时间："+timeFormat(data.detail.accept_time)+"</p>";		
			}
			html +='<p>工作流程：</p>';
			var html2='';
			
			var lenList = data.sub_list.length;
			if(lenList == 0){
				$("#msgModals .modal-body").html(html);
				$("#waiting").hide();
				return  $(".out_band,.out_image,.out_project").remove();
			}
			var lenLast = data.sub_list.length-1;			
			for(var i=0;i<lenList;i++){
				html2 +='<div class="round"><span>'+data.sub_list[i].status+'<br/>'+data.sub_list[i].progress+'%</span></div>';
								
				if( i == lenLast){
					html +='<div class="liucheng">'+html2+"</div><div class='clearfix'></div>";					
					$("#msgModals .modal-body").html(html);
					return;
				}
				html2+='<div class="pull-left line80">→</div>';
				$("#waiting").hide();
			}
			
			

	})
	
}

/**
 * 验证时间格式 yyyy/mm/dd
 * params:time(string)
 * */
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

function check(str){
	//var a = /((?:19|20)\d{2}\/(?:0[1-9]|1[0-2])\/(?:0[1-9]|[12][0-9]|3[01]))/;
	var a  =/((^((1[8-9]\d{2})|([2-9]\d{3}))([-\/\._])(10|12|0?[13578])([-\/\._])(3[01]|[12][0-9]|0?[1-9])$)|(^((1[8-9]\d{2})|([2-9]\d{3}))([-\/\._])(11|0?[469])([-\/\._])(30|[12][0-9]|0?[1-9])$)|(^((1[8-9]\d{2})|([2-9]\d{3}))([-\/\._])(0?2)([-\/\._])(2[0-8]|1[0-9]|0?[1-9])$)|(^([2468][048]00)([-\/\._])(0?2)([-\/\._])(29)$)|(^([3579][26]00)([-\/\._])(0?2)([-\/\._])(29)$)|(^([1][89][0][48])([-\/\._])(0?2)([-\/\._])(29)$)|(^([2-9][0-9][0][48])([-\/\._])(0?2)([-\/\._])(29)$)|(^([1][89][2468][048])([-\/\._])(0?2)([-\/\._])(29)$)|(^([2-9][0-9][2468][048])([-\/\._])(0?2)([-\/\._])(29)$)|(^([1][89][13579][26])([-\/\._])(0?2)([-\/\._])(29)$)|(^([2-9][0-9][13579][26])([-\/\._])(0?2)([-\/\._])(29)$))/;
	if (!a.test(str)) { 
		alert("日期格式不正确!") 		
		return false;
	}
} 
/**
 * 转换时间
 * params:timestamp(string)
 * */
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

//点击选中某个文件夹的时候设置值
function selectTheNode(path,pPath,name)
{		
	$(".outPath").val("");
	$(".outPath:visible").val(path);
	$("#pathFile").val(path);
	sessionStorage.setItem("filePath",path)
}


/**
 * 云盘
 * */
function queryOnlyDirectory(strObj){
	// 树的设置参数
	var treeSetting = {
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
					sessionStorage.setItem("treeNode2",treeNode);
				}
			}
	};
	var r = Math.floor(Math.random() * 9999 + 1);
	if(strObj =="2"){		
		var url = manageHost+"/userStorage/queryDirectory";//文件类型	
	}else{
		var url = manageHost+"/userStorage/queryDirectory?showFile=true";//文件夹
	}
	var params = {r:r};
	$("#waiting").show();;
	
	$.getJSON(url,params,function(data) {
		var str=new Array;
		var zNodes = data.list;
		for(var i in zNodes){
			if(strObj =="2"){	
				str[i]=zNodes[i];				
			}else{				
				var r = {};
				r.id =zNodes[i].path;
				r.name=zNodes[i].filename;
				r.click ="selectTheNode('"+zNodes[i].path+"','','"+zNodes[i].filename+"')";
				str.push(r)
			}
		}

    	$.fn.zTree.init($("#treeDemo"), treeSetting, str);
  	    var treeObj = $.fn.zTree.getZTreeObj("treeDemo");
  	    $("#waiting").hide();
  	    
  	    var node = sessionStorage.getItem("treeNode2");
		if(node !=""){		
			node =JSON.parse(node);
			$("#"+node.tId+"_a").click();
		}
  	    
	});

}