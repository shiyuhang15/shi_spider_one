package com.hbrb.spider.dao;

import java.sql.SQLException;
import java.util.List;

import com.hbrb.exception.DataAccessException;
import com.hbrb.spider.model.task.NaviTask;

public interface NaviTaskDao extends BasicDao {
	void create(NaviTask naviTask) throws SQLException, DataAccessException;
	void delete(long id) throws SQLException, DataAccessException;
	int countByUrl(String url) throws SQLException, DataAccessException;
	int updateForCrawl(int spiderId, int limit) throws SQLException, DataAccessException;
	List<NaviTask> listBySpiderId(int spiderId) throws SQLException, DataAccessException;
	void releaseTasks(int spiderId) throws SQLException, DataAccessException;
	void updateWorkedTimeById(long id, long currentTimeMillis) throws SQLException, DataAccessException;
}
