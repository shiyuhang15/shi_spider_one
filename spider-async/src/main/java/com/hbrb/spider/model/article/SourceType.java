package com.hbrb.spider.model.article;

public interface SourceType {
	int OTHER = 0;
	int WEB = 1;
	int PAPER = 2;
	int BLOG = 3;
	int BBS = 4;
	int WEIBO = 5;
	int WEIXIN = 6;
	int APP = 7;
	interface Name {
		String OTHER = "other";
		String WEB = "web";
		String WEIBO = "weibo";
		String WEIXIN = "weixin";
		String APP = "app";
		String PAPER = "paper";
		String BBS = "bbs";
		String BLOG = "blog";
	}
}
