package com.whatsthatlight.teamcity.hipchat.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.whatsthatlight.teamcity.hipchat.HipChatApiProcessor;
import com.whatsthatlight.teamcity.hipchat.HipChatApiResultLinks;
import com.whatsthatlight.teamcity.hipchat.HipChatConfiguration;
import com.whatsthatlight.teamcity.hipchat.HipChatEmoticon;
import com.whatsthatlight.teamcity.hipchat.HipChatEmoticonCache;
import com.whatsthatlight.teamcity.hipchat.HipChatEmoticons;

public class HipChatEmoticonCacheTest {

	@BeforeClass
	public static void ClassSetup() {
		// Set up a basic logger for debugging purposes
		BasicConfigurator.configure();
	}
	
	@Test(enabled = false)
	public void testReload() throws URISyntaxException {
		String apiUrl = "https://api.hipchat.com/v2/";
		String apiToken = "token";
		
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setApiUrl(apiUrl);
		configuration.setApiToken(apiToken);
						
		// Execute
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);
		HipChatEmoticonCache emoticonCache = new HipChatEmoticonCache(processor);
		emoticonCache.reload();
		
		// Test
		AssertJUnit.assertEquals(204, emoticonCache.getSize());
	}
	
	@Test
	public void testSingleBatch() throws IOException {

		// Batch size
		int maxResults = 1;

		// First batch
		String emoticonId = "id1";
		String emoticonShortcut = "emo1";
		String emoticonUrl = "http://example.com/";
		int startIndex = 0;
		
		// First call
		HipChatEmoticon emoticon1 = new HipChatEmoticon(emoticonId, null, emoticonShortcut, emoticonUrl);
		List<HipChatEmoticon> items1 = new ArrayList<HipChatEmoticon>();
		items1.add(emoticon1);
		HipChatApiResultLinks links1 = new HipChatApiResultLinks(null, null, new String());
		HipChatEmoticons expectedEmoticons1 = new HipChatEmoticons(items1, startIndex, maxResults, links1);		

		// API call mocks
		HipChatApiProcessor processor = mock(HipChatApiProcessor.class);
		when(processor.getEmoticons(startIndex)).thenReturn(expectedEmoticons1);

		// Execute
		HipChatEmoticonCache emoticonCache = new HipChatEmoticonCache(processor);
		emoticonCache.reload();

		// Test
		AssertJUnit.assertEquals(1, emoticonCache.getSize());
		
		// Verifications
		verify(processor).getEmoticons(startIndex);
	}
	
	@Test
	public void testMultipleBatches() throws IOException {

		// Batch size
		int maxResults = 1;

		// First batch
		String emoticonId1 = "id1";
		String emoticonShortcut1 = "emo1";
		String emoticonUrl1 = "http://example.com/";
		int startIndex1 = 0;
		
		// Second batch
		String emoticonId2 = "id2";
		String emoticonShortcut2 = "emo2";
		String emoticonUrl2 = "http://example.com/";
		int startIndex2 = startIndex1 + maxResults;

		// First call
		HipChatEmoticon emoticon1 = new HipChatEmoticon(emoticonId1, null, emoticonShortcut1, emoticonUrl1);
		List<HipChatEmoticon> items1 = new ArrayList<HipChatEmoticon>();
		items1.add(emoticon1);
		HipChatApiResultLinks links1 = new HipChatApiResultLinks(null, null, new String());
		HipChatEmoticons expectedEmoticons1 = new HipChatEmoticons(items1, startIndex1, maxResults, links1);		

		// Second call
		HipChatEmoticon emoticon2 = new HipChatEmoticon(emoticonId2, null, emoticonShortcut2, emoticonUrl2);
		List<HipChatEmoticon> items2 = new ArrayList<HipChatEmoticon>();
		items1.add(emoticon2);
		HipChatApiResultLinks links2 = new HipChatApiResultLinks(null, null, null);
		HipChatEmoticons expectedEmoticons2 = new HipChatEmoticons(items2, startIndex1, maxResults, links2);		

		// API call mocks
		HipChatApiProcessor processor = mock(HipChatApiProcessor.class);
		when(processor.getEmoticons(startIndex1)).thenReturn(expectedEmoticons1);
		when(processor.getEmoticons(startIndex2)).thenReturn(expectedEmoticons2);

		// Execute
		HipChatEmoticonCache emoticonCache = new HipChatEmoticonCache(processor);
		emoticonCache.reload();

		// Test
		AssertJUnit.assertEquals(2, emoticonCache.getSize());
		
		// Verifications
		verify(processor).getEmoticons(startIndex1);
		verify(processor).getEmoticons(startIndex2);
	}
	
}
