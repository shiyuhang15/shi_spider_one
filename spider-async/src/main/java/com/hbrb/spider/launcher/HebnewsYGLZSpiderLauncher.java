package com.hbrb.spider.launcher;

import java.io.File;
import java.io.IOException;

import com.hbrb.spider.ConstantsHome;
import com.hbrb.spider.Spider;
import com.hbrb.spider.SyncSpider;
import com.hbrb.spider.constraint.RedisBasedUniqueConstraint;
import com.hbrb.spider.constraint.RedisSetBasedUniqueConstraint;
import com.hbrb.spider.downloader.sync.HtmlPageBuilder;
import com.hbrb.spider.downloader.sync.HttpClientDownloader;
import com.hbrb.spider.downloader.sync.PageAdapter;
import com.hbrb.spider.model.SpiderConfig;
import com.hbrb.spider.model.page.HtmlPage;
import com.hbrb.spider.pageprocessor.HebnewsYGLZPageProcessor;
import com.hbrb.spider.pageprocessor.PageProcessorFactory;
import com.hbrb.spider.pipeline.FilePipeline;

public class HebnewsYGLZSpiderLauncher implements SpiderLauncher {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HebnewsYGLZSpiderLauncher.class);

	@Override
	public void launchSpider(SpiderConfig config) {
		HttpClientDownloader downloader = new HttpClientDownloader(config, true, null, null);
		PageAdapter<HebnewsYGLZPageProcessor.RequestTask, HtmlPage<HebnewsYGLZPageProcessor.RequestTask>> ghDownloader = new PageAdapter<>(
				downloader, new HtmlPageBuilder<HebnewsYGLZPageProcessor.RequestTask>(config.getCharset()));
		FilePipeline pipeline = new FilePipeline(downloader, null);
		HebnewsYGLZPageProcessor pageProcessor = new HebnewsYGLZPageProcessor(pipeline);
		Spider<HebnewsYGLZPageProcessor.RequestTask, HtmlPage<HebnewsYGLZPageProcessor.RequestTask>> spider = new SyncSpider<>(
				config,
				Spider.inTestMode() ? new RedisSetBasedUniqueConstraint("set_yglz") : new RedisBasedUniqueConstraint(),
				ghDownloader, null, PageProcessorFactory.wrap(pageProcessor));
		File block = new File(ConstantsHome.USER_DIR + File.separatorChar + "block");
		try {
			for (;;) {
				spider.addTask(new HebnewsYGLZPageProcessor.RequestTask("http://yglz.tousu.hebnews.cn/l--30,70,80,99-",
						HebnewsYGLZPageProcessor.TASK_TYPE_NAVI));
				spider.run();
				try {
					pipeline.close();
				} catch (IOException e) {
					logger.error("pipeline close 失败", e);
					break;
				}
				if (block.exists()) {
					logger.info("met block");
					break;
				}
				// 间息
				if (config.getInterval() > 0) {
					logger.info("interval sleeping - {}", config.getInterval());
					try {
						Thread.sleep(config.getInterval() * 1000);
					} catch (InterruptedException e) {
					}
					if (block.exists()) {
						logger.info("met block");
						break;
					}
				}
			}
		} finally {
			try {
				pipeline.close();
			} catch (IOException e) {
				logger.error("pipeline close failed", e);
			} finally {
				spider.close();
			}
		}
	}

	public static void main(String[] args) throws IOException {
		BasicSpiderLauncher.launch(new RedisBasedSpiderLauncher(new HebnewsYGLZSpiderLauncher()), args);
	}
}
