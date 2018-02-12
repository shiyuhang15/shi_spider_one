package com.hbrb.spider.model.template;

public class Rectangle {
	private final int x;
	private final int y;
	private final int width;
	private final int height;

	public Rectangle(int x, int y, int width, int height) {
		super();
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public boolean contains(float X, float Y) {
		if ((width | height) < 0) {
			return false;
		}
		if (X < x || Y < y) {
			return false;
		}
		return (x + width > X) && (y + height > Y);
	}
}
