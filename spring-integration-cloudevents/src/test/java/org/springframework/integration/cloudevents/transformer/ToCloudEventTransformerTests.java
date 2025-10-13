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

package org.springframework.integration.cloudevents.transformer;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import io.cloudevents.avro.compact.AvroCompactFormat;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.jackson.JsonFormat;
import io.cloudevents.spring.messaging.CloudEventMessageConverter;
import io.cloudevents.xml.XMLFormat;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig
class ToCloudEventTransformerTests {

	private static final String PAYLOAD = "test message";

	private ToCloudEventTransformer transformer;

	@Autowired
	private ApplicationContext applicationContext;

	private final Expression[] extensionExpressions = {new FunctionExpression<>(new Function<String, Boolean>() {

		@Override
		public Boolean apply(String message) {
			return Boolean.TRUE;//message.equals("customer-header");
		}
	})
	};

	@Test
	void doCloudEventConverterTransformWithPayload() {
		Message<?> result = getSampleMessage(PAYLOAD.getBytes(), Collections.singletonList(new JsonFormat()));
		assertThat(result.getPayload()).isEqualTo(PAYLOAD.getBytes());
		verifyMessageHeaders(result.getHeaders());
	}

	@Test
	void doJsonTransformWithPayload() {
		String expectedPayload = "{\"specversion\":\"1.0\",\"id\":\"test-id\",\"source\":\"test-source\",\"type\":\"test-type\"," +
				"\"data\":test message}";
		Message<?> result = getSampleMessage(PAYLOAD.getBytes(), Collections.singletonList(new JsonFormat()));
		assertThat(result.getPayload()).isEqualTo(expectedPayload.getBytes());
		verifyMessageHeaders(result.getHeaders());
	}

	@Test
	void doXmlTransformWithPayload() {
		String expectedPayload = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><event " +
				"xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
				"specversion=\"1.0\" xmlns=\"http://cloudevents.io/xmlformat/V1\"><id>test-id</id>" +
				"<source>test-source</source><type>test-type</type>" +
				"<data xsi:type=\"xs:base64Binary\">dGVzdCBtZXNzYWdl</data></event>";
		Message<?> result = getSampleMessage(PAYLOAD.getBytes(), Collections.singletonList(new XMLFormat()));
		assertThat(result.getPayload()).isEqualTo(expectedPayload.getBytes());
		verifyMessageHeaders(result.getHeaders());
	}

	@Test
	void doAvroTransformWithPayload() {
		byte[] expectedPayload = {-61, 1, -70, 39, -71, 34, 9, -16, -17, 86, 14, 116, 101, 115, 116, 45, 105, 100, 22,
				116, 101, 115, 116, 45, 115, 111, 117, 114, 99, 101, 18, 116, 101, 115, 116, 45, 116, 121, 112, 101,
				0, 0, 0, 0, 0, 0, 24, 116, 101, 115, 116, 32, 109, 101, 115, 115, 97, 103, 101};
		Message<?> result = getSampleMessage(PAYLOAD.getBytes(), Collections.singletonList(new AvroCompactFormat()));
		assertThat(result.getPayload()).isEqualTo(expectedPayload);
		verifyMessageHeaders(result.getHeaders());
	}

	@Test
	void doTransformWithObjectPayload() {
		this.transformer = new ToCloudEventTransformer(Collections.singletonList(new JsonFormat()), this.extensionExpressions);
		Object payload = new Object() {
			@Override
			public String toString() {
				return "custom object";
			}
		};
		Message<Object> message = MessageBuilder.withPayload(payload).setHeader("test_id", "test-id")
				.setHeader("test_source", "test-source")
				.setHeader("test_type", "test-type")
				.build();
		Object result = this.transformer.doTransform(message);

		assertThat(result).isNotNull();
		assertThat(result).isInstanceOf(Message.class);

		Message<?> resultMessage = (Message<?>) result;
		assertThat(resultMessage.getPayload()).isNotNull();
		assertThat(resultMessage.getPayload()).isEqualTo(payload.toString().getBytes());
	}

	@Test
	void emptyExtensionNames() {
		ToCloudEventTransformer emptyExtensionTransformer = new ToCloudEventTransformer(Collections.singletonList(new JsonFormat()), null);

		String payload = "test message";
		Message<byte[]> message = createBaseMessage(payload.getBytes())
			.setHeader("some-header", "some-value")
			.build();

		Object result = emptyExtensionTransformer.doTransform(message);

		assertThat(result).isNotNull();
		Message<?> resultMessage = (Message<?>) result;

		// All headers should be preserved when no extension mapping exists
		assertThat(resultMessage.getHeaders().containsKey("some-header")).isTrue();
		assertThat(resultMessage.getHeaders().get("some-header")).isEqualTo("some-value");
	}

