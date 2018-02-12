package com.hbrb.spider.constraint;

public interface UniqueConstraint {
	/**
	 * 检查ID是否已存在
	 * 
	 * @param id
	 * @return 已存在：true，不存在：false
	 */
	boolean isDuplicated(String id);
}
