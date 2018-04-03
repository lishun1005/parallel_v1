/**********地图操作********/
var host = manageHost;
$(function(){
	//初始化地图
	setBaseLayers();
	//经纬度
	$(".latitude input").keyup(function(){
			clearNoNum(this);
			drawPolygonByXY();	
			$("#areaName_hidden").val("3");
	});
	$("#select-range-ll").click(function(){
		$("#areaName_hidden").val("3");
	})
	$("#select-range-regions").click(function(){
		$("#areaName_hidden").val("1");
	})
	selectNew();
	initMenuList();

})



/**
 * 分辨率初始化
 * 
 * *
 */


function initMenuList(){
	var url = host + "/querySatelliteType";	
	$.ajax({
		url:url,
		type: 'POST',
        dataType:"json",	        
        success: function (data) {
        	var len = data.length;
        	var htmls='';
        	for(var i = 0;i<len;i++){
        		
        		htmls +='<li role="presentation">'+
	                '<a role="menuitem" tabindex="-1"  text="'+data[i].satellite+'">'+data[i].satellite+'-'+data[i].sensorId+"("+data[i].resolution+"m)"+'</a>'+
	            '</li>'
        	}
        	$("#menuList").html(htmls);
        	
        }
	})
};

var map;//主地图 
var vectors2;//边框矢量图层
var select;//交互对象
var drawCustomExtendVector;//区域矢量图层
var drawCustomExtendVector2;//区域矢量图层蓝色
var polygon;//自定义控件
/**
 * 初始化主地图
 * 
 * 
 * **/
function setBaseLayers(){
    /* 设置中央地图区域的地图 */
    map = new ol.Map({
        target:'map',
        controls:[],
        layers:[
            new ol.layer.Tile({
                title:"Global Imagery",
                source: new ol.source.TileWMS({
                    url:'http://210.77.87.225:8080/geowebcache/service/wms',
                    params:{
                        layers:'mv-ne_sw-jpeg_90_2013_world_china-0gtscreen',
                        style:'',
                        format:'image/jpeg',
                        VERSION:'1.1.1'
                    }
                })
            })
        ],
        view: new ol.View({
            projection:'EPSG:4326',
            center:[105.410562,31.209316],
            zoom:3,
            resolutions:[ 0.703125, 0.3515625, 0.17578125, 0.087890625,
                0.0439453125, 0.02197265625, 0.010986328125, 0.0054931640625,
                0.00274658203125, 0.001373291015625, 6.866455078125E-4,
                3.4332275390625E-4, 1.71661376953125E-4, 8.58306884765625E-5, 4.291534423828125E-5 ]
        })

    });
    
    //用户用来勾画想要查找的范围的工具
    drawCustomExtendVector = new ol.layer.Vector({
        source: new ol.source.Vector(),
        style: new ol.style.Style({
            fill: new ol.style.Fill({
                color: 'rgba(153,51,0,0.15)'
            }),
            stroke: new ol.style.Stroke({
                color: 'rgba(187,0,0,0.3)',
                width: 2
            })
        })
    })
    map.addLayer(drawCustomExtendVector);

    drawCustomExtendVector2 = new ol.layer.Vector({
        source: new ol.source.Vector(),
        style: new ol.style.Style({
            fill: new ol.style.Fill({
                color: 'rgba(255,255,255,0)'
            }),
            stroke: new ol.style.Stroke({
                color: 'rgba(0,0,255,0.7)',
                width: 2
            })
        })
    })
    map.addLayer(drawCustomExtendVector2);

    vectors2 = new ol.layer.Vector({
        source: new ol.source.Vector(),
        style: new ol.style.Style({
            fill: new ol.style.Fill({
                color: 'rgba(255,255,255,0)'
            }),
            stroke: new ol.style.Stroke({
                color: 'rgba(32,70,250,0.3)',
                width: 2
            })
        })
    })
    map.addLayer(vectors2);
    
    newVectors = new ol.layer.Vector({
        source: new ol.source.Vector(),
        style: new ol.style.Style({
            fill: new ol.style.Fill({
                color: 'rgba(255,255,255,0)'
            }),
            stroke: new ol.style.Stroke({
                color: 'rgba(32,70,250,0.3)',
                width: 2
            })
        })
    })
    map.addLayer(newVectors);

    map.on('pointermove', function (evt) {    	
        map.getTargetElement().style.cursor =  map.hasFeatureAtPixel(evt.pixel) ? 'pointer' : '';  
    }); 

    //增加画图控件
    polygon = new ol.interaction.Draw({
        type: 'Polygon',//Polygon
        source: drawCustomExtendVector.getSource(),    //添加到这个source里
    });
    polygon.on("drawend",function(event){    	
		map.removeInteraction(polygon);
		//多边形立即执行查询数据方法
		//var formatGeo = ol.format.GeoJSON;
		//var geojson = formatGeo.writeGeometry(event.feature.getGeometry());		
		//var geoJsons = JSON.stringify(geojson)
		//var data = {range:geoJsons,type:'2'};
		//获取自定义geoJson
		var geJsons = JSON.stringify(event.feature.getGeometry().getCoordinates()),
			      data  = '{"type":"Polygon","coordinates":'+geJsons+'}';
		$("#id_hidden").val("");
		$("#type_hidden").val("2");
		$("#geom_hidden").val(data);
		
		$(".buyBtn").css({"pointer-events":"inherit","background": "#3399FF","color":"#fff"});
	})
    $("#mapTools").append($("#rihgt-bar").html());

}
//绘制多边形
function resetTheLocalText(){		

	$("#map").addClass("draw");
	cleanAllFeatures();
	//vectors2.getSource().clear();
	map.addInteraction(polygon);
    $("#type_hidden").val("2");
    $("#areaName_hidden").val("2");
	$("#show_index_local").html("自定义范围");
	$(".rihgt-bar-li4").addClass("active");
	$("#select-range-regions").prop("checked",true);
	
    //$("#select-range-").prop("checked",true);	
}





