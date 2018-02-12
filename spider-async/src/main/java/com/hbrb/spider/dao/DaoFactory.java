package com.hbrb.spider.dao;

import java.lang.reflect.Proxy;

import com.hbrb.spider.dao.aspect.JDBCDaoInvocationHandler;
import com.hbrb.spider.dao.impl.JDBCFollowedWeiboSpiderTaskDao;
import com.hbrb.spider.dao.impl.JDBCNaviTaskDao;
import com.hbrb.spider.dao.impl.JDBCSiteTaskDao;
import com.hbrb.spider.dao.impl.JDBCSohuMtUserDao;
import com.hbrb.spider.dao.impl.JDBCSpiderDao;
import com.hbrb.spider.dao.impl.JDBCTemplateDao;
import com.hbrb.spider.dao.impl.JDBCToutiaohaoDao;
import com.hbrb.spider.dao.impl.JDBCWeiboUserDao;
import com.hbrb.spider.dao.impl.JDBCWeiboVisitorCookieDao;

public class DaoFactory {
	private static SpiderDao spiderDao;
	private static TemplateDao templateDao;
	private static SiteTaskDao siteTaskDao;
	private static NaviTaskDao naviTaskDao;
	private static WeiboVisitorCookieDao weiboVisitorCookieDao;
	private static FollowedWeiboSpiderTaskDao followedWeiboSpiderTaskDao;
	private static WeiboUserDao weiboUser2Dao;
	private static ToutiaohaoDao toutiaohaoDao;
	private static SohuMtUserDao sohuMtUserDao;

	public static SpiderDao getSpiderDao() {
		if (null == spiderDao) {
			spiderDao = (SpiderDao) Proxy.newProxyInstance(
					SpiderDao.class.getClassLoader(),
					new Class[] { SpiderDao.class },
					new JDBCDaoInvocationHandler(new JDBCSpiderDao()));
		}
		return spiderDao;
	}
	
	public static SiteTaskDao getSiteTaskDao() {
		if (null == siteTaskDao) {
			siteTaskDao = (SiteTaskDao) Proxy.newProxyInstance(
					SiteTaskDao.class.getClassLoader(),
					new Class[] { SiteTaskDao.class },
					new JDBCDaoInvocationHandler(new JDBCSiteTaskDao()));
		}
		return siteTaskDao;
	}
	
	public static TemplateDao getTemplateDao() {
		if (null == templateDao) {
			templateDao = (TemplateDao) Proxy.newProxyInstance(
					TemplateDao.class.getClassLoader(),
					new Class[] { TemplateDao.class },
					new JDBCDaoInvocationHandler(new JDBCTemplateDao()));
		}
		return templateDao;
	}

	public static NaviTaskDao getNaviTaskDao() {
		if (null == naviTaskDao) {
			naviTaskDao = (NaviTaskDao) Proxy.newProxyInstance(
					NaviTaskDao.class.getClassLoader(),
					new Class[] { NaviTaskDao.class },
					new JDBCDaoInvocationHandler(new JDBCNaviTaskDao()));
		}
		return naviTaskDao;
	}

	public static WeiboVisitorCookieDao getWeiboVisitorCookieDao() {
		if (null == weiboVisitorCookieDao) {
			weiboVisitorCookieDao = (WeiboVisitorCookieDao) Proxy.newProxyInstance(
					WeiboVisitorCookieDao.class.getClassLoader(),
					new Class[] { WeiboVisitorCookieDao.class },
					new JDBCDaoInvocationHandler(new JDBCWeiboVisitorCookieDao()));
		}
		return weiboVisitorCookieDao;
	}

	public static FollowedWeiboSpiderTaskDao getFollowedWeiboSpiderTaskDao() {
		if (null == followedWeiboSpiderTaskDao) {
			followedWeiboSpiderTaskDao = (FollowedWeiboSpiderTaskDao) Proxy.newProxyInstance(
					FollowedWeiboSpiderTaskDao.class.getClassLoader(),
					new Class[] { FollowedWeiboSpiderTaskDao.class },
					new JDBCDaoInvocationHandler(new JDBCFollowedWeiboSpiderTaskDao()));
		}
		return followedWeiboSpiderTaskDao;
	}

	public static WeiboUserDao getWeiboUserDao() {
		if (null == weiboUser2Dao) {
			weiboUser2Dao = (WeiboUserDao) Proxy.newProxyInstance(
					WeiboUserDao.class.getClassLoader(),
					new Class[] { WeiboUserDao.class },
					new JDBCDaoInvocationHandler(new JDBCWeiboUserDao()));
		}
		return weiboUser2Dao;
	}
	
	public static ToutiaohaoDao getToutiaohaoDao() {
		if (null == toutiaohaoDao) {
			toutiaohaoDao = (ToutiaohaoDao) Proxy.newProxyInstance(
					ToutiaohaoDao.class.getClassLoader(),
					new Class[] { ToutiaohaoDao.class },
					new JDBCDaoInvocationHandler(new JDBCToutiaohaoDao()));
		}
		return toutiaohaoDao;
	}
	
	public static SohuMtUserDao getSohuMtUserDao() {
		if (null == sohuMtUserDao) {
			sohuMtUserDao = (SohuMtUserDao) Proxy.newProxyInstance(
					SohuMtUserDao.class.getClassLoader(),
					new Class[] { SohuMtUserDao.class },
					new JDBCDaoInvocationHandler(new JDBCSohuMtUserDao()));
		}
		return sohuMtUserDao;
	}
}
