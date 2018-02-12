package com.hbrb.spider.pageprocessor;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.hbrb.exception.LogicError;
import com.hbrb.exception.ServiceException;
import com.hbrb.spider.ConstantsHome;
import com.hbrb.spider.exception.PageProcessException;
import com.hbrb.spider.model.Region;
import com.hbrb.spider.model.page.HtmlPage;
import com.hbrb.spider.model.task.NaviTask;
import com.hbrb.spider.model.template.UrlsTemplate;
import com.hbrb.spider.service.NaviTaskService;
import com.hbrb.spider.service.ServiceFactory;
import com.hbrb.spider.service.TemplateService;
import com.hbrb.util.JedisUtils;
import com.hbrb.util.MD5;
import com.hbrb.util.ProcessUtils;
import com.hbrb.util.TaskUtils;

import redis.clients.jedis.Jedis;

public class EfficientNaviPageProcessor extends UrlTemplateServedPageProcessor<NaviTask> {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EfficientNaviPageProcessor.class);
	private final TemplateService<UrlsTemplate> urlTemplateService = ServiceFactory.getUrlTemplateService();
	private final Pattern PA_TITLE_LONG =  Pattern.compile("[一-龥]{5,}");
	private final Pattern PA_URL_TARGET =  Pattern.compile("https?://[^?/]+[?/](.+)");
	private final NaviTaskService naviTaskService = ServiceFactory.getNaviTaskService();
	@Override
	public void process(HtmlPage<NaviTask> page) throws PageProcessException {
		NaviTask currentTask = page.getRequestTask();
		String taskUrl = currentTask.getUrl();
		
		// 删除redirect的导航
		int siteTaskId = currentTask.getSiteTaskId();
		String currentUrl = page.getDocument().location();
		if (currentUrl != taskUrl) {
			deleteTask(currentTask.getId(), siteTaskId, taskUrl);
			logger.info("redirect {} -> {}", taskUrl, currentUrl);
			return;
		}
		
		int targetCount = 0;
		int targetTotal = 0;
		Calendar calendar = Calendar.getInstance();
		Date today = calendar.getTime();
		
		// 获取当前页面所有的链接和对应的标题
		Elements linkEles = page.getDocument().getElementsByTag("a");
		int size = linkEles.size();
		if (size != 0) {
			// 获取所匹配的模板
			UrlsTemplate matchedTemplate = null;
			if (TaskUtils.dependsOnUrlTemplate(currentTask.getType())) {
				matchedTemplate = urlTemplateService.retrieveTemplate(siteTaskId);
				if (null == matchedTemplate) {
					throw new LogicError("没有缓存模板 - " + siteTaskId);
				}
			}
			
			Region region = findRegion(matchedTemplate, currentUrl);
			
			// 遍历前的准备
			String currentDomain = ProcessUtils.extractDomain(currentUrl);
			if (null == currentDomain) {
				throw new LogicError("任务域名不可能为空 - " + currentUrl);
			}
			Set<String> startDomains = getStartDomains(currentDomain, matchedTemplate);
			calendar.add(Calendar.DATE, -1);
			Date yesterday = calendar.getTime();
			
			// 开始遍历
			int sourceType = currentTask.getSourceType();
			// 头版
			boolean isFrontPage = ConstantsHome.FRONT_PAGE == currentTask.getName();
			for (int i = 0; i < size; i++) {
				Element linkEle = linkEles.get(i);
				String url = ProcessUtils.absUrl(linkEle, "href");
				if (url.isEmpty() || !url.startsWith("http")) {
					continue;
				}
				if (ProcessUtils.containsIllegalCharacter(url)) {
					continue;
				}
				
				// 处理URL
				try {
					url = ProcessUtils.processUrl(url);
				} catch (IOException e) {
					logger.warn("process url fail - " + url, e);
					continue;
				}
				
				// 提取域名
				String domain = ProcessUtils.extractDomain(url);
				if (null == domain) {
					logger.warn("extract domain failed - {}", url);
					continue;
				}
				
				String title = linkEle.text().trim();
				// 过滤
				if (ignoreUrl(title, url, domain, startDomains, matchedTemplate, sourceType)) {
					continue;
				}
				
				int res = 0;
				if (currentTask.isSingle()) {
					// 单独采集首页
					// 链接名4个汉字以下的不算目标
					if (!PA_TITLE_LONG.matcher(title).find()) {
						continue;
					}
					// 不含数字的不算目标
					Matcher targetMa = PA_URL_TARGET.matcher(url);
					if (!targetMa.matches()) {
						continue;
					}
					String path = targetMa.group(1);
					char[] ca = path.toCharArray();
					boolean containsDigit = false;
					for (char c : ca) {
						if (c > '0' && c < '9') {
							containsDigit = true;
							break;
						}
					}
					if (!containsDigit) {
						continue;
					}
					
					// 去重
					if (isMD5Duplicate(ProcessUtils.removePort(url))) {
						targetTotal++;
						continue;
					}
					
					res = pushWhenNoDuplicate(region, isFrontPage, page, url, title);
				} else {
					// 目标页匹配
					res = pushTarget(region, isFrontPage, page, url, title, matchedTemplate, today, yesterday);
				}
				
				if (res != 0) { // 匹配了正则
					if (res == 1) { // 加入了队列
						targetCount++;
					}
					targetTotal++;
				}
			}
		}

		long taskId = currentTask.getId();
		long workedTime = currentTask.getWorkedTime();
		if (workedTime > 0) {
			if (targetCount == 0) {
				if (today.getTime() - workedTime > ConstantsHome.EFFECTIVE_PERIOD_NAVI) {// 好久没更新
					deleteTask(taskId, siteTaskId, taskUrl);
					logger.info("no target since {} - {}", workedTime, currentUrl);
					return;
				}
			} else {
				String naviUrlKey = ConstantsHome.Redis.Key.NAVI_TASK_WORKED_TIME + siteTaskId;
				String currentMd5 = MD5.get(currentUrl);
				try (Jedis jedis = JedisUtils.createJedis()) {
					String value = jedis.hget(naviUrlKey, currentMd5);
					if (null == value || (today.getTime()
							- Long.parseLong(value) > ConstantsHome.INTERVAL_UPDATE_TIME_WORKED)) {
						try {
							naviTaskService.updateWorkedTimeById(taskId);
							jedis.hset(naviUrlKey, currentMd5, String.valueOf(today.getTime()));
						} catch (ServiceException e) {
							logger.warn("update worked time failed - " + currentUrl, e);
						}
					}
				}
			}
		}

		// 此处日志用作导航任务采集频率调整依据
		logger.info("taskId={}, targetCount={}/{} - {}", taskId, targetCount,
				targetTotal, currentUrl);
		if (targetCount > 0 && siteTaskId > 0) {
			// 此处日志用于站点日报统计
			LOGGER_TARGET.info("{}\t{}", siteTaskId, targetCount);
		}
	}
	
	private void deleteTask(long taskId, long siteTaskId, String taskUrl) throws PageProcessException {
		try {
			this.naviTaskService.deleteTask(taskId);
			try (Jedis jedis = JedisUtils.createJedis()) {
				jedis.hdel(ConstantsHome.Redis.Key.NAVI_TASK_WORKED_TIME + siteTaskId, MD5.get(taskUrl));
			}
		} catch (ServiceException e) {
			throw new PageProcessException(
					"delete task[" + taskId + "] failed", e);
		}
	}
}
