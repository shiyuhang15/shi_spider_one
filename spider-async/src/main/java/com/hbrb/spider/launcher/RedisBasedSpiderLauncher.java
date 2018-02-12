package com.hbrb.spider.launcher;

import com.hbrb.spider.model.SpiderConfig;
import com.hbrb.util.JedisUtils;

public class RedisBasedSpiderLauncher implements SpiderLauncher {
	private final SpiderLauncher target;

	public RedisBasedSpiderLauncher(SpiderLauncher target) {
		super();
		this.target = target;
	}

	public void launchSpider(SpiderConfig config) {
		JedisUtils.initPool(config.getRedisHost(), config.getRedisPort());
		try {
			target.launchSpider(config);
		} finally {
			JedisUtils.closePool();
		}
	}
}
