/**
 * Created by Administrator on 2017/3/9.
 */
// JavaScript Document
/**
 * 根据正坤的地区的webservice得到
 */
var CheckedSelectedArea = null;//存放区域areid
var areaList = "";// 存放行政区域的json数据
// var manageHost="http://58.252.5.13:8080";//webservice的ip地址
var manageHost2 = "http://210.77.87.225:8084";// webservice的ip地址
var specialAreaStr = "北京1市_天津1市_上海1市_重庆1市";// 特殊地区集 目前特殊地区集只存在于市一级地区


$(document).ready(function(){    
    // 初始化地区选择框中的一系列相关控件的事件
    setAreaSelectedDivFun(true);
    // 初始化省份一级的地区选择
    initProvinceDiv();

});

/**
 * 获取请求url的参数值
 * @param name   等号左边的key
 * @returns
 */
function isLastPathname(pathname) 
{    
    var pathnames = window.location.pathname.split("/");
    if(pathnames[pathnames.length-1] == pathname) return true;
    else return false;    
}

/**
 * 得到CheckedSelectedArea保存的最小范围的地区Id
 *
 * @returns
 */
function getSelectedLitteAreaName() {
    var str = ""
    if (CheckedSelectedArea.hasOwnProperty("town")) {
        str += CheckedSelectedArea.pro.proname + "/";
        str += CheckedSelectedArea.city.cityname + "/";
        str += CheckedSelectedArea.town.townname;
    } else if (CheckedSelectedArea.hasOwnProperty("city")) {
        str += CheckedSelectedArea.pro.proname + "/";
        str += CheckedSelectedArea.city.cityname;
    } else if (CheckedSelectedArea.hasOwnProperty("pro")) {
        str += CheckedSelectedArea.pro.proname
    } else {
        str = "请选择";
    }

    return str;
}

/**
 * 初始化地区选择框中的一系列相关控件的事件\
 *
 * @param falg
 *            点击了按钮，是否触发筛选数据的事件
 */
