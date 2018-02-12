package com.hbrb.spider.constraint;

import com.hbrb.util.JedisUtils;

import redis.clients.jedis.Jedis;

public class RedisBasedUniqueConstraint implements UniqueConstraint {
	/**
	 * 过期时间
	 */
	private final int EX;

	public RedisBasedUniqueConstraint() {
		// 默认60天过期
		this(5184000);
	}

	public RedisBasedUniqueConstraint(int ex) {
		super();
		this.EX = ex;
	}

	@Override
	public boolean isDuplicated(String id) {
		try (Jedis jedis = JedisUtils.createJedis()) {
			long ttl = jedis.ttl(id);
			if (ttl < EX / 2) {
				jedis.setex(id, EX, "");
				if (ttl == -2) {
					return false;
				}
			}
			return true;
		}
	}

}
