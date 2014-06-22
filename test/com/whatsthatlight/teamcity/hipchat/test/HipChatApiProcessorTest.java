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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.jetty.server.Request;
import org.junit.BeforeClass;

import org.testng.annotations.Test;

import com.whatsthatlight.teamcity.hipchat.HipChatApiProcessor;
import com.whatsthatlight.teamcity.hipchat.HipChatApiResultLinks;
import com.whatsthatlight.teamcity.hipchat.HipChatConfiguration;
import com.whatsthatlight.teamcity.hipchat.HipChatEmoticon;
import com.whatsthatlight.teamcity.hipchat.HipChatEmoticons;
import com.whatsthatlight.teamcity.hipchat.HipChatMessageColour;
import com.whatsthatlight.teamcity.hipchat.HipChatMessageFormat;
import com.whatsthatlight.teamcity.hipchat.HipChatRoom;
import com.whatsthatlight.teamcity.hipchat.HipChatRoomLinks;
import com.whatsthatlight.teamcity.hipchat.HipChatRoomNotification;
import com.whatsthatlight.teamcity.hipchat.HipChatRooms;

import org.eclipse.jetty.server.handler.AbstractHandler;

public class HipChatApiProcessorTest {

	private static Logger logger = Logger.getLogger("com.whatsthatlight.teamcity.hipchat");

	@BeforeClass
	public static void ClassSetup() {
		// Set up a basic logger for debugging purposes
		BasicConfigurator.configure();
	}
	
	@Test(enabled = false)
	public void testGetEmoticons() throws URISyntaxException {
		String apiUrl = "https://api.hipchat.com/v2/";
		String apiToken = "token";
		
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setApiUrl(apiUrl);
		configuration.setApiToken(apiToken);
		
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);
		
