package com.hbrb.spider.service;

import java.util.List;

import com.hbrb.exception.ServiceException;
import com.hbrb.spider.model.WeiboUser;

public interface WeiboUser2Service {
	List<WeiboUser> retrieveAll() throws ServiceException;
}
