package com.hbrb.spider.model.task;

public class RequestTask {
	private String url;

	public void setUrl(String url) {
		this.url = url;
	}

	public RequestTask(String url) {
		super();
		this.url = url;
	}

	public String getUrl() {
		return url;
	}

	@Override
	public String toString() {
		return this.url;
	}
}
