package com.hbrb.exception;

public class ConfigError extends Error {
	private static final long serialVersionUID = 1L;

	public ConfigError() {
		super();
	}

	public ConfigError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ConfigError(String message, Throwable cause) {
		super(message, cause);
	}

	public ConfigError(String message) {
		super(message);
	}

	public ConfigError(Throwable cause) {
		super(cause);
	}

}
