package com.hbrb.spider.dao;

import java.sql.SQLException;
import java.util.List;

import com.hbrb.spider.model.WeiboUser;

public interface WeiboUserDao extends BasicDao {
	List<WeiboUser> retrieveAll() throws SQLException;
}
