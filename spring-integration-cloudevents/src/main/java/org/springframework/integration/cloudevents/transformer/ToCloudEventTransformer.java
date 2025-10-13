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
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventExtension;
import io.cloudevents.CloudEventExtensions;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.format.EventFormat;
import org.jspecify.annotations.Nullable;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.cloudevents.FormatStrategy;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.support.utils.PatternMatchUtils;
import org.springframework.integration.transformer.AbstractTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
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

	private Expression idExpression = new FunctionExpression<Message<?>>(o -> o.getHeaders().getId().toString());

	@SuppressWarnings("NullAway.Init")
	private Expression sourceExpression;

	private Expression typeExpression = new LiteralExpression("spring.message");

	private @Nullable URI dataSchema;

	private @Nullable String subject;

	private final String @Nullable [] cloudEventExtensionPatterns;

	private @Nullable FormatStrategy formatStrategy;

	private @Nullable MessageConverter messageConverter;

	private EvaluationContext evaluationContext;

	/**
	 * ToCloudEventTransformer Constructor
	 *
	 * @param formatStrategy The strategy that determines how the CloudEvent will be rendered
	 * @param idPattern a pattern for matching the pattern for the cloud event id to a header in the message.
	 * @param sourcePattern a pattern for matching the pattern for the cloud event source to a header in the message.
	 * @param typePattern a pattern for matching the pattern for the cloud event type to a header in the message.
	 * @param cloudEventExtensionPatterns an array of patterns for matching headers that should become CloudEvent extensions,
	 * supports wildcards and negation with '!' prefix   If a header matches one of the '!' it is excluded from
	 * cloud event headers and the message headers. Null to disable extension mapping.
	 */
	public ToCloudEventTransformer(EventFormat formatStrategy, String idPattern, String sourcePattern,
			String typePattern, String @Nullable ... cloudEventExtensionPatterns) {
		this.cloudEventExtensionPatterns = cloudEventExtensionPatterns;
		this.formatStrategy = formatStrategy;
		this.idPattern = idPattern;
		this.sourcePattern = sourcePattern;
		this.typePattern = typePattern;
	}

	/**
	 * ToCloudEventTransformer Constructor
	 *
	 * @param messageConverter The strategy that determines how the CloudEvent message will be converted to the proper format
	 * @param idPattern a pattern for matching the pattern for the cloud event id to a header in the message.
	 * @param sourcePattern a pattern for matching the pattern for the cloud event source to a header in the message.
	 * @param typePattern a pattern for matching the pattern for the cloud event type to a header in the message.
	 * @param cloudEventExtensionPatterns an array of patterns for matching headers that should become CloudEvent extensions,
	 * supports wildcards and negation with '!' prefix   If a header matches one of the '!' it is excluded from
	 * cloud event headers and the message headers. Null to disable extension mapping.
	 */
	public ToCloudEventTransformer(MessageConverter messageConverter, String idPattern, String sourcePattern,
			String typePattern, String @Nullable ... cloudEventExtensionPatterns) {
		this.cloudEventExtensionPatterns = cloudEventExtensionPatterns;
		this.messageConverter = messageConverter;
		this.idPattern = idPattern;
		this.sourcePattern = sourcePattern;
		this.typePattern = typePattern;
	}

	public void setIdExpression(Expression idExpression) {
		this.idExpression = idExpression;
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
		if (this.sourceExpression == null) {
			String appName = this.getApplicationContext().getEnvironment().getProperty("spring.application.name");
			appName = appName == null ? "unknown" : appName;
			this.sourceExpression = new LiteralExpression("/spring/" + appName + "." + getBeanName());
		}

	}

	/**
	 * Transforms the input message into a CloudEvent message.
	 * <p>
	 * This method performs the core transformation logic:
	 * <ol>
	 *   <li>Extracts CloudEvent extensions from message headers using configured patterns</li>
	 *   <li>Builds a CloudEvent with the configured properties and message payload</li>
	 *   <li>Applies the specified conversion type to format the output</li>
	 *   <li>Filters headers to exclude those mapped to CloudEvent extensions</li>
	 * </ol>
	 *
	 * @param message the input Spring Integration message to transform
	 * @return transformed message as CloudEvent in the specified format
	 * @throws RuntimeException if serialization fails for XML, JSON, or Avro formats
	 */
	@Override
	protected Object doTransform(Message<?> message) {
		Assert.isInstanceOf(byte[].class, message.getPayload());
		Object result = null;
		ToCloudEventTransformerExtensions extensions =
				new ToCloudEventTransformerExtensions(message.getHeaders(), this.cloudEventExtensionPatterns);
		CloudEvent cloudEvent = CloudEventBuilder.v1()
				.withId(this.idExpression.getValue(this.evaluationContext, message, String.class))
				.withSource(this.sourceExpression.getValue(this.evaluationContext, message, URI.class))
				.withType(this.typeExpression.getValue(this.evaluationContext, message, String.class))
				.withTime(OffsetDateTime.now())
				.withDataContentType(message.getHeaders().get(MessageHeaders.CONTENT_TYPE, String.class))
				.withDataSchema(this.dataSchema)
				.withSubject(this.subject)
				.withData((byte[])message.getPayload())
				.withExtension(extensions)
				.build();
		if (this.messageConverter != null) {
			result = this.messageConverter.toMessage(cloudEvent, filterHeaders(message.getHeaders()));
			Assert.state(result != null, "MessageConverter was unable to convert Message " +
					"to CloudEvent");
		}
		else if (this.formatStrategy != null) {
			result = MessageBuilder.withPayload(this.formatStrategy.toByteArray(cloudEvent))
					.copyHeaders(filterHeaders(message.getHeaders()))
					.build();

		}
		Assert.state(result != null, "Transformer was unable to convert Message to CloudEvent");
		return result;
	}

	private Function<Object, String> getStringValueFunction() {
		return id -> {
			if ((id instanceof String idString)) {
				return idString;
			}
			throw new IllegalStateException(
					"Field in CloudEvent must be a String but header contains: " + id.getClass().getName());
		};
	}

	private Function<Object, URI> getSourceValueFunction() {
		return  object -> {
			if (object instanceof String stringUri) {
				return URI.create(stringUri);
			}
			else if (object instanceof URI uri) {
				return uri;
			}
			else {
				throw new IllegalStateException(
						"CloudEvent 'source' must be String or URI but header contains: " +
								object.getClass().getName());
			}
		};
	}

	private  <T> @Nullable T getAttribute(MessageHeaders headers, String pattern,
			@Nullable T defaultAttribute, Function<Object, T> converter) {
		Set<Object> attributes = getAttributesForPattern(headers, pattern);
		Assert.state(!attributes.isEmpty() || defaultAttribute != null, "No attribute for " +
				pattern + " found");
		if (!attributes.isEmpty()) {
			Object headerValue = attributes.iterator().next();
			return converter.apply(headerValue);
		}
		return defaultAttribute;
	}

	private static Set<Object> getAttributesForPattern(MessageHeaders headers, String pattern) {
		Set<Object> attributes = new HashSet<>();
		headers.keySet().forEach(key -> {
			Boolean result = ToCloudEventTransformerExtensions.categorizeHeader(key, pattern);
			if (result != null) {
				attributes.add(Objects.requireNonNull(headers.get(key)));
			}
		});
		Assert.state(attributes.size() < 2, "Multiple headers match the '" + pattern + "'");
		return attributes;
	}

	@Override
	public String getComponentType() {
		return "ce:to-cloudevents-transformer";
	}

	public void setDataSchema(@Nullable URI dataSchema) {
		this.dataSchema = dataSchema;
	}

	public void setSubject(@Nullable String subject) {
		this.subject = subject;
	}

	/**
	 * This method creates a {@link MessageHeaders} that were not placed in the CloudEvent and were not excluded via the
	 * categorization mechanism.
	 * @param headers The {@link MessageHeaders} to be filtered.
	 * @return {@link MessageHeaders} that have been filtered.
	 */
	private MessageHeaders filterHeaders(MessageHeaders headers) {

		Map<String, Object> filteredHeaders = new HashMap<>();
		headers.keySet().forEach(key -> {
			if (ToCloudEventTransformerExtensions.categorizeHeader(key, this.cloudEventExtensionPatterns) == null) {
				filteredHeaders.put(key, Objects.requireNonNull(headers.get(key)));
			}
		});
		return new MessageHeaders(filteredHeaders);
	}

	private static class ToCloudEventTransformerExtensions implements CloudEventExtension {

		/**
		 * Map storing the CloudEvent extensions extracted from message headers.
		 */
		private final Map<String, Object> cloudEventExtensions;

		/**
		 * Construct CloudEvent extensions by filtering message headers against patterns.
		 * <p>
		 * Headers are evaluated against the provided patterns.
		 * Only headers that match the patterns (and are not excluded by negation patterns)
		 * will be included as CloudEvent extensions.
		 *
		 * @param headers the Spring Integration message headers to process
		 * @param patterns an array patterns for header matching, may be null to include no extensions
		 */
		ToCloudEventTransformerExtensions(MessageHeaders headers, String @Nullable ... patterns) {
			this.cloudEventExtensions = new HashMap<>();
			headers.keySet().forEach(key -> {
				Boolean result = categorizeHeader(key, patterns);
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
		 * Categorizes a header value by matching it against an array of pattern strings.
		 * <p>
		 * This method takes a header value and matches it against one or more patterns
		 * specified in an array strings. It uses Spring's smart pattern matching
		 * which supports wildcards and other pattern matching features.
		 *
		 * @param value the header value to match against the patterns
		 * @param patterns an array of string patterns to match against, or null.  If pattern is null then null is returned.
		 * @return {@code Boolean.TRUE} if the value starts with a pattern token,
		 *         {@code Boolean.FALSE} if the value starts with the pattern token that is prefixed with a `!`,
		 *         or {@code null} if the header starts with a value that is not enumerated in the pattern
		 */
		static @Nullable Boolean categorizeHeader(String value, String @Nullable ... patterns) {
			Boolean result = null;
			if (patterns != null) {
				for (String patternItem : patterns) {
					result = PatternMatchUtils.smartMatch(value, patternItem);
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
