package com.hbrb.spider.downloader.async;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;

import com.hbrb.exception.LogicError;
import com.hbrb.jdbc.MyDataSource;
import com.hbrb.spider.ConstantsHome;
import com.hbrb.spider.model.AsyncResult;
import com.hbrb.spider.model.page.HtmlPage;
import com.hbrb.spider.model.task.RequestTask;
import com.hbrb.util.JsoupUtils;

public class HtmlPageBufferAdaptor<T extends RequestTask> implements PageBuffer<T, HtmlPage<T>> {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HtmlPageBufferAdaptor.class);
	private final RawResultBuffer<T> buffer;

	public HtmlPageBufferAdaptor(RawResultBuffer<T> buffer) {
		super();
		this.buffer = buffer;
	}

	public HtmlPage<T> poll(int timeout, TimeUnit unit) {
		AsyncResult<T> rawResult;
		HtmlPage<T> page = null;
		IOException ex;
		do {
			ex = null;
			try {
				rawResult = buffer.poll(timeout, unit);
			} catch (InterruptedException e) {
				throw new LogicError("unexpected interruption", e);
			}
			if (null == rawResult) {
				return null;
			}
			try {
				page = JsoupUtils.buildHtmlPage(rawResult);
			} catch (IOException e) {
				ex = e;
				handle(rawResult, e);
			}
		} while (ex != null);
		return page;
	}

	public HtmlPage<T> poll() {
		HtmlPage<T> page = null;
		IOException ex;
		do {
			ex = null;
			AsyncResult<T> rawResult = buffer.poll();
			if (null == rawResult) {
				return null;
			}
			try {
				page = JsoupUtils.buildHtmlPage(rawResult);
			} catch (IOException e) {
				ex = e;
				handle(rawResult, e);
			}
		} while (ex != null);
		return page;
	}

	@Override
	public int size() {
		return buffer.size();
	}

	private void handle(AsyncResult<T> rawResult, IOException e) {
		String fileName = MyDataSource.generateId();
		logger.warn("unzip " + fileName + " failed - " + rawResult.getRequestTask().getUrl(), e);
		byte[] array = rawResult.getRawData().array();
		try {
			FileUtils.writeByteArrayToFile(new File(ConstantsHome.USER_DIR + File.separatorChar + fileName), array);
		} catch (IOException e1) {
			logger.warn("记录解压失败的网页失败 - " + rawResult.getRequestTask().getUrl(), e1);
		}
	}
}
