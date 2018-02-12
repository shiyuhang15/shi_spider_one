package com.hbrb.spider.pipeline;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.hbrb.json.JSArray;
import com.hbrb.json.JSObject;
import com.hbrb.json.JSUtils;
import com.hbrb.spider.ConstantsHome;
import com.hbrb.spider.downloader.ImgDownloader;
import com.hbrb.spider.model.article.Article;
import com.hbrb.spider.model.article.NewsArticle;
import com.hbrb.spider.model.article.SourceType;
import com.hbrb.spider.model.region.City;
import com.hbrb.spider.model.region.Province;
import com.hbrb.spider.service.RegionService;
import com.hbrb.util.ModelUtils;
import com.hbrb.util.PipeUtils;

public class FilePipeline {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FilePipeline.class);
	private static final org.slf4j.Logger targetLogger = org.slf4j.LoggerFactory
			.getLogger("com.cmcc.yuqing.collector.statistics.target");
	private final String[] imageTmpPaths;
	private final OutFileWriter[] writers;
	private final List<Province> provinces;
	private final ImgDownloader downloader;

	public FilePipeline(ImgDownloader downloader, String filePipelinePath) {
		super();
		this.downloader = downloader;
		int[] sts = new int[] {SourceType.OTHER, SourceType.WEB, SourceType.PAPER, SourceType.BLOG, SourceType.BBS, SourceType.WEIBO };
		File outPath = null;
		if (null == filePipelinePath || filePipelinePath.isEmpty()) {
			outPath = new File(ConstantsHome.USER_DIR + File.separator + "out");
		} else {
			File file = new File(filePipelinePath);
			if (file.isAbsolute()) {
				outPath = file;
			} else {
				outPath = new File(ConstantsHome.USER_DIR, filePipelinePath);
			}
		}
		if (!outPath.exists() || !outPath.isDirectory()) {
			if (!outPath.mkdirs()) {
				throw new Error("创建输出目录失败 - " + outPath.getAbsolutePath());
			}
		}
		String pipePath = outPath.getAbsolutePath();
		writers = new OutFileWriter[sts.length];
		for (int i = 0; i < sts.length; i++) {
			writers[i] = new OutFileWriter(pipePath, ModelUtils.sourceType2Name(sts[i]), 100);
		}

		imageTmpPaths = new String[sts.length];
		String parentPath = ConstantsHome.USER_DIR + File.separator + "tmp_image" + File.separatorChar;
		for (int i = 0; i < sts.length; i++) {
			String sourceTypeName = ModelUtils.sourceType2Name(sts[i]);
			File itd = new File(parentPath + sourceTypeName);
			if (!itd.isDirectory()) {
				if (!itd.mkdirs()) {
					throw new Error("创建图片临时目录失败 - " + itd.getAbsolutePath());
				}
			}
			imageTmpPaths[i] = itd.getAbsolutePath();
		}
		
		// 地域
		File regionFile = new File(ConstantsHome.USER_DIR + File.separator + "region.json");
		String regionJson;
		try {
			regionJson = FileUtils.readFileToString(regionFile, Charset.defaultCharset());
		} catch (IOException e) {
			throw new Error("地域读取失败 - " + regionFile.getAbsolutePath(), e);
		}
		JSArray jsProvinces = JSUtils.createJSArray(regionJson);
		int length = jsProvinces.length();
		provinces = new ArrayList<>(length);
		for (int i = 0; i < length; i++) {
			JSObject jsProvince = jsProvinces.getNotNullJSObject(i);
			Province province = new Province();
			province.setName(jsProvince.getNotNullString("name"));
			String provinceFullName = jsProvince.getNotNullString("fullName");
			if (RegionService.HEBEI.equals(provinceFullName)) {
				provinceFullName = RegionService.HEBEI;
			}
			province.setFullName(provinceFullName);
			JSArray jsCities = jsProvince.getNotNullJSArray("cities");
			int clength = jsCities.length();
			List<City> cities = new ArrayList<>(clength);
			for (int j = 0; j < clength; j++) {
				JSObject jsCity = jsCities.getNotNullJSObject(j);
				City city = new City();
				city.setName(jsCity.getNotNullString("name"));
				city.setFullName(jsCity.getNotNullString("fullName"));
				cities.add(city);
			}
			province.setCities(cities);
			provinces.add(province);
		}
	}

	public boolean pipe(boolean pipeLog, int siteTaskId, Article article) {
		judgeRegion(article);
		int index = article.getSourceType();
		// FIXME test
//		List<File> imageFiles = PipeUtils.pipeArticleImage(article, imageTmpPaths[index]);
		PipeUtils.pipeArticleImage(this.downloader, article, imageTmpPaths[index]);
//		if (!Spider.inTestMode()) {
//		}

		try {
			writers[index].writeArticle(article);
			if (pipeLog) {
				logger.info("pipe - {}", article.getUrl());
			}
			return true;
		} catch (IOException e) {
			String url = article.getUrl();
			logger.warn("pipe fail - " + url, e);
			if (null != targetLogger) {
				if (siteTaskId > 0) {
					targetLogger.info("{}\t{}\t{}\t{}\t{}\t{}\t{}", siteTaskId, 0, 0, 0, 0, 0, 1);
				}
			}
			return false;
		}
	}
	
	private void judgeRegion(Article article) {
		String txt;
		if (article instanceof NewsArticle) {
			txt = ((NewsArticle)article).getTitle();
		} else {
			txt = article.getContent();
		}
		
		List<String> region = null;
		for (Province province : this.provinces) {
			List<City> cities = province.getCities();
			boolean contains = false;
			for (City city : cities) {
				if (txt.contains(city.getName())) {
					contains = true;
					region = new ArrayList<>(2);
					region.add(province.getFullName());
					region.add(city.getFullName());
					break;
				}
			}
			if (contains) {
				break;
			} else {
				if (txt.contains(province.getName())) {
					region = new ArrayList<>(1);
					region.add(province.getFullName());
					break;
				}
			}
		}
		if (null != region) {
			article.setRegion(region);
		}
	}

	public void close() throws IOException{
		for (OutFileWriter writer : writers) {
			writer.close();
		}
	}
}
