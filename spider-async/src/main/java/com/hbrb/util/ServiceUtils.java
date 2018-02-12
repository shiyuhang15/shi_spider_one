package com.hbrb.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.hbrb.exception.ConfigError;
import com.hbrb.spider.ConstantsHome;

public class ServiceUtils {
	public static String getHttpServiceBase() {
		File configFile = new File(ConstantsHome.USER_DIR + File.separatorChar + "service_http");
		if (!configFile.isFile()) {
			throw new ConfigError("service_http file not found - " + configFile.getAbsolutePath());
		}
		List<String> lines;
		try {
			lines = FileUtils.readLines(configFile, Charset.defaultCharset());
		} catch (IOException e) {
			throw new ConfigError("service_http load faied", e);
		}
		if (lines.size() < 1) {
			throw new ConfigError("service_http lines.size() < 1");
		}
		String line = lines.get(0);
		if (line.isEmpty()) {
			throw new ConfigError("service_http line.isEmpty()");
		}
		return line;
	}
}