/**********经纬度********/
function drawPolygonByXY(){
	var x1 = $("#ll_in1").val(),//left
		x2 = $("#ll_in2").val(),//right
		y2 = $("#ll_in4").val(),//top
		y1 = $("#ll_in3").val();//bottom

	if(x1 == "" || x2 =="" || y1 =="" || y2 ==""){
		return;
	}

	$("#show_index_local").attr("title","请选择").text("请选择"); 	
	$("#select-range-regions").prop("checked",false);
	$("#select-range-ll").prop("checked",true);
	
	cleanAllFeatures();
	x1 = parseFloat(x1);
	x2 = parseFloat(x2);
	y1 = parseFloat(y1);
	y2 = parseFloat(y2);	
	var arry = [];
	var geojsonObject = {
          'type': 'Polygon',
          'coordinates': [[[x2,y2], [x2, y1], [x1, y1],[x1, y2],[x2, y2]]]
    }
    $("#type_hidden").val("2");
	$("#geom_hidden").val(JSON.stringify(geojsonObject));

    var polygons = (new ol.format.GeoJSON()).readGeometry(geojsonObject);	
    var features = new ol.Feature(polygons);
    drawCustomExtendVector.getSource().addFeature(features)
    var size =(map.getSize());    
    map.getView().fit(polygons, size);   
}

/**
 * 显示出地区的范围
 * @param areaid
 */
var cacheAreaRange;//保存选中区域geoJson
function showAreaBounds(areaid){
    //请求后台数据库
    ShowDIV("waiting");
    var r = Math.floor(Math.random() * 9999 + 1);
    var params = {admincode:areaid};
    var url =host+"/areaInfo/queryGeomByAreaCode";
    $.getJSON(url,params,function(data) {
        $("#type_hidden").val("1");
        showLocationBounds(data);        
        cacheAreaRange = data;
        areaid = areaid;        
        closeDiv("waiting");
    });
    
}

/**
 * 显示数据库中数据的覆盖范围
 */
function showLocationBounds(bounds){
    cleanAllFeatures();
    //bounds={"type":"Polygon","coordinates":[[[113.148225,42.55438889],[113.148225,36.03571667],[119.6972889,36.03571667],[119.6972889,42.55438889],[113.148225,42.55438889]]]};
    var geom = (new ol.format.GeoJSON()).readGeometry(bounds);    
    var feature = new ol.Feature(geom);
    var source = new ol.source.Vector({
        features:[feature],
        wrapx:false
    })

    var polygons = (source.getFeatures()[0].getGeometry());
    var size =(map.getSize());

    drawCustomExtendVector.getSource().addFeature(feature)
    map.getView().fit(polygons, size);
}

/**
 *  区域矢量边蓝色边框 
 */
