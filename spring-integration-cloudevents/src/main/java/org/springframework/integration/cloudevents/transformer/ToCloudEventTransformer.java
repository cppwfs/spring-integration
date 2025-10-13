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

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventExtension;
import io.cloudevents.CloudEventExtensions;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.provider.EventFormatProvider;
import org.jspecify.annotations.Nullable;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.transformer.AbstractTransformer;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * A Spring Integration transformer that converts messages to CloudEvent format.
 * Header filtering and extension mapping is performed based on configurable patterns,
 * allowing control over which headers are preserved and which become CloudEvent extensions.
 *
 * @author Glenn Renfro
 *
 * @since 7.0
 */
public class ToCloudEventTransformer extends AbstractTransformer {

	private Expression idExpression = new FunctionExpression<Message<?>>(o -> Objects.requireNonNull(o.getHeaders().getId()).toString());

	@SuppressWarnings("NullAway.Init")
	private Expression sourceExpression;

	private Expression typeExpression = new LiteralExpression("spring.message");

	private @Nullable Expression dataSchemaExpression;

	private @Nullable Expression subjectExpression;

	private final Expression @Nullable [] cloudEventExtensionExpressions;

	private List<EventFormat> eventFormats;

	@SuppressWarnings("NullAway.Init")
	private EvaluationContext evaluationContext;

	private EventFormatProvider eventFormatProvider = EventFormatProvider.getInstance();

	/**
	 * ToCloudEventTransformer Constructor
	 *
	 * @param eventFormats {@link EventFormat}s that will be used to convert the message to a CloudEvent message.
	 * @param cloudEventExtensionExpressions an array of {@link Expression} for matching headers that should become CloudEvent extensions
	 */
	public ToCloudEventTransformer(List<EventFormat> eventFormats,
			Expression @Nullable ... cloudEventExtensionExpressions) {
		this.eventFormats = eventFormats;
		this.cloudEventExtensionExpressions = cloudEventExtensionExpressions;
	}

	public void setIdExpression(Expression idExpression) {
		this.idExpression = idExpression;
	}

	public void setSourceExpression(Expression sourceExpression) {
		this.sourceExpression = sourceExpression;
	}

	public void setTypeExpression(Expression typeExpression) {
		this.typeExpression = typeExpression;
	}

	public void setDataSchemaExpression(@Nullable Expression dataSchemaExpression) {
		this.dataSchemaExpression = dataSchemaExpression;
	}

	public void setSubjectExpression(@Nullable Expression subjectExpression) {
		this.subjectExpression = subjectExpression;
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
		String appName = this.getApplicationContext().getEnvironment().getProperty("spring.application.name");
		appName = appName == null ? "unknown" : appName;
		this.sourceExpression = new LiteralExpression("/spring/" + appName + "." + getBeanName());
	}

	/**
	 * Transforms the input message into a CloudEvent message.
	 *
	 * @param message the input Spring Integration message to transform
	 * @return transformed message as CloudEvent in the specified format
	 * @throws RuntimeException if serialization fails
	 */
	@Override
	protected Object doTransform(Message<?> message) {
		Assert.isInstanceOf(byte[].class, message.getPayload());
		ToCloudEventTransformerExtensions extensions =
				new ToCloudEventTransformerExtensions(this.evaluationContext, message.getHeaders(),
						this.cloudEventExtensionExpressions);
		CloudEvent cloudEvent = CloudEventBuilder.v1()
				.withId(this.idExpression.getValue(this.evaluationContext, message, String.class))
				.withSource(this.sourceExpression.getValue(this.evaluationContext, message, URI.class))
				.withType(this.typeExpression.getValue(this.evaluationContext, message, String.class))
				.withTime(OffsetDateTime.now())
				.withDataContentType(message.getHeaders().get(MessageHeaders.CONTENT_TYPE, String.class))
				.withDataSchema(Objects.requireNonNull(this.dataSchemaExpression).getValue(this.evaluationContext, message, URI.class))
				.withSubject(Objects.requireNonNull(this.subjectExpression).getValue(this.evaluationContext, message, String.class))
				.withData((byte[]) message.getPayload())
				.withExtension(extensions)
				.build();

		String contentType = message.getHeaders().get(MessageHeaders.CONTENT_TYPE, String.class);
		if (contentType == null) {
			throw new MessageTransformationException(message, "Missing 'Content-Type' header");
		}
		EventFormat eventFormat = this.eventFormatProvider.resolveFormat(contentType);
		return MessageBuilder.withPayload(Objects.requireNonNull(eventFormat).serialize(cloudEvent))
				.copyHeaders(message.getHeaders())
				.build();
	}

	@Override
	public String getComponentType() {
		return "ce:to-cloudevents-transformer";
	}

	private static class ToCloudEventTransformerExtensions implements CloudEventExtension {

		/**
		 * Map storing the CloudEvent extensions extracted from message headers.
		 */
		private final Map<String, Object> cloudEventExtensions;

		/**
		 * Construct CloudEvent extensions by filtering message headers against expressions that deterimine if each of the headers is an extension.
		 * <p>
		 * Headers are evaluated against the provided {@link Expression}.
		 * Only headers that have an expression that returns true will be included as CloudEvent extension.
		 *
		 * @param headers the Spring Integration message headers to process
		 * @param expressions an array of {@link Expression}s that accepts a message and returns a {@link Boolean}.
		 */
		ToCloudEventTransformerExtensions(EvaluationContext evaluationContext, MessageHeaders headers, Expression @Nullable ... expressions) {
			this.cloudEventExtensions = new HashMap<>();
			headers.keySet().forEach(key -> {
				Boolean result = categorizeHeader(evaluationContext, key, expressions);
				if (result != null && result) {
					this.cloudEventExtensions.put(key, Objects.requireNonNull(headers.get(key)));
				}
			});
		}

		@Override
		public void readFrom(CloudEventExtensions extensions) {
			extensions.getExtensionNames()
					.forEach(key -> {
						Object value = extensions.getExtension(key);
						if (value != null) {
							this.cloudEventExtensions.put(key, value);
						}
					});
		}

		@Override
		public @Nullable Object getValue(String key) throws IllegalArgumentException {
			return this.cloudEventExtensions.get(key);
		}

		@Override
		public Set<String> getKeys() {
			return this.cloudEventExtensions.keySet();
		}

		/**
		 * Categorizes a header value by matching it against an array of {@link Expression}s.
		 * <p>
		 * This method takes a header value and executes an expression
		 * specified in an array {@link Expression}s.
		 *
		 * @param value the header value to match against the patterns
		 * @param expressions an array of string patterns to match against, or null.  If pattern is null then null is returned.
		 * @return {@code Boolean.TRUE} if the value starts with a pattern token,
		 *         {@code Boolean.FALSE} if the value starts with the pattern token that is prefixed with a `!`,
		 *         or {@code null} if the header starts with a value that is not enumerated in the pattern
		 */
		static @Nullable Boolean categorizeHeader(EvaluationContext evaluationContext, String value, Expression @Nullable ... expressions) {
			Boolean result = null;
			if (expressions != null) {
				for (Expression extension : expressions) {
					result = extension.getValue(evaluationContext, value, Boolean.class);
					if (result != null && result) {
						break;
					}
					else if (result != null) {
						break;
					}
				}
			}
			return result;
		}

	}

}
