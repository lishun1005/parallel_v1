function sysRoleFormSubmit() {
	if (checkFormValue()) {
		$('#sysRoleForm').submit();
	}
}
$(function() {
	// 弹出框高度对页面高度的控制
	dialog_show_hide("sysRolePermissionModal,sysRoleModal,sysRoleModal");
})
function sysRoleMenuFormSubmit() {
	$('#sysRoleMenuForm').submit();
}

function sysRoleDel(sysRoleId) {
	if (confirm("你确定删除吗？")) {
		window.location.href = "${manageHost}/sysrole/sysRoleDelete?sysRoleId="
				+ sysRoleId;
	}
}

function sysRoleAdd() {
	var form = $('#sysRoleForm');
	form.attr("action", "../sysrole/sysRoleAdd.html");
	form.find("[name='role']").val("");
	form.find("[name='description']").val("");
//	form.find("[name='available']").find("option[value='true']").attr(
//			"selected", true);
	$('#sysRoleModal').modal({
		keyboard : false,
		backdrop : false
	});
}

function sysRoleUpdate(sysRoleId) {
	var role = $("#" + sysRoleId).find("[name='role']").text();
	var description = $("#" + sysRoleId).find("[name='description']").text();
//	var available = $("#" + sysRoleId).find("[name='available']").text();
	var form = $('#sysRoleForm');
	form.attr("action", "../sysrole/sysRoleUpdate?id=" + sysRoleId);
	form.find("[name='role']").val(role);
	form.find("[name='description']").val(description);
//	form.find("[name='available']").find("option[value=" + available + "]")
//			.attr("selected", true);
	$('#sysRoleModal').modal({
		keyboard : false,
		backdrop : false
	});
}

/*function invalidHandler(e) {
	checkValue(e.target);
	e.stopPropagation();
	e.preventDefault();
}

var checkFormValue = function() {
	var form = document.getElementById("sysRoleForm");
	;
	for (var i = 0; i < form.elements.length - 1; i++) {
		var element = form.elements[i];
		if (!checkValue(element)) {
			return false;
		}
	}
	return true;
}

var checkValue = function(element) {
	if (element.checkValidity()) {
		element.parentElement.className = "form-group";
		return true;
	} else {
		element.parentElement.className = "form-group has-error";
		alert(element.validationMessage);
		return false;
	}
}*/

var checkFormValue = function(){
	var form = document.getElementById("sysRoleForm");;
	for(var i=0;i< form.elements.length;i++){
		var element = form.elements[i];
		if(!checkValue(element)){
			return false; 
		}
	}
	return true;
}
function invalidHandler(e){
    checkValue(e.target);
    e.stopPropagation();
    e.preventDefault();
}
var checkValue = function(element){
	if(element.checkValidity()){
		return true;
	}else{
		$(element).attr("data-toggle","tooltip");
		$(element).attr("data-placement","top");
		showPopover($(element),"请填写字段");
		return false;
	}
}

/*function loadinvalidHandler() {
	var myform = document.getElementById("sysRoleForm");
	for (var i = 0; i < myform.elements.length - 1; i++) {
		// 表单元素的onchange事件，优化用户体验
		myform.elements[i].addEventListener("change", invalidHandler, false);
	}
}
// 在页面初始化事件（onload）时注册的自定义事件
window.addEventListener("load", loadinvalidHandler, false);*/

