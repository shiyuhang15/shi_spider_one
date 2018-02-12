package com.hbrb.spider.model.task;

public class SiteTask extends TemplateTask implements Cloneable {
	private String title;
	private int deep;

	public SiteTask(String url) {
		super(url);
	}
	
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getDeep() {
		return deep;
	}

	public void setDeep(int deep) {
		this.deep = deep;
	}

	@Override
	public SiteTask clone() throws CloneNotSupportedException {
		return (SiteTask) super.clone();
	}
}
