package com.whatsthatlight.teamcity.hipchat.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import jetbrains.buildServer.serverSide.ServerPaths;
import static org.mockito.Mockito.*;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;

import org.apache.log4j.BasicConfigurator;

import com.whatsthatlight.teamcity.hipchat.HipChatNotificationMessageTemplates;
import com.whatsthatlight.teamcity.hipchat.TeamCityEvent;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class HipChatNotificationMessageTemplatesTest {

	@BeforeClass
	public static void classSetUp() {
		BasicConfigurator.configure();
	}
	
	@DataProvider(name = "dataProvider")
	public Object[][] provideData() {
		return new Object[][] {
				{ TeamCityEvent.BUILD_STARTED, "buildStartedTemplate.ftl", "buildStarted", HipChatNotificationMessageTemplates.BUILD_STARTED_DEFAULT_TEMPLATE },
				{ TeamCityEvent.BUILD_SUCCESSFUL, "buildSuccessfulTemplate.ftl", "buildSuccessful", HipChatNotificationMessageTemplates.BUILD_SUCCESSFUL_DEFAULT_TEMPLATE },
				{ TeamCityEvent.BUILD_FAILED, "buildFailedTemplate.ftl", "buildFailed", HipChatNotificationMessageTemplates.BUILD_FAILED_DEFAULT_TEMPLATE },
				{ TeamCityEvent.BUILD_INTERRUPTED, "buildInterruptedTemplate.ftl", "buildInterrupted", HipChatNotificationMessageTemplates.BUILD_INTERRUPTED_DEFAULT_TEMPLATE },
				{ TeamCityEvent.SERVER_STARTUP, "serverStartupTemplate.ftl", "serverStartup", HipChatNotificationMessageTemplates.SERVER_STARTUP_DEFAULT_TEMPLATE },
				{ TeamCityEvent.SERVER_SHUTDOWN, "serverShutdownTemplate.ftl", "serverShutdown", HipChatNotificationMessageTemplates.SERVER_SHUTDOWN_DEFAULT_TEMPLATE }
		};
	}
	
	@Test
	public void testOptionalContributorsExists() throws IOException, TemplateException {
		// Test parameters
		String expectedContributors = "foo, bar, baz";
		
		// Create template
		String templateName = "template";
		StringTemplateLoader loader = new StringTemplateLoader();
		loader.putTemplate(templateName, HipChatNotificationMessageTemplates.BUILD_STARTED_DEFAULT_TEMPLATE);
		Configuration config = new Configuration();
		config.setTemplateLoader(loader);
		Template template = config.getTemplate(templateName);
		Map<String, Object> templateMap = new HashMap<String, Object>();
		templateMap.put("hasContributors", true);
		templateMap.put("contributors", expectedContributors);
		// Other required values
		templateMap.put("fullName", "");
		templateMap.put("hasBranch", false);
		templateMap.put("buildNumber", "");
		templateMap.put("triggeredBy", "");
		templateMap.put("emoticonUrl", "");
		templateMap.put("serverUrl", "");
		templateMap.put("projectId", "");
		templateMap.put("buildId", "");
		templateMap.put("buildTypeId", "");
		
		// Render
		Writer writer = new StringWriter();
	    template.process(templateMap, writer);
	    writer.flush();
	    String renderedTemplate = writer.toString();
	    writer.close();
	    
	    // Test where contributors are present.		
		System.out.println(renderedTemplate);
		assertTrue(renderedTemplate.contains(expectedContributors));
	}

	@Test
	public void testOptionalContributorsDoesNotExist() throws IOException, TemplateException {
		// Test parameters
		String expectedContributors = "foo, bar, baz";
		
		// Create template
		String templateName = "template";
		StringTemplateLoader loader = new StringTemplateLoader();
		loader.putTemplate(templateName, HipChatNotificationMessageTemplates.BUILD_STARTED_DEFAULT_TEMPLATE);
		Configuration config = new Configuration();
		config.setTemplateLoader(loader);
		Template template = config.getTemplate(templateName);
		Map<String, Object> templateMap = new HashMap<String, Object>();
		templateMap.put("hasContributors", false);
		// Other required values
		templateMap.put("fullName", "");
		templateMap.put("hasBranch", false);
		templateMap.put("buildNumber", "");
		templateMap.put("triggeredBy", "");
		templateMap.put("emoticonUrl", "");
		templateMap.put("serverUrl", "");
		templateMap.put("projectId", "");		
		templateMap.put("buildId", "");
		templateMap.put("buildTypeId", "");
		
		// Render
		Writer writer = new StringWriter();
	    template.process(templateMap, writer);
	    writer.flush();
	    String renderedTemplate = writer.toString();
	    writer.close();
	    
	    // Test where contributors are present.		
		System.out.println(renderedTemplate);
		assertFalse(renderedTemplate.contains(expectedContributors));
	}
	
	@Test(dataProvider = "dataProvider")
	public void testReadAndWriteTemplate(TeamCityEvent expectedEvent, String expectedFileName, String ignored, String expectedTemplateString) throws IOException, TemplateException {
		// Parameters
		String expectedConfigDir = ".";
		String expectedTemplateDir = "hipchat";
		System.out.println(expectedFileName);
		
		// Pre-conditions
		File templateFile = new File(expectedConfigDir, expectedFileName);
		templateFile.delete();
		assertFalse(templateFile.exists());
		
		// Prepare
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(expectedConfigDir);
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		
		// Execute (write)
		templates.writeTemplate(expectedEvent, expectedTemplateString);
		
		// Test
		templateFile = new File(expectedTemplateDir, expectedFileName);
		assertTrue(templateFile.exists());
		String actualTemplateString = new Scanner(templateFile).useDelimiter("\\A").next();
		assertEquals(expectedTemplateString, actualTemplateString);
		
		// Execute (read)
		Template actualTemplate = templates.readTemplate(expectedEvent);
	    
	    // Test
	    assertEquals(expectedTemplateString, actualTemplate.toString());
	}
	
	@Test(dataProvider = "dataProvider")
	public void testReadDefaultTemplate(TeamCityEvent expectedEvent, String expectedFileName, String ignored, String expectedTemplateString) throws IOException, TemplateException {
		// Parameters
		String expectedConfigDir = ".";
		String expectedTemplateDir = "hipchat";
		System.out.println(expectedFileName);
		
		// Pre-conditions
		File templateFile = new File(expectedTemplateDir, expectedFileName);
		templateFile.delete();
		assertFalse(templateFile.exists());
		
		// Prepare
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(expectedConfigDir);
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
				
		// Execute (read)
		Template actualTemplate = templates.readTemplate(expectedEvent);
	    
	    // Test
	    assertEquals(expectedTemplateString, actualTemplate.toString());
	}
	
	@Test(dataProvider = "dataProvider")
	public void testOverwriteExistingTemplate(TeamCityEvent expectedEvent, String expectedFileName, String expectedTemplateStringFirst, String expectedTemplateString) throws IOException {
		// Parameters
		String expectedTemplateStringSecond = "bar";
		String expectedConfigDir = ".";
		String expectedTemplateDir = "hipchat";
		System.out.println(expectedFileName);
		
		// Pre-conditions
		File templateFile = new File(expectedConfigDir, expectedFileName);
		templateFile.delete();
		assertFalse(templateFile.exists());
		
		// Prepare
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(expectedConfigDir);
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		
		// Execute (write)
		templates.writeTemplate(expectedEvent, expectedTemplateStringFirst);
		templates.writeTemplate(expectedEvent, expectedTemplateStringSecond);
		
		// Test
		templateFile = new File(expectedTemplateDir, expectedFileName);
		assertTrue(templateFile.exists());
		String actualTemplateString = new Scanner(templateFile).useDelimiter("\\A").next();
		assertEquals(expectedTemplateStringSecond, actualTemplateString);
		
		// Execute (read)
		Template actualTemplate = templates.readTemplate(expectedEvent);
	    
	    // Test
	    assertEquals(expectedTemplateStringSecond, actualTemplate.toString());
	}
	
}
