package com.hbrb.spider.pageprocessor;

import com.hbrb.spider.model.page.Page;
import com.hbrb.spider.model.task.RequestTask;
import com.hbrb.spider.pageprocessor.aspect.BasicPageProcessExceptionHandler;

public class PageProcessorFactory {
	public static <T extends RequestTask, P extends Page<T>> PageProcessor<T, P> wrap(PageProcessor<T, P> pp) {
		return new BasicPageProcessExceptionHandler<>(pp);
	}
}
