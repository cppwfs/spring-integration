/*
 * Copyright 2015-present the original author or authors.
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

package org.springframework.integration.hazelcast.inbound;

import com.hazelcast.topic.ITopic;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.hazelcast.inbound.util.HazelcastInboundChannelAdapterTestUtils;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Hazelcast Distributed Topic Event Driven Inbound Channel Adapter Test
 *
 * @author Eren Avsarogullari
 * @author Artem Bilan
 *
 * @since 6.0
 */
@SpringJUnitConfig
@DirtiesContext
@SuppressWarnings({"unchecked", "rawtypes"})
public class HazelcastDistributedTopicEventDrivenInboundChannelAdapterTests {

	@Autowired
	private PollableChannel edTopicChannel1;

	@Autowired
	private ITopic edDistributedTopic1;

	@Test
	public void testEventDrivenForOnlyADDEDEntryEvent() {
		HazelcastInboundChannelAdapterTestUtils
				.testEventDrivenForTopicMessageEvent(edDistributedTopic1, edTopicChannel1);
	}

}
