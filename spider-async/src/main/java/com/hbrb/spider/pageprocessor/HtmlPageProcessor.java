package com.hbrb.spider.pageprocessor;

import com.hbrb.spider.exception.PageProcessException;
import com.hbrb.spider.model.page.HtmlPage;
import com.hbrb.spider.model.task.RequestTask;
import com.hbrb.util.MD5;

public abstract class HtmlPageProcessor<T extends RequestTask> extends PageProcessor<T, HtmlPage<T>> {
	protected boolean isMD5Duplicate(String url) {
		return getSpider().isDuplicated(MD5.get(url));
	}

	protected void throwPageProcessException(HtmlPage<T> page, String message) throws PageProcessException {
		throw new PageProcessException(message, page.getDocument().outerHtml(), page.getRequestTask().getUrl());
	}

	protected void throwPageProcessException(HtmlPage<T> page, String message, Throwable e)
			throws PageProcessException {
		throw new PageProcessException(message, page.getDocument().outerHtml(), page.getRequestTask().getUrl(), e);
	}
}
