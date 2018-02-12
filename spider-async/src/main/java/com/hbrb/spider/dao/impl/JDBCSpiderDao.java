package com.hbrb.spider.dao.impl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.hbrb.spider.dao.SpiderDao;
import com.hbrb.spider.model.SpiderConfig;
import com.hbrb.exception.DataAccessException;

public class JDBCSpiderDao extends JDBCBasicDao implements SpiderDao {

	@Override
	public boolean updateSpiderStatus(String spiderId, int status)
			throws SQLException {
		if (1 == executeUpdate("update t_spider set c_status = " + status
				+ " where c_id = '" + spiderId + "'")) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void loadSpiderConfig(SpiderConfig config, PreparedStatement stmt, int spiderId)
			throws SQLException, DataAccessException {
		stmt.setInt(1, spiderId);
		try (ResultSet rs = stmt.executeQuery()) {
			if (!rs.next()) {
				throw new DataAccessException("spider[" + spiderId + "] not exist");
			}
			// 名称
			String name = rs.getString(1);
			if (!name.isEmpty()) {
				config.setName(name);
			}
			// 本机IP
			/*String ips = rs.getString("c_ip");
			if (null != ips && (ips = ips.trim()).length() != 0) {
				site.setIps(ips);
			}*/
			// 信源类型
			/*int recType = rs.getInt("c_type_source");
			if (!rs.wasNull() && recType > 0) {
				site.setSourceName(sourceName);
			}*/
			// 最大并发连接数
			int connectionMaxTotal = rs.getInt(2);
			if (connectionMaxTotal > 0) {
				config.setConnectionMaxTotal(connectionMaxTotal);
			}
			// connectionDefaultMaxPerRoute
			int connectionDefaultMaxPerRoute = rs.getInt(3);
			if (connectionDefaultMaxPerRoute > 0) {
				config.setConnectionDefaultMaxPerRoute(connectionDefaultMaxPerRoute);
			}
			// 页面编码
			String charset = rs.getString(4);
			if (!charset.isEmpty()) {
				config.setCharset(charset);
			}
			// 两次请求间隔时间
			/*int sleepTime = rs.getInt("c_sleep_request");
			if (!rs.wasNull() && sleepTime >= 0) {
				site.setSleepTime(sleepTime);
			}*/
			// 重试次数
			int retryTimes = rs.getInt(5);
			if (retryTimes >= 0) {
				config.setRetryTimes(retryTimes);
			}
			// 循环重试次数
			int cycleRetryTimes = rs.getInt(6);
			if (retryTimes >= 0) {
				config.setCycleRetryTimes(cycleRetryTimes);
			}
			// 重试间隔时间
			/*int retrySleepTime = rs.getInt("c_sleep_retry");
			if (!rs.wasNull() && retrySleepTime >= 0) {
				site.setRetrySleepTime(retrySleepTime);
			}*/
			// 超时时间
			int timeout = rs.getInt(7);
			if (timeout > 0) {
				config.setSoTimeout(timeout);
				config.setConnectTimeout(timeout);
				config.setReactorSoTimeout(timeout);
				config.setReactorConnectTimeout(timeout);
			}
			// 请求分配连接的超时时间
			int connectionRequestTimeout = rs.getInt(8);
			if (connectionRequestTimeout > 0) {
				config.setConnectionRequestTimeout(connectionRequestTimeout);
			}
			// 两轮采集间隔时间
			int interval = rs.getInt(9);
			if (interval >= 0) {
				config.setInterval(interval);
			}
			// 任务数量
			int taskLimit = rs.getInt(10);
			if (taskLimit > 0) {
				config.setTaskLimit(taskLimit);
			}
			// redis
			String redisHost = rs.getString(11);
			if (!redisHost.isEmpty()) {
				config.setRedisHost(redisHost);
			}
			int redisPort = rs.getInt(12);
			if (redisPort > 0) {
				config.setRedisPort(redisPort);
			}
			// 请求间隔时间
			int requestInterval = rs.getInt(13);
			if (requestInterval >= 0) {
				config.setRequestInterval(requestInterval);
			}
			
			/*String idSetKey = rs.getString("c_redis_key_set_url_target");
			if (null != idSetKey && !(idSetKey = idSetKey.trim()).isEmpty()) {
				site.setIdSetRedisKey(idSetKey);
			}
			String targetTaskQueueRedisKeyPrefix = rs
					.getString("c_redis_key_queue_task_target");
			if (null != targetTaskQueueRedisKeyPrefix
					&& (targetTaskQueueRedisKeyPrefix = targetTaskQueueRedisKeyPrefix
							.trim()).length() != 0) {
				site.setTargetTaskQueueRedisKeyPrefix(targetTaskQueueRedisKeyPrefix);
			}*/
		}
	}

}
