package com.hbrb.spider.service;

import java.util.List;
import java.util.Map;

import com.hbrb.exception.ServiceException;
import com.hbrb.spider.model.task.TemplateTask;

public interface TemplateService<E> {
	/**
	 * 从数据库获取指定任务所需的模板并缓存
	 * @param tasks
	 * @throws ServiceException
	 */
	<T extends TemplateTask> void cacheForTasks(List<T> tasks, Map<Integer, Integer> siteTaskIdTemplateIdMap)
			throws ServiceException;

	void cacheTemplates(Map<Integer, List<Integer>> templateIdSiteTaskIdsMap) throws ServiceException;

	/**
	 * 从缓存获取模板
	 * @param templateId
	 * @return
	 * @throws ServiceException
	 */
	E retrieveTemplate(int templateId);
	
	void cleanCache();
	
	/**
	 * 删除缓存的模板，下次使用到的时候重新从数据库加载
	 * @throws ServiceException
	 */
	void updateTemplatesByLocalNotice() throws ServiceException;
	void uncacheTemplates(List<String> templateIds);
}
