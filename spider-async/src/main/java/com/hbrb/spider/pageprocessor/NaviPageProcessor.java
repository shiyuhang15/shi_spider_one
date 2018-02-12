package com.hbrb.spider.pageprocessor;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.hbrb.exception.LogicError;
import com.hbrb.exception.ServiceException;
import com.hbrb.spider.ConstantsHome;
import com.hbrb.spider.Spider;
import com.hbrb.spider.exception.PageProcessException;
import com.hbrb.spider.model.Region;
import com.hbrb.spider.model.article.SourceType;
import com.hbrb.spider.model.page.HtmlPage;
import com.hbrb.spider.model.task.SiteTask;
import com.hbrb.spider.model.template.HOFPExtractRule;
import com.hbrb.spider.model.template.Rectangle;
import com.hbrb.spider.model.template.UrlsTemplate;
import com.hbrb.spider.service.NaviTaskService;
import com.hbrb.spider.service.ServiceFactory;
import com.hbrb.spider.service.TemplateService;
import com.hbrb.util.JedisUtils;
import com.hbrb.util.MD5;
import com.hbrb.util.ProcessUtils;
import com.hbrb.util.TaskUtils;

import redis.clients.jedis.Jedis;

public class NaviPageProcessor extends UrlTemplateServedPageProcessor<SiteTask> {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(NaviPageProcessor.class);
	private static final org.slf4j.Logger escapedUrlLogger = org.slf4j.LoggerFactory.getLogger("com.hbrb.spider.async.pageprocessor.NaviPageProcessor.escapedUrl");
	private final NaviTaskService naviTaskService = ServiceFactory.getNaviTaskService();
	private final TemplateService<UrlsTemplate> urlTemplateService = ServiceFactory.getUrlTemplateService();
	private final Set<String> naviSet;
	private AtomicIntegerArray naviCounts;
	private static final String CSS_MAP_AREA = "map > area[href]";
	
	public NaviPageProcessor(Set<String> naviSet) {
		this.naviSet = naviSet;
	}

	public void reset(int maxSiteTaskId){
		naviCounts = new AtomicIntegerArray(maxSiteTaskId);
	}

	public void process(HtmlPage<SiteTask> page) throws PageProcessException {
		// 获取所匹配的模板
		SiteTask currentTask = page.getRequestTask();
		int siteTaskId = currentTask.getSiteTaskId();
		UrlsTemplate matchedTemplate = null;
		if (TaskUtils.dependsOnUrlTemplate(currentTask.getType())) {
			matchedTemplate = urlTemplateService.retrieveTemplate(siteTaskId);
			if (null == matchedTemplate) {
				throw new LogicError("没有缓存模板 - " + siteTaskId);
			}
		}
		
		// 处理redirect
		String taskUrl = currentTask.getUrl();
		String taskDomain = ProcessUtils.extractDomain(taskUrl);
		if (null == taskDomain) {
			throw new LogicError("任务域名不可能为空 - " + taskUrl);
		}
		Set<String> startDomains = getStartDomains(taskDomain,
				matchedTemplate);
		String currentUrl = page.getDocument().location();
		if (redirectToAnother(taskUrl, currentUrl)) {
			// 此时currentUrl为跳转到的地址
			String currentDomain = ProcessUtils.extractDomain(currentUrl);
			if (null == currentDomain) {
				logger.warn("解析重定向域名失败 - {}", currentUrl);
				return;
			}
			
			if (!startDomains.contains(currentDomain)) {
				logger.info("redirect out - {}", currentUrl);
				return;
			}
			
			if (isDuplicatedNavi(currentUrl)) {
				logger.info("redirect duplicated - {}", currentUrl);
				return;
			}
			logger.info("redirect {} -> {}", taskUrl, currentUrl);
		}
		
		// 获取当前页面所有的链接和对应的名称
		Elements linkEles = page.getDocument().getElementsByTag("a");
		if (currentTask.getSourceType() == SourceType.PAPER) {
			Elements areaEles = page.getDocument().select(CSS_MAP_AREA);
			if (!areaEles.isEmpty()) {
				linkEles.addAll(areaEles);
			}
		}
		int size = linkEles.size();
		if (size == 0) {
			logger.info("no links - {}", currentUrl);
			return;
		}
		
		// 头版头条
		String taskTitle = currentTask.getTitle();
		String hofpUrl = null;
		if (ConstantsHome.FRONT_PAGE == taskTitle && matchedTemplate != null) {
			HOFPExtractRule[] hofps = matchedTemplate.getHofps();
			if (null != hofps) {
				for (HOFPExtractRule hofp : hofps) {
					if (null == hofp) {
						continue;
					}
					Rectangle limit = hofp.getLimit();
					String location = hofp.getLocation();
					if (null == location) {
						if (null == limit) {
							continue;
						} else {
							location = CSS_MAP_AREA;
						}
					}
					Elements hofpEles = page.getDocument().select(location);
					if (hofpEles.isEmpty()) {
						continue;
					}
					
					Element hofpEle = null;
					if (limit == null) {
						try {
							hofpEle = hofpEles.get(hofp.getIndex());
						} catch (java.lang.IndexOutOfBoundsException e) {
							logger.warn("HOFP index - " + currentUrl, e);
							continue;
						}
					} else {
						for (Element ele : hofpEles) {
							String coords = ele.attr("coords");
							if (null == coords) {
								continue;
							}
							String[] split = coords.split(",");
							float minX = Float.MAX_VALUE;
							float minY = Float.MAX_VALUE;
							try {
								for (int i = 0; i < split.length; i+=2) {
									float x = Float.parseFloat(split[i]);
									if (minX > x) {
										minX = x;
									}
								}
								for (int i = 0; i < split.length; i+=2) {
									float x = Float.parseFloat(split[i]);
									if (x - minX < 10) {
										float y = Float.parseFloat(split[i + 1]);
										if (minY > y) {
											minY = y;
										}
									}
								}
							} catch (NumberFormatException e) {
								logger.warn("HOFP coords" + currentUrl, e);
								continue;
							}
							if (limit.contains(minX, minY)) {
								hofpEle = ele;
								break;
							}
						}
						if (null == hofpEle) {
							logger.warn("HOFP coords not found - {}", currentUrl);
							continue;
						}
					}

					String url = ProcessUtils.absUrl(hofpEle, "href");
					if (null == url || url.isEmpty()) {
						continue;
					}
					hofpUrl = url;
					break;
				}
				if (hofpUrl == null) {
					logger.warn("HOFP not found - {}", currentUrl);
				}
			}
		}
		

		// 深度限制，模板中配的深度优先
		int bottom = -1;
		if (null != matchedTemplate) {
			bottom = matchedTemplate.getBottom();
		}
		if (bottom < 0) { // 没配深度的话使用默认的深度
			bottom = 2;
		}
		boolean reachBottom = false;
		int currentDeep = currentTask.getDeep();
		if (bottom >= 0 && currentDeep >= bottom) {
			reachBottom = true;
		}
		
		// 地域
		Region region = findRegion(matchedTemplate, currentUrl);

		// 遍历前的一些准备
		Calendar calendar = Calendar.getInstance();
		Date today = calendar.getTime();
		calendar.add(Calendar.DATE, -1);
		Date yesterday = calendar.getTime();

		// 开始遍历
		int targetCount = 0;
		int targetTotal = 0;
		int naviCount = 0;
		int naviTotal = 0;
		int sourceType = currentTask.getSourceType();
		int siteIndex = siteTaskId - 1;
		/*// 头版
		boolean isFrontPage = ConstantsHome.FRONT_PAGE == currentTask.getTitle();*/
		for (int i = 0; i < size; i++) {
			Element linkEle = linkEles.get(i);
			String url = ProcessUtils.absUrl(linkEle, "href");
			boolean isHofp = false;
			if (hofpUrl != null && hofpUrl.equals(url)) {
				isHofp = true;
			}
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
			
			String title = linkEle.attr("title").trim();
			if (title.isEmpty()) {
				title = linkEle.text().trim();
				if (title.isEmpty()) {
					Elements children = linkEle.children();
					if (children.size() == 1) {
						Element child = children.get(0);
						if (child.tagName().equals("img")) {
							title = child.attr("alt").trim();
						}
					}
				}
			}
			// 过滤
			if (ignoreUrl(title, url, domain, startDomains, matchedTemplate, sourceType)) {
				continue;
			}

			// target正则匹配
			int res = pushTarget(region, isHofp, page, url, title, matchedTemplate, today, yesterday);
			if (res != 0) { // 匹配了正则
				if (res == 1) { // 加入了队列
					targetCount++;
				}
				targetTotal++;
				continue;
			}
			
			// 按链接名称过滤
			if (null == matchedTemplate || !matchedTemplate.isDisableCommonIgnore()) {
				if (ProcessUtils.isUncorrelatedName(title)) {
					continue;
				}
			}
			
			// 达到深度或数量限制后不再增加导航
			if (reachBottom || naviCounts.get(siteIndex) > 10000) {
				continue;
			}
			
			// 标题过长
			if (title.length() > 50) {
				logger.warn("n/t? {} - {}", title, url);
				continue;
			}

			// 没有匹配到目标页且没有达到深度限制的话继续深入导航
			String naviInfo = null;
			if (null != matchedTemplate) {
				// 精确导航正则匹配
				Pattern[] preciseHelpUrlPatterns = matchedTemplate
						.getNaviUrlPatterns();
				if (null != preciseHelpUrlPatterns && preciseHelpUrlPatterns.length != 0) {
					for (Pattern helpUrlPattern : preciseHelpUrlPatterns) {
						if (helpUrlPattern.matcher(url).matches()) {
							naviInfo = "precise navi/{} {} - {}";
							break;
						}
					}
				}
				
				if (null == naviInfo) {
					// 导航正则匹配
					Pattern[] helpUrlPatterns = matchedTemplate
							.getFuzzyNaviUrlPatterns();
					if (null != helpUrlPatterns && helpUrlPatterns.length != 0) {
						for (Pattern helpUrlPattern : helpUrlPatterns) {
							if (helpUrlPattern.matcher(url).matches()) {
								naviInfo = "navi/{} {} - {}";
								break;
							}
						}
					}
				}
			}
			
			if (null == naviInfo && (null == matchedTemplate || matchedTemplate.isAutoExtractNaviAsSupplement())
					&& ProcessUtils.isNaviUrl(url, sourceType)) {
				naviInfo = "auto navi/{} {} - {}";
			}
			if (null == naviInfo) {
				escapedUrlLogger.info("{} - {}", url, title);
				continue;
			}
			
			// 过滤往期报刊
			if (sourceType == SourceType.PAPER) {
				String sdfPa = null;
				Matcher matcher = ProcessUtils.datePa.matcher(url);
				if (matcher.find()) {
					sdfPa = "yyyyMMdd";
				} else if ((matcher = ProcessUtils.datePa1.matcher(url)).find()) {
					sdfPa = "yyyyMd";
				}
				boolean urlExpired = true;
				if (null != sdfPa) {
					String datePart = matcher.group();
					datePart = datePart.replaceAll("\\D", "");
					SimpleDateFormat sdf = new SimpleDateFormat(sdfPa);
					if (sdf.format(today).equals(datePart)) {
						urlExpired = false;
					} else if (sdf.format(yesterday).equals(datePart)) {
						urlExpired = false;
					}
				}
				if (urlExpired) {
					continue;
				}
			}
			
			// 导航去重
			naviTotal++;
			if (isDuplicatedNavi(url)) {
				continue;
			}

			// 深度设置
			int deep = -1;
			if (null != matchedTemplate) {
				// 模板矫正规则优先
				Entry<Pattern, Integer>[] deepEntries = matchedTemplate.getDeepEntries();
				if (null != deepEntries && deepEntries.length != 0) {
					for (Entry<Pattern, Integer> entry : deepEntries) {
						if (entry.getKey().matcher(url).matches()) {
							deep = entry.getValue();
							break;
						}
					}
				}
			} else if (sourceType == SourceType.BBS){
				if (ProcessUtils.forumPa.matcher(url).find()) {
					deep = 1;
				}
			}
			// 默认矫正规则
			if (deep < 0) {
				deep = ProcessUtils.calculateDeep(url, domain);
			}
			// 都没有矫正
			if (deep < 0) {
				// 深度自然递增
				deep = currentDeep + 1;
			}

			// 加入队列
			SiteTask requestTask = new SiteTask(url);
			requestTask.setTitle(title);
			requestTask.setSiteTaskId(siteTaskId);
			requestTask.setDeep(deep);
			requestTask.setSourceType(sourceType);
			requestTask.setType(currentTask.getType());
			forward(requestTask);
			naviCounts.getAndAdd(siteIndex, 1);
			naviCount++;
			logger.info(naviInfo, deep, title, url);
		}
		
		if (targetCount != 0 && sourceType != SourceType.PAPER) {
			currentTask.setUrl(currentUrl);
			if (Spider.inTestMode()) {
				try {
					naviTaskService.createTask(currentTask);
				} catch (ServiceException e) {
					logger.warn("create navi task failed - " + currentUrl, e);
				}
			} else {
				String key = ConstantsHome.Redis.Key.NAVI_TASK_WORKED_TIME + siteTaskId;
				String currentMd5 = MD5.get(currentUrl);
				try (Jedis jedis = JedisUtils.createJedis()) {
					Boolean hexists = jedis.hexists(key, currentMd5);
					if (!hexists) {
						try {
							naviTaskService.createTask(currentTask);
							jedis.hset(key, currentMd5, String.valueOf(today.getTime()));
						} catch (ServiceException e) {
							logger.warn("create navi task failed - " + currentUrl, e);
						}
					}
				}
			}
		}

		logger.info("deep={}/{}, targetCount={}/{}, naviCount[{}]={}/{}/{} - {}", currentDeep, bottom, targetCount,
				targetTotal, siteTaskId, naviCount, naviTotal, naviCounts.get(siteIndex), currentUrl);
		if (targetCount > 0) {
			LOGGER_TARGET.info("{}\t{}", siteTaskId, targetCount);
		}
	}
	
	/**
	 * 判断是否跳转
	 * @param taskUrl	任务执行时访问的url
	 * @param currentUrl	结果返回shi
	 * @return
	 */
	private boolean redirectToAnother(String taskUrl, String currentUrl) {
		if (taskUrl.equals(currentUrl)) {
			return false;
		}
		
		int cl = currentUrl.length();
		int tl = taskUrl.length();
		int d = cl - tl;
		String longger;
		if (d == 1) {
			longger = currentUrl;
		} else if (d == -1) {
			longger = taskUrl;
		} else {
			return true;
		}
		
		int shorterLength = longger.length() - 1;
		if (longger.charAt(shorterLength) == '/') {
			for (int i = 0; i < shorterLength; i++) {
				if (currentUrl.charAt(i) != taskUrl.charAt(i)) {
					return true;
				}
			}
			return false;
		} else {
			return true;
		}
	}

	public boolean isDuplicatedNavi(String naviUrl) {
		if ('/' == naviUrl.charAt(naviUrl.length() - 1)) {
			naviUrl = naviUrl.substring(0, naviUrl.length() - 1);
		}
		return !this.naviSet.add(MD5.get(ProcessUtils.removePort(naviUrl)));
	}
}