function showSearchSelectZoom() {
	var search_areaId = $("#type_hidden").val();
    drawCustomExtendVector.getSource().clear();//清空红色边框    
    if ( search_areaId == 1 ) {
        //选中了地区直接显示地区的
        //showAreaZoomByAreaId(search_areaId)    	
        var geom = (new ol.format.GeoJSON()).readGeometry(cacheAreaRange);
        var feature = new ol.Feature(geom);
        var source = new ol.source.Vector({
            features: [feature],
            wrapx: false
        })

        var polygon = (source.getFeatures()[0].getGeometry());
        var size = (map.getSize());

        drawCustomExtendVector2.getSource().addFeature(feature);        
        map.getView().fit(polygon, size);

    } else if (search_areaId == 2 || search_areaId ==3) {//自定义
        //组装点    	
    	var bounds = $("#geom_hidden").val();
		var geom2 = (new ol.format.GeoJSON()).readGeometry(bounds);
	    var feature2= new ol.Feature(geom2);	    		    
	    var source2 = new ol.source.Vector({
            features: [feature2],
            wrapx: false
        })
        var polygon2 = (source2.getFeatures()[0].getGeometry());
        var size2 = (map.getSize());
        drawCustomExtendVector2.getSource().addFeature(feature2);       
        map.getView().fit(polygon2, size2);        
    } else {
        //如果上面都没有就选中全国的默认范围
       // map.moveTo(new OpenLayers.LonLat(105.410562, 31.209316), 4);
        map.getView().fit([105.410562, 31.209316],4);
    }

}

/**
 *检索
 * 
 */
