package com.hbrb.spider.model.task;

import com.hbrb.spider.model.WeiboUser;

public class HomeWeiboRequestTask extends GenericRequestTask {
	private WeiboUser user;
	private String page_id;
	private String domain_op;
	private String pl_name;
	private int pagebar;
	private int page_num;
	public HomeWeiboRequestTask(String url, int type) {
		super(url, type);
	}
	public String getPl_name() {
		return pl_name;
	}
	public void setPl_name(String pl_name) {
		this.pl_name = pl_name;
	}
	public String getDomain_op() {
		return domain_op;
	}
	public void setDomain_op(String domain_op) {
		this.domain_op = domain_op;
	}
	public String getPage_id() {
		return page_id;
	}
	public void setPage_id(String page_id) {
		this.page_id = page_id;
	}
	public int getPagebar() {
		return pagebar;
	}
	public void setPagebar(int pagebar) {
		this.pagebar = pagebar;
	}
	public int getPage_num() {
		return page_num;
	}
	public void setPage_num(int page_num) {
		this.page_num = page_num;
	}
	public WeiboUser getUser() {
		return user;
	}
	public void setUser(WeiboUser user) {
		this.user = user;
	}
}