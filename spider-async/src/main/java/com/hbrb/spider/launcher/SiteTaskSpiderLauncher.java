package com.hbrb.spider.launcher;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.helper.StringUtil;
import org.jsoup.select.Elements;

import com.hbrb.exception.LogicError;
import com.hbrb.exception.ServiceException;
import com.hbrb.spider.ConstantsHome;
import com.hbrb.spider.Spider;
import com.hbrb.spider.AsyncSpider;
import com.hbrb.spider.constraint.RedisBasedUniqueConstraint;
import com.hbrb.spider.constraint.RedisSetBasedUniqueConstraint;
import com.hbrb.spider.downloader.async.HtmlPageBufferAdaptor;
import com.hbrb.spider.downloader.async.HttpAsyncClientDownloader;
import com.hbrb.spider.downloader.async.RawResultBuffer;
import com.hbrb.spider.exception.PageProcessException;
import com.hbrb.spider.model.SpiderConfig;
import com.hbrb.spider.model.page.HtmlPage;
import com.hbrb.spider.model.task.GenericRequestTask;
import com.hbrb.spider.model.task.RequestTask;
import com.hbrb.spider.model.task.SiteTask;
import com.hbrb.spider.model.template.UrlsTemplate;
import com.hbrb.spider.pageprocessor.NaviPageProcessor;
import com.hbrb.spider.pageprocessor.PageProcessorFactory;
import com.hbrb.spider.scheduler.SiteTaskRequestBuffer;
import com.hbrb.spider.service.ServiceFactory;
import com.hbrb.spider.service.SiteTaskService;
import com.hbrb.spider.service.TemplateService;
import com.hbrb.util.JsoupUtils;
import com.hbrb.util.ProcessUtils;
import com.hbrb.util.TaskUtils;

