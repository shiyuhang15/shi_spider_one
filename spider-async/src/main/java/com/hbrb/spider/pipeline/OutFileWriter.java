package com.hbrb.spider.pipeline;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Random;

import org.apache.commons.io.FileUtils;

import com.alibaba.fastjson.JSON;
import com.hbrb.spider.ConstantsHome;
import com.hbrb.spider.model.article.Article;

public class OutFileWriter {
	/**
	 * 每个文件中所包含的最大文件数
	 */
	private int size;

	/**
	 * 存放目录，不包括日期部分
	 */
	private String dir;
	private final File tmpDir;

	/**
	 * 计数器
	 */
	private int index = 1;

	/**
	 * 爬虫类型 网页模板爬虫 webspider 自动化模板爬虫 webautospider APP爬虫 appspider 微博爬虫
	 * weibospider 微信爬虫 weixinspider 流媒体爬虫 tvstream
	 */
	private String recType;

	private FileOutputStream out;

	/**
	 * 当前文件的绝对路径
	 */
	private String currentFilePath;

	public OutFileWriter(String dir, String recType, int size) {
		tmpDir = new File(ConstantsHome.USER_DIR + File.separator
				+ "tmp" + File.separator + recType);
		if (!tmpDir.isDirectory()) {
			if (!tmpDir.mkdirs()) {
				throw new Error("创建临时目录失败 - " + tmpDir.getAbsolutePath());
			}
		}
		if (!dir.endsWith("/") && !dir.endsWith("\\")) {
			dir += File.separator;
		}
		this.dir = dir;
		this.recType = recType;
		this.size = size;
	}

	/**
	 * 将article对象写入到out文件
	 * 
	 * @param article
	 * @param imageFiles 
	 * @throws IOException
	 */
	public synchronized void writeArticle(Article article) throws IOException {
		if (out == null || index > size) {
			close();
			File file = createFile();
			out = new FileOutputStream(file);
			currentFilePath = file.getAbsolutePath();
		}

		out.write((JSON.toJSONString(article) + '\n').getBytes());
		index++;
	}

	/**
	 * 构建文件名中的日期部分（yyyyMMddHHmmss格式）
	 * 
	 * @return
	 */
	private String createFileNameDatePart() {
		Date date = new Date();
		SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMddHHmmss");
		return fmt.format(date);
	}

	/**
	 * 获取计算机名称，文件名中的一个组成部分
	 * 
	 * @return
	 */
	private String getPcName() {
		try {
			InetAddress addr = InetAddress.getLocalHost();
			// 获取本机计算机名称
			String hostName = addr.getHostName().toString();
			return hostName;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * 获取四位随机数，文件名中的一个组成部分
	 * 
	 * @return
	 */
	private String getRandomPart() {
		Random random = new Random();
		int value = random.nextInt(10000);
		DecimalFormat fmt = new DecimalFormat("0000");
		return fmt.format(value);
	}

	/**
	 * 获取当前进程id，文件名中的一个组成部分
	 * 
	 * @return
	 */
	private String getProcessId() {
		String name = ManagementFactory.getRuntimeMXBean().getName();
		// get pid
		String pid = name.split("@")[0];
		long threadId = Thread.currentThread().getId();
		return pid + "_" + threadId;
	}

	/**
	 * 创建out文件File实例
	 * 
	 * @return
	 */
	private File createFile() {
		// 拼接完整的文件名
		String fileName = String.format("%s_%s_%s_%s_%s.out", createFileNameDatePart(), getRandomPart(), recType, getPcName(), getProcessId());

		File file = new File(tmpDir, fileName);

		// 如果文件名已经存在则重新创建文件
		if (file.exists()) {
			file = createFile();
		}
		return file;
	}

	/**
	 * 关闭输出流
	 * 
	 * @throws IOException
	 */
	public synchronized void close() throws IOException {
		if (out == null){
			return;
		}
		out.close();
		out = null;
		if (currentFilePath != null) {
			File tmpFile = new File(currentFilePath);
			if(tmpFile.exists()){
				File destDir = new File(dir + File.separatorChar + recType);
				if (!destDir.exists()) {
					destDir.mkdirs();
				}
				Collection<File> listFiles = FileUtils.listFiles(tmpFile.getParentFile(), null, false);
				File tmpLock = new File(destDir, "tmp.lock");
				tmpLock.createNewFile();
				try {
					for (File file : listFiles) {
						FileUtils.moveFileToDirectory(file, destDir, false);
					}
				} finally {
					tmpLock.delete();
				}
			}
		}
		index = 1;
	}
}
