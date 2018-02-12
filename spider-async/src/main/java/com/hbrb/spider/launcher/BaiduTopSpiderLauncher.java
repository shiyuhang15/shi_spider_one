package com.hbrb.spider.launcher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Consts;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.alibaba.fastjson.JSON;
import com.hbrb.json.JSArray;
import com.hbrb.json.JSException;
import com.hbrb.json.JSObject;
import com.hbrb.spider.ConstantsHome;
import com.hbrb.spider.downloader.sync.HtmlPageBuilder;
import com.hbrb.spider.downloader.sync.HttpClientDownloader;
import com.hbrb.spider.downloader.sync.JSObjectPageBuilder;
import com.hbrb.spider.downloader.sync.PageAdapter;
import com.hbrb.spider.model.RawResult;
import com.hbrb.spider.model.SpiderConfig;
import com.hbrb.spider.model.page.HtmlPage;
import com.hbrb.spider.model.page.JSObjectPage;
import com.hbrb.spider.model.task.GenericRequestTask;
import com.hbrb.util.ServiceUtils;

public class BaiduTopSpiderLauncher {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BaiduTopSpiderLauncher.class);
	public static class Entry {
		/**
		 * 排行
		 */
		private int rank;
		/**
		 * 关键词
		 */
		private String keyword;
		/**
		 * 搜索指数
		 */
		private int searches;
		/**
		 * 趋势
		 * 		1：上升；2：下降
		 */
		private int trend;
		/**
		 * 类型
		 * 		0：热门搜索；1：世说新词
		 */
		private int type;
		public Entry(int type, int rank, String keyword, int searches, int trend) {
			super();
			this.rank = rank;
			this.keyword = keyword;
			this.searches = searches;
			this.type = type;
			this.trend = trend;
			
		}
		public int getRank() {
			return rank;
		}
		public String getKeyword() {
			return keyword;
		}
		public int getSearches() {
			return searches;
		}
		public int getType() {
			return type;
		}
		public int getTrend() {
			return trend;
		}
	}

	public static void main(String[] args) throws IOException {
		// 获取服务器位置
		String baseUrl = ServiceUtils.getHttpServiceBase();
		File block = new File(ConstantsHome.USER_DIR + File.separatorChar + "block");
		GenericRequestTask baiduTopTask = new GenericRequestTask("http://top.baidu.com/category?c=12&fr=topindex");
		String singlelist = "http://top.baidu.com/region/singlelist";
		GenericRequestTask hebBaiduHotTask = new GenericRequestTask(GenericRequestTask.METHOD_POST, singlelist, 0);
		hebBaiduHotTask.setEntity(new StringEntity("boardid=2&divids[]=920", ContentType.APPLICATION_JSON));
		GenericRequestTask hebBaiduNewTask = new GenericRequestTask(GenericRequestTask.METHOD_POST, singlelist, 0);
		hebBaiduNewTask.setEntity(new StringEntity("boardid=396&divids[]=920", ContentType.APPLICATION_JSON));
		// 初始化downloader
		HttpClientDownloader downloader = new HttpClientDownloader(new SpiderConfig(), true, null, null);
		PageAdapter<GenericRequestTask, HtmlPage<GenericRequestTask>> htmlDownloader = new PageAdapter<>(downloader,
				new HtmlPageBuilder<GenericRequestTask>("gb2312"));
		PageAdapter<GenericRequestTask, JSObjectPage<GenericRequestTask>> jsonDownloader = new PageAdapter<>(downloader,
				new JSObjectPageBuilder<GenericRequestTask>(Consts.UTF_8.name()));
		try {
			// 开始采集
			List<Entry> entries = new ArrayList<>();
			do {
				entries.clear();
				// 百度风云榜
				logger.info("get - {}", baiduTopTask.getUrl());
				HtmlPage<GenericRequestTask> page = htmlDownloader.execute(baiduTopTask);
				if (null != page) {
					// 开始处理
					logger.info("process start");
					Document doc = page.getDocument();
					Elements ulEles = doc.select("ul.item-list");
					if (ulEles.size() != 2) {
						logger.warn("ulEles.size() != 2");
						return;
					}
					for (int i = 0; i < 2; i++) {
						Element ulEle = ulEles.get(i);
						Elements itemEles = ulEle.select(":root > li > div.item-hd");
						for (Element itemEle : itemEles) {
							Elements children = itemEle.children();
							if (children.size() < 3) {
								logger.warn("children.size() < 3");
								continue;
							}
							// 排名
							Element topNumEle = children.get(0);
							if (!topNumEle.className().startsWith("num-")) {
								logger.warn(".num-top not found");
								continue;
							}
							int rank;
							try {
								rank = Integer.parseInt(topNumEle.ownText());
							} catch (NumberFormatException e) {
								logger.warn("num-top", e);
								continue;
							}
							
							// 关键词
							Element listTitleEle = children.get(1);
							if (!listTitleEle.className().equals("list-title")) {
								logger.warn(".list-title not found");
								continue;
							}
							String keyword = listTitleEle.attr("title");
							if (keyword.isEmpty()) {
								logger.warn("title.isEmpty()");
								continue;
							}
							
							// 搜索指数
							Element searchNumEle = children.get(2);
							int trend = findTrend(searchNumEle);
							if (trend == 0) {
								searchNumEle = children.get(3);
								trend = findTrend(searchNumEle);
								if (trend == 0) {
									logger.warn("trend not found - {}", keyword);
									continue;
								}
							}
							int searches;
							try {
								searches = Integer.parseInt(searchNumEle.ownText());
							} catch (NumberFormatException e) {
								logger.warn("searches", e);
								continue;
							}
							
							entries.add(new Entry(i, rank, keyword, searches, trend));
						}
					}
					
					// 保存采集结果
					String body = JSON.toJSONString(entries);
					logger.info(body);
					//			logger.info(JSON.toJSONStringWithDateFormat(entries, ConstantsHome.PATTERN.DATEFORMAT_DEFAULT,
					//					SerializerFeature.PrettyFormat));
					GenericRequestTask req = new GenericRequestTask(GenericRequestTask.METHOD_PUT,
							baseUrl + "/data/top_baidu", 0);
					req.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
					logger.info("put top_baidu start");
					RawResult res = downloader.execute(req);
					if (null != res && res.getStatusCode() == HttpStatus.SC_OK) {
						logger.info("put top_baidu ok");
					} else {
						logger.warn("put top_baidu failed");
					}
				}
				
				entries.clear();
				try {
					Thread.sleep(20000);
				} catch (InterruptedException e) {}
				
				logger.info("post - top_baidu_hebei_hot");
				JSObjectPage<GenericRequestTask> jsonPage = jsonDownloader.execute(hebBaiduHotTask);
				if (null != jsonPage) {
					try {
						processHebBadiuPage(jsonPage, entries);
					} catch (JSException e) {
						logger.warn("河北百度热搜返回结果异常", e);
						return;
					}
				}
				try {
					Thread.sleep(20000);
				} catch (InterruptedException e) {}
				logger.info("post - top_baidu_hebei_new");
				jsonPage = jsonDownloader.execute(hebBaiduNewTask);
				if (null != jsonPage) {
					try {
						processHebBadiuPage(jsonPage, entries);
					} catch (JSException e) {
						logger.warn("河北百度新词返回结果异常", e);
						return;
					}
				}
				// 保存采集结果
				if (!entries.isEmpty()) {
					String body = JSON.toJSONString(entries);
					logger.info(body);
					GenericRequestTask req = new GenericRequestTask(GenericRequestTask.METHOD_PUT,
							baseUrl + "/data/top_baidu_hebei", 0);
					req.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
					logger.info("put top_baidu_hebei start");
					RawResult res = downloader.execute(req);
					if (null != res && res.getStatusCode() == HttpStatus.SC_OK) {
						logger.info("put top_baidu_hebei ok");
					} else {
						logger.warn("put top_baidu_hebei failed");
					}
				}
				
				try {
					Thread.sleep(20000);
				} catch (InterruptedException e) {}
			} while (!block.exists());
			
			logger.info("spider stop");
		} finally {
			try {
				downloader.close();
			} catch (IOException e) {
				logger.warn("爬虫关闭失败", e);
			}
		}
	}

	private static void processHebBadiuPage(JSObjectPage<GenericRequestTask> jsonPage, List<Entry> entries) {
		logger.info("process start");
		JSObject content = jsonPage.getContent();
		JSArray jsa = content.getNotNullJSObject("topWords").getNotNullJSArray("920");
		int length = jsa.length();
		for (int i = 0; i < length; i++) {
			JSObject topWord = jsa.getNotNullJSObject(i);
			entries.add(new Entry(0, i + 1, topWord.getNotNullString("keyword"), topWord.getNotNullInt("searches"),
					findHebTrend(topWord.getNotNullString("trend"))));
		}
		logger.info("process done");
	}

	private static int findTrend(Element searchNumEle) {
		String className = searchNumEle.className();
		switch (className) {
		case "icon-rise":
			return 3;
		case "icon-fair":
			return 2;
		case "icon-fall":
			return 1;
		default:
			return 0;
		}
	}
	
	private static int findHebTrend(String trend) {
		switch (trend) {
		case "rise":
			return 3;
		case "fair":
			return 2;
		case "fall":
			return 1;
		default:
			return 0;
		}
	}
}