	@Test
	void multipleExtensionMappings() {
		String[] extensionPatterns = {"trace-id", "span-id", "user-id"};

		ToCloudEventTransformer extendedTransformer = new ToCloudEventTransformer(Collections.singletonList(new JsonFormat()),this.extensionExpressions);
		String payload = "test message";
		Message<byte[]> message = createBaseMessage(payload.getBytes())
			.setHeader("trace-id", "trace-123")
			.setHeader("span-id", "span-456")
			.setHeader("user-id", "user-789")
			.setHeader("correlation-id", "corr-999")
			.build();

		Object result = extendedTransformer.doTransform(message);

		assertThat(result).isNotNull();
		Message<?> resultMessage = (Message<?>) result;

		assertThat(resultMessage.getHeaders()).containsKeys("correlation-id",
				"ce-trace-id", "ce-span-id", "ce-user-id");
		assertThat(resultMessage.getHeaders().get("correlation-id")).isEqualTo("corr-999");
	}

	@Test
	void emptyStringPayloadHandling() {
		Message<byte[]> message = createBaseMessage("".getBytes()).build();
		this.transformer = new ToCloudEventTransformer(Collections.singletonList(new JsonFormat()), this.extensionExpressions);
		Object result = this.transformer.doTransform(message);

		assertThat(result).isNotNull();
		assertThat(result).isInstanceOf(Message.class);
	}

	@Test
	void defaultConstructorUsesDefaultCloudEventProperties() {
		ToCloudEventTransformer defaultTransformer = new ToCloudEventTransformer(Collections.singletonList(new JsonFormat()));

		String payload = "test default properties";
		Message<byte[]> message = createBaseMessage(payload.getBytes()).build();

		Object result = defaultTransformer.doTransform(message);

		assertThat(result).isNotNull();
		assertThat(result).isInstanceOf(Message.class);
	}

	@Test
	void failWhenNoIdHeaderAndNoDefault() {
		ToCloudEventTransformer transformer = new ToCloudEventTransformer(Collections.singletonList(new JsonFormat()));

		Message<?> message = MessageBuilder.withPayload("test")
				.setHeader("source", "test-source")
				.setHeader("type", "test-type")
				.build();

		assertThatThrownBy(() -> transformer.transform(message)).isInstanceOf(MessageTransformationException.class)
				.hasMessageContaining("failed to transform message")
				.cause().hasMessageContaining("missing_id");
	}

	@Test
	void failWhenMultipleHeadersMatchPattern() {
		ToCloudEventTransformer transformer = new ToCloudEventTransformer(Collections.singletonList(new JsonFormat()));

		Message<?> message = MessageBuilder.withPayload("test")
				.setHeader("id1", "test-id-1")
				.setHeader("id2", "test-id-2")
				.setHeader("source", "test-source")
				.setHeader("type", "test-type")
				.build();

		assertThatThrownBy(() -> transformer.transform(message))
				.hasMessageContaining("failed to transform message")
				.cause().hasMessageContaining("Multiple headers");
	}

	@Test
	void failWhenIdIsNotString() {
		ToCloudEventTransformer transformer = new ToCloudEventTransformer(Collections.singletonList(new JsonFormat()));

		Message<?> message = MessageBuilder.withPayload("test")
				.setHeader("id_test", 1234)  // Integer, not String
				.setHeader("source", "test-source")
				.setHeader("type", "test-type")
				.build();

		assertThatThrownBy(() -> transformer.transform(message))
				.cause()
				.hasMessageContaining("Field in CloudEvent must be a String but header contains:")
				.hasMessageContaining("String");
	}

	@SuppressWarnings("unchecked")
	private Message<byte[]> getSampleMessage(byte[] payload, @Nullable List<EventFormat> eventFormats) {
		this.transformer = (eventFormats != null) ? new ToCloudEventTransformer(eventFormats, this.extensionExpressions) :
				new ToCloudEventTransformer(Collections.singletonList(new JsonFormat()), this.extensionExpressions);
		this.transformer.setApplicationContext(this.applicationContext);
		Message<byte[]> message = createBaseMessage(payload)
				.setHeader("custom-header", "test-value")
				.setHeader("other-header", "other-value")
				.build();
		Object result = this.transformer.doTransform(message);

		assertThat(result).isNotNull();
		assertThat(result).isInstanceOf(Message.class);
		return (Message<byte[]>) result;
	}

	private void verifyMessageHeaders(MessageHeaders headers) {
		assertThat(headers).isNotNull();
		assertThat(headers.containsKey("other-header")).isTrue();
		assertThat(headers.get("other-header")).isEqualTo("other-value");
	}

	private MessageBuilder<byte[]> createBaseMessage(byte[] payload) {
		MessageBuilder<byte[]> messageBuilder = MessageBuilder.withPayload(payload)
				.setHeader("test_id", "test-id")
				.setHeader("test_source", "test-source")
				.setHeader("test_type", "test-type");
		return messageBuilder;
	}

}
