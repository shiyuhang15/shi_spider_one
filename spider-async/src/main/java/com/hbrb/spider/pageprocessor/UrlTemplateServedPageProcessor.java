package com.hbrb.spider.pageprocessor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hbrb.exception.LogicError;
import com.hbrb.exception.ServiceException;
import com.hbrb.spider.Spider;
import com.hbrb.spider.model.Region;
import com.hbrb.spider.model.article.SourceType;
import com.hbrb.spider.model.page.HtmlPage;
import com.hbrb.spider.model.task.TemplateTask;
import com.hbrb.spider.model.task.TargetTask;
import com.hbrb.spider.model.template.TargetUrl;
import com.hbrb.spider.model.template.UrlsTemplate;
import com.hbrb.spider.scheduler.Scheduler;
import com.hbrb.spider.service.ServiceFactory;
import com.hbrb.spider.service.TargetTaskService;
import com.hbrb.util.ProcessUtils;

public abstract class UrlTemplateServedPageProcessor<T extends TemplateTask> extends HtmlPageProcessor<T> {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(UrlTemplateServedPageProcessor.class);
	private final TargetTaskService targetTaskService;
	// 站点日报统计日志
	protected final org.slf4j.Logger LOGGER_TARGET = org.slf4j.LoggerFactory
			.getLogger("com.cmcc.yuqing.collector.statistics.target");

	public UrlTemplateServedPageProcessor() {
//		this.templateService = ServiceFactory.getSiteTemplateService();
		this.targetTaskService = ServiceFactory.getTargetTaskService();
	}
	
	private int pushTargetTask(TargetTask targetTask) {
		try {
			if (targetTaskService.createTask(targetTask) == 1) {
				return Scheduler.PUSH_SUCCESS;
			} else {
				return Scheduler.PUSH_FAILED;
			}
		} catch (ServiceException e) {
			logger.warn("target task push failed", e);
			return Scheduler.PUSH_FAILED;
		}
	}
	
	protected Region findRegion(UrlsTemplate matchedTemplate, String url) {
		if (null == matchedTemplate) {
			return null;
		}
		Entry<String,Region>[] entries = matchedTemplate.getRegionUrlPrefixes();
		if (null == entries) {
			return null;
		}
		for (Entry<String, Region> entry : entries) {
			String prefix = entry.getKey();
			if (url.equals(prefix) || url.startsWith(prefix + '/') || url.startsWith(prefix + '?')) {
				return entry.getValue();
			}
		}
		return null;
	}

	/**
	 * 
	 * @param url
	 * @param matchedTemplate
	 * @param jedis
	 * @param pageExpired
	 * @param dates
	 * @return 0:不匹配；1:成功；2:过期了；3:重复了；4:限制了；5:失败了
	 */
	protected int pushTarget(Region region, boolean isHofp, HtmlPage<T> page, String url, String title, UrlsTemplate matchedTemplate, Date... dates) {
		// 模板判断
		if (null != matchedTemplate) {
			TargetUrl[] targetUrls = matchedTemplate.getTargetUrls();
			if (null != targetUrls && targetUrls.length != 0) {
				boolean autoExtractTargetAsSupplement = matchedTemplate.isAutoExtractTargetAsSupplement();
				// 目标匹配
				for (TargetUrl targerUrl : targetUrls) {
					Matcher ma = targerUrl.getPattern().matcher(url);
					if (ma.matches()) {
						// 过期判断
						if (!ProcessUtils.judgeUrlExpired(url, targerUrl, dates)) {
							// 去重
							String key;
							try {
								key = ma.group("key");
							} catch (java.lang.IllegalArgumentException e) {
								key = ProcessUtils.removePort(url);
							}
							
							if (isMD5Duplicate(key)) {
								return 3;
							}
							
							return pushWhenNoDuplicate(region, isHofp, page, url, title);
						} else {
							return 2;
						}
					}
				}
				if (!autoExtractTargetAsSupplement) {
					return 0;
				}
			}
		}
		
		// 自动化判断
		int sourceType = page.getRequestTask().getSourceType();
		// 论坛
		if (SourceType.BBS == sourceType) {
			Matcher matcher = ProcessUtils.threadPa.matcher(url);
			if (matcher.find()) {
				if (isMD5Duplicate(ProcessUtils.removePort(url))) {
					return 3;
				}
				return pushWhenNoDuplicate(region, isHofp, page, url, title);
			} else {
				return 0;
			}
		}
		
		// 报刊
		if (SourceType.PAPER == sourceType) {
			if (!url.contains("/content_")) {
				return 0;
			}
		}
		
		// 新闻
		if (ProcessUtils.containNaviKeyword(url.toLowerCase())) {
			return 0;
		}
		
		String sdfPa = null;
		Matcher matcher = ProcessUtils.datePa.matcher(url);
		if (matcher.find()) {
			sdfPa = "yyyyMMdd";
		} else if ((matcher = ProcessUtils.datePa1.matcher(url)).find()) {
			sdfPa = "yyyyMd";
		}
		if (null != sdfPa) {
			// 过期判断
			boolean urlExpired = true;
			// FIXME test
			if (Spider.judgeExpired) {
				String datePart = matcher.group();
				datePart = datePart.replaceAll("\\D", "");
				SimpleDateFormat sdf = new SimpleDateFormat(sdfPa);
				for (Date date : dates) {
					if (sdf.format(date).equals(datePart)) {
						urlExpired = false;
						break;
					}
				}
			} else {
				urlExpired = false;
			}
			if (!urlExpired) {
				// 去重
				if (isMD5Duplicate(ProcessUtils.removePort(url))) {
					return 3;
				}
				return pushWhenNoDuplicate(region, isHofp, page, url, title);
			} else {
				return 2;
			}
		} else {
			return 0;
		}
	}

