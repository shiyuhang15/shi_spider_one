package com.hbrb.spider.service;

import com.hbrb.spider.model.article.Article;

public interface RegionService {
	String HEBEI = "河北省";
	String PATH_COUNT_REGION = "/count/region";
	void countRegion(Article article);
	String getRegionCount();
	void clearRegionCountMap();
//	void putRegion(Region region);
}
