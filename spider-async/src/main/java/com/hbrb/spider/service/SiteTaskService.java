package com.hbrb.spider.service;

import java.util.List;

import com.hbrb.exception.ServiceException;
import com.hbrb.spider.model.task.SiteTask;

public interface SiteTaskService {
	/**
	 * 获取没有在执行的（正在执行任务的爬虫ID为null）前limit条任务，并更新正在执行任务的爬虫ID
	 * @param spiderId	指定爬虫ID
	 * @param limit	指定任务数
	 * @return
	 * @throws ServiceException
	 */
	List<SiteTask> tasksForCrawl(int spiderId, int limit) throws ServiceException;
	List<SiteTask> tasksForTest(int spiderId) throws ServiceException;

	/**
	 * 将所有指定爬虫在执行的任务的“爬虫ID”字段设置为null
	 * @param spiderId	指定爬虫ID
	 * @throws ServiceException
	 */
	void releaseTasks(int spiderId) throws ServiceException;

	List<SiteTask> loadTaskForSpider(int spiderId, int limit) throws ServiceException;
}