var search_areaId = ""; //选中的地区编号
var search_geom = ""; //选中的范围坐标点
var imageMap = new Map();	
var detailMap = new Map();//用于保存检索出来数据
var chooseAreaType = 1;//行政区域为1,2为经纬度
function submitSearch(){
	
	var  startDate   = $("#startDate").val(),//开始日期
		   endDate    = $("#endDate").val(),//最后日期
		   clound       = $("#amount").text(),//云量
		   provinceId = $("#show_index_local").attr("areaid"),		
		   cityId         =  $("#show_index_local").attr("cityId"),
		   countyId    =  $("#show_index_local").attr("CountyId"),
		   showText   = $("#show_index_local").text(),
		   geom         = $("#geom_hidden").val(),//判断自定义传参
		   resolutAttr  = $("#resolution_option").attr("text"),
		   resolution  = $("#resolution_option").text();//分辨率	
	if(cityId != undefined){
		provinceId =cityId;
	}
	var areaName_hidden = $("#areaName_hidden").val();
	if(areaName_hidden == 1){	

		if(showText == "请选择"){			
			alert("请选择区域!");
			return;	
		}
	}else if(areaName_hidden =="3"){//经纬度
		if("" == $("#ll_in1").val() || "" == $("#ll_in2").val() || "" == $("#ll_in4").val() || "" == $("#ll_in3").val()){
			alert("请输入完整的经纬度");
			return;
		}
	}
	
	if(resolution == "请选择"){
		alert("请选择分辨率!");
		return;
	}
	
	cleanAllFeatures();
	selectMap.getFeatures().clear();
	$(".table-con").addClass("active");
	$(".table-con,.dc-ad-drag").show();
	$("#map").css({"width":"53.1%"});
	map.updateSize();
	showSearchSelectZoom();
	
	//判断请求，1为行政区域，2自定义，3shp文件
	
	var typePost = $("#type_hidden").val(),params;  

	var url = host+"/areaImage/query";
	if(typePost == 1){		
		params  = {
				areaNo:provinceId,		
				imageSatelliteType:resolutAttr,			
				startDate:startDate,
				endDate:endDate,
				startcloudsRange:"0",			
				endcloudsRange : clound,				
		};
	}else if(typePost == 2){		
		
		params  = {				
				imageSatelliteType :resolutAttr,			
				startDate:startDate,
				endDate:endDate,
				startcloudsRange:"0",			
				endcloudsRange : clound,
				geom:geom
		};
	}else{//shp
		params  = {				
				imageSatelliteType :resolutAttr,			
				startDate:startDate,
				endDate:endDate,
				startcloudsRange:"0",			
				endcloudsRange : clound,		
				geom:geom
		};
	}

	$("#waiting").show();
	$(".mask").show();
	var tableInit = $('#data-grid')
	 .on('xhr.dt', function ( e, settings, json, xhr ) {
		 		json.draw =0;
		 		json.recordsFiltered=json.total;
		 		json.recordsTotal=json.total;
	 } )
	.DataTable( {
		serverSide: true,
		searching:false,
		bPaginate:false,
		lengthChange:false,
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
            "sProcessing": "正在加载数据..."            
        },
		ajax:{
				url :url,
				type: "post", 
				data : params,
				dataSrc: "list",	
				/*error: function(){
					$(".data-grid-error").html("");
					$("#data-grid").append('<tbody class="data-grid-error"><tr><th colspan="10" style="text-align:center;">没有数据</th></tr></tbody>');
					$("#data-grid_processing").css("display","none");
				}*/
		},
		columns:[
            {"data":null,"width":"5%"},
            {"data":null,"width":"10%"},
            {"data":null,"width":"24%"},
            {"data":"image_satellite_type","width":"12%"},
            {"data":"image_sensor_type","width":"10%"},
            {"data":"begin_time","width":"16%"},
            {"data":"image_resolution","width":"12%"},
            {"data":null,"width":"12%"}
        ],
        columnDefs:[
            {
            	targets:0,
            	"width": "5%",
            	render:function(data,type,row){
            		var obj = interStringName(data);
            		return '<input data-index="0" name="btSelectItem" type="checkbox"  id="'+obj.sensor+'_image_checkbox_'+obj.id+'"  id2="image_checkbox_'+obj.id+'"  dataid="'+obj.id+'" onclick="selectFeatureByFid(this)">'
            	}
            },
            {
            	targets:1,
            	"width": "10%",
            	render:function(data,type,row){
            		var obj = interStringName(data);
            		return '<img id="'+data.data_id+'" title="点击查看拇指图和数据详情" class="muzhimg" src="http://www.rscloudmart.com/image/'+data.thumbnail_path+'_98_98.jpg?op=OPEN" onclick="detailImage(this)">'
            	}
            },
            {
            	targets:2,
            	"width": "24%",
            	"class":"tdId",
            	render:function(data,type,row){            		
            		return data.image_product_id;
            	}
            },
            {targets:3,"width": "12%"},
            {targets:4,"width": "10%"},
            {	
            	targets:5,"width": "16%",
            	render:function(data,type,row){
            		var time = data;
            		time = time.split(" ");
            		return time[0];
            	}
            },
            {
            	targets:6,"width": "12%",
            	render:function(data,type,row){
            		var resolution = data.replace("{","").replace("}","");//分辨率
        		    var resolutions = resolution.split(",");
        		    if(resolutions.length >1){
        		    	resolution = resolutions[0]+","+resolutions[1];
        		    }else{    	
        		    	resolution = resolutions[0];    
        		    }
            		return resolution+"m";
            	}
        	},
            {
        		targets:7,"width": "12%",
        		render:function(data,type,row){            		
            		return data.image_cloudage+"%"
            	}
        	}        
        ],        
		scrollY: 600,//表格高度
		deferRender: true,
		scrollCollapse: true,
		scroller: {
		    loadingIndicator: true
		},
		destroy:true, 
		initComplete: function(settings, json) {
			$("#waiting").hide();
			$(".mask").hide();
			$(".totalNum").text(json.total);
			var len = json.list.length;					    
			for(var i = 0;i<len;i++){	
				var jsonObj = json.list[i],
					 id = jsonObj.id,
					geom = jsonObj.range;
				imageMap.put(id, geom);		
				//画出蓝色边框	
				showChildDataExtend(id,geom);
				
			}
			
		}
    } );	

	//tableInit.ajax.reload();		
}

/**
 * 画出区域影像显示结果(小蓝框)
 * @param id--->标识ol3矢量id
 * @param range--->geojson 坐标
 * */
function showChildDataExtend(id, range) {   	       
    var geom = (new ol.format.GeoJSON()).readGeometry(range),    
    	  feature = new ol.Feature(geom),
    	  source = new ol.source.Vector({
              features: [feature],
              wrapx: false
          });
    feature.setId(id); //根据要素id删除添加切换
    //hoverFeature = feature;
    vectors2.getSource().addFeature(feature);
}


