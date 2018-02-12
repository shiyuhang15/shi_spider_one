package com.hbrb.spider.model.task;

public class NaviTask extends TemplateTask {
	private long id;
	private String name;
	/**
	 * 最近一次采集到有效新闻的时间
	 */
	private long workedTime;
	private boolean isSingle;
	
	public NaviTask(String url) {
		super(url);
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getWorkedTime() {
		return workedTime;
	}

	public void setWorkedTime(long workedTime) {
		this.workedTime = workedTime;
	}

	public boolean isSingle() {
		return isSingle;
	}

	public void setSingle(boolean isSingle) {
		this.isSingle = isSingle;
	}
}
