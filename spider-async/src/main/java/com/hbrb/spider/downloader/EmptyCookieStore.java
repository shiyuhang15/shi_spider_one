package com.hbrb.spider.downloader;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;

public class EmptyCookieStore implements CookieStore {
	@Override
	public List<Cookie> getCookies() {
		return Collections.emptyList();
	}

	@Override
	public boolean clearExpired(Date date) {
		return true;
	}

	@Override
	public void clear() {
	}

	@Override
	public void addCookie(Cookie cookie) {
	}
}