//提交任务
function resultSubmit(){	
	
	var startDate   = $("#startDate").val(),//开始日期
	   	  endDate    = $("#endDate").val(),//最后日期
		  datatype          = $.trim($("#resolution_option").text()),
		  id              =[],	
		  area			  = $("#show_index_local").text(),
		  areaid		  =	$("#show_index_local").attr('areaid'),
		  cityid		  =	$("#show_index_local").attr('cityid'),
		  countyid		  =	$("#show_index_local").attr('countyid'),	  
		  areatype        = "",
		  areaGeom ="",
		  flag           = $("#type_hidden").val();//判断标识 1为行政区域，2为自定义
	//判断是否有检索	
	if(!$(".table-con").hasClass("active")){
		return;
	}
	
	
	if(flag != 1){
		areatype ="自定义区域";
		areaGeom = $("#geom_hidden").val();
	}else{
		areatype ="行政区域";
		areaGeom = null;
	}	
	
	var areaName_hidden = $("#areaName_hidden").val();
	if(areaName_hidden == 1){
		if(area == "请选择"){
			alert("请选择区域!");
			return;
		}		
	}else if(areaName_hidden =="3"){//经纬度
		if("" == $("#ll_in1").val() || "" == $("#ll_in2").val() || "" == $("#ll_in4").val() || "" == $("#ll_in3").val()){
			alert("请输入完整的经纬度");
			return;
		}
	}
	
	if(datatype == "请选择"){
		alert("请选择分辨率!");
		return;
	}
	
	var SelectFalse = false; //用于判断是否被选择条件
	var chboxValue = [];
	var CheckBox = $(".sorting_1 input");//得到所的复选框
	for(var i = 0; i < CheckBox.length; i++){	
		if(CheckBox[i].checked)//如果有1个被选中时
		{
			SelectFalse = true;
			chboxValue.push(CheckBox[i].value)//将被选择的值追加到
		}
	}

	if(!SelectFalse){
		alert("至少要选一项");
		return false
	}

	$(".table-con").removeClass("active");
	$(".tdId").each(function(i,c){
		var thisTr =$(c).siblings(".sorting_1").find("input"),			 
			  thisId = thisTr.attr("dataid");
		if(thisTr.hasClass("active")){			
			id.push(thisId);
		}
	})
	
	var returnObj ={
		"id":id,
		"areatype":areatype,
		"area":area,
		"areaid":areaid,
		"cityid":cityid,
		"countyid":countyid,
		"radio":flag,
		"startDate":startDate,
		"endDate":endDate,
		"dataType":datatype,
		"areaGeom":areaGeom,
		"disabled":"true",
	};
	returnObj = JSON.stringify(returnObj);	
	sessionStorage.setItem("data",returnObj);
	window.location = "/taskSubmission.html";
	
}

/**影像详情*/
function detailImage(obj){
	$("#imageModal").modal("show");
	var ids = obj.id;
	//var json = detailMap.get(ids);

	$.ajax({
		url :host+"/areaImage/queryDetail/"+ids,
		type: "post",
		success:function(json){
			json=JSON.parse(json);
			var el = document.getElementById("imageInfos");
			el.innerHTML ="";
			el.innerHTML +='<p>行列号：'+json.image_row_col+'</p>';
			el.innerHTML +='<p>卫星：'+json.image_satellite_type+'</p>';
			el.innerHTML +='<p>影像最小分辨率：'+json.image_resolution+'</p>';
			el.innerHTML +='<p>光谱：'+json.image_spectrum_type_display+'</p>';
			el.innerHTML +='<p>开始时间：'+json.image_start_time+'</p>';
			el.innerHTML +='<p>更新时间：'+json.image_center_time+'</p>';
			el.innerHTML +='<p>数据类型：'+json.image_product_type+'</p>';
			el.innerHTML +='<p>名称：'+json.name+'</p>';
			el.innerHTML +='<p>云量：'+json.image_cloudage+'</p>';
			el.innerHTML +='<p>传感器类型：'+json.image_sensor_type+'</p>';
			el.innerHTML +='<p>影像级别：'+json.image_product_level+'</p>';
			el.innerHTML +='<p>采集开始时间：'+json.image_take_time +'</p>';
			/*el.innerHTML +='<p>产品序列号：'+json.imageRowCol+'</p>';*/
			el.innerHTML +='<p>影像面积：'+json.image_area+'</p>';
			el.innerHTML +='<p>数据采集结束时间：'+json.image_end_time+'</p>';        		
			$(".photos").html('<img src=http://www.rscloudmart.com/image/'+json.thumbnail_path+'_800_800.jpg?op=OPEN" width="100%" height="450px">');	
		}
	});
	
}


