package com.hbrb.spider.scheduler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.hbrb.spider.model.task.TemplateTask;

public class NaviTaskRequestBuffer<T extends TemplateTask> extends TemplateTaskRequestBuffer<T> {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(NaviTaskRequestBuffer.class);

	@SuppressWarnings("unchecked")
	public void reset(List<T> tasks) {
		Map<Integer, Queue<T>> map = new HashMap<>();
		for (T task : tasks) {
			Integer siteTaskId = Integer.valueOf(task.getSiteTaskId());
			Queue<T> queue = map.get(siteTaskId);
			if (null == queue) {
				queue = new ConcurrentLinkedQueue<>();
				map.put(siteTaskId, queue);
			}
			if (queue.offer(task)) {
				logger.info("start: {}", task.getUrl());
			}
		}
		reset(map.values().toArray(new ConcurrentLinkedQueue[map.size()]));
	}

	@Override
	public boolean offer(T t) {
		return false;
	}
}
