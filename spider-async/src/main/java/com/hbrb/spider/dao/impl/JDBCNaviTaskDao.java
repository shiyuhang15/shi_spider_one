package com.hbrb.spider.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.hbrb.exception.DataAccessException;
import com.hbrb.jdbc.MyDataSource;
import com.hbrb.spider.dao.NaviTaskDao;
import com.hbrb.spider.model.task.NaviTask;

public class JDBCNaviTaskDao extends JDBCBasicDao implements NaviTaskDao {
	@Override
	public void create(NaviTask naviTask) throws SQLException, DataAccessException {
		String url = naviTask.getUrl();
		if (null == url || url.isEmpty()) {
			throw new DataAccessException("URL不能为空");
		}
		int sourceType = naviTask.getSourceType();
		if (sourceType <= 0 || sourceType > 7) {
			throw new DataAccessException("信源类型无效 - " + sourceType);
		}
		int siteTaskId = naviTask.getSiteTaskId();
		if (siteTaskId == 0) {
			throw new DataAccessException("站点任务ID不能为0");
		}
		int type = naviTask.getType();
		if (type < 0 || type > 3) {
			throw new DataAccessException("类型无效 - " + type);
		}
		

		try (PreparedStatement stmt = MyDataSource.getCurrentConnection().prepareStatement(
				"insert into t_task_navi(c_name,c_url,c_time_start,c_time_worked,c_type_source,c_id_task_site,c_type) values (?,?,?,?,?,?,?)")) {
			String taskName = naviTask.getName();
			if (null == taskName) {
				taskName = "";
			}
			stmt.setString(1, taskName);
			stmt.setString(2, url);
			long now = System.currentTimeMillis();
			stmt.setLong(3, now);
			stmt.setLong(4, now);
			stmt.setInt(5, sourceType);
			stmt.setInt(6, siteTaskId);
			stmt.setInt(7, type);
			int res = stmt.executeUpdate();
			if (1 != res) {
				throw new DataAccessException("导航任务[" + url + "]插入失败");
			}
		}
	}

	@Override
	public int countByUrl(String url) throws SQLException, DataAccessException {
		if (null == url || url.isEmpty()) {
			throw new DataAccessException("URL不能为空");
		}
		Connection conn = MyDataSource.getCurrentConnection();
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.prepareStatement("select count(*) from t_task_navi where c_url=?");
			stmt.setString(1, url);
			rs = stmt.executeQuery();
			if (rs.next()) {
				int count = rs.getInt(1);
				if (!rs.wasNull()) {
					return count;
				} else {
					throw new DataAccessException("count is null");
				}
			} else {
				throw new DataAccessException("rs.next() is false");
			}
		} finally {
			MyDataSource.release(rs, stmt);
		}
	}

	@Override
	public void delete(long id) throws SQLException, DataAccessException {
		int res = executeUpdate("delete from t_task_navi where c_id=" + id);
		if (1 != res) {
			throw new DataAccessException("导航任务[" + id + "]不存在");
		}
	}

	@Override
	public int updateForCrawl(int spiderId, int limit) throws SQLException, DataAccessException {
		if (spiderId == 0) {
			throw new DataAccessException("spiderId == 0");
		}
		if (limit <= 0) {
			throw new DataAccessException("limit <= 0");
		}
		long now = System.currentTimeMillis();
		return executeUpdate("update t_task_navi set c_id_spider=" + spiderId + ",c_time_start=" + now
				+ " where c_id_spider = 0 and " + now
				+ "-c_time_start>c_interval*1000 order by c_interval, c_time_start limit " + limit);
	}

	@Override
	public List<NaviTask> listBySpiderId(int spiderId) throws SQLException, DataAccessException {
		List<NaviTask> res = new ArrayList<NaviTask>();
		try (ResultSet rs = executeQuery(
				"select c_url,c_id,c_time_worked,c_type_source,c_id_task_site,c_type from t_task_navi where c_id_spider="
						+ spiderId)) {
			while (rs.next()) {
				NaviTask task = new NaviTask(rs.getString(1));
				task.setId(rs.getLong(2));
				task.setWorkedTime(rs.getLong(3));
				task.setSourceType(rs.getInt(4));
				task.setSiteTaskId(rs.getInt(5));
				task.setType(rs.getInt(6));
				res.add(task);
			}
		}
		return res;
	}

	@Override
	public void releaseTasks(int spiderId) throws SQLException, DataAccessException {
		executeUpdate("update t_task_navi set c_id_spider=0 where c_id_spider=" + spiderId);
	}

	@Override
	public void updateWorkedTimeById(long id, long workedTime) throws SQLException, DataAccessException {
		executeUpdate("update t_task_navi set c_time_worked=" + workedTime + " where c_id=" + id);
	}
}
