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

package com.whatsthatlight.teamcity.hipchat;

import org.apache.log4j.Logger;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonAnySetter;

public class HipChatApiResponseLinks {

	private static Logger logger = Logger.getLogger("com.whatsthatlight.teamcity.hipchat");
	
	@JsonProperty("self")
	public String self;
	
	@JsonProperty("webhooks")
	public String webhooks;
	
	@JsonProperty("members")
	public String members;
	
	public HipChatApiResponseLinks() {
		// Intentionally left empty
	}
	
	public HipChatApiResponseLinks(String self, String webhooks, String members) {
		this.self = self;
		this.webhooks = webhooks;
		this.members = members;
	}
	
	@JsonAnySetter
	public void handleUnknown(String key, Object value) {
	    logger.debug(String.format("Unknown property: %s", key));
	}
	
}
