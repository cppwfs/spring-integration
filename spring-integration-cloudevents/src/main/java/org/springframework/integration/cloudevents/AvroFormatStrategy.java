/*
 * Copyright 2025-present the original author or authors.
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

package org.springframework.integration.cloudevents;

import io.cloudevents.CloudEvent;
import io.cloudevents.avro.compact.AvroCompactFormat;
import io.cloudevents.core.format.EventSerializationException;

/**
 * Serializes a {@link CloudEvent} via AVRO as an implementation of the {@link FormatStrategy} interface.
 *
 * @author Glenn Renfro
 * @since 7.0
 */
public class AvroFormatStrategy implements FormatStrategy {

	private static final AvroCompactFormat avroFormat = new AvroCompactFormat();

	/**
	 * Serializes the payload {@link CloudEvent} to the AVRO format.
	 *
	 * @param cloudEvent the CloudEvent to be converted to a byte array.
	 * @return the converted byte[]
	 * @throws EventSerializationException if CloudEvent can't be serialized
	 */
	@Override
	public byte[] toByteArray(CloudEvent cloudEvent) {
		return avroFormat.serialize(cloudEvent);
	}
}
