package com.hbrb.spider;

import com.hbrb.spider.model.page.Page;
import com.hbrb.spider.model.task.RequestTask;

public abstract class Spider<T extends RequestTask, P extends Page<T>> implements Runnable {
	public final static boolean judgeExpired = false;//过期判断true，不判断false
	private static boolean inTestMode = false;
	public static void setTestMode(boolean inTestMode) {
		Spider.inTestMode = inTestMode;
	}

	public static boolean inTestMode() {
		return inTestMode;
	}
	public abstract boolean addTask(T taskRequest);
	public abstract boolean isDuplicated(String id);
	public abstract void close();
}
