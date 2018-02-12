package com.hbrb.spider.service.impl;

import java.sql.SQLException;
import java.util.List;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import com.hbrb.spider.dao.DaoFactory;
import com.hbrb.spider.dao.ToutiaohaoDao;
import com.hbrb.exception.DataAccessException;
import com.hbrb.exception.ServiceException;
import com.hbrb.spider.model.Toutiaohao;
import com.hbrb.spider.service.JinRiTouTiaoSpiderTaskService;
import com.hbrb.spider.service.ToutiaohaoService;
import com.hbrb.util.JedisUtils;

public class JDBCToutiaohaoService implements ToutiaohaoService {

	private final ToutiaohaoDao toutiaohaoDao = DaoFactory
			.getToutiaohaoDao();
	@Override
	public void addToutiaohaos(List<Toutiaohao> tths) throws ServiceException {
		try {
			toutiaohaoDao.addToutiaohaos(tths);
		} catch (SQLException | DataAccessException e) {
			throw new ServiceException("头条号入数据库异常", e);
		}
		try (Jedis jedis = JedisUtils.createJedis()) {
			Pipeline pipe = jedis.pipelined();
			for (Toutiaohao tth : tths) {
				pipe.rpush(JinRiTouTiaoSpiderTaskService.KEY_PREFIX_QUEUE + tth.getLiveness(),
						String.valueOf(tth.getId()));
			}
			pipe.sync();
		} catch (Exception e) {
			throw new ServiceException("头条号入redis异常", e);
		}
	}
}
