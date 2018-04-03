var table;
var currentPath ="";
var sorts = "time_down";
var datatable;


jQuery.extend( jQuery.fn.dataTableExt.oSort, { 
	"cfl-pre" : function (str) { 
			if(str =="-"){
				str = 0;
			}else{			
				//改判断构造了字符串带s_和_s进行截取，文件名排序
				if(str.indexOf("s_") != -1){
					var splitStr = str.split("s_");				
					var str0 =splitStr[1][0];
					var str1;
				}
				
				
				if(str0 =="-"){					
					str = 0;
				}else{
					if(str.indexOf("s_") != -1){
						str1 = splitStr[1].split("_s");															
						str1 = str1[0].substr(0,str1[0].length-1);
						str = str1;
					}					
				}
			}	
			
			x = String(str).replace(/\d+(\.\d+)*/, ""); //替换所有数字符并转为number类型 
			z = Number(String(str).replace(/(K|M|G)/, "")); 
	
			var multiplier = 1;
			if ( x === '-' ) {
				multiplier =0;
			}else if ( x === 'K' ) { 
				multiplier = 1024;				
			}else if ( x === 'M' ) { 
				multiplier = 1048576; 
			} else if ( x === 'G' ) { 
				multiplier = 1073741824; 
			} 
			z = z * multiplier; 
			return z; 
	}, 
	"cfl-asc" : function (s1, s2) { 
			return (s1 < s2) ? -1 : ((s1 > s2) ? 1 : 0); 
	}, 
	"cfl-desc" : function (s1, s2) { 
			return ((s1 < s2) ? 1 : ((s1 > s2) ? -1 : 0)); 
	}	
})
	
