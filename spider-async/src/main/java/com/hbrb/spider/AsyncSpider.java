package com.hbrb.spider;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;

import com.hbrb.exception.LogicError;
import com.hbrb.spider.constraint.UniqueConstraint;
import com.hbrb.spider.downloader.async.HttpAsyncClientDownloader;
import com.hbrb.spider.downloader.async.PageBuffer;
import com.hbrb.spider.downloader.async.RequestBuffer;
import com.hbrb.spider.downloader.async.SimpleRequestBuffer;
import com.hbrb.spider.exception.PageProcessException;
import com.hbrb.spider.model.AsyncResult;
import com.hbrb.spider.model.page.Page;
import com.hbrb.spider.model.task.RequestTask;
import com.hbrb.spider.pageprocessor.PageProcessor;

public class AsyncSpider<T extends RequestTask, P extends Page<T>> extends Spider<T, P> {
	private final PageProcessor<T, P> pageProcessor;
	private final UniqueConstraint uc;
	private final HttpAsyncClientDownloader<T> downloader;
	private final PageBuffer<T, P> pageBuffer;
	private final RequestBuffer<T> requestBuffer;

	public AsyncSpider(UniqueConstraint uc, HttpAsyncClientDownloader<T> downloader, PageBuffer<T, P> pageBuffer,
			RequestBuffer<T> requestBuffer, PageProcessor<T, P> pageProcessor) {
		super();
		this.uc = uc;
		this.downloader = downloader;
		this.pageBuffer = pageBuffer;
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
		int threadCount = Runtime.getRuntime().availableProcessors() - 1;
		final CountDownLatch latch = new CountDownLatch(threadCount + 1);
		Runnable target = new Runnable() {
			public void run() {
				try {
					for (;;) {
						P page = pageBuffer.poll();
						if (null == page) {
							T req = requestBuffer.poll();
							if (null == req) {
								// FIXME test 此处的超时时间须比爬虫的请求超时时间长
								// 生产环境跑爬虫时改大些保险（30）
								page = pageBuffer.poll(11, TimeUnit.SECONDS);
								if (null == page) {
									req = requestBuffer.poll();
									if (null == req) {
										if (!downloader.isWorking()) {
											break;
										}
									} else {
										downloader.execute(req);
									}
								} else {
									processPage(page);
								}
							} else {
								downloader.execute(req);
							}
						} else {
							processPage(page);
						}
					}
				} finally {
					latch.countDown();
				}
			}

			private void processPage(P page) {
				try {
					pageProcessor.process(page);
				} catch (PageProcessException e) {
					throw new LogicError("未处理PageProcessException", e);
				}
			}
		};
		
		for (int i = 0; i < threadCount; i++) {
			new Thread(target).start();
		}
		target.run();
		
		if (threadCount > 0) {
			try {
				latch.await();
			} catch (InterruptedException e) {}
		}
	}

	public void close() {
		try {
			this.downloader.close();
		} catch (IOException e) {
			throw new Error("爬虫close失败", e);
		}
	}

	public Future<AsyncResult<RequestTask>> download(int method, String url, String content, ContentType contentType,
			FutureCallback<AsyncResult<RequestTask>> callback) throws UnsupportedEncodingException {
		return downloader.download(method, url, content, contentType, callback);
	}

	public boolean isWorking() {
		return downloader.isWorking();
	}
}
