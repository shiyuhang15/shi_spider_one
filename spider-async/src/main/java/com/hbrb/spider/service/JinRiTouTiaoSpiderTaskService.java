package com.hbrb.spider.service;

import java.util.List;

import com.hbrb.exception.ServiceException;
import com.hbrb.spider.model.Toutiaohao;

public interface JinRiTouTiaoSpiderTaskService {
	String KEY_PREFIX_QUEUE = "queue_task_spider_web_toutiao_";
	List<Toutiaohao> tasksForSpider(String spiderId, int limit) throws ServiceException;
}
