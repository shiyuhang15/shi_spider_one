package com.hbrb.spider.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hbrb.exception.ServiceException;
import com.hbrb.spider.model.Region;
import com.hbrb.spider.model.task.TargetTask;

public interface FillerService {
	String retrieveSiteName(int siteTaskId) throws ServiceException;
	Region retrieveSiteRegion(int siteTaskId);
	Integer retrieveSiteLevel(int siteTaskId);
	Map<Integer, Integer> retrieveSitePageTemplateIdMap();

	void cacheForTargetTasks(List<TargetTask> targetTasks)
			throws ServiceException;

	void cacheTaskInfo(Set<Integer> siteTaskIds) throws ServiceException;

	void updateSiteNamesByLocalNotice() throws ServiceException;
}
