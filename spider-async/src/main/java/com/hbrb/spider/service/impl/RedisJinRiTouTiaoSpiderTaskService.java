package com.hbrb.spider.service.impl;

import java.util.ArrayList;
import java.util.List;

import com.hbrb.exception.ServiceException;
import com.hbrb.spider.model.Toutiaohao;
import com.hbrb.spider.service.JinRiTouTiaoSpiderTaskService;
import com.hbrb.util.JedisUtils;
import com.hbrb.util.TaskUtils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

public class RedisJinRiTouTiaoSpiderTaskService implements
	JinRiTouTiaoSpiderTaskService {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(RedisJinRiTouTiaoSpiderTaskService.class);
	@Override
	public List<Toutiaohao> tasksForSpider(String spiderId, int limit)
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
	

	private List<Toutiaohao> popTasks(Jedis jedis, int limit, int[] pops) {
		List<Toutiaohao> tasks = new ArrayList<Toutiaohao>(limit);
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
				long uid;
				try {
					uid = Long.parseLong((String)res);
				} catch (NumberFormatException e) {
					throw new Error("uid[" + res + "]非数字", e);
				}
				tasks.add(new Toutiaohao(uid, null, taskLevel));
			}
		}
		return tasks;
	}
}