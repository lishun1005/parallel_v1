package com.rsclouds.common.utils;


import org.dozer.DozerBeanMapper;
import org.dozer.loader.api.BeanMappingBuilder;
import org.dozer.loader.api.TypeMappingOptions;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * Description:简单封装Dozer, 实现深度转换Bean<->Bean的Mapper.实现:1. 持有Mapper的单例;2. 返回值类型转换;3. 批量转换Collection中的所有对象;4.
 * 区分创建新的B对象与将对象A值复制到已存在的B对象两种函数
 *
 * @author JhYao 2014年10月20日
 *
 * @version v1.0
 *
 */
public class BeanMapper {

	/**
	 * 持有Dozer单例, 避免重复创建DozerMapper消耗资源.
	 */
	private static DozerBeanMapper dozer = new DozerBeanMapper();

	/**
	 * 基于Dozer转换对象的类型.
	 */
	public static <T> T map(Object source, Class<T> destinationClass) {
		if(source==null){
			return null;
		}else{
			return dozer.map(source, destinationClass);
		}
	}

	/**
	 * 基于Dozer转换Collection中对象的类型.
	 */
	public static <T> List<T> mapList(Collection sourceList, Class<T> destinationClass) {
		List<T> destinationList = new ArrayList<T>();
		for (Object sourceObject : sourceList) {
			T destinationObject = dozer.map(sourceObject, destinationClass);
			destinationList.add(destinationObject);
		}
		return destinationList;
	}

	/**
	 * 基于Dozer将对象A的值拷贝到对象B中.
	 */
	public static void copy(Object source, Object destinationObject) {
		dozer.map(source, destinationObject);
	}
	/**
	 *
	 * Description: null值不复制
	 *  @param sources
	 *  @param destination
	 * @author lishun
	 * @date 2017年8月16日
	 * @return void
	 */
	public static void copyProperties(final Object sources, final Object destination) {
		WeakReference weakReference = new WeakReference(new DozerBeanMapper());
		DozerBeanMapper mapper = (DozerBeanMapper) weakReference.get();
		mapper.addMapping(new BeanMappingBuilder() {
			@Override
			protected void configure() {
				mapping(sources.getClass(), destination.getClass(), TypeMappingOptions.mapNull(false),TypeMappingOptions.mapEmptyString(false));
			}
		});
		mapper.map(sources, destination);
		mapper.destroy();
	}
	public static void main(String[] args){
	}
}
