/*
 * MR的主程序入口
 * 2014-7-22 made by chenshangshang
 */
package com.rsclouds.gtparallel.core.download.mr;

public class Download /*extends Configured implements Tool*/ {
	/*private final Log log = LogFactory.getLog(Download.class);
	@SuppressWarnings("deprecation")
	public int run(String[] args) throws Exception {
		Configuration conf = (getConf() == null ? new Configuration()
				: getConf());
		conf.set("mapreduce.map.maxattempts", "0");
		conf.set("mapreduce.map.failures.maxpercent", "100");
		conf.setInt("mapred.task.timeout", Integer.MAX_VALUE);

		// 记录参数的长度
		if (args == null || args.length == 0){
			System.out.println("Usage <jobid_0> <jobid_1> <jobid_2> <jobid_3> ...");
			System.exit(3);
		}
		int length = args.length;
		conf.setInt("length", length);
		
		for(int i=0;i<length;i++){
			conf.set("job_"+i, args[i]);
		}

		Job job = Job.getInstance(conf, "DownLoad_MR");
		job.setJarByClass(Download.class);
		FileSystem fs = FileSystem.get(getConf());
		fs.delete(new Path("/DownLoad/output/" + args[0]), true);
		FileInputFormat.addInputPath(job, new Path("/DownLoad"));
		FileOutputFormat.setOutputPath(job, new Path("/DownLoad/output/"+ args[0]));

		// 设置输入的InputFormat
		job.setInputFormatClass(DownloadInputFormat.class);
		job.setMapperClass(DownloadMR.DownloadMapper.class);
		job.setNumReduceTasks(0);
		job.setOutputKeyClass(NullWritable.class);
		job.setOutputValueClass(Text.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		boolean status = job.waitForCompletion(true);
		fs = FileSystem.get(getConf());
		fs.delete(new Path("/DownLoad/output/"+ args[0]), true);
		fs.close();
		return status?0:1;
	}

	public static void main(String[] args) throws Exception {
		ToolRunner.run(new Download(), args);
	}*/
}