function setAreaSelectedDivFun(flag) {

    // 选择地区的确定按钮事件
    $(".address-confirm").click(function() {
        $(".each-input input").val("");
        $("#government_list,.datapro_index_arealist").hide();
        $(".rihgt-bar-li1").removeClass("active");
        if (undefined != selectedArea && undefined != selectedArea.pro) {
            totalAddress = selectedArea.pro.proname;
            if (undefined != selectedArea.city) {
                totalAddress += selectedArea.city.cityname;
            }
            if (undefined != selectedArea.town) {
                totalAddress += selectedArea.town.townname;
            }
        }
        CheckedSelectedArea = selectedArea;
        $(".current-area").removeClass("active");

        var currentAdd_show = "";
        $(".result-item").each(function(i) {
            if (i < 3) {
                // alert("隐藏?"+$(this).is(":hidden"));
                if ($(this).css("display") == "inline") {
                    if (i > 0) {
                        currentAdd_show += "/"
                            + $(this)
                                .text();
                    } else {
                        currentAdd_show += $(
                            this).text();
                    }
                }
            }
        });
        if (currentAdd_show == "") {
            currentAdd_show = "请选择";
        }
        $(".localposition span,.dc-sdsc-area span").text(currentAdd_show);
        var area_a = $(this).parents(".datapro_index_arealist").siblings("a.datapro_index_select");
        var text_old = $("#show_index_local").html();
        var text_new = "";
        // $(area_a).children("span").attr("title",text_old);
        $("#show_index_local").attr("title", text_old);

        if (text_old.length > 15) {
            text_old = text_old.slice(0, 15);
            $("#show_index_local").text(text_old + "...");
        }


        /*if(!!jump_isShp&&jump_isShp == "true"){
            isShp = true;
            jump_isShp = false;
        }else{
            isShp = false;
        }*/

       
        head_chooseAreaId = getSelectedLitteAreaId();
        head_chooseAreaName = getSelectedLitteAreaName();
        $("#show_index_local").attr("areaId", head_chooseAreaId);
        $("#show_index_local").attr("areaName", head_chooseAreaName);

        /*console.log(head_chooseAreaId + '---'+ head_chooseAreaName);*/

        $("#select-range-regions").prop("checked",true);
        //隐藏选择框
        $("#government_list").removeClass('show');
        $("#type_hidden").val("1");      
        $("#areaName_hidden").val("1");
        //在地图显示区域范围
        getSelectedDataParams();
        
    });
   

   
    // 省市切换选择
    $(".result-item").click(function() {
        if ($(this).attr("data-attr") == "province") {
            delete selectedArea.pro;
            delete selectedArea.city;
            delete selectedArea.town;
            // 点击列表顶端的town存放标签后,清除town列表激活样式
            $(".select-province li").removeClass("active");
        } else if ($(this).attr("data-attr") == "city") {
            delete selectedArea.city;
            delete selectedArea.town;
            // 点击列表顶端的town存放标签后,清除town列表激活样式
            $(".select-city li").removeClass("active");
        } else if ($(this).attr("data-attr") == "town") {
            delete selectedArea.town;
            // 点击列表顶端的town存放标签后,清除town列表激活样式
            $(".select-town li").removeClass("active");
        }
        $(this).hide().next(".result-item").hide().next(".result-item").hide();
        var thisAttr = $(this).attr("data-attr");
        $(".select-" + thisAttr).show().siblings(".select-item").hide();
        $(".please-select").show();
    });
    // 关闭地区选择列表
    $(".close-areaselect-dataOrder").click(function() {
        $("#government_list,.datapro_index_arealist").hide();
        // $("#government_list").hide();
        $(".current-area").removeClass("active");
        $(".rihgt-bar-li1").removeClass("active");
    });

    // 改版后行政区域控件
    $(".sel-pro-city-town li").on("click",function() {
            var forDiv = $(this).attr("data-attr");
            $(this).addClass("active").siblings("li").removeClass("active");
            $(".government-list-" + forDiv).show().siblings(".government-list-eacharea").hide();
    });
}

/**
 * 初始化省份一级的地区选择
 */
function initProvinceDiv() {
    // 检查全局变量areaList中是否已保存省份地区列表
    var proflag = checkHasAreaList(0);    
    getAreaListDivFromWebService(0);    
}

/**
 * 初始化市级的地区选择 并设置全局变量中所选择的省份
 *
 * @param proId
 *            市级的地区所在省份的id
 * @param proname
 *            市级的地区所在省份的名称
 * @param event
 * @returns
 */
function initCityDiv(proId, proname, event) {
    $(event).parent().addClass("active").siblings().removeClass("active");
    selectedArea = {
        pro : {
            proid : proId,
            proname : proname
        }
    };

    var a = selectedArea.pro.proname.indexOf("省");
    if (a == -1) {
        a = selectedArea.pro.proname.indexOf("市");
        if (a == -1) {
            a = selectedArea.pro.proname.indexOf("特");
            if (a == -1) {
                if (selectedArea.pro.proname == "内蒙古自治区")
                    a = 3;
                else
                    a = 2;
            }
        }
    }
    var pro1 = selectedArea.pro.proname.substring(0, a);

    $(".current-province").show().text(pro1);
    $(".select-province").hide();
    $(".select-city").show();

    // 检查全局变量areaList中是否已保存地区Id为proId/cityId子地区列表
    var cityContent = checkHasAreaList(1, proId);
    if (cityContent) {// 存在
        initCityDivFromcityList(cityContent);
    } else {// 不存在
        // 特殊地区判断
        var test = specialAreaStr.indexOf(proname);
        if (specialAreaStr.indexOf(proname) >= 0) {// 是特殊地区
            getSpecialAreaListFromWebService(0, proId, proname);
        } else {
            getAreaListDivFromWebService(1, proId);
        }

    }

}
/**
 * 初始化县/区级的地区选择 并设置全局变量中所选择的市
 *
 * @param cityId
 *            县/区级的地区所在市级的地区的id
 * @param cityname
 *            县/区级的地区所在市级的地区的名称
 * @param event
 * @returns
 */
