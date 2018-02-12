package com.hbrb.spider.tool.controller;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import javax.naming.ConfigurationException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.xml.sax.SAXException;

import com.hbrb.spider.downloader.sync.HttpClientDownloader;
import com.hbrb.spider.exception.PageProcessException;
import com.hbrb.spider.exception.TemplateParseException;
import com.hbrb.spider.model.RawResult;
import com.hbrb.spider.model.SpiderConfig;
import com.hbrb.spider.model.page.HtmlPage;
import com.hbrb.spider.model.task.GenericRequestTask;
import com.hbrb.spider.model.template.PageTemplate;
import com.hbrb.spider.pageprocessor.PageProcessor;
import com.hbrb.spider.pageprocessor.PageProcessorFactory;
import com.hbrb.spider.service.impl.CachedJDBCPageTemplateService;
import com.hbrb.spider.tool.pageprocessor.TestTemplatePageProcessor;
import com.hbrb.spider.tool.view.MainFrame;
import com.hbrb.util.JsoupUtils;

public class MainController {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MainController.class);
	private CachedJDBCPageTemplateService cachedJDBCPageTemplateService = new CachedJDBCPageTemplateService();
	private PageTemplate template;
	private int sourceType = 1;
	private String charsetName;
	private MainFrame mainFrame;
	private HttpClientDownloader downloader;
	public HttpClientDownloader getDownloader() {
		return downloader;
	}

	private PageProcessor<GenericRequestTask, HtmlPage<GenericRequestTask>> pageProcessor;

	public MainController() throws ParserConfigurationException, SAXException, IOException, ConfigurationException {
		super();
		SpiderConfig config = new SpiderConfig();
		downloader = new HttpClientDownloader(config, true, null, null);
		
		pageProcessor = PageProcessorFactory.wrap(new TestTemplatePageProcessor(this));
		mainFrame = new MainFrame(this);
	}

	public void executeTaskAction(String url) {
		GenericRequestTask task = new GenericRequestTask(url);
		logger.info("download start - {}", url);
		RawResult res = downloader.execute(task);
		if (null != res) {
			HtmlPage<GenericRequestTask> htmlPage = new HtmlPage<>(task, JsoupUtils.parseByteData(res.getRawData(),
					this.charsetName == null ? res.getCharsetName() : this.charsetName, res.getLastRedirectLocation()));
			try {
				pageProcessor.process(htmlPage);
			} catch (PageProcessException e) {
				logger.error("未处理异常", e);
			}
		}
	}

	public void showMainFrame() {
		mainFrame.setVisible(true);
	}

	public void updateSite(int retryTimes, int sleepTime, String charset, int sourceType) {
		this.sourceType = sourceType;
		this.charsetName = charset;
		// 暂不支持重试次数和请求间隔的动态设置
	}

	public int getSourceType() {
		return sourceType;
	}

	public String loadTemplate(String path) throws IOException, DocumentException, TemplateParseException {
		this.template = null;
		String content = FileUtils.readFileToString(new File(path), Charset.defaultCharset());
		this.template = cachedJDBCPageTemplateService.buildTemplate(DocumentHelper.parseText(content));
		return content;
	}

	public PageTemplate getTemplate() {
		return this.template;
	}

	public void closeAction() {
		try {
			this.downloader.close();
		} catch (IOException e) {
			logger.warn("downloader close failed", e);
		}
	}
}
