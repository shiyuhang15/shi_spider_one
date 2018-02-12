package com.hbrb.spider.tool;

import java.io.IOException;

import javax.naming.ConfigurationException;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.hbrb.spider.tool.controller.MainController;

public class Launcher {
	public static void main(String[] args) {
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				MainController mc;
				try {
					mc = new MainController();
					if (null != mc) {
						mc.showMainFrame();
					}
				} catch (ParserConfigurationException | SAXException
						| IOException | ConfigurationException e) {
					e.printStackTrace();
				}
			}
		});
	}
}
