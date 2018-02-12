package com.hbrb.spider.downloader.async;

import java.nio.ByteBuffer;

import org.apache.http.nio.util.SimpleInputBuffer;

public class MySimpleInputBuffer extends SimpleInputBuffer {
	public MySimpleInputBuffer(int buffersize) {
		super(buffersize);
	}
	// 就为获取这个buffer
	public ByteBuffer getByteBuffer() {
		return buffer;
	}
}