$(function () {		
	
	$('.date').datepicker({
	    format: "yyyy/mm/dd",        
	    clearBtn: true,
	    todayBtn: "linked",
	    language: "zh-CN",
	    orientation: "bottom left",
	    keyboardNavigation: false,
	    calendarWeeks: true, 
	    autoclose: true,
	    todayHighlight: true
	});
	
	//初始化
	//addTable(currentPath,1);
	queryUserFilesSize();
	datatable=$('#content-table')
	.DataTable({
		searching:false,
		//iDisplayLength:10,//默认每页数量
		bPaginate:true,
		lengthChange:false,
		bSort : true,
		bProcessing : true,  
		oLanguage: {  
			"oAria": {
                "sSortAscending": " - click/return to sort ascending",
                "sSortDescending": " - click/return to sort descending"
            },
            "sLengthMenu": "显示 _MENU_ 记录",
            "sZeroRecords": "对不起，查询不到任何相关数据",
            "sEmptyTable": "未有相关数据",
            "sLoadingRecords": "正在加载数据-请等待...",
            "sInfo": "当前显示 _START_ 到 _END_ 条，共 _TOTAL_ 条记录。",
            "sInfoEmpty": "当前显示0到0条，共0条记录",
            "sInfoFiltered": "（数据库中共为 _MAX_ 条记录）",
            "oPaginate": {
                "sFirst": "首页",
                "sPrevious": " 上一页 ",
                "sNext": " 下一页 ",
                "sLast": " 尾页 "
            },
            "sProcessing": "正在加载数据..."            
        },
        ajax:{
        		"url": manageHost+'/userStorage/AllFile',
                "type":"POST",
                "data":{
                	currentPath:currentPath,
                	sorts:sorts
                },
                "dataSrc": "list",                
        },
        aoColumns:[
            {"data":"", "sType": "cfl", "aTargets": [0]},
            {"data":"", "sType": "cfl", "aTargets": [1]},
            {"data":"time"}            
        ],
        columnDefs:[
            {
	              targets:0,
	              render:function (data,type,row) {  
	            	            	
		            	var   file ="<span class='fa fa-file'></span>",
		     		   			fileText ="<span class='fa fa-file-o'></span>",            	
		            			fileHtml ="<span class='filename' size=s_"+changeFileSize(row.size)+"_s filePath='"+row.path+"'>"+row.filename+"</span>";
		            			filedit    ="<div  class='pull-left ml20' id='file-edit'><a href='javascript:void(0)' class='changeNameBtn' data-name='"+ row.filename +"'>重命名</a>"+
									     		"<a href='javascript:void(0)' class='removeBtn' filename='"+row.filename+"'>移动</a>"+
									     		"<a href='javascript:void(0)' class='deleteBtn' filename='"+row.filename+"'>删除</a>";
			     		//注意：暂时屏蔽切片
			     		/*if(row.filename.indexOf(".tif") !== -1){
			     			return   "<label class='pull-left'><input type='checkbox' class='select-single' filename='"+row.filename+"' filePath='"+row.path+"'/></label>" +
				     					 "<div class='pull-left ml10'>"+file+fileHtml+"<a href='javascript:void(0)' " +
		     							 "onclick='cuttingModalShow(\""+row.path+"\")' class='qieBtn' >切片</a></div>"+filedit+"</div>";
			     		}else{
			     			return   "<label class='pull-left'><input type='checkbox' class='select-single' filename='"+row.filename+"' filePath='"+row.path+"'/></label>" +
			     						 "<div class='pull-left ml10'>"+fileText+fileHtml+"</div>"+filedit+"</div>";
			     					
			     		}*/
            			return   "<label class='pull-left'><input type='checkbox' class='select-single' filename='"+row.filename+"' filePath='"+row.path+"'/></label>" +
 						 "<div class='pull-left ml10'>"+fileText+fileHtml+"</div>"+filedit+"</div>";
	             
	              }
            },
            {
                targets:1,
                render:function (data,type,row) {
                	return changeFileSize(row.size)
                }  
            },
        ],
/*       drawCallback: function( settings ) {
            var api = this.api();
        },*/
        drawCallback: function(s,json) {
            //$(row).find("td").eq(0).html(rowIndex+1)

        	var path = s.ajax.data.currentPath;
			if(path == ""){
				$(".current-path").html("当前路径:<a class='myFile'>我的文件</a>")
			}else{					
				path = path.replace("/","");
				var dirData = path.split("/");
				var html ="当前路径:<a class='myFile'>我的文件</a>";
				var dataHtml ='';
				var str ="/";
				for(var i=0; i < dirData.length; i++){										
					
					//dataHtml +=' > <a onclick=\'addTable("'+str+dirData[i]+'")\'>'+dirData[i]+'</a>';
					dataHtml +=' > <a class="myPath" path="'+str+dirData[i]+'">'+dirData[i]+'</a>';
					str += dirData[i]+"/";
				}
				$(".current-path").html(html+dataHtml);
			}
			
        }
    });
		
	//路径面包屑点击
	$(document).on("click",".myFile",function(){		
		var param ={
				currentPath:"",
				sorts:sorts
		}
		datatable.settings()[0].ajax.data = param;
		datatable.ajax.reload();
	})
	
	$(document).on("click",".myPath",function(){
		var path = $(this).attr("path");
		var param ={
				currentPath:path,
				sorts:sorts
		}
		datatable.settings()[0].ajax.data = param;
		datatable.ajax.reload();
	})
	
	//点击文件夹
	$(document).on("click",".filename ",function(){
		currentPath =$(this).attr("filepath");	
		if(currentPath.indexOf(".") != -1){
			return false;
		}		
		//addTable(currentPath,1);	
		var param ={
				currentPath:currentPath,
				sorts:sorts
		}
		datatable.settings()[0].ajax.data = param;
		datatable.ajax.reload();
		
	})
	
	
	
     //新建文件夹
	$(document).on("click",".addFile-btn",function(){
		createNewDirectory();
		$("#addNewFlieModal").modal("hide");
	})
	
	   //重命名弹窗
	 var clickName;
	   $(document).on("click",".changeNameBtn",function(){
		   clickName = $(this).closest("#content-table tr td");
	       $("#file-name").attr("placeholder",$(this).attr("data-name"));	       
	       $("#changeNameModal").modal("show");
	   })
	   
	   //改文件名字	   
	   $(document).on("click",".update-btn",function(){
		   var name = clickName.find(".filename");		   
		   updateName(name);
	   })
	   
	
	   //删除
	   $(document).on("click",".deleteBtn",function(){
		   clickName = $(this).closest("#content-table tr");		
		   var selectFileName = {};
			selectFileName[0] = $(this).attr("filename");
			selectFileName = JSON.stringify(selectFileName);
			$("#deleteFileName").val(selectFileName);
	       $("#deleteModal").modal("show");	       
	   })
	
	   //确定删除
	   $(document).on("click",".confirm-delete-btn",function(){
	       /*var selectFileName = clickName.find(".filename").text();*/
	       deleteDirectoryOrFile();
	   })

	   //移动弹窗
	   $(document).on("click",".removeBtn",function(){
		   $("#removeFileModal").modal("show");
		   var selectFileName = {};
		   selectFileName[0] = $(this).attr("filename");
		   selectFileName = JSON.stringify(selectFileName);
		   $("#moveFileName").val(selectFileName);
		   queryOnlyDirectory();
	   })
	   
	   $(document).on("click",".removeFile-btn",function(){	    	
	    	sureMoveFileOrDir();
	   })
	   //选项框
	   $(document).on("click",".select-single",function(){
		   var tr = $(this).closest("tr");
		   var obj = $(this);
		   if(tr.hasClass("active")){
			   obj.removeClass("active");
			   tr.removeClass("active");
			   tr.find(".fa").removeClass("active");
		   }else{
			   obj.addClass("active");
			   tr.addClass("active");
			   tr.find(".fa").addClass("active");
		   }	
		      		   
		   checkbox();
	   })
	   
	   $(document).on("click",".checklist",function(){
		   var select = $(".select-single");
		   if(this.checked){   
		        $(".select-single").prop("checked", true);
		        select.addClass("active");        
		        select.closest("tr").addClass("active");
		        select.closest("tr").find(".fa").addClass("active");
		    }else{   
		    	$(".select-single").prop("checked", false);
		    	select.removeClass("active");        
		    	select.closest("tr").removeClass("active");
		        select.closest("tr").find(".fa").removeClass("active");
		    }   
		   checkbox();
	   })
})





