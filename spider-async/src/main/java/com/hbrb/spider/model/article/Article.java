package com.hbrb.spider.model.article;

import java.util.Date;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.hbrb.spider.ConstantsHome;

public class Article {
	/**
	 * 信源地域
	 */
	private List<String> sourceRegion;
	/**
	 * 地域
	 */
	private List<String> region;
	/**
	 * 信源类型
	 */
	private Integer sourceType;
	/**
	 * 信源名称
	 */
	private String sourceName;
	/**
	 * URL
	 */
	private String url;
	/**
	 * 正文
	 */
	private String content;
	/**
	 * 作者
	 */
	private String authors;
	/**
	 * 采集时间
	 */
	private Date createTime;
	/**
	 * 发布时间
	 */
	private Date pubTime;

	public Article(Integer sourceType) {
		setSourceType(sourceType);
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Integer getSourceType() {
		return sourceType;
	}

	public void setSourceType(Integer sourceType) {
		this.sourceType = sourceType;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getSourceName() {
		return sourceName;
	}

	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}

	public Date getPubTime() {
		return pubTime;
	}

	public void setPubTime(Date pubTime) {
		this.pubTime = pubTime;
	}

	public String getAuthors() {
		return authors;
	}

	public void setAuthors(String authors) {
		this.authors = authors;
	}

	public List<String> getSourceRegion() {
		return sourceRegion;
	}

	public void setSourceRegion(List<String> sourceRegion) {
		this.sourceRegion = sourceRegion;
	}
	
	@Override
	public String toString() {
		return JSON.toJSONStringWithDateFormat(this, ConstantsHome.PATTERN.DATEFORMAT_DEFAULT,
				SerializerFeature.PrettyFormat);
	}

	public List<String> getRegion() {
		return region;
	}

	public void setRegion(List<String> region) {
		this.region = region;
	}
}
