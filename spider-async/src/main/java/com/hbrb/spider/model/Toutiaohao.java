package com.hbrb.spider.model;

public class Toutiaohao {
	public static final int LIVENESS_3 = 3;
	private Long id;
	private String name;
	private Integer liveness;
	
	public Toutiaohao(Long id, String name, Integer liveness) {
		super();
		this.id = id;
		this.name = name;
		this.liveness = liveness;
	}
	public Toutiaohao() {
		super();
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
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