//checkbox操作
function checkbox(){
	
	var trlen = $("#addBody tr.active").length;
	var folderLen =$(".fa-file-o.active").length;
	var fileLen =$(".fa-file.active").length;
	
	if(trlen>1){
		$(".yunfile-select-num").show();
		$(".yunfile-select").hide();
	}else{
		$(".yunfile-select-num").hide();
		$(".yunfile-select").show();
	}
		
	$(".selected-num").text(trlen);
	$(".selected-folder-num").text(folderLen);
	$(".selected-file-num").text(fileLen);
}


//打开批量移动文件或文件夹的窗口
function showMoveSomeDirectoryOrFile()
{
	// 拿到选中的文件和文件夹名字
	var selectFileName = {};
	var i = 0;
	$(".select-single").each(function() {
		if($(this).hasClass("active")){
			if(stringIsNull($(this).attr("filename")) == false){
				selectFileName[i] = $(this).attr("filename");
				i++;
			}
		}
	});  
	
	if($.isEmptyObject(selectFileName)){
		alert("您没有选择要移动的文件哦！");
		return false;
	}
	selectFileName = JSON.stringify(selectFileName);
	queryOnlyDirectory();
	$("#removeFileModal").modal("show");
	
	$("#moveFileName").val(selectFileName);
  	$("#dialog-yunfile-move-warning").hide();
}

//移动文件确定
function sureMoveFileOrDir(){
	// 拿到当前修改的路径和，旧名字,只能选中一个
	var selectFileName = $("#moveFileName").val();

	var currentPath_move = $("#move_path").val();//要移动的文件夹
	var oldPath = currentPath;//旧文件夹就是当前文件夹
	
	var operFlag=true;//复制、移动操作标志  true为移动
	
	//对比2个文件夹，如果相同则提示，不能移动到原来的文件夹上！
	if(currentPath_move == oldPath)
	{
		$("#yunfile-move-warning").html("不能移动到原来的文件夹上哦！请重新选择！");
		$("#yunfile-move-warning").show();
		return;
	}
	
	var url = manageHost+"/userStorage/moveDirectoryOrFile";

	if(stringIsNull(selectFileName)){
		$("#dialog-yunfile-move-warning").html("您没有选择要移动的文件哦！请重新选择！");
		$("#dialog-yunfile-move-warning").show();
		return;
	}else{
		var params = {
				currentPath:encodeURI(currentPath),
				newPath:encodeURI(currentPath_move),
				selectFileName:encodeURI(selectFileName),
				operFlag:operFlag
		};
		$.getJSON(url,params,function(data) {
			if(1 == data.result)// 隐藏
			{				
				//addTable(currentPath,1);
				var param ={
						currentPath:currentPath,
						sorts:sorts
				}
				datatable.settings()[0].ajax.data = param;
				datatable.ajax.reload();
				$("#removeFileModal").modal("hide");	
				return;
			}else if(97 == data.result){
				$("#repeatFileName").val(data.message);
			}else{
				$("#yunfile-move-warning").html(data.message);
				$("#yunfile-move-warning").show();
			}
		})
		
	}
}


