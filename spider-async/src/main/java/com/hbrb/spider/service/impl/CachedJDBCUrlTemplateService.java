package com.hbrb.spider.service.impl;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.dom4j.Document;
import org.dom4j.Element;

import com.hbrb.spider.ConstantsHome;
import com.hbrb.spider.exception.TemplateParseException;
import com.hbrb.spider.model.Region;
import com.hbrb.spider.model.template.HOFPExtractRule;
import com.hbrb.spider.model.template.Rectangle;
import com.hbrb.spider.model.template.TargetUrl;
import com.hbrb.spider.model.template.UrlsTemplate;
import com.hbrb.util.TaskUtils;

public class CachedJDBCUrlTemplateService extends CachedJDBCTemplateService<UrlsTemplate> {
	private final String tableName = "t_template_urls";
	@Override
	String getTableName() {
		return tableName;
	}

	@Override
	@SuppressWarnings("unchecked")
	UrlsTemplate buildTemplate(Document doc) throws TemplateParseException {
		if (null == doc) {
			return null;
		}
		UrlsTemplate template = new UrlsTemplate();
				
		// 是否启用自动化抽取目标URL作为补充
		String autoTarget = doc.valueOf("/urls/target/@disableAuto").trim();
		if (!autoTarget.isEmpty()) {
			template.setAutoExtractTargetAsSupplement(false);
		}
		
		// 是否启用自动化抽取导航URL作为补充
		String autoNavi = doc.valueOf("/urls/navi/@disableAuto").trim();
		if (!autoNavi.isEmpty()) {
			template.setAutoExtractNaviAsSupplement(false);
		}
		
		// 深度配置
		Number bottomNum = doc.numberValueOf("/urls/navi/@bottom");
		if (!"NaN".equals(bottomNum.toString())) {
			int bottom = bottomNum.intValue();
			if (bottom >= 0) {
				template.setBottom(bottom);
			}
		}

		List<Element> deepRegEles = doc.selectNodes("/urls/deep/regex");
		int deepRegSize = deepRegEles.size();
		if (deepRegSize > 0) {
			SimpleEntry<Pattern, Integer>[] deepEntries = new SimpleEntry[deepRegSize];
			for (int i = 0; i < deepRegSize; i++) {
				Element deepRegEle = deepRegEles.get(i);
				String deepText = deepRegEle.attributeValue("deep");
				if (null == deepText || (deepText = deepText.trim()).isEmpty()) {
					throw new TemplateParseException("/urls/deep/regex@deep[" + i + "]不存在");
				}
				int deep = -1;
				try {
					deep = Integer.parseInt(deepText);
				} catch (NumberFormatException e) {
					throw new TemplateParseException("/urls/deep/regex@deep[" + i + "]非数字");
				}
				if (deep < 0) {
					throw new TemplateParseException("/urls/deep/regex@deep[" + i + "]小于0");
				}
				String deepRegex = deepRegEle.getTextTrim();
				if (deepRegex.isEmpty()) {
					throw new TemplateParseException("/urls/deep/regex[" + i + "]为空");
				}
				deepEntries[i] = new SimpleEntry<Pattern, Integer>(Pattern.compile(deepRegex), deep);
			}
			template.setDeepEntries(deepEntries);
		}
		
		// startUrl
		List<Element> startUrlEles = doc.selectNodes("/urls/start/url");
		int startUrlSize = startUrlEles.size();
		if (startUrlSize > 0) {
			String[] startUrls = new String[startUrlSize];
			for (int i = 0; i < startUrlSize; i++) {
				Element startUrlEle = startUrlEles.get(i);
				String startUrl = startUrlEle.getTextTrim();
				if (startUrl.isEmpty()) {
					throw new TemplateParseException("/urls/start/url[" + i + "]为空");
				}
				String refresh = startUrlEle.attributeValue("refresh");
				if (null != refresh && !refresh.isEmpty()) {
					startUrl += ConstantsHome.MARK_URL_START_REFRESH;
				}
				startUrls[i] = startUrl;
			}
			template.setStartUrls(startUrls);
		}
		
		// startPattern
		List<Element> startUrlPatternEles = doc.selectNodes("/urls/start/pattern");
		int startUrlPatternSize = startUrlPatternEles.size();
		if (startUrlPatternSize > 0) {
//			List<String> startUrlPatterns = new ArrayList<String>(startUrlPatternSize);
			String[] startUrlPatterns = new String[startUrlPatternSize];
			for (int i = 0; i < startUrlPatternSize; i++) {
				Element startUrlPatternEle = startUrlPatternEles.get(i);
				String startUrlPattern = startUrlPatternEle.getTextTrim();
				if (startUrlPattern.isEmpty()) {
					throw new TemplateParseException("/urls/start/pattern[" + i + "]为空");
				}
				// 跳转
				String refresh = startUrlPatternEle.attributeValue("refresh");
				if (null != refresh && !refresh.isEmpty()) {
					if (startUrlPattern.charAt(startUrlPattern.length() - 1) == '\'') {
						startUrlPattern = startUrlPattern.substring(0, startUrlPattern.length() - 1) + ConstantsHome.MARK_URL_START_REFRESH + '\'';
					} else {
						startUrlPattern = startUrlPattern + '\'' + ConstantsHome.MARK_URL_START_REFRESH + '\'';
					}
				}
				// 延迟
				String postpone = startUrlPatternEle.attributeValue("postpone");
				if (null != postpone && !postpone.isEmpty()) {
					startUrlPattern += ConstantsHome.MARK_URL_START_POSTPONE;
				}
				startUrlPatterns[i] = startUrlPattern;
			}
			template.setStartUrlPatterns(startUrlPatterns);
		}
		String[] startUrls = template.getStartUrls();
		String[] startUrlPatterns = template.getStartUrlPatterns();
		if ((null == startUrls || startUrls.length == 0)
				&& (null == startUrlPatterns || startUrlPatterns.length == 0)) {
			throw new TemplateParseException("没有start");
		}
		
		// 导航正则
		List<Element> helpRegEles = doc.selectNodes("/urls/navi/regex");
		if (!helpRegEles.isEmpty()) {
			List<Pattern> pas = null;
			List<Pattern> fuzzyPas = null;
			for (Element regEle : helpRegEles) {
				String reg = regEle.getTextTrim();
				if (!reg.isEmpty()) {
					String fuzzy = regEle.attributeValue("fuzzy");
					if (null != fuzzy && !fuzzy.isEmpty()) {
						if (null == fuzzyPas) {
							fuzzyPas = new ArrayList<Pattern>();
						}
						fuzzyPas.add(Pattern.compile(reg));
					} else {
						if (null == pas) {
							pas = new ArrayList<Pattern>();
						}
						pas.add(Pattern.compile(reg));
					}
				}
			}
			if (null != pas) {
				template.setNaviUrlPatterns(pas.toArray(new Pattern[pas.size()]));
			}
			if (null != fuzzyPas) {
				template.setFuzzyNaviUrlPatterns(fuzzyPas.toArray(new Pattern[fuzzyPas.size()]));
			}
		}
		
		// targetUrl
		List<Element> targetRegEles = doc.selectNodes("/urls/target/regex");
		int taRegsSize = targetRegEles.size();
		if (taRegsSize > 0) {
//			List<TargetUrl> targetUrls = new ArrayList<TargetUrl>(taRegsSize);
			TargetUrl[] targetUrls = new TargetUrl[taRegsSize];
			for (int i = 0; i < taRegsSize; i++) {
				Element regEle = targetRegEles.get(i);
				String regex = regEle.getTextTrim();
				if (regex.isEmpty()) {
					throw new TemplateParseException("/urls/target/regex[" + i + "]为空");
				}
				TargetUrl targetUrl = new TargetUrl(Pattern.compile(regex));
				String datePattern = regEle.attributeValue("datePattern");
				String dateRegex = regEle.attributeValue("dateRegex");
				if (null != datePattern
						&& !(datePattern = datePattern.trim()).isEmpty()
						&& null != dateRegex
						&& !(dateRegex = dateRegex.trim()).isEmpty()) {
					targetUrl.setDatePattern(datePattern);
					targetUrl.setDateRegex(dateRegex);
				}
				targetUrls[i] = targetUrl;
			}
			template.setTargetUrls(targetUrls);
		}
		
		// 头版头条
		List<Element> hofpEles = doc.selectNodes("/urls/target/HOFP/extractor");
		int hofpSize = hofpEles.size();
		if (hofpSize != 0) {
			HOFPExtractRule[] hofps = new HOFPExtractRule[hofpSize];
			for (int i = 0; i < hofpSize; i++) {
				hofps[i] = new HOFPExtractRule();
				Element hofpEle = hofpEles.get(i);
				String text = hofpEle.getTextTrim();
				if (null != text && !text.isEmpty()) {
					hofps[i].setLocation(text);
				}
				String indexTxt = hofpEle.attributeValue("index");
				if (null != indexTxt) {
					try {
						hofps[i].setIndex(Integer.parseInt(indexTxt));
					} catch (NumberFormatException e) {
						throw new TemplateParseException("HOFP index", e);
					}
				}
				
				String limit = hofpEle.attributeValue("limit");
				if (null != limit) {
					String xValue = hofpEle.attributeValue("x");
					if (null == xValue) {
						throw new TemplateParseException("HOFP limit x == null");
					}
					String yValue = hofpEle.attributeValue("y");
					if (null == yValue) {
						throw new TemplateParseException("HOFP limit y == null");
					}
					String widthValue = hofpEle.attributeValue("width");
					if (null == widthValue) {
						throw new TemplateParseException("HOFP limit width == null");
					}
					String heightValue = hofpEle.attributeValue("height");
					if (null == heightValue) {
						throw new TemplateParseException("HOFP limit height == null");
					}
					try {
						hofps[i].setLimit(new Rectangle(Integer.parseInt(xValue), Integer.parseInt(yValue),
								Integer.parseInt(widthValue), Integer.parseInt(heightValue)));
					} catch (NumberFormatException e) {
						throw new TemplateParseException("HOFP limit", e);
					}
				}
				
				if (hofps[i].getLocation() == null && hofps[i].getLimit() == null) {
					throw new TemplateParseException("HOFP rule is null");
				}
			}
			template.setHofps(hofps);
		}
		
		// disableCommonReg
		String disableCommonReg = doc.valueOf("/urls/ignore/@disableCommon").trim();
		if (!disableCommonReg.isEmpty()) {
			template.setDisableCommonIgnore(true);
		}

		// ignorePrefix
		List<Element> prefixes = doc.selectNodes("/urls/ignore/prefix");
		int ignorePrefixesSize = prefixes.size();
		if (ignorePrefixesSize > 0) {
			String[] ignoreUrlPrefixes = new String[ignorePrefixesSize];
			for (int i = 0; i < ignorePrefixesSize; i++) {
				Element prefix = prefixes.get(i);
				String ignoreUrlPrefix = prefix.getTextTrim();
				if (ignoreUrlPrefix.isEmpty()) {
					throw new TemplateParseException("/urls/ignore/prefix[" + i + "]为空");
				}
				ignoreUrlPrefixes[i] = ignoreUrlPrefix;
			}
			template.setIgnoreUrlPrefixes(ignoreUrlPrefixes);
		}
		
		// ignoreUrlPattern
		List<Element> ignoreRegEles = doc.selectNodes("/urls/ignore/regex");
		int ignoreRegsSize = ignoreRegEles.size();
		if (ignoreRegsSize > 0) {
//			List<Pattern> pas = new ArrayList<Pattern>(ignoreRegsSize);
			Pattern[] pas = new Pattern[ignoreRegsSize];
			for (int i = 0; i < ignoreRegsSize; i++) {
				Element regEle = ignoreRegEles.get(i);
				String reg = regEle.getTextTrim();
				if (reg.isEmpty()) {
					throw new TemplateParseException("/urls/ignore/regex[" + i + "]为空");
				}
				pas[i] = Pattern.compile(reg);
			}
			template.setIgnoreUrlPatterns(pas);
		}

		// region
		String commonProvince = doc.valueOf("/urls/region/@province").trim();
		String commonCity = doc.valueOf("/urls/region/@city").trim();
		List<Element> regionPrefixEles = doc.selectNodes("/urls/region/prefix");
		int regionPrefixesSize = regionPrefixEles.size();
		if (regionPrefixesSize > 0) {
			Map.Entry<String, Region>[] regionUrlPrefixes = new Map.Entry[regionPrefixesSize];
			for (int i = 0; i < regionPrefixesSize; i++) {
				Element prefixEle = regionPrefixEles.get(i);
				String regionUrlPrefix = prefixEle.getTextTrim();
				if (regionUrlPrefix.isEmpty()) {
					throw new TemplateParseException("/urls/region/prefix[" + i + "]为空");
				}
				String province = prefixEle.attributeValue("province");
				if (null == province || province.isEmpty()) {
					if (!commonProvince.isEmpty()) {
						province = commonProvince;
					} else {
						throw new TemplateParseException("/urls/region/prefix[" + i + "]@province为空");
					}
				}
				String city = prefixEle.attributeValue("city");
				if (null == city || city.isEmpty()) {
					if (!commonCity.isEmpty()) {
						city = commonCity;
					}
				}
				String county = null;
				if (null != city && !city.isEmpty()) {
					county = prefixEle.attributeValue("county");
				}
				Region region = new Region();
				region.setProvince(province);
				region.setCity(city);
				region.setCounty(county);
				
				regionUrlPrefixes[i] = new SimpleEntry<>(regionUrlPrefix, region);
			}
			template.setRegionUrlPrefixes(regionUrlPrefixes);
		}

		return template;
	}

	@Override
	boolean dependsOnTemplate(int type) {
		return TaskUtils.dependsOnUrlTemplate(type);
	}
}
