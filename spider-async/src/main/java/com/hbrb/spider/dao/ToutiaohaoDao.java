package com.hbrb.spider.dao;

import java.sql.SQLException;
import java.util.List;

import com.hbrb.exception.DataAccessException;
import com.hbrb.spider.model.Toutiaohao;

public interface ToutiaohaoDao extends BasicDao {
	void addToutiaohaos(List<Toutiaohao> tths) throws SQLException, DataAccessException;
}
