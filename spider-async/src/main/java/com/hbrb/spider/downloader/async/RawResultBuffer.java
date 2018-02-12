package com.hbrb.spider.downloader.async;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.http.concurrent.FutureCallback;

import com.hbrb.exception.LogicError;
import com.hbrb.spider.model.AsyncResult;
import com.hbrb.spider.model.task.RequestTask;

public class RawResultBuffer<T extends RequestTask> implements FutureCallback<AsyncResult<T>> {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RawResultBuffer.class);
	private final BlockingQueue<AsyncResult<T>> queue;
	public RawResultBuffer(int capacity) {
		super();
		this.queue  = new LinkedBlockingQueue<>(capacity);
	}

	@Override
	public void completed(AsyncResult<T> result) {
		try {
			if (!queue.offer(result, 1, TimeUnit.MINUTES)) {
				logger.warn("buffer[{}] overflow - {}", queue.size(), result);
			}
		} catch (InterruptedException e) {
			logger.warn("unexpected interruption", e);
		}
	}

	@Override
	public void failed(Exception ex) {
		// 异常在RawAsyncResponseConsumer.releaseResources里处理过了
	}

	@Override
	public void cancelled() {
		throw new LogicError("若需取消请求，在此适当处理");
	}
	
	AsyncResult<T> poll(int timeout, TimeUnit unit) throws InterruptedException {
		return queue.poll(timeout, unit);
	}
	
	AsyncResult<T> poll() {
		return queue.poll();
	}

	public int size() {
		return queue.size();
	}
}
