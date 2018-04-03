package com.rsclouds.common.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PageUtils {
	/**
	 * 
	 * Description：根据当前页，获取当前页左右边的页码，用于分页 如当前是第3页： （ 前一页 1 2 3 4 5 后一页） （ l1 p1 p2 p3 p4 p5 r1）
	 * 
	 * @param currentPage
	 * @param numInPage
	 * @param total
	 * @return
	 *
	 */
	public static Map<String, Object> countCurrPageNearPages(int currentPage, int numInPage, int total,
			Map<String, Object> map) {
		Integer l1 = -1, r1 = -1;// 如果是-1就不显示了
		Integer pageData[] = new Integer[5];
		for (int i = 0; i < pageData.length; i++) {
			pageData[i] = -1;
		}

		int pageNum = 0;// 总页数
		if (total % numInPage == 0) {
			pageNum = total / numInPage;
		} else {
			pageNum = total / numInPage + 1;
		}
		if (currentPage > pageNum) {
			currentPage = pageNum;
		}
		// 分3种情况，小于5等于5和大于5
		if (pageNum <= 5)// 小于5
		{
			if (currentPage == 1) // 前一页
			{
				l1 = 1;
				r1 = 1;
			} else {
				l1 = currentPage - 1;
			}
			for (int i = 0; i < pageNum; i++) {
				pageData[i] = i + 1;
			}

			if (currentPage + 1 <= pageNum) {
				r1 = currentPage + 1;
			} else {
				r1 = currentPage;
			}
		} else// 大于5
		{
			if (currentPage - 1 < 1) {
				l1 = 1;
			} else {
				l1 = currentPage - 1;
			}
			int distanceP = currentPage - 1; // 当前页距离第一页相差多少页
			int distanceN = pageNum - currentPage; // 当前页距离最后一页相差多少页

			if (distanceP == 0) // 前面没有其他页
			{
				for (int i = 0; i < 5; i++) {
					pageData[i] = currentPage + i;
				}
			} else if (distanceP == 1)// 前面有一页
			{
				for (int i = -1; i < 4; i++) {
					pageData[i + 1] = currentPage + i;
				}
			} else if (distanceN >= 2) // 如果当前页后面还有2页以上
			{
				for (int i = -2; i < 3; i++) {
					pageData[i + 2] = currentPage + i;
				}
			} else if (distanceN == 1)// //如果当前页后面还有1页
			{
				for (int i = -3; i < 2; i++) {
					pageData[i + 3] = currentPage + i;
				}
			} else if (distanceN == 0)// //如果当前页后面还有0页
			{
				for (int i = -4; i < 1; i++) {
					pageData[i + 4] = currentPage + i;
				}
			}
			if (currentPage + 1 <= pageNum) {
				r1 = currentPage + 1;
			} else {
				r1 = currentPage;
			}

		}

		map.put("l1", l1);
		map.put("r1", r1);
		map.put("pageData", pageData);
		map.put("pageNum", pageNum);
		map.put("numInPage", numInPage);
		map.put("currentPage", currentPage);
		return map;
	}
	public static List countFileData(List list, int currentPage, int numInPage, int total) {
		List listResult = new ArrayList();
		int start = 0;// 开始坐标
		int end = 0;// 结束坐标
		if (1 < currentPage) {
			start = (currentPage - 1) * numInPage;
		}
		if (numInPage + start > total) {
			end = total;
		} else {
			end = start + numInPage;
		}

		for (int i = start; i < end; i++) {
			listResult.add(list.get(i));
		}
		return listResult;
	}
}
