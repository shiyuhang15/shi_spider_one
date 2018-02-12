package com.hbrb.spider.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.hbrb.exception.DataAccessException;
import com.hbrb.spider.dao.TemplateDao;

public class JDBCTemplateDao extends JDBCBasicDao implements TemplateDao {

	@Override
	public Map<Integer, String> listTemplates(String sql) throws SQLException, DataAccessException {
		try (ResultSet rs = executeQuery(sql)) {
			Map<Integer, String> map = new HashMap<>();
			while (rs.next()) {
				int id = rs.getInt(1);
				String content = rs.getString(2);
				if (content.isEmpty()) {
					throw new DataAccessException("模板[" + id + "]内容为空");
				}
				map.put(id, content);
			}
			return map;
		}
	}

}
