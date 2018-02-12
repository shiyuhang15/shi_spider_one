package com.hbrb.spider.scheduler;

import com.hbrb.spider.model.task.RequestTask;

public interface Scheduler<T extends RequestTask> {
	int PUSH_FAILED = 0;
	int PUSH_SUCCESS = 1;
	int PUSH_DUPLICATE = 2;
	int PUSH_LIMITED = 3;
}
