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

package org.springframework.integration.test.support;

import java.util.Collections;
import java.util.List;

/**
 * Convenience class for a single {@link RequestResponseScenario} test
 *
 * @author David Turanski
 * @author Jiandong Ma
 *
 * @deprecated since 7.0 in favor of {@link SingleRequestResponseScenarioTest}
 */
@Deprecated(since = "7.0", forRemoval = true)
@SuppressWarnings("removal")
public abstract class SingleRequestResponseScenarioTests extends AbstractRequestResponseScenarioTests {

	@Override
	protected List<RequestResponseScenario> defineRequestResponseScenarios() {
		return Collections.singletonList(defineRequestResponseScenario());
	}

	protected abstract RequestResponseScenario defineRequestResponseScenario();

}
