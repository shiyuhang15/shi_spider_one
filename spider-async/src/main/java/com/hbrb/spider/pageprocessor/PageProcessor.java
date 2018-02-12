package com.hbrb.spider.pageprocessor;

import com.hbrb.spider.Spider;
import com.hbrb.spider.exception.PageProcessException;
import com.hbrb.spider.model.page.Page;
import com.hbrb.spider.model.task.RequestTask;

public abstract class PageProcessor<T extends RequestTask, P extends Page<T>> {
	private Spider<T, P> spider;

	public abstract void process(P page) throws PageProcessException;

	protected Spider<T, P> getSpider() {
		return spider;
	}

	public void setSpider(Spider<T, P> spider) {
		this.spider = spider;
	}

	protected boolean forward(T taskRequest) {
		return spider.addTask(taskRequest);
	}
}
