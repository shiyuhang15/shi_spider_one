package com.hbrb.spider.service.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.hbrb.jdbc.MyDataSource;
import com.hbrb.spider.dao.DaoFactory;
import com.hbrb.spider.dao.SpiderDao;
import com.hbrb.spider.model.SpiderConfig;
import com.hbrb.exception.DataAccessException;
import com.hbrb.exception.ServiceException;
import com.hbrb.spider.service.SpiderService;

public class JDBCSpiderService implements SpiderService {
	private final SpiderDao spiderDao = DaoFactory.getSpiderDao();

	@Override
	public SpiderConfig buildSpiderConfig(int[] spiderIds) throws ServiceException {
		SpiderConfig config = new SpiderConfig();
		config.setSpiderId(spiderIds[spiderIds.length - 1]);
		Connection conn = MyDataSource.getCurrentConnection();
		int i = 0;
		try (PreparedStatement stmt = conn
				.prepareStatement("select c_name,c_connection_max_total,c_connection_max_route,c_charset,c_times_retry,c_times_retry_cycle,c_timeout,c_timeout_request_connection,c_interval,c_limit_task,c_redis_ip,c_redis_port,c_interval_request from t_spider_async where c_id=?")) {
			while (i < spiderIds.length) {
				spiderDao.loadSpiderConfig(config, stmt, spiderIds[i]);
				i++;
			}
		} catch (SQLException | DataAccessException e) {
			throw new ServiceException("爬虫[" + spiderIds[i] + "]配置加载失败", e);
		}
		return config;
	}

	@Override
	public boolean updateSpiderStatus(String spiderId, int status)
			throws ServiceException {
		try {
			return spiderDao.updateSpiderStatus(spiderId, status);
		} catch (SQLException e) {
			throw new ServiceException("爬虫[" + spiderId + "]状态[" + status
					+ "]更新失败", e);
		}
	}

}
