package com.hbrb.spider.downloader.sync;

import com.hbrb.json.JSUtils;
import com.hbrb.spider.model.RawResult;
import com.hbrb.spider.model.page.JSObjectPage;
import com.hbrb.spider.model.task.RequestTask;

public class JSObjectPageBuilder<T extends RequestTask> extends PageDecoder<T, JSObjectPage<T>> {

	public JSObjectPageBuilder(String charsetName) {
		super(charsetName);
	}

	@Override
	public JSObjectPage<T> build(T requestTask, RawResult res) {
		return new JSObjectPage<T>(requestTask, JSUtils.createJSObject(decode(res)));
	}
}
