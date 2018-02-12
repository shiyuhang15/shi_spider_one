package com.hbrb.util;

import java.util.ArrayList;
import java.util.List;

import com.hbrb.exception.LogicError;
import com.hbrb.spider.model.Region;
import com.hbrb.spider.model.article.SourceType;

public class ModelUtils {
	public static String sourceType2Name(int sourceType){
		switch (sourceType) {
		case SourceType.OTHER:
			return SourceType.Name.OTHER;
		case SourceType.WEB:
			return SourceType.Name.WEB;
		case SourceType.BBS:
			return SourceType.Name.BBS;
		case SourceType.BLOG:
			return SourceType.Name.BLOG;
		case SourceType.WEIXIN:
			return SourceType.Name.WEIXIN;
		case SourceType.APP:
			return SourceType.Name.APP;
		case SourceType.WEIBO:
			return SourceType.Name.WEIBO;
		case SourceType.PAPER:
			return SourceType.Name.PAPER;
		default:
			throw new Error("无效的信源类型 - " + sourceType);
		}
	}
	
	public static int sourceName2Type(String name){
		switch (name) {
		case SourceType.Name.OTHER:
			return SourceType.OTHER;
		case SourceType.Name.WEB:
			return SourceType.WEB;
		case SourceType.Name.WEIBO:
			return SourceType.WEIBO;
		case SourceType.Name.WEIXIN:
			return SourceType.WEIXIN;
		case SourceType.Name.APP:
			return SourceType.APP;
		case SourceType.Name.PAPER:
			return SourceType.PAPER;
		case SourceType.Name.BBS:
			return SourceType.BBS;
		case SourceType.Name.BLOG:
			return SourceType.BLOG;
		default:
			throw new LogicError("无效的信源类型名 - " + name);
		}
	}
	
	public static List<String> buildSourceRegion(Region region){
		List<String> sourceRegion = new ArrayList<>(3);
		sourceRegion.add(region.getProvince());
		String city = region.getCity();
		if (null != city) {
			sourceRegion.add(city);
		}
		String county = region.getCounty();
		if (null != county) {
			sourceRegion.add(county);
		}
		return sourceRegion;
	}
}
