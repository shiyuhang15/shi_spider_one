package com.hbrb.spider.launcher;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.entity.ContentType;

import com.hbrb.exception.ServiceException;
import com.hbrb.spider.AsyncSpider;
import com.hbrb.spider.ConstantsHome;
import com.hbrb.spider.Spider;
import com.hbrb.spider.downloader.async.HtmlPageBufferAdaptor;
import com.hbrb.spider.downloader.async.HttpAsyncClientDownloader;
import com.hbrb.spider.downloader.async.RawResultBuffer;
import com.hbrb.spider.model.SpiderConfig;
import com.hbrb.spider.model.page.HtmlPage;
import com.hbrb.spider.model.task.GenericRequestTask;
import com.hbrb.spider.model.task.TargetTask;
import com.hbrb.spider.pageprocessor.PageProcessor;
import com.hbrb.spider.pageprocessor.TargetPageProcessor;
import com.hbrb.spider.pageprocessor.aspect.BasicPageProcessExceptionHandler;
import com.hbrb.spider.pipeline.FilePipeline;
import com.hbrb.spider.scheduler.NaviTaskRequestBuffer;
import com.hbrb.spider.service.RegionService;
import com.hbrb.spider.service.ServiceFactory;
import com.hbrb.util.ProcessUtils;
import com.hbrb.util.ServiceUtils;

public class TargetTaskSpiderLauncher implements SpiderLauncher {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TargetTaskSpiderLauncher.class);
	@Override
	public void launchSpider(SpiderConfig config) {
		ProcessUtils.loadProperties(null);
		
		RawResultBuffer<TargetTask> resultBuffer = new RawResultBuffer<>(config.getConnectionMaxTotal());
		NaviTaskRequestBuffer<TargetTask> requestBuffer = new NaviTaskRequestBuffer<>();
		HttpAsyncClientDownloader<TargetTask> downloader = new HttpAsyncClientDownloader<>(resultBuffer, config);
		FilePipeline pipeline = new FilePipeline(downloader,null);
		final PageProcessor<TargetTask, HtmlPage<TargetTask>> pageProcessor = new BasicPageProcessExceptionHandler<>(
				new TargetPageProcessor(pipeline));
		AsyncSpider<TargetTask, HtmlPage<TargetTask>> spider = new AsyncSpider<>(null,
				downloader, new HtmlPageBufferAdaptor<>(resultBuffer),
				requestBuffer, pageProcessor);
		try {
			for (;;) {
				// 加载目标任务
				List<TargetTask> tasks;
				try {
					tasks = ServiceFactory.getTargetTaskService().tasksForCrawl(config.getTaskLimit(),
							Spider.inTestMode());
				} catch (ServiceException e) {
					logger.error("get tasks failed", e);
					break;
				}
				
				if (tasks.isEmpty()) {
					logger.info("no task for crawl");
					try {
						Thread.sleep(30000);
					} catch (InterruptedException e) {}
				} else {
					// 缓存站点名称、地域、级别、模板id
					try {
						ServiceFactory.getFillerService().cacheForTargetTasks(tasks);
					} catch (ServiceException e) {
						logger.error("cache site name failed", e);
						return;
					}
					
					// 缓存模板
					try {
						ServiceFactory.getPageTemplateService().cacheForTasks(tasks,
								ServiceFactory.getFillerService().retrieveSitePageTemplateIdMap());
					} catch (ServiceException e) {
						logger.error("cache templates failed", e);
						break;
					}
					
					// 启动任务
					requestBuffer.reset(tasks);
					tasks = null;
					spider.run();
					logger.info("tasks all finished");
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
					if (null != base) {
						String regionCount = ServiceFactory.getRegionService().getRegionCount();
						logger.info("region count - {}", regionCount);
						if (null != regionCount) {
							String countUrl = base + RegionService.PATH_COUNT_REGION;
							try {
								spider.download(GenericRequestTask.METHOD_POST, countUrl, regionCount,
										ContentType.APPLICATION_JSON, null).get(10, TimeUnit.SECONDS);
								logger.info("post ok - {}", countUrl);
							} catch (Throwable e) {
								logger.warn("post failed - " + countUrl, e);
							}
						}
					}
					if (!Spider.inTestMode()) {
					}
				}
				
				File block = new File(ConstantsHome.USER_DIR + File.separatorChar + "block");
				if (block.exists()) {
					logger.info("met block");
					break;
				}
				
				// 依据通知更新缓存
				try {
					ServiceFactory.getPageTemplateService().updateTemplatesByLocalNotice();
				} catch (ServiceException e) {
					logger.warn("update templates failed", e);
				}
				try {
					ServiceFactory.getFillerService().updateSiteNamesByLocalNotice();
				} catch (ServiceException e) {
					logger.warn("update site name failed", e);
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
				pipeline.close();
			} catch (IOException e) {
				logger.error("pipeline close 失败", e);
			} finally {
				spider.close();
			}
		}
	}
	
	public static void main(String[] args) {
		BasicSpiderLauncher.launch(new RedisBasedSpiderLauncher(new TargetTaskSpiderLauncher()), args);
	}
}
