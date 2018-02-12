package com.hbrb.spider.service;

import java.lang.reflect.Proxy;

import com.hbrb.spider.model.template.PageTemplate;
import com.hbrb.spider.model.template.UrlsTemplate;
import com.hbrb.spider.service.aspect.JDBCServiceInvocationHandler;
import com.hbrb.spider.service.impl.CachedAtomicRegionService;
import com.hbrb.spider.service.impl.CachedJDBCFillerService;
import com.hbrb.spider.service.impl.CachedJDBCPageTemplateService;
import com.hbrb.spider.service.impl.CachedJDBCUrlTemplateService;
import com.hbrb.spider.service.impl.CachedRegionService;
import com.hbrb.spider.service.impl.JDBCFollowedWeiboSpiderTaskService;
import com.hbrb.spider.service.impl.JDBCNaviTaskService;
import com.hbrb.spider.service.impl.JDBCSiteTaskService;
import com.hbrb.spider.service.impl.JDBCSohuMtUserService;
import com.hbrb.spider.service.impl.JDBCSpiderService;
import com.hbrb.spider.service.impl.JDBCToutiaohaoService;
import com.hbrb.spider.service.impl.JDBCWeiboUser2Service;
import com.hbrb.spider.service.impl.JDBCWeiboVisitorCookieService;
import com.hbrb.spider.service.impl.RedisJinRiTouTiaoSpiderTaskService;
import com.hbrb.spider.service.impl.RedisSohuMtSpiderTaskService;
import com.hbrb.spider.service.impl.RedisTargetTaskService;

public class ServiceFactory {
	private static TemplateService<UrlsTemplate> urlTemplateService;
	private static TemplateService<PageTemplate> pageTemplateService;
	private static SpiderService spiderService;
	private static TargetTaskService targetTaskService;
	private static SiteTaskService siteTaskService;
	private static NaviTaskService naviTaskService;
	private static JinRiTouTiaoSpiderTaskService jinRiTouTiaoSpiderTaskService;
	private static SohuMtSpiderTaskService sohuMtSpiderTaskService;
	private static WeiboUser2Service weiboUser2Service;
	private static ToutiaohaoService toutiaohaoService;
	private static SohuMtUserService sohuMtUserService;
	private static FollowedWeiboSpiderTaskService followedWeiboSpiderTaskService;
	private static WeiboVisitorCookieService weiboVisitorCookieService;
	private static FillerService fillerService;
	private static CachedRegionService regionService;
	private static CachedAtomicRegionService atomicRegionService;

	public static SpiderService getSpiderService() {
		if (null == spiderService) {
			spiderService = (SpiderService) Proxy.newProxyInstance(
					SpiderService.class.getClassLoader(),
					new Class[] { SpiderService.class },
					new JDBCServiceInvocationHandler(new JDBCSpiderService()));
		}
		return spiderService;
	}
	
	public static TemplateService<UrlsTemplate> getUrlTemplateService() {
		if (null == urlTemplateService) {
			urlTemplateService = new CachedJDBCUrlTemplateService();
		}
		return urlTemplateService;
	}
	
	public static TemplateService<PageTemplate> getPageTemplateService() {
		if (null == pageTemplateService) {
			pageTemplateService = new CachedJDBCPageTemplateService();
		}
		return pageTemplateService;
	}

	/*public static SiteTemplateService getSiteTemplateService() {
		if (null == templateService) {
			templateService = new CachedJDBCSiteTemplateService();
		}
		return templateService;
	}*/

	public static SiteTaskService getSiteTaskService() {
		if (null == siteTaskService) {
			siteTaskService = (SiteTaskService) Proxy
					.newProxyInstance(SiteTaskService.class.getClassLoader(),
							new Class[] { SiteTaskService.class },
							new JDBCServiceInvocationHandler(
									new JDBCSiteTaskService()));
		}
		return siteTaskService;
	}

