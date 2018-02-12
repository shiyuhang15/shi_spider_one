package com.hbrb.spider.dao.impl;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import com.hbrb.jdbc.MyDataSource;
import com.hbrb.spider.dao.SohuMtUserDao;
import com.hbrb.exception.DataAccessException;
import com.hbrb.spider.model.SohuMtUser;

public class JDBCSohuMtUserDao extends JDBCBasicDao implements SohuMtUserDao {

	@Override
	public void addUsers(List<SohuMtUser> users) throws SQLException,
			DataAccessException {
		if (null == users || users.isEmpty()) {
			throw new DataAccessException("null == users || users.isEmpty()");
		}
		try (PreparedStatement stmt = MyDataSource.getCurrentConnection()
				.prepareStatement("insert into t_task_spider_web_sohu_mt values (?,?,?)")) {
			for (SohuMtUser user : users) {
				String id = user.getId();
				if (null == id || id.isEmpty()) {
					throw new DataAccessException("搜狐公众号id不能为空");
				}
				String name = user.getName();
				if (null == name || name.isEmpty()) {
					throw new DataAccessException("搜狐公众号名称不能为空");
				}
				Integer liveness = user.getLiveness();
				if (null == liveness) {
					throw new DataAccessException("搜狐公众号活跃度不能为空");
				}
				stmt.setString(1, id);
				stmt.setString(2, name);
				stmt.setInt(3, liveness);
				stmt.addBatch();
			}
			stmt.executeBatch();
		}
	}
}
