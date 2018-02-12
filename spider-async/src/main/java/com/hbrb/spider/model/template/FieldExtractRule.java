package com.hbrb.spider.model.template;

import java.util.regex.Pattern;

public class FieldExtractRule {
	/**
	 * 规则类型  0：XPATH；1：CSS；
	 */
//	private int type = 0;
	private String location;
	private int index = -1;
	/**
	 * 抽取方式  0：ownText()；1：text()；2：outerHtml()
	 */
	private int method = 0;
	private Pattern pattern;
	private int group = 0;
//	private boolean extractOwnText = false;
//	private boolean keepTags = false;
//	private Pattern pattern;
//	private int group = 0;

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public int getMethod() {
		return method;
	}

	public void setMethod(int method) {
		this.method = method;
	}

	public Pattern getPattern() {
		return pattern;
	}

	public void setPattern(Pattern pattern) {
		this.pattern = pattern;
	}

	public int getGroup() {
		return group;
	}

	public void setGroup(int group) {
		this.group = group;
	}

	@Override
	public String toString() {
		return this.location;
	}
}
