package com.hbrb.spider.model.page;

import com.hbrb.spider.model.task.RequestTask;

public class Page<T extends RequestTask> {
	private final T requestTask;

	public Page(T requestTask) {
		this.requestTask = requestTask;
	}
	public T getRequestTask() {
		return this.requestTask;
	}
}
