$(function(){
	bindInputchange();
})

function bindInputchange(){
	$("#shpModal .shp input").change(
			function() {
				var r = Math.random();
				var url = "http://127.0.0.1:8099/uploadShpFile?r="+r;
				var uplist = $("#shpModal .shp input[name^=file]");
				var arrId = [];
				for ( var i = 0; i < uplist.length; i++) {
					if (uplist[i].value) {
						arrId[i] = uplist[i].id;
					}
				}
				
				$.ajaxFileUpload({
					url : url,
					secureuri : false,
					fileElementId : arrId,
					dataType : 'json',
					success : function(data) {
						if (data.code == 1) {

							var format=new ol.format.WKT();
							var wkt=format.readFeature(data.data);//wkt 等于Feature
							var geoJson =new ol.format.GeoJSON();//转化wkt格式为geoJson
							var fGeojson = geoJson.writeFeature(wkt);							
							var splits = fGeojson.split("geometry");							
							/*var feature=new ol.Feature(fGeojson);*/
							drawCustomExtendVector.getSource().addFeature(wkt);		
							var size=map.getSize();							
							map.getView().fit(wkt.getGeometry(),size);
							$("#shpModal").modal("hide");
							
							var Coordinates= JSON.stringify(wkt.getGeometry().getCoordinates()),
						      	 splitdata  = '{"type":"Polygon","coordinates":'+Coordinates+'}';
							$("#geom_hidden").val(splitdata);
							$("#show_index_local").text("自定义区域");
							$("#select-range-regions").prop("checked",true);
							$("#type_hidden").val("3");
						} else {
							alert(data.message);
						}
					},
					complete : function(data) {
						$("#shpModal .shp ").empty();
						$("#shpModal .shp  ").html('<a href="javascript:;" class="schuan"><input type="file" name="file" id="imgFile">上传本地shp文件</a>');
						bindInputchange();
					},
					error: function (data, status, e) {  
		                alert(e);  
		            }
				});
	});
	$("#shpdownloads").click(function(){
		var r = Math.random();
		var feature = wfst.getFeatureByFid("graph");
		if(feature==null){
			alert("地图中无检索使用的几何对象！");
			return false;
		}
		var geomWKT = wkt_c.write(feature);
		var url = "Exportshpfile?r=" + r;
		var form = $("<form>"); // 定义一个form表单
		form.attr('style', 'display:none'); // 在form表单中添加查询参数
		form.attr('target', '');
		form.attr('method', 'POST');
		form.attr('action', url);
		// <input name="uploads" id="imgFile"type="file" />
		var input = "<input type=\"text\" name=\"geomWKT\" value=\"" + geomWKT
				+ "\" />";//
		$(input).appendTo(form);
		$('body').append(form); // 将表单放置在web中
		form.submit();
	});
}