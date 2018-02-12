package com.hbrb.spider.downloader.async;

import java.util.concurrent.TimeUnit;

import com.hbrb.spider.model.page.Page;
import com.hbrb.spider.model.task.RequestTask;

public interface PageBuffer<T extends RequestTask, P extends Page<T>> {
	P poll(int timeout, TimeUnit unit);

	P poll();

	int size();
}
