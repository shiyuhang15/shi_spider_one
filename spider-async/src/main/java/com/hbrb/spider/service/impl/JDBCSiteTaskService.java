package com.hbrb.spider.service.impl;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import com.hbrb.exception.DataAccessException;
import com.hbrb.exception.ServiceException;
import com.hbrb.spider.Spider;
import com.hbrb.spider.dao.DaoFactory;
import com.hbrb.spider.dao.SiteTaskDao;
import com.hbrb.spider.model.task.SiteTask;
import com.hbrb.spider.service.SiteTaskService;

public class JDBCSiteTaskService implements SiteTaskService {
	private final SiteTaskDao siteTaskDao = DaoFactory.getSiteTaskDao();

	@Override
	public List<SiteTask> tasksForCrawl(int spiderId, int limit)
			throws ServiceException {
		int res;
		try {
			res = siteTaskDao.updateForCrawl(spiderId, limit);
		} catch (SQLException | DataAccessException e) {
			throw new ServiceException("爬虫[" + spiderId + "]的任务更新异常", e);
		}

		if (res == 0) {
			return Collections.emptyList();
		}
		try {
			return siteTaskDao.listBySpiderId(spiderId);
		} catch (SQLException | DataAccessException e) {
			throw new ServiceException("爬虫[" + spiderId + "]的任务获取异常", e);
		}
	}

	@Override
	public List<SiteTask> tasksForTest(int spiderId) throws ServiceException {
		try {
			return siteTaskDao.listBySpiderId(spiderId);
		} catch (SQLException | DataAccessException e) {
			throw new ServiceException("爬虫[" + spiderId + "]的任务获取异常", e);
		}
	}

	@Override
	public void releaseTasks(int spiderId) throws ServiceException {
		try {
			siteTaskDao.releaseTasks(spiderId);
		} catch (SQLException | DataAccessException e) {
			throw new ServiceException("爬虫[" + spiderId + "]的任务释放异常", e);
		}
	}

	@Override
	public List<SiteTask> loadTaskForSpider(int spiderId, int limit)
			throws ServiceException {
		// 先释放任务（如果有异常退出的情况可能存在之前未释放掉的任务）
		if (!Spider.inTestMode()) {
			releaseTasks(spiderId);
		}

		// 获取任务
		if (!Spider.inTestMode()) {
			return tasksForCrawl(spiderId, limit);
		} else {
			return tasksForTest(spiderId);
		}
	}

}