	protected int pushWhenNoDuplicate(Region region, boolean isHofp, HtmlPage<T> page, String url, String title) {
		// 创建目标任务
		TargetTask targetTask = new TargetTask(url);
		targetTask.setHofp(isHofp);
		if (null != title && !title.isEmpty() && title.length() <= 20) {
			targetTask.setTitle(title);
		}
		TemplateTask requestTask = page.getRequestTask();
		targetTask.setSiteTaskId(requestTask.getSiteTaskId());
		targetTask.setSourceType(requestTask.getSourceType());
		targetTask.setType(requestTask.getType());
		targetTask.setRegion(region);
		
		switch (pushTargetTask(targetTask)) {
		case Scheduler.PUSH_SUCCESS:
			if (Spider.inTestMode()) {
				logger.info("push target - {} - {}", url, title);
			} else {
				logger.info("push target - {}", url);
			}
			return 1;
		case Scheduler.PUSH_FAILED:
			return 5;
		case Scheduler.PUSH_LIMITED:
			return 4;
		default:
			throw new LogicError("不可能出现的push结果");
		}
	}

	protected boolean ignoreUrl(String title, String url, String domain, Set<String> startDomains, UrlsTemplate matchedTemplate, int sourceType){
		// 过滤与startDomain不符的链接
		if (null != domain && null != startDomains) {
			boolean ignore = true;		
			for (String startHost : startDomains) {
				if (domain.equals(startHost)) {
					ignore = false;
					break;
				}
			}
			if (ignore) {
				return true;
			}
		}

		// 过滤非网页链接(须在去锚点后)
		if (!ProcessUtils.judgePageLink(url)) {
			return true;
		}
		
		// 模板配置的过滤前缀
		if (null != matchedTemplate) {
			String[] ignoreUrlPrefixes = matchedTemplate
					.getIgnoreUrlPrefixes();
			if (null != ignoreUrlPrefixes && ignoreUrlPrefixes.length != 0) {
				for (String ignoreUrlPrefix : ignoreUrlPrefixes) {
					if (url.equals(ignoreUrlPrefix)) {
						return true;
					}
					if (url.startsWith(ignoreUrlPrefix + '/') || url.startsWith(ignoreUrlPrefix + '?')) {
						return  true;
					}
				}
			}
		}
		
		// 模板配置的过滤正则
		if (null != matchedTemplate) {
			Pattern[] ignoreUrlPatterns = matchedTemplate
					.getIgnoreUrlPatterns();
			if (null != ignoreUrlPatterns && ignoreUrlPatterns.length != 0) {
				for (Pattern ignoreUrlPattern : ignoreUrlPatterns) {
					if (ignoreUrlPattern.matcher(url).matches()) {
						return true;
					}
				}
			}
		}
		
		// 通用过滤
		if (null == matchedTemplate || !matchedTemplate.isDisableCommonIgnore()) {
			if (ProcessUtils.commonIgnore(title, url, sourceType)) {
				return true;
			}
		}
		
		return false;
	}

	protected Set<String> getStartDomains(String currentDomain,
			UrlsTemplate matchedTemplate) {
		Set<String> startDomains;
		String[] startUrls = null == matchedTemplate ? null
				: matchedTemplate.getStartUrls();
		if (null == startUrls || startUrls.length == 0) {
			startDomains = new HashSet<String>(1);
			startDomains.add(currentDomain);
		} else {
			startDomains = new HashSet<String>(startUrls.length);
			for (String startUrl : startUrls) {
				String startDomain = ProcessUtils.extractDomain(startUrl);
				if (null != startDomain) {
					startDomains.add(startDomain);
				} else {
					throw new LogicError("getStartDomains failed - " + startUrl);
				}
			}
		}
		return startDomains;
	}
}
