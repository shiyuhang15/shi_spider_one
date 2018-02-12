package com.hbrb.spider.service;

import com.hbrb.exception.ServiceException;
import com.hbrb.spider.model.SpiderConfig;

public interface SpiderService {
	SpiderConfig buildSpiderConfig(int[] spiderIds) throws ServiceException;
	boolean updateSpiderStatus(String spiderId, int status) throws ServiceException;
}
