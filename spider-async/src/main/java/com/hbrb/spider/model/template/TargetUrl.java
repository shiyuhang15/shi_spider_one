package com.hbrb.spider.model.template;

import java.util.regex.Pattern;

public class TargetUrl {
	private Pattern pattern;
	private String datePattern;
	private String dateRegex;

	public TargetUrl(Pattern pattern) {
		super();
		this.pattern = pattern;
	}

	public String getDatePattern() {
		return datePattern;
	}

	public void setDatePattern(String datePattern) {
		this.datePattern = datePattern;
	}

	public String getDateRegex() {
		return dateRegex;
	}

	public void setDateRegex(String dateRegex) {
		this.dateRegex = dateRegex;
	}

	public Pattern getPattern() {
		return pattern;
	}

	public void setPattern(Pattern pattern) {
		this.pattern = pattern;
	}
}