//点击选中某个文件夹的时候设置值
function selectTheNode(path,pPath,name)
{
	$("#move_path").val(path);
	$("#move_pPath").val(pPath);
	$("#move_name").val(name);
}
function queryOnlyDirectory()
{
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
			}
	};
	var r = Math.floor(Math.random() * 9999 + 1);
	var url = manageHost+"/userStorage/queryDirectory";
	var params = {r:r};
	
	$("#waiting").show();
	$.getJSON(url,params,function(data) {
		var markArray;
		var str=new Array;
		var zNodes = data.list;
		for(var i in zNodes){
			str[i]=zNodes[i];			
		}
		str.splice(markArray,0)
    	$.fn.zTree.init($("#treeDemo"), treeSetting, str);
  	    var treeObj = $.fn.zTree.getZTreeObj("treeDemo");
  	    var nodes = treeObj.getNodes();
  		treeObj.selectNode(nodes[0]);
  		$("#treeDemo_1_span").click();  
  		$("#waiting").hide();		
	});
}

function removeFile(){
	var url = manageHost+"/userStorage/moveDirectoryOrFile";
	var params ={
		currentPath:currentPath,
		newPath:'',
		selectFileName:'',
		operFlag:true,
		deleteFlag:true
	}
	$.getJSON(url,params,function(data){

	})
}

//更新文件名
function updateName(name){
	var dataName =  $("#file-name").val();		
	var oldName = name.text();
	name.text(dataName);
	var url = manageHost+"/userStorage/renameDirectoryOrFile";	
	var params = {
			currentPath:encodeURIComponent(currentPath),
			oldName:encodeURIComponent(oldName),
			newName:encodeURIComponent(dataName)
	};	 
	$.getJSON(url,params,function(data){
		if(1 == data.result){
			$("#changeNameModal").modal("hide");
			//addTable(currentPath);
			var param ={
					currentPath:currentPath,
					sorts:sorts
			}
			datatable.settings()[0].ajax.data = param;
			datatable.ajax.reload();
		}else{
			$(".changeNameModal").html(data.message);
			$(".changeNameModal").show();
		}
	})
}

//打开批量删除文件或文件夹的确认窗口
function showDeleteSomeDirectoryOrFile()
{
	// 拿到选中的文件和文件夹名字
	$("#deleteModal").modal("show")
	var selectFileName = {};
	var i = 0;
	$(".select-single").each(function() {
		if($(this).hasClass("active"))
		{
			if(stringIsNull($(this).attr("filename")) == false)
			{
				selectFileName[i] = $(this).attr("filename");
				i++;
			}
		}
	});  
	if(selectFileName.length == 0)
	{
		alert("您没有选择要删除的文件哦！");
		returm;
	}
	selectFileName = JSON.stringify(selectFileName);
	$("#deleteFileName").val(selectFileName);
  	$("#yunfile-delete-warning").hide();
}


