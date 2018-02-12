package com.hbrb.spider.model.article;

import java.util.ArrayList;
import java.util.List;

public class NewsArticle extends Article {
	/**
	 * 所属模块
	 */
	private List<Integer> modules = new ArrayList<>();
	/**
	 * 级别
	 */
	private Integer level;
	/**
	 * 标题
	 */
	private String title;
	/**
	 * 引题
	 */
	private String pretitle;
	/**
	 * 副题
	 */
	private String subtitle;
	/**
	 * 发布源
	 */
	private String pubSource;
	/**
	 * 关键词
	 */
	private String keywords;
	/**
	 * 摘要
	 */
	private String description;
	/**
	 * 频道
	 */
	private String channel;
	/**
	 * 标签
	 */
	private String tags;
	/**
	 * 图片
	 */
	private List<ArticleImg> imgs;

	public NewsArticle(Integer sourceType) {
		super(sourceType);
	}

	public String getSubtitle() {
		return subtitle;
	}

	public void setSubtitle(String subtitle) {
		this.subtitle = subtitle;
	}

	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}

	public String getPretitle() {
		return pretitle;
	}

	public void setPretitle(String pretitle) {
		this.pretitle = pretitle;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getPubSource() {
		return pubSource;
	}

	public void setPubSource(String pubSource) {
		this.pubSource = pubSource;
	}

	public String getKeywords() {
		return keywords;
	}

	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public List<ArticleImg> getImgs() {
		return imgs;
	}

	public void setImgs(List<ArticleImg> imgs) {
		this.imgs = imgs;
	}

	public List<Integer> getModules() {
		return modules;
	}
	public boolean addModule(int id){
		return this.modules.add(id);
	}

	public Integer getLevel() {
		return level;
	}

	public void setLevel(Integer level) {
		this.level = level;
	}
}