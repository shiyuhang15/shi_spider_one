package com.hbrb.spider.pageprocessor.weibo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.hbrb.json.JSException;
import com.hbrb.json.JSObject;
import com.hbrb.json.JSUtils;
import com.hbrb.spider.Spider;
import com.hbrb.spider.exception.PageProcessException;
import com.hbrb.spider.model.WeiboUser;
import com.hbrb.spider.model.article.WeiboArticle;
import com.hbrb.spider.model.page.PlainPage;
import com.hbrb.spider.model.task.HomeWeiboRequestTask;
import com.hbrb.spider.pipeline.FilePipeline;
import com.hbrb.spider.service.RegionService;
import com.hbrb.spider.service.ServiceFactory;
import com.hbrb.util.JedisUtils;
import com.hbrb.util.WeiBoUtils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

public class HomeWeiboPageProcessor extends WeiboPageProcessor<HomeWeiboRequestTask, PlainPage<HomeWeiboRequestTask>> {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(HomeWeiboPageProcessor.class);
	private static final org.slf4j.Logger articlesLogger = org.slf4j.LoggerFactory
			.getLogger("pageprocessor.ArticlesLogger");
	
	public static final int REQUEST_TYPE_START = 0;
	public static final int REQUEST_TYPE_FALLS = 1;
	public static final int REQUEST_TYPE_NAVI = 2;
	
	private static final String REDIS_KEY_PREFIX_MID = "mid_";
	
	private static final Pattern PA_PAGE_ID = Pattern
			.compile("\\$CONFIG\\['page_id'\\]='([^']+)';");
	private static final Pattern PA_ONICK = Pattern
			.compile("\\$CONFIG\\['onick'\\]='([^']+)';");
	private static final Pattern PA_DOMAIN = Pattern
			.compile("\\$CONFIG\\['domain'\\]='([^']+)';");
	private static final int EXPIRE = 173000;
	private static final int EXPIRE_M = 172800000;
	
	private static final Pattern PA_URL_IMAGE = Pattern.compile("%2F%2Fwx\\d\\.sinaimg\\.cn%2Fmw690%2F\\w{32}\\.((?i)jpg|gif|png|jpeg)");
	private static final Pattern PA_USERCARD = Pattern.compile("id=(\\d+)&");
	private final FilePipeline pipeline;
//	private final Map<String, Integer> areaCountMap;
	private final RegionService regionService = ServiceFactory.getRegionService();
	public HomeWeiboPageProcessor(FilePipeline pipeline) {
		this.pipeline = pipeline;
	}
	@Override
	public void process(PlainPage<HomeWeiboRequestTask> page) throws com.hbrb.spider.exception.PageProcessException {
		HomeWeiboRequestTask currentRequest = page.getRequestTask();
		int type = currentRequest.getType();
		switch (type) {
		case REQUEST_TYPE_START:
			processNaviPage(page, true);
			break;
		case REQUEST_TYPE_NAVI:
			processNaviPage(page, false);
			break;
		case REQUEST_TYPE_FALLS:
			processFallsPage(page);
			break;
		default:
			break;
		}
	}
	
	private void processFallsPage(PlainPage<HomeWeiboRequestTask> page) throws PageProcessException {
		// 获取feedList
		String rawText = page.getContent();
		Document doc;
		try {
			JSObject jp = JSUtils.createJSObject(rawText);
			String html = jp.getString("data");
			doc = Jsoup.parse(html);
		} catch (JSException e) {
			throw new PageProcessException("FallsPage解析JSON异常", e);
		}
		Element feedList;
		try {
			feedList = doc.child(0).child(1);
		} catch (IndexOutOfBoundsException e) {
			throw new PageProcessException("FallsPage获取feedList失败", e);
		}
		processFeedList(page, feedList, false);
	}
	
	private void processNaviPage(PlainPage<HomeWeiboRequestTask> page, boolean isFirstPage)
			throws PageProcessException {
		HomeWeiboRequestTask currentRequest = page.getRequestTask();
		WeiboUser user = currentRequest.getUser();
		String rawText = page.getContent();
		
		// 跳转到首页表示该微博账号可能不存在
		if (isFirstPage) {
			if (-1 != rawText.indexOf("<title>微博-随时随地发现新鲜事</title>")) {
				logger.info("task error not exist - {} @Lv {}", user.getId());
				return;
			}
		}
		
		if (-1 != rawText.indexOf("<title>404错误</title>")) {
			logger.info("task error 404 - {} @Lv {}", user.getId());
			return;
		}

		if (isFirstPage) {
			// 准备好一些导航需要的参数
			Matcher ma = PA_PAGE_ID.matcher(rawText);
			if (!ma.find()) {
				throw new PageProcessException("没有找到page_id");
			}
			currentRequest.setPage_id(ma.group(1));
			
			// 昵称
			ma.usePattern(PA_ONICK);
			if (!ma.find()) {
				throw new PageProcessException("没有找到onick");
			}
			user.setNickName(ma.group(1));
			
			// 导航需要的参数
			ma.usePattern(PA_DOMAIN);
			if (!ma.find()) {
				throw new PageProcessException("没有找到domain");
			}
			currentRequest.setDomain_op(ma.group(1));
		}
		
		// 获取feedList
		String script_head = "<script>FM.view({\"ns\":\"pl.content.homeFeed.index\",\"domid\":\"Pl_Official_MyProfileFeed__";
		int indexFMViewScript = rawText
				.indexOf(script_head);
		if (-1 == indexFMViewScript) {
			throw new PageProcessException("-1 == indexFMViewScript");
		}
		int beginIndex;
		if (isFirstPage) {
			// 导航需要的参数
			beginIndex = indexFMViewScript + script_head.length();
			int endIndexOfPlName = rawText.indexOf("\",\"", beginIndex);
			if (-1 == endIndexOfPlName) {
				throw new PageProcessException("-1 == endIndexOfPlName");
			}
			currentRequest.setPl_name(rawText.substring(beginIndex, endIndexOfPlName));
		}
		
		beginIndex = indexFMViewScript + 16;
		int endIndexOfJsonText = rawText.indexOf(")</script>", indexFMViewScript);
		if (-1 == endIndexOfJsonText) {
			throw new PageProcessException("-1 == endIndexOfJsonText");
		}

		Document doc;
		try {
			JSObject jp = JSUtils.createJSObject(rawText.substring(beginIndex, endIndexOfJsonText));
			doc = Jsoup.parse(jp.getNotNullString("html"));
		} catch (JSException e) {
			throw new PageProcessException("NaviPage解析JSON异常", e);
		}
		
		Element feedList;
		try {
			feedList = doc.child(0).child(1).child(0);
		} catch (IndexOutOfBoundsException e) {
			throw new PageProcessException("NaviPage获取feedList失败", e);
		}

		processFeedList(page, feedList, isFirstPage);
	}
	
	private void processFeedList(PlainPage<HomeWeiboRequestTask> page, Element feedList, boolean isFirstPage)
			throws PageProcessException {
		HomeWeiboRequestTask currentRequest = page.getRequestTask();
		WeiboUser user = currentRequest.getUser();
		List<WeiboArticle> articles = new ArrayList<>();
		boolean haveMore = true;
		Date now = new Date();
//		long now = System.currentTimeMillis();
		try (Jedis jedis = JedisUtils.createJedis()) {
			for (Element feedEle : feedList.children()) {
				String mid = feedEle.attr("mid").trim();
				if (mid.isEmpty()) {
					continue;
				}
				
				Elements wbDetailEles = feedEle.select(":root > div[node-type=feed_content] > div.WB_detail");
				if (1 != wbDetailEles.size()) {
					throw new PageProcessException("wbDetailEles.size()="
							+ wbDetailEles.size() + " - " + mid);
				}
				Element wbDetailEle = wbDetailEles.get(0);
				
				// 判断是否置顶
				boolean isTopFeed = false;
				Elements W_icon_feedpin = wbDetailEle.select(
						"span.W_icon_feedpin");
				if (!W_icon_feedpin.isEmpty()) {
					if (W_icon_feedpin.get(0).ownText().contains("置顶")) {
						isTopFeed = true;
					}
				}
				
				// 去重
				if (Spider.inTestMode()) {
					Long res = jedis.sadd(REDIS_KEY_WEIBO_MID, mid);
					if (res == 0) {
						if (isTopFeed) { 
							continue;
						}
						haveMore = false;
						break;
					} else if (res != 1) {
						throw new JedisException("sadd 结果异常 - " + res);
					}
					/*集群
					Long setnx = JedisUtils.jc.setnx(key, "");
					if (0 == setnx) {// 重复
						if (isTopFeed) { // 置顶
							continue;
						}
						// 重复且不是置顶，说明后面也都是采集过的，没必要继续了
						haveMore = false;
						break;
					}
					JedisUtils.jc.expire(key, EXPIRE);*/
				} else {
					String key = REDIS_KEY_PREFIX_MID + mid;
					Long setnx = jedis.setnx(key, "");
					if (0 == setnx) {
						if (isTopFeed) { 
							continue;
						}
						haveMore = false;
						break;
					}
					jedis.expire(key, EXPIRE);
				}
				
				// 时间
				Elements dateEles = wbDetailEle.select(":root > div.WB_from > a[date~=\\d{13,}]");
				if (dateEles.size() != 1) {
					throw new PageProcessException("dateEles.size()="
							+ dateEles.size() + " - " + mid);
				}
				long pubTime = Long.parseLong(dateEles.get(0).attr("date"));
				if (now.getTime() - pubTime > EXPIRE_M) { // 过期
					if (isTopFeed) {
						// 跳过过期的置顶微博
						continue;
					}
					// 过期且不是置顶，说明再往后也都是过期的，没必要继续了
					haveMore = false;
					break;
				}
				
				// url
				WeiboArticle article = createWeiboArticle(user, mid, now);
				article.setPubTime(new Date(pubTime));
				
				// 正文
				Elements contentEles = wbDetailEle.select(":root > div.WB_text");
				if (contentEles.size() != 1) {
					throw new PageProcessException("contentEles.size()="
							+ contentEles.size() + " - " + mid);
				}
				String content = contentEles.get(0).text();
				article.setContent(content);
				
				// 计数
				Elements handleUlEles = feedEle.select(":root > div.WB_feed_handle > div.WB_handle > ul:nth-child(1)");
				if (handleUlEles.size() == 1) {
					Elements liEles = handleUlEles.get(0).getElementsByTag("li");
					if (liEles.size() == 4) {
						// 转发数
						Elements emEles = liEles.get(1).select(":root > a[action-type=fl_forward] > span.pos > span[node-type=forward_btn_text] > span:nth-child(1) > em");
						if (emEles.size() == 2) {
							String emText = emEles.get(1).text().trim();
							try {
								article.setRepostsCount(Integer.parseInt(emText));
							} catch (NumberFormatException e) {}
						} else {
							throw new PageProcessException("forward em size = " + emEles.size() + " - " + mid);
						}
						// 评论数
						emEles = liEles.get(2).select(":root > a[action-type=fl_comment] > span.pos > span[node-type=comment_btn_text] > span:nth-child(1) > em");
						if (emEles.size() == 2) {
							String emText = emEles.get(1).text().trim();
							try {
								article.setCommentsCount(Integer.parseInt(emText));
							} catch (NumberFormatException e) {}
						} else {
							throw new PageProcessException("comment em size = " + emEles.size() + " - " + mid);
						}
						// 点赞数
						emEles = liEles.get(3).select(":root > a[action-type=fl_like] > span.pos > span:nth-child(1) > span[node-type=like_status] > em");
						if (emEles.size() == 2) {
							String emText = emEles.get(1).text().trim();
							try {
								article.setAttitudesCount(Integer.parseInt(emText));
							} catch (NumberFormatException e) {}
						} else {
							throw new PageProcessException("like em size = " + emEles.size() + " - " + mid);
						}
					} else {
						throw new PageProcessException("handleUlLiEles.size() == " + liEles.size() + " - " + mid);
					}
				} else {
					throw new PageProcessException("handleUlEles.size() == " + handleUlEles.size() + " - " + mid);
				}
				
				// 图片
				Elements ulEles = wbDetailEle.select(":root > div.WB_media_wrap > div.media_box > ul[action-data]");
				if (ulEles.size() == 1) {
					List<String> imgSrcs = new ArrayList<>();
					String actionData = ulEles.get(0).attr("action-data").trim();
					Matcher ma = PA_URL_IMAGE.matcher(actionData);
					while (ma.find()) {
						imgSrcs.add("http:" + ma.group().replace("%2F", "/"));
					}
					if (imgSrcs.isEmpty()) {
						logger.warn("no img in action-data - {}", actionData);
					} else {
						article.setImgSrcs(imgSrcs);
					}
				}
				
				// 转发部分
				String omid = feedEle.attr("omid");
				if (!omid.isEmpty()) {
					Elements wbExpandEles = wbDetailEle.select(":root > div.WB_feed_expand > div.WB_expand");
					if (wbExpandEles.size() != 1) {
						throw new PageProcessException("wbExpandEles.size() == " + wbExpandEles.size() + " - " + mid);
					}
					Element wbExpandEle = wbExpandEles.get(0);
					Elements divEles = wbExpandEle.getElementsByTag("div");
					if (divEles.size() > 1) {
						Elements originNickEles = wbExpandEles.select(":root > div.WB_info > a[node-type=feed_list_originNick]");
						String originNick;
						long ouid;
						if (originNickEles.size() == 1) {
							Element originNickEle = originNickEles.get(0);
							// 昵称
							originNick = originNickEle.attr("nick-name").trim();
							if (originNick.isEmpty()) {
								throw new PageProcessException("feed_list_originNick@nick-name not found - " + mid);
							}
							// uid
							String usercard = originNickEle.attr("usercard");
							Matcher ma = PA_USERCARD.matcher(usercard);
							if (!ma.find()) {
								throw new PageProcessException("feed_list_originNick@usercard not found - " + mid);
							}
							ouid = Long.parseLong(ma.group(1));
						} else {
							throw new PageProcessException("feed_list_originNick not found - " + mid);
						}
						
						WeiboArticle oriArticle = new WeiboArticle(null);
						oriArticle.setUrl(WeiBoUtils.getReference(ouid, omid));
						article.setOrigin(oriArticle);
						oriArticle.setAuthors(originNick);
						
						// 内容
						Elements expandContentEles = wbExpandEles.select(":root > div.WB_text");
						if (expandContentEles.size() == 1) {
							oriArticle.setContent(expandContentEles.get(0).text());
						} else {
							throw new PageProcessException("WB_expand > WB_text not found - " + mid);
						}
						
						// 时间
						Elements odateEles = wbExpandEles.select(
								":root > div.WB_func > div.WB_from > a[date~=\\d{13,}]");
						if (odateEles.size() != 1) {
							throw new PageProcessException("odateEles.size() = " + dateEles.size() + " - " + mid);
						}
						oriArticle.setPubTime(new Date(Long.parseLong(odateEles.get(0).attr("date"))));
						
						// 计数
						Elements oemEles = wbExpandEles.select(
								":root > div.WB_func > div.WB_handle > ul:nth-child(1) > li > span.line > a.S_txt2 > span > em");
						if (oemEles.size() == 6) {
							// 转发数
							String emText = oemEles.get(1).text().trim();
							try {
								oriArticle.setRepostsCount(Integer.parseInt(emText));
							} catch (NumberFormatException e) {}
							// 评论数
							emText = oemEles.get(3).text().trim();
							try {
								oriArticle.setCommentsCount(Integer.parseInt(emText));
							} catch (NumberFormatException e) {}
							// 点赞数
							emText = oemEles.get(5).text().trim();
							try {
								oriArticle.setAttitudesCount(Integer.parseInt(emText));
							} catch (NumberFormatException e) {}
						} else {
							throw new PageProcessException("oemEles.size() == " + oemEles.size() + " - " + mid);
						}
						
						// 图片
						Elements actionDataUlEles = wbExpandEles.select(
								":root > div[node-type=feed_list_media_prev] > div.WB_media_wrap > div.media_box > ul[action-data]");
						if (actionDataUlEles.size() == 1) {
							List<String> imgSrcs = new ArrayList<>();
							String actionData = actionDataUlEles.get(0).attr("action-data").trim();
							Matcher ma = PA_URL_IMAGE.matcher(actionData);
							while (ma.find()) {
								imgSrcs.add("http:" + ma.group().replace("%2F", "/"));
							}
							if (imgSrcs.isEmpty()) {
								logger.warn("no img in action-data - {}", actionData);
							} else {
								oriArticle.setImgSrcs(imgSrcs);
							}
						}
					}
				}
				
				articles.add(article);
				if (Spider.inTestMode()) {
					articlesLogger.info(article.toString());
				}
			}
		}
		
		if (articles.isEmpty()) {
			return;
		}

		for (WeiboArticle weiboArticle : articles) {
			if (pipeline.pipe(true, 0, weiboArticle)) {
				this.regionService.countRegion(weiboArticle);
			}
		}
		
		if (!haveMore) {
			return;
		}
		
		// 导航
		String naviUrl = null;
		int pagebar = currentRequest.getPagebar();
		int pagenum = currentRequest.getPage_num();
		String page_id = currentRequest.getPage_id();
		String domain = currentRequest.getDomain_op();
		String plName = currentRequest.getPl_name();

		int requestType;
		if (pagebar < 1) {
			requestType = REQUEST_TYPE_FALLS;
			pagebar++;
			if (pagenum == 1) {
				naviUrl = "http://weibo.com/p/aj/v6/mblog/mbloglist?ajwvr=6&domain="
						+ domain
						+ "&is_all=1&pagebar="
						+ pagebar
						+ "&pl_name=Pl_Official_MyProfileFeed__"
						+ plName
						+ "&id="
						+ page_id
						+ "&script_uri=/u/"
						+ user.getId()
						+ "&feed_type=0&page=1&pre_page=1&domain_op="
						+ domain
						+ "&__rnd=" + System.currentTimeMillis();
			} else {
				naviUrl = "http://weibo.com/p/aj/v6/mblog/mbloglist?ajwvr=6&domain="
						+ domain
						+ "&is_search=0&visible=0&is_all=1&is_tag=0&profile_ftype=1&page="
						+ pagenum
						+ "&pagebar="
						+ pagebar
						+ "&pl_name=Pl_Official_MyProfileFeed__"
						+ plName
						+ "&id="
						+ page_id
						+ "&script_uri=/u/"
						+ user.getId()
						+ "&feed_type=0&pre_page="
						+ pagenum
						+ "&domain_op="
						+ domain + "&__rnd=" + System.currentTimeMillis();
			}
		} else {
			requestType = REQUEST_TYPE_NAVI;
			pagebar = -1;
			pagenum++;
			naviUrl = "http://weibo.com/u/"
					+ user.getId()
					+ "?pids=Pl_Official_MyProfileFeed__"
					+ plName
					+ "&is_search=0&visible=0&is_all=1&is_tag=0&profile_ftype=1&page="
					+ pagenum + "#feedtop";
		}

		HomeWeiboRequestTask nextRequest = new HomeWeiboRequestTask(naviUrl, requestType);
		nextRequest.setHeaders(currentRequest.getHeaders());
		nextRequest.setUser(user);
		nextRequest.setPagebar(pagebar);
		nextRequest.setPage_num(pagenum);
		nextRequest.setPage_id(page_id);
		nextRequest.setDomain_op(domain);
		nextRequest.setPl_name(plName);
		getSpider().addTask(nextRequest);
		logger.info("push navi - {}", naviUrl);
	}
}