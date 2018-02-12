package com.hbrb.spider.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hbrb.exception.DataAccessException;
import com.hbrb.spider.dao.SiteTaskDao;
import com.hbrb.spider.model.Region;
import com.hbrb.spider.model.task.SiteTask;
import com.hbrb.spider.service.RegionService;
import com.hbrb.spider.service.ServiceFactory;
import com.hbrb.spider.service.impl.CachedAtomicRegionService;

public class JDBCSiteTaskDao extends JDBCBasicDao implements SiteTaskDao {
	public void cacheTaskInfo(Map<Integer, String> siteNameMap, Map<Integer, Region> siteRegionMap,
			Map<Integer, Integer> siteLevleMap, Map<Integer, Integer> sitePageTemplateIdMap, String sql)
			throws SQLException, DataAccessException {
		CachedAtomicRegionService atomicRegionService = ServiceFactory.getAtomicRegionService();
		try (ResultSet rs = executeQuery(sql)) {
			while (rs.next()) {
				int id = rs.getInt(1);
				String name = rs.getString(2);
				if (null == name || name.isEmpty()) {
					throw new DataAccessException("站点任务[" + id + "]名称为空");
				}
				siteNameMap.put(id, name);
				
				String province = rs.getString(3);
				Region region = null;
				if (null != province && !province.isEmpty()) {
					if (RegionService.HEBEI.equals(province)) {
						province = RegionService.HEBEI;
					}
					region = new Region();
					region.setProvince(province);
					String city = rs.getString(4);
					if (null != city && !city.isEmpty()) {
						region.setCity(city);
						String county = rs.getString(5);
						if (null != county && !county.isEmpty()) {
							region.setCounty(county);
						}
					}
				}
				if (null != region) {
					siteRegionMap.put(id, region);
					atomicRegionService.putRegion(region);
				}
				
				int level = rs.getInt(6);
				if (level > 0) {
					siteLevleMap.put(id, level);
				}
				
				int pageTemplateId = rs.getInt(7);
				if (pageTemplateId > 0 && pageTemplateId != id) {
					sitePageTemplateIdMap.put(id, pageTemplateId);
				}
			}
		}
	}

	@Override
	public int updateForCrawl(int spiderId, int limit) throws SQLException, DataAccessException {
		if (limit <= 0) {
			throw new DataAccessException("limit <= 0");
		}
		return executeUpdate("update t_task_site set c_id_spider=" + spiderId + ",c_time_start="
				+ System.currentTimeMillis() + " where c_id_spider = 0 order by c_time_start limit " + limit);
	}

	@Override
	public List<SiteTask> listBySpiderId(int spiderId) throws SQLException {
		try (ResultSet rs = executeQuery("select c_url_start,c_id,c_type_source,c_type from t_task_site where c_id_spider=" + spiderId)) {
			return loadTaskFromResultSet(rs);
		}
	}

	private List<SiteTask> loadTaskFromResultSet(ResultSet rs) throws SQLException {
		List<SiteTask> tasks = new ArrayList<>();
		while (rs.next()) {
			String url = rs.getString(1);
			SiteTask task = new SiteTask(url.isEmpty() ? null : url);
			task.setSiteTaskId(rs.getInt(2));
			// task.setTitle(rs.getString(3));
			task.setSourceType(rs.getInt(3));
			task.setType(rs.getInt(4));
			tasks.add(task);
		}
		return tasks;
	}

	@Override
	public void releaseTasks(int spiderId) throws SQLException, DataAccessException {
		executeUpdate("update t_task_site set c_id_spider=0 where c_id_spider=" + spiderId);
	}
}
