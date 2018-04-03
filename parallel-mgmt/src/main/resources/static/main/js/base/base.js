	/*
	 * 功能：对话框控制
	 * 
	 * 
	 * */
	$(".dialog-show").dialog({
		open:function(e,ui){
			e.stopPropagation();
			if($(this).hasClass("dialog-warning")){
				$(this).parents(".ui-dialog").addClass("border-red");
			}
		},
		resizable:false,
		autoOpen:false,
		modal:true,
		width:480
	});

	$("*[data-target-dialog]").on("click",function(){
		$("*[data-dialog-name="+$(this).attr("data-target-dialog")+"]").dialog("open");
		$("*[data-dialog-name="+$(this).attr("data-target-dialog")+"]").attr("data-dialog-id",$(this).attr("data-target-id"));
		$("#mainImage").attr("src","");
		if($(this).hasClass("picture")){
			$("#mainImage").attr("src",$(this).attr("path"));
		}
	});
	
	function bytesToSize(bytes) {  
	       if (bytes === 0) return '0 B';  
	        var k = 1024;  
	        sizes = ['B','KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];  
	        i = Math.floor(Math.log(bytes) / Math.log(k));  
	    return (bytes / Math.pow(k, i)) + ' ' + sizes[i];   
	       //toPrecision(3) 后面保留一位小数，如1.0GB                                                                                                                  //return (bytes / Math.pow(k, i)).toPrecision(3) + ' ' + sizes[i];  
	} 
	
	/*$("*[data-target-confirm]").on("click",function(){
		if(confirm("你确定这样做么？")){
			$("*[data-dialog-function="+$(this).attr("data-target-dialog")+"]").dialog("open");
		}
	});*/
	
	$(".dialog-close").on("click",function(){
		$(this).parents(".dialog-show").dialog("close");
	});
	
	//上传文件
	$(".file-upload").change(function(){
		var fileName=$(this).val();
		console.log(fileName);
		$(this).parent().siblings(".file-name").text(fileName).attr("title",fileName);
	});
	
	//左导航栏对应页面样式，两个参数分别对应激活左侧导航栏的第几个栏目和该栏目下的第几个列表
	function changeLeftMenu(channel,num){
		$(".sidebar-menu >li").eq(channel-1).addClass("active").find(".treeview-menu >li").eq(num-1).addClass("active");
	}
	
	//禁止页数跳转输入框输入非数字
	$('.integer').bind({  
        keyup : function() {  
            $(this).val($(this).val().replace(/[^\d]/g, ''));  
        }  
    });  
	//金额格式判断
	$('.rsMoney').bind({
		keyup :function(){
			var val=$(this).val();
			var exp=/^([0-9]+|[0-9]{1,3}(,[0-9]{3})*)(.[0-9]{1,2})?$/;
			var i=0,j;
			if(!exp.test(val)){
				for(i=0;i<val.length;i++){
					var valChar=val[i];
					if(valChar.replace(/[^\d]/g, '')==''&&valChar!='.'){
						break;
					}
				}
				//判断是否存在小数点，若存在，后面就不能再出现小数点
				if(val.indexOf('.')!=-1){
					for(j=val.indexOf('.')+1;j<val.length;j++){
						var valChar=val[j];
						if(valChar=='.'){
							i=j;
							break;
						}
					}
				}
				$(this).val(val.substring(0,i));
			}
		}
	});
	
	
	//禁止页数跳转输入框黏贴非数字
    $('.integer').each(function() {  
        var _input = $(this)[0];  
        if (_input.attachEvent) {  
            _input.attachEvent('onbeforepaste', formatPasteDataToInteger);  
        } else {  
            _input.addEventListener('onbeforepaste', formatPasteDataToInteger, false);  
        }  
    });
    
    function formatPasteDataToInteger() {  
        clipboardData.setData('text', clipboardData.getData('text').replace(/[^\d]/g, ''));  
    } 
    
    function formateRestGtPath(host,gtpath){
    	return host + "/api/v1/gtdata/" + gtpath;
    }
	//调用父窗体的的函数，让iframe自适应高度
	
	//弹出框控制iframe高度 parms dialogIdArr:每个弹出框的id，按“,”分隔
	function dialog_show_hide(dialogIdArr){
			
			var arr=new Array();
			arr=dialogIdArr.split(',');
      		var dialog_before_height;
			for(i=0;i<arr.length;i++){
				//若弹出框的高度大于页面的高度，则按弹出框的高度自适应iframe
				$('#'+arr[i]).on('shown.bs.modal', function () {
		        	dialog_before_height=$("body").height();
		        	var dialog_height=$(this).find(".modal-content").height();
		        	if(dialog_before_height<dialog_height+70){
		  				window.parent.reload_view(dialog_height+70);
		  			}
				});
				//关闭弹出框时，重新设置页面的高度为原来的高度
		        $('#'+arr[i]).on('hidden.bs.modal', function () {
		  			window.parent.reload_view(dialog_before_height);
				});
			};
	}
    String.prototype.trim=function() { return this.replace(/(^\s*)|(\s*$)/g, ""); }
	
    $(function(){
    	//更改url地址
    	//alert(location.href);
    	//判断页面是否在一个iframe中
    	/*if (top != self) {
        	window.parent.set_iframe();
        	//每次加载iframe时获取当前iframe的实际高度
        	window.parent.clickIfreameUrl=window.location;
        	//alert($(window).height()+"--"+$(".wrapper").height());
        	window.parent.reload_view($(".wrapper").height());
    	}*/
	});
    
    //比较时间和当前时间的前后 startdate 2015-7-13 enddate 2015-7-14
    /*return返回结果解释:
     * 一、输入两个参数为情况：-1:startdate早于enddate,
     * 					0:startdate等于enddate,
     * 					1:startdate晚于enddate
     * 二、只输入一个参数则识别为startdate,
     * 				-1:startdate早于当前时间,
     * 				1：startdate晚于当前时间,
     * 				0:startdate等于当前时间。
     */
    function CompareDateBeforeAndAfter(startdate,enddate){//newdate格式为1990/3/11
    	var enddates,starttimes;
    	startdate = startdate.replace(/-/g,"/");
    	if(enddate){
    		enddate = enddate.replace(/-/g,"/");
    		enddates = new Date(enddate).getTime();
    	}else{
    		enddates = new Date().getTime();
    	}
        starttimes = new Date(startdate).getTime();
        if (starttimes < enddates) {
            return -1;//开始时间早于结束时间
        }else if(starttimes == enddates){
        	return 0;
        }else{
        	return 1;
        }
    }
    
  //日期转换格式转换----函数
    function formatDate(date,format)
    {
    	  var o = {
    	    "M+" : date.getMonth()+1, //month
    	    "d+" : date.getDate(),    //day
    	    "h+" : date.getHours(),   //hour
    	    "m+" : date.getMinutes(), //minute
    	    "s+" : date.getSeconds(), //second
    	    "q+" : Math.floor((date.getMonth()+3)/3),  //quarter
    	    "S" : date.getMilliseconds() //millisecond
    	  }
    	  if(/(y+)/.test(format)) format=format.replace(RegExp.$1,(date.getFullYear()+"").substr(4 - RegExp.$1.length));
    	  for(var k in o) if(new RegExp("("+ k +")").test(format))
    	      format = format.replace(RegExp.$1,
    	      RegExp.$1.length==1 ? o[k] :("00"+ o[k]).substr((""+ o[k]).length));
    	  return format;
    }

    /**
    *
    * @param phone
    *            检查手机号码格式
    * @returns {Boolean} 如果正确返回true,不正确返回false
    */
   function checkPhone(phone) {
    var yidong = /^[1]{1}(([3]{1}[4-9]{1})|([5]{1}[012789]{1})|([8]{1}[2378]{1})|([4]{1}[7]{1}))[0-9]{8}$/;
    var liantong = /^[1]{1}(([3]{1}[0-2]{1})|([5]{1}[56]{1})|([8]{1}[56]{1}))[0-9]{8}$/;
    var dianxin = /^[1]{1}(([3]{1}[3]{1})|([5]{1}[3]{1})|([8]{1}[019]{1}))[0-9]{8}$/;
    if (!phone.match(yidong) && !phone.match(liantong) && !phone.match(dianxin)) {
     return false;
    } else {
     return true;
    }
   }
   /**
    * description:字符串长度限制 by Lishun
    * @param e:当前文本对象
    * @param msg 提示信息
    * @param savaLeng 限制长度
    */
   function  limitChar(e,msg,savaLeng){
		 var val=$(e).val();
       if(val.length>savaLeng){
           alert(msg)
           $(e).val($(e).val().substring(0,savaLeng));
       }
    }
   /*表单提交*/
   function complaintFormPage(page_no) {	
		var totalPage = $('#totalPage').val();
		if(totalPage != null){
			if(page_no >  parseInt(totalPage)){
				page_no = totalPage;
			}
		}
		$("#pageNo").val(page_no);
		$("#pageForm").submit();
	}
	function complaintFormGoPage() {
		complaintFormPage($("#goPage").val());
	}

