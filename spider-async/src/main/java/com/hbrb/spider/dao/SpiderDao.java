package com.hbrb.spider.dao;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.hbrb.exception.DataAccessException;
import com.hbrb.spider.model.SpiderConfig;

public interface SpiderDao extends BasicDao {
	boolean updateSpiderStatus(String spiderId, int status) throws SQLException;

	void loadSpiderConfig(SpiderConfig site, PreparedStatement stmt, int spiderId) throws SQLException, DataAccessException;
}
