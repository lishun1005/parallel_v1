package com.rsclouds.gtparallel.dao;


import com.rsclouds.gtparallel.entity.CutJob;
import com.rsclouds.gtparallel.entity.CutJobLog;
import com.rsclouds.gtparallel.entity.CutJobLazyLoadTest;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface CutJobDao {
	/**
	 *
	* Description: 通过jobid或id更新
	*  @param cutJob
	* @author lishun
	* @date 2017年8月24日
	* @return void
	 */
	public void update(CutJob cutJob);
	public List<Map<String,Object>> queryAll(@Param("keyword") String keyword, @Param("status") String status, @Param("pageNum") int pageNum,
	                                         @Param("pageSize") int pageSize, @Param("userName") String userName);


	public List<CutJobLazyLoadTest> queryAllLazyLoad();
	public Map<String,Object> queryById(@Param("id") String id);

	/**
	 *
	* Description: 更新日志的cut_id
	*  @param newid
	*  @param oldid
	*  @return
	* @author lishun
	* @date 2017年10月16日
	* @return int
	 */
	public int updateLog(@Param("newid") String newid, @Param("oldid") String oldid);
	/**
	 *
	* Description: 插入日志
	*  @param record
	*  @return
	* @author lishun
	* @date 2017年10月16日
	* @return int
	 */
	public int insertLog(CutJobLog record);

	/**
	 *
	* Description: 获取日志
	*  @param
	*  @return
	* @author lishun
	* @date 2017年8月24日
	* @return List<Map<String,Object>>
	 */
	public List<Map<String,Object>> querylog(@Param("cut_id") String cut_id);
	
	
}