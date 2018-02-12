package com.hbrb.spider.model;

import com.hbrb.spider.ConstantsHome;

public class SpiderConfig {
	private int spiderId;
	private String name;
	private int connectTimeout = 10;
	private int soTimeout = 10;
	private int reactorConnectTimeout = 10;
	private int reactorSoTimeout = 10;
	private int connectionMaxTotal = 256;
	private int connectionDefaultMaxPerRoute = 1;
	private int connectionRequestTimeout = 0;
	private String userAgent = ConstantsHome.USER_AGENT;
	private String charset;
	private int retryTimes = 0;
	private int interval = 0;
	private int requestInterval = 0;
	private int cycleRetryTimes = 3;
	private int taskLimit = Integer.MAX_VALUE;
	private String redisHost = "localhost";
	private int redisPort = 6379;

	public int getSpiderId() {
		return spiderId;
	}

	public void setSpiderId(int spiderId) {
		this.spiderId = spiderId;
	}

	public int getConnectionDefaultMaxPerRoute() {
		return connectionDefaultMaxPerRoute;
	}

	public int getConnectionMaxTotal() {
		return connectionMaxTotal;
	}

	public int getConnectionRequestTimeout() {
		return connectionRequestTimeout;
	}

	public int getConnectTimeout() {
		return connectTimeout;
	}

	public int getReactorConnectTimeout() {
		return reactorConnectTimeout;
	}

	public int getReactorSoTimeout() {
		return reactorSoTimeout;
	}

	public int getSoTimeout() {
		return soTimeout;
	}

	public void setConnectionDefaultMaxPerRoute(int connectionDefaultMaxPerRoute) {
		this.connectionDefaultMaxPerRoute = connectionDefaultMaxPerRoute;
	}

	public void setConnectionMaxTotal(int connectionMaxTotal) {
		this.connectionMaxTotal = connectionMaxTotal;
	}

	public void setConnectionRequestTimeout(int connectionRequestTimeout) {
		this.connectionRequestTimeout = connectionRequestTimeout;
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public void setReactorConnectTimeout(int reactorConnectTimeout) {
		this.reactorConnectTimeout = reactorConnectTimeout;
	}

	public void setReactorSoTimeout(int reactorSoTimeout) {
		this.reactorSoTimeout = reactorSoTimeout;
	}

	public void setSoTimeout(int soTimeout) {
		this.soTimeout = soTimeout;
	}

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public int getRetryTimes() {
		return retryTimes;
	}

	public void setRetryTimes(int retryTimes) {
		this.retryTimes = retryTimes;
	}

	public int getCycleRetryTimes() {
		return cycleRetryTimes;
	}

	public void setCycleRetryTimes(int cycleRetryTimes) {
		this.cycleRetryTimes = cycleRetryTimes;
	}

	public int getTaskLimit() {
		return taskLimit;
	}

	public void setTaskLimit(int taskLimit) {
		this.taskLimit = taskLimit;
	}

	public String getRedisHost() {
		return redisHost;
	}

	public void setRedisHost(String redisHost) {
		this.redisHost = redisHost;
	}

	public int getRedisPort() {
		return redisPort;
	}

	public void setRedisPort(int redisPort) {
		this.redisPort = redisPort;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public int getInterval() {
		return interval;
	}

	public void setInterval(int interval) {
		this.interval = interval;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getRequestInterval() {
		return requestInterval;
	}

	public void setRequestInterval(int requestInterval) {
		this.requestInterval = requestInterval;
	}
}
