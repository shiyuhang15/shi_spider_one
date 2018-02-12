package com.hbrb.spider.launcher;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.hbrb.exception.ServiceException;
import com.hbrb.spider.ConstantsHome;
import com.hbrb.spider.Spider;
import com.hbrb.spider.AsyncSpider;
import com.hbrb.spider.constraint.RedisBasedUniqueConstraint;
import com.hbrb.spider.constraint.RedisSetBasedUniqueConstraint;
import com.hbrb.spider.downloader.async.HtmlPageBufferAdaptor;
import com.hbrb.spider.downloader.async.HttpAsyncClientDownloader;
import com.hbrb.spider.downloader.async.RawResultBuffer;
import com.hbrb.spider.model.SpiderConfig;
import com.hbrb.spider.model.page.HtmlPage;
import com.hbrb.spider.model.task.NaviTask;
import com.hbrb.spider.pageprocessor.EfficientNaviPageProcessor;
import com.hbrb.spider.pageprocessor.PageProcessorFactory;
import com.hbrb.spider.scheduler.NaviTaskRequestBuffer;
import com.hbrb.spider.service.ServiceFactory;
import com.hbrb.util.ProcessUtils;

public class NaviTaskSpiderLauncher implements SpiderLauncher {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(NaviTaskSpiderLauncher.class);
	
	@Override
	public void launchSpider(SpiderConfig config) {
		System.out.println(config.getSpiderId());
		ProcessUtils.loadProperties(null);
		
		RawResultBuffer<NaviTask> resultBuffer = new RawResultBuffer<>(config.getConnectionMaxTotal());
		NaviTaskRequestBuffer<NaviTask> requestBuffer = new NaviTaskRequestBuffer<>();
		AsyncSpider<NaviTask, HtmlPage<NaviTask>> spider = new AsyncSpider<>(
				Spider.inTestMode() ? new RedisSetBasedUniqueConstraint("set_md5_url_target")
						: new RedisBasedUniqueConstraint(),
				new HttpAsyncClientDownloader<>(resultBuffer, config), new HtmlPageBufferAdaptor<>(resultBuffer), requestBuffer,
				PageProcessorFactory.wrap(new EfficientNaviPageProcessor()));
		try {
			for (;;) {
				// 加载导航任务
				List<NaviTask> tasks;
				try {
					tasks = ServiceFactory.getNaviTaskService().reloadTaskForSpider(config.getSpiderId(),
							config.getTaskLimit());
				} catch (ServiceException e) {
					logger.error("load navi tasks failed", e);
					break;
				}
				
				if (tasks.isEmpty()) {
					logger.info("no task for crawl");
					try {
						Thread.sleep(30000);
					} catch (InterruptedException e) {}
				} else {
					// 缓存模板
					try {
						ServiceFactory.getUrlTemplateService().cacheForTasks(tasks,
								Collections.<Integer, Integer> emptyMap());
					} catch (ServiceException e) {
						logger.error("cache templates failed", e);
						break;
					}
					
					// 启动任务
					requestBuffer.reset(tasks);
					tasks = null;
					spider.run();
					logger.info("tasks all finished");
				}
				
				File block = new File(ConstantsHome.USER_DIR + File.separatorChar + "block");
				if (block.exists()) {
					logger.info("met block");
					break;
				}
				
				// 依据通知更新缓存
				try {
					ServiceFactory.getUrlTemplateService().updateTemplatesByLocalNotice();
				} catch (ServiceException e) {
					logger.warn("update templates failed", e);
				}
				try {
					ProcessUtils.updateProperties();
				} catch (IOException e) {
					logger.warn("update properties failed", e);
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
				ServiceFactory.getNaviTaskService().releaseTasks(config.getSpiderId());
			} catch (ServiceException e) {
				logger.error("release tasks failed", e);
			} finally {
				spider.close();
			}
		}
	}
	
	public static void main(String[] args) {
		BasicSpiderLauncher.launch(new RedisBasedSpiderLauncher(new NaviTaskSpiderLauncher()), args);
	}
}