function initTownDiv(cityId, cityname, event) {
    $(event).parent().addClass("active").siblings().removeClass("active");
    $(this).parent().attr("class");
    selectedArea.city = {
        cityid : cityId,
        cityname : cityname
    };
    $(".current-city").show().text(selectedArea.city.cityname);
    if (!isLastPathname("imageMosaic")) {

        $(".select-city").hide();
        // 检查全局变量areaList中是否已保存地区Id为proId/cityId子地区列表
        var townContent = checkHasAreaList(2, selectedArea.pro.proid, cityId);
        /*
         * if(townContent){//存在 initTownDivFromcityList(townContent);
         * }else{//不存在
         */getAreaListDivFromWebService(2, selectedArea.pro.proid, cityId);
        // }
        $(".select-town").show();
    } else {
        $(".please-select").hide();
    }
}
/**
 * 设置全局变量中所选择的县/区级的地区
 *
 * @param townId
 *            所选择的的县/区级的地区的id
 * @param townname
 *            所选择的的县/区级的地区的名称
 * @param event
 * @returns
 */
function getTownSelected(townId, townname, event) {
    $(event).parent().addClass("active").siblings().removeClass("active");
    var test = $(this).parent();
    $(this).parent().parent().attr("class");
    selectedArea.town = {
        townid : townId,
        townname : townname
    };
    $(".current-town").show().text(selectedArea.town.townname);
    $(".please-select").hide();
}

/**
 * 根据areaList的内容初始化省级地区的选择框
 */
function initProvinceDivFromAreaList() {
    // 根据areaList的内容初始化省份一级的地区选择
    if (undefined != areaList.prolist && areaList.prolist.length > 0) {
        var prostr = "";
        for (var i = 0; i < areaList.prolist.length; i++) {
            var pro = areaList.prolist[i];
            var a = pro.name.indexOf("省");
            if (a == -1) {
                a = pro.name.indexOf("市");
                if (a == -1) {
                    a = pro.name.indexOf("特");
                    if (a == -1) {
                        if (pro.name == "内蒙古自治区")
                            a = 3;
                        else
                            a = 2;
                    }
                }
            }
            var pro1 = pro.name.substring(0, a);
            prostr += '<li><a onclick=initCityDiv(' + pro.adminId + ',\"'
                + pro.name + '\",this)>' + pro1 + '</a></li>';
        }
        $(".province").html(prostr);
    }
}

/**
 * 根据ciytlist的内容初始化市级地区的选择框
 *
 * @param ciytlist
 * @returns
 */
function initCityDivFromcityList(ciytlist) {
    var citystr = "";
    if (undefined != ciytlist && ciytlist.length > 0) {
        for (var i = 0; i < ciytlist.length; i++) {
            citystr += '<li><a onclick=initTownDiv(' + ciytlist[i].adminId
                + ',\"' + ciytlist[i].name + '\",this)>' + ciytlist[i].name
                + '</a></li>';
        }
    }
    $(".city").html(citystr);
    $(".select-city").show();
}

/**
 * 根据ciytlist的内容初始化县/区级地区选择的框
 *
 * @param ciytlist
 * @returns
 */
