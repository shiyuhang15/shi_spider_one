package com.hbrb.spider.service.impl;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;

public class CachedRegionService extends AbstractRegionService {
	private Map<String, Integer> cityCountMap = new HashMap<>();
	private Map<String, Integer> provinceCountMap = new HashMap<>();


	@Override
	public String getRegionCount() {
		if (!provinceCountMap.isEmpty() || !cityCountMap.isEmpty()) {
			Map<String, Map<String, Integer>> regionCountMap = new HashMap<>(2);
			regionCountMap.put("province", provinceCountMap);
			regionCountMap.put("city", cityCountMap);
			return JSON.toJSONString(regionCountMap);
		} 
		return null;
	}


	@Override
	public void clearRegionCountMap() {
		cityCountMap.clear();
		provinceCountMap.clear();
	}
/*
	@Override
	public void putProvince(String province) {
		Integer count = provinceCountMap.get(province);
		if (null == count) {
			provinceCountMap.put(province, 0);
		}
	}

	@Override
	public void putCity(String city) {
		Integer count = cityCountMap.get(city);
		if (null == count) {
			cityCountMap.put(city, 0);
		}
	}
*/
	@Override
	void countCity(String city) {
		Integer count = cityCountMap.get(city);
		if (null == count) {
			cityCountMap.put(city, 1);
		} else {
			cityCountMap.put(city, 1 + count);
		}
	}


	@Override
	void countProvince(String province) {
		Integer count = provinceCountMap.get(province);
		if (null == count) {
			provinceCountMap.put(province, 1);
		} else {
			provinceCountMap.put(province, 1 + count);
		}
	}
}
