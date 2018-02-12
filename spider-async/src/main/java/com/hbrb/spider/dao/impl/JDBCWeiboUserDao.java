package com.hbrb.spider.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.hbrb.spider.dao.WeiboUserDao;
import com.hbrb.spider.model.Region;
import com.hbrb.spider.model.WeiboUser;
import com.hbrb.spider.service.RegionService;

public class JDBCWeiboUserDao extends JDBCBasicDao implements
		WeiboUserDao {

	@Override
	public List<WeiboUser> retrieveAll() throws SQLException {
		try (ResultSet rs = executeQuery("select c_id,c_province,c_city,c_county from t_user_weibo")) {
			List<WeiboUser> users = new ArrayList<WeiboUser>();
			while (rs.next()) {
				WeiboUser user = new WeiboUser();
				user.setId(rs.getLong(1));
				String province = rs.getString(2);
				Region region = null;
				if (null != province && !province.isEmpty()) {
					if (RegionService.HEBEI.equals(province)) {
						province = RegionService.HEBEI;
					}
					region = new Region();
					region.setProvince(province);
					String city = rs.getString(3);
					if (null != city && !city.isEmpty()) {
						region.setCity(city);
						String county = rs.getString(4);
						if (null != county && !county.isEmpty()) {
							region.setCounty(county);
						}
					}
				}
				user.setRegion(region);
				users.add(user);
			}
			return users;
		}
	}
}
