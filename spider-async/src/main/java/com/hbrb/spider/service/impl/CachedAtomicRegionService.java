package com.hbrb.spider.service.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.alibaba.fastjson.JSON;
import com.hbrb.spider.model.Region;
import com.hbrb.spider.service.RegionService;

public class CachedAtomicRegionService extends AbstractRegionService {
	private Map<String, AtomicInteger> cityCountMap = new ConcurrentHashMap<>();
	private Map<String, AtomicInteger> provinceCountMap = new ConcurrentHashMap<>();

	private Map<String, Integer> getCountMap(Map<String, AtomicInteger> countMap) {
		Map<String, Integer> res = new HashMap<>();
		Set<Entry<String, AtomicInteger>> entrySet = countMap.entrySet();
		for (Entry<String, AtomicInteger> entry : entrySet) {
			AtomicInteger value = entry.getValue();
			int count = value.get();
			if (count != 0) {
				res.put(entry.getKey(), count);
			}
		}
		return res;
	}
	
	private void clearCountMap(Map<String, AtomicInteger> countMap) {
		for (AtomicInteger counter : countMap.values()) {
			counter.set(0);
		}
	}


	@Override
	public String getRegionCount() {
		Map<String, Integer> pcm = getCountMap(this.provinceCountMap);
		Map<String, Integer> ccm = getCountMap(this.cityCountMap);
		if (!ccm.isEmpty() || !pcm.isEmpty()) {
			Map<String, Map<String, Integer>> regionCountMap = new HashMap<>(2);
			regionCountMap.put("province", pcm);
			regionCountMap.put("city", ccm);
			return JSON.toJSONString(regionCountMap);
		} 
		return null;
	}


	@Override
	public void clearRegionCountMap() {
		clearCountMap(cityCountMap);
		clearCountMap(provinceCountMap);
	}

	public void putRegion(Region region){
		String province = region.getProvince();
		putProvince(province);
		if (RegionService.HEBEI == province) {
			String city = region.getCity();
			if (null != city && !city.isEmpty()) {
				putCity(city);
			}
		}
	}

	private void putProvince(String province) {
		AtomicInteger provinceCount = provinceCountMap.get(province);
		if (null == provinceCount) {
			provinceCountMap.put(province, new AtomicInteger(0));
		}
	}


	private void putCity(String city) {
		AtomicInteger cityCount = cityCountMap.get(city);
		if (null == cityCount) {
			cityCountMap.put(city, new AtomicInteger(0));
		}
	}

	@Override
	void countCity(String city) {
		cityCountMap.get(city).incrementAndGet();
	}

	@Override
	void countProvince(String provice) {
		cityCountMap.get(provice).incrementAndGet();
	}
}
