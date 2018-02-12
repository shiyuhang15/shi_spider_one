package com.hbrb.spider.downloader;

import java.io.File;
import java.io.IOException;

public interface ImgDownloader {
	void zeroCopyDownload(String imgSrc, File destFile) throws IOException;
}
