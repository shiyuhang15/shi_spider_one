package com.hbrb.spider.model.article;

import java.util.ArrayList;
import java.util.List;

public class WeiboArticle extends Article {
	/**
	 * 被转发的原微博
	 */
	private WeiboArticle origin;
	/**
	 * 图
	 */
	private List<String> imgSrcs;
	/**
	 * 转发数
	 */
	private int repostsCount;
	/**
	 * 评论数
	 */
	private int commentsCount;
	/**
	 * 赞数
	 */
	private int attitudesCount;
	/**
	 * 所属模块
	 */
	private List<Integer> modules = new ArrayList<>();

	public WeiboArticle(Integer sourceType) {
		super(sourceType);
	}

	public WeiboArticle getOrigin() {
		return origin;
	}

	public void setOrigin(WeiboArticle origin) {
		this.origin = origin;
	}

	public List<String> getImgSrcs() {
		return imgSrcs;
	}

	public void setImgSrcs(List<String> imgSrcs) {
		this.imgSrcs = imgSrcs;
	}

	public int getRepostsCount() {
		return repostsCount;
	}

	public void setRepostsCount(int repostsCount) {
		this.repostsCount = repostsCount;
	}

	public int getCommentsCount() {
		return commentsCount;
	}

	public void setCommentsCount(int commentsCount) {
		this.commentsCount = commentsCount;
	}

	public int getAttitudesCount() {
		return attitudesCount;
	}

	public void setAttitudesCount(int attitudesCount) {
		this.attitudesCount = attitudesCount;
	}

	public List<Integer> getModules() {
		return modules;
	}

	public boolean addModule(int id) {
		return this.modules.add(id);
	}
}
