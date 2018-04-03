package com.rsclouds.gtparallel.core.job;

import com.rsclouds.gtparallel.core.common.CoreConfig;
import com.rsclouds.gtparallel.core.common.Utils;
import com.rsclouds.gtparallel.core.entity.Job;
import com.rsclouds.gtparallel.gtdata.service.HbaseBase;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

public class JobHbase {

	private final static Logger logger = LoggerFactory.getLogger(JobHbase.class);

	public static Job getJob(String jobid) throws IOException {
		Job job = Utils.result2Job(HbaseBase.selectRow(CoreConfig.MANAGER_JOB_TABLE, jobid));
		return job;
	}

	public static boolean createJob(Job job) throws IOException {
		if (job.getRowKey() != null) {
			HbaseBase.writeRows(CoreConfig.MANAGER_JOB_TABLE, job.getRowKey(), CoreConfig.JOB.FAMILY.strVal, job.toMap());
			return true;
		} else {
			return false;
		}
	}

	public static boolean setJobState(String jobid, CoreConfig.JOB_STATE state) {
		return setJobState(jobid, state, 0);
	}

	public static boolean serJobPart(String jobid, int currPart, int allPart) {
		try {
			if (!jobid.isEmpty() && currPart <= allPart) {
				HbaseBase.writeRow(CoreConfig.MANAGER_JOB_TABLE, jobid, CoreConfig.JOB.FAMILY.strVal, CoreConfig.JOB.PART.strVal, currPart + "/" + allPart);
				return true;
			}
			return false;
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			;
			return false;
		}
	}

	public static boolean setJobState(String jobid, CoreConfig.JOB_STATE state, long endTime) {
		try {
			if (!jobid.isEmpty()) {
				HbaseBase.writeRow(CoreConfig.MANAGER_JOB_TABLE, jobid, CoreConfig.JOB.FAMILY.strVal, CoreConfig.JOB.STATE.strVal, state.toString());
				if (endTime != 0) {
					HbaseBase.writeRow(CoreConfig.MANAGER_JOB_TABLE, jobid, CoreConfig.JOB.FAMILY.strVal, CoreConfig.JOB.END_TIME.strVal, endTime + "");
				}
				return true;
			}
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			;
		}
		return false;
	}

	public static boolean setJobState(String jobid, CoreConfig.JOB_STATE state, String log, long endTime) {
		try {
			if (!jobid.isEmpty()) {
				HbaseBase.writeRow(CoreConfig.MANAGER_JOB_TABLE, jobid, CoreConfig.JOB.FAMILY.strVal, CoreConfig.JOB.STATE.strVal, state.toString());
				if (endTime != 0) {
					HbaseBase.writeRow(CoreConfig.MANAGER_JOB_TABLE, jobid, CoreConfig.JOB.FAMILY.strVal, CoreConfig.JOB.END_TIME.strVal, endTime + "");
				}
				if (StringUtils.isNotBlank(log)) {
					HbaseBase.writeRow(CoreConfig.MANAGER_JOB_TABLE, jobid, CoreConfig.JOB.FAMILY.strVal, CoreConfig.JOB.LOG.strVal, log);
				}
				return true;
			}
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			;

		}
		return false;
	}

	/**
	 * Description: 添加地理范围
	 *
	 * @param jobid
	 * @param extend
	 * @return boolean
	 * @author lishun
	 * @date 2017年9月12日
	 */
	public static boolean setJobGeoRange(String jobid, String geoRange) {
		try {
			if (!jobid.isEmpty()) {
				HbaseBase.writeRow(CoreConfig.MANAGER_JOB_TABLE, jobid, CoreConfig.JOB.FAMILY.strVal, CoreConfig.JOB.GEO_RANGE.strVal, geoRange.toString());
				return true;
			}
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			;
		}
		return false;
	}

	public static boolean setQueryStr(String jobid, String queryStr) {
		try {
			if (!jobid.isEmpty()) {
				HbaseBase.writeRow(CoreConfig.MANAGER_JOB_TABLE, jobid, CoreConfig.JOB.FAMILY.strVal, CoreConfig.JOB.QUERY_STR.strVal, queryStr);
				return true;
			}
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			;
		}
		return false;
	}

	public static boolean setJobLog(String jobid, String log) {
		try {
			if (!StringUtils.isBlank(jobid)) {
				HbaseBase.writeRow(CoreConfig.MANAGER_JOB_TABLE, jobid,
						CoreConfig.JOB.FAMILY.strVal,
						CoreConfig.JOB.LOG.strVal, log);
				return true;
			}
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			;
		}
		return false;
	}

	public static long genterJobidTime() {
		return Long.valueOf("9999999999999") - System.currentTimeMillis();
	}

	public static String genterJobid(String username) {
		return genterJobidTime() + "_" + username + "_" + UUID.randomUUID().toString();
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
	}

}
