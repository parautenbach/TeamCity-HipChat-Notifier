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

import org.testng.annotations.Test;

import com.whatsthatlight.teamcity.hipchat.HipChatMessageColour;
import com.whatsthatlight.teamcity.hipchat.HipChatMessageFormat;

public class DummyTest {

	@Test
	public void forCoverageOnly() {
		// EMMA doesn't cover these classes fully, as their constructors are never invoked
		// The problem is that they are effectively static classes, but there's no support for static classes in Java
		new HipChatMessageColour();
		new HipChatMessageFormat();
	}

}
