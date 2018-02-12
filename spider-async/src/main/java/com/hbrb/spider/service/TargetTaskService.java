package com.hbrb.spider.service;

import java.util.List;

import com.hbrb.exception.ServiceException;
import com.hbrb.spider.model.task.TargetTask;

public interface TargetTaskService {
	/**
	 * 新建任务
	 * @param task
	 * @throws ServiceException
	 */
	int createTask(TargetTask task) throws ServiceException;
	
	/**
	 * 获取指定信源类型且没有在执行的（正在执行任务的爬虫ID为null）前limit条任务，并更新正在执行任务的爬虫ID
	 * @param spiderId	指定爬虫ID
	 * @param sourceType	指定信源类型
	 * @param limit	指定任务数
	 * @return
	 * @throws ServiceException
	 */
	List<TargetTask> tasksForCrawl(int limit, boolean test) throws ServiceException;
}
