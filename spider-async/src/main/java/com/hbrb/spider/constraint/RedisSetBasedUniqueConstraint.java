package com.hbrb.spider.constraint;

import com.hbrb.util.JedisUtils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

public class RedisSetBasedUniqueConstraint implements UniqueConstraint {
	private final String KEY_SET_ID;
	
	public RedisSetBasedUniqueConstraint(String idSetKey) {
		super();
		if (null == idSetKey || idSetKey.isEmpty()) {
			throw new IllegalArgumentException("去重集合的key不能为空");
		}
		this.KEY_SET_ID = idSetKey;
	}

	@Override
	public boolean isDuplicated(String id) {
		if (null == id || id.isEmpty()) {
			throw new IllegalArgumentException("id不能为空");
		}
		try (Jedis jedis = JedisUtils.createJedis()) {
			Long res = jedis.sadd(KEY_SET_ID, id);
			if (res == 1) {
				return false;
			} else if (res == 0) {
				return true;
			} else {
				throw new JedisException("sadd 结果异常 - " + res);
			}
		}
	}

}
