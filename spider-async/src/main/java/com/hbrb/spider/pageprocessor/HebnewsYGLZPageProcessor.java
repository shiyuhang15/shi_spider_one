package com.hbrb.spider.pageprocessor;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.hbrb.exception.LogicError;
import com.hbrb.spider.exception.PageProcessException;
import com.hbrb.spider.model.article.SourceType;
import com.hbrb.spider.model.page.HtmlPage;
import com.hbrb.spider.model.task.GenericRequestTask;
import com.hbrb.spider.pageprocessor.HebnewsYGLZPageProcessor.RequestTask;
import com.hbrb.spider.pipeline.FilePipeline;

public class HebnewsYGLZPageProcessor extends HtmlPageProcessor<RequestTask> {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HebnewsYGLZPageProcessor.class);
	public static final int TASK_TYPE_NAVI = 0;
	public static final int TASK_TYPE_TARGET = 1;
	private static final DateFormat DF = DateFormat.getDateInstance();
	private static final Pattern PA_DATE_REPLY = Pattern.compile("日期：(\\d{4}-\\d{2}-\\d{2})");
	private static final String NODATE = "---- -- --";
	private final FilePipeline pipeline;
	

	public HebnewsYGLZPageProcessor(FilePipeline pipeline) {
		super();
		this.pipeline = pipeline;
	}

	public static final class RequestTask extends GenericRequestTask {
		private String category;
		private Date acceptDate;
		private Date overDate;

		public RequestTask(String url, int type) {
			super(url, type);
		}

		public RequestTask(String url, int type, String category, Date acceptDate, Date overDate) {
			super(url, type);
			this.category = category;
			this.acceptDate = acceptDate;
			this.overDate = overDate;
		}

		public Date getAcceptDate() {
			return acceptDate;
		}

		public void setAcceptDate(Date acceptDate) {
			this.acceptDate = acceptDate;
		}

		public Date getOverDate() {
			return overDate;
		}

		public void setOverDate(Date overDate) {
			this.overDate = overDate;
		}

		public String getCategory() {
			return category;
		}

		public void setCategory(String category) {
			this.category = category;
		}
	}

	public static final class Article extends com.hbrb.spider.model.article.Article {
		// 标题
		private String title;
		// 状态
		private String status;
		// 受理部门
		private String admission;
		// 分类
		private String category;
		// 回复日期
		private Date replyDate;
		// 受理日期
		private Date acceptDate;
		// 结办日期
		private Date overDate;
		public Article(Integer sourceType) {
			super(sourceType);
		}
		public String getTitle() {
			return title;
		}
		public void setTitle(String title) {
			this.title = title;
		}
		public String getStatus() {
			return status;
		}
		public void setStatus(String status) {
			this.status = status;
		}
		public String getAdmission() {
			return admission;
		}
		public void setAdmission(String admission) {
			this.admission = admission;
		}
		public String getCategory() {
			return category;
		}
		public void setCategory(String category) {
			this.category = category;
		}
		public Date getReplyDate() {
			return replyDate;
		}
		public void setReplyDate(Date replyDate) {
			this.replyDate = replyDate;
		}
		public Date getAcceptDate() {
			return acceptDate;
		}
		public void setAcceptDate(Date acceptDate) {
			this.acceptDate = acceptDate;
		}
		public Date getOverDate() {
			return overDate;
		}
		public void setOverDate(Date overDate) {
			this.overDate = overDate;
		}
		
	}

	@Override
	public void process(HtmlPage<RequestTask> page) throws PageProcessException {
		GenericRequestTask task = page.getRequestTask();
		switch (task.getType()) {
		case TASK_TYPE_NAVI:
			processNavi(page);
			break;
		case TASK_TYPE_TARGET:
			processTarget(page);
			break;
		default:
			throw new LogicError("未知任务类型 - " + task.getType());
		}
	}

	private void processTarget(HtmlPage<RequestTask> page) throws PageProcessException {
		Document doc = page.getDocument();
		Article article = new Article(SourceType.OTHER);
		article.setCreateTime(new Date());
		
		// 标题
		Element titleEle = doc.getElementById("ctl00_ContentPlaceHolder1_lblMsgName");
		if (null == titleEle) {
			throwPageProcessException(page, "title not found");
		}
		String title = titleEle.ownText();
		if (title.isEmpty()) {
			throwPageProcessException(page, "title is empty");
		}
		article.setTitle(title);
		
		// 状态
		Element statusEle = doc.getElementById("ctl00_ContentPlaceHolder1_lblMsgStatus");
		if (null == statusEle) {
			throwPageProcessException(page, "status not found");
		}
		String status = statusEle.ownText();
		if (status.isEmpty()) {
			throwPageProcessException(page, "status is empty");
		}
		article.setStatus(status);
		
		// 留言(发布)日期
		Date pubTime = null;
		Element dateEle = doc.getElementById("ctl00_ContentPlaceHolder1_lblIndate");
		if (null == dateEle) {
			throwPageProcessException(page, "pubtime not found");
		}
		String date = dateEle.ownText();
		if (date.isEmpty()) {
			throwPageProcessException(page, "pubtime is empty");
		}
		try {
			pubTime = DF.parse(date);
		} catch (ParseException e) {
			throwPageProcessException(page, "pubtime", e);
		}
		article.setPubTime(pubTime);
		
		// 受理部门
		Elements admissionEles = doc
				.select("div.boxl:nth-child(2) > div.con > ul:nth-child(2) > li:nth-child(2) > p > a");
		if (admissionEles.isEmpty()) {
			throwPageProcessException(page, "admission not found");
		}
		String admission = admissionEles.get(0).ownText();
		if (admission.isEmpty()) {
			throwPageProcessException(page, "admission is empty");
		}
		article.setAdmission(admission);
		
		// 正文
		Elements boxlEles = doc.select("#ctl00_ContentPlaceHolder1_divMsg > div.boxl");
		int size = boxlEles.size();
		if (size < 3) {
			throwPageProcessException(page, "content not found");
		}
		StringBuilder content = new StringBuilder();
		/*Element myGalleryEle = doc.getElementById("myGallery");
		if (null != myGalleryEle) {
			logger.info(myGalleryEle.outerHtml());
			content.append(myGalleryEle.outerHtml());
		}*/
		for (int i = 0; i < 3; i++) {
			Element boxlEle = boxlEles.get(i);
			if (i == 0) {
				Elements children = boxlEle.children();
				if (children.isEmpty()) {
					throwPageProcessException(page, "score div not found");
				}
				// 去掉网友评分
				for (Element child : children) {
					Element scoreEle = child.getElementById("ctl00_ContentPlaceHolder1_ucSat_div_01");
					if (scoreEle != null) {
						child.remove();
						break;
					}
				}
			}
			content.append(boxlEle.outerHtml());
		}
		if (content.length() == 0) {
			throwPageProcessException(page, "content is empty");
		}
		article.setContent(content.toString());
		
		// 回复日期
		Date replyDate = null;
		
		Elements replyDateEles = doc.getElementsByClass("spreplydate");
		if (replyDateEles.isEmpty()) {
			throwPageProcessException(page, "reply date not found");
//			logger.warn("reply date not found");
		} else {
			String txt = replyDateEles.get(0).ownText();
			Matcher ma = PA_DATE_REPLY.matcher(txt);
			if (!ma.find()) {
				logger.warn("reply date not mactch - {}", txt);
			}
			try {
				replyDate = DF.parse(ma.group(1));
			} catch (ParseException e) {
				logger.warn("reply date", e);
			}
		}
		article.setReplyDate(replyDate);
		
		RequestTask task = page.getRequestTask();
		// 分类
		article.setCategory(task.getCategory());
		// 受理时间
		article.setAcceptDate(task.getAcceptDate());
		// 办结时间
		article.setOverDate(task.getOverDate());
		// URL
		article.setUrl(task.getUrl());
		
		logger.info("pipe start - {}", article.getUrl());
		pipeline.pipe(false, 0, article);
	}

	private void processNavi(HtmlPage<RequestTask> page) throws PageProcessException {
		Document doc = page.getDocument();
		// Elements linkEles = doc.select("#divList > div.listcon >
		// span:nth-child(3) > p > a");
		Elements itemEles = doc.select("#divList > div.listcon");
		if (itemEles.isEmpty()) {
			throwPageProcessException(page, "target not found");
		}
		// Spider<HeaderRequestTask, HtmlPage<HeaderRequestTask>> spider =
		// getSpider();
		for (Element itemEle : itemEles) {
			Elements linkEles = itemEle.select(":root > span:nth-child(3) > p > a");
			if (linkEles.isEmpty()) {
				logger.warn("link not found");
				continue;
			}
			Element linkEle = linkEles.get(0);
			// 获取目标链接
			String url = linkEle.absUrl("href");
			if (url.isEmpty()) {
				logger.warn("empty link");
				continue;
			}
			String title = linkEle.attr("title");
			if (title.isEmpty()) {
				logger.info("{} - {}", linkEle.ownText(), url);
				continue;
			}

			// 分类
			String category = null;
			Elements categoryEles = itemEle.select(":root > span:nth-child(2) > p");
			if (categoryEles.isEmpty()) {
				logger.warn("category not found");
			} else {
				category = categoryEles.get(0).ownText();
				if (category.isEmpty()) {
					logger.warn("category is empty");
					category = null;
				}
			}

			// 受理时间
			Date acceptDate = null;
			Elements acceptDateEles = itemEle.select(":root > span:nth-child(4) > p");
			if (acceptDateEles.isEmpty()) {
				logger.warn("accept date not found");
			} else {
				String txt = acceptDateEles.get(0).ownText();
				if (txt.isEmpty()) {
					logger.warn("accept date is empty");
				} else if (!NODATE.equals(txt)) {
					try {
						acceptDate = DF.parse(txt);
					} catch (ParseException e) {
						logger.warn("accept date", e);
					}
				}
			}

			// 办结时间
			Date overDate = null;
			Elements overDateEles = itemEle.select(":root > span:nth-child(5) > p");
			if (overDateEles.isEmpty()) {
				logger.warn("over date not found");
			} else {
				String txt = overDateEles.get(0).ownText();
				if (txt.isEmpty()) {
					logger.warn("over date is empty");
				} else if (!NODATE.equals(txt)) {
					try {
						overDate = DF.parse(txt);
					} catch (ParseException e) {
						logger.warn("over date", e);
					}
				}
			}

			// 去重
			if (isMD5Duplicate(url)) {
				continue;
			}

			// 添加任务
			if (forward(new RequestTask(url, TASK_TYPE_TARGET, category, acceptDate, overDate))) {
				logger.info("offer target - {}", url);
			}
		}
	}
}
