package com.hbrb.spider.exception;

public class TemplateParseException extends Exception {
	private static final long serialVersionUID = 1L;

	public TemplateParseException() {
		super();
	}

	public TemplateParseException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public TemplateParseException(String message, Throwable cause) {
		super(message, cause);
	}

	public TemplateParseException(String message) {
		super(message);
	}

	public TemplateParseException(Throwable cause) {
		super(cause);
	}

}
