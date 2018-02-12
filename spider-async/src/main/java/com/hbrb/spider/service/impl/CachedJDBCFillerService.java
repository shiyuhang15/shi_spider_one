package com.hbrb.spider.service.impl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;

import com.hbrb.exception.DataAccessException;
import com.hbrb.exception.ServiceException;
import com.hbrb.jdbc.MyDataSource;
import com.hbrb.spider.ConstantsHome;
import com.hbrb.spider.dao.DaoFactory;
import com.hbrb.spider.dao.SiteTaskDao;
import com.hbrb.spider.model.Region;
import com.hbrb.spider.model.task.TargetTask;
import com.hbrb.spider.service.FillerService;

public class CachedJDBCFillerService implements FillerService {
	private Map<Integer, String> siteNameMap = new ConcurrentHashMap<Integer, String>();
	private Map<Integer, Integer> siteLevelMap = new ConcurrentHashMap<Integer, Integer>();
	private Map<Integer, Region> siteRegionMap = new ConcurrentHashMap<Integer, Region>();
	private Map<Integer, Integer> sitePageTemplateIdMap = new HashMap<Integer, Integer>();
	private final SiteTaskDao siteTaskDao = DaoFactory.getSiteTaskDao();
	
	@Override
	public String retrieveSiteName(int siteTaskId) throws ServiceException {
		String siteName = siteNameMap.get(siteTaskId);
		if (null == siteName) {
			throw new ServiceException("站点任务[" + siteTaskId + "]获取失败");
		}
		return siteName;
	}
	
	@Override
	public Region retrieveSiteRegion(int siteTaskId) {
		return siteRegionMap.get(siteTaskId);
	}
	
	@Override
	public Integer retrieveSiteLevel(int siteTaskId) {
		return siteLevelMap.get(siteTaskId);
	}
	
	@Override
	public Map<Integer, Integer> retrieveSitePageTemplateIdMap() {
		return sitePageTemplateIdMap;
	}

	@Override
	public void cacheForTargetTasks(List<TargetTask> targetTasks)
			throws ServiceException {
		if (targetTasks.isEmpty()) {
			return;
		}
		Set<Integer> siteTaskIds = new HashSet<Integer>();
		for (TargetTask task : targetTasks) {
			siteTaskIds.add(task.getSiteTaskId());
		}
		cacheTaskInfo(siteTaskIds);
	}

	@Override
	public void cacheTaskInfo(Set<Integer> siteTaskIds) throws ServiceException {
		if (siteTaskIds.isEmpty()) {
			return;
		}
		StringBuilder sql = new StringBuilder(
				"select c_id,c_name,c_province,c_city,c_county,c_level,c_id_template_page from t_task_site where c_id in (");
		boolean allCached = true;
		for (Integer siteTaskId : siteTaskIds) {
			if (siteTaskId == 0) {
				continue;
			}
			if (null != siteNameMap.get(siteTaskId)) {
				continue;
			}
			if (allCached) {
				allCached = false;
				sql.append('\'');
			} else {
				sql.append(", '");
			}
			sql.append(siteTaskId).append('\'');
		}
		if (allCached) {
			return;
		}
		sql.append(')');

		try {
			siteTaskDao.cacheTaskInfo(siteNameMap, siteRegionMap, siteLevelMap, sitePageTemplateIdMap, sql.toString());
		} catch (SQLException | DataAccessException e) {
			throw new ServiceException(e);
		} finally {
			MyDataSource.releaseCurrentConnection();
		}
	}

	@Override
	public void updateSiteNamesByLocalNotice() throws ServiceException {
		File file = new File(ConstantsHome.USER_DIR + File.separatorChar
				+ "update" + File.separatorChar + "site");
		if (!file.isFile()) {
			return;
		}
		List<String> siteIds;
		try {
			siteIds = FileUtils.readLines(file, Charset.defaultCharset());
		} catch (IOException e) {
			throw new ServiceException("站点名称更新文件读取失败", e);
		}
		uncacheTemplates(new HashSet<String>(siteIds));
		file.delete();
	}

	private void uncacheTemplates(HashSet<String> siteIds) {
		for (String siteId : siteIds) {
			siteNameMap.remove(siteId);
			siteRegionMap.remove(siteId);
			siteLevelMap.remove(siteId);
			sitePageTemplateIdMap.remove(siteId);
		}
	}
/*
	@Override
	public void countCity(String city) {
		AtomicInteger count = cityCountMap.get(city);
		if (null != count) {
			count.incrementAndGet();
		}
	}
	
	@Override
	public void countProvince(String province) {
		AtomicInteger count = provinceCountMap.get(province);
		if (null != count) {
			count.incrementAndGet();
		}
	}
	
	@Override
	public Map<String, Integer> getAndClearCityCountMap() {
		Map<String, Integer> res = new HashMap<>();
		Set<Entry<String, AtomicInteger>> entrySet = cityCountMap.entrySet();
		for (Entry<String, AtomicInteger> entry : entrySet) {
			AtomicInteger value = entry.getValue();
			int count = value.get();
			if (count != 0) {
				res.put(entry.getKey(), count);
				value.set(0);
			}
		}
		return res;
	}

	@Override
	public Map<String, Integer> getAndClearProvinceCountMap() {
		Map<String, Integer> res = new HashMap<>();
		Set<Entry<String, AtomicInteger>> entrySet = provinceCountMap.entrySet();
		for (Entry<String, AtomicInteger> entry : entrySet) {
			AtomicInteger value = entry.getValue();
			int count = value.get();
			if (count != 0) {
				res.put(entry.getKey(), count);
				value.set(0);
			}
		}
		return res;
	}*/
}
