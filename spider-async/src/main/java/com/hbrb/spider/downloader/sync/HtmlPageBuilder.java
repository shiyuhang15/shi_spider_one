package com.hbrb.spider.downloader.sync;

import com.hbrb.spider.model.RawResult;
import com.hbrb.spider.model.page.HtmlPage;
import com.hbrb.spider.model.task.RequestTask;
import com.hbrb.util.JsoupUtils;

public class HtmlPageBuilder<T extends RequestTask> extends AbstractPageBuilder<T, HtmlPage<T>> {

	public HtmlPageBuilder(String charsetName) {
		super(charsetName);
	}

	@Override
	public HtmlPage<T> build(T requestTask, RawResult res) {
		String url = res.getLastRedirectLocation();
		if (null == url) {
			url = requestTask.getUrl();
		}
		return new HtmlPage<>(requestTask,
				JsoupUtils.parseByteData(res.getRawData(), getCharsetName(res), url));
	}
}
