package com.hbrb.spider.scheduler;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import com.hbrb.spider.downloader.async.RequestBuffer;
import com.hbrb.spider.model.task.TemplateTask;

public abstract class TemplateTaskRequestBuffer<T extends TemplateTask> implements RequestBuffer<T> {
	AtomicInteger nullCount = new AtomicInteger(0);
	private AtomicInteger pointer = new AtomicInteger(-1);
	Queue<T>[] qs;

	@Override
	public T poll() {
		T t = null;
		for (;;) {
			t = qs[loopPointer()].poll();
			if (null == t) {
				if (nullCount.incrementAndGet() >= qs.length) {
					return t;
				}
			} else {
				nullCount.set(0);
				return t;
			}
		}
	}

	private int loopPointer() {
		for (;;) {
			int p = pointer.incrementAndGet();
			if (p >= qs.length) {
				if (pointer.compareAndSet(p, 0)) {
					return 0;
				}
			} else {
				return p;
			}
		}
	}

	void reset(Queue<T>[] qs) {
		pointer.set(-1);
		nullCount.set(0);
		this.qs = qs;
	}
}
