package com.hbrb.spider.service.impl;

import java.sql.SQLException;
import java.util.List;

import com.hbrb.exception.ServiceException;
import com.hbrb.spider.dao.DaoFactory;
import com.hbrb.spider.dao.WeiboUserDao;
import com.hbrb.spider.model.WeiboUser;
import com.hbrb.spider.service.WeiboUser2Service;

public class JDBCWeiboUser2Service implements
		WeiboUser2Service {
	private final WeiboUserDao dao = DaoFactory
			.getWeiboUserDao();

	@Override
	public List<WeiboUser> retrieveAll() throws ServiceException {
		try {
			return dao.retrieveAll();
		} catch (SQLException e) {
			throw new ServiceException("获取所有微博用户异常", e);
		}
	}
}
