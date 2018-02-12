package com.hbrb.spider.model.task;

import org.json.JSONObject;

import com.hbrb.json.JSException;
import com.hbrb.json.JSObject;
import com.hbrb.json.impl.JSObjectImpl;
import com.hbrb.spider.model.Region;


public class TargetTask extends TemplateTask{
	private Region region;
	private boolean isHofp;
	private String title;
	/**
	 * 请求头信息
	 */
//	private String headers;
	
	public TargetTask(String url) {
		super(url);
	}

	public TargetTask(JSObject jsonTask) {
		super(jsonTask.getNotNullString("url"));
		setType(jsonTask.getNotNullInt("type"));
		setSourceType(jsonTask.getNotNullInt("sourceType"));
		try {
			setTitle(jsonTask.getNotNullString("title"));
		} catch (JSException e) {
		}
		try {
			setSiteTaskId(jsonTask.getNotNullInt("siteTaskId"));
		} catch (JSException e) {
		}
		try {
			int isHofp = jsonTask.getNotNullInt("isHofp");
			if (isHofp == 1) {
				setHofp(true);
			}
		} catch (JSException e) {
		}
		try {
			String province = jsonTask.getNotNullString("province");
			Region region = new Region();
			region.setProvince(province);
			setRegion(region);
			try {
				String city = jsonTask.getNotNullString("city");
				region.setCity(city);
				try {
					String county = jsonTask.getNotNullString("county");
					region.setCounty(county);
				} catch (JSException e) {
				}
			} catch (JSException e) {
			}
		} catch (JSException e) {
		}
		/*try {
			setHeaders(jsonTask.getNotNullString("headers"));
		} catch (JSException e) {
		}*/
	}
	
	public JSObject toJson(){
		JSONObject jso = new JSONObject();
		jso.put("url", getUrl());
		jso.put("title", this.title);
		jso.put("sourceType", getSourceType());
		jso.put("siteTaskId", getSiteTaskId());
		jso.put("type", getType());
		if (isHofp) {
			jso.put("isHofp", 1);
		}
		if (null != region) {
			String province = region.getProvince();
			if (null != province) {
				jso.put("province", province);
				String city = region.getCity();
				if (null != city) {
					jso.put("city", city);
					String county = region.getCounty();
					if (null != county) {
						jso.put("county", county);
					}
				}
			}
		}
//		jso.put("headers", this.headers);
		return new JSObjectImpl(jso);
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public boolean isHofp() {
		return isHofp;
	}

	public void setHofp(boolean isHofp) {
		this.isHofp = isHofp;
	}
	public Region getRegion() {
		return region;
	}
	public void setRegion(Region region) {
		this.region = region;
	}
}
