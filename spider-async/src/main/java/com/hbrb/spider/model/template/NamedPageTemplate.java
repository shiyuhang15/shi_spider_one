package com.hbrb.spider.model.template;

public class NamedPageTemplate extends PageTemplate {
	private String name;
	private int sourceType;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getSourceType() {
		return sourceType;
	}

	public void setSourceType(int sourceType) {
		this.sourceType = sourceType;
	}
}
