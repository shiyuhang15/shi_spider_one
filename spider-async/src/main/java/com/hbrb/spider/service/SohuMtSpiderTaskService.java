package com.hbrb.spider.service;

import java.util.List;

import com.hbrb.exception.ServiceException;
import com.hbrb.spider.model.SohuMtUser;

public interface SohuMtSpiderTaskService {
	String KEY_PREFIX_QUEUE = "queue_task_spider_web_sohu_mt_";
	List<SohuMtUser> tasksForSpider(String spiderId, int limit) throws ServiceException;
}
