/**
Copyright 2014 Pieter Rautenbach

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package com.whatsthatlight.teamcity.hipchat.test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// Modelled on Python's threading.Event
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