public class SiteTaskSpiderLauncher implements SpiderLauncher {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SiteTaskSpiderLauncher.class);

	private void refreshTask(AsyncSpider<SiteTask, HtmlPage<SiteTask>> spider, SiteTask task) throws Exception {
		String startUrl = task.getUrl();
		if (startUrl.endsWith(ConstantsHome.MARK_URL_START_REFRESH)) {
			task.setUrl(startUrl.substring(0, startUrl.length() - ConstantsHome.MARK_URL_START_REFRESH.length()));
			IOException ex = null;
			HtmlPage<RequestTask> page = null;
			int count = 0;
			while (page == null && count < 3) {
				count++;
				logger.info("#REFRESH[{}] - {}", count, task.getUrl());
				try {
					page = JsoupUtils.buildHtmlPage(spider.download(GenericRequestTask.METHOD_GET, task.getUrl(), null, null, null).get(30, TimeUnit.SECONDS));
				} catch (IOException e) {
					ex = e;
				}
			}
			if (null == page) {
				throw ex;
			}

			Elements eles = page.getDocument().select("head > meta[http-equiv=REFRESH]");
			if (eles.isEmpty()) {
				throw new PageProcessException("head > meta[http-equiv=REFRESH] not found");
			}
			Pattern pa = Pattern.compile("URL=([^\\s;\"']+)");
			Matcher ma = pa.matcher(eles.get(0).attr("content"));
			if (!ma.find()) {
				throw new PageProcessException("URL=([^\\s;\"']+) not found");
			}
			String url = ma.group(1);
			String absUrl = StringUtil.resolve(startUrl, url);
			if (absUrl.isEmpty()) {
				throw new PageProcessException("resolve url failed - " + url);
			}
			task.setUrl(absUrl);
		}
	}

	@Override
	public void launchSpider(SpiderConfig config) {
		ProcessUtils.loadProperties(null);
		
		List<SiteTask> temp = new ArrayList<>();
		Set<String> naviSet = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
		NaviPageProcessor pageProcessor = new NaviPageProcessor(naviSet);
		
		RawResultBuffer<SiteTask> resultBuffer = new RawResultBuffer<>(config.getConnectionMaxTotal());
		SiteTaskRequestBuffer<SiteTask> requestBuffer = new SiteTaskRequestBuffer<>();
		AsyncSpider<SiteTask, HtmlPage<SiteTask>> spider = new AsyncSpider<>(
				Spider.inTestMode() ? new RedisSetBasedUniqueConstraint("set_md5_url_target")
						: new RedisBasedUniqueConstraint(),
				new HttpAsyncClientDownloader<>(resultBuffer, config), new HtmlPageBufferAdaptor<>(resultBuffer),
				requestBuffer, PageProcessorFactory.wrap(pageProcessor));
		try {
			for (;;) {
				temp.clear();
				SiteTaskService taskService = ServiceFactory.getSiteTaskService();
				// 获取任务
				List<SiteTask> tasks;
				if (Spider.inTestMode()) {
					try {
						tasks = taskService.tasksForTest(config.getSpiderId());
					} catch (ServiceException e) {
						logger.error("get tasks for test failed", e);
						return;
					}
				} else {
					// 先释放任务（如果有异常退出的情况可能存在之前未释放掉的任务）
					try {
						taskService.releaseTasks(config.getSpiderId());
					} catch (ServiceException e) {
						logger.error("release tasks failed", e);
						return;
					}
					
					try {
						tasks = taskService.tasksForCrawl(config.getSpiderId(), config.getTaskLimit());
					} catch (ServiceException e) {
						logger.error("get tasks for crawl failed", e);
						return;
					}
				}
				
				if (tasks.isEmpty()) {
					logger.info("no task for crawl");
					try {
						Thread.sleep(30000);
					} catch (InterruptedException e) {}
				} else {
					// 缓存模板
					TemplateService<UrlsTemplate> urlTemplateService = ServiceFactory.getUrlTemplateService();
					try {
						urlTemplateService.cacheForTasks(tasks, Collections.<Integer, Integer> emptyMap());
					} catch (ServiceException e) {
						logger.error("cache templates failed", e);
						break;
					}
					
					// 启动任务
					Calendar calendar = Calendar.getInstance();
					Date today = calendar.getTime();
					calendar.add(Calendar.DATE, -1);
					Date yesterday = calendar.getTime();
					int maxSiteTaskId = 0;
					for (SiteTask task : tasks) {
						if (task.getSiteTaskId() > maxSiteTaskId) {
							maxSiteTaskId = task.getSiteTaskId();
						}
						task.setTitle(ConstantsHome.FRONT_PAGE);
						if (TaskUtils.dependsOnUrlTemplate(task.getType())) {
							UrlsTemplate template = urlTemplateService.retrieveTemplate(task.getSiteTaskId());
							if (null == template) {
								logger.error("没有缓存模板 - " + task.getSiteTaskId());
								return;
							}

							String[] startUrls = template.getStartUrls();
							boolean foundStartUrl = false;
							if (null != startUrls && startUrls.length != 0) {
								for (String startUrl : startUrls) {
									task = cloneTask(task, foundStartUrl, startUrl);
									try {
										refreshTask(spider, task);
										foundStartUrl = true;
										temp.add(task);
//										logger.info("start - {}", startUrl);
//										spider.execute(task);
									} catch (Throwable e) {
										logger.warn("refresh task failed - " + task.getSiteTaskId(), e);
									}
								}
							}

							String[] startUrlPatterns = template.getStartUrlPatterns();
							if (null != startUrlPatterns && startUrlPatterns.length != 0) {
								for (String startUrlPattern : startUrlPatterns) {
									if (startUrlPattern.endsWith(ConstantsHome.MARK_URL_START_POSTPONE)) {
										startUrlPattern = startUrlPattern.substring(0, startUrlPattern.length()
												- ConstantsHome.MARK_URL_START_POSTPONE.length());
										String startUrl = new SimpleDateFormat(startUrlPattern).format(yesterday);
										task = cloneTask(task, foundStartUrl, startUrl);
										try {
											refreshTask(spider, task);
											foundStartUrl = true;
											temp.add(task);
//											logger.info("start - {}", startUrl);
//											spider.execute(task);
										} catch (Throwable e) {
											logger.info("refresh task failed - " + task.getSiteTaskId(), e);
										}
									}
									task = cloneTask(task, foundStartUrl,
											new SimpleDateFormat(startUrlPattern).format(today));
									try {
										refreshTask(spider, task);
									} catch (Exception e) {
										logger.warn("refresh task failed - " + task.getSiteTaskId(), e);
									}
									foundStartUrl = true;
									temp.add(task);
//									logger.info("start - {}", startUrl);
//									spider.execute(task);
								}
							}
							if (!foundStartUrl) {
								logger.warn("no startUrl found from template - {}", task.getSiteTaskId());
							}

						} else {
							temp.add(task);
//							logger.info("start - {}", task.getUrl());
//							spider.execute(task);
						}
					}
					tasks = null;
					
					requestBuffer.reset(maxSiteTaskId);
					naviSet.clear();
					for (SiteTask st : temp) {
						logger.info("start - {}", st.getUrl());
						if (!pageProcessor.isDuplicatedNavi(st.getUrl())) {
							requestBuffer.offer(st);
						}
					}
					temp.clear();
					pageProcessor.reset(maxSiteTaskId);
					
					// 处理任务结果
					spider.run();
					logger.info("missed requests = {}", requestBuffer.size());
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
				if (!Spider.inTestMode()) {
					ServiceFactory.getSiteTaskService().releaseTasks(config.getSpiderId());
				}
			} catch (ServiceException e) {
				logger.error("release tasks failed", e);
			} finally {
				spider.close();
			}
		}
	}

	private SiteTask cloneTask(SiteTask task, boolean foundStartUrl, String startUrl) throws LogicError {
		if (foundStartUrl) {
			try {
				task = task.clone();
			} catch (CloneNotSupportedException e) {
				throw new LogicError(e);
			}
		}
		task.setUrl(startUrl);
		return task;
	}

	public static void main(String[] args) {
		BasicSpiderLauncher.launch(new RedisBasedSpiderLauncher(new SiteTaskSpiderLauncher()), args);
	}
}
