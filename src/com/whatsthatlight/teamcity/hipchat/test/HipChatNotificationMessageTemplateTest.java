package com.whatsthatlight.teamcity.hipchat.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.whatsthatlight.teamcity.hipchat.HipChatNotificationMessageTemplate;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class HipChatNotificationMessageTemplateTest {

	@Test
	public void testOptionalContributorsExists() throws IOException, TemplateException {
		// Test parameters
		String expectedContributors = "foo, bar, baz";
		
		// Create template
		String templateName = "template";
		StringTemplateLoader loader = new StringTemplateLoader();
		loader.putTemplate(templateName, HipChatNotificationMessageTemplate.BUILD_STARTED);
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
		templateMap.put("emoticon", "");
		
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
		loader.putTemplate(templateName, HipChatNotificationMessageTemplate.BUILD_STARTED);
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
		templateMap.put("emoticon", "");
		
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

}