/**
 * 截取接口queryproductInformationByAreaName.do name字符串
 * 
 * */
function interStringName(item){
	var idh ="",cgq =null;
	
	//根据内容判断使用什么分隔符
    var splitStr = "_";

    if(item.name.indexOf(splitStr) == -1 && item.name.indexOf("-") != -1) {
        splitStr = "-";
    }
    var names = item.name.split(splitStr);
    
	if(!!item.imageSatelliteType&&(item.imageSatelliteType == "PL"||item.imageSatelliteType == "pl")){   //PL信息截取        
        if(names[3]!="planet"){ 
        	idh = names[0]+"_"+names[1]+"_"+names[2]+"_"+names[3];
        }else{
        	idh = names[0]+"_"+names[1]+"_"+names[2];	
        }
        cgq=item.image_sensor_type?item.image_sensor_type:'-';     
    }else if(!!item.imageSatelliteType&&(item.imageSatelliteType == "Terra"||item.imageSatelliteType == "Aqua")){
        var modisNames = names[0].split(".");        
        var idhName=modisNames[1]+modisNames[2]+modisNames[3];
        idh = parseFloat(idhName.substring(idhName.indexOf("A")+1,idhName.length));
        cgq=names[0];        
    }else{        
        idh = parseFloat(names[5].substring(names[5].indexOf("0"),names[5].length));
        cgq=names[0];
   }             
    var returnObj = {
    		"id":item.id,
    		"idh":idh,
    		"sensor":cgq
    };
    return returnObj;
}

/**
 *点击复选框，在地图上显示位置
 * @param id
 */
var selectNum = 0;
function selectFeatureByFid(obj) {
	selectMap.getFeatures().clear();
	var dataid = $(obj).attr("dataid");
	if($(obj).prop("checked") == true){
		$(obj).addClass("active");
		$(obj).parents("tr").addClass("active-blue").siblings("tr").removeClass("active-blue");		
		blueLightId = $(obj).parents("tr").attr("id");
		//去到该影像位置
		var b = imageMap.get(dataid).geom;
		var geom = (new ol.format.GeoJSON()).readGeometry(b);
		var feature = new ol.Feature(geom);
		feature.setId(dataid);
				
		//if(sun.hasClass("active")){
			vectors2.getSource().getFeatureById(dataid).setStyle(
				new ol.style.Style({
					stroke: new ol.style.Stroke({
						color: 'rgba(187,0,0,1)',
						width: 2
					})
				})
			)
		//}
	}else{
			$(obj).removeClass("active");
			blueLightId = null;
			$(obj).parents("tr").removeClass("active-blue");		
			//if(sun.hasClass("active")){
				vectors2.getSource().getFeatureById(dataid).setStyle(
					new ol.style.Style({
						stroke: new ol.style.Stroke({
							color: 'rgba(32,70,250,0.3)',
							width: 2
						})
					})
				)
			//}	
	}
}

/**
 * 地图交互操作
 * @param function vectors2ed()
 * */
var selectMap;
function selectNew(){		
	selectMap = new ol.interaction.Select({
        toggleCondition: ol.events.condition.singleClick,
        layers:[vectors2,newVectors]
    });
    map.addInteraction(selectMap);
    
    selectMap.on('select', function(e) {  
    	//var container = $('#data-grid');
    	var container = $('.dataTables_scrollBody');    	    	
        //选中
        if(e.selected.length>0) {        	
	        	var featureId =  e.selected[0].getId();	    	        	
	        	if(newVectors.getSource().getFeatures().length == 0){
	        		vectors2ed(featureId,container);
	        	}
	        }else if(e.deselected.length>0){//不选中	        	
	        	var featureId2 =  e.deselected[0].getId();	        	
		        if(newVectors.getSource().getFeatures().length == 0){
			    	  vectors2ed(featureId2,container);
			    }
        }
    });
}

