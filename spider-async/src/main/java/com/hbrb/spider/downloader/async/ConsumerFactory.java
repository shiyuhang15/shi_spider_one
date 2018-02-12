package com.hbrb.spider.downloader.async;

import java.io.File;

import org.apache.http.nio.protocol.HttpAsyncResponseConsumer;

import com.hbrb.spider.model.AsyncResult;
import com.hbrb.spider.model.task.RequestTask;

public class ConsumerFactory {
	public static <T extends RequestTask> HttpAsyncResponseConsumer<AsyncResult<T>> produceRawAsyncResponseConsumer(T requestTask){
		return new RawAsyncResponseConsumer<>(requestTask);
	}
	public static RawZeroCopyConsumer produceRawZeroCopyConsumer(String imgSrc, File destFile) {
		return new RawZeroCopyConsumer(destFile, imgSrc);
	}
}
