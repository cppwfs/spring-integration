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

package org.springframework.integration.grpc.outbound;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.MethodDescriptor;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;

/**
 * Dynamic invoker for gRPC methods using {@code MethodDescriptor}s.
 * <p>Provides methods for invoking gRPC calls with different patterns:
 * blocking, async unary/server-streaming, and bidirectional streaming.
 *
 * @author Glenn Renfro
 * @since 7.1
 */
public class GrpcDynamicInvoker {

	private final Channel channel;

	private final CallOptions callOptions;

	/**
	 * Create a new {@code GrpcDynamicInvoker}.
	 * @param channel the gRPC channel
	 */
	public GrpcDynamicInvoker(Channel channel) {
		this.channel = channel;
		this.callOptions = CallOptions.DEFAULT;
	}

	/**
	 * Invoke a unary or server-streaming gRPC method asynchronously.
	 * @param <ReqT> the request type
	 * @param <RespT> the response type
	 * @param method the method descriptor to invoke
	 * @param request the request object
	 * @param responseObserver the observer to receive responses
	 * @throws UnsupportedOperationException if the method type is client or bidirectional streaming
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public <ReqT, RespT> void invoke(MethodDescriptor method, ReqT request,
			StreamObserver<RespT> responseObserver) {

		ClientCall<ReqT, RespT> call = this.channel.newCall(method, this.callOptions);

		switch (method.getType()) {
			case UNARY -> ClientCalls.asyncUnaryCall(call, request, responseObserver);
			case SERVER_STREAMING -> ClientCalls.asyncServerStreamingCall(call, request, responseObserver);
			case CLIENT_STREAMING, BIDI_STREAMING ->
					throw new UnsupportedOperationException("Use invokeBiDirectional() for streaming methods");
			default -> throw new IllegalStateException("Unknown method type: " + method.getType());
		}
	}

	/**
	 * Invoke a client-streaming or bidirectional-streaming gRPC method.
	 * @param <ReqT> the request type
	 * @param <RespT> the response type
	 * @param method the method descriptor to invoke
	 * @param responseObserver the observer to receive responses
	 * @return a {@code StreamObserver} for sending requests
	 * @throws UnsupportedOperationException if the method type is unary or server streaming
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public <ReqT, RespT> StreamObserver<ReqT> invokeBiDirectional(MethodDescriptor method,
			StreamObserver<RespT> responseObserver) {

		ClientCall<ReqT, RespT> call = this.channel.newCall(method, this.callOptions);

		return switch (method.getType()) {
			case CLIENT_STREAMING -> ClientCalls.asyncClientStreamingCall(call, responseObserver);
			case BIDI_STREAMING -> ClientCalls.asyncBidiStreamingCall(call, responseObserver);
			case UNARY, SERVER_STREAMING ->
					throw new UnsupportedOperationException("Use invoke() for unary/server-streaming methods");
			default -> throw new IllegalStateException("Unknown method type: " + method.getType());
		};
	}

	/**
	 * Invoke a unary gRPC method with blocking semantics.
	 * @param <ReqT> the request type
	 * @param <RespT> the response type
	 * @param method the method descriptor to invoke
	 * @param request the request object
	 * @return the response object
	 * @throws IllegalArgumentException if the method type is not unary
	 */
	public <ReqT, RespT> RespT invokeBlocking(MethodDescriptor<ReqT, RespT> method, ReqT request) {
		if (method.getType() != MethodDescriptor.MethodType.UNARY) {
			throw new IllegalArgumentException("Blocking invocation only supports UNARY methods, " +
					"but method type is: " + method.getType());
		}
		return ClientCalls.blockingUnaryCall(this.channel, method, this.callOptions, request);
	}

}
