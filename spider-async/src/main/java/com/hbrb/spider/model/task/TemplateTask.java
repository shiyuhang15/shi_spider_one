package com.hbrb.spider.model.task;

public class TemplateTask extends RequestTask {
	private int siteTaskId;
	/**
	 * 0：自动化任务；1：模板A；2：模板B；3：模板AB
	 */
	private int type;
	/**
	 * com.hbrb.spider.model.article.SourceType
	 */
	private int sourceType;

	public int getSourceType() {
		return sourceType;
	}

	public void setSourceType(int sourceType) {
		this.sourceType = sourceType;
	}

	public TemplateTask(String url) {
		super(url);
	}

	public int getSiteTaskId() {
		return siteTaskId;
	}

	public int getType() {
		return type;
	}

	public void setSiteTaskId(int siteTaskId) {
		this.siteTaskId = siteTaskId;
	}

	public void setType(int type) {
		this.type = type;
	}
}
