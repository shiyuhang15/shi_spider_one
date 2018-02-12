package com.hbrb.spider.service.impl;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import com.hbrb.exception.DataAccessException;
import com.hbrb.exception.ServiceException;
import com.hbrb.spider.Spider;
import com.hbrb.spider.dao.DaoFactory;
import com.hbrb.spider.dao.NaviTaskDao;
import com.hbrb.spider.model.task.SiteTask;
import com.hbrb.spider.model.task.NaviTask;
import com.hbrb.spider.service.NaviTaskService;

public class JDBCNaviTaskService implements NaviTaskService {
	private final NaviTaskDao naviTaskDao = DaoFactory.getNaviTaskDao();

	@Override
	public List<NaviTask> loadTaskForSpider(int spiderId, int limit)
			throws ServiceException {
		int res;
		try {
			res = naviTaskDao.updateForCrawl(spiderId, limit);
		} catch (SQLException | DataAccessException e) {
			throw new ServiceException("爬虫[" + spiderId + "]的导航任务更新异常", e);
		}
		if (res == 0) {
			return Collections.emptyList();
		}
		try {
			return naviTaskDao.listBySpiderId(spiderId);
		} catch (SQLException | DataAccessException e) {
			throw new ServiceException("爬虫[" + spiderId + "]的导航任务获取异常", e);
		}
	}

	@Override
	public void releaseTasks(int spiderId) throws ServiceException {
		try {
			naviTaskDao.releaseTasks(spiderId);
		} catch (SQLException | DataAccessException e) {
			throw new ServiceException("爬虫[" + spiderId + "]的导航任务释放异常", e);
		}
	}

	@Override
	public void createTask(SiteTask request) throws ServiceException {
		int count;
		try {
			count = naviTaskDao.countByUrl(request.getUrl());
		} catch (SQLException | DataAccessException e) {
			throw new ServiceException("验证URL[" + request.getUrl() + "]是否存在失败", e);
		}
		if (count != 0) {
			return;
		}

		NaviTask naviTask = new NaviTask(request.getUrl());
		naviTask.setName(request.getTitle());
		naviTask.setSourceType(request.getSourceType());
		naviTask.setSiteTaskId(request.getSiteTaskId());
		naviTask.setType(request.getType());
		try {
			naviTaskDao.create(naviTask);
		} catch (SQLException | DataAccessException e) {
			throw new ServiceException("创建导航任务失败 - " + naviTask.toString(), e);
		}
	}

	@Override
	public void deleteTask(long id) throws ServiceException {
		try {
			naviTaskDao.delete(id);
		} catch (SQLException | DataAccessException e) {
			throw new ServiceException("删除导航任务[" + id + "]异常", e);
		}
	}

	@Override
	public void updateWorkedTimeById(long id) throws ServiceException {
		try {
			naviTaskDao.updateWorkedTimeById(id, System.currentTimeMillis());
		} catch (SQLException | DataAccessException e) {
			throw new ServiceException("更新导航任务[" + id + "]的workedTime异常", e);
		}
	}

	@Override
	public List<NaviTask> reloadTaskForSpider(int spiderId, int limit)
			throws ServiceException {
		// 先释放任务（如果有异常退出的情况可能存在之前未释放掉的任务）
		if (!Spider.inTestMode()) {
			releaseTasks(spiderId);
		}

		// 获取任务
		if (Spider.inTestMode()) {
			return tasksForTest(spiderId);
		}
		return loadTaskForSpider(spiderId, limit);
	}

	@Override
	public List<NaviTask> tasksForTest(int spiderId) throws ServiceException {
		try {
			return naviTaskDao.listBySpiderId(spiderId);
		} catch (SQLException | DataAccessException e) {
			throw new ServiceException("爬虫[" + spiderId + "]的任务获取异常", e);
		}
	}

}
