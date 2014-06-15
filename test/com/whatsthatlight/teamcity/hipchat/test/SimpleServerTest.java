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

import static org.junit.Assert.*;

import java.net.URI;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

public class SimpleServerTest {

	@Test
	public void testTestServer() throws Exception {
		// Test parameters
		String expectedResponse = "<h1>Hello World</h1>";
		int expectedStatusCode = HttpServletResponse.SC_OK;
		int port = 8080;
		URI uri = new URI(String.format("http://localhost:%s/", port));

		// Setup
		SimpleServer server = new SimpleServer(port, new SimpleHandler(expectedResponse, expectedStatusCode));
		server.start();
		
		// Make request
		HttpClient client = HttpClientBuilder.create().build();
		HttpGet getRequest = new HttpGet(uri.toString());
		HttpResponse getResponse = client.execute(getRequest);
		int actualStatusCode = getResponse.getStatusLine().getStatusCode();
		String actualResponse = EntityUtils.toString(getResponse.getEntity());

		// Clean up
		server.stop();

		// Test
		assertEquals(expectedStatusCode, actualStatusCode);
		assertEquals(expectedResponse, actualResponse);
	}

}
