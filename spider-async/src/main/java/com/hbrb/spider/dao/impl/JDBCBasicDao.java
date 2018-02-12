package com.hbrb.spider.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.hbrb.jdbc.MyDataSource;
import com.hbrb.spider.dao.BasicDao;

public class JDBCBasicDao implements BasicDao {
	@Override
	public int executeUpdate(String sql) throws SQLException {
		return MyDataSource.getCurrentStatement().executeUpdate(sql);
	}
	
	@Override
	public ResultSet executeQuery(String sql) throws SQLException {
		return MyDataSource.getCurrentStatement().executeQuery(sql);
	}
}