		HipChatEmoticons emoticons = processor.getEmoticons(0);
		for (HipChatEmoticon emoticon : emoticons.items) {
			System.out.println(String.format("%s: %s - %s", emoticon.id, emoticon.shortcut, emoticon.url));
		}
	}
	
	@Test
	public void testGetEmoticonsSuccess() throws Exception {
		// Test parameters
		int expectedNumberOfEmoticons = 1;
		String expectedEmoticonId = "0";
		String expectedEmoticonShortcut = "emo";
		String expectedEmoticonUrl = "http://example.com/";
		int expectedStatusCode = HttpServletResponse.SC_OK;
		int port = 8080;
		URI uri = new URI(String.format("http://localhost:%s/", port));
		String token = "token";

		// JSON
		HipChatRoomLinks emoticonLinks = new HipChatRoomLinks("self", "webhooks", "members");
		HipChatEmoticon emoticon = new HipChatEmoticon(expectedEmoticonId, emoticonLinks, expectedEmoticonShortcut, expectedEmoticonUrl);
		HipChatApiResultLinks resultLinks = new HipChatApiResultLinks("self", "prev", "next");
		List<HipChatEmoticon> emoticonsList = new ArrayList<HipChatEmoticon>();
		emoticonsList.add(emoticon);
		HipChatEmoticons emoticons = new HipChatEmoticons(emoticonsList, 0, expectedNumberOfEmoticons, resultLinks);
		ObjectMapper mapper = new ObjectMapper();
		String expectedJson = mapper.writeValueAsString(emoticons);
		System.out.println(expectedJson);
				
		// Configuration
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setApiUrl(uri.toString());
		configuration.setApiToken(token);
		
		// Handler
		class Handler extends AbstractHandler {
			
			private int statusCode;
			private String response;

			public Handler(int statusCode, String response) {
				this.statusCode = statusCode;
				this.response = response;
			}

			@Override
			public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
				response.setContentType("text/html;charset=utf-8");
		        response.setStatus(this.statusCode);
		        response.getWriter().write(this.response);
		        baseRequest.setHandled(true);
			}
			
		}
		
		// Setup
		SimpleServer server = new SimpleServer(port, new Handler(expectedStatusCode, expectedJson));
		server.start();
		
		// Execute
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);
		HipChatEmoticons actualEmoticons = processor.getEmoticons(0);
		
		// Clean up
		server.stop();
		
		// Test
		assertEquals(expectedNumberOfEmoticons, actualEmoticons.maxResults);
		assertEquals(expectedNumberOfEmoticons, actualEmoticons.items.size());
		HipChatEmoticon actualRoom = actualEmoticons.items.get(0);
		assertEquals(expectedEmoticonId, actualRoom.id);
		assertEquals(expectedEmoticonShortcut, actualRoom.shortcut);
		assertEquals(expectedEmoticonUrl, actualRoom.url);
	}
	
	@Test
	public void testGetEmoticonsException() throws Exception {
		// Test parameters
		String expectedExceptionText = "UnsupportedSchemeException";
		int port = 8080;
		String uri = String.format("nohttp://localhost:%s/", port);
		String token = "token";
		
		// Setup
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		Appender appender = new WriterAppender(new PatternLayout("%m%n"), outputStream);
		logger.addAppender(appender);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setApiUrl(uri);
		configuration.setApiToken(token);
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);
		
		// Execute
		HipChatEmoticons actualEmoticons = processor.getEmoticons(0);
		logger.removeAppender(appender);
		
		// Test
		boolean exceptionFound = false;
		String logOutput = new String(outputStream.toByteArray());
		for (String line : logOutput.split("\n")) {
			if (line.contains(expectedExceptionText)) {
				exceptionFound = true;
				break;
			}
		}
		assertTrue(exceptionFound);
		assertNull(actualEmoticons);
	}
	
	@Test
	public void testGetEmoticonsReturnsEmptyInCaseOfFailure() throws URISyntaxException {
		String apiUrl = "https://api.hipchat.com/v2/";
		String apiToken = "invalid_token";
		
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setApiUrl(apiUrl);
		configuration.setApiToken(apiToken);
		
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);
		
		HipChatEmoticons emoticons = processor.getEmoticons(0);
		assertNull(emoticons);
	}
	
	@Test
	public void testGetRoomsReturnsEmptyInCaseOfFailure() throws URISyntaxException {
		
		String apiUrl = "https://api.hipchat.com/v2/";
		String apiToken = "invalid_token";
		
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setApiUrl(apiUrl);
		configuration.setApiToken(apiToken);
		
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);
		
		HipChatRooms rooms = processor.getRooms();
		assertNotNull(rooms);
		assertNotNull(rooms.items);
		assertEquals(0, rooms.items.size());
	}
	
	@Test(enabled = false)
	public void testGetRooms() throws URISyntaxException {

		String apiUrl = "https://api.hipchat.com/v2/";
		String apiToken = "token";

		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setApiUrl(apiUrl);
		configuration.setApiToken(apiToken);

		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);

		HipChatRooms rooms = processor.getRooms();
		for (HipChatRoom room : rooms.items) {
			System.out.println(String.format("%s - %s", room.id, room.name));
		}
	}
	
	@Test
	public void testGetRoomsSuccess() throws Exception {
		// Test parameters
		int expectedNumberOfRooms = 1;
		String expectedRoomId = "0";
		String expectedRoomName = "testRoom";
		int expectedStatusCode = HttpServletResponse.SC_OK;
		int port = 8080;
		URI uri = new URI(String.format("http://localhost:%s/", port));
		String token = "token";

		// JSON
		HipChatRoomLinks roomLinks = new HipChatRoomLinks("self", "webhooks", "members");
		HipChatRoom room = new HipChatRoom(expectedRoomId, roomLinks, expectedRoomName);
		HipChatApiResultLinks resultLinks = new HipChatApiResultLinks("self", "prev", "next");
		List<HipChatRoom> roomsList = new ArrayList<HipChatRoom>();
		roomsList.add(room);
		HipChatRooms rooms = new HipChatRooms(roomsList, 0, expectedNumberOfRooms, resultLinks);
		ObjectMapper mapper = new ObjectMapper();
		String expectedJson = mapper.writeValueAsString(rooms);
		System.out.println(expectedJson);
				
		// Configuration
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setApiUrl(uri.toString());
		configuration.setApiToken(token);
		
		// Handler
		class Handler extends AbstractHandler {
			
			private int statusCode;
			private String response;

			public Handler(int statusCode, String response) {
				this.statusCode = statusCode;
				this.response = response;
			}

			@Override
			public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
				response.setContentType("text/html;charset=utf-8");
		        response.setStatus(this.statusCode);
		        response.getWriter().write(this.response);
		        baseRequest.setHandled(true);
			}
			
		}
		
		// Setup
		SimpleServer server = new SimpleServer(port, new Handler(expectedStatusCode, expectedJson));
		server.start();
		
		// Execute
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);
		HipChatRooms actualRooms = processor.getRooms();
		
		// Clean up
		server.stop();
		
		// Test
		assertEquals(expectedNumberOfRooms, actualRooms.maxResults);
		assertEquals(expectedNumberOfRooms, actualRooms.items.size());
		HipChatRoom actualRoom = actualRooms.items.get(0);
		assertEquals(expectedRoomId, actualRoom.id);
		assertEquals(expectedRoomName, actualRoom.name);
	}

	@Test
	public void testGetRoomsException() throws Exception {
		// Test parameters
		int expectedNumberOfRooms = 0;
		String expectedExceptionText = "UnsupportedSchemeException";
		int port = 8080;
		String uri = String.format("nohttp://localhost:%s/", port);
		String token = "token";
		
		// Setup
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		Appender appender = new WriterAppender(new PatternLayout("%m%n"), outputStream);
		logger.addAppender(appender);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setApiUrl(uri);
		configuration.setApiToken(token);
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);
		
		// Execute
		HipChatRooms actualRooms = processor.getRooms();
		logger.removeAppender(appender);
		
		// Test
		boolean exceptionFound = false;
		String logOutput = new String(outputStream.toByteArray());
		for (String line : logOutput.split("\n")) {
			if (line.contains(expectedExceptionText)) {
				exceptionFound = true;
				break;
			}
		}
		assertTrue(exceptionFound);
		assertEquals(expectedNumberOfRooms, actualRooms.startIndex);
		assertEquals(expectedNumberOfRooms, actualRooms.maxResults);
		assertEquals(expectedNumberOfRooms, actualRooms.items.size());
		assertNull(actualRooms.links);
	}
	
	@Test
	public void testSendNotificationSuccess() throws Exception {
		// Test parameters
		int expectedStatusCode = HttpServletResponse.SC_NO_CONTENT;
		String expectedRoomId = "1";
		String expectedToken = "token";
		int port = 8080;
		URI uri = new URI(String.format("http://localhost:%s/", port));
		ArrayList<String> responses = new ArrayList<String>();		
		HipChatRoomNotification notification = new HipChatRoomNotification("foo", HipChatMessageFormat.TEXT, HipChatMessageColour.INFO, true);
		ObjectMapper mapper = new ObjectMapper();
		String expectedJson = mapper.writeValueAsString(notification);

		// Handler
		class Handler extends AbstractHandler {
			
			private String roomId;
			private int statusCode;
			private String authToken;
			private ArrayList<String> responses;

			public Handler(String roomId, int statusCode, String authToken, ArrayList<String> responses) {
				this.roomId = roomId;
				this.statusCode = statusCode;
				this.authToken = authToken;
				this.responses = responses;
			}

			@Override
			public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
				assertEquals(this.authToken, request.getHeader("Authorization").split(" ")[1]);
				assertEquals(this.roomId, target.split("/")[2]);
				response.setContentType("text/html;charset=utf-8");
		        response.setStatus(this.statusCode);
		        baseRequest.setHandled(true);
		        ServletInputStream inputStream = request.getInputStream();
		        String body = new Scanner(inputStream, "UTF-8").useDelimiter("\\A").next();
		        this.responses.add(body);
			}
			
		}
		
		// Setup
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setApiUrl(uri.toString());
		configuration.setApiToken(expectedToken);
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);
		SimpleServer server = new SimpleServer(port, new Handler(expectedRoomId, expectedStatusCode, expectedToken, responses));
		server.start();
				
		// Execute
		processor.sendNotification(notification, expectedRoomId);
		
		// Clean up
		server.stop();

		// Test
		assertEquals(1, responses.size());
		assertEquals(expectedJson, responses.get(0));
	}
	
	@Test
	public void testSendNotificationException() throws Exception {
		// Test parameters
		String expectedExceptionText = "UnsupportedSchemeException";
		int port = 8080;
		String uri = String.format("nohttp://localhost:%s/", port);
		String token = "token";
		
		// Setup
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		Appender appender = new WriterAppender(new PatternLayout("%m%n"), outputStream);
		logger.addAppender(appender);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setApiUrl(uri);
		configuration.setApiToken(token);
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);
		
		// Execute
		processor.sendNotification(new HipChatRoomNotification(null, null, null, false), "1");
		logger.removeAppender(appender);
		
		// Test
		boolean exceptionFound = false;
		String logOutput = new String(outputStream.toByteArray());
		for (String line : logOutput.split("\n")) {
			if (line.contains(expectedExceptionText)) {
				exceptionFound = true;
				break;
			}
		}
		assertTrue(exceptionFound);
	}
	
	@Test
	public void testSendNotificationUndeliverable() throws Exception {
		// Test parameters
		String expectedExceptionText = "Message could not be delivered: 400 Bad Request";
		int expectedStatusCode = HttpServletResponse.SC_BAD_REQUEST;
		String expectedRoomId = "1";
		String expectedToken = "token";
		int port = 8080;
		URI uri = new URI(String.format("http://localhost:%s/", port));
		HipChatRoomNotification notification = new HipChatRoomNotification("foo", "text", "black", true);

		// Setup
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		Appender appender = new WriterAppender(new PatternLayout("%m%n"), outputStream);
		logger.addAppender(appender);

		// Handler
		class Handler extends AbstractHandler {
			
			private int statusCode;

			public Handler(int statusCode) {
				this.statusCode = statusCode;
			}

			@Override
			public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		        response.setStatus(this.statusCode);
		        baseRequest.setHandled(true);
			}
			
		}
		
		// Setup
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setApiUrl(uri.toString());
		configuration.setApiToken(expectedToken);
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);
		SimpleServer server = new SimpleServer(port, new Handler(expectedStatusCode));
		server.start();
				
		// Execute
		processor.sendNotification(notification, expectedRoomId);
		
		// Clean up
		logger.removeAppender(appender);
		server.stop();

		// Test
		boolean exceptionFound = false;
		String logOutput = new String(outputStream.toByteArray());
		for (String line : logOutput.split("\n")) {
			if (line.contains(expectedExceptionText)) {
				exceptionFound = true;
				break;
			}
		}
		assertTrue(exceptionFound);
	}
	
	@Test
	public void testTestAuthenticationSuccess() throws Exception {
		// Test parameters
		int expectedStatusCode = HttpServletResponse.SC_ACCEPTED;
		int port = 8080;
		URI uri = new URI(String.format("http://localhost:%s/", port));
		String token = "token";

		// Handler
		class Handler extends AbstractHandler {
			
			private int statusCode;
			private String authToken;

			public Handler(int statusCode, String authToken) {
				this.statusCode = statusCode;
				this.authToken = authToken;
			}

			@Override
			public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
				assertEquals(this.authToken, request.getParameter("auth_token"));
				assertEquals("true", request.getParameter("auth_test"));
				response.setContentType("text/html;charset=utf-8");
		        response.setStatus(this.statusCode);
		        baseRequest.setHandled(true);
			}
			
		}
		
		// Setup
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setApiUrl(uri.toString());
		configuration.setApiToken(token);
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);
		SimpleServer server = new SimpleServer(port, new Handler(expectedStatusCode, token));
		server.start();
		
		// Execute
		boolean actualAuthResult = processor.testAuthentication();
		
		// Clean up
		server.stop();

		// Test
		assertTrue(actualAuthResult);
	}
	
	@Test
	public void testTestAuthenticationFailure() throws Exception {
		// Test parameters
		int expectedStatusCode = HttpServletResponse.SC_BAD_REQUEST;
		int port = 8080;
		URI uri = new URI(String.format("http://localhost:%s/", port));
		String token = "token";

		// Handler
		class Handler extends AbstractHandler {
			
			private int statusCode;

			public Handler(int statusCode) {
				this.statusCode = statusCode;
			}

			@Override
			public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
				assertEquals("true", request.getParameter("auth_test"));
				response.setContentType("text/html;charset=utf-8");
		        response.setStatus(this.statusCode);
		        baseRequest.setHandled(true);
			}
			
		}
		
		// Setup
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setApiUrl(uri.toString());
		configuration.setApiToken(token);
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);
		SimpleServer server = new SimpleServer(port, new Handler(expectedStatusCode));
		server.start();
		
		// Execute
		boolean actualAuthResult = processor.testAuthentication();
		
		// Clean up
		server.stop();

		// Test
		assertFalse(actualAuthResult);
	}
	
	@Test
	public void testTestAuthenticationFailureWhenException() throws Exception {
		// Test parameters
		int port = 8080;
		String uri = String.format("nohttp://localhost:%s/", port);
		String token = "token";
	
		// Setup
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setApiUrl(uri);
		configuration.setApiToken(token);
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);
		
		// Execute
		boolean actualAuthResult = processor.testAuthentication();
		
		// Test
		assertFalse(actualAuthResult);
	}
	
	@Test(enabled = false)
	public void testTestAuthentication() throws URISyntaxException {

		String apiUrl = "https://api.hipchat.com/v2/";
		String apiToken = "token";

		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setApiUrl(apiUrl);
		configuration.setApiToken(apiToken);

		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);

		assertTrue(processor.testAuthentication());
	}
		
}
