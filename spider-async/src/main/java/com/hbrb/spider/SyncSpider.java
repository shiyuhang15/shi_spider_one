package com.hbrb.spider;

import java.io.IOException;

import com.hbrb.exception.LogicError;
import com.hbrb.spider.constraint.UniqueConstraint;
import com.hbrb.spider.downloader.async.RequestBuffer;
import com.hbrb.spider.downloader.async.SimpleRequestBuffer;
import com.hbrb.spider.downloader.sync.PageAdapter;
import com.hbrb.spider.exception.PageProcessException;
import com.hbrb.spider.model.SpiderConfig;
import com.hbrb.spider.model.page.Page;
import com.hbrb.spider.model.task.GenericRequestTask;
import com.hbrb.spider.pageprocessor.PageProcessor;

public class SyncSpider<T extends GenericRequestTask, P extends Page<T>> extends Spider<T, P> {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SyncSpider.class);
	private static boolean inTestMode = false;
	public static void setTestMode(boolean inTestMode) {
		SyncSpider.inTestMode = inTestMode;
	}
	public static boolean inTestMode() {
		return inTestMode;
	}
	
	private final int requestInterval;
	private final int cycleRetryTimes;
	private final PageProcessor<T, P> pageProcessor;
	private final UniqueConstraint uc;
	private final PageAdapter<T, P> downloader;
	private final RequestBuffer<T> requestBuffer;

	public SyncSpider(SpiderConfig config, UniqueConstraint uc, PageAdapter<T, P> downloader,
			RequestBuffer<T> requestBuffer, PageProcessor<T, P> pageProcessor) {
		super();
		this.requestInterval = config.getRequestInterval() * 1000;
		this.cycleRetryTimes = config.getCycleRetryTimes();
		this.uc = uc;
		this.downloader = downloader;
		if (null == requestBuffer) {
			this.requestBuffer = new SimpleRequestBuffer<>();
		} else {
			this.requestBuffer = requestBuffer;
		}
		pageProcessor.setSpider(this);
		this.pageProcessor = pageProcessor;
	}

	public boolean isDuplicated(String id) {
		return uc.isDuplicated(id);
	}

	public boolean addTask(T requestTask) {
		return requestBuffer.offer(requestTask);
	}
	
	public void run(){
		T req;
		while ((req = requestBuffer.poll()) != null) {
			logger.info("task - {}", req.getUrl());
			P page = downloader.execute(req);
			if (null == page) {
				if (cycleRetryTimes > 0 && req.getRetryCount() < cycleRetryTimes) {
					addTask(req);
					req.setRetryCount(req.getRetryCount() + 1);
					logger.info("cycle retry - ", req.getRetryCount());
				}
			} else {
				processPage(page);
			}
			if (requestInterval > 0) {
				try {
					Thread.sleep(requestInterval);
				} catch (InterruptedException e) {
					logger.warn("请求间隔被打断");
				}
			}
		}
		logger.warn("crawl finished");
	}
	
	private void processPage(P page) {
		try {
			pageProcessor.process(page);
		} catch (PageProcessException e) {
			throw new LogicError("未处理PageProcessException", e);
		}
	}
	
	@Override
	public void close() {
		try {
			downloader.close();
		} catch (IOException e) {
			logger.warn("downloader close failed", e);
		}
	}
}
