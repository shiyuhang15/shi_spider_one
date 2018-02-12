package com.hbrb.spider.model.article;

import java.util.List;

public class WeixinArticle extends Article {
	private List<String> imgSrcs;
	private int forwardCount;
	private int commentCount;
	private int likeCount;
	public WeixinArticle(Integer sourceType) {
		super(sourceType);
	}
	public List<String> getImgSrcs() {
		return imgSrcs;
	}
	public void setImgSrcs(List<String> imgSrcs) {
		this.imgSrcs = imgSrcs;
	}
	public int getForwardCount() {
		return forwardCount;
	}
	public void setForwardCount(int forwardCount) {
		this.forwardCount = forwardCount;
	}
	public int getCommentCount() {
		return commentCount;
	}
	public void setCommentCount(int commentCount) {
		this.commentCount = commentCount;
	}
	public int getLikeCount() {
		return likeCount;
	}
	public void setLikeCount(int likeCount) {
		this.likeCount = likeCount;
	}
	
}
