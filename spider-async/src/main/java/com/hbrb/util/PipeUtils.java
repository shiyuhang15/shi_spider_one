package com.hbrb.util;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hbrb.spider.downloader.ImgDownloader;
import com.hbrb.spider.model.article.Article;
import com.hbrb.spider.model.article.ArticleImg;
import com.hbrb.spider.model.article.NewsArticle;
import com.hbrb.spider.model.article.SourceType;
import com.hbrb.spider.model.article.WeiboArticle;

public class PipeUtils {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PipeUtils.class);
	private final static Pattern PA_SRC_IMG = Pattern.compile("<img[^>]* src=\"([^\"]+)\"");

	// XXX 不下载了
	/*private static boolean pipeImg(ImgDownloader downloader, String imgSrc, File destFile) {
		return true;
		if (ProcessUtils.containsIllegalCharacter(imgSrc)) {
			return false;
		}
		imgSrc = imgSrc.replace(" ", "%20");
		logger.info("pipe image - {} -> {}", imgSrc, destFile.getName());
		try {
			downloader.zeroCopyDownload(imgSrc, destFile);
			return true;
		} catch (IOException e) {
			logger.warn("pipe image failed - " + imgSrc, e);
			return false;
		}
	}*/

	public static void pipeArticleImage(ImgDownloader downloader, Article article, String parentPath) {
//		List<File> imageFiles = new ArrayList<>();
		// 正文外的图片
		switch (article.getSourceType()) {
		case SourceType.WEIBO:
			WeiboArticle weiboArticle = (WeiboArticle)article;
			List<String> imgSrcs = weiboArticle.getImgSrcs();
			if (null != imgSrcs && !imgSrcs.isEmpty()) {
				pipeWeiboImage(downloader, article, parentPath, imgSrcs/*, imageFiles*/);
			}
			WeiboArticle original = weiboArticle.getOrigin();
			if (null != original) {
				imgSrcs = original.getImgSrcs();
				if (null != imgSrcs && !imgSrcs.isEmpty()) {
					pipeWeiboImage(downloader, article, parentPath, imgSrcs/*, imageFiles*/);
				}
			}
			break;
		case SourceType.OTHER:
			break;
		default:
			List<ArticleImg> imgs = ((NewsArticle)article).getImgs();
			if (null != imgs && !imgs.isEmpty()) {
				for (ArticleImg img : imgs) {
					// XXX GG
					String imgSrc = img.getSrc();
					try {
						String resolve = ProcessUtils.resolve(article.getUrl(), imgSrc);
						if (resolve.isEmpty()) {
							logger.warn("imgSrc error - {}" + imgSrc);
							continue;
						}
						img.setSrc(resolve);
					} catch (Throwable e) {
						logger.warn("process url error - " + imgSrc, e);
						continue;
					}
					
					
					/*String imgName = MyDataSource.generateId();
					File imageFile = new File(parentPath, imgName);
					String imgSrc = img.getSrc();
					try {
						String resolve = ProcessUtils.resolve(article.getUrl(), imgSrc);
						if (resolve.isEmpty()) {
							logger.warn("imgSrc error - {}" + imgSrc);
							continue;
						}
						imgSrc = ProcessUtils.processUrl(resolve);
					} catch (IOException e) {
						logger.warn("process url error - " + imgSrc, e);
						continue;
					}
					if (imgSrc.isEmpty()) {
						continue;
					}
					if (pipeImg(downloader, imgSrc, imageFile)) {
						img.setSrc(imgName);
					}*/
				}
			}
			break;
		}

		// 正文中的图片
		String content = article.getContent();
		if (null != content && !content.isEmpty()) {
			Matcher ma = PA_SRC_IMG.matcher(content);
			StringBuilder sb = new StringBuilder();
			int end = 0;
			while (ma.find()) {
				String imgSrc = ma.group(1);
				// http://www.sjzjgbx.gov.cn/col/1506589357990/2017/11/04/1509779527778.html
				// 这个页面上有src='data:开头的img
				// http://www.sjz119.com/tongzhigonggao/289.html
				// 这个页面上有src='file:开头的img
				if (imgSrc.startsWith("data:") || imgSrc.startsWith("file:")) {
					continue;
				}
				int imgSrcLength = imgSrc.length();
				
				// XXX GG
				String resolve = ProcessUtils.resolve(article.getUrl(), imgSrc);
				if (resolve.isEmpty()) {
					logger.warn("imgSrc error - {}" + imgSrc);
					continue;
				}
				int start = ma.start(1);
				sb.append(content.substring(end, start)).append(resolve);
				end = start + imgSrcLength;
				
				
				/*try {
					String resolve = ProcessUtils.resolve(article.getUrl(), imgSrc);
					if (resolve.isEmpty()) {
						logger.warn("imgSrc error - {}" + imgSrc);
						continue;
					}
					imgSrc = ProcessUtils.processUrl(resolve);
				} catch (IOException e) {
					logger.warn("process url error - " + imgSrc, e);
					continue;
				}
				String imgName = MyDataSource.generateId();
				File imageFile = new File(parentPath, imgName);
				if (pipeImg(downloader, imgSrc, imageFile)) {
					// 用保存到本地的图片名称替换正文中的原始图片路径
					int start = ma.start(1);
					sb.append(content.substring(end, start)).append(imgName);
					end = start + imgSrcLength;
				}*/
			}
			if (0 != end) {
				sb.append(content.substring(end));
				article.setContent(sb.toString());
			}
		}
//		return imageFiles.isEmpty() ? null : imageFiles;
	}

	private static void pipeWeiboImage(ImgDownloader downloader, Article article, String parentPath,
			List<String> imgSrcs/*, List<File> imageFiles*/) {
		// XXX GG
		/*int size = imgSrcs.size();
		for (int i = 0; i < size; i++) {
			String imgName = MyDataSource.generateId();
			// XXX 待测试:如果这里的imgSrc如果不是绝对路径需要处理
			String imgSrc = imgSrcs.get(i);
			File imageFile = new File(parentPath, imgName);
			if (pipeImg(downloader, imgSrc, imageFile)) {
				imgSrcs.set(i, imgName);
			}
		}*/
	}
}
