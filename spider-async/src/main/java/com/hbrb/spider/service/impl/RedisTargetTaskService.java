package com.hbrb.spider.service.impl;

import java.util.ArrayList;
import java.util.List;

import com.hbrb.exception.ServiceException;
import com.hbrb.json.JSException;
import com.hbrb.json.JSObject;
import com.hbrb.json.JSUtils;
import com.hbrb.spider.model.task.TargetTask;
import com.hbrb.spider.service.TargetTaskService;
import com.hbrb.util.JedisUtils;

import redis.clients.jedis.Jedis;

public class RedisTargetTaskService implements TargetTaskService {
	// 目标任务的RedisKey最好通过配置获取
	private static final String KEY_REDIS_TASKS_TARGET = "set_task_target";

	@Override
	public List<TargetTask> tasksForCrawl(int limit, boolean test) throws ServiceException {
		List<TargetTask> tasks = new ArrayList<TargetTask>();
		try (Jedis jedis = JedisUtils.createJedis()) {
			if (test) {
				List<String> srandmember = jedis.srandmember(KEY_REDIS_TASKS_TARGET, limit);
				for (String strTask : srandmember) {
					if (null == strTask) {
						continue;
					}
					tasks.add(new TargetTask(JSUtils.createJSObject(strTask)));
				}
			} else {
				for (int i = 0; i < limit; i++) {
					String strTask = jedis.spop(KEY_REDIS_TASKS_TARGET);
					if (null == strTask) {
						break;
					}
					tasks.add(new TargetTask(JSUtils.createJSObject(strTask)));
				}
			}
			return tasks;
		} catch (JSException e) {
			throw new ServiceException("获取目标任务失败");
		}
	}

	@Override
	public int createTask(TargetTask task) throws ServiceException {
		JSObject json = task.toJson();
		String value = json.toString();
		try (Jedis jedis = JedisUtils.createJedis()) {
			return jedis.sadd(KEY_REDIS_TASKS_TARGET, value).intValue();
		}
	}
}
