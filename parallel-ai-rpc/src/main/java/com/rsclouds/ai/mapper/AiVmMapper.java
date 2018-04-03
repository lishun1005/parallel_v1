package com.rsclouds.ai.mapper;

import com.rscloud.ipc.rpc.api.entity.AiVm;
import com.rscloud.ipc.rpc.api.entity.AiVmInstancePort;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AiVmMapper {

    int insert(AiVm record);

    List<AiVm> queryAll(@Param("keyword") String keyword, @Param("pageNum") int pageNum,
                        @Param("pageSize") int pageSize);

    int update(AiVm record);

    int aiVmInstancePortAdd(AiVmInstancePort aiVmInstancePort);
    AiVmInstancePort selectAiVmInstancePortByVmIdAndPort(@Param("vmId")String vmId, @Param("vmPort")Integer vmPort);

    int updateAiVmInstancePort(AiVmInstancePort aiVmInstancePort);

    int aiJobVmPortAdd(@Param("id")String id , @Param("jobid")String jobid,
                       @Param("vmPortId")String vmPortId,@Param("vmType")Integer vmType);

}