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

package org.springframework.integration.stomp.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.stomp.outbound.StompMessageHandler;

/**
 * The {@link AbstractOutboundChannelAdapterParser} implementation for
 * the {@code <stomp:outbound-channel-adapter/>} element.
 *
 * @author Artem Bilan
 * @since 4.2
 */
public class StompOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(StompMessageHandler.class);
		StompAdapterParserUtils.configureStompAdapter(builder, parserContext, element);
		BeanDefinition expressionDef =
				IntegrationNamespaceUtils.createExpressionDefinitionFromValueOrExpression("destination",
						"destination-expression", parserContext, element, false);
		if (expressionDef != null) {
			builder.addPropertyValue("destinationExpression", expressionDef);
		}
		return builder.getBeanDefinition();

	}

}
