package com.hbrb.spider.exception;

public class PageProcessException extends Exception {
	private static final long serialVersionUID = 1L;
	private String url;
	private String content;

	public PageProcessException() {
		super();
	}

	public PageProcessException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public PageProcessException(String message, Throwable cause) {
		super(message, cause);
	}

	public PageProcessException(String message) {
		super(message);
	}

	public PageProcessException(Throwable cause) {
		super(cause);
	}

	public PageProcessException(String message, String content, String url, Throwable cause) {
		super(message, cause);
		this.setUrl(url);
		this.setContent(content);
	}
	
	public PageProcessException(String message, String content, String url) {
		super(message);
		this.setUrl(url);
		this.setContent(content);
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
	
}
