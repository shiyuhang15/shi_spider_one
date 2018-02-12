package com.hbrb.spider.service;

import java.util.List;

import com.hbrb.exception.ServiceException;
import com.hbrb.spider.model.Toutiaohao;

public interface ToutiaohaoService {
	void addToutiaohaos(List<Toutiaohao> tths) throws ServiceException;
}
