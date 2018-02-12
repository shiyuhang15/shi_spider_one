package com.hbrb.spider.model.page;

import org.jsoup.nodes.Document;

import com.hbrb.spider.model.task.RequestTask;

public class HtmlPage<T extends RequestTask> extends Page<T> {
	private final Document document;

	public HtmlPage(T requestTask, Document document) {
		super(requestTask);
		this.document = document;
	}

	public Document getDocument() {
		return document;
	}
}
