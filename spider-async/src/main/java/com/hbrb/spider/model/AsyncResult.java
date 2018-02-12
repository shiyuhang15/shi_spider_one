package com.hbrb.spider.model;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.hbrb.spider.model.task.RequestTask;

public class AsyncResult<T extends RequestTask> extends RawResult {
	private final T requestTask;
	/**
	 * 0：无压缩；1：gzip；2：deflate
	 */
	private final int contentEncoding;

	public AsyncResult(T requestTask, int statusCode, ByteBuffer rawData, String charsetName, int contentEncoding,
			String lastRedirectLocation) throws IOException {
		super(statusCode, rawData, charsetName);
		this.requestTask = requestTask;
		this.contentEncoding = contentEncoding;
		this.setLastRedirectLocation(lastRedirectLocation);
	}

	public T getRequestTask() {
		return requestTask;
	}

	public int getContentEncoding() {
		return contentEncoding;
	}
	
	@Override
	public String toString() {
		return getStatusCode() + ('-' + requestTask.getUrl());
	}
}
