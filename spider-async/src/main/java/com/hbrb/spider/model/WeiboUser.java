package com.hbrb.spider.model;

public class WeiboUser {
	private Long id;
	/**
	 * 昵称
	 */
	private String nickName;
	/**
	 * 地域
	 */
	private Region region;

	public WeiboUser() {
		super();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNickName() {
		return nickName;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}

	public Region getRegion() {
		return region;
	}

	public void setRegion(Region region) {
		this.region = region;
	}

	@Override
	public String toString() {
		return "WeiboUser [id=" + id + ", nickName=" + nickName + ", region=" + region + "]";
	}
}
