package com.hbrb.spider.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.hbrb.spider.dao.WeiboVisitorCookieDao;

public class JDBCWeiboVisitorCookieDao extends JDBCBasicDao implements
		WeiboVisitorCookieDao {
	@Override
	public List<String> getAllCookies() throws SQLException {
		try (ResultSet rs = executeQuery("select c_cookie from t_cookie_weibo_visitor")) {
			List<String> cookies = new ArrayList<String>();
			while (rs.next()) {
				cookies.add(rs.getString(1));
			}
			return cookies;
		}
	}
}
