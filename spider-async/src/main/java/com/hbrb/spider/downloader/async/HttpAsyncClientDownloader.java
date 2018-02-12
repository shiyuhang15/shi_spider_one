package com.hbrb.spider.downloader.async;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.apache.http.Header;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.message.BasicHeader;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.pool.PoolStats;

import com.hbrb.spider.downloader.EmptyCookieStore;
import com.hbrb.spider.downloader.ImgDownloader;
import com.hbrb.spider.model.AsyncResult;
import com.hbrb.spider.model.SpiderConfig;
import com.hbrb.spider.model.task.GenericRequestTask;
import com.hbrb.spider.model.task.RequestTask;

public class HttpAsyncClientDownloader<T extends RequestTask> implements ImgDownloader {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HttpAsyncClientDownloader.class);
	private final CloseableHttpAsyncClient client;
	private final PoolingNHttpClientConnectionManager connManager;
	private final FutureCallback<AsyncResult<T>> callback;
	// private final boolean useProxy;
	// private final boolean changeProxy;

	public HttpAsyncClientDownloader(FutureCallback<AsyncResult<T>> callback, SpiderConfig config
	/* , boolean useProxy, boolean changeProxy */){
		this.callback = callback;
		// this.useProxy = useProxy;
		// this.changeProxy = changeProxy;
		IOReactorConfig ioReactorConfig = IOReactorConfig.custom().setConnectTimeout(config.getReactorConnectTimeout() * 1000)
				.setSoTimeout(config.getReactorSoTimeout() * 1000).build();

		try {
			this.connManager = new PoolingNHttpClientConnectionManager(
					new DefaultConnectingIOReactor(ioReactorConfig));
		} catch (IOReactorException e) {
			throw new Error("new downloader失败", e);
		}
		connManager.setMaxTotal(config.getConnectionMaxTotal());
		connManager.setDefaultMaxPerRoute(config.getConnectionDefaultMaxPerRoute());

		RequestConfig defaultRequestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(config.getConnectionRequestTimeout() * 1000)
				.setConnectTimeout(config.getConnectTimeout() * 1000).setSocketTimeout(config.getSoTimeout() * 1000)
				.setCookieSpec(CookieSpecs.IGNORE_COOKIES).build();

		List<Header> defaultHeaders = new ArrayList<>(3);
		defaultHeaders.add(new BasicHeader("Accept", "text/html, application/xhtml+xml, image/jxr, */*"));
		defaultHeaders.add(new BasicHeader("Accept-Encoding", "gzip, deflate"));
		defaultHeaders.add(new BasicHeader("Accept-Language", "zh-CN"));
		CloseableHttpAsyncClient client = HttpAsyncClients.custom().setUserAgent(config.getUserAgent()).setDefaultHeaders(defaultHeaders)
				.setConnectionManager(connManager).setDefaultRequestConfig(defaultRequestConfig)
				.setDefaultCookieStore(new EmptyCookieStore()).build();

		this.client = client;
		client.start();
	}

	public void close() throws IOException {
		if (null != client) {
			client.close();
		}
	}

	public void execute(final T requestTask) {
		try {
			this.client.execute(HttpAsyncMethods.createGet(requestTask.getUrl()),
					ConsumerFactory.produceRawAsyncResponseConsumer(requestTask), /* context, */this.callback);
		} catch (Throwable e) {
			if (e instanceof IllegalStateException) {
				String message = e.getMessage();
				if (null != message && message.endsWith("I/O reactor status: STOPPED")) {
					throw e;
				}
			}
			logger.error("请求异常 - " + requestTask.getUrl(), e);
		}
	}

	public boolean isWorking() {
		PoolStats totalStats = connManager.getTotalStats();
		logger.info("TotalStats: {}", totalStats);
		if (totalStats.getLeased() == 0 && totalStats.getPending() == 0) {
			return false;
		}
		return true;
	}

	public Future<AsyncResult<RequestTask>> download(int method, String url, String content, ContentType contentType,
			FutureCallback<AsyncResult<RequestTask>> callback) throws UnsupportedEncodingException {
		HttpAsyncRequestProducer req;
		switch (method) {
		case GenericRequestTask.METHOD_POST:
			req = HttpAsyncMethods.createPost(url, content, contentType);
			break;
		case GenericRequestTask.METHOD_PUT:
			req = HttpAsyncMethods.createPut(url, content, contentType);
			break;
		default:
			req = HttpAsyncMethods.createGet(url);
			break;
		}

		return this.client.execute(req, ConsumerFactory.produceRawAsyncResponseConsumer(new RequestTask(url)),
				callback);
	}
	
	public void zeroCopyDownload(String imgSrc, File destFile) {
		this.client.execute(HttpAsyncMethods.createGet(imgSrc),
				ConsumerFactory.produceRawZeroCopyConsumer(imgSrc, destFile), null);
	}
}
