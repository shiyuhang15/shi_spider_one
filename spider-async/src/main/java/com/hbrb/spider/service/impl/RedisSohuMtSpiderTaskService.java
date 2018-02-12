package com.hbrb.spider.service.impl;

import java.util.ArrayList;
import java.util.List;

import com.hbrb.exception.ServiceException;
import com.hbrb.spider.model.SohuMtUser;
import com.hbrb.spider.service.SohuMtSpiderTaskService;
import com.hbrb.util.JedisUtils;
import com.hbrb.util.TaskUtils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

public class RedisSohuMtSpiderTaskService implements
	SohuMtSpiderTaskService {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(RedisSohuMtSpiderTaskService.class);
	@Override
	public List<SohuMtUser> tasksForSpider(String spiderId, int limit)
			throws ServiceException {
		try (Jedis jedis = JedisUtils.createJedis()) {
			double[] taskTotals = { jedis.llen(KEY_PREFIX_QUEUE + 1),
					jedis.llen(KEY_PREFIX_QUEUE + 2),
					jedis.llen(KEY_PREFIX_QUEUE + 3),
					jedis.llen(KEY_PREFIX_QUEUE + 4),
					jedis.llen(KEY_PREFIX_QUEUE + 5) };
			double[] weights = {1, 4, 24, 144, 1440};
			int[] pops = TaskUtils.countPops(limit, taskTotals, weights);
			return popTasks(jedis, limit, pops);
		}
	}
	

	private List<SohuMtUser> popTasks(Jedis jedis, int limit, int[] pops) {
		List<SohuMtUser> tasks = new ArrayList<SohuMtUser>(limit);
		for (int i = 0; i < pops.length; i++) {
			if (pops[i] == 0) {
				continue;
			}
			
			int taskLevel = i + 1;
			logger.info("pop " + pops[i] + " tasks from level " + taskLevel);
			String key = KEY_PREFIX_QUEUE + taskLevel;
			Pipeline pipe = jedis.pipelined();
			for (int j = 0; j < pops[i]; j++) {
				pipe.rpoplpush(key, key);
			}
			List<Object> ress = pipe.syncAndReturnAll();
			for (Object res : ress) {
				if (null == res) {
					continue;
				}
				tasks.add(new SohuMtUser((String)res, null, taskLevel));
			}
		}
		return tasks;
	}
}