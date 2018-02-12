package com.hbrb.spider.service.impl;

import java.util.List;
import java.util.regex.Pattern;

import org.dom4j.Document;
import org.dom4j.Element;

import com.hbrb.spider.exception.TemplateParseException;
import com.hbrb.spider.model.article.FieldName;
import com.hbrb.spider.model.template.ArticleField;
import com.hbrb.spider.model.template.FieldExtractRule;
import com.hbrb.spider.model.template.PageTemplate;
import com.hbrb.util.TaskUtils;

public class CachedJDBCPageTemplateService extends CachedJDBCTemplateService<PageTemplate> {
	private final String tableName = "t_template_page";
	@Override
	String getTableName() {
		return tableName;
	}

	@SuppressWarnings("unchecked")
	@Override
	public PageTemplate buildTemplate(Document doc) throws TemplateParseException {
		if (null == doc) {
			return null;
		}
		PageTemplate template = new PageTemplate();
		boolean isEmpty = true;
		
		// 标题过滤正则
		List<Element> ignoreTitleRegEles = doc.selectNodes("/page/ignore/title/regex");
		int ignoreTitleRegsSize = ignoreTitleRegEles.size();
		if (ignoreTitleRegsSize > 0) {
			isEmpty = false;
//			List<Pattern> pas = new ArrayList<Pattern>(ignoreTitleRegsSize);
			Pattern[] pas = new Pattern[ignoreTitleRegsSize];
			for (int i = 0; i < ignoreTitleRegsSize; i++) {
				Element regEle = ignoreTitleRegEles.get(i);
				String reg = regEle.getTextTrim();
				if (reg.isEmpty()) {
					throw new TemplateParseException("/fields/ignoreTitle/regex[" + i + "]为空");
				}
				pas[i] = Pattern.compile(reg);
			}
			template.setIgnoreTitlePatterns(pas);
		}

		// 图片抽取配置
		List<Element> ruleEles = doc.selectNodes("/page/imgs/extractor");
		if (null != ruleEles && !ruleEles.isEmpty()) {
			isEmpty = false;
			FieldExtractRule[] rules = parseRules(ruleEles, FieldName.IMGS);
			template.setImgsExtractRules(rules);
		}

		// 抽取配置
		List<Element> fieldEles = doc.selectNodes("/page/fields/field");
		int size;
		if (null == fieldEles || (size = fieldEles.size()) == 0) {
			if (isEmpty) {
				throw new TemplateParseException("nothing configed");
			}
			return template;
		}
		ArticleField[] fields = new ArticleField[size];
		for (int i = 0; i < size; i++) {
			Element fieldEle = fieldEles.get(i);
			String name = fieldEle.attributeValue("name");
			if (null == name || (name = name.trim()).length() == 0) {
				throw new TemplateParseException("抓取项无名称");
			}
			switch (name) {
			case FieldName.TITLE:
				name = FieldName.TITLE;
				break;
			case FieldName.CONTENT:
				name = FieldName.CONTENT;
				break;
			case FieldName.AUTHORS:
				name = FieldName.AUTHORS;
				break;
			case FieldName.CHANNEL:
				name = FieldName.CHANNEL;
				break;
			case FieldName.DESCRIPTION:
				name = FieldName.DESCRIPTION;
				break;
			case FieldName.KEYWORDS:
				name = FieldName.KEYWORDS;
				break;
			case FieldName.PUBSOURCE:
				name = FieldName.PUBSOURCE;
				break;
			case FieldName.PUBTIME:
				name = FieldName.PUBTIME;
				break;
			case FieldName.PRETITLE:
				name = FieldName.PRETITLE;
				break;
			case FieldName.SUBTITLE:
				name = FieldName.SUBTITLE;
				break;
			case FieldName.TAGS:
				name = FieldName.TAGS;
				break;
			default:
				throw new TemplateParseException("未识别的抽取字段：" + name);
			}

			ArticleField field = new ArticleField();
			field.setName(name);
			String required = fieldEle.attributeValue("required");
			if (null != required && !required.isEmpty()) {
				field.setRequired(true);
			}
			String disableAuto = fieldEle.attributeValue("disableAuto");
			if (null != disableAuto && !disableAuto.isEmpty()) {
				field.setDisableAuto(true);
			}
			ruleEles = fieldEle.elements("extractor");
			if (!ruleEles.isEmpty()) {
				field.setExtractRules(parseRules(ruleEles, name));
			}

			fields[i] = field;
		}
		template.setFields(fields);

		return template;
	}

	private FieldExtractRule[] parseRules(List<Element> ruleEles, String fieldName) throws TemplateParseException {
		int size = ruleEles.size();
		FieldExtractRule[] rules = new FieldExtractRule[size];
		for (int i = 0; i < size; i++) {
			Element ruleEle = ruleEles.get(i);
			
			// location
			String location = ruleEle.getTextTrim();
			if (location.isEmpty()) {
				throw new TemplateParseException("字段[" + fieldName + "]抽取规则为空");
			}
			FieldExtractRule rule = new FieldExtractRule();
			rule.setLocation(location);
//			rule.setType(type);
			// index
			String index = ruleEle.attributeValue("index");
			if (null != index && !index.isEmpty()) {
				rule.setIndex(Integer.parseInt(index));
			}
			// method
			String method = ruleEle.attributeValue("method");
			if (null != method && !method.isEmpty()) {
				rule.setMethod(Integer.parseInt(method));
			}
			// regex
			String regex = ruleEle.attributeValue("regex");
			if (null != regex && !regex.isEmpty()) {
				rule.setPattern(Pattern.compile(regex));
			}
			// group
			String group = ruleEle.attributeValue("group");
			if (null != group && !group.isEmpty()) {
				rule.setGroup(Integer.parseInt(group));
			}
			
			rules[i] = rule;
		}
		return rules;
	}

	@Override
	boolean dependsOnTemplate(int type) {
		return TaskUtils.dependsOnPageTemplate(type);
	}

}
