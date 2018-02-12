package com.hbrb.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import com.hbrb.jdbc.MyDataSource;
import com.hbrb.spider.ConstantsHome;
import com.hbrb.spider.Spider;
import com.hbrb.spider.model.article.FieldName;
import com.hbrb.spider.model.article.NewsArticle;
import com.hbrb.spider.model.article.SourceType;
import com.hbrb.spider.model.template.TargetUrl;

public class ProcessUtils {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(ProcessUtils.class);
	private static final String REGEX_LAYER = "(https?://(?!{c}\\.)[^/?]+{d0}|https?://{c}\\.[^/?]+{d1})([./?].*)?";
	private static final Pattern PATTERN_URL;
	static {
		String countryRegex = "(c[nacdfhiklmoqruvxyz]|a[adefgilmnorstuwz]|b[abdefghijmnorstvwyz]|d[ejkmoz]|e[ceghrstuv]|f[ijkmor]|g[adefghilmnprstuwy]|h[kmnrtu]|i[delmnoqrst]|j[emop]|k[eghimnprwyz]|l[abcikrstuvy]|m[acdeghklmnopqrstuvwxyz]|n[acefgiloprtuz]|om|p[aefghklmnrtwy]|qa|r[eosuw]|s[abcdeghijklmnortuvxyz]|t[cdfghjklmnortvwz]|u[agkmsyz]|v[aceginu]|w[fs]|y[et]|z[amw])";
		PATTERN_URL = Pattern.compile(
				"https?://(?<ip>\\d{1,3}(\\.\\d{1,3}){3}|(?<channel>([\\w-]+\\.)*)(?!(?<s>(com|net|gov|org|edu|aero|army|arts|biz|cc|co|coop|europa|firm|fm|gc|idv|im|info|int|me|mil|museum|name|nom|pro|rec|store|tv|tx|web|travel|xxx|site|mobi|wang|ren|citic|zj|js)(\\."
						+ countryRegex + ")?|" + countryRegex + ")([#?/:]|$))(?<domain>[\\w-]+\\.\\k<s>))($|[#?/:].*)");
	}
	
	// 去除HTML中的script标签、注释
	public static String cleanHtml(Element ele){
		String text = ele.text();
		if (text.isEmpty()) {
			Elements imgs = ele.getElementsByTag("img");
			if (imgs.isEmpty()) {
				return null;
			}
		}
		ele.getElementsByTag("script").remove();
		removeComment(ele);
		return ele.outerHtml();
	}
	
	public static void removeComment(Element ele){
		List<Node> childNodes = ele.childNodes();
		int size = childNodes.size();
		for (int i = size - 1; i >= 0; i--) {
			Node node = childNodes.get(i);
			if (node instanceof Comment) {
				node.remove();
			} else if (node instanceof Element) {
				removeComment((Element)node);
			}
		}
	}
	
	
	// 去除URL中的端口号
	private static final Pattern PATTERN_PORT = Pattern.compile("^https?://[^/?#:]+(:\\d{1,5})($|[?/#])");
	public static String removePort(String url) {
		Matcher ma = PATTERN_PORT.matcher(url);
		if (ma.find()) {
			return url.substring(0, ma.start(1)) + url.substring(ma.end(1));
		}
		return url;
	}
	
	// 解决Jsoup的absUrl方法不支持‘\’分隔符的问题
	public static String absUrl(Element ele, String attributeKey){
		String attr = ele.attr(attributeKey);
		if (attr.isEmpty()) {
			return "";
		}
		return resolve(ele.baseUri(), attr);
	}
	public static String resolve(String baseUrl, String relUrl){
		return StringUtil.resolve(baseUrl, relUrl.replace('\\', '/'));
	}
	
	public static boolean judgePageLink(String link) {
		// 转小写
		link = link.toLowerCase();
		
		// http://ipo.csrc.gov.cn/pdfdownload.action?ipoCode=732526198&xmlId=1
		// http://weblbs.cc.163.com/cc-download
		if (link.contains("download")) {
			return false;
		}
		
		// http://tech.ifeng.com/{{$item.document.extraLink|default:$item.url}}
		if (link.indexOf('{') >= 0) {
			return false;
		}
		
		// http://sjbdy.ycwb.com/wap/index.php?mdl=android
		// http://sjbdy.ycwb.com/wap/index.php?mdl=iphone
		if (link.endsWith("?mdl=android") || link.endsWith("?mdl=iphone")){
			return false;
		}
		
		// 去掉参数
		int indexOfInterrogation = link.indexOf('?');
		if (indexOfInterrogation > 0) {
			link = link.substring(0, indexOfInterrogation);
		}

		// 开始判断
		// .(jpg|png|gif|bmp|docx|doc|xls|xlsx|swf|ppt|flv|mp3|avi|mp4|mov|mpeg|mpg|rm|wmv|rmvb|3gp|aac|m4v|f4v|wav|mlv|ac3|ogg|dat|asf|flac|ape|m4a|mid|cda|aif|pic|mkv|zip|7z|rar|gz|tar.gz|tar|jar|iso|pdf|exe|dll|bin|apk)
		if (link.endsWith(".jpg") || link.endsWith(".png")
				|| link.endsWith(".gif") || link.endsWith(".bmp")
				|| link.endsWith(".docx") || link.endsWith(".doc")
				|| link.endsWith(".xls") || link.endsWith(".xlsx")
				|| link.endsWith(".swf") || link.endsWith(".ppt")
				|| link.endsWith(".flv") || link.endsWith(".mp3")
				|| link.endsWith(".avi") || link.endsWith(".mp4")
				|| link.endsWith(".mov") || link.endsWith(".mpeg")
				|| link.endsWith(".mpg") || link.endsWith(".rm")
				|| link.endsWith(".wmv") || link.endsWith(".rmvb")
				|| link.endsWith(".3gp") || link.endsWith(".aac")
				|| link.endsWith(".m4v") || link.endsWith(".f4v")
				|| link.endsWith(".wav") || link.endsWith(".mlv")
				|| link.endsWith(".ac3") || link.endsWith(".ogg")
				|| link.endsWith(".dat") || link.endsWith(".asf")
				|| link.endsWith(".flac") || link.endsWith(".ape")
				|| link.endsWith(".m4a") || link.endsWith(".mid")
				|| link.endsWith(".cda") || link.endsWith(".aif")
				|| link.endsWith(".pic") || link.endsWith(".mkv")
				|| link.endsWith(".zip") || link.endsWith(".7z")
				|| link.endsWith(".rar") || link.endsWith(".gz")
				|| link.endsWith(".tar.gz") || link.endsWith(".tar")
				|| link.endsWith(".jar") || link.endsWith(".iso")
				|| link.endsWith(".pdf") || link.endsWith(".exe")
				|| link.endsWith(".dll") || link.endsWith(".bin")
				|| link.endsWith(".apk")) {
			return false;
		}
		
		// http://auto.sohu.com/20061008/mailto:jubao@contact.sohu.com
		if (Pattern.compile("/mailto:\\w+@").matcher(link).find()) {
			return false;
		}
		
		return true;
	}
	
