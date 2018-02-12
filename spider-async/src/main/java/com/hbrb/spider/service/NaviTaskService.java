package com.hbrb.spider.service;

import java.util.List;

import com.hbrb.exception.ServiceException;
import com.hbrb.spider.model.task.SiteTask;
import com.hbrb.spider.model.task.NaviTask;

public interface NaviTaskService {
	/**
	 * 获取指定信源类型且没有在执行的（正在执行任务的爬虫ID为null）前limit条任务，并更新正在执行任务的爬虫ID
	 * @param spiderId	指定爬虫ID
	 * @param sourceType	指定信源类型
	 * @param limit	指定任务数
	 * @return
	 * @throws ServiceException
	 */
	List<NaviTask> loadTaskForSpider(int spiderId, int limit) throws ServiceException;
	List<NaviTask> tasksForTest(int spiderId) throws ServiceException;

	/**
	 * 将所有指定爬虫在执行的任务的“爬虫ID”字段设置为null
	 * @param spiderId	指定爬虫ID
	 * @throws ServiceException
	 */
	void releaseTasks(int spiderId) throws ServiceException;

	/**
	 * 如果请求所对应的任务已存在，则更新“最后采集到新闻的时间”
	 * 如果任务不存在，创建新任务
	 * @param request
	 * @param currentDate
	 * @throws ServiceException
	 */
	void createTask(SiteTask request) throws ServiceException;
	
	void deleteTask(long id) throws ServiceException;

	void updateWorkedTimeById(long id) throws ServiceException;

	List<NaviTask> reloadTaskForSpider(int spiderId, int limit) throws ServiceException;
}
