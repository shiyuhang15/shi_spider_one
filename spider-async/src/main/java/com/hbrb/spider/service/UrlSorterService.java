package com.hbrb.spider.service;

import com.hbrb.exception.ServiceException;

public interface UrlSorterService {
	void sort(long ruleTaskId) throws ServiceException;

	void release(long frtid) throws ServiceException;
}
