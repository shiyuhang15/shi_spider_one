package com.hbrb.spider.downloader.sync;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.CookieStore;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicHeader;
import org.jsoup.helper.DataUtil;

import com.hbrb.exception.ConfigError;
import com.hbrb.spider.ConstantsHome;
import com.hbrb.spider.downloader.EmptyCookieStore;
import com.hbrb.spider.downloader.ImgDownloader;
import com.hbrb.spider.exception.UnacceptableResponseException;
import com.hbrb.spider.model.RawResult;
import com.hbrb.spider.model.SpiderConfig;
import com.hbrb.spider.model.task.GenericRequestTask;
import com.hbrb.spider.model.task.RequestTask;

public class HttpClientDownloader implements ImgDownloader, Closeable {
	private static final int LIMIT_CONTENT_LENGTH = 10485760;
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HttpClientDownloader.class);
	private final CloseableHttpClient client;
	private static final ResponseHandler<RawResult> responseHandler = new  ResponseHandler<RawResult>(){
		@Override
		public RawResult handleResponse(HttpResponse response) throws IOException {
			int status = response.getStatusLine().getStatusCode();
			logger.info("sc:{}", status);
			/*Header[] allHeaders = response.getAllHeaders();
			for (Header header : allHeaders) {
				logger.info(header.toString());
			}*/
			
			if (status != HttpStatus.SC_OK) {
				throw new UnacceptableResponseException();
			}
			HttpEntity entity = response.getEntity();
			ByteBuffer rawData = DataUtil.readToByteBuffer(entity.getContent(), 0);
			
			String charsetName = null;
			ContentType contentType = ContentType.get(entity);
			if (null != contentType) {
				Charset charset = contentType.getCharset();
				if (null != charset) {
					charsetName = charset.name();
				}
			}

			return new RawResult(status, rawData, charsetName);
		}
	};
	
	public HttpClientDownloader(SpiderConfig config, boolean disableCookie, String useragent, List<Header> defaultHeaders) {
		HttpClientBuilder builder = HttpClients.custom();
		if (null == useragent) {
			useragent = ConstantsHome.USER_AGENT;
		}
		builder.setUserAgent(useragent);
		CookieStore cookieStore;
		String cookieSpecs;
		if (disableCookie) {
			cookieSpecs = CookieSpecs.IGNORE_COOKIES;
			cookieStore = new EmptyCookieStore();
		} else {
			cookieSpecs = CookieSpecs.DEFAULT;
			cookieStore = new BasicCookieStore();
			File cookiesDir = new File(ConstantsHome.USER_DIR + File.separatorChar + "cookies");
			if (cookiesDir.isDirectory()) {
				String[] list = cookiesDir.list();
				for (String domain : list) {
					File domainFile = new File(cookiesDir, domain);
					List<String> lines;
					try {
						lines = FileUtils.readLines(domainFile, Charset.defaultCharset());
					} catch (IOException e) {
						throw new RuntimeException("cookie文件读取失败 - " + domainFile.getAbsolutePath(), e);
					}
					for (String line : lines) {
						int indexOfSep = line.indexOf(';');
						String path = null;
						String body;
						if (indexOfSep > 0) {
							body = line.substring(0, indexOfSep);
							path = line.substring(indexOfSep + 1);
						} else {
							body = line;
						}
						int indexOfEq = body.indexOf('=');
						if (indexOfEq == -1) {
							throw new ConfigError("cookie配置错误 - " + line);
						}
						
						BasicClientCookie cookie = new BasicClientCookie(body.substring(0, indexOfEq),
								body.substring(indexOfEq + 1));
						cookie.setDomain(domain);
						cookie.setPath(path);
						cookieStore.addCookie(cookie);
					}
				}
			}
		}
		
		builder.setDefaultCookieStore(cookieStore);
		
		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
		cm.setMaxTotal(config.getConnectionMaxTotal());
		cm.setDefaultMaxPerRoute(config.getConnectionDefaultMaxPerRoute());
		builder.setConnectionManager(cm);
		
		RequestConfig defaultRequestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(config.getConnectionRequestTimeout() * 1000)
				.setConnectTimeout(config.getConnectTimeout() * 1000).setSocketTimeout(config.getSoTimeout() * 1000)
				.setCookieSpec(cookieSpecs)
				.build();
		builder.setDefaultRequestConfig(defaultRequestConfig);
		
		if (null == defaultHeaders) {
			defaultHeaders = new ArrayList<>(2);
			defaultHeaders.add(new BasicHeader("Accept", "text/html, application/xhtml+xml, image/jxr, */*"));
			defaultHeaders.add(new BasicHeader("Accept-Language", "zh-CN"));
		}
        builder.setDefaultHeaders(defaultHeaders);
        
        client = builder.build();
	}
	
	public RawResult execute(GenericRequestTask requestTask, HttpClientContext context) {
		if (null == context) {
			context = HttpClientContext.create();
		}
		HttpUriRequest req;
		switch (requestTask.getMethod() ) {
		case GenericRequestTask.METHOD_POST:
			HttpPost post = new HttpPost(requestTask.getUrl());
			post.setEntity(requestTask.getEntity());
			req = post;
			break;
		case GenericRequestTask.METHOD_PUT:
			HttpPut put = new HttpPut(requestTask.getUrl());
			put.setEntity(requestTask.getEntity());
			req = put;
			break;
		default:
			req = new HttpGet(requestTask.getUrl());
			break;
		}
		Map<String, String> headers = requestTask.getHeaders();
		if (null != headers && !headers.isEmpty()) {
			for (Entry<String, String> header : headers.entrySet()) {
				req.addHeader(header.getKey(), header.getValue());
			}
		}
		
		try {
			RawResult res = this.client.execute(req, responseHandler, context);
			resolve(res, context, req);
			return res;
		} catch (Throwable ex) {
			handleRequestException(requestTask, ex);
		}
		return null;
	}
	public RawResult execute(GenericRequestTask requestTask) {
		return execute(requestTask, null);
	}
	
	private void resolve(RawResult res, HttpClientContext context, HttpUriRequest req) throws URISyntaxException {
		res.setLastRedirectLocation(
				URIUtils.resolve(req.getURI(), context.getTargetHost(), context.getRedirectLocations()).toString());
	}
	
	private void handleRequestException(RequestTask requestTask, Throwable ex) {
		if (ex instanceof UnacceptableResponseException) {
			if (null != ex.getMessage()) {
				logger.info("{} - {}", ex.getMessage(), requestTask.getUrl());
			}
		} else {
			if (ex instanceof UnknownHostException) {
				logger.warn("UnknownHost - {}", requestTask.getUrl());
			} else if (ex instanceof SocketTimeoutException) {
				logger.warn("SocketTimeout - {}", requestTask.getUrl());
			} else {
				logger.warn("request failed - " + requestTask.getUrl(), ex);
			}
		}
	}
	
	@Override
	public void close() throws IOException {
		if (null != client) {
			client.close();
		}
	}

	@Override
	public void zeroCopyDownload(String imgSrc, File destFile) throws IOException {
		HttpGet httpGet = new HttpGet(imgSrc);
		CloseableHttpResponse response = this.client.execute(httpGet);
		try {
			int status = response.getStatusLine().getStatusCode();
			if (status != HttpStatus.SC_OK) {
				throw new UnacceptableResponseException("statusCode:" + status);
			}
		    HttpEntity entity = response.getEntity();
		    long contentLength = entity.getContentLength();
		    if (contentLength <= 0) {
				
			}
		    // TODO 丢弃小于10k的图片
			InputStream instream = entity.getContent();
		    if (instream != null) {
		    	try (FileOutputStream outstream = new FileOutputStream(destFile)) {
		    		long count = 0;
		    		int n;
		    		byte[] buffer = new byte[4096];
		    		while (-1 != (n = instream.read(buffer))) {
		    			count += n;
		    			if (count > LIMIT_CONTENT_LENGTH) {
		    				throw new UnacceptableResponseException("response too long: " + count);
		    			}
		    			outstream.write(buffer, 0, n);
		    		}
		    	} finally {
		    		instream.close();
		    	}
		    }
		} finally {
		    response.close();
		}
	}
}