	public static final String PLACEHOLDER_DOMAIN = "{domain}";
	public static final String REGEX_DEEP_0 = "https?://(\\w+\\.)?{domain}(/(index\\.(s?html?|php|jsp))?)?";
	public static final String REGEX_DEEP_1 = "https?://((\\w+\\.)?{domain}[/?]\\D+|(\\w+\\.){2,}{domain}([/?]\\D*)?)";
	public static int calculateDeep(String url, String domain){
		if (null == domain || domain.length() == 0) {
			return -1;
		}
		domain = domain.replace(".", "\\.");
		
		if (url.matches(REGEX_DEEP_0.replace(PLACEHOLDER_DOMAIN, domain))) {
			return 0;
		}
		
		if (url.matches(REGEX_DEEP_1.replace(PLACEHOLDER_DOMAIN, domain))) {
			return 1;
		}
		
		return -1;
	}
	
	/**
	 * 提取url中的domain部分
	 * @param url
	 * @return	非empty字符串或null(当无法提取时)
	 */
	public static String extractDomain(String url) {
		Matcher ma = PATTERN_URL.matcher(url.toLowerCase());
		if (ma.matches()) {
			String domain = ma.group("domain");
			if (null == domain) {
				return ma.group("ip");
			} else {
				return domain;
			}
		} else {
			return null;
		}
	}

