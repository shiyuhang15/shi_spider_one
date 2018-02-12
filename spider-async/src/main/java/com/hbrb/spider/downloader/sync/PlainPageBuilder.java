package com.hbrb.spider.downloader.sync;

import com.hbrb.spider.model.RawResult;
import com.hbrb.spider.model.page.PlainPage;
import com.hbrb.spider.model.task.RequestTask;

public class PlainPageBuilder<T extends RequestTask> extends PageDecoder<T, PlainPage<T>> {

	public PlainPageBuilder(String charsetName) {
		super(charsetName);
	}

	@Override
	public PlainPage<T> build(T requestTask, RawResult res) {
		return new PlainPage<>(requestTask, decode(res));
	}
}
