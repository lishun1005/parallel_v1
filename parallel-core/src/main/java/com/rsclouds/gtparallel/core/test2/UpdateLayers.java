package com.rsclouds.gtparallel.core.test2;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Scan;

public class UpdateLayers {

	public static void mian(String[] args) throws IOException {
		Configuration conf = HBaseConfiguration.create();
		HTable htable = new HTable(conf, "map_meta");
		Scan scan = new Scan();
		scan.setStartRow("/map/auto_proc/img/warter/RealtimeChinaTest2015080701/Layers/_alllayers/L0d//".getBytes());
		scan.setStopRow("/map/auto_proc/img/warter/RealtimeChinaTest2015080701/Layers/_alllayers/L0d/{".getBytes());
	}
}
