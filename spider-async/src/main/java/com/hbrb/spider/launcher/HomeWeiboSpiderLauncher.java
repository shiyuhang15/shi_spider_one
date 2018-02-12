package com.hbrb.spider.launcher;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.RandomUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import com.hbrb.exception.ServiceException;
import com.hbrb.spider.ConstantsHome;
import com.hbrb.spider.Spider;
import com.hbrb.spider.SyncSpider;
import com.hbrb.spider.downloader.sync.HttpClientDownloader;
import com.hbrb.spider.downloader.sync.PageAdapter;
import com.hbrb.spider.downloader.sync.PlainPageBuilder;
import com.hbrb.spider.model.RawResult;
import com.hbrb.spider.model.SpiderConfig;
import com.hbrb.spider.model.WeiboUser;
import com.hbrb.spider.model.page.PlainPage;
import com.hbrb.spider.model.task.GenericRequestTask;
import com.hbrb.spider.model.task.HomeWeiboRequestTask;
import com.hbrb.spider.pageprocessor.PageProcessorFactory;
import com.hbrb.spider.pageprocessor.weibo.HomeWeiboPageProcessor;
import com.hbrb.spider.pipeline.FilePipeline;
import com.hbrb.spider.service.RegionService;
import com.hbrb.spider.service.ServiceFactory;
import com.hbrb.util.JedisUtils;
import com.hbrb.util.ServiceUtils;

public class HomeWeiboSpiderLauncher implements SpiderLauncher{
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HomeWeiboSpiderLauncher.class);
	@Override
	public void launchSpider(SpiderConfig config) {
		HttpClientDownloader downloader = new HttpClientDownloader(config, true, null, null);
		PageAdapter<HomeWeiboRequestTask, PlainPage<HomeWeiboRequestTask>> gpDownloader = new PageAdapter<>(downloader,
				new PlainPageBuilder<HomeWeiboRequestTask>(config.getCharset()));
		FilePipeline pipeline = new FilePipeline(downloader, null);
		HomeWeiboPageProcessor pageProcessor = new HomeWeiboPageProcessor(pipeline);
		Spider<HomeWeiboRequestTask, PlainPage<HomeWeiboRequestTask>> spider = new SyncSpider<>(config, null,
				gpDownloader, null, PageProcessorFactory.wrap(pageProcessor));
		
		// 初始化redis
		String redisHost = config.getRedisHost();
		int redisPort = config.getRedisPort();
		if (null == redisHost || redisHost.length() == 0 || redisPort <= 0) {
			logger.error("redis config error");
			return;
		}
		JedisUtils.initPool(redisHost, redisPort);
		try {
			for (;;) {
				// 获取任务
				List<WeiboUser> users;
				try {
					users = ServiceFactory.getWeiboUser2Service().retrieveAll();
				} catch (ServiceException e) {
					logger.error("get tasks for crawl failed", e);
					return;
				}
				if (users.isEmpty()) {
					logger.info("no task for crawl");
					break;
				}
				// cookie
				List<String> cookies;
				try {
					cookies = ServiceFactory.getWeiboVisitorCookieService().getAllCookies();
				} catch (ServiceException e) {
					logger.error("get cookies failed", e);
					return;
				}
				int cookieSize = cookies.size();
				if (0 == cookieSize) {
					logger.error("no cookies");
					break;
				}
				
				for (WeiboUser user : users) {
					HomeWeiboRequestTask request = new HomeWeiboRequestTask(
							"http://weibo.com/u/" + user.getId() + "?is_all=1",
							HomeWeiboPageProcessor.REQUEST_TYPE_START);
					request.setUser(user);
					request.setPagebar(-1);
					request.setPage_num(1);
					Map<String, String> requestHeaders = new HashMap<String, String>();
					requestHeaders.put(ConstantsHome.RequestHeader.COOKIE,
							cookies.get(RandomUtils.nextInt(0, cookieSize)));
					request.setHeaders(requestHeaders);
					spider.addTask(request);
					logger.info("start task - {}", request.getUrl());
				}
				
				spider.run();
				try {
					pipeline.close();
				} catch (IOException e) {
					logger.error("pipeline close 失败", e);
					break;
				}
				
				// 地域计数
				String base = null;
				try {
					base = ServiceUtils.getHttpServiceBase();
				} catch (Throwable e) {
					logger.warn(e.getMessage());
				}
				if (base != null) {
					String regionCount = ServiceFactory.getRegionService().getRegionCount();
					logger.info("region count - {}", regionCount);
					if (null != regionCount) {
						String countUrl = base + RegionService.PATH_COUNT_REGION;
						GenericRequestTask req = new GenericRequestTask(GenericRequestTask.METHOD_POST, countUrl, 0);
						req.setEntity(new StringEntity(regionCount, ContentType.APPLICATION_JSON));
						RawResult res = downloader.execute(req);
						if (null != res && res.getStatusCode() == HttpStatus.SC_OK) {
							ServiceFactory.getRegionService().clearRegionCountMap();
							logger.info("post ok - {}", countUrl);
						} else {
							logger.warn("post failed - {}", countUrl);
						}
					}
				}
				
				File block = new File(ConstantsHome.USER_DIR + File.separatorChar + "block");
				if (block.exists()) {
					logger.info("met block");
					break;
				}
				
				// 间息
				if (config.getInterval() > 0) {
					logger.info("interval sleeping - {}", config.getInterval());
					try {
						Thread.sleep(config.getInterval() * 1000);
					} catch (InterruptedException e) {}
					if (block.exists()) {
						logger.info("met block");
						break;
					}
				}
			}
		} finally {
			try {
				pipeline.close();
			} catch (IOException e) {
				logger.error("pipeline close 失败", e);
			} finally {
				spider.close();
			}
		}
	}
	
	public static void main(String[] args) {
		BasicSpiderLauncher.launch(new RedisBasedSpiderLauncher(new HomeWeiboSpiderLauncher()), args);
	}
}
