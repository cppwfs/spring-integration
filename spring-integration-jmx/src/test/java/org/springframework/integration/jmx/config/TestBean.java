/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jmx.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @since 2.0
 */
@ManagedResource
public class TestBean {

	final List<String> messages = new ArrayList<String>();

	@ManagedAttribute
	public String getFirstMessage() {
		return (messages.size() > 0) ? messages.get(0) : null;
	}

	@ManagedOperation
	public void test(String text) {
		this.messages.add(text);
	}

	@ManagedOperation
	public List<String> testWithReturn(String text) {
		this.messages.add(text);
		return messages;
	}

	@ManagedOperation
	public void testPrimitiveArgs(boolean bool, long time, int foo) {
		this.messages.add(bool + " " + time + " " + foo);
	}

}
