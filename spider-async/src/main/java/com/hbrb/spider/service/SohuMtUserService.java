package com.hbrb.spider.service;

import java.util.List;

import com.hbrb.exception.ServiceException;
import com.hbrb.spider.model.SohuMtUser;

public interface SohuMtUserService {
	void addUsers(List<SohuMtUser> users) throws ServiceException;
}
