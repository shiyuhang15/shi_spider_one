package com.hbrb.spider.launcher;

import com.hbrb.exception.ServiceException;
import com.hbrb.jdbc.MyDataSource;
import com.hbrb.spider.AsyncSpider;
import com.hbrb.spider.model.SpiderConfig;
import com.hbrb.spider.service.ServiceFactory;

public class BasicSpiderLauncher {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(BasicSpiderLauncher.class);

	public BasicSpiderLauncher() {
		super();
	}

	public static void launch(SpiderLauncher target, String[] args) {
		if (args.length == 0) {
			logger.error("缺少爬虫ID");
			return;
		}
		int[] spiderIds = new int[args.length];
		for (int i = 0; i < spiderIds.length; i++) {
			spiderIds[i] = Integer.parseInt(args[i]);
		}
		AsyncSpider.setTestMode(spiderIds[spiderIds.length - 1] < 0);

		MyDataSource.init();
		try {
			target.launchSpider(loadConfig(spiderIds));
		} catch (Throwable e) {
			logger.error("异常退出", e);
		} finally {
			MyDataSource.destroy();
		}
	}

	private static SpiderConfig loadConfig(int[] spiderIds) {
		// 获取配置
		try {
			return ServiceFactory.getSpiderService().buildSpiderConfig(spiderIds);
		} catch (ServiceException e) {
			throw new Error("load spider config error", e);
		}
	}

}
