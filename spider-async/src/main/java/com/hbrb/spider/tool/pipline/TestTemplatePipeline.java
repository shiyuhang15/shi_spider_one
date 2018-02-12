package com.hbrb.spider.tool.pipline;

import java.io.File;
import java.util.Collection;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.jsoup.Jsoup;

import com.hbrb.spider.ConstantsHome;
import com.hbrb.spider.downloader.ImgDownloader;
import com.hbrb.spider.model.article.NewsArticle;
import com.hbrb.util.PipeUtils;

public class TestTemplatePipeline {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestTemplatePipeline.class);
	private final String pipePath;
	private final ImgDownloader downloader;

	public TestTemplatePipeline(ImgDownloader downloader) {
		super();
		this.downloader = downloader;
		pipePath = ConstantsHome.USER_DIR + File.separator + "test";
		File pipeDir = new File(pipePath);
		if (!pipeDir.isDirectory()) {
			if (!pipeDir.mkdirs()) {
				throw new Error("创建输出目录失败 - " + pipeDir.getAbsolutePath());
			}
		}
	}
	
	public void process(NewsArticle article) {
		if (null == article) {
			return;
		}
		logger.info("pipe start - {}", article.getUrl());
		IOFileFilter fileFilter = new IOFileFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return false;
			}
			
			@Override
			public boolean accept(File file) {
				if (file.isDirectory()) {
					return false;
				}
				String name = file.getName();
				if (name.endsWith(".out")) {
					return false;
				}
				return true;
			}
		};
		Collection<File> files = FileUtils.listFiles(new File(pipePath), fileFilter, null);
		for (File file : files) {
			file.delete();
		}
		
		article.setCreateTime(new Date());
		
		PipeUtils.pipeArticleImage(this.downloader, article, pipePath);
		
		String content = article.getContent();
		if (null != content && !content.isEmpty()) {
			logger.info("raw content - {}", Jsoup.parse(content).text());
		}
		logger.info(article.toString());
	}
}
