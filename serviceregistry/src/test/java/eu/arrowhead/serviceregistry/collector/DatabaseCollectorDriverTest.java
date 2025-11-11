/*******************************************************************************
 *
 * Copyright (c) 2025 AITIA
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 *
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  	AITIA - implementation
 *  	Arrowhead Consortia - conceptualization
 *
 *******************************************************************************/
package eu.arrowhead.serviceregistry.collector;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;

import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.intf.properties.validators.HttpOperationsValidator;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.service.validation.name.InterfaceTemplateNameNormalizer;
import eu.arrowhead.common.service.validation.name.InterfaceTemplateNameValidator;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceDefinition;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInstance;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInstanceInterface;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInterfaceTemplate;
import eu.arrowhead.serviceregistry.jpa.service.ServiceInstanceDbService;
import eu.arrowhead.serviceregistry.jpa.entity.System;
import eu.arrowhead.serviceregistry.service.model.ServiceLookupFilterModel;

@ExtendWith(MockitoExtension.class)
public class DatabaseCollectorDriverTest {

	//=================================================================================================
	// members

	@InjectMocks
	private DatabaseCollectorDriver driver;

	@Mock
	private ServiceDefinitionNameValidator serviceDefNameValidator;

	@Mock
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

	@Mock
	private InterfaceTemplateNameValidator interfaceTemplateNameValidator;

	@Mock
	private InterfaceTemplateNameNormalizer interfaceTemplateNameNormalizer;

	@Mock
	private ServiceInstanceDbService instanceDbService;

	@Mock
	private HttpOperationsValidator httpOperationsValidator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInit() {
		assertDoesNotThrow(() -> driver.init());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAcquireServiceServiceDefNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.acquireService(null, "generic_http", "ProviderName"));

		assertEquals("service definition is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAcquireServiceServiceDefEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.acquireService("", "generic_http", "ProviderName"));

		assertEquals("service definition is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAcquireServiceUnsupportedInterface() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> driver.acquireService("testService", "custom_http", "ProviderName"));

		assertTrue(ex.getMessage().startsWith("This collector only supports the following interfaces: "));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAcquireServiceNoMatchingInstances() {
		final PageRequest pagination = PageRequest.of(0, 1, Direction.DESC, ServiceInstance.DEFAULT_SORT_FIELD);

		final ServiceLookupFilterModel filterModel = new ServiceLookupFilterModel(
				new ServiceInstanceLookupRequestDTO.Builder()
						.serviceDefinitionName("testService")
						.interfaceTemplateName("generic_http")
						.build());

		when(serviceDefNameNormalizer.normalize("testService")).thenReturn("testService");
		doNothing().when(serviceDefNameValidator).validateServiceDefinitionName("testService");
		when(interfaceTemplateNameNormalizer.normalize("generic_http")).thenReturn("generic_http");
		doNothing().when(interfaceTemplateNameValidator).validateInterfaceTemplateName("generic_http");
		when(instanceDbService.getPageByFilters(pagination, filterModel)).thenReturn(new PageImpl<>(List.of()));

		final ServiceModel result = driver.acquireService("testService", "generic_http", null);

		assertNull(result);

		verify(serviceDefNameNormalizer).normalize("testService");
		verify(serviceDefNameValidator).validateServiceDefinitionName("testService");
		verify(interfaceTemplateNameNormalizer).normalize("generic_http");
		verify(interfaceTemplateNameValidator).validateInterfaceTemplateName("generic_http");
		verify(instanceDbService).getPageByFilters(pagination, filterModel);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAcquireServiceNoMatchingInstancesWithProviderName() {
		final PageRequest pagination = PageRequest.of(0, 1, Direction.DESC, ServiceInstance.DEFAULT_SORT_FIELD);

		final ServiceLookupFilterModel filterModel = new ServiceLookupFilterModel(
				new ServiceInstanceLookupRequestDTO.Builder()
						.serviceDefinitionName("testService")
						.interfaceTemplateName("generic_http")
						.build());

		final ServiceInstance instance = new ServiceInstance(
				"OtherProvider|testService|1.0.0",
				new System("OtherProvider", "{}", "1.0.0"),
				new ServiceDefinition("testService"),
				"1.0.0",
				null,
				"{}");
		final ServiceInstanceInterface intf = new ServiceInstanceInterface(
				instance,
				new ServiceInterfaceTemplate("generic_http", "http"),
				"{}",
				ServiceInterfacePolicy.NONE);

		when(serviceDefNameNormalizer.normalize("testService")).thenReturn("testService");
		doNothing().when(serviceDefNameValidator).validateServiceDefinitionName("testService");
		when(interfaceTemplateNameNormalizer.normalize("generic_http")).thenReturn("generic_http");
		doNothing().when(interfaceTemplateNameValidator).validateInterfaceTemplateName("generic_http");
		when(instanceDbService.getPageByFilters(pagination, filterModel)).thenReturn(new PageImpl<>(List.of(Map.entry(instance, List.of(intf)))));

		final ServiceModel result = driver.acquireService("testService", "generic_http", "TestProvider");

		assertNull(result);

		verify(serviceDefNameNormalizer).normalize("testService");
		verify(serviceDefNameValidator).validateServiceDefinitionName("testService");
		verify(interfaceTemplateNameNormalizer).normalize("generic_http");
		verify(interfaceTemplateNameValidator).validateInterfaceTemplateName("generic_http");
		verify(instanceDbService).getPageByFilters(pagination, filterModel);
	}
}