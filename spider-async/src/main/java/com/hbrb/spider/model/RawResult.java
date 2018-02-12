package com.hbrb.spider.model;

import java.nio.ByteBuffer;

public class RawResult {
	private final int statusCode;
	private final ByteBuffer rawData;
	private final String charsetName;
	private String lastRedirectLocation;

	public RawResult(int statusCode, ByteBuffer rawData, String charsetName) {
		super();
		this.statusCode = statusCode;
		this.rawData = rawData;
		this.charsetName = charsetName;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public ByteBuffer getRawData() {
		return rawData;
	}

	public String getCharsetName() {
		return charsetName;
	}

	public String getLastRedirectLocation() {
		return lastRedirectLocation;
	}

	public void setLastRedirectLocation(String lastRedirectLocation) {
		this.lastRedirectLocation = lastRedirectLocation;
	}

}
