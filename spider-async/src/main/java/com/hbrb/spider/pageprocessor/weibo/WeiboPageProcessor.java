package com.hbrb.spider.pageprocessor.weibo;

import java.util.Date;

import com.hbrb.spider.model.Region;
import com.hbrb.spider.model.WeiboUser;
import com.hbrb.spider.model.article.SourceType;
import com.hbrb.spider.model.article.WeiboArticle;
import com.hbrb.spider.model.page.Page;
import com.hbrb.spider.model.task.RequestTask;
import com.hbrb.spider.pageprocessor.PageProcessor;
import com.hbrb.util.ModelUtils;
import com.hbrb.util.WeiBoUtils;

public abstract class WeiboPageProcessor<T extends RequestTask, P extends Page<T>> extends PageProcessor<T, P> {
	public static final String REDIS_KEY_WEIBO_MID = "set_weibo_mid";

	WeiboArticle createWeiboArticle(WeiboUser user, String mid, Date now) {
		WeiboArticle article = new WeiboArticle(SourceType.WEIBO);
		article.setUrl(WeiBoUtils.getReference(user.getId(), mid));
		article.setAuthors(user.getNickName());
		Region region = user.getRegion();
		if (null != region) {
			article.setSourceRegion(ModelUtils.buildSourceRegion(region));
			article.addModule(1);
		}
		article.setSourceName("新浪微博");
		article.setCreateTime(now);
		return article;
	}
}