var currPermissionData;
function editRolePermissionBtn(sysRoleId) {
	$.get("${manageHost}/sysrole/getMyPermission?sysRoleId=" + sysRoleId,
			function(data, status) {
				if (status == "success") {
					currPermissionData = data;
					$('#sysRolePermissionForm').attr(
							"action",
							"${manageHost}/sysrole/editMyPermission?sysRoleId="
									+ sysRoleId);
					initPermissionICheck(data);
					$('#sysRolePermissionModal').modal({
						keyboard : false,
						backdrop : false
					});
				} else {
					alert("该用户获取角色信息失败");
				}
			});
}
function resetPermissionICheck() {
	initPermissionICheck(currPermissionData);
}
function initPermissionICheck(data) {
	$('#sysRolePermissionForm').html('');
	$.each(data, function(key, value) {
		var permission = value.sysPermission.permission;
		var description = value.sysPermission.description;
		var permissionId = value.sysPermission.id;
		var isbind = value.isbind;
		$('#sysRolePermissionForm').append(
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
		if (value.isbind)
			$('#permission' + key).iCheck('check');
		else
			$('#permission' + key).iCheck('uncheck');
	});
}

/**
 * 给角色分配菜单
 */
function editRoleMenuBtn(sysRoleId,description) {
	
	$.ajax({
        url:"../sysrole/getMyMenu",
        data:{"sysRoleId":sysRoleId},
        type:"get",
        success:function(data, status){
        	if (status == "success") {
				$('#sysRoleMenuForm').attr(
						"action",
						"../sysrole/editMyMenu?sysRoleId="
								+ sysRoleId);
				initMenuICheck(data.roleMenuDtoList);
				$('#sysRoleMenuModal').modal({
					keyboard : false,
					backdrop : false
				});
				 $("#edit-rolename").html(description);
				 window.location.href="#menu-checked0";
			} else {
				alert("该用户获取角色信息失败");
			}
        }
    });
}

function initMenuICheck(data) {
	$('#sysRoleMenuForm').html('');
	$.each( data,
			function(key, value) {
				var sysMenu = value.sysMenuDto;
				var isCheck = value.isCheck;
				
				if(sysMenu.name != 'ROOT'){
					if(sysMenu.menuType == '1'){
						$('#sysRoleMenuForm')
						.append('<li class="list-group-item" id="'+sysMenu.id+'" parentId="'+sysMenu.parentId+'"><input type="checkbox" id="menu'+key+'" name="folderMenu" onclick="clickFolderMenu(\''+sysMenu.id+'\',\'menu'+key+'\')"/><label style="padding-left: 8px;" for="menu'+key+'" onclick="clickFolderMenu(\''+sysMenu.id+'\',\'menu'+key+'\')">'+sysMenu.name + '</label><ul class="list-group"></ul>');
					}else{
						$('#sysRoleMenuForm')
						.append('<li class="list-group-item" id="'+sysMenu.id+'" parentId="'+sysMenu.parentId+'"><input type="checkbox" id="menu'+key+'" name="pageMenu" value="'+sysMenu.id+'"/><label style="padding-left: 8px;font-weight: normal;" id="label' + key + '" for="menu'+key+'">'+sysMenu.name + '</label>');
					}
				}
			}
	);
	
	$("#sysRoleMenuForm").find("li").each(function() { //遍历出所有的菜单项li		
		var parentId = $(this).attr("parentId"); //获取当前菜单的父级id
		if ($("#" + parentId).length > 0) { //父菜单不存在则不做处理
			$(this).appendTo("#" + parentId + " ul:first"); //将该菜单移动到其父菜单的ul下面
		}
	});
	
	var index = 0;
	$.each(data, function(key, value) {
		if (value.isCheck){
			$('#menu' + key).iCheck('check');
			$('#label' + key).attr("style", "color: red;");
			$('#label' + key).attr("id", "menu-checked" + index);
			index++;
		}
		else{
			$('#menu' + key).iCheck('uncheck');
		}
		
		
	});
}

window.onload = function() {
	window.parent.$("#waiting").hide();
}

function clickFolderMenu(menuId,inputId) {
	
	var check = "uncheck";
	if ($("#" + inputId).is(':checked')) {
		check = "check";
	}
	
	$("#sysRoleMenuForm").find("li").each(function() { //遍历出所有的菜单项li		
		var parentId = $(this).attr("parentId"); //获取当前菜单的父级id
		if(parentId == menuId){
			$(this).iCheck(check);
		}
	});
}

$(function(){
	//弹出框高度对页面高度的控制
	dialog_show_hide("sysRoleModal,sysRoleMenuModal");
})

