package com.hbrb.spider.dao;

import java.sql.SQLException;
import java.util.List;

public interface WeiboVisitorCookieDao extends BasicDao {
	List<String> getAllCookies() throws SQLException;
}
