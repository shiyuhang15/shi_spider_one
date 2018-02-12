package com.util.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;

public class ReadLog {
	
	public static void main(String[] args) {
		readFile();
	}
	public static void readFile()
	{
		String file = System.getProperty("user.dir")+"/crawl.log";
		File files = new File(file);
		FileInputStream str;
		String content = "";
		int escapedUrl=0;
		int sc200=0;
		int titlea = 0;
		int prenavi=0;
		int autnavi=0;
		int pust = 0;
		int cont=0;
		int socketTimeout=0;
		try {
			str = new FileInputStream(files);
			BufferedReader reader = new BufferedReader(new InputStreamReader(str));
			String lines = reader.readLine();
			while(lines !=null && !lines.equals(""))
			{
				//System.out.println(lines);
				if(lines.indexOf("push target") !=-1)
				{
					//查看有效目标
					pust++;
				}
				if(lines.indexOf("precise navi") !=-1)
				{
					//查看导航并查看是否有分页的
					prenavi++;
					String url = StringUt.getSubUtilSimple(lines, ".*(-.*_\\d.*)$");
					if(url.length()>0)
					System.out.println(url);
				}
				if(lines.indexOf("auto navi") !=-1)
				{
					//查看导航并查看是否有分页的
					autnavi++;
					String url = StringUt.getSubUtilSimple(lines, ".*(-.*_\\d.*)$");
					if(url.length()>0)
					System.out.println(url);
				}
				if(lines.indexOf("escapedUrl") != -1)
				{
					//escapedUrl - http://www.hbhsjj.gov.cn/ShowArticle.aspx?ID=11452 -
					String strac =StringUt.getSubUtilSimple(lines, ".*((escapedUrl)+.*-)");
					if(strac != null && strac.length()>0)
					{
						System.out.println(strac);
					}
					escapedUrl++;
				}
				//read warn
				if(lines.indexOf("WARN") !=-1)
				{
					if(lines.matches(".*(- title|- content)+.*"))
					{
						String stra =StringUt.getSubUtilSimple(lines, ".*(((- title|- content)+) +.*)$");
						if(stra != null && stra.length() >0)
							System.out.println(stra);
					}
				}
				if(lines.indexOf("- sc:200") !=-1)
				{
					sc200++;
				}
				if(lines.indexOf("SocketTimeout")!=-1) {
					socketTimeout++;
				}
				if(lines.indexOf("\"title\":\"") !=-1)
				{
					titlea++;
				}
				if(lines.indexOf("\"content\":\"")!=-1) {
					cont++;
				}
				 lines = reader.readLine();
			}
			
			System.out.println("漏掉escapedUrl:"+escapedUrl);
			System.out.println("title:"+titlea+"--sc200:"+sc200+"--content:"+cont+"--SocketTimeout:"+socketTimeout);
			System.out.println("precise navi:"+prenavi+"--auto navi:"+autnavi);
			System.out.println("有效目标target:"+pust);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
