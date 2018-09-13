/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.cloud.servicebroker.service;

import reactor.core.publisher.Mono;

import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.GetServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.GetServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.service.events.EventFlowRegistries;

/**
 * Internal implementation of {@link ServiceInstanceBindingService} that attaches event
 * hooks to the requests to create and delete service instance bindings
 *
 * @author Roy Clarkson
 */
public class ServiceInstanceBindingEventService implements ServiceInstanceBindingService {

	private final ServiceInstanceBindingService service;

	private final EventFlowRegistries flows;

	public ServiceInstanceBindingEventService(ServiceInstanceBindingService service, EventFlowRegistries flows) {
		this.service = service;
		this.flows = flows;
	}

	@Override
	public Mono<CreateServiceInstanceBindingResponse> createServiceInstanceBinding(CreateServiceInstanceBindingRequest request) {
		return flows.getCreateInstanceBindingRegistry().getInitializationFlows(request)
				.then(service.createServiceInstanceBinding(request))
				.onErrorResume(e -> flows.getCreateInstanceBindingRegistry().getErrorFlows(request, e)
						.then(Mono.error(e)))
				.flatMap(response -> flows.getCreateInstanceBindingRegistry().getCompletionFlows(request, response)
						.then(Mono.just(response)));
	}

	@Override
	public Mono<GetServiceInstanceBindingResponse> getServiceInstanceBinding(GetServiceInstanceBindingRequest request) {
		return service.getServiceInstanceBinding(request);
	}

	@Override
	public Mono<Void> deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest request) {
		return flows.getDeleteInstanceBindingRegistry().getInitializationFlows(request)
				.then(service.deleteServiceInstanceBinding(request))
				.onErrorResume(e -> flows.getDeleteInstanceBindingRegistry().getErrorFlows(request, e)
						.then(Mono.error(e)))
				.thenEmpty(flows.getDeleteInstanceBindingRegistry().getCompletionFlows(request));
	}
}
