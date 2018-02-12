package com.hbrb.spider.downloader.sync;

import com.hbrb.spider.model.RawResult;
import com.hbrb.spider.model.page.Page;
import com.hbrb.spider.model.task.RequestTask;

public abstract class AbstractPageBuilder<T extends RequestTask, P extends Page<T>> implements PageBuilder<T, P> {
	private final String charsetName;

	public AbstractPageBuilder(String charsetName) {
		super();
		this.charsetName = charsetName;
	}

	String getCharsetName(RawResult res) {
		if (null == charsetName) {
			return res.getCharsetName();
		} else {
			return charsetName;
		}
	}
}
