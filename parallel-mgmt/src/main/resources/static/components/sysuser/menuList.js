
// 弹出框高度对页面高度的控制
$(function() {
	dialog_show_hide("infoModal,sysMenuPermissionModal");
})

$(document).ready(function() {
	
	/**
	 * 组建菜单树（原理：页面原先的菜单是乱的，根据parentId构建菜单树）
	 */
	$("#menu_div").find("li").each(function() { //遍历出所有的菜单项li
		
		var parentId = $(this).attr("parentId"); //获取当前菜单的父级id
		
		if ($("#" + parentId).length > 0) { //父菜单不存在则不做处理
			
			if ($("#" + parentId).children("ul").length <= 0) { //如果父菜单下没有存放子菜单的ul，则创建
				var ul = $("<ul></ul>").addClass("list-group");
				$("#" + parentId).append(ul);
			}
			
			$(this).appendTo("#" + parentId + " ul:first"); //将该菜单移动到其父菜单的ul下面
		}
	});
});

/**
 * 弹出新增菜单对话框
 */
function infoAdd() {
	
	$("#infoModalLabel").html("新增菜单");
	$('#editDialog').modal({
		keyboard : false,
		backdrop : false
	});
	
	$("input[name=url]").attr("disabled",false);
	$("input[name=permission]").attr("disabled",false);
	
	$('.form-text').val("");
	
	var parentId = document.getElementById("parentId");
	parentId.options[0].selected = true;
	changeFolder();
	
	$('#addType').css("display", "");
	$('#updateType').css("display", "none");
	
	$("#icon-show").attr("class", "fa fa-bar-chart-o");
	$("#menuForm").find("select[name='icon']").val("fa-bar-chart-o");
}

/**
 * 弹出更新菜单对话框
 */
function infoUpdate(id) {
	
	var menu = $("#" + id);
	var name = menu.attr("name");
	var parentId = menu.attr("parentId");
	var zIndex = menu.attr("zIndex");
	var url = menu.attr("url");
	var sequence = menu.attr("sequence");
	var menuType = menu.attr("menuType");
	var permission = menu.attr("permission");
	var icon = menu.attr("icon");
	var isShow = menu.attr("isShow");
	
	var menuForm = $("#menuForm");

	menuForm.find("input[name='name']").val(name);
	menuForm.find("input[name='url']").val(url);
	menuForm.find("input[name='sequence']").val(sequence);
	menuForm.find("input[name='permission']").val(permission);
	menuForm.find("input[name='id']").val(id);
	menuForm.find("select[name='isShow']").val(isShow);
	
	menuForm.find("select[name='parentId']").val(parentId);
	menuForm.find("select[name='icon']").val(icon);
	menuForm.find("select[name='menuType']").val(menuType);
	
	
	$("#icon-show").attr("class", "fa " + icon);
	changeFolder();
	
	var parentName = menuForm.find("select[name='parentId']").find("option:selected").text();
	$("#infoModalLabel").html(parentName + "：" + name);
	
	$("input[name=url]").attr("disabled",true);
	$("input[name=permission]").attr("disabled",true);
	$('#editDialog').modal({
		keyboard : false,
		backdrop : false
	});
	
	$('#addType').css("display", "none");
	$('#updateType').css("display", "");
	
	if(menuType == "1"){
		$('#type').html("文件夹");
	}else if(menuType=="2"){
		$('#type').html("页面");
	}else if(menuType=="3"){
		$('#type').html("接口");
	}else{
		$('#type').html("api");
	}
	
}

function menuFormSubmit() {
	
	
	var form = $("#menuForm");
	//var isFolder = $('input[name="isFolder"]:checked').val();
	var memuType=form.find("select[name=menuType]").val();
	var name = form.find("[name='name']");
	if(!name.val()){
		alert("菜单名不能为空");
		name.focus();
		return;
	}
	
	var sequence = form.find("[name='sequence']");
	if($('#sequenceDiv').css("display")!="none" && !sequence.val()){
		alert("排序不能为空");
		sequence.focus();
		return;
	}
	
	if(memuType == "1"){
		var icon = form.find("[name='icon']");
		if(!icon.val()){
			alert("图标不能为空");
			icon.focus();
			return;
		}
		$('#menuForm').submit();
		window.location.href="#" + form.find("[name='id']").val();
	}else{
		var url = form.find("[name='url']");
		if(!url.val()){
			alert("url不能为空");
			url.focus();
			return;
		}
		var permission = form.find("[name='permission']");
		if(!permission.val()){
			alert("权限不能为空");
			permission.focus();
			return;
		}
		
		//检查数据库中是否存在相同的权限名
		$.ajax({
	        url:"../menuManageNew/checkPermission",
	        data:{"permission":permission.val(),"id":form.find("[name='id']").val()},
	        type:"get",
	        success:function(data){
	          if(data.isHere){
	        	  alert("权限名已存在");
	        	  permission.focus();
	          }else{
	        	  $('#menuForm').submit();
	        	  window.location.href="#" + form.find("[name='id']").val();
	          }
	        }
	    });
	}
}

