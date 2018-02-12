package com.hbrb.spider.model.template;

import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.hbrb.spider.model.Region;

public class UrlsTemplate {
	/**
	 * headline on front page
	 */
	private HOFPExtractRule[] hofps;
	private String[] startUrls;
	private String[] startUrlPatterns;
	private boolean autoExtractTargetAsSupplement = true;
	private TargetUrl[] targetUrls;
	private boolean autoExtractNaviAsSupplement = true;
	private Pattern[] naviUrlPatterns;
	private Pattern[] fuzzyNaviUrlPatterns;
	private String[] ignoreUrlPrefixes;
	private boolean disableCommonIgnore;
	private Pattern[] ignoreUrlPatterns;
	private Map.Entry<String, Region>[] regionUrlPrefixes;
	private int bottom = -1;
	private Entry<Pattern, Integer>[] deepEntries;

	public String[] getStartUrls() {
		return startUrls;
	}

	public void setStartUrls(String[] startUrls) {
		this.startUrls = startUrls;
	}

	public String[] getStartUrlPatterns() {
		return startUrlPatterns;
	}

	public void setStartUrlPatterns(String[] startUrlPatterns) {
		this.startUrlPatterns = startUrlPatterns;
	}

	public boolean isAutoExtractTargetAsSupplement() {
		return autoExtractTargetAsSupplement;
	}

	public void setAutoExtractTargetAsSupplement(boolean autoExtractTargetAsSupplement) {
		this.autoExtractTargetAsSupplement = autoExtractTargetAsSupplement;
	}

	public TargetUrl[] getTargetUrls() {
		return targetUrls;
	}

	public void setTargetUrls(TargetUrl[] targetUrls) {
		this.targetUrls = targetUrls;
	}

	public boolean isAutoExtractNaviAsSupplement() {
		return autoExtractNaviAsSupplement;
	}

	public void setAutoExtractNaviAsSupplement(boolean autoExtractNaviAsSupplement) {
		this.autoExtractNaviAsSupplement = autoExtractNaviAsSupplement;
	}

	public Pattern[] getNaviUrlPatterns() {
		return naviUrlPatterns;
	}

	public void setNaviUrlPatterns(Pattern[] naviUrlPatterns) {
		this.naviUrlPatterns = naviUrlPatterns;
	}

	public Pattern[] getFuzzyNaviUrlPatterns() {
		return fuzzyNaviUrlPatterns;
	}

	public void setFuzzyNaviUrlPatterns(Pattern[] fuzzyNaviUrlPatterns) {
		this.fuzzyNaviUrlPatterns = fuzzyNaviUrlPatterns;
	}

	public String[] getIgnoreUrlPrefixes() {
		return ignoreUrlPrefixes;
	}

	public void setIgnoreUrlPrefixes(String[] ignoreUrlPrefixes) {
		this.ignoreUrlPrefixes = ignoreUrlPrefixes;
	}

	public boolean isDisableCommonIgnore() {
		return disableCommonIgnore;
	}

	public void setDisableCommonIgnore(boolean disableCommonIgnore) {
		this.disableCommonIgnore = disableCommonIgnore;
	}

	public Pattern[] getIgnoreUrlPatterns() {
		return ignoreUrlPatterns;
	}

	public void setIgnoreUrlPatterns(Pattern[] ignoreUrlPatterns) {
		this.ignoreUrlPatterns = ignoreUrlPatterns;
	}

	public int getBottom() {
		return bottom;
	}

	public void setBottom(int bottom) {
		this.bottom = bottom;
	}

	public Entry<Pattern, Integer>[] getDeepEntries() {
		return deepEntries;
	}

	public void setDeepEntries(Entry<Pattern, Integer>[] deepEntries) {
		this.deepEntries = deepEntries;
	}

	public HOFPExtractRule[] getHofps() {
		return hofps;
	}

	public void setHofps(HOFPExtractRule[] hofps) {
		this.hofps = hofps;
	}

	public Map.Entry<String, Region>[] getRegionUrlPrefixes() {
		return regionUrlPrefixes;
	}

	public void setRegionUrlPrefixes(Map.Entry<String, Region>[] regionUrlPrefixes) {
		this.regionUrlPrefixes = regionUrlPrefixes;
	}
}
