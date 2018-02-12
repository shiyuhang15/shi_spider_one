package com.hbrb.spider;

public interface ConstantsHome {
	String USER_DIR = System.getProperty("user.dir");

	interface PATTERN {
		String DATEFORMAT_DEFAULT = "yyyy-MM-dd HH:mm:ss";
	}

	String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.116 Safari/537.36 Edge/15.15063";

	interface Prefix {
		String TEST = "test_";
	}

	String MARK_URL_START_REFRESH = "#REFRESH";
	String MARK_URL_START_POSTPONE = "#POSTPONE";
	
	/**
	 * 持续不产出有效目标的导航任务只保留两周
	 */
	int EFFECTIVE_PERIOD_NAVI = 1209600000;
	/**
	 * 超过1天没更新的话才允许更新workedTime
	 */
	int INTERVAL_UPDATE_TIME_WORKED = 86400000;
	//	private static final  StackTraceElement[] STACKTRACE_EMPTY = new StackTraceElement[0];
	/**
	 * 只采集2天内发布的目标
	 */
	int EFFECTIVE_PERIOD_TARGET = 172800000;
	
	int ENCODING_GZIP = 1;
	int ENCODING_DEFLATE = 2;

	// String ID_CONTEXT_REQUESTTASK = "task_request";
	interface Redis {
		interface Key {
			String NAVI_TASK_WORKED_TIME = "NTWT_";
		}
	}
	
	String FRONT_PAGE = "_首_";
	
	interface RequestHeader {
		String COOKIE = "Cookie";
	}
}
