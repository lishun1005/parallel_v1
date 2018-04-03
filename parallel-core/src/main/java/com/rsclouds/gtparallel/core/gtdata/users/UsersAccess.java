package com.rsclouds.gtparallel.core.gtdata.users;

import java.io.IOException;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import com.rsclouds.gtparallel.core.common.CoreConfig;
import com.rsclouds.gtparallel.gtdata.service.HbaseBase;

public class UsersAccess {

	/**
	 * users register
	 * 
	 * @param username
	 * @param passwdMD5
	 * @return if register successes, return ture.Otherwise, return false
	 */
	public static boolean regist(String username, String passwdMD5) {
		try {
			if (StringUtils.isEmpty(username) || StringUtils.isEmpty(passwdMD5)) {
				return false;
			}
			HbaseBase.writeRow(CoreConfig.USERS_TABLE, username, CoreConfig.USERS_ATTS,
					CoreConfig.USERS_ATTS_PWD, passwdMD5);
			return true;
		} catch (IOException e) {
			return false;
		}

	}

	/**
	 * check user's password
	 * 
	 * @param username
	 * @param passwdMD5
	 * @return
	 */
	public static boolean login(String username, String passwdMD5) {
		try {
			if (StringUtils.isEmpty(username) || StringUtils.isEmpty(passwdMD5)) {
				return false;
			}
			Result result = HbaseBase.selectRow(CoreConfig.USERS_TABLE, username);
			String pssd = new String(result.getValue(
					Bytes.toBytes(CoreConfig.USERS_ATTS),
					Bytes.toBytes(CoreConfig.USERS_ATTS_PWD)));
			if (pssd.endsWith(passwdMD5)) {
				return true;
			} else {
				return false;
			}
		} catch (IOException e) {
			return false;
		}

	}

	/**
	 * change
	 * 
	 * @param username
	 * @param oldPwdMD5
	 * @param newPwdMD5
	 * @return
	 */
	public static boolean changePasswd(String username, String oldPwdMD5,
			String newPwdMD5) {
		try {
			if (StringUtils.isEmpty(username) || StringUtils.isEmpty(oldPwdMD5) || StringUtils.isEmpty(newPwdMD5)) {
				return false;
			}
			Result result = HbaseBase.selectRow(CoreConfig.USERS_TABLE, username);
			if (result.isEmpty()) {
				return false;
			} else {
				String pssd = new String(result.getValue(
						Bytes.toBytes(CoreConfig.USERS_ATTS),
						Bytes.toBytes(CoreConfig.USERS_ATTS_PWD)));
				if (pssd.endsWith(oldPwdMD5)) {
					HbaseBase
							.writeRow(CoreConfig.USERS_TABLE, username,
									CoreConfig.USERS_ATTS, CoreConfig.USERS_ATTS_PWD,
									newPwdMD5);
					return true;
				} else {
					System.out.println("password error!");
					return false;
				}
			}
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Verify whether specified user exist
	 * 
	 * @param username
	 * @return
	 */
	public static boolean isUserExit(String username) {
		try {
			if (StringUtils.isEmpty(username)) {
				return false;
			}
			Result result = HbaseBase.selectRow(CoreConfig.USERS_TABLE, username);
			if (result.isEmpty()) {
				return false;
			}
			return true;
		} catch (IOException e) {
			return false;
		}

	}

	/**
	 * check permission
	 * 
	 * @param username
	 * @param path
	 * @param permNum
	 * @return
	 */
	public static boolean permissionCheck(String username, String path,
			int permNum) {
		try {
			if (username == null || username.equals("") || path == null
					|| path.equals("")) {
				return false;
			}
			String rowkey = pathFormat(username, path);
			Result result = HbaseBase.selectRow(CoreConfig.USERS_TABLE, rowkey);
			if (result.isEmpty()) {
				return false;
			} else {
				String perissionStr = new String(result.getValue(
						Bytes.toBytes(CoreConfig.USERS_ATTS),
						Bytes.toBytes(CoreConfig.USERS_ATTS_ACCESS)));
				int permReallyNum = Integer.parseInt(perissionStr);
				if ((permReallyNum & permNum) == 0) {
					return false;
				} else {
					return true;
				}

			}
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * assign permission
	 * 
	 * @param username
	 * @param path
	 * @param permNum
	 * @return
	 */
	public static boolean permissionAssign(String username, String path,
			int permNum) {
		try {
			if (username == null || username.equals("") || path == null
					|| path.equals("")) {
				return false;
			}
			String rowkey = pathFormat(username, path);
			HbaseBase.writeRow(CoreConfig.USERS_TABLE, rowkey, CoreConfig.USERS_ATTS,
					CoreConfig.USERS_ATTS_ACCESS, "" + permNum);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public static String pathFormat(String username, String path) {
		String[] splits;
		if (path.startsWith("/")) {
			splits = path.substring(1, path.length()).split("/");
		} else {
			splits = path.split("/");
		}

		StringBuilder rowkey = new StringBuilder(username + "/");
		if (splits.length < 2) {
			if (splits.length != 0) {
				for (int i = 0; i < splits.length; i++) {
					rowkey.append(splits[i]);
					rowkey.append("/");
				}
				rowkey.delete(rowkey.length() - 2, rowkey.length() - 1);
			}
		} else {
			rowkey.append(splits[0]);
			rowkey.append("/");
			rowkey.append(splits[1]);
		}
		return rowkey.toString();
	}

	/**
	 * remove the repeated char of the string
	 * 
	 * @param str
	 * @return
	 */
	public static String removeRepeated(String str) {
		TreeSet<String> noRepeated = new TreeSet<String>();
		StringBuilder resultStr = new StringBuilder();
		for (int i = 0; i < str.length(); i++) {
			noRepeated.add("" + str.charAt(i));
		}
		for (String index : noRepeated) {
			resultStr.append(index);
		}
		return resultStr.toString();
	}

	/**
	 * change the string of permission to a number
	 * 
	 * @param permissionStr
	 * @return
	 */
	public static int permissionStrChangInt(String permissionStr) {
		int permNum = 0;
		String permNoRep = removeRepeated(permissionStr);
		char[] permChars = permNoRep.toCharArray();
		for (int i = 0; i < permChars.length; i++) {
			switch (permChars[i]) {
			case 'r':
				permNum += CoreConfig.PERMISSION_READ;
				break;
			case 'w':
				permNum += CoreConfig.PERMISSION_WRITE;
				break;
			default:
				break;
			}
		}
		return permNum;
	}
}