//删除文件
function deleteDirectoryOrFile()
{
	// 拿到当前修改的路径和，旧名字,只能选中一个
	var dataName = $("#deleteFileName").val();
	if(dataName == "")
	{
		$("#dialog-yunfile-delete-warning").html("您没有选择要删除的文件哦！请重新选择！");
		$("#dialog-yunfile-delete-warning").show();
		return;
	}else{
		var r = Math.floor(Math.random() * 9999 + 1);
		var params = {r:r,currentPath:encodeURIComponent(currentPath),selectFileName:encodeURIComponent(dataName)};
		 var url =manageHost+"/userStorage/deleteSomeDirectoryOrFile";
		$.getJSON(url,params,function(data){
			if(1 == data.result)// 隐藏
			{
				$("#deleteModal").modal("hide");	   
				//addTable(currentPath);
				var param ={
						currentPath:currentPath,
						sorts:sorts
				}
				datatable.settings()[0].ajax.data = param;
				datatable.ajax.reload();
				return;
			}
			else
			{
				$("#dialog-yunfile-delete-warning").html(data.message);
				$("#dialog-yunfile-delete-warning").show();
			}
		});
	}
}


var sorts = "time_down";
//初始化添加表格
function addTable(currentPath,currentPage){		
	queryUserFilesSize();//获取容量
	$.ajax({
		url: manageHost+'/userStorage/AllFile',
		type: 'POST',
		dataType: 'json',
		data: {
			currentPath:currentPath,
			currentPage:currentPage,
			sort:sorts
		},
		success: function(data, textStatus){	

			var path = data.currentPath;
			if(path == ""){
				$(".current-path").html("当前路径:<a onclick='addTable()'>我的文件</a>")
			}else{					
				path = path.replace("/","");
				var dirData = path.split("/");
				var html ='当前路径:<a onclick="addTable()">我的文件</a>';
				var dataHtml ='';
				var str ="/";
				for(var i=0; i < dirData.length; i++){										
					
					dataHtml +=' > <a onclick=\'addTable("'+str+dirData[i]+'")\'>'+dirData[i]+'</a>';
					str += dirData[i]+"/";
				}
				$(".current-path").html(html+dataHtml);
			}
			
			
			if(data.total == 0){
				$("#addBody").html("<tr><td colspan='3' class='red' align='center'>空文件夹！上传几个文件吧</td></tr>");

				$('.M-box4').html("");
			}else{				
				addHtml(data);			
				
				
				$('.M-box4').pagination({
		        	current:1,//
		        	totalData:data.total,//总条数
		        	showData:data.numInPage,//每页显示的条数
		        	pageCount:data.pageNum,//总页数
		        	jump:false,
		        	coping:true,
		        	homePage:'首页',
		        	endPage:'末页',
		        	prevContent:'上页',
		        	nextContent:'下页',	
		        	callback:function(api){
		        		$("#waiting").show();
		        		var datas ={	        				
		        				currentPage: api.getCurrent(),
		        				sort:sorts
		        		}
		        		$.getJSON(manageHost+'/userStorage/AllFile',datas,function(json){
		        			addHtml(json);		        			
		                });
		        	}        	
		        });
			}	

		}
	});	
	
}
//转成mb
function changeFileSize(size){
	var sizeStr = "";
	if(size =="-1"){
		sizeStr ="-";
	}else if(size < 1024){
		sizeStr = size+"B";
	}else if (size < 1024*1024){
		size = size/(1024);
		size = size.toFixed(2);
		sizeStr = size+"K";
	}else{
		size = size/(1024*1024);
		size = size.toFixed(2);
		sizeStr = size+"M";
	}
	
	return sizeStr;
}
//表格数据添加
function addHtml(data){
	var dataList = data.list,
		   len = dataList.length,
		   file ="<span class='fa fa-file'></span>",
		   fileText ="<span class='fa fa-file-o'></span>",
		   html ='';
	for(var i =0;i<len;i++){		
		var fileHtml ="<span class='filename'  filePath='"+dataList[i].path+"'>"+dataList[i].filename+"</span>"+
		"<div id='file-edit'><a href='javascript:void(0)' class='changeNameBtn' data-name='"+ dataList[i].filename +"'>重命名</a>"+
		"<a href='javascript:void(0)' class='removeBtn' filename='"+dataList[i].filename+"'>移动</a>"+
		"<a href='javascript:void(0)' class='deleteBtn' filename='"+dataList[i].filename+"'>删除</a>";
		
		if(dataList[i].filename.indexOf(".tif") !== -1){
			html += "<tr><td>" +
					"<label class='pull-left'><input type='checkbox' class='select-single' filename='"+dataList[i].filename+"' filePath='"+dataList[i].path+"'/></label>" +
					"<div class='pull-left ml10'>"+file+fileHtml+"<a href='javascript:void(0)' onclick='cuttingModalShow(\""+dataList[i].path+"\")' class='qieBtn' >切片</a></div></div></td>" +
					"<td>"+changeFileSize(dataList[i].size)+"</td>" +
					"<td>"+dataList[i].time+"</td>";
		}else{
			html += "<tr><td>" +
					"<label class='pull-left'><input type='checkbox' class='select-single' filename='"+dataList[i].filename+"' filePath='"+dataList[i].path+"'/></label>" +
					"<div class='pull-left ml10'>"+fileText+fileHtml+"</div></div></td>" +
					"<td>"+changeFileSize(dataList[i].size)+"</td>" +
					"<td>"+dataList[i].time+"</td>";
		}
		$("#addBody").html(html);
		
	}
	$("#addBody").html();
}

