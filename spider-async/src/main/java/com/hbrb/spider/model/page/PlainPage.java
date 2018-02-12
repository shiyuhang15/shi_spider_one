package com.hbrb.spider.model.page;

import com.hbrb.spider.model.task.RequestTask;

public class PlainPage<T extends RequestTask> extends Page<T> {
	private final String content;

	public PlainPage(T requestTask, String content) {
		super(requestTask);
		this.content = content;
	}

	public String getContent() {
		return content;
	}
}
