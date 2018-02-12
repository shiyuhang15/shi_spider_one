package com.hbrb.spider.downloader.sync;

import com.hbrb.spider.model.RawResult;
import com.hbrb.spider.model.page.Page;
import com.hbrb.spider.model.task.RequestTask;

public interface PageBuilder<T extends RequestTask, P extends Page<T>> {
	P build(T requestTask, RawResult res);
}
