package com.hbrb.spider.service;

import java.util.List;

import com.hbrb.exception.ServiceException;

public interface WeiboVisitorCookieService {
	List<String> getAllCookies() throws ServiceException;
}