// 下载的MR
/*class DownloadMR {

	static class DownloadMapper extends Mapper<NullWritable, Text, NullWritable, Text> {
		String jobid = null;
		String downloadURL = null;
		String saveFilename = null;
		String path = null;
		Boolean MAP_SWITCH = false;

		private final Log log = LogFactory.getLog(getClass().getName());

		@Override
		protected void setup(Context context) throws IOException,
				InterruptedException {
			super.setup(context);
		}

		@SuppressWarnings("deprecation")
		@Override
		protected void map(NullWritable key, Text value, Context context)throws IOException,InterruptedException {
			try {
				jobid = value.toString();
				Result  result = HbaseBase.selectRow(CoreConfig.MANAGER_JOB_TABLE, jobid);
				if(result == null){
					log.error("jobid is not exist : " + jobid);
					System.exit(1);
				}
				com.rsclouds.gtparallel.core.entity.Job job = Utils.result2Job(result);
				String ip_old = job.getNode();				
				// 当前运行MR程序的JID
				JobID JID = context.getJobID();	
				// 下载路径
				downloadURL = job.getInPath();
				// Gt-data 上传路径
				path = job.getOutPath();
				// 获得本机IP
				InetAddress addr = InetAddress.getLocalHost();
				String ip = addr.getHostAddress().toString();
				// 获取当前运行的PID
				String name = ManagementFactory.getRuntimeMXBean().getName();
				String pid = name.split("@")[0];
				log.info("当前运行的进程为:" + pid);

				// Hbase操作
				Map<String, String> map = new HashMap<String, String>();
				map.put(CoreConfig.JOB.PID.strVal, pid);
				map.put(CoreConfig.JOB.NODE.strVal, ip);
				map.put(CoreConfig.JOB.STATE.strVal,CoreConfig.JOB_STATE.RUNNING.toString());
				map.put(CoreConfig.JOB.JID.strVal, JID.toString());
				// 插入记录记录
				HbaseBase.writeRows(CoreConfig.MANAGER_JOB_TABLE, jobid, CoreConfig.JOB.FAMILY.strVal,map);

				String sourceUrl = downloadURL;
				String urlString = sourceUrl.substring(0, 5).toLowerCase();
				if (urlString.startsWith("http")) {
					saveFilename = getFileNameFromUrl(sourceUrl);// 保存的文件名
					String savePath = CoreConfig.DOWNLOAD_TEMP_PATH;// 保存的路径
					int threadNum = 4;// 开启的线程数

					// 封装下载相关参数信息
					DownloadBeanHTTP bean = new DownloadBeanHTTP();
					bean.setSourceUrl(sourceUrl);
					bean.setSaveFilename(saveFilename);
					bean.setSavePath(savePath);
					bean.setThreadNum(threadNum);

					// 断点续传，执行拷贝工作
					if (ip_old != null && !ip_old.equalsIgnoreCase(ip)) {
						String commands = "scp " + ip_old + ":"
								+ CoreConfig.DOWNLOAD_TEMP_PATH + "/"
								+ saveFilename + " " + ip_old + ":"
								+ CoreConfig.DOWNLOAD_TEMP_PATH + "/"
								+ saveFilename + ".lck yarn@" + ip + ":"
								+ CoreConfig.DOWNLOAD_TEMP_PATH;
						log.info("scp复制命令为:" + commands);
						long startTime = System.currentTimeMillis();
						log.info("开始远程复制");
						Process process = Runtime.getRuntime().exec(commands);
						int code = 1;
						try {
							code = process.waitFor();
						} catch (InterruptedException e) {
							e.printStackTrace();
							log.error(commands, e);
						}
						if (code == 0) {
							long endTime = System.currentTimeMillis();
							log.info("远程复制成功");
							log.info("复制耗时:" + (endTime - startTime));
						} else {
							log.info("远程复制失败");
						}
					}

					log.info("启动HTTP下载程序");

					// 使用HTTP下载
					StartDownloadHTTP sd = new StartDownloadHTTP(bean, jobid,
							downloadURL, saveFilename, path);// 创建开始下载对象
					sd.startDownload();// 开始下载

				} else if (urlString.startsWith("ftp")) {
					if (sourceUrl.contains("%")) {
						sourceUrl = URLDecoder.decode(sourceUrl);
					}

					// 使用FTP下载
					int index_1 = sourceUrl.lastIndexOf("?");
					String url_1 = sourceUrl.substring(0, index_1);
					int index_1_1 = url_1.indexOf("/", 6);
					int index_1_2 = url_1.lastIndexOf("/");

					// 把域名解析成ip地址
					String ipString1 = url_1.substring(6, index_1_1);
					String ipString = null;
					int number = 0;
					while ((ipString == null) & (number <= 5)) {
						try {
							InetAddress localAddress = InetAddress
									.getByName(ipString1);
							ipString = localAddress.getHostAddress();
						} catch (Exception e) {
							log.info("域名解析异常:" + ipString1);
							log.info("异常为:" + e.getMessage());
							log.info("重试第" + number + "次");
						}
						number++;
					}

					if (ipString == null) {
						throw new IOException("域名解析异常:" + ipString1);
					}

					saveFilename = getFileNameFromUrl(url_1);
					String remotePath = url_1.substring(index_1_1,
							index_1_2 + 1);

					String url_2 = sourceUrl.substring(index_1 + 1);
					int index_2 = url_2.indexOf("&");

					String url_3 = url_2.substring(0, index_2);
					String url_4 = url_2.substring(index_2 + 1);

					int index_3 = url_3.lastIndexOf("=");
					String username = url_3.substring(index_3 + 1);

					int index_4 = url_4.lastIndexOf("=");
					String password = url_4.substring(index_4 + 1);

					// 断点续传，执行拷贝工作
					if (ip_old != null && !ip_old.equalsIgnoreCase(ip)) {
						String commands = "scp " + ip_old + ":"
								+ CoreConfig.DOWNLOAD_TEMP_PATH + "/"
								+ saveFilename + " " + ip_old + ":"
								+ CoreConfig.DOWNLOAD_TEMP_PATH + "/"
								+ saveFilename + ".lck yarn@" + ip + ":"
								+ CoreConfig.DOWNLOAD_TEMP_PATH;
						log.info("scp复制命令为:" + commands);
						long startTime = System.currentTimeMillis();
						log.info("开始远程复制");
						Process process = Runtime.getRuntime().exec(commands);
						int code = 1;
						try {
							code = process.waitFor();
						} catch (InterruptedException e) {
							e.printStackTrace();
							log.error(commands, e);
						}
						if (code == 0) {
							long endTime = System.currentTimeMillis();
							log.info("远程复制成功");
							log.info("复制耗时:" + (endTime - startTime));
						} else {
							log.info("远程复制失败");
						}
					}

					log.info("启动FTP下载程序");
					DownloadBeanFTP bean = new DownloadBeanFTP(ipString, 21,
							username, password, remotePath, saveFilename,
							CoreConfig.DOWNLOAD_TEMP_PATH, saveFilename, 4);
					StartDownloadFTP sd = new StartDownloadFTP(bean, jobid,
							downloadURL, saveFilename, path);// 创建开始下载对象
					sd.startDownload();// 开始下载
				} else {
					System.out.println("Please Use HTTP of FTP download");
				}
			} catch (IOException e) {
				JobHbase.setJobState(jobid, CoreConfig.JOB_STATE.FAILED);
				DownloadUtils.pushProgress(jobid, "100",CoreConfig.JOB_STATE.FAILED.name());
				log.info("写入数据库error:"+e.getMessage());
				 throw e;
			}catch (Exception e){
				JobHbase.setJobState(jobid, CoreConfig.JOB_STATE.FAILED);
				DownloadUtils.pushProgress(jobid, "100",CoreConfig.JOB_STATE.FAILED.name());
				log.info("写入数据库error:" + e.getMessage());
				throw e;
			}
		}

		@Override
		protected void cleanup(Context context) throws IOException,
				InterruptedException {
			super.cleanup(context);
		}

		public static String getFileNameFromUrl(String url) {
			String name = new Long(System.currentTimeMillis()).toString()
					+ ".X";
			int index_1 = url.lastIndexOf("/");

			if (index_1 > 0) {
				name = url.substring(index_1 + 1);
				int index_2 = name.lastIndexOf("?");
				if (index_2 != -1) {
					name = name.substring(0, index_2);
				}
				if (name.trim().length() > 0) {
					return name;
				}
			}
			return name;
		}
	}
}*/
