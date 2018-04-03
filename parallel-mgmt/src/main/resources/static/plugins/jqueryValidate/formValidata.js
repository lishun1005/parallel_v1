var checkFormValue = function(formid){
	var form = document.getElementById(formid);;
	for(var i=0;i< form.elements.length;i++){
		var element = form.elements[i];
		if(!checkValue(element)){
			return false; 
		}
	}
	return true;
}
var checkValue = function(element){
	var required = element.required;
	if(required == true){
        var tagName = element.tagName;
        var val = element.value;
        if(val == ""){
            $(element).attr("data-toggle","tooltip");
            $(element).attr("data-placement","top");
            var tipMessage = "";
            if(tagName == "SELECT"){
                tipMessage = "请选择信息";
            }else {
                tipMessage = "请填写信息";
            }
            showPopover($(element),tipMessage);
            return false;
		}else{
            var pattern = element.pattern;
            if(pattern != ""){
                var reg = eval(pattern);
                if(!reg.test(val) ){
                    showPopover($(element),"格式错误");
                    return false;
                }else{
                    return true;
                }
            }else{
                return true;
            }

		}
	}else{
		return true;
	}
}
function showPopover(target, msg) {
	target.attr("data-original-title", msg);
	$('[data-toggle="tooltip"]').tooltip();
	target.tooltip('show');
	target.focus();
	var id = setTimeout( //2秒后消失提示框
		function () {
		  target.attr("data-original-title", "");
		  target.tooltip('hide');
		}, 2000
	);
}
function wordLimit(obj,num){
	var val=$(obj).val();
	var str = new String(val);  
    var bytesCount = 0;
    var tempLenth=0;
    for (var i = 0 ,n = str.length; i < n; i++) {  
        var c = str.charCodeAt(i);  
        //数字
        if ((c >= 0x0001 && c <= 0x007e) || (0xff60<=c && c<=0xff9f)) {  
            bytesCount += 1;
        } else {  
        //中文
            bytesCount += 2;
        }
        if((bytesCount/2).toFixed(0)==20){
        	tempLenth=(bytesCount/2).toFixed(0);
        }
    }
    var length=(bytesCount/2).toFixed(0);
    if(length>num){
        alert("字数不超过"+num);
        $(obj).val("");
    }
}

function checkfile(obj,maxSize){
    try{  
    	var errMsg = "上传的附件文件不能超过"+maxSize+"KB！！！";  
    	var tipMsg = "浏览器暂不支持计算上传文件的大小，确保上传文件不要超过"+maxSize+"KB";  
    	maxSize=maxSize*1024;
	    if(obj.value==""){  
           return;  
        }  
        var filesize = filesize = obj.files[0].size;  
        if(filesize==-1||filesize==undefined){  
            alert(tipMsg);  
            return;  
        }else if(filesize>maxSize){  
            alert(errMsg);
            $(obj).val("");
            return;  
        }
    }catch(e){  
        alert(e);  
    }  
}  