	public static String extractLayer(String url, String thirdDomain, int dc0,
			int dc1) {
		if (null == thirdDomain || thirdDomain.length() == 0) {
			thirdDomain = "www";
		}
		String d0 = "";
		String d1 = "/[^./?]+";
		if (dc0 > 0) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < dc0; i++) {
				sb.append(d1);
			}
			d0 = sb.toString();
		}
		if (dc1 > 0) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < dc1; i++) {
				sb.append(d1);
			}
			d1 = sb.append(d1).toString();
		}

		String regex = REGEX_LAYER.replace("{c}", thirdDomain)
				.replace("{d0}", d0).replace("{d1}", d1);
		Pattern pa = Pattern.compile(regex);
		Matcher ma = pa.matcher(url);
		if (ma.matches()) {
			return ma.group(1);
		}
		return null;
	}

	public static String extractChannelFromUrl(String url) {
		Matcher ma = PATTERN_URL.matcher(url);
		if (ma.matches()) {
			String group = ma.group(3);
			if (null != group && group.length() != 0) {
				return group.substring(0, group.length() - 1);
			}
		}
		return null;
	}

	public static String removeCdataTag(String source) {
		if (source.startsWith("<![CDATA[") && source.endsWith("]]>")) {
			return source.substring(9, source.length() - 3);
		} else {
			return source;
		}
	}

	public static final Pattern forumPa = Pattern
			.compile("/forum-\\d+-1\\.");
	public static final Pattern paperPa = Pattern
			.compile("\\D(20|19)\\d{2}[/_-]?(0[1-9]|1[0-2])[/_-]?(0[1-9]|[12]\\d|3[01])/node_");
	public static final Pattern threadPa = Pattern
			.compile("/thread-\\d+-1-1\\.");
	public static final Pattern datePa = Pattern
			.compile("\\D(20|19)\\d{2}[/_-]?(0[1-9]|1[0-2])[/_-]?(0[1-9]|[12]\\d|3[01])");
	public static final Pattern datePa1 = Pattern
			.compile("\\D(20|19)\\d{2}[/_-]?([1-9]|1[0-2])[/_-]?([1-9]|[12]\\d|3[01])\\D");
	private static final Pattern naviPa = Pattern
			.compile("https?://[^/?]+([/?]\\D*(201[89]\\D*)?)?");
	private static final Pattern warnPa = Pattern
			.compile("/(?![a-z]*[./])[\\w-]{8,}");
	private static final Pattern naviKeywordsPa = Pattern.compile("[^a-zA-Z](category|articles|index|list|node|channels?|tags?|search|keywords?)([^a-zA-Z]|$)");
	private static final org.slf4j.Logger analysisUrlLogger = org.slf4j.LoggerFactory
			.getLogger("com.hbrb.spider.util.ProcessUtils.analysisUrlLogger");

	public static int analysisUrl(String url) {
		if (null == url || url.length() == 0) {
			return -1;
		}
		String lowerLine = url.toLowerCase();
		if (naviPa.matcher(lowerLine).matches()) {
			analysisUrlLogger.trace(url);
			return 0;
		} else if (containNaviKeyword(lowerLine)) {
			analysisUrlLogger.debug(url);
			return 1;
		} else if (datePa.matcher(lowerLine).find()) {
			analysisUrlLogger.error(url);
			return 4;
		} else if (datePa1.matcher(lowerLine).find()) {
			analysisUrlLogger.error(url);
			return 4;
		} else if (lowerLine.contains("article")) {
			analysisUrlLogger.warn(url);
			return 3;
		} else if (warnPa.matcher(lowerLine).find()) {
			analysisUrlLogger.warn(url);
			return 3;
		} else {
			analysisUrlLogger.info(url);
			return 2;
		}
	}
	public static boolean containNaviKeyword(String url){
		if (url.length() < 8) {
			return false;
		}
		int indexOf = url.indexOf('/', 8);
		if (indexOf < 0) {
			indexOf = url.indexOf('?', 8);
			if (indexOf < 0) {
				return false;
			}
		}
		url = url.toLowerCase();
		if (naviKeywordsPa.matcher(url.substring(indexOf)).find()) {
			return true;
		}
		return false;
	}
	
	/**
	 * 判断是否为导航URL：根据sourceType进行正则匹配
	 * </br><b>OR</b> 根据导航URL正则匹配
	 * </br><b>OR</b> URL中是否包含关键词：articles,list,node...
	 * @param url
	 * @param sourceType
	 * @return
	 */
	public static boolean isNaviUrl(String url, int sourceType){
		url = url.toLowerCase();
		if (SourceType.PAPER == sourceType) {
			Matcher matcher = ProcessUtils.paperPa.matcher(url);
			if (matcher.find()) {
				String datePart = matcher.group();
				datePart = datePart.replaceAll("\\D", "");
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
				if (sdf.format(new Date()).equals(datePart)) {
					return true;
				}
			}
			return false;
		}
		if (SourceType.BBS == sourceType) {
			if (ProcessUtils.forumPa.matcher(url).find()) {
				return true;
			}
		}
		if (naviPa.matcher(url).matches()) {
			return true;
		}
		if (containNaviKeyword(url)) {
			return true;
		}
		return false;
	}

	/**
	 * 通过URL判断新闻是否过期，最近两天之前的新闻都算过期
	 * @param url
	 * @param targerUrl
	 * @param today
	 * @param yesterday
	 * @return
	 */
	public static boolean judgeUrlExpired(String url, TargetUrl targerUrl,
			Date... dates) {
		// FIXME test
		if (!Spider.judgeExpired) {
			return false;
		}
		String datePattern = targerUrl.getDatePattern();
		String dateRegex = targerUrl.getDateRegex();
		if (null != dates && dates.length != 0 && null != datePattern
				&& datePattern.length() != 0 && null != dateRegex
				&& dateRegex.length() != 0) {
			SimpleDateFormat sdf = new SimpleDateFormat(datePattern);
			StringBuilder sb = new StringBuilder("(");
			for (Date date : dates) {
				sb.append(sdf.format(date)).append('|');
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.append(')');
			Pattern recentTargetUrlPattern = Pattern.compile(targerUrl
					.getPattern().pattern().replace(dateRegex, sb.toString()));
			if (!recentTargetUrlPattern.matcher(url).matches()) {
				return true;
			}
		}
		return false;
	}
	
	private static final String REGEX_KEYWORDS_IGNORE_HEAD = "https?://([\\w-]+\\.)*(";
	private static final String REGEX_KEYWORDS_IGNORE_FOOT = ")(\\.[\\w-]+)*([/?].*)?";
	private static final String REGEX_KEYWORDS_IGNORE_HEAD_ = "https?://([^/]+/)+(";
	private static final String REGEX_KEYWORDS_IGNORE_FOOT_ = ")([/?.].*)?";
	private static Pattern[] newsIgnorePatterns;
	private static Pattern[] bbsIgnorePatterns;
	private static Pattern[] blogIgnorePatterns;
	private static Pattern[] paperIgnorePatterns;
	/**
	 * 匹配汉字的正则
	 */
	private static final Pattern PATTERN_CH = Pattern.compile("[一-龥]");
	private static final Pattern PATTERN_USER = Pattern.compile("[?&]user([nN]ame|[iI][dD])=[^?&=]+");
	private static final Pattern PATTERN_PAGINATION = Pattern.compile("[\\s 　]*(\\d+|\\.[\\s 　]*\\.[\\s 　]*\\.|\\[[\\s 　]*\\d+[\\s 　]*\\]|([上下][\\s 　]*一|[尾末])[\\s 　]*页)[\\s 　]*");
	public static boolean commonIgnore(String title, String url, int sourceType) {
		if (PATTERN_PAGINATION.matcher(title).matches()) {
			return true;
		}
		if (PATTERN_CH.matcher(url).find()) {
			return true;
		}
		if (PATTERN_USER.matcher(url).find()) {
			return true;
		}
		char[] charArray = url.toCharArray();
		int scount = 0;
		for (char c : charArray) {
			if (c == '/') {
				scount++;
			}
		}
		if (scount >= 12) {
			return true;
		}
		
		url = url.toLowerCase();
		switch (sourceType) {
		case SourceType.BBS:
			if (null != bbsIgnorePatterns) {
				for (Pattern pa : bbsIgnorePatterns) {
					if (pa.matcher(url).matches()) {
						return true;
					}
				}
			}
			break;
		case SourceType.BLOG:
			if (null != blogIgnorePatterns) {
				for (Pattern pa : blogIgnorePatterns) {
					if (pa.matcher(url).matches()) {
						return true;
					}
				}
			}
			break;
		case SourceType.PAPER:
			if (null != paperIgnorePatterns) {
				for (Pattern pa : paperIgnorePatterns) {
					if (pa.matcher(url).matches()) {
						return true;
					}
				}
			}
			break;
		default:
			if (null != newsIgnorePatterns) {
				for (Pattern pa : newsIgnorePatterns) {
					if (pa.matcher(url).matches()) {
						return true;
					}
				}
			}
			break;
		}
		
		return false;
	}

//	private static final String PARAM_SITEMAP = "?sitemap";
//	private static final Pattern PATTERN_PARAM_FROM = Pattern.compile("([^?]+)\\?from=[^&]*&?");
	
	/**
	 * 处理URL：去空格、锚点、去掉尾部?sitemap、去掉 ?from=、去掉尾部? 等
	 * @param url
	 * @return 处理后的URL
	 * @throws IOException
	 */
	public static String processUrl(String url) throws IOException {
		// 去空格换行
		url = url.replaceAll("[\\s 　]", "");
		
		// 去掉锚点
		int indexOfPound = url.indexOf('#');
		if (indexOfPound > 0) {
			url = url.substring(0, indexOfPound);
		}
		
		// 处理相对路径
		boolean pathEndsWithSlash = false;
		int indexOfPathStart = url.indexOf('/', 8);
		if (-1 != indexOfPathStart) {
			int indexOfPathEnd = url.indexOf("?");
			if (-1 == indexOfPathEnd) {
				indexOfPathEnd = url.length();
			}
			// http://sso.sjy.net.cn?AppID=d5edf20d76ed446ead1f382e0cb3a50e&topath=http://wljy.sjy.net.cn/&forward=http://wljy.sjy.net.cn/sjzUser!qhtfsso
			if (indexOfPathEnd > indexOfPathStart) {
				String path = url.substring(indexOfPathStart, indexOfPathEnd);
				if (path.charAt(path.length()-1) == '/') {
					pathEndsWithSlash = true;
				}
				
				File file = new File(path.replaceAll("/{2,}", "/"));
				String canonicalPath = file.getCanonicalPath();
				if (File.separatorChar == '\\') {
					int indexOfFirstSeparatorChar = canonicalPath.indexOf(File.separatorChar);
					canonicalPath = canonicalPath.substring(indexOfFirstSeparatorChar).replace(File.separatorChar, '/');
				}
				url = url.substring(0, indexOfPathStart) + canonicalPath
						+ (pathEndsWithSlash && canonicalPath.length() != 1 ? '/' : "")
						+ url.substring(indexOfPathEnd);
			}
		}
		
		// 去掉尾部的“?sitemap”
		/*if (url.endsWith(PARAM_SITEMAP)) {
			url = url.substring(0, url.length() - PARAM_SITEMAP.length());
		}*/
		
		// http://love.163.com?from=sitemap 去掉 ?from=
		/*Matcher ma = PATTERN_PARAM_FROM.matcher(url);
		if (ma.matches()) {
			url = ma.group(1);
		}*/
		
		// 如果以?结尾的话，去掉?
		int lastIndex = url.length() -1;
		if (url.charAt(lastIndex) == '?') {
			url = url.substring(0, lastIndex);
		}
		
		return url;
	}

	private static String[] uncorrelatedNames;
	private static String[] uncorrelatedWords;
	
	/**
	 * 查看name是否命中t_properties表c_name中array.comma.keywords.ignore.linkname.contain对应的词
	 * <p> 去掉藏品、娱乐、地产等频道链接
	 * @param name
	 * @return
	 */
	public static boolean isUncorrelatedName(String name) {
		if (null != uncorrelatedWords) {
			for (String word : uncorrelatedWords) {
				if (name.contains(word)) {
					return true;
				}
			}
		}
		if (null != uncorrelatedNames) {
			for (String uncorrelatedName : uncorrelatedNames) {
				if (uncorrelatedName.equals(name)) {
					return true;
				}
			}
		}
		return false;
	}

	public static Date parseDate(String source) throws ParseException {
		source = source.replaceAll("\\D+", " ").trim();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy MM dd HH mm ss");
		Date date;
		try {
			date = sdf.parse(source);
		} catch (ParseException e) {
			sdf = new SimpleDateFormat("yyyy MM dd HH mm");
			try {
				date = sdf.parse(source);
			} catch (ParseException e1) {
				sdf = new SimpleDateFormat("yyyy MM dd");
				date = sdf.parse(source);
			}
		}
		return date;
	}
	
	/**
	 * 自动化抽取论坛
	 * @param page
	 * @param article
	 */
	public static void extractPaper(Document dom, NewsArticle article) {
		// 引题+标题+副题
		Elements titleEles = dom.select("table > tbody > tr > td > table > tbody > tr[valign=top] > td[align=center]");
		if (titleEles.size() >= 3) {
			String className0 = titleEles.get(0).className();
			String className1 = titleEles.get(1).className();
			String className2 = titleEles.get(2).className();
			if (!className0.isEmpty() && !className2.isEmpty() && className0.equals(className2) && !className0.equals(className1)) {
				String title = titleEles.get(0).text().trim();
				if (!title.isEmpty()) {
					article.setPretitle(title);
				}
				title = titleEles.get(1).text().trim();
				if (!title.isEmpty()) {
					article.setTitle(title);
				}
				title = titleEles.get(2).text().trim();
				if (!title.isEmpty()) {
					article.setSubtitle(title);
				}
			}
		}
		
		// 正文
		String content = null;
		Element ozoom = dom.getElementById("ozoom");
		if (null != ozoom) {
			content = ozoom.outerHtml();
		} else {
			Elements founderContentEles = dom.getElementsByTag("founder-content");
			if (founderContentEles.size() == 1) {
				content = founderContentEles.get(0).outerHtml();
			}
		}
		if (null != content) {
			article.setContent(content);
		}
		
		// 作者
		Elements founderAuthorEles = dom.getElementsByTag("founder-author");
		if (founderAuthorEles.size() == 1) {
			String authors = founderAuthorEles.get(0).text().trim();
			if (!authors.isEmpty()) {
				article.setAuthors(authors);
			}
		}
	}
	
	/**
	 * 自动化抽取论坛
	 * @param page
	 * @param article
	 */
	public static void extractPost(Document doc, NewsArticle article) {
		// TODO 待测试
		// 标题
		Element thread_subject = doc.getElementById("thread_subject");
		if (null != thread_subject) {
			String title = thread_subject.ownText().trim();
			if (null != title && !title.isEmpty()) {
				article.setTitle(title);
			}
		} else {
			return;
		}

		// 正文、日期
		// http://bbs.0513.org/thread-3571151-1-1.html
		// http://bbs.0566cc.cc/thread-178588-1-1.html
		Element postlist = doc.getElementById("postlist");
		if (null == postlist) {
			return;
		}
		Elements postEles = postlist.getElementsByAttributeValueStarting("id", "post_");
		if (!postEles.isEmpty()) {
			Element postEle = postEles.get(0);
			String pid = postEle.id().substring(5);
			if (pid.isEmpty()) {
				logger.warn("pid.isEmpty() - {}", article.getUrl());
				return;
			}
			
			Element postmessageEle = postEle.getElementById("postmessage_" + pid);
			if (null != postmessageEle) {
				String content = postmessageEle.text().trim();
				if (null != content && !content.isEmpty()) {
					article.setContent(content);
				}
			}
			
			Element authorpostonEle = postEle.getElementById("authorposton_" + pid);
			if (null != authorpostonEle) {
				String date = null;
				Elements spans = authorpostonEle.getElementsByTag("span");
				if (!spans.isEmpty()) {
					date = spans.get(0).attr("title").trim();
				} else {
					date = authorpostonEle.text().trim();
				}
				if (null != date && !date.isEmpty()) {
					Date createDate = null;
					try {
						createDate = ProcessUtils.parseDate(date);
					} catch (ParseException e) {
						logger.warn("unparseable date : \"{}\" - {}", date,
								article.getUrl());
					}
					article.setPubTime(createDate);
				}
			}
		}

		// 来源
		Elements eles = doc.select("head > meta[name=application-name]");
		if (!eles.isEmpty()) {
			String source = eles.get(0).attr("content").trim(); 
			if (null != source && !source.isEmpty()) {
				article.setPubSource(source);
			}
		}
	}

	private final static String homeRegex = "首[\\s 　]*页";
	private final static String HOLDER_HOME = "${home}";
	private final static String channelMark = "(-{1,3}[\\s 　]*)?(>{1,3}|→{1,3}|/{1,3}|＞{1,3})";
	private final static String channelHeadRegex = "(((当前|[现所]在)的?|您的)位置[是为]?[\\s 　]*[：:]|"+HOLDER_HOME+")";
	private final static String channelBodyRegex = "(((当前|[现所]在)的?|您的)位置[是为]?[\\s 　]*[：:]|"+HOLDER_HOME+"[\\s 　]*"+channelMark+")[\\s 　]*[^>→＞/|\\s 　]+([\\s 　]*"+channelMark+"[\\s 　]*[^>→＞/|\\s 　]+)*";
	private final static Pattern channelTailPa = Pattern.compile(channelMark + "[\\s 　]*正[\\s 　]*文");
	private final static Pattern PA_BODY_CHANNEL = Pattern.compile("([^>→＞/|\\s 　]+[\\s 　]*"+channelMark + "[\\s 　]*)+正[\\s 　]*文");
	private final static Pattern authorHeadPa = Pattern.compile("([作记]者|编辑|责编|(通讯|评论)员)[：:\\s| 　]+");
	private final static Pattern authorBodyPa = Pattern.compile("([作记]者|编辑|责编|(通讯|评论)员)[：:\\s| 　]+(?<"+FieldName.AUTHORS+">[^\\s|/ 　：:）)\\]；;，,、\\/”]{2,5}([\\s| 　）)\\]；;，,、\\/”]+[^\\s|/ 　：:）)\\]；;，,、\\/”]{2,5})*)([\\s| 　）)\\]；;。.”]+|$)");
	private static Pattern sourceHeadPa = Pattern.compile("(来源于?|出处|转自|稿源|来自于)[\\s 　]*[：:]");
	private static Pattern sourceBodyPa = Pattern.compile("(来源于?|出处|转自|稿源|来自于)[\\s 　]*[：:](?![\\s 　]*(20|19)\\d{2}[/_-]?(0[1-9]|1[0-2])[/_-]?(0[1-9]|[12]\\d|3[01]))[\\s 　]*(?<"+FieldName.PUBSOURCE+">[^\\s|/ 　：:）)\\]”]{1,30})([\\s| 　）)\\]”]|$)");
	private static Pattern pubtimePa = Pattern.compile("(^|[\\s：:| 　])(?<year>(20|19)?\\d{2})[.年/-](?<month>0?[1-9]|1[0-2])[.月/-](?<day>0?[1-9]|[12]\\d|3[01])日?([\\s 　]{0,4}(?<hour>0?\\d|1\\d|2[0-3])[:：](?<minute>[0-5]?\\d)([:：](?<second>[0-5]?\\d))?)?([\\s| 　]|$)");
	private final static String MINYOU = ",Minyoo.cn";
	private final static String[] TAGS_TITLE = {"h1", "h2", "h3", "h4", "h5", "h6", "strong"};
	// http://www.qhdcoop.com/html/2018/0-25/n339536256.html - #fileTile、#artibody
	private final static String[] ICS_TITLE = { "article-title", "article_title", "content_title", "main-title",
			"text-title", "details_title", "art-title", "con_title", "newsContent_title", "News_Title",
			"news_info_title", "articleinfo_title", "newstitle", "view_title", "showtitle", "MsgTitle", "conttitle", "title_tex", "news_tit", "fontnewstitle", "artibodyTitle", "tdTitle", "title", "Title", "subject",
			"biaoti", "dabiaoti", "comment-subject" };
	private final static String[] ICS_CONTENT = { "article-content", "article_content", "articleContnet", ".content_content",
			"newscontent", "display_content", "textContent", "contenttext", "text_content", "news_content",
			"content-text", "articleContent", "detail_content", "content_nr", "content_main", "content_news",
			"news_info_Content", "article_con", "text-text", "nr_text", "text_zw", "main_article", "article-main", "ContentBody",
			"news_nr", "maintxt", "article_body", "news_body", "articlebody", "ArticleBody", "artibody_content", "view_content", "DetailContent", "contentdetail", "Custom_UnionStyle", "TRS_Editor", "ArticleCnt",
			"article_box", "article-box", "conten_box", "detail_txt", "conText", "ContentShow", "conN", "conbox", "fontzoom", "pb_text", "show_text",
			"zhengwen", "neirong", "content", "article", "zoom", "Zoom", "article1", "detail", "con" };
	public static void extractArticle(Document dom, NewsArticle article) {
		String title = article.getTitle();
		String content = article.getContent();
		Date createDate = article.getPubTime();
		String source = article.getPubSource();
		String keyword = article.getKeywords();
		String summary = article.getDescription();
		String authors = article.getAuthors();
		String channel = article.getChannel();
		if (null != title && !title.isEmpty() && null != content && !content.isEmpty() && null != source
				&& !source.isEmpty() && null != keyword && !keyword.isEmpty() && null != summary && !summary.isEmpty()
				&& null != createDate && null != authors && !authors.isEmpty() && null != channel && !channel.isEmpty()) {
			return;
		}
		
		// 标题
		if (null == title || title.isEmpty()) {
			Elements titleEles = dom.select("head title");
			if (titleEles.size() != 1) {
				titleEles = dom.select("title");
			}
			if (titleEles.size() == 1) {
				String headTitle = titleEles.get(0).text().trim();
				if (!headTitle.isEmpty()) {
					boolean foundTitle = false;
					for (String tag : TAGS_TITLE) {
						Elements hEles = dom.select(tag);
						for (Element hEle : hEles) {
							String htext = hEle.text().trim();
							if (!htext.isEmpty() && headTitle.startsWith(htext)) {
								foundTitle = true;
								title = htext;
								article.setTitle(title);
								break;
							}
						}
						if (foundTitle) {
							break;
						}
					}
				}
			}
		}
		if (null == title || title.isEmpty()) {
			Element titleEle = null;
			for (String ic : ICS_TITLE) {
				titleEle = dom.getElementById(ic);
				if (null != titleEle) {
					if (isTitleElement(titleEle)) {
						break;
					} else {
						titleEle = null;
					}
				}
			}
			if (null == titleEle) {
				for (String ic : ICS_TITLE) {
					Elements titleEles = dom.getElementsByClass(ic);
					if (titleEles.size() == 1) {
						titleEle = titleEles.get(0);
						if (isTitleElement(titleEle)) {
							break;
						} else {
							titleEle = null;
						}
					}
				}
			}
			if (null != titleEle) {
				title = titleEle.text().trim();
				if (!title.isEmpty()) {
					article.setTitle(title);
				}
			}
		}
		
		// 正文
		if (null == content || content.isEmpty()) {
			// zhengwen + content & newstitle + title - http://www.tshjs.gov.cn/web/NewsDetail/NewsDetail.aspx?colid=21&newsid=2384
			Element contentEle = null;
			for (String ic : ICS_CONTENT) {
				contentEle = dom.getElementById(ic);
				if (null != contentEle) {
					break;
				}
			}
			if (null == contentEle) {
				for (String ic : ICS_CONTENT) {
					Elements contentEles = dom.getElementsByClass(ic);
					if (contentEles.size() == 1) {
						contentEle = contentEles.get(0);
						break;
					}
				}
			}
			if (null != contentEle) {
				content = contentEle.outerHtml();
				article.setContent(content);
			}
		}
		if (null == content || content.isEmpty()) {
			Elements ps = dom.select("p");
			int size = ps.size();
			if (size != 0) {
				List<Element> parents = new ArrayList<Element>();
				List<Integer> counts = new ArrayList<Integer>();
				for (int i = 0; i < size; i++) {
					Element p = ps.get(i);
					Element parent = p.parent();
					boolean exist = false;
					for (int j = 0; j < parents.size(); j++) {
						Element existParent = parents.get(j);
						if (existParent == parent) {
							Integer integer = counts.get(j);
							counts.set(j, integer + 1);
							exist = true;
							break;
						}
					}
					if (!exist) {
						parents.add(parent);
						counts.add(1);
					}
				}
				
				int maxCountIndex = 0;
				int maxCount = 0;
				int countSize = counts.size();
				for (int i = 0; i < countSize; i++) {
					Integer count = counts.get(i);
					if (count > maxCount) {
						maxCount = count;
						maxCountIndex = i;
					}
				}
				Element contentEle = parents.get(maxCountIndex);
				content = contentEle.outerHtml();
				article.setContent(content);
			}
		}
		
		
		// 发布时间
		if (null == createDate) {
			String pubTime = null;
			// baidu
			Element pubTimeEle = dom.getElementById("pubtime_baidu");
			if (null != pubTimeEle) {
				pubTime = pubTimeEle.text().trim();
			}
			// meta
			if (null == pubTime || pubTime.isEmpty()) {
				Elements select = dom.select("meta[name=publishdate]");
				if (select.isEmpty()) {
					select = dom.select("meta[http-equiv=publishdate]");
				}
				if (!select.isEmpty()) {
					pubTime = select.get(0).attr("content").trim();
				}
			}
			// 正则
			if (null == pubTime || pubTime.isEmpty()) {
				Elements pubtimeEles = dom
						.getElementsMatchingOwnText(pubtimePa);
				if (!pubtimeEles.isEmpty()) {
					pubTime = pubtimeEles.get(0).text().trim();
				}
			}
			
			if (null != pubTime && !pubTime.isEmpty()) {
				Matcher ma = pubtimePa.matcher(pubTime);
				if (ma.find()) {
					String year = ma.group("year");
					if (year.length() == 2) {
						char tens = year.charAt(0);
						if (tens > '6') {
							year = "19" + year;
						} else {
							year = "20" + year;
						}
					}
					String month = ma.group("month");
					if (month.length() == 1) {
						month = '0' + month;
					}
					String day = ma.group("day");
					if (day.length() == 1) {
						day = '0' + day;
					}
					
					String hour = ma.group("hour");
					if (null == hour) {
						hour = "00";
					} else if (hour.length() == 1) {
						hour = '0' + hour;
					}
					String minute = ma.group("minute");
					if (null == minute) {
						minute = "00";
					} else if (minute.length() == 1) {
						minute = '0' + minute;
					}
					String second = ma.group("second");
					if (null == second) {
						second = "00";
					} else if (second.length() == 1) {
						second = '0' + second;
					}
					try {
						article.setPubTime(new SimpleDateFormat("yyyyMMddHHmmss").parse(year + month + day+ hour + minute + second));
					} catch (ParseException e) {
						logger.warn("pubtime parse exception", e);
					}
				}
			}
		}
		
		// 来源
		if (null == source || source.isEmpty()) { // baidu
			Element pubSourceEle = dom.getElementById("source_baidu");
			if (null != pubSourceEle) {
				source = pubSourceEle.text().trim();
				if (!source.isEmpty()) {
					String text = extractPubSource(source);
					if (null != text && !text.isEmpty()) {
						source = text;
					}
					article.setPubSource(source);
				}
			}
		}
		if (null == source || source.isEmpty()) { // meta
			Elements select = dom.select("meta[name=source]");
			if (select.isEmpty()) {
				select = dom.select("meta[http-equiv=source]");
			}
			if (!select.isEmpty()) {
				source = select.get(0).attr("content").trim();
				if (null != source && !source.isEmpty()) {
					article.setPubSource(source);
				}
			}
		}
		// http://www.tscdi.gov.cn/news/html/2/?12613.html
		if ((null == source || source.isEmpty()) && null != sourceHeadPa
				&& null != sourceBodyPa) { // 正则
			Elements sourceEles = dom
					.getElementsMatchingOwnText(sourceHeadPa);
			for (Element sourceEle : sourceEles) {
				source = extractPubSource(sourceEle.text());
				if (null != source && !source.isEmpty()) {
					article.setPubSource(source);
					break;
				}
			}
		}
		
		// 关键词
		if (null == keyword || keyword.isEmpty()) {
			Elements select = dom.select("meta[name=keywords]");
			if (select.size() == 0) {
				select = dom.select("meta[http-equiv=keywords]");
			}
			if (!select.isEmpty()) {
				String value = select.get(0).attr("content").trim();
				if (null != value && !value.isEmpty() && !value.endsWith(MINYOU)) {
					article.setKeywords(value);
				}
			}
		}
		
		// 摘要
		if (null == summary || summary.length() == 0) {
			Elements select = dom.select("meta[name=description]");
			if (select.size() == 0) {
				select = dom.select("meta[http-equiv=description]");
			}
			if (!select.isEmpty()) {
				String value = select.get(0).attr("content").trim();
				if (null != value && !value.isEmpty() && !value.endsWith(MINYOU)) {
					article.setDescription(value);
				}
			}
		}
		
		String sourceName = article.getSourceName();
		
		// 作者
		if (null == authors || authors.isEmpty()) {
			Elements select = dom.select("meta[name=author]");
			if (select.isEmpty()) {
				select = dom.select("meta[http-equiv=author]");
			}
			if (!select.isEmpty()) {
				String text = select.get(0).attr("content").trim();
				if (checkAuthors(sourceName, text)) {
					authors = text;
					article.setAuthors(authors);
				}
			}
			
			if (null == authors || authors.isEmpty()) {
				Element baiduAuthorEle = dom.getElementById("author_baidu");
				if (null != baiduAuthorEle) {
					String text = baiduAuthorEle.text().trim();
					if (checkAuthors(sourceName, text)) {
						authors = text;
						article.setAuthors(authors);
					}
				}
				
				if (null == authors || authors.isEmpty()) {
					Elements authorEles = dom.getElementsMatchingOwnText(authorHeadPa);
					for (Element authorEle : authorEles) {
						authors = extractAuthors(authorEle.text());
						if (null != authors && !authors.isEmpty()) {
							article.setAuthors(authors);
							break;
						}
					}
				}
			}
		}
		
		// 频道
		// http://de.haiwainet.cn/n/2017/0908/c457012-31107559.html
		// http://politics.gmw.cn/2017-09/07/content_26063507.htm
		// http://news.china.com/focus/xysjd/13000924/20170909/31335300.html
		// http://china.huanqiu.com/article/2017-09/11230987.html
		// http://www.cankaoxiaoxi.com/roll10/bd/20170910/2229660.shtml
		// http://xinwen.eastday.com/a/n170910080219837.html
		// http://society.workercn.cn/2/201709/10/170910103955116.shtml
		// http://travel.cnr.cn/list/20170910/t20170910_523942283.shtml
		// http://pol.zjol.com.cn/201709/t20170910_5014447.shtml
		// http://linyi.iqilu.com/lyyaowen/2017/0910/3677848.shtml
		// http://fortune.chinanews.com/sh/2017/09-10/8327053.shtml
		// http://www.dzwww.com/xinwen/guoneixinwen/201709/t20170910_16404272.htm
		// http://news.sxxw.net/html/20179/10/417012.shtml
		// http://news.21cn.com/world/a/2017/0910/09/32701019.shtml
		// http://news.cnhubei.com/xw/gj/201709/t3985245.shtml
		// http://www.guancha.cn/MeiXinYu/2017_09_10_426465.shtml
		// http://www.techweb.com.cn/internet/2017-09-10/2583502.shtml
		// http://www.takefoto.cn/viewnews-1264757.html
		// http://news.cyol.com/content/2017-09/10/content_16485661.htm
		// http://news.163.com/17/0908/15/CTQQIP6T0001899N.html
		// http://news.fjsen.com/2017-09/08/content_20101460.htm
		// http://overseas.caijing.com.cn/20170907/4328295.shtml
		// http://usa.people.com.cn/n1/2017/0907/c241376-29521213.html
		// http://news.ifeng.com/a/20170907/51904895_0.shtml
		// http://shanghai.xinmin.cn/xmsz/2017/09/11/31273172.html
		// http://aq.anhuinews.com/system/2017/09/11/007708298.shtml
		// http://hf.anhuinews.com/system/2017/09/11/007707878.shtml?bzriqeiqgmpldnrp
		// 以上目标测试通过
		if (null == channel || channel.isEmpty()) {
			String chr = homeRegex;
			if (null != chr && !chr.isEmpty()) {
				chr = '(' + chr + '|' + sourceName + "(首页)?)";
			}
			Pattern channelHeadPa = Pattern.compile(channelHeadRegex.replace(HOLDER_HOME, chr));
			Elements channelEles = dom.getElementsMatchingOwnText(channelHeadPa);
			if (!channelEles.isEmpty()) {
				Pattern channelBodyPa = Pattern.compile(channelBodyRegex.replace(HOLDER_HOME, chr));
				for (Element channelEle : channelEles) {
					channel = extractChannel(channelBodyPa, channelEle);
					if (null == channel) {
						channel = extractChannel(channelBodyPa, channelEle.parent());
					}
					if (null != channel && !channel.isEmpty()) {
						article.setChannel(channel);
						break;
					}
				}
			}
			if (null == channel || channel.isEmpty()) {
				channelEles = dom.getElementsMatchingOwnText(channelTailPa);
				if (!channelEles.isEmpty()) {
					for (Element channelEle : channelEles) {
						channel = extractChannel(PA_BODY_CHANNEL, channelEle);
						if (null == channel) {
							channel = extractChannel(PA_BODY_CHANNEL, channelEle.parent());
						}
						if (null != channel && !channel.isEmpty()) {
							article.setChannel(channel);
							break;
						}
					}
				}
			}
			if (null == channel || channel.isEmpty()) {
				channelEles = dom.getElementsMatchingOwnText(channelMark);
				if (!channelEles.isEmpty()) {
					for (Element channelEle : channelEles) {
						String tagName = channelEle.tagName();
						if (!"a".equals(tagName)) {
							channel = extractChannel(channelEle, "#text");
							if (null == channel || channel.isEmpty()) {
								channel = extractChannel(channelEle.parent(), tagName);
							}
							if (null != channel && !channel.isEmpty()) {
								article.setChannel(channel);
								break;
							}
						}
					}
				}
			}
		}
	}
	
	private static boolean isTitleElement(Element titleEle) {
		int allElements = titleEle.getAllElements().size();
		if (allElements <= 3) {
			int children = titleEle.children().size();
			if (allElements == children + 1) {
				return true;
			}
		}
		return false;
	}

	private static Elements getChannelLinkEles(Element channelEle, String name){
		Elements eles = new Elements();
		List<Node> childNodes = channelEle.childNodes();
		int size = childNodes.size();
		if (size == 0) {
			return eles;
		}
		
		// 处理带注释的：http://news.huaxi100.com/show-255-935645-1.html
		List<Integer> skipIndexs = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			Node node = childNodes.get(i);
			if (!(node instanceof Comment)) {
				continue;
			}
			
			// 开头
			if (i == 0) {
				while (node instanceof Comment) {
					skipIndexs.add(i);
					i++;
					if (i == size) {
						break;
					}
					node = childNodes.get(i);
				}
				continue;
			}
			
			// 中间
			if (i < size - 1) {
				Node perNode = childNodes.get(i-1);
				if (perNode instanceof TextNode) {
					while (node instanceof Comment) {
						skipIndexs.add(i);
						i++;
						if (i == size) {
							break;
						}
						node = childNodes.get(i);
					}
					if (i < size && node instanceof TextNode) {
						skipIndexs.add(i);
						TextNode preText = (TextNode)perNode;
						TextNode nextText = (TextNode)node;
						preText.text(preText.text() + nextText.text());
						nextText.text("");
					}
				} else {
					while (node instanceof Comment) {
						skipIndexs.add(i);
						i++;
						if (i == size) {
							break;
						}
						node = childNodes.get(i);
					}
				}
				continue;
			}
			
			// 结尾
			if (i == size - 1) {
				skipIndexs.add(i);
			}
		}
		
		boolean startCheck = false;
		boolean checkName = false;
		for (int i = 0; i < size; i++) {
			if (skipIndexs.contains(i)) {
				continue;
			}
			Node node = childNodes.get(i);
			String nodeName = node.nodeName();
			if (!startCheck) {
				if ("a".equals(nodeName)) {
					// http://politics.gmw.cn/2017-09/07/content_26063507.htm 第一个a标签是网站logo
					Element ele = (Element)node;
					Elements children = ele.children();
					if (children.size() == 1) {
						if ("img".equals(children.get(0).tagName())) {
							continue;
						}
					}
					eles.add(ele);
					startCheck = true;
					checkName = true;
				}
			} else {
				if (checkName) {
					if (!nodeName.equals(name)) {
						return eles;
					}
					String text = null;
					if (node instanceof TextNode) {
						text = ((TextNode)node).text();
					} else if (node instanceof Element) {
						text = ((Element)node).text();
					} else {
						return eles;
					}
					if (!Pattern.compile("[\\s 　]*" + channelMark + "[\\s 　]*").matcher(text).matches()) {
						return eles;
					}
					
					checkName = false;
				} else {
					if (!"a".equals(nodeName)) {
						return eles;
					}
					eles.add((Element)node);
					checkName = true;
				}
			}
		}
		return eles;
	}
	
	private static String extractChannel(Element channelEle, String name) {
		Elements channelLinkEles = getChannelLinkEles(channelEle, name);
		if (channelLinkEles.size() > 1) {
			return extractChannel(channelLinkEles);
		}
		return null;
	}

	private static String extractChannel(Pattern channelBodyPa, Element channelEle) {
		String tagName = channelEle.tagName();
		if ("body".equals(tagName)) {
			return null;
		}
		Matcher ma = channelBodyPa.matcher(channelEle.text());
		if (ma.find()) {
			Elements eles = channelEle.getElementsByTag("a");
			if (eles.size() > 6) {
				return null;
			}
			return extractChannel(eles);
		}
		return null;
	}
	
	private static String extractChannel(Elements channelLinkEles) {
		if (!channelLinkEles.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for (Element aEle : channelLinkEles) {
				String text = aEle.text().trim();
				if (!text.isEmpty()) {
					sb.append(text).append('>');
				}
			}
			int length = sb.length();
			if (length != 0) {
				sb.deleteCharAt(length - 1);
				return sb.toString();
			}
		}
		return null;
	}
	
	private static boolean checkAuthors(String siteName, String authors){
		if (null == authors || authors.isEmpty()) {
			return false;
		}
		if (authors.indexOf('.') != -1) {
			return false;
		}
		if (siteName.equals(authors)) {
			return false;
		}
		return true;
	}

	public static String extractAuthors(String text){
		Matcher ma = authorBodyPa.matcher(text);
		if (ma.find()) {
			return ma.group(FieldName.AUTHORS).trim();
		}
		return null;
	}
	
	public static String extractPubSource(String text){
		Matcher sourceMa = sourceBodyPa.matcher(text);
		if (sourceMa.find()) {
			return sourceMa.group(FieldName.PUBSOURCE);
		}
		return null;
	}
	
	public static void loadProperties(Set<String> names){
		boolean updateIgnoreKeywordsRegex = false;
		boolean updateSourceExtractorRegex = false;
		boolean updatePubtimeExtractorRegex = false;
		boolean updateUncorrelatedWords = false;
		boolean updateUncorrelatedNames = false;
		boolean updateTitleIgnoreRegexs = false;
		StringBuilder sql = new StringBuilder("SELECT c_name,c_value FROM t_properties");
		if (null != names && names.size() != 0) {
			sql.append(" where c_name in (");
			for (String name : names) {
				if (null == name || name.length() == 0) {
					continue;
				}
				if (name.startsWith("regex.keywords.ignore.")) {
					if (updateIgnoreKeywordsRegex) {
						continue;
					} else {
						name = "regex.keywords.ignore.common','regex.keywords.ignore.bbs','regex.keywords.ignore.blog','regex.keywords.ignore.paper";
						updateIgnoreKeywordsRegex = true;
					}
				} else if ("regex.extractor.web.source".equals(name)) {
					updateSourceExtractorRegex = true;
				} else if ("regex.extractor.web.pubtime".equals(name)) {
					updatePubtimeExtractorRegex = true;
				} else if ("array.comma.keywords.ignore.linkname.contain".equals(name)) {
					updateUncorrelatedWords = true;
				} else if ("array.comma.keywords.ignore.linkname.equal".equals(name)) {
					updateUncorrelatedNames = true;
				} else if ("array.regex.ignore.title".equals(name)) {
					updateTitleIgnoreRegexs = true;
				}
				sql.append('\'').append(name).append('\'').append(',');
			}
			sql.deleteCharAt(sql.length() - 1);
			sql.append(")");
		} else {
			updateIgnoreKeywordsRegex = true;
			updateSourceExtractorRegex = true;
			updatePubtimeExtractorRegex = true;
			updateUncorrelatedWords = true;
			updateUncorrelatedNames = true;
			updateTitleIgnoreRegexs = true;
		}
		
		if (!updateIgnoreKeywordsRegex && !updateSourceExtractorRegex
				&& !updatePubtimeExtractorRegex && !updateUncorrelatedWords
				&& !updateUncorrelatedNames && !updateTitleIgnoreRegexs) {
			return;
		}
		
		Map<String, String> properties = new HashMap<String, String>();
		Connection conn = MyDataSource.connect();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			while (rs.next()) {
				properties.put(rs.getString(1), rs.getString(2));
			}
		} catch (SQLException e) {
			throw new Error("properties获取异常", e);
		} finally {
			MyDataSource.release(rs, stmt, conn);
		}
		
		if (updateIgnoreKeywordsRegex) {
			updateIgnoreKeywordsRegex(properties);
		}
		if (updateSourceExtractorRegex) {
			updateSourceExtractorRegex(properties);
		}
		if (updatePubtimeExtractorRegex) {
			updatePubtimeExtractorRegex(properties);
		}
		if (updateUncorrelatedWords) {
			updateUncorrelatedWords(properties);
		}
		if (updateUncorrelatedNames) {
			updateUncorrelatedNames(properties);
		}
		if (updateTitleIgnoreRegexs) {
			updateTitleIgnoreRegexs(properties);
		}
	}

	private static void updateUncorrelatedNames(Map<String, String> properties) {
		String value = properties.get("array.comma.keywords.ignore.linkname.equal");
		if (null == value || value.length() == 0) {
			throw new RuntimeException("property[array.comma.keywords.ignore.linkname.equal] not found");
		}
		uncorrelatedNames = value.split(",");
		logger.info("load property[array.comma.keywords.ignore.linkname.equal] - {}", value);
	}

	private static void updateUncorrelatedWords(Map<String, String> properties) {
		String value = properties.get("array.comma.keywords.ignore.linkname.contain");
		if (null == value || value.length() == 0) {
			throw new RuntimeException("property[array.comma.keywords.ignore.linkname.contain] not found");
		}
		uncorrelatedWords = value.split(",");
		logger.info("load property[array.comma.keywords.ignore.linkname.contain] - {}", value);
	}

	private static void updatePubtimeExtractorRegex(
			Map<String, String> properties) {
		String value = properties.get("regex.extractor.web.pubtime");
		if (null == value || value.length() == 0) {
			throw new RuntimeException("property[regex.extractor.web.pubtime] not found");
		}
		pubtimePa = Pattern.compile(value);
		logger.info("load property[regex.extractor.web.pubtime] - {}", value);
	}

	private static void updateSourceExtractorRegex(
			Map<String, String> properties) {
		String value = properties.get("regex.extractor.web.source");
		if (null == value || value.length() == 0) {
			throw new RuntimeException("property[regex.extractor.web.source] not found");
		}
		String[] split = value.split("\r\n");
		if (split.length != 2) {
			throw new RuntimeException("property[regex.extractor.web.source] error");
		}
		sourceHeadPa = Pattern.compile(split[0]);
		sourceBodyPa = Pattern.compile(split[0] + split[1]);
		logger.info("load property[regex.extractor.web.source] - {}", value);
	}

	private static void updateIgnoreKeywordsRegex(Map<String, String> properties) {
		String rkiCommon = properties.get("regex.keywords.ignore.common");
		if (null == rkiCommon || rkiCommon.length() == 0) {
			throw new RuntimeException("property[regex.keywords.ignore.common] not found");
		}
		String rkiBBS = properties.get("regex.keywords.ignore.bbs");
		if (null == rkiBBS || rkiBBS.length() == 0) {
			throw new RuntimeException("property[regex.keywords.ignore.bbs] not found");
		}
		String rkiBlog = properties.get("regex.keywords.ignore.blog");
		if (null == rkiBlog || rkiBlog.length() == 0) {
			throw new RuntimeException("property[regex.keywords.ignore.blog] not found");
		}
		String rkiPaper = properties.get("regex.keywords.ignore.paper");
		if (null == rkiPaper || rkiPaper.length() == 0) {
			throw new RuntimeException("property[regex.keywords.ignore.paper] not found");
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append(rkiCommon).append('|');
		sb.append(rkiBBS).append('|');
		sb.append(rkiBlog).append('|');
		sb.append(rkiPaper);
		newsIgnorePatterns = new Pattern[2];
		newsIgnorePatterns[1] = Pattern.compile(REGEX_KEYWORDS_IGNORE_HEAD_ + sb.toString() + REGEX_KEYWORDS_IGNORE_FOOT_);
		sb.append("|\\w");
		newsIgnorePatterns[0] = Pattern.compile(REGEX_KEYWORDS_IGNORE_HEAD + sb.toString() + REGEX_KEYWORDS_IGNORE_FOOT);
		logger.info("load property[regex.keywords.ignore.common] - {}", rkiCommon);
		
		sb = new StringBuilder();
		sb.append(rkiCommon).append('|');
		sb.append(rkiBlog).append('|');
		sb.append(rkiPaper);
		bbsIgnorePatterns = new Pattern[2];
		bbsIgnorePatterns[1] = Pattern.compile(REGEX_KEYWORDS_IGNORE_HEAD_ + sb.toString() + REGEX_KEYWORDS_IGNORE_FOOT_);
		sb.append("|\\w");
		bbsIgnorePatterns[0] = Pattern.compile(REGEX_KEYWORDS_IGNORE_HEAD + sb.toString() + REGEX_KEYWORDS_IGNORE_FOOT);
		logger.info("load property[regex.keywords.ignore.bbs] - {}", rkiBBS);
		
		sb = new StringBuilder();
		sb.append(rkiCommon).append('|');
		sb.append(rkiBBS).append('|');
		sb.append(rkiPaper);
		blogIgnorePatterns = new Pattern[2];
		blogIgnorePatterns[1] = Pattern.compile(REGEX_KEYWORDS_IGNORE_HEAD_ + sb.toString() + REGEX_KEYWORDS_IGNORE_FOOT_);
		sb.append("|\\w");
		blogIgnorePatterns[0] = Pattern.compile(REGEX_KEYWORDS_IGNORE_HEAD + sb.toString() + REGEX_KEYWORDS_IGNORE_FOOT);
		logger.info("load property[regex.keywords.ignore.blog] - {}", rkiBlog);
		
		sb = new StringBuilder();
		sb.append(rkiCommon).append('|');
		sb.append(rkiBBS).append('|');
		sb.append(rkiBlog);
		paperIgnorePatterns = new Pattern[2];
		paperIgnorePatterns[1] = Pattern.compile(REGEX_KEYWORDS_IGNORE_HEAD_ + sb.toString() + REGEX_KEYWORDS_IGNORE_FOOT_);
		sb.append("|\\w");
		paperIgnorePatterns[0] = Pattern.compile(REGEX_KEYWORDS_IGNORE_HEAD + sb.toString() + REGEX_KEYWORDS_IGNORE_FOOT);
		logger.info("load property[regex.keywords.ignore.paper] - {}", rkiPaper);
	}

	public static void updateProperties() throws IOException {
		File file = new File(ConstantsHome.USER_DIR + File.separatorChar
				+ "update" + File.separatorChar + "properties");
		if (!file.isFile()) {
			return;
		}
		List<String> lines = FileUtils.readLines(file, Charset.defaultCharset());
		file.delete();
		if (null == lines || lines.size() == 0) {
			return;
		}
		loadProperties(new HashSet<String>(lines));
	}

	private static void updateTitleIgnoreRegexs(
			Map<String, String> properties) {
		String value = properties.get("array.regex.ignore.title");
		if (null == value || value.length() == 0) {
			invalidTitlePatterns = null;
		} else {
			String[] split = value.split("\r?\n");
			invalidTitlePatterns = new Pattern[split.length];
			for (int i = 0; i < split.length; i++) {
				invalidTitlePatterns[i] = Pattern.compile(split[i]);
			}
		}
		logger.info("load property[array.regex.ignore.title] - {}", value);
	}
	private static Pattern[] invalidTitlePatterns = {Pattern.compile("(.*\\D|^)404(\\D.*|$)")};
	private final static String invalidTitle = "无法找到页面";
	public static boolean isInvalidTitle(String title) {
		if (null == title || title.length() == 0) {
			return true;
		}
		if (null != invalidTitlePatterns) {
			for (Pattern pa : invalidTitlePatterns) {
				if (pa.matcher(title).matches()) {
					return true;
				}
			}
		}
		if (title.contains(invalidTitle)) {
			return true;
		}
		return false;
	}

	private final static char[] CHARACTER_ILLEGAL = new char[]{'|','*','<','>','[',']','"','{','}'};
	public static boolean containsIllegalCharacter(String url) {
		for (char c : CHARACTER_ILLEGAL) {
			if (url.indexOf(c) > 0) {
				return true;
			}
		}
		return false;
	}
}