/**
 * 去掉字符串空格
 * 
 * @param stringToTrim
 * @returns
 */
function g_trim(stringToTrim) {
	if (undefined == stringToTrim || null == stringToTrim) {
		return "";
	}
	return stringToTrim.replace(/^\s+|\s+$/g, "");
}

function checkIllegalChar(m) {
	if ((m.indexOf("<") >= 0) || (m.indexOf(">") >= 0)
			|| (m.indexOf("\\") >= 0) || (m.indexOf("/") >= 0)
			|| (m.indexOf(":") >= 0) || (m.indexOf("*") >= 0)
			|| (m.indexOf("?") >= 0) || (m.indexOf("\"") >= 0)
			|| (m.indexOf("|") >= 0)) {
		return 0;
	} else
		return 1;
}

/**
 * 新建文件夹
 */
var currentPath ="";
function createNewDirectory()
{
	var dataName = g_trim($("#newFile-name").val());	
	dataName = dataName.replace(/[ ]/g,"");
	
	
	if(checkIllegalChar(dataName) == 0){
		$("#errorInfo").html("文件夹名字不能为空或含有非法字符哦！");		
	}else{
		//$("#dialog-yunfile-create-warning").hide();
		var url = manageHost+"/userStorage/createNewDirectory";
		var params = {currentPath:encodeURI(currentPath),newName:encodeURI(dataName)};
		$.getJSON(url,params,function(data){
			if(1 == data.result){
//				addTable();				
				datatable.ajax.reload();
			}else if(data.message == "目录已存在"){
				$("#dialog-yunfile-create-warning").html("目录已存在！");
				$("#dialog-yunfile-create-warning").show();
			}

		});
	}
}
function GetDateT(){
	 var d,s;
	 d = new Date();
	 s = d.getYear() + "-";             //取年份
	 s = s + (d.getMonth() + 1) + "-";//取月份
	 s += d.getDate() + " ";         //取日期
	 s += d.getHours() + ":";       //取小时
	 s += d.getMinutes() + ":";    //取分
	 s += d.getSeconds();         //取秒
	 
	 return(s);  

} 

//用于文件排序
function sortFileList(type, sort, obj){
	var thisObj = $(obj).find(".yunfile-sort-up");
	thisObj.css("display","inline-block");
	sorts  = sort;
	if("name" == type){
		if("name_down" == sort)
		{
			$("#name_sort").attr("onclick","sortFileList('name', 'name_up','#name_sort')");
			$(obj).attr("data-sortType","up");
			thisObj.addClass("sort-down");		
		}else{
			$("#name_sort").attr("onclick","sortFileList('name', 'name_down','#name_sort')");
			$(obj).attr("data-sortType","down");			
		}
		//addTable(currentPath,1);
		var param ={
				currentPath:currentPath,
				sorts:sorts
		}
		datatable.settings()[0].ajax.data = param;
		datatable.ajax.reload();
		return;
	}else if("size" == type){
		
		if("size_down" == sort){
			$("#size_sort").attr("onclick","sortFileList('size', 'size_up','#size_sort')");
			$(obj).attr("data-sortType","up");
			thisObj.addClass("sort-down");		

		}else{
			$("#size_sort").attr("onclick","sortFileList('size', 'size_down','#size_sort')");
			$(obj).attr("data-sortType","down");	
		}
	}else{
		if("time_down" == sort){
			$("#time_sort").attr("onclick","sortFileList('time', 'time_up','#time_sort')");
			$(obj).attr("data-sortType","up");
			thisObj.addClass("sort-down");		
		}else{
			$("#time_sort").attr("onclick","sortFileList('time', 'time_down','#time_sort')");
			$(obj).attr("data-sortType","down");

		}
	}
	
	//addTable(currentPath,1);
	var param ={
			currentPath:currentPath,
			sorts:sorts
	}
	datatable.settings()[0].ajax.data = param;
	datatable.ajax.reload();
	
}

