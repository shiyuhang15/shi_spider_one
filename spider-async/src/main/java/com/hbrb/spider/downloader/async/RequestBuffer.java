package com.hbrb.spider.downloader.async;

import com.hbrb.spider.model.task.RequestTask;

public interface RequestBuffer<T extends RequestTask> {
	boolean offer(T request);
	T poll();
}
