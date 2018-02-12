package com.hbrb.spider.model.template;

import java.util.regex.Pattern;

public class PageTemplate {
	private ArticleField[] fields;
	private FieldExtractRule[] imgsExtractRules;
	private Pattern[] ignoreTitlePatterns;

	public ArticleField[] getFields() {
		return fields;
	}

	public void setFields(ArticleField[] fields) {
		this.fields = fields;
	}

	public FieldExtractRule[] getImgsExtractRules() {
		return imgsExtractRules;
	}

	public void setImgsExtractRules(FieldExtractRule[] imgsExtractRules) {
		this.imgsExtractRules = imgsExtractRules;
	}

	public Pattern[] getIgnoreTitlePatterns() {
		return ignoreTitlePatterns;
	}

	public void setIgnoreTitlePatterns(Pattern[] ignoreTitlePatterns) {
		this.ignoreTitlePatterns = ignoreTitlePatterns;
	}
}
