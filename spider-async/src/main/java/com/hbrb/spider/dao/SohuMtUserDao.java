package com.hbrb.spider.dao;

import java.sql.SQLException;
import java.util.List;

import com.hbrb.exception.DataAccessException;
import com.hbrb.spider.model.SohuMtUser;

public interface SohuMtUserDao extends BasicDao {
	void addUsers(List<SohuMtUser> users) throws SQLException, DataAccessException;
}
