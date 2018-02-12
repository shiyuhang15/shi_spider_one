package com.hbrb.spider.service.impl;

import java.sql.SQLException;
import java.util.List;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import com.hbrb.spider.dao.DaoFactory;
import com.hbrb.spider.dao.SohuMtUserDao;
import com.hbrb.exception.DataAccessException;
import com.hbrb.exception.ServiceException;
import com.hbrb.spider.model.SohuMtUser;
import com.hbrb.spider.service.SohuMtSpiderTaskService;
import com.hbrb.spider.service.SohuMtUserService;
import com.hbrb.util.JedisUtils;

public class JDBCSohuMtUserService implements SohuMtUserService {
	private final SohuMtUserDao sohuMtUserDao = DaoFactory
			.getSohuMtUserDao();
	@Override
	public void addUsers(List<SohuMtUser> users) throws ServiceException {
		try {
			sohuMtUserDao.addUsers(users);
		} catch (SQLException | DataAccessException e) {
			throw new ServiceException("搜狐公众号入数据库异常", e);
		}
		try (Jedis jedis = JedisUtils.createJedis()) {
			Pipeline pipe = jedis.pipelined();
			for (SohuMtUser user : users) {
				pipe.rpush(SohuMtSpiderTaskService.KEY_PREFIX_QUEUE + user.getLiveness(),
						user.getId());
			}
			pipe.sync();
		} catch (Exception e) {
			throw new ServiceException("搜狐公众号入redis异常", e);
		}
	}

}
