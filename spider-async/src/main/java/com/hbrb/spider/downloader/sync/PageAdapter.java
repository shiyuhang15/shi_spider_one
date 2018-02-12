package com.hbrb.spider.downloader.sync;

import java.io.IOException;

import org.apache.http.client.protocol.HttpClientContext;

import com.hbrb.spider.model.RawResult;
import com.hbrb.spider.model.page.Page;
import com.hbrb.spider.model.task.GenericRequestTask;

public class PageAdapter<T extends GenericRequestTask, P extends Page<T>> {
	private final HttpClientDownloader downloader;
	private final PageBuilder<T, P> pageBuilder;

	public PageAdapter(HttpClientDownloader downloader, PageBuilder<T, P> pageBuilder) {
		super();
		this.downloader = downloader;
		this.pageBuilder = pageBuilder;
	}

	public P execute(T requestTask) {
		return execute(requestTask, null);
	}

	public P execute(T requestTask, HttpClientContext context) {
		RawResult res = downloader.execute(requestTask, context);
		if (null == res) {
			return null;
		}
		return pageBuilder.build(requestTask, res);
	}

	public void close() throws IOException {
		downloader.close();
	}
}
