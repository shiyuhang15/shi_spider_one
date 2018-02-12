package com.hbrb.spider.exception;

import java.io.IOException;

public class UnacceptableResponseException extends IOException {
	private static final long serialVersionUID = 1L;

	public UnacceptableResponseException() {
		super();
	}

	public UnacceptableResponseException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnacceptableResponseException(String message) {
		super(message);
	}

	public UnacceptableResponseException(Throwable cause) {
		super(cause);
	}

}
