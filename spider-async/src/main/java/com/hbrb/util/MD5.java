package com.hbrb.util;

import org.apache.commons.codec.digest.DigestUtils;

public final class MD5 {
	public final static String get(String data) {
		return DigestUtils.md5Hex(data);
	}
}