function vectors2ed(featureId,container){
	var sa2 = imageMap.get(featureId),
    checked2 = $("input[id2=image_checkbox_"+featureId+"]")
	if(checked2.prop("checked") == true ){		
    	vectors2.getSource().getFeatureById(featureId).setStyle(
		        new ol.style.Style({
		            stroke: new ol.style.Stroke({
		            	color: 'rgba(32,70,250,0.3)',
		                width: 2
		            })
		        })
		    )
    	checked2.prop("checked", false);	
		checked2.parents("tr").removeClass("active-blue");
	    blueLightId = null;
	    container.scrollTop(//列表定位到当前选中的行
	            checked2.offset().top - container.offset().top + container.scrollTop() - 50
	    );	    
	
	}else {
	    $("input[id2=image_checkbox_"+featureId+"]").prop("checked", true);
	    //blueLightId = sa2 + "_image_tr_" + featureId; //保存当前高亮行
	    checked2.parents("tr").addClass("active-blue").siblings("tr").removeClass("active-blue");
	    container.scrollTop(//列表定位到当前选中的行
	            checked2.offset().top - container.offset().top + container.scrollTop() - 50
	    );
	    vectors2.getSource().getFeatureById(featureId).setStyle(
	        new ol.style.Style({
	            stroke: new ol.style.Stroke({
	            	 color: 'rgba(187,0,0,1)',
	                width: 2
	            })
	        })
	    )	    	   
	}	  
}

/**
 * 限制input输入数字和一个小数点
 * 
 * @param obj
 *            this
 */
function clearNoNum(obj) {	
	//先把非数字的都替换掉，除了数字和.
	obj.value = obj.value.replace(/[^\d.\-]/g,"");
	//保证只有出现一个.而没有多个.
	obj.value = obj.value.replace(".","$#$").replace(/\./g,"").replace("$#$",".");
	//保证只有出现一个-而没有多个-
	obj.value = obj.value.replace("-","$#$").replace(/\-/g,"").replace("$#$","-");
	
	//- 只能是第一个
	if(obj.value.indexOf("-") != 0){
		obj.value = obj.value.replace("-","");
	}
		
	//验证大小
	var id = obj.id;
	var value = parseFloat(obj.value)
	if(id == "ll_in1" || id == "ll_in2"){
		if(-180 >= value){
			obj.value = "-180";
		}
		if(180 <= value){
			obj.value = "180";
		}		
 	}else{
		if(-90 >= value){
			obj.value = "-90";
		}
		if(90 <= value){
			obj.value = "90"
		}
	}
	
}

//显示等待
function ShowDIV(thisObjID) {
    $("#BgDiv").css({ display: "block", height: $(document).height() });    
    $("#" + thisObjID ).show();    
}

function closeDiv(thisObjID) {
    $("#BgDiv").css("display", "none");
    $("#" + thisObjID).css("display", "none");
    $(".olMap").removeClass("active");
}





function Map() {
	 var struct = function(key, value) {
		 this.key = key;
		 this.value = value;
	 }
 
	 var put = function(key, value){
		  for (var i = 0; i < this.arr.length; i++) {
			   if ( this.arr[i].key === key ) {
				    this.arr[i].value = value;
				    return;
			   }
		  }
		   this.arr[this.arr.length] = new struct(key, value);
	 }
 
	 var get = function(key) {
		  for (var i = 0; i < this.arr.length; i++) {
			   if ( this.arr[i].key === key ) {
			     return this.arr[i].value;
			   }
		  }
		  return null;
	 }
 
	 var remove = function(key) {
		  var v;
		  for (var i = 0; i < this.arr.length; i++) {
			  v = this.arr.pop();
			   if ( v.key === key ) {
			    continue;
			   }
			   this.arr.unshift(v);
		  }
	 }
 
	 var size = function() {
		 return this.arr.length;
	 }
 
	 var toArray = function() {
		 var array = new Array();
		 for (var i = 0; i < this.arr.length; i++) 
		 {
			 array.push(this.arr[i].value);
		 }
		 return array;
	 }
 
	 var isEmpty = function() {
		 return this.arr.length <= 0;
	 } 
	 this.arr = new Array();
	 this.get = get;
	 this.put = put;
	 this.remove = remove;
	 this.size = size;
	 this.toArray = toArray;
	 this.isEmpty = isEmpty;
}

/**
 * 清空所有的多边形
 */
function cleanAllFeatures()
{
    if (drawCustomExtendVector.getSource().getFeatures().length > 0)
    {
        drawCustomExtendVector.getSource().clear();
    }
    if (drawCustomExtendVector2.getSource().getFeatures().length > 0)
    {
        drawCustomExtendVector2.getSource().clear();
    }
    
    if (vectors2.getSource().getFeatures().length > 0)
    {
    	vectors2.getSource().clear();
    }
}