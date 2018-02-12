package com.hbrb.spider.launcher;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.client.CookieStore;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.message.BasicHeader;

import com.hbrb.json.JSArray;
import com.hbrb.json.JSObject;
import com.hbrb.json.JSUtils;
import com.hbrb.spider.ConstantsHome;
import com.hbrb.spider.downloader.sync.HttpClientDownloader;
import com.hbrb.spider.model.RawResult;
import com.hbrb.spider.model.SpiderConfig;
import com.hbrb.spider.model.task.GenericRequestTask;

public class WeixinSpiderLauncher {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WeixinSpiderLauncher.class);
	public static void main(String[] args) throws IOException {
		List<Header> defaultHeaders = new ArrayList<>(9);
		defaultHeaders.add(new BasicHeader("Cache-Control", "max-age=0"));
		defaultHeaders.add(new BasicHeader("Upgrade-Insecure-Requests", "1"));
		defaultHeaders.add(new BasicHeader("x-wechat-uin", "MTAyNjgzOTgxMg%3D%3D"));
		defaultHeaders.add(new BasicHeader("x-wechat-key", "e346578cd6b6d65b5367593545f933d146576b6084eb78025165e3b18d9b534c7b6bfe8e88f420f9829c86fbd7f3de5864cc5290f7fa4f5094a6bb0256f550ff2d65bb3495dbc9cc154ec0ecf347d012"));
		defaultHeaders.add(new BasicHeader("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/wxpic,image/sharpp,*/*;q=0.8"));
		defaultHeaders.add(new BasicHeader("Accept-Language", "zh-CN,en-US;q=0.8"));
		defaultHeaders.add(new BasicHeader("Q-UA2", "QV=3&PL=ADR&PR=WX&PP=com.tencent.mm&PPVN=6.5.22&TBSVC=43602&CO=BK&COVC=043613&PB=GE&VE=GA&DE=PHONE&CHID=0&LCID=9422&MO= FRD-AL00 &RL=1080*1794&OS=7.0&API=24"));
		defaultHeaders.add(new BasicHeader("Q-GUID", "569c3928bbcdbc36e5015398122188cb"));
		defaultHeaders.add(new BasicHeader("Q-Auth", "31045b957cf33acf31e40be2f3e71c5217597676a9729f1b"));
		HttpClientContext c = HttpClientContext.create();
		Random random = new Random();
		String pass_ticket = URLEncoder.encode("dUQCW0idqAuc01IsihHZPNan0Am4HDHxIcYL5DyfuMb1OxX6fGe7NeqOyHbXJYX/", Consts.UTF_8.name()) ;
		try (HttpClientDownloader downloader = new HttpClientDownloader(new SpiderConfig(), false,
				"Mozilla/5.0 (Linux; Android 7.0; FRD-AL00 Build/HUAWEIFRD-AL00; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/53.0.2785.49 Mobile MQQBrowser/6.2 TBS/043613 Safari/537.36 MicroMessenger/6.5.22.1160 NetType/WIFI Language/zh_CN",
				defaultHeaders)) {
//			RequestTask req = new RequestTask(
//					"https://mp.weixin.qq.com/mp/profile_ext?action=home&__biz=MzAwNTgxNDcwNA==&scene=124#wechat_redirect"); // 360娱乐
			File block = new File(ConstantsHome.USER_DIR + File.separatorChar + "block");
			for (;;) {
				GenericRequestTask req0 = new GenericRequestTask(
						"https://mp.weixin.qq.com/mp/getmasssendmsg?__biz=MjM5MjAxNDM4MA==&devicetype=android-24&version=26051633&lang=zh_CN&nettype=WIFI&ascene=3&pass_ticket="
								+ pass_ticket + "&wx_header=1"); // 人民日报
				RawResult rawResult = downloader.execute(req0, c);
				if (null == rawResult) {
					continue;
				}
				/*CookieStore cookieStore = c.getCookieStore();
				if (null != cookieStore) {
					for (Cookie cookie : cookieStore.getCookies()) {
						logger.info(cookie.toString());
						if ("pass_ticket".equalsIgnoreCase(cookie.getName())) {
							pass_ticket = cookie.getValue();
						}
					}
				}*/
				/*RequestTask req = new RequestTask(
						"https://mp.weixin.qq.com/mp/profile_ext?action=home&__biz=MjM5MjAxNDM4MA==&scene=124&devicetype=android-24&version=26051633&lang=zh_CN&nettype=WIFI&a8scene=3&pass_ticket="
								+ pass_ticket + "&wx_header=1"); // 人民日报
				rawResult = downloader.doGet(req, c);
				if (null == rawResult) {
					continue;
				}*/
				String responseBody = new String(rawResult.getRawData().array(), Consts.UTF_8);
				int ios = responseBody.indexOf("var msgList = '{");
				if (-1 != ios) {
					int ion = responseBody.indexOf("}';", ios);
					if (-1 != ion) {
						String ss = responseBody.substring(ios + 15, ion + 1).replace("&quot;", "\"");
						JSObject msgList = JSUtils.createJSObject(ss);
						JSArray list = msgList.getNotNullJSArray("list");
						int length = list.length();
						DateFormat df = DateFormat.getDateTimeInstance();
						for (int i = 0; i < length; i++) {
							JSObject item = list.getNotNullJSObject(i);
							JSObject cmi = item.getNotNullJSObject("comm_msg_info");
							long datetime = cmi.getNotNullLong("datetime") * 1000;
							Date pubtime = new Date(datetime);
							JSObject app_msg_ext_info = item.getNotNullJSObject("app_msg_ext_info");
							String title = app_msg_ext_info.getNotNullString("title");
							String content_url = app_msg_ext_info.getNotNullString("content_url").replace("\\/", "/")
									.replace("&amp;amp;", "&");
							logger.info("{} - {} - {}", df.format(pubtime), title, content_url);
						}
					} else {
						logger.warn("ion not found - {}");
						logger.warn(responseBody);
					}
				} else {
					logger.warn("ios not found - {}");
					logger.warn(responseBody);
				}
				CookieStore cookieStore = c.getCookieStore();
				if (null != cookieStore) {
					for (Cookie cookie : cookieStore.getCookies()) {
						logger.info(cookie.toString());
						if ("pass_ticket".equalsIgnoreCase(cookie.getName())) {
							pass_ticket = URLEncoder.encode(cookie.getValue(), Consts.UTF_8.name());
						}
					}
				}
				try {
					Thread.sleep(60000 + random.nextInt(60000));
				} catch (InterruptedException e) {
				}
				if (block.exists()) {
					logger.info("met block");
					List<Cookie> cookies = c.getCookieStore().getCookies();
					for (Cookie cook : cookies) {
						logger.info("{}={}", cook.getName(), cook.getValue());
					}
					break;
				}
			}
		}
	}
}
