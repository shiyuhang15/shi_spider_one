package com.hbrb.spider.model;

public class SohuMtUser {
	public static final int LIVENESS_3 = 3;
	private String id;
	private String name;
	private Integer liveness;
	
	public SohuMtUser(String id, String name, Integer liveness) {
		super();
		this.id = id;
		this.name = name;
		this.liveness = liveness;
	}
	public SohuMtUser() {
		super();
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Integer getLiveness() {
		return liveness;
	}
	public void setLiveness(Integer liveness) {
		this.liveness = liveness;
	}
	
}