function initTownDivFromtownList(townlist) {
    // 根据areaList的内容初始化省份一级的地区选择
    var townstr = "";
    if (undefined != townlist && townlist.length > 0) {

        for (var i = 0; i < townlist.length; i++) {
            townstr += '<li><a onclick=getTownSelected(' + townlist[i].adminId
                + ',\"' + townlist[i].name + '\",this)>' + townlist[i].name
                + '</a></li>';
        }
    }
    $(".town").html(townstr);
    $(".select-town").show();
}
/**
 * 检查全局变量areaList中是否已保存省份地区列表，或者是否已保存地区Id为proId/cityId子地区列表
 *
 * @param type
 *            0 检查全局变量areaList是否保存了省份地区的列表 1 检查全局变量areaList是否已保存省份Id为proId市级地区列表
 *            2 检查全局变量areaList是否已保存省份Id为proId,市级Id为cityId的县区级列表
 * @param proId
 * @param cityId
 * @returns 不存在 返回false 存在 返回地区列表
 */
function checkHasAreaList(type, proId, cityId) {
    // 根据areaList的内容初始化省份一级的地区选择
    var json = {
        hasAreaList : false
    };
    if (undefined != areaList.prolist && areaList.prolist.length > 0) {
        if (type == 0) {// 查找省级列表
            return true;
        } else {
            for (var i = 0; i < areaList.prolist.length; i++) {
                var pro = areaList.prolist[i];
                if (proId == pro.adminId) {
                    if (type == 1) {// 查找市级列表
                        if (undefined != pro.citylist
                            && undefined != pro.citylist.length
                            && pro.citylist.length > 0) {
                            return pro.citylist;
                        } else if (undefined != pro.citylist
                            && undefined != pro.noChildList) {
                            return pro.noChildList;
                        }
                    } else if (type == 2) {// 查找县/区级列表
                        if (undefined != pro.citylist
                            && undefined != pro.citylist.length
                            && pro.citylist.length > 0) {
                            for (var j = 0; j < pro.citylist.length; j++) {
                                var city = pro.citylist[j];
                                if (cityId == city.adminId) {
                                    if (undefined != city.townlist
                                        && undefined != city.townlist.length
                                        && city.townlist.length > 0) {
                                        return city.townlist;
                                    } else {
                                        return false;
                                    }
                                }
                            }
                        } else {
                            return false;
                        }
                    }
                }
            }
        }
    } else {
        return false;
    }
}

/**
 * 访问gt-cloud的webservice得到不同级别的地区列表，并初始化相应的地区选择区域
 *
 * @param type
 *            0 得到省级列表，1 得到市级列表，2 得到县/区级列表
 * @param fatherId
 */
