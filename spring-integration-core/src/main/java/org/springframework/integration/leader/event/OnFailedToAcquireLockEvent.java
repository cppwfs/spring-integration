/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.leader.event;

import org.springframework.integration.leader.Context;

/**
 * Generic event representing that a lock could not be acquired during leader election.
 *
 * @author Glenn Renfro
 *
 * @since 5.0.0
 */
public class OnFailedToAcquireLockEvent extends AbstractLeaderEvent {

	/**
	 * Instantiates a new onFailedToAquireLock event.
	 *
	 * @param source the component that published the event (never {@code null})
	 * @param context the context associated with this event
	 * @param role the role of the leader
	 */
	public OnFailedToAcquireLockEvent(Object source, Context context, String role) {
		super(source, context, role);
	}
}
