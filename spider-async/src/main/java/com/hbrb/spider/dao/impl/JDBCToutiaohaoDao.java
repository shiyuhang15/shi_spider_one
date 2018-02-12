package com.hbrb.spider.dao.impl;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import com.hbrb.jdbc.MyDataSource;
import com.hbrb.spider.dao.ToutiaohaoDao;
import com.hbrb.exception.DataAccessException;
import com.hbrb.spider.model.Toutiaohao;

public class JDBCToutiaohaoDao extends JDBCBasicDao implements ToutiaohaoDao {

	@Override
	public void addToutiaohaos(List<Toutiaohao> tths) throws SQLException,
			DataAccessException {
		if (null == tths || tths.size() == 0) {
			throw new DataAccessException("null == tths || tths.size() == 0");
		}
		try (PreparedStatement stmt = MyDataSource.getCurrentConnection()
				.prepareStatement("insert into t_task_spider_web_toutiao values (?,?,?)")) {
			for (Toutiaohao tth : tths) {
				Long id = tth.getId();
				if (null == id) {
					throw new DataAccessException("头条号id不能为空");
				}
				String name = tth.getName();
				if (null == name || name.length() == 0) {
					throw new DataAccessException("头条号名称不能为空");
				}
				Integer liveness = tth.getLiveness();
				if (null == liveness) {
					throw new DataAccessException("头条号活跃度不能为空");
				}
				stmt.setLong(1, id);
				stmt.setString(2, name);
				stmt.setInt(3, liveness);
				stmt.addBatch();
			}
			stmt.executeBatch();
		}
	}
}
