package com.hbrb.json.impl;

import org.json.JSONArray;
import org.json.JSONException;

import com.hbrb.json.JSArray;
import com.hbrb.json.JSException;
import com.hbrb.json.JSObject;

public class JSArrayImpl implements JSArray {
	private JSONArray target;

	public JSArrayImpl(JSONArray target) {
		super();
		this.target = target;
	}

	@Override
	public int length() {
		return target.length();
	}

	@Override
	public JSObject getNotNullJSObject(int index) throws JSException {
		try {
			return new JSObjectImpl(target.getJSONObject(index));
		} catch (JSONException e) {
			throw new JSException(e);
		}
	}

	@Override
	public JSArray getNotNullJSArray(int index) throws JSException {
		try {
			return new JSArrayImpl(target.getJSONArray(index));
		} catch (JSONException e) {
			throw new JSException(e);
		}
	}

	@Override
	public String getNotNullString(int index) throws JSException {
		try {
			return target.getString(index);
		} catch (JSONException e) {
			throw new JSException(e);
		}
	}
	
	@Override
	public String toString() {
		return target.toString();
	}

	@Override
	public String toFormattedString() {
		return target.toString(4);
	}

}
