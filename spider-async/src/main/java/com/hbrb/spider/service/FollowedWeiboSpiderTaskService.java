package com.hbrb.spider.service;

import java.util.List;

import com.hbrb.exception.ServiceException;
import com.hbrb.spider.model.task.FollowedWeiboSpiderTask;

public interface FollowedWeiboSpiderTaskService {
	List<FollowedWeiboSpiderTask> tasksForCrawl(String spiderId, int limit) throws ServiceException;
	List<FollowedWeiboSpiderTask> getTasksBySpiderId(String spiderId) throws ServiceException;
	void releaseTasks(String spiderId) throws ServiceException;
	void removeInvalidCookie(Long taskId) throws ServiceException;
}
