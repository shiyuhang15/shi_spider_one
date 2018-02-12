package com.hbrb.spider.downloader.sync;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import com.hbrb.exception.LogicError;
import com.hbrb.spider.model.RawResult;
import com.hbrb.spider.model.page.Page;
import com.hbrb.spider.model.task.RequestTask;

public abstract class PageDecoder<T extends RequestTask, P extends Page<T>> extends AbstractPageBuilder<T, P> {
	public PageDecoder(String charsetName) {
		super(charsetName);
	}

	String decode(RawResult res) {
		String charsetName = getCharsetName(res);
		if (null == charsetName) {
			charsetName = Charset.defaultCharset().name();
		}
		String rawText;
		try {
			rawText = new String(res.getRawData().array(), charsetName);
		} catch (UnsupportedEncodingException e) {
			throw new LogicError("解码异常 - " + charsetName, e);
		}
		return rawText;
	}
}
