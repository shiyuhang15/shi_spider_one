package com.hbrb.spider.downloader.async;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.hbrb.spider.model.task.RequestTask;

public class SimpleRequestBuffer<T extends RequestTask> implements RequestBuffer<T> {
	private Queue<T> target = new ConcurrentLinkedQueue<>();

	@Override
	public boolean offer(T request) {
		return target.offer(request);
	}

	@Override
	public T poll() {
		return target.poll();
	}
}
