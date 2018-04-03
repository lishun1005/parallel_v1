package com.rsclouds.gtparallel.core;

import com.rsclouds.gtparallel.core.common.CoreConfig;
import com.rsclouds.gtparallel.core.gtdata.cutting.GenerateMapConf;
import com.rsclouds.gtparallel.core.gtdata.cutting.ImageSegementBase;
import com.rsclouds.gtparallel.core.gtdata.cutting.RealChinaSegement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.util.ToolRunner;

/**
 * gt-parallel入口
 *
 * @author wugq
 */
public class App {
	private static final Log logger = LogFactory.getLog(App.class);

	public static void main(String[] args) throws Exception {
		String jobOp = args[0];
		String[] jobargs = new String[args.length - 1];
		System.arraycopy(args, 1, jobargs, 0, args.length - 1);
		for (String str : jobargs) {
			System.out.println(str);
		}
		int status = 0;
		switch (jobOp) {
			case CoreConfig.JOB_OP_ONEMAP:

				status = ToolRunner.run(new ImageSegementBase(), jobargs);
				System.exit(status);
				break;
			case CoreConfig.JOB_OP_REALTIMEUPT:
				status = ToolRunner.run(new RealChinaSegement(), jobargs);//时间属性
				System.exit(status);
				break;
			case CoreConfig.JOB_OP_GENERATE_MAP_CONF:
				GenerateMapConf.main(jobargs);
			default:
				break;
		}
	}
}
