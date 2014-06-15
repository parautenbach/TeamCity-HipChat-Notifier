package com.whatsthatlight.teamcity.hipchat.test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Event {

	Lock lock = new ReentrantLock();
	Condition condition = lock.newCondition();
	boolean flag = false;
	
	public void doWait() throws InterruptedException {
		this.lock.lock();
		try {
			if (!this.flag) {
				this.condition.await();
			}
		} finally {
			this.lock.unlock();
		}
	}

	public void doWait(int milliSeconds) throws InterruptedException {
		this.lock.lock();
		try {
			if (!this.flag) {
				this.condition.await(milliSeconds, TimeUnit.MILLISECONDS);
			}
		} finally {
			this.lock.unlock();
		}
	}

	public boolean isSet() {
		this.lock.lock();
		try {
			return this.flag;
		} finally {
			this.lock.unlock();
		}
	}

	public void set() {
		this.lock.lock();
		try {
			this.flag = true;
			this.condition.signalAll();
		} finally {
			this.lock.unlock();
		}
	}

	public void clear() {
		this.lock.lock();
		try {
			this.flag = false;
			this.condition.signalAll();
		} finally {
			this.lock.unlock();
		}
	}
}
