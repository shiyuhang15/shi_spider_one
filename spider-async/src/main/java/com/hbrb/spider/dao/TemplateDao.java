package com.hbrb.spider.dao;

import java.sql.SQLException;
import java.util.Map;

import com.hbrb.exception.DataAccessException;

public interface TemplateDao {
	Map<Integer, String> listTemplates(String sql) throws SQLException, DataAccessException;
}