function menuFormclose() {
	
	var form = $("#menuForm");
	window.location.href="#" + form.find("[name='id']").val();
}

var currPermissionData;
var allPermissionData;
function editMenuPermissionBtn(menuId) {
	$.get("restAllPermission", function(data, status) {
		if (status == "success") {
			currPermissionData = $("#" + menuId).attr("permissionIds");
			allPermissionData = data;
			$('#sysMenuPermissionForm').attr("action",
					"editMenuPermission?menuId=" + menuId);
			initPermissionICheck(data, currPermissionData);
			$('#sysMenuPermissionModal').modal({
				keyboard : false,
				backdrop : false
			});
		} else {
			alert("该菜单获取角色信息失败");
		}
	});
}
function clearPermissionData() {
	currPermissionData = null;
	allPermissionData = null;
}
function resetPermissionICheck() {
	if (currPermissionData == null || allPermissionData == null) {
		alert("重置异常");
	}
	initPermissionICheck(allPermissionData, currPermissionData);
}
function initPermissionICheck(data, selectData) {
	if (selectData == null) {
		selectData = "";
	}
	$('#sysMenuPermissionForm').html('');
	$.each(data, function(key, value) {
		var permission = value.permission;
		var description = value.description;
		var permissionId = value.id;
		var isbind = selectData.indexOf(permissionId) >= 0;
		$('#sysMenuPermissionForm').append(
				'<div class="form-group"><input type="checkbox" id="permission'
						+ key
						+ '" name="permission"  class="form-control" value="'
						+ permissionId + '" checked="' + isbind
						+ '"><label for="permission' + key + '">' + description
						+ '_' + permission + '</label></div>');
	});
	$('input').iCheck({
		checkboxClass : 'icheckbox_minimal',
		radioClass : 'iradio_minimal',
		increaseArea : '20%' // optional
	});
	$.each(data, function(key, value) {
		if (selectData.indexOf(key) >= 0)
			$('#permission' + key).iCheck('check');
		else
			$('#permission' + key).iCheck('uncheck');
	});
}

function recursionJSON(data) {
	if (typeof (data) == "object") {
		$.each(data, function(i, n) {
			if (typeof (n) == "object") {
				if (n != null)
					recursionJSON(n);
			} else {
				console.log(i + ": " + n);
			}
		})
	}
}
function invalidHandler(e) {
	checkValue(e.target);
	e.stopPropagation();
	e.preventDefault();
}


function loadinvalidHandler() {
	var myform = document.getElementById("menuForm");
	for (var i = 0; i < myform.elements.length - 1; i++) {
		// 表单元素的onchange事件，优化用户体验
		myform.elements[i].addEventListener("change", invalidHandler, false);
	}
}
// 在页面初始化事件（onload）时注册的自定义事件
window.addEventListener("load", loadinvalidHandler, false);


function changeFolder() {
	
	var form = $('#menuForm');
	var memuType=form.find("select[name=menuType]").val();
	if (memuType == "1") {
		$('#icon').css("display", "");
		$('#page-menu').css("display", "none");
		$('#sequenceDiv').css("display", "");
		form.find("[name='url']").val("");
		form.find("[name='permission']").val("");
		$("#isShowDiv").css("display", "");
	} else if(memuType == "2"){
		$('#page-menu').css("display", "");
		$('#icon').css("display", "none");
		form.find("[name='icon']").val("");
		$('#sequenceDiv').css("display", "");
		$("#isShowDiv").css("display", "");
	}else{
		$('#page-menu').css("display", "");
		$('#icon').css("display", "none");
		$('#sequenceDiv').css("display", "none");
		form.find("[name='icon']").val("");
		$("#isShowDiv").css("display", "none");
	}
}

function changIconSelect(obj) {
	
	var index = obj.selectedIndex; // 选中索引
	var text = obj.options[index].text; // 选中文本
	$("#icon-show").attr("class", "fa " + text);
}



$(function() {
	window.parent.reload_view($(window).height());
});
window.onload = function() {
	//changeFolder("true");
	window.parent.$("#waiting").hide();
}