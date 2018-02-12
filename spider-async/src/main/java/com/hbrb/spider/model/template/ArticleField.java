package com.hbrb.spider.model.template;

public class ArticleField {
	private String name;
	private boolean  disableAuto = false;
	private boolean required = false;
	private FieldExtractRule[] extractRules;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public boolean isDisableAuto() {
		return disableAuto;
	}

	public void setDisableAuto(boolean disableAuto) {
		this.disableAuto = disableAuto;
	}

	public FieldExtractRule[] getExtractRules() {
		return extractRules;
	}

	public void setExtractRules(FieldExtractRule[] extractRules) {
		this.extractRules = extractRules;
	}

	@Override
	public String toString() {
		return this.name;
	}
	
}