	public static NaviTaskService getNaviTaskService() {
		if (null == naviTaskService) {
			naviTaskService = (NaviTaskService) Proxy
					.newProxyInstance(NaviTaskService.class.getClassLoader(),
							new Class[] { NaviTaskService.class },
							new JDBCServiceInvocationHandler(
									new JDBCNaviTaskService()));
		}
		return naviTaskService;
	}

	public static TargetTaskService getTargetTaskService() {
		if (null == targetTaskService) {
			targetTaskService = new RedisTargetTaskService();
		}
		return targetTaskService;
	}
	
	public static JinRiTouTiaoSpiderTaskService getJinRiTouTiaoSpiderTaskService() {
		if (null == jinRiTouTiaoSpiderTaskService) {
			jinRiTouTiaoSpiderTaskService = new RedisJinRiTouTiaoSpiderTaskService();
		}
		return jinRiTouTiaoSpiderTaskService;
	}
	
	public static SohuMtSpiderTaskService getSohuMtSpiderTaskService() {
		if (null == sohuMtSpiderTaskService) {
			sohuMtSpiderTaskService = new RedisSohuMtSpiderTaskService();
		}
		return sohuMtSpiderTaskService;
	}

	public static FollowedWeiboSpiderTaskService getFollowedWeiboSpiderTaskService() {
		if (null == followedWeiboSpiderTaskService) {
			followedWeiboSpiderTaskService = (FollowedWeiboSpiderTaskService) Proxy.newProxyInstance(
					FollowedWeiboSpiderTaskService.class.getClassLoader(),
					new Class[] { FollowedWeiboSpiderTaskService.class },
					new JDBCServiceInvocationHandler(new JDBCFollowedWeiboSpiderTaskService()));
		}
		return followedWeiboSpiderTaskService;
	}

	public static WeiboVisitorCookieService getWeiboVisitorCookieService() {
		if (null == weiboVisitorCookieService) {
			weiboVisitorCookieService = (WeiboVisitorCookieService) Proxy.newProxyInstance(
					WeiboVisitorCookieService.class.getClassLoader(),
					new Class[] { WeiboVisitorCookieService.class },
					new JDBCServiceInvocationHandler(new JDBCWeiboVisitorCookieService()));
		}
		return weiboVisitorCookieService;
	}

	public static FillerService getFillerService() {
		if (null == fillerService) {
			fillerService = new CachedJDBCFillerService();
		}
		return fillerService;
	}

	public static CachedAtomicRegionService getAtomicRegionService() {
		if (null == atomicRegionService) {
			atomicRegionService = new CachedAtomicRegionService();
		}
		return atomicRegionService;
	}
	
	public static CachedRegionService getRegionService() {
		if (null == regionService) {
			regionService = new CachedRegionService();
		}
		return regionService;
	}

	public static WeiboUser2Service getWeiboUser2Service() {
		if (null == weiboUser2Service) {
			weiboUser2Service = (WeiboUser2Service) Proxy.newProxyInstance(
					WeiboUser2Service.class.getClassLoader(),
					new Class[] { WeiboUser2Service.class },
					new JDBCServiceInvocationHandler(new JDBCWeiboUser2Service()));
		}
		return weiboUser2Service;
	}
	
	public static ToutiaohaoService getToutiaohaoService() {
		if (null == toutiaohaoService) {
			toutiaohaoService = (ToutiaohaoService) Proxy.newProxyInstance(
					ToutiaohaoService.class.getClassLoader(),
					new Class[] { ToutiaohaoService.class },
					new JDBCServiceInvocationHandler(new JDBCToutiaohaoService()));
		}
		return toutiaohaoService;
	}
	
	public static SohuMtUserService getSohuMtUserService() {
		if (null == sohuMtUserService) {
			sohuMtUserService = (SohuMtUserService) Proxy.newProxyInstance(
					SohuMtUserService.class.getClassLoader(),
					new Class[] { SohuMtUserService.class },
					new JDBCServiceInvocationHandler(new JDBCSohuMtUserService()));
		}
		return sohuMtUserService;
	}
}
