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

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class SimpleServer implements Runnable {
		
	private org.eclipse.jetty.server.Server server;
	private Thread thread;
	
	public SimpleServer(int port, AbstractHandler handler) {
		this(port, handler, false);
	}

	public SimpleServer(int port, AbstractHandler handler, boolean secure) {
		this.server = new Server(port);
		this.server.setHandler(handler);
		this.thread = new Thread(this);
	}

	public void run() {
		try {
			this.server.start();
			this.server.join();
		} catch (Exception e) {
			System.out.println(e);
		}
	}
	
	public void start() throws InterruptedException {
		this.thread.start();
		while (!this.server.getState().equals("STARTED"));
	}
	
	public void stop() throws Exception {
		server.stop();
		this.thread.join();
	}
}