/**
 * 判断字符串时候为空，空就返回true，否则返回false
 * @param str
 * @returns {Boolean}
 */
function stringIsNull(str){
	if("0" == str){
		return false;
	}
	if(null == str || "" == str || undefined == str){
		return true;
	}
	return false;
}



var UserSizeBoolean;
var availableSpace=0;
//计算云盘容量
function queryUserFilesSize()
{
	var r = Math.floor(Math.random() * 9999 + 1);
	var params = {r:r};
	var url = manageHost+"/userStorage/queryUserSize";

	$.getJSON(url,params,function(data) {
		
		if(!!data && (typeof data) == "string"){
			$("#waiting").hide();
			data = eval("("+data+")");
		}		
		if(data.result == 1)
		{
			var total = parseFloat(data.TotalSize);
			var UsedSize = parseFloat(data.UsedSize);
			//var WilltoDateSize = parseFloat(data.WilltoDateSize);
			if(parseFloat((UsedSize/(1024*1024*1024)).toFixed(2)) >= parseFloat((total/(1024*1024*1024)).toFixed(2)))
				UserSizeBoolean = 0;// 云盘已满，不能使用
			else{
				UserSizeBoolean = 1;// 云盘未满，可以使用
				availableSpace=total-UsedSize;
			}
				
			/*if((data.WilltoDateSize != 0 && WilltoDateSize) || UserSizeBoolean == 0){
				$(".warning").addClass("dn");
				$(".warning-active").removeClass("dn");
			}
			else{
				$(".warning").removeClass("dn");
				$(".warning-active").addClass("dn");
			}*/
			/*if(WilltoDateSize != 0 && WilltoDateSize){
				WilltoDateSize = WilltoDateSize/(1024*1024*1024);
				WilltoDateSize = WilltoDateSize.toFixed(0);  // 得到四舍五入的值22.13
				$("#userSizeBar").removeClass("bgred");
				$(".sizeTip-content").html("");
				var content="<p>您有 "+WilltoDateSize+"G 空间即将到期，到期后若存储内容大于云盘空间，云盘被冻结，只能下载或删除<p>";
				$(".sizeTip-content").html(content);	
			}*/
			
			if(UserSizeBoolean == 0){
				$(".progress-bar").addClass("bgred");
				$(".sizeTip-content").html("");
				var content="<p>云盘储存内容过多，已冻结云盘，暂时只能下载或删除云盘内容<p>";
				$(".sizeTip-content").html(content);	
			}
			var p = (UsedSize/total)*100;
			var toP = p.toFixed(2);
			$(".progress-bar").css("width",toP+"%");
			
			$(".progress-bar").attr("data-width",toP);
			total = total/(1024*1024*1024);
			total = total.toFixed(2);  // 得到四舍五入的值22.13
			UsedSize = UsedSize/(1024*1024*1024);
			UsedSize = UsedSize.toFixed(2);  // 得到四舍五入的值22.13
			$("#userSizeNum").html(UsedSize+"G/"+total+"G");
			
/*			var allSongSize = data.allSongSize;
			allSongSize = allSongSize/(1024*1024*1024);
			allSongSize = allSongSize.toFixed(2);  // 得到四舍五入的值22.13
			$("#songSize").html(allSongSize +"G");
			var kr=allSongSize/total;
			$(".uc-yunfile-kuorong").css("width",kr*100+"%");
			$(".uc-yunfile-kuorong").attr("data-width",kr*100);*/
			//showBarIndex();// 容量与扩容显示
			
		}
	});
}

