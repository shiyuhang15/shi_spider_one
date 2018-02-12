package com.hbrb.spider.scheduler;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.hbrb.spider.model.task.TemplateTask;

public class SiteTaskRequestBuffer<T extends TemplateTask> extends TemplateTaskRequestBuffer<T> {
	@SuppressWarnings("unchecked")
	public void reset(int maxSiteTaskId) {
		Queue<T>[] qs = new ConcurrentLinkedQueue[maxSiteTaskId];
		for (int i = 0; i < maxSiteTaskId; i++) {
			qs[i] = new ConcurrentLinkedQueue<>();
		}
		reset(qs);
	}

	@Override
	public boolean offer(T e) {
		nullCount.set(0);
		return qs[e.getSiteTaskId() - 1].offer(e);
	}

	public int size() {
		int size = 0;
		for (Queue<T> queue : qs) {
			size += queue.size();
		}
		return size;
	}
}
