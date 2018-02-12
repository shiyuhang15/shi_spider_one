package com.hbrb.spider.downloader.async;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.protocol.AbstractAsyncResponseConsumer;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import com.hbrb.spider.ConstantsHome;
import com.hbrb.spider.Spider;
import com.hbrb.spider.exception.UnacceptableResponseException;
import com.hbrb.spider.model.AsyncResult;
import com.hbrb.spider.model.task.RequestTask;

public class RawAsyncResponseConsumer<T extends RequestTask> extends AbstractAsyncResponseConsumer<AsyncResult<T>> {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RawAsyncResponseConsumer.class);
	private static final int LIMIT_CONTENT_LENGTH = 10485760;
	private volatile MySimpleInputBuffer buf;
	private volatile String charsetName;
	private volatile int statusCode;
	/**
	 * 0：无压缩；1：gzip；2：deflate
	 */
	private volatile int contentEncoding = 0; 
	private final T requestTask;
	public RawAsyncResponseConsumer(T requestTask) {
		super();
		this.requestTask = requestTask;
	}

	@Override
	protected void onResponseReceived(HttpResponse response) throws HttpException, IOException {
		statusCode = response.getStatusLine().getStatusCode();
		logger.info("sc:{} - {}", statusCode, requestTask.getUrl());
		
		if (statusCode != HttpStatus.SC_OK) {
			throw new UnacceptableResponseException();
		}
		
		if (!Locale.SIMPLIFIED_CHINESE.equals(response.getLocale())) {
			throw new UnacceptableResponseException("unsupported locale: " + response.getLocale());
		}
		
		Header contentType = response.getFirstHeader(HTTP.CONTENT_TYPE);
		if (null != contentType) {
			String ct = contentType.getValue();
			if (null != ct && !ct.isEmpty()) {
				ct = ct.toLowerCase().trim();
				if (!ct.startsWith("text/") && !ct.contains("json") && !ct.contains("javascript")) {
					throw new UnacceptableResponseException("unsupport Content-Type:" + ct);
				}
			}
		}
		
		// FIXME test
		if (Spider.judgeExpired) {
			Header lastModified = response.getFirstHeader("Last-Modified");
			if (null != lastModified) {
				String value = lastModified.getValue();
				if (null != value) {
					Date lastModifiedDate = DateUtils.parseDate(value);
					if (null != lastModifiedDate && System.currentTimeMillis() - lastModifiedDate.getTime() > ConstantsHome.EFFECTIVE_PERIOD_TARGET) {
						throw new UnacceptableResponseException("expired:" + lastModifiedDate.getTime());
					}
				}
			}
		}
		
		Header[] ceHeaders = response.getHeaders(HTTP.CONTENT_ENCODING);
		if (null != ceHeaders) {
			for (Header header : ceHeaders) {
				String value = header.getValue().toLowerCase();
				if (null != value) {
					if (value.contains("gzip")) {
						contentEncoding = ConstantsHome.ENCODING_GZIP;
					} else if (value.contains("deflate")) {
						contentEncoding = ConstantsHome.ENCODING_DEFLATE;
					}
				}
			}
		}
	}

	@Override
	protected void onContentReceived(ContentDecoder decoder, IOControl ioctrl) throws IOException {
		this.buf.consumeContent(decoder);
		if (this.buf.length() > LIMIT_CONTENT_LENGTH) {
			throw new UnacceptableResponseException("response too long: " + this.buf.length());
		}
	}

	@Override
	protected void onEntityEnclosed(HttpEntity entity, ContentType contentType) throws IOException {
        long len = entity.getContentLength();
        if (len > LIMIT_CONTENT_LENGTH) {
            throw new UnacceptableResponseException("response too long: " + len);
        }
        if (len < 0) {
            len = 4096;
        }
        this.buf = new MySimpleInputBuffer((int) len);
        if (null != contentType) {
        	Charset charset = contentType.getCharset();
        	if (null != charset) {
        		charsetName = charset.name();
			}
		}
    }

	@Override
	protected AsyncResult<T> buildResult(HttpContext context) throws Exception {
		String lastRedirectLocation = null;
		List<URI> redirectLocations = HttpClientContext.adapt(context).getRedirectLocations();
		if (null != redirectLocations && !redirectLocations.isEmpty()) {
			lastRedirectLocation = redirectLocations.get(redirectLocations.size() - 1).toString();
		}
		return new AsyncResult<T>(this.requestTask, this.statusCode, this.buf == null ? null : this.buf.getByteBuffer(),
				this.charsetName, this.contentEncoding, lastRedirectLocation);
	}

	@Override
	protected void releaseResources() {
		this.buf = null;
		this.charsetName = null;
		Exception ex = getException();
		if (null != ex) {
			if (ex instanceof UnacceptableResponseException) {
				if (null != ex.getMessage()) {
					logger.info("{} - {}", ex.getMessage(), this.requestTask.getUrl());
				}
			} else {
				if (ex instanceof UnknownHostException) {
					logger.warn("UnknownHost - {}", this.requestTask.getUrl());
				} else if (ex instanceof SocketTimeoutException) {
					logger.warn("SocketTimeout - {}", this.requestTask.getUrl());
				} else {
					logger.warn("request failed - " + this.requestTask.getUrl(), ex);
				}
			}
		}
	}

	@Override
	protected ContentType getContentType(HttpEntity entity) {
		return entity != null ? ContentType.get(entity) : null;
	}
}
