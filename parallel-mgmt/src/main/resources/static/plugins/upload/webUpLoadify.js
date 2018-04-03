var isPickCreate = false;
var uploader;
var state = 'pending';
var filePathTemp=null;
var oldFilePathTemp=null;
var shpFileCacheID;
var fileTypeArray = new Array(["SHP","DBF","SHX","PRJ","ZIP"]);
//限制上传文件大小（默认100KB）
var fileSize=100*1024;


if (!Array.prototype.indexOf)
{
  Array.prototype.indexOf = function(elt /*, from*/)
  {
    var len = this.length >>> 0;
    var from = Number(arguments[1]) || 0;
    from = (from < 0)
         ? Math.ceil(from)
         : Math.floor(from);
    if (from < 0)
      from += len;
    for (; from < len; from++)
    {
      if (from in this &&
          this[from] === elt)
        return from;
    }
    return -1;
  };
}

function createBtn(){

    uploader.addButton({
     	id: '#picker',
     	innerHTML: '+选择本地文件'
    });    
}

function createPick() {
	if(isLastPathname("customMosaicIndex")){
		fileSize=2*1024*1024;
	}
	//if (!isPickCreate) {
	if (!isPickCreate) {
		//filePathTemp = dateByFormate(null,"yyyyMMdd")+"/"+createUuid();
		filePathTemp = createUuid();
		uploader = WebUploader.create({
			// swf文件路径
			swf : 'webuploader/Uploader.swf',
			sendAsBinary:true,
			// 文件接收服务端。
			server : manageHost+'/uploadShpFile',
			//文件上传请求的参数表，每次发送都会发送此对象中的参数
			formData:{"filePathTemp":filePathTemp},

			//设置是否主动上传
			auto: true,
			//是否已二进制的流的方式发送文件，这样整个上传内容
			sendAsBinary:false,
			//限制文件上传个数
			fileNumLimit:6,
			//限制上传文件大小
			fileSingleSizeLimit:fileSize,
			//开启分片上传
			chunked: false,
			//上传并发数
			threads: 1,
			// 内部根据当前运行是创建，可能是input元素，也可能是flash.
			pick : '#picker',
			// 不压缩image, 默认如果是jpeg，文件上传前会压缩一把再上传！
			resize : false,
			duplicate :true,
			accept: {
			    extensions: 'zip,dbf,shp,shx,prj,sbn,sbx'
			}
		});
		isPickCreate = true;
	}
	//$("#picker").css("padding","0")

	uploader.on('beforeFileQueued', function(file) {
		oldFilePathTemp = filePathTemp;			
		$(".mpbox3").addClass("webuploader-element-invisible");		
		$(".mpbox5").css("display", "block");
		//$("#buttonWaiting").show();
	});	
	
	uploader.on('fileQueued', function(file) {
		
		
		//$(".boxcontent").html("");
		//$(".boxcontent").append("<p>"+file.name+"</p>");
		
		var isFileType=false;
		var file_lastName=file.name.substring(file.name.lastIndexOf(".")+1,file.name.length) 
		for (var f1 in fileTypeArray) {				
			if ($.inArray(file_lastName.toUpperCase(),fileTypeArray[f1]) > -1) {
				isFileType=true;
			}
		}
		
		if($(".boxcontent").html().indexOf(file.name)<1 && isFileType){
			$(".boxcontent").append("<p>"+file.name+"</p>");
		}
	});
	
	uploader.on('uploadSuccess', function(file,responseData) {
		shpFileCacheID=responseData.cacheId;//shp文件缓存ID，账号服务功能用到
		
	});
	
	uploader.on('error', function(type) {
		if (type==='Q_TYPE_DENIED'){
			$("#dialogTitle").html("");
       		$("#dialogTitle").html("<h3 class=\"cwtip\" style=\"color:#ff7e6f\">文件类型错误，请重新上传!</h3>")
		}else if(type==='F_EXCEED_SIZE'){
			$("#dialogTitle").html("");
			if(isLastPathname("customMosaicIndex")){
				$("#dialogTitle").html("<h3 class=\"cwtip\" style=\"color:#ff7e6f\">单个文件大小不能超过2MB!</h3>")
			}else{
				$("#dialogTitle").html("<h3 class=\"cwtip\" style=\"color:#ff7e6f\">单个文件大小不能超过100KB!</h3>")
			}
		}
		uploader.reset();
		$(".boxcontent").html("");
		$(".mpbox5").css("display","none");		
		$(".mpbox3").removeClass("webuploader-element-invisible");
	});
	
	uploader.on('uploadFinished', function(file) {
		$("#buttonWaiting").hide();
		//filePathTemp =dateByFormate(null,"yyyyMMdd")+"/"+createUuid();
		filePathTemp = createUuid();
		uploader.option("formData",{"filePathTemp":filePathTemp});
	});

	
	uploader.on('all', function(type) {
		if (type === 'startUpload') {
			state = 'uploading';
		} else if (type === 'stopUpload') {
			state = 'paused';
		} else if (type === 'uploadFinished') {
			state = 'done';
		}

	});	
	
}