/**
 * Created by admin on 2017/2/28.
 */
 var host = 'http://localhost:8080/'; //本地
    // var host = 'http://192.168.101.140:8080/';  //刘鹏电脑   
$(function () {
        if($.fn.dataTable!=undefined){
            //datatable默认设置
            $.extend($.fn.dataTable.defaults,{
                // "iDisplayLength" : 10,//默认每页数量
            	"processing": true,
                "serverSide":true,  //开启服务器模式
                "lengthChange": false,//是否允许用户自定义显示数量
                "bPaginate": true, //翻页功能
                "bFilter": false, //列筛序功能
                "searching": false,//本地搜索
                "ordering": true, //排序功能
                "autoWidth":false,
            })
            $.extend($.fn.dataTable.defaults.oLanguage,{
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
                   "sProcessing": "正在加载数据...",
                   "sSearch": "模糊查询：",
                   "sUrl": "",
                   "oPaginate": {
                       "sFirst": "首页",
                       "sPrevious": " 上一页 ",
                       "sNext": " 下一页 ",
                       "sLast": " 尾页 "
                   }
            })
        }
        

        
    }
)