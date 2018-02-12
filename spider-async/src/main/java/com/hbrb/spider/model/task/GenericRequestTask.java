package com.hbrb.spider.model.task;

import java.util.Map;

import org.apache.http.HttpEntity;

public class GenericRequestTask extends RequestTask {
	public static final int METHOD_GET = 0;
	public static final int METHOD_POST = 1;
	public static final int METHOD_PUT = 2;
	private int method;
	private int type;
	private int retryCount;
	private Map<String, String> headers;
	private HttpEntity entity;

	public GenericRequestTask(int method, String url, int type) {
		super(url);
		this.method = method;
		this.type = type;
	}

	public GenericRequestTask(String url, int type) {
		this(0, url, type);
	}

	public GenericRequestTask(String url) {
		this(0, url, 0);
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	public int getMethod() {
		return method;
	}

	public HttpEntity getEntity() {
		return entity;
	}

	public void setEntity(HttpEntity entity) {
		this.entity = entity;
	}

	public int getRetryCount() {
		return retryCount;
	}

	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}

	public int getType() {
		return type;
	}
}
