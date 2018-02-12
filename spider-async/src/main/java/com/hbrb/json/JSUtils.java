package com.hbrb.json;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.hbrb.json.impl.JSArrayImpl;
import com.hbrb.json.impl.JSObjectImpl;

public class JSUtils {
	public static JSObject createJSObject(String source) throws JSException {
		if (null == source) {
			return new JSObjectImpl(new JSONObject());
		}
		try {
			return new JSObjectImpl(new JSONObject(source));
		} catch (JSONException e) {
			throw new JSException(e);
		}
	}

	public static JSArray createJSArray(String source) throws JSException {
		try {
			return new JSArrayImpl(new JSONArray(source));
		} catch (JSONException e) {
			throw new JSException(e);
		}
	}
}
