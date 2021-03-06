package com.hbrb.spider.dao;

import java.sql.SQLException;
import java.util.List;

import com.hbrb.exception.DataAccessException;
import com.hbrb.spider.model.task.FollowedWeiboSpiderTask;

public interface FollowedWeiboSpiderTaskDao extends BasicDao {
	void releaseTasks(String spiderId) throws SQLException, DataAccessException;
	int updateForCrawl(String spiderId, int limit) throws SQLException, DataAccessException;
	List<FollowedWeiboSpiderTask> listBySpiderId(String spiderId) throws SQLException, DataAccessException;
	List<FollowedWeiboSpiderTask> specialListBySpiderId(String spiderId) throws SQLException, DataAccessException;
	void updateCookieById(long id, String cookie) throws SQLException;
}
