package com.hbrb.spider.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface BasicDao {
	int executeUpdate(String sql) throws SQLException;
	ResultSet executeQuery(String sql) throws SQLException;
}
