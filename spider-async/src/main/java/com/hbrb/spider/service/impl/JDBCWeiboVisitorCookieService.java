package com.hbrb.spider.service.impl;

import java.sql.SQLException;
import java.util.List;

import com.hbrb.spider.dao.DaoFactory;
import com.hbrb.spider.dao.WeiboVisitorCookieDao;
import com.hbrb.exception.ServiceException;
import com.hbrb.spider.service.WeiboVisitorCookieService;

public class JDBCWeiboVisitorCookieService implements WeiboVisitorCookieService {
	private WeiboVisitorCookieDao weiboVisitorCookieDao = DaoFactory.getWeiboVisitorCookieDao();
	@Override
	public List<String> getAllCookies() throws ServiceException {
		try {
			return weiboVisitorCookieDao.getAllCookies();
		} catch (SQLException e) {
			throw new ServiceException("微博访客cookie获取失败", e);
		}
	}
}