//容量与扩容显示
function showBarIndex(){
	var ordinary=$("#userSizeBar").attr("data-width")*$(".yunfile-volumen").width()/100;
	var add=$(".yunfile-volumen .uc-yunfile-kuorong").attr("data-width")*$(".yunfile-volumen").width()/100;
	if(ordinary<add){
		$(".yunfile-volumen .progress-bar").css("z-index","10");
		$(".yunfile-volumen .uc-yunfile-kuorong").css("z-index","1");
	}else{
		$(".yunfile-volumen .progress-bar").css("z-index","1");
		$(".yunfile-volumen .uc-yunfile-kuorong").css("z-index","10");
	}	
}

function cuttingModalShow(inPath){
	cutTypeChange($("#cuttingForm").find("select[name=cutType]"));
	$("#cuttingForm").find("input[name=inPath]").val(inPath);
	$('#qiepianModal').modal({ keyboard: false,backdrop:false});	
}

function cuttingSubmit(){
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
	
	var realTime=form.find("input[name=realTime]:visible").val();
	
	var jobName=form.find("input[name=jobName]:visible").val();
	var  regNum =/^[1-9]\d*|0$/;	   
	var re = new RegExp(regNum);
	//校验
	if(mapName == "" ){
		alert("请填写图层名!");
		form.find("input[name=mapName]").focus();
		return;
	}else if(outPath ==""){
		alert("请填写输出路径");
		form.find("input[name=outPath]").focus();
		return;
	}else if(minLayers >25){
		alert("层级输入最大25");		
		form.find("input[name=minLayers]").focus();
		return;
	}else if(maxLayers >25){
		alert("层级输入最大25");		
		form.find("input[name=maxLayers]").focus();
		return;	
	}else if(maxLayers ==""){
		alert("请填写最大级");
		form.find("input[name=maxLayers]").focus();
		return;
	}else if(minLayers ==""){
		alert("请填写最小级");
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
		"cutType":cutType,
		"mapName":mapName,
		"minLayers":minLayers,
		"isCover":isCover,
		"maxLayers":maxLayers,
		"outPath":outPath,
		"usage":usage,
		"inPath":inPath,
		"waterMark":waterMark,
		"jobName":jobName
	}
	if(!form.find("input[name=realTime]").closest(".row").hasClass('hide')){
		
		if(realTime ==""){
			parmas.realTime = "";
		}else{
			parmas.realTime = realTime
			check(realTime);
		}
		
	}
	$("#waiting").show();
	$.ajax({
		url:"/dataCenter/onemapCut",
		type:"post",
		data:parmas,
		success:function(res){
			if(res.code==2001){
				$("#qiepianModal").modal("hide");
				$("#waiting").hide();
				location.href="/production/jobMonitoring?pageType=cut&msg=提交任务成功";
			}else{
				$("#waiting").hide();
				$(".errorMsg").text(res.message);
			}
		}
	})
}

function check(str){
	var a = /((?:19|20)\d{2}\/(?:0[1-9]|1[0-2])\/(?:0[1-9]|[12][0-9]|3[01]))/;
	if (!a.test(str)) { 
		alert("日期格式不正确!") 
		return;
	}
} 

function cutTypeChange(e){
	var form=$("#cuttingForm");
	var val=$(e).val();
	
	if(val==0){//新增，不需要填写最小层级
		form.find("input[name=minLayers]").css("display","none").closest(".row").css("display","none");
		form.find("input[name=realTime]").closest(".row").addClass('hide');
		form.find("select[name=waterMark]").css("display","block").closest(".row").css("display","block");
		form.find("input[name=usage]").css("display","block").closest(".row").css("display","block");
		form.find("input[name=outPath]").css("display","block").closest(".row").css("display","block");
		
	}else{//更新 不需要填写用途，水印方案，输出路径
		
		form.find("input[name=minLayers]").css("display","block").closest(".row").css("display","block");
		form.find("input[name=realTime]").closest(".row").removeClass('hide');
		form.find("select[name=waterMark]").val("").css("display","none").closest(".row").css("display","none");
		form.find("input[name=usage]").val("").css("display","none").closest(".row").css("display","none");
		form.find("input[name=outPath]").val("").css("display","none").closest(".row").css("display","none");
	}
}