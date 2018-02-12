package com.hbrb.spider.downloader.async;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.hbrb.spider.model.task.RequestTask;

public class SimpleBlockingRequestBuffer<T extends RequestTask> implements RequestBuffer<T> {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SimpleBlockingRequestBuffer.class);
	private BlockingQueue<T> target = new LinkedBlockingQueue<>();

	@Override
	public boolean offer(T request) {
		return target.offer(request);
	}

	@Override
	public T poll() {
		try {
			return target.take();
		} catch (InterruptedException e) {
			logger.warn("target.take()", e);
			return null;
		}
	}
}
