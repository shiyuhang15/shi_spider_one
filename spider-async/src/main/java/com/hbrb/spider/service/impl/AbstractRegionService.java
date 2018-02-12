package com.hbrb.spider.service.impl;

import java.util.List;

import com.hbrb.spider.model.article.Article;
import com.hbrb.spider.service.RegionService;

public abstract class AbstractRegionService implements RegionService {

	@Override
	public void countRegion(Article article) {
		List<String> sourceRegion = article.getSourceRegion();
		if (null != sourceRegion) {
			String province = sourceRegion.get(0);
			if (RegionService.HEBEI == province && sourceRegion.size() > 1) {
				String city = sourceRegion.get(1);
				if (null != city) {
					countCity(city);
				}
			}
			List<String> region = article.getRegion();
			if (null != region && region.get(0) == RegionService.HEBEI) {
				countProvince(province);
			}
		}
	}

	abstract void countCity(String city);
	abstract void countProvince(String city);
}
