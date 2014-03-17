package com.whatsthatlight.teamcity.hipchat.test;

import static org.junit.Assert.*;

import org.junit.Test;
import org.stringtemplate.v4.ST;

import com.whatsthatlight.teamcity.hipchat.HipChatNotificationMessageTemplate;

public class HipChatNotificationMessageTemplateTest {

	@Test
	public void testOptionalContributors() {
		// Test parameters
		String expectedContributors = "foo, bar, baz";
		
		// Test where contributors are present.
		ST template = new ST(HipChatNotificationMessageTemplate.BUILD_STARTED);
		template.add("hasContributors", true);
		template.add("contributors", expectedContributors);
		String renderedTemplate = template.render();
		System.out.println(renderedTemplate);
		assertTrue(renderedTemplate.contains(expectedContributors));

		// Test where there are no contributors.
		template = new ST(HipChatNotificationMessageTemplate.BUILD_STARTED);
		template.add("hasContributors", false);
		template.add("contributors", expectedContributors);
		renderedTemplate = template.render();
		System.out.println(renderedTemplate);
		assertFalse(renderedTemplate.contains(expectedContributors));
	}

}
