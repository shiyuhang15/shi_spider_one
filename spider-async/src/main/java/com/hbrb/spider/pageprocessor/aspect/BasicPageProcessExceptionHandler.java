package com.hbrb.spider.pageprocessor.aspect;

import com.hbrb.spider.Spider;
import com.hbrb.spider.exception.PageProcessException;
import com.hbrb.spider.model.page.Page;
import com.hbrb.spider.model.task.RequestTask;
import com.hbrb.spider.pageprocessor.PageProcessor;

public class BasicPageProcessExceptionHandler<T extends RequestTask, P extends Page<T>> extends PageProcessor<T, P> {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(BasicPageProcessExceptionHandler.class);
//	private final org.slf4j.Logger LOGGER_TARGET;

	private PageProcessor<T, P> pageProcessor;

	public BasicPageProcessExceptionHandler(PageProcessor<T, P> pageProcessor) {
		this.pageProcessor = pageProcessor;
		/*if (pageProcessor instanceof TargetPageProcessor) {
			LOGGER_TARGET = org.slf4j.LoggerFactory
					.getLogger("com.cmcc.yuqing.collector.statistics.target");
		} else {
			LOGGER_TARGET = null;
		}*/
	}

	@Override
	public void process(P page) {
		String url = page.getRequestTask().getUrl();
		logger.info("process start - {}", url);
		try {
			this.pageProcessor.process(page);
			logger.info("process done - {}", url);
		} catch (PageProcessException e) {
			handlePageProcessException(e);
		}
	}

	/**
	 * 处理异常
	 * 
	 * @param page
	 * @param e
	 */
	private void handlePageProcessException(PageProcessException e) {
		/*Request request = page.getRequest();
		if (null != LOGGER_TARGET) {
			String siteTaskId = (String) request
					.getExtra(ConstantsHome.REQUEST_EXTRA_SITE_TASK_ID);
			if (null != siteTaskId) {
				LOGGER_TARGET.info("{}\t{}\t{}\t{}\t{}\t{}", siteTaskId, 0, 0,
						0, 0, 1);
			}
		}*/
		logger.warn(e.getUrl(), e);
		logger.warn(e.getContent());
	}

	@Override
	public void setSpider(Spider<T, P> spider) {
		pageProcessor.setSpider(spider);
	}
}
