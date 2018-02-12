package com.hbrb.spider.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.hbrb.exception.DataAccessException;
import com.hbrb.spider.model.Region;
import com.hbrb.spider.model.task.SiteTask;

public interface SiteTaskDao extends BasicDao {
	int updateForCrawl(int spiderId, int limit) throws SQLException, DataAccessException;

	List<SiteTask> listBySpiderId(int spiderId) throws SQLException, DataAccessException;

	void releaseTasks(int spiderId) throws SQLException, DataAccessException;

	void cacheTaskInfo(Map<Integer, String> siteNameMap, Map<Integer, Region> siteRegionMap,
			Map<Integer, Integer> siteLevelMap, Map<Integer, Integer> sitePageTemplateIdMap, String sql) throws SQLException, DataAccessException;
}
