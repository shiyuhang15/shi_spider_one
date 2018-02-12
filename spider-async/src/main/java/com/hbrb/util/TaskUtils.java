package com.hbrb.util;

public class TaskUtils {
	public static int[] countPops(int limit, double[] taskTotals,
			double[] weights) {
		int[] pops = new int[taskTotals.length];
		double[] proportions = new double[pops.length];
		for (int i = 0; i < pops.length; i++) {
			if (taskTotals[i] == 0) {
				continue;
			} else {
				pops[i] = -1;
				proportions[i] = 1;
				for (int j = 0; j < i; j++) {
					if (pops[j] != 0) {
						proportions[j] += weights[i] / weights[j]
								* taskTotals[i] / taskTotals[j];
					}
				}
			}
		}
		for (int i = 0; i < pops.length; i++) {
			if (pops[i] == 0) {
				continue;
			}
			pops[i] = (int) (limit / proportions[i]);
			limit -= pops[i];
		}
		return pops;
	}

	public static boolean dependsOnPageTemplate(int siteTaskType) {
		switch (siteTaskType) {
		case 2:
		case 3:
			return true;
		default:
			return false;
		}
	}

	public static boolean dependsOnUrlTemplate(int siteTaskType) {
		switch (siteTaskType) {
		case 1:
		case 3:
			return true;
		default:
			return false;
		}
	}
}
