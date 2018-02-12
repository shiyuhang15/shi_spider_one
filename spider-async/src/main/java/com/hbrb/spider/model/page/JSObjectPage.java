package com.hbrb.spider.model.page;

import com.hbrb.json.JSObject;
import com.hbrb.spider.model.task.RequestTask;

public class JSObjectPage<T extends RequestTask> extends Page<T> {
	private final JSObject content;

	public JSObjectPage(T requestTask, JSObject content) {
		super(requestTask);
		this.content = content;
	}

	public JSObject getContent() {
		return content;
	}
}
