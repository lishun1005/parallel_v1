package com.rsclouds.gtparallel.dao;


import com.rscloud.ipc.rpc.api.dto.MosaicJobDto;
import com.rsclouds.gtparallel.entity.MosaicJobLog;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;


public interface MosaicJobDao {
	public List<Map<String,Object>> queryAll(@Param("keyword") String keyword, @Param("status") String status,
	                                         @Param("pageNum") int pageNum, @Param("pageSize") int pageSize,
	                                         @Param("userName") String userName, @Param("algorithmType") String algorithmType);

	public int insertLog(MosaicJobLog record);

    public Map<String,Object> queryById(@Param("id") String id);

    /**
     *
    * Description: 通过jobid或id更新记录
    *  @param record
    *  @return
    * @author lishun
    * @date 2017年8月24日
    * @return int
     */
    public int updateByJobid(MosaicJobDto record);

    /**
     *
    * Description: 添加子任务记录
    *  @param jobid
    *  @param status
    *  @param progress
    *  @param sortOrder
    *  @param id
    *  @param ctTime
    * @author lishun
    * @date 2017年8月24日
    * @return void
     */
    public void addMosaicJobSub(@Param("jobid") String jobid, @Param("status") String status, @Param("progress") Integer progress,
                                @Param("sortOrder") Integer sortOrder, @Param("id") String id, @Param("ctTime") Date ctTime);
	/**
	 * @Description:批量添加子任务
	 * @author lishun
	 * @date 2018/1/18
	 * @param list
	 * @return void
	 */
	public void insertMosaicJobSubBatch(List<Map<String,Object>> list);
    /**
     *
    * Description: 获取子任务
    *  @param jobId
    *  @param status
    *  @return
    * @author lishun
    * @date 2017年8月24日
    * @return List<Map<String,Object>>
     */
    public List<Map<String,Object>> getMosaicSub(@Param("jobid") String jobId, @Param("status") String status);

    /**
     *
    * Description: 更新子进度
    *  @param id
    *  @param progress
    * @author lishun
    * @date 2017年8月24日
    * @return void
     */
    public void updateMosaicJobSub(@Param("id") String id, @Param("progress") Integer progress);

    /**
     *
    * Description: 通过jobid更新小于sortOrder(任务排序)的子进度为100%
    *  @param jobId
    *  @param sortOrder
    * @author lishun
    * @date 2017年8月24日
    * @return void
     */
    public void updateMosaicJobSubByJobid(@Param("jobid") String jobId, @Param("sortOrder") Integer sortOrder);

    public List<Map<String,Object>> querylog(@Param("mosaicId") String mosaicId);

    public void mosaicDel(@Param("id") String id);
}