/*
 * Copyright 2017-present the original author or authors.
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

package org.springframework.integration.support.json;

import org.springframework.util.ClassUtils;

/**
 * The utility to check if Jackson JSON processor is present in the classpath.
 *
 * @author Artem Bilan
 *
 * @since 4.3.10
 */
public final class JacksonPresent {

	private static final boolean JACKSON_2_PRESENT =
			ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", null) &&
					ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", null);

	public static boolean isJackson2Present() {
		return JACKSON_2_PRESENT;
	}

	private JacksonPresent() {
	}

}
