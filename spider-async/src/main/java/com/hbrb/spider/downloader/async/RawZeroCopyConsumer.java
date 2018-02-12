package com.hbrb.spider.downloader.async;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.channels.FileChannel;
import java.security.InvalidParameterException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentDecoderChannel;
import org.apache.http.nio.FileContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.protocol.AbstractAsyncResponseConsumer;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Asserts;

import com.hbrb.spider.exception.UnacceptableResponseException;

public class RawZeroCopyConsumer extends AbstractAsyncResponseConsumer<Object> {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RawZeroCopyConsumer.class);
	private static final int LIMIT_CONTENT_LENGTH = 10485760;
    private final File file;
    private final RandomAccessFile accessfile;

    private FileChannel fileChannel;
    private long idx = -1;
    private String target;

    public RawZeroCopyConsumer(final File file, String target) {
        super();
        this.target = target;
        this.file = file;
        try {
			this.accessfile = new RandomAccessFile(this.file, "rw");
		} catch (FileNotFoundException e) {
			throw new InvalidParameterException(e.getMessage());
		}
    }

    @Override
    protected void onResponseReceived(final HttpResponse response) throws HttpException, IOException {
        int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != HttpStatus.SC_OK) {
            throw new UnacceptableResponseException("zcsc:" + statusCode);
        }
    }

    @Override
    protected void onEntityEnclosed(
            final HttpEntity entity, final ContentType contentType) throws IOException {
        this.fileChannel = this.accessfile.getChannel();
        this.idx = 0;
    }

    @Override
    protected void onContentReceived(
            final ContentDecoder decoder, final IOControl ioctrl) throws IOException {
        Asserts.notNull(this.fileChannel, "File channel");
        final long transferred;
		if (decoder instanceof FileContentDecoder) {
			transferred = ((FileContentDecoder) decoder).transfer(this.fileChannel, this.idx, Integer.MAX_VALUE);
		} else {
			transferred = this.fileChannel.transferFrom(new ContentDecoderChannel(decoder), this.idx,
					Integer.MAX_VALUE);
		}
        if (transferred > 0) {
            this.idx += transferred;
        }
        if (decoder.isCompleted()) {
            this.fileChannel.close();
        }
        if (this.idx > LIMIT_CONTENT_LENGTH) {
        	throw new UnacceptableResponseException("response too long: " + this.idx);
        }
    }

    @Override
    protected Object buildResult(final HttpContext context) throws Exception {
        return null;
    }

    @Override
    protected void releaseResources() {
        try {
            this.accessfile.close();
        } catch (final IOException ignore) {
        }
		Exception ex = getException();
		if (null != ex) {
			if (this.file.isFile()) {
				this.file.delete();
			}
			if (ex instanceof UnacceptableResponseException) {
				if (null != ex.getMessage()) {
					logger.info("{} - {}", ex.getMessage(), target);
				}
			} else {
				if (ex instanceof UnknownHostException) {
					logger.warn("UnknownHost - {}", target);
				} else if (ex instanceof SocketTimeoutException) {
					logger.warn("SocketTimeout - {}", target);
				} else {
					logger.warn("download failed - " + target, ex);
				}
			}
		}
		target = null;
    }

}
