package com.rsclouds.gtparallel.core.job;

/**
 * job读写例子
 * @author wugq
 *
 */
public class JobExample {

	/*public static void main(String[] args) throws IOException {
		String jobid = UUID.randomUUID().toString();
		System.out.println(jobid);
		//新建一个table
		HbaseBase.createTable(CoreConfig.MANAGER_JOB_TABLE, new String[]{CoreConfig.JOB.FAMILY.strVal});
		Job newJob = new Job();
		newJob.setRowKey(jobid);
		newJob.setPid("14523");
		newJob.setInPath("http://www.dsadada.com/file/123.txt");
		newJob.setOutPath("resource/XTYY/public/china_cia/Layers/_allLayers//L00");
		newJob.setType(CoreConfig.JOB_TYPE.ONEMAP.name());
		newJob.setProgress("55");
		newJob.setState(CoreConfig.JOB_STATE.RUNNING.name());
		newJob.setJid("job_276123121787");
		newJob.setStartTime(""+System.currentTimeMillis());
		//插入一条模拟job记录
		JobHbase.createJob(newJob);
		//通过jobid查找job
		System.out.println(JobHbase.getJob(jobid));
		//搜索指定范围的记录
		System.out.println("搜索指定范围的记录:");
		for(Result rs : HbaseBase.selectByRegions(CoreConfig.MANAGER_JOB_TABLE,null,null)){//设为null表示所有范围
			System.out.println("==================");
			Job job = Utils.result2Job(rs);
			System.out.println(job.toString());
		}
		//搜索rowkey包含指定关键字的记录
		System.out.println("搜索rowkey包含指定关键字的记录:");
		for(Result rs : HbaseBase.selectByRowFilter(CoreConfig.MANAGER_JOB_TABLE, "www.sina.com")){
			System.out.println("==================");
			Job job = Utils.result2Job(rs);
			System.out.println(job.toString());
		}
	}*/
}