function getAreaListDivFromWebService(type, proId, cityId) {
    var fatherId = "";
    if (type == 1) {
        fatherId = proId;
    } else if (type == 2) {
        fatherId = cityId;
    }
    var r = Math.floor(Math.random() * 9999 + 1);
    // var url=manageHost+"/mapsrv/services/rest/geoService/getAreaList?r="+r+"&_type=json&key=&fatherId="+fatherId+"&geomType=1&accuracy=2&_jsonp=?";
    var url = manageHost + "/area/getAreaList?fatherId=" + fatherId;
    
    $.getJSON(url,
        function(data) {
            if (data.code == 1) {
                if (type == 0) {
                    areaList = {
                        prolist : data.list
                        // 省份列表
                    }
                    initProvinceDivFromAreaList();
                } else if (type == 1) {
                    initCityDivFromcityList(data.list);
                    if (undefined != areaList.prolist
                        && areaList.prolist.length > 0) {
                        for (var i = 0; i < areaList.prolist.length; i++) {
                            if (areaList.prolist[i].adminId == proId) {
                                areaList.prolist[i].citylist = data.list;
                                break;
                            }
                        }
                    }
                } else if (type == 2) {
                    initTownDivFromtownList(data.list);
                    if (undefined != areaList.prolist
                        && areaList.prolist.length > 0) {
                        for (var i = 0; i < areaList.prolist.length; i++) {
                            if (areaList.prolist[i].adminId == proId) {
                                var pro = areaList.prolist[i];
                                if (undefined != pro.citylist
                                    && pro.citylist.length > 0) {
                                    for (var j = 0; j < pro.citylist.length; j++) {
                                        if (pro.citylist[j].adminId == cityId) {
                                            areaList.prolist[i].citylist[j].townlist = data.list;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (data.code == 0) {
                if (type == 1) {
                    if (undefined != areaList.prolist
                        && areaList.prolist.length > 0) {
                        for (var i = 0; i < areaList.prolist.length; i++) {
                            if (areaList.prolist[i].adminId == proId) {
                                areaList.prolist[i].noChildList = true;
                                $(".city").html("");
                                break;
                            }
                        }
                    }
                } else if (type == 2) {
                    if (undefined != areaList.prolist
                        && areaList.prolist.length > 0) {
                        for (var i = 0; i < areaList.prolist.length; i++) {
                            if (areaList.prolist[i].adminId == proId) {
                                var pro = areaList.prolist[i];
                                if (undefined != pro.citylist
                                    && pro.citylist.length > 0) {
                                    for (var j = 0; j < pro.citylist.length; j++) {
                                        if (pro.citylist[j].adminId == cityId) {
                                            areaList.prolist[i].citylist[j].noChildList = true;
                                            $(".town").html("");
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });
}
/**
 * 获取特殊地区的子地区列表
 *
 * @param type
 *            specialAreaStr="北京市_天津市_上海市_重庆市"
 *            目前这四个直辖市因在数据库中存在“县”，“直辖区”两个类别的地区，此方法是将“县”，“直辖区”两个类别的地区合并为一个地区列表
 * @param proId
 * @param proname
 * @param cityid
 * @param cityname
 */
function getSpecialAreaListFromWebService(type, proId, proname, cityid, cityname) {
    var fatherId = "";
    if (type == 0) {
        fatherId = proId;
    } else if (type == 1) {
        fatherId = cityId;
    }
    var r = Math.floor(Math.random() * 9999 + 1);
    var url = manageHost + "/dataProduct/getAreaList?fatherId=" + fatherId;
    $.getJSON(url, function(data) {
        if (data.code == 1) {// 目前的特殊地区将其子地区划分为县/直辖区两种不同类型的地区，需要将此两种不同的地区合并到一起
            var adminIds = [ data.list[0].adminId, data.list[1].adminId ];
            var datalist = null;
            r = Math.floor(Math.random() * 9999 + 1);
            $.getJSON(manageHost + "/dataProduct/getAreaList?fatherId="
                + adminIds[0], function(arealist1) {
                if (arealist1.code == 1) {
                    datalist = arealist1.list;
                    r = Math.floor(Math.random() * 9999 + 1);
                    $.getJSON(manageHost
                        + "/dataProduct/getAreaList?fatherId="
                        + adminIds[1], function(arealist2) {
                        if (arealist2.code == 1) {
                            for (var i = 0; i < arealist2.list.length; i++) {
                                datalist.push(arealist2.list[i]);
                            }
                            initCityDivFromcityList(datalist);
                            for (var i = 0; i < areaList.prolist.length; i++) {
                                if (areaList.prolist[i].adminId == proId) {
                                    areaList.prolist[i].citylist = datalist;
                                    break;
                                }
                            }
                        }
                    });
                }
            });
        }
    });
}




/**
* 得到CheckedSelectedArea保存的最小范围的地区Id
* 
* @returns
*/
function getSelectedLitteAreaId(){
    if(CheckedSelectedArea.hasOwnProperty("town")){
        return CheckedSelectedArea.town.townid;
    }
    if(CheckedSelectedArea.hasOwnProperty("city")){
        return CheckedSelectedArea.city.cityid;
    }
    if(CheckedSelectedArea.hasOwnProperty("pro")){
        return CheckedSelectedArea.pro.proid;
    }
}

/**
* 根据所选的地区及所选的数据类型后台发送申请，得到数据
* 
* 注：另producttypeid='b' 或者producttypeid='a' 就是页面初始化成功后信息产品、影像数据 模块中【查看更多】的功能
*/
function getSelectedDataParams(){
    // 得到CheckedSelectedArea保存的最小范围的地区Id
    var areaid=getSelectedLitteAreaId();    
    getSelectedDataParamswithareaId(areaid);
}

/**
* 传入areaid 根据所选的地区及所选的数据类型后台发送申请，得到数据
* 
* 注：另producttypeid='b' 或者producttypeid='a' 就是页面初始化成功后信息产品、影像数据 模块中【查看更多】的功能
*/
function getSelectedDataParamswithareaId(areaid){
    //根据地区id查询地区的面积和在地图上显示地图的范围
     //显示地区
    chooseAreaType = 1;    
    chooseAreaId = areaid;
    showAreaBounds(areaid);
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


function initJumpArea(areaId){
    var cityIndex,
        cityName,
        townIndex,
        townName;
    if(areaId.substring(4,6) != "00"){
        var proid = areaId.substring(0,2)+"0000";
        var cityid = areaId.substring(0,4)+"00";
        var townid = areaId;
    }
    else if(areaId.substring(2,6) != "0000"){
        var proid = areaId.substring(0,2)+"0000";
        var cityid = areaId;
    }
    else{
        var proid = areaId;
    }
    if(proid){
        var url = manageHost + "/area/getAreaList?fatherId=";
        $.getJSON(url,function(data) {
            if (data.code == 1) {
                areaList = {
                    prolist : data.list
                    // 省份列表
                }
                initProvinceDivFromAreaList();
                for (var i = 0; i < areaList.prolist.length; i++){
                    if (areaList.prolist[i].adminId == proid) {
                        initCityDiv(proid, areaList.prolist[i].name,this);
                        cityIndex = i;
                        cityName = areaList.prolist[i].name;
                        break;
                    }
                }
                if(cityid){
                    var url = manageHost + "/area/getAreaList?fatherId=" + proid;
                    $.getJSON(url,function(data) {
                        if (data.code == 1) {
                            initCityDivFromcityList(data.list);
                            if (undefined != areaList.prolist
                                && areaList.prolist.length > 0) {
                                for (var i = 0; i < areaList.prolist.length; i++) {
                                    if (areaList.prolist[i].adminId == proid) {
                                        areaList.prolist[i].citylist = data.list;
                                        break;
                                    }
                                }
                                for (var i = 0; i < areaList.prolist[cityIndex].citylist.length; i++){
                                    if (areaList.prolist[cityIndex].citylist[i].adminId == cityid) {
                                        initTownDiv(cityid,areaList.prolist[cityIndex].citylist[i].name,this);
                                        townIndex = i;
                                        townName = areaList.prolist[cityIndex].citylist[i].name;
                                        break;
                                    }
                                }
                            }
                            //getAreaListDivFromWebService(2, proid, cityid);

                            if(townid){
                                var url = manageHost + "/dataProduct/getAreaList?fatherId=" + cityid;
                                $.getJSON(url,function(data) {
                                    if (data.code == 1) {
                                        initTownDivFromtownList(data.list);
                                        var pro = areaList.prolist[cityIndex];
                                        if (undefined != areaList.prolist[cityIndex].citylist
                                            && areaList.prolist[cityIndex].citylist.length > 0) {
                                            for (var j = 0; j < areaList.prolist[cityIndex].citylist.length; j++) {
                                                if (pro.citylist[j].adminId == cityid) {
                                                    areaList.prolist[cityIndex].citylist[j].townlist = data.list;
                                                }
                                            }
                                            var cityList = areaList.prolist[cityIndex].citylist[townIndex];
                                            for (var i = 0; i < cityList.townlist.length; i++){
                                                if (areaList.prolist[cityIndex].citylist[townIndex].townlist[i].adminId == townid) {
                                                    getTownSelected(townid,areaList.prolist[cityIndex].citylist[townIndex].townlist[i].name,($(".town li").eq(i).find("a")));
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });

    }
}
