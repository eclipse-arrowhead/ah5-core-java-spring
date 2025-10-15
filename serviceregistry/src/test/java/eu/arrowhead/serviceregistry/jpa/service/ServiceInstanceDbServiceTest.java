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
package eu.arrowhead.serviceregistry.jpa.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;

import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.exception.LockedException;
import eu.arrowhead.common.service.util.ServiceInterfaceAddressPropertyProcessor;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import eu.arrowhead.dto.ServiceInstanceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceUpdateRequestDTO;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;
import eu.arrowhead.serviceregistry.ServiceRegistrySystemInfo;
import eu.arrowhead.serviceregistry.jpa.repository.ServiceDefinitionRepository;
import eu.arrowhead.serviceregistry.jpa.repository.ServiceInstanceInterfaceRepository;
import eu.arrowhead.serviceregistry.jpa.repository.ServiceInstanceRepository;
import eu.arrowhead.serviceregistry.jpa.repository.ServiceInterfaceTemplatePropertyRepository;
import eu.arrowhead.serviceregistry.jpa.repository.ServiceInterfaceTemplateRepository;
import eu.arrowhead.serviceregistry.jpa.repository.SystemRepository;
import eu.arrowhead.serviceregistry.jpa.entity.Device;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceAddress;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceDefinition;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInstance;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInstanceInterface;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInterfaceTemplate;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInterfaceTemplateProperty;
import eu.arrowhead.serviceregistry.jpa.entity.System;
import eu.arrowhead.serviceregistry.jpa.entity.SystemAddress;
import eu.arrowhead.serviceregistry.service.ServiceDiscoveryInterfacePolicy;
import eu.arrowhead.serviceregistry.service.model.ServiceLookupFilterModel;

@ExtendWith(MockitoExtension.class)
public class ServiceInstanceDbServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private ServiceInstanceDbService service;

	@Mock
	private ServiceRegistrySystemInfo sysInfo;

	@Mock
	private ServiceInstanceRepository serviceInstanceRepo;

	@Mock
	private ServiceInstanceInterfaceRepository serviceInstanceInterfaceRepo;

	@Mock
	private SystemRepository systemRepo;

	@Mock
	private ServiceDefinitionRepository serviceDefinitionRepo;

	@Mock
	private ServiceInterfaceTemplateRepository serviceInterfaceTemplateRepo;

	@Mock
	private ServiceInterfaceTemplatePropertyRepository serviceInterfaceTemplatePropsRepo;

	@Mock
	private ServiceInterfaceAddressPropertyProcessor interfaceAddressPropertyProcessor;

	private static final String DB_ERROR_MSG = "Database operation error";

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkRestricedIntfPolicyInvalidInterfaceThrowsInvalidParameterException() {

		when(sysInfo.getServiceDiscoveryInterfacePolicy()).thenReturn(ServiceDiscoveryInterfacePolicy.RESTRICTED);

		final ServiceInstanceInterfaceRequestDTO requestDTO = new ServiceInstanceInterfaceRequestDTO(
			"generic_http",
			"http",
			"RSA_SHA512_JSON_WEB_TOKEN_AUTH",
			Map.of("accessPort", 4041));

		final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO(
				"TemperatureManager",
				"temperatureManagement",
				"5.1.0",
				"2030-11-04T01:53:02Z",
				Map.of("indoor", true),
				List.of(requestDTO));

		final System system = new System("TemperatureManager", "{\r\n  \"indoor\" : true\r\n}", "5.1.0");
		final ServiceDefinition serviceDefinition = new ServiceDefinition("temperatureManagement");

		when(systemRepo.findAllByNameIn(any())).thenReturn(List.of(system));
		when(serviceDefinitionRepo.findAllByNameIn(any())).thenReturn(List.of(serviceDefinition));
		when(serviceInterfaceTemplateRepo.findAllByNameIn(any())).thenReturn(List.of());

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.createBulk(List.of(dto)));
		assertEquals("Interface template does not exist: generic_http", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkRestricedIntfPolicyValidInterface() {

		when(sysInfo.getServiceDiscoveryInterfacePolicy()).thenReturn(ServiceDiscoveryInterfacePolicy.RESTRICTED);

		final ServiceInstanceInterfaceRequestDTO requestDTO = new ServiceInstanceInterfaceRequestDTO(
			"generic_https",
			"https",
			"RSA_SHA512_JSON_WEB_TOKEN_AUTH",
			Map.of("accessPort", 4041));

		final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO(
				"TemperatureManager",
				"temperatureManagement",
				"5.1.0",
				"2030-11-04T01:53:02Z",
				Map.of("indoor", true),
				List.of(requestDTO));

		final System system = new System("TemperatureManager", "{\r\n  \"indoor\" : true\r\n}", "5.1.0");
		final ServiceDefinition serviceDefinition = new ServiceDefinition("temperatureManagement");
		final ServiceInterfaceTemplate template = new ServiceInterfaceTemplate("generic_https", "https");
		final ServiceInstance instance = new ServiceInstance("TemperatureManager|temperatureManagement|5.1.0", system, serviceDefinition, "5.1.0", ZonedDateTime.of(2030, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")), "{\r\n  \"indoor\" : true\r\n}");
		final ServiceInstanceInterface instanceInterface = new ServiceInstanceInterface(instance, template, "{\r\n  \"accessPort\" : 4041\r\n}", ServiceInterfacePolicy.RSA_SHA512_JSON_WEB_TOKEN_AUTH);

		when(systemRepo.findAllByNameIn(any())).thenReturn(List.of(system));
		when(serviceDefinitionRepo.findAllByNameIn(any())).thenReturn(List.of(serviceDefinition));
		when(serviceInterfaceTemplateRepo.findAllByNameIn(any())).thenReturn(List.of(template));
		when(serviceInstanceRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));
		when(serviceInstanceInterfaceRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		final List<Entry<ServiceInstance, List<ServiceInstanceInterface>>> expected = List.of(Map.entry(instance, List.of(instanceInterface)));
		final List<Entry<ServiceInstance, List<ServiceInstanceInterface>>> actual = assertDoesNotThrow(() -> service.createBulk(List.of(dto)));

		assertEquals(expected, actual);

		final InOrder inOrder = Mockito.inOrder(serviceInstanceRepo);
		inOrder.verify(serviceInstanceRepo).deleteAllByServiceInstanceIdIn(
				argThat(collection -> collection != null && ((Collection<?>) collection).contains("TemperatureManager|temperatureManagement|5.1.0") && ((Collection<?>) collection).size() == 1));
		inOrder.verify(serviceInstanceRepo).flush();
		inOrder.verify(serviceInstanceRepo).saveAllAndFlush(List.of(instance));

		verify(serviceInstanceInterfaceRepo).saveAllAndFlush(List.of(instanceInterface));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkOpenIntfPolicy() {

		when(sysInfo.getServiceDiscoveryInterfacePolicy()).thenReturn(ServiceDiscoveryInterfacePolicy.OPEN);

		final ServiceInstanceInterfaceRequestDTO requestDTO = new ServiceInstanceInterfaceRequestDTO(
			"generic_https",
			"https",
			"RSA_SHA512_JSON_WEB_TOKEN_AUTH",
			Map.of("accessPort", 4041));

		final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO(
				"TemperatureManager",
				"temperatureManagement",
				"5.1.0",
				"2030-11-04T01:53:02Z",
				Map.of("indoor", true),
				List.of(requestDTO));

		final System system = new System("TemperatureManager", "{\r\n  \"indoor\" : true\r\n}", "5.1.0");
		final ServiceDefinition serviceDefinition = new ServiceDefinition("temperatureManagement");
		final ServiceInterfaceTemplate template = new ServiceInterfaceTemplate("generic_https", "https");
		final ServiceInstance instance = new ServiceInstance("TemperatureManager|temperatureManagement|5.1.0", system, serviceDefinition, "5.1.0", ZonedDateTime.of(2030, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")), "{\r\n  \"indoor\" : true\r\n}");
		final ServiceInstanceInterface instanceInterface = new ServiceInstanceInterface(instance, template, "{\r\n  \"accessPort\" : 4041\r\n}", ServiceInterfacePolicy.RSA_SHA512_JSON_WEB_TOKEN_AUTH);

		when(systemRepo.findAllByNameIn(any())).thenReturn(List.of(system));
		when(serviceDefinitionRepo.findAllByNameIn(any())).thenReturn(List.of(serviceDefinition));
		when(serviceInterfaceTemplateRepo.findAllByNameIn(any())).thenReturn(List.of(template));
		when(serviceInstanceRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));
		when(serviceInstanceInterfaceRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		final List<Entry<ServiceInstance, List<ServiceInstanceInterface>>> expected = List.of(Map.entry(instance, List.of(instanceInterface)));
		final List<Entry<ServiceInstance, List<ServiceInstanceInterface>>> actual = service.createBulk(List.of(dto));

		assertEquals(expected, actual);

		final InOrder inOrder = Mockito.inOrder(serviceInstanceRepo);
		inOrder.verify(serviceInstanceRepo).deleteAllByServiceInstanceIdIn(
				argThat(collection -> collection != null && ((Collection<?>) collection).contains("TemperatureManager|temperatureManagement|5.1.0") && ((Collection<?>) collection).size() == 1));
		inOrder.verify(serviceInstanceRepo).flush();
		inOrder.verify(serviceInstanceRepo).saveAllAndFlush(List.of(instance));

		verify(serviceInstanceInterfaceRepo).saveAllAndFlush(List.of(instanceInterface));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkSystemDoesNotExistThrowsInvalidParameterException() {

		when(sysInfo.getServiceDiscoveryInterfacePolicy()).thenReturn(ServiceDiscoveryInterfacePolicy.OPEN);

		final ServiceInstanceInterfaceRequestDTO requestDTO = new ServiceInstanceInterfaceRequestDTO(
			"generic_https",
			"https",
			"RSA_SHA512_JSON_WEB_TOKEN_AUTH",
			Map.of("accessPort", 4041));

		final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO(
				"TemperatureManager",
				"temperatureManagement",
				"5.1.0",
				"2030-11-04T01:53:02Z",
				Map.of("indoor", true),
				List.of(requestDTO));

		final ServiceDefinition serviceDefinition = new ServiceDefinition("temperatureManagement");
		final ServiceInterfaceTemplate template = new ServiceInterfaceTemplate("generic_https", "https");

		when(systemRepo.findAllByNameIn(any())).thenReturn(List.of());
		when(serviceDefinitionRepo.findAllByNameIn(any())).thenReturn(List.of(serviceDefinition));
		when(serviceInterfaceTemplateRepo.findAllByNameIn(any())).thenReturn(List.of(template));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.createBulk(List.of(dto)));

		assertEquals("System does not exist: TemperatureManager", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkServiceDefinitionDoesNotExist() {

		when(sysInfo.getServiceDiscoveryInterfacePolicy()).thenReturn(ServiceDiscoveryInterfacePolicy.OPEN);

		final ServiceInstanceInterfaceRequestDTO requestDTO = new ServiceInstanceInterfaceRequestDTO(
			"generic_https",
			"https",
			"RSA_SHA512_JSON_WEB_TOKEN_AUTH",
			Map.of("accessPort", 4041));

		final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO(
				"TemperatureManager",
				"temperatureManagement",
				"5.1.0",
				"2030-11-04T01:53:02Z",
				Map.of("indoor", true),
				List.of(requestDTO));

		final System system = new System("TemperatureManager", "{\r\n  \"indoor\" : true\r\n}", "5.1.0");
		final ServiceDefinition createdServiceDefinition = new ServiceDefinition("temperatureManagement");
		final ServiceInterfaceTemplate template = new ServiceInterfaceTemplate("generic_https", "https");
		final ServiceInstance instance = new ServiceInstance(
				"TemperatureManager|temperatureManagement|5.1.0", system, createdServiceDefinition, "5.1.0", ZonedDateTime.of(2030, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")), "{\r\n  \"indoor\" : true\r\n}");
		final ServiceInstanceInterface instanceInterface = new ServiceInstanceInterface(instance, template, "{\r\n  \"accessPort\" : 4041\r\n}", ServiceInterfacePolicy.RSA_SHA512_JSON_WEB_TOKEN_AUTH);

		when(systemRepo.findAllByNameIn(any())).thenReturn(List.of(system));
		when(serviceDefinitionRepo.findAllByNameIn(any())).thenReturn(List.of());
		when(serviceInterfaceTemplateRepo.findAllByNameIn(any())).thenReturn(List.of(template));
		when(serviceInstanceRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));
		when(serviceInstanceInterfaceRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		final List<Entry<ServiceInstance, List<ServiceInstanceInterface>>> expected = List.of(Map.entry(instance, List.of(instanceInterface)));
		final List<Entry<ServiceInstance, List<ServiceInstanceInterface>>> actual = service.createBulk(List.of(dto));

		assertEquals(expected, actual);

		verify(serviceDefinitionRepo).saveAndFlush(new ServiceDefinition("temperatureManagement"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkNoProtocolDefinedThrowsInvalidParameterException() {

		when(sysInfo.getServiceDiscoveryInterfacePolicy()).thenReturn(ServiceDiscoveryInterfacePolicy.OPEN);

		final ServiceInstanceInterfaceRequestDTO requestDTO = new ServiceInstanceInterfaceRequestDTO(
			"generic_https",
			"",
			"RSA_SHA512_JSON_WEB_TOKEN_AUTH",
			Map.of("accessPort", 4041));

		final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO(
				"TemperatureManager",
				"temperatureManagement",
				"5.1.0",
				"2030-11-04T01:53:02Z",
				Map.of("indoor", true),
				List.of(requestDTO));

		final System system = new System("TemperatureManager", "{\r\n  \"indoor\" : true\r\n}", "5.1.0");
		final ServiceDefinition serviceDefinition = new ServiceDefinition("temperatureManagement");

		when(systemRepo.findAllByNameIn(any())).thenReturn(List.of(system));
		when(serviceDefinitionRepo.findAllByNameIn(any())).thenReturn(List.of(serviceDefinition));
		when(serviceInterfaceTemplateRepo.findAllByNameIn(any())).thenReturn(List.of());
		when(serviceInstanceRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.createBulk(List.of(dto)));

		assertEquals("No protocol has been defined", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkNewTemplateNotExtendableIntfPolicy() {

		when(sysInfo.getServiceDiscoveryInterfacePolicy()).thenReturn(ServiceDiscoveryInterfacePolicy.OPEN);

		final ServiceInstanceInterfaceRequestDTO requestDTO = new ServiceInstanceInterfaceRequestDTO(
			"generic_https",
			"https",
			"RSA_SHA512_JSON_WEB_TOKEN_AUTH",
			Map.of("accessPort", 4041));

		final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO(
				"TemperatureManager",
				"temperatureManagement",
				"5.1.0",
				"2030-11-04T01:53:02Z",
				Map.of("indoor", true),
				List.of(requestDTO));

		final System system = new System("TemperatureManager", "{\r\n  \"indoor\" : true\r\n}", "5.1.0");
		final ServiceDefinition serviceDefinition = new ServiceDefinition("temperatureManagement");
		final ServiceInterfaceTemplate newTemplate = new ServiceInterfaceTemplate("generic_https", "https");
		final ServiceInstance instance = new ServiceInstance(
				"TemperatureManager|temperatureManagement|5.1.0", system, serviceDefinition, "5.1.0", ZonedDateTime.of(2030, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")), "{\r\n  \"indoor\" : true\r\n}");
		final ServiceInstanceInterface instanceInterface = new ServiceInstanceInterface(instance, newTemplate, "{\r\n  \"accessPort\" : 4041\r\n}", ServiceInterfacePolicy.RSA_SHA512_JSON_WEB_TOKEN_AUTH);

		when(systemRepo.findAllByNameIn(any())).thenReturn(List.of(system));
		when(serviceDefinitionRepo.findAllByNameIn(any())).thenReturn(List.of(serviceDefinition));
		when(serviceInterfaceTemplateRepo.findAllByNameIn(any())).thenReturn(List.of());
		when(serviceInstanceRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));
		when(serviceInterfaceTemplateRepo.saveAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));
		when(serviceInstanceInterfaceRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		final List<Entry<ServiceInstance, List<ServiceInstanceInterface>>> expected = List.of(Map.entry(instance, List.of(instanceInterface)));
		final List<Entry<ServiceInstance, List<ServiceInstanceInterface>>> actual = service.createBulk(List.of(dto));

		assertEquals(expected, actual);
		verify(serviceInterfaceTemplateRepo).saveAndFlush(newTemplate);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkNewTemplateExtendableIntfPolicy() {

		when(sysInfo.getServiceDiscoveryInterfacePolicy()).thenReturn(ServiceDiscoveryInterfacePolicy.EXTENDABLE);

		final ServiceInstanceInterfaceRequestDTO requestDTO = new ServiceInstanceInterfaceRequestDTO(
			"generic_https",
			"https",
			"RSA_SHA512_JSON_WEB_TOKEN_AUTH",
			Map.of("accessPort", 4041));

		final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO(
				"TemperatureManager",
				"temperatureManagement",
				"5.1.0",
				"2030-11-04T01:53:02Z",
				Map.of("indoor", true),
				List.of(requestDTO));

		final System system = new System("TemperatureManager", "{\r\n  \"indoor\" : true\r\n}", "5.1.0");
		final ServiceDefinition serviceDefinition = new ServiceDefinition("temperatureManagement");
		final ServiceInterfaceTemplate newTemplate = new ServiceInterfaceTemplate("generic_https", "https");
		final ServiceInterfaceTemplateProperty property = new ServiceInterfaceTemplateProperty(newTemplate, "accessPort", true, null);
		final ServiceInstance instance = new ServiceInstance(
				"TemperatureManager|temperatureManagement|5.1.0", system, serviceDefinition, "5.1.0", ZonedDateTime.of(2030, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")), "{\r\n  \"indoor\" : true\r\n}");
		final ServiceInstanceInterface instanceInterface = new ServiceInstanceInterface(instance, newTemplate, "{\r\n  \"accessPort\" : 4041\r\n}", ServiceInterfacePolicy.RSA_SHA512_JSON_WEB_TOKEN_AUTH);

		when(systemRepo.findAllByNameIn(any())).thenReturn(List.of(system));
		when(serviceDefinitionRepo.findAllByNameIn(any())).thenReturn(List.of(serviceDefinition));
		when(serviceInterfaceTemplateRepo.findAllByNameIn(any())).thenReturn(List.of());
		when(serviceInstanceRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));
		when(serviceInterfaceTemplateRepo.saveAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));
		when(serviceInstanceInterfaceRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));
		when(serviceInterfaceTemplatePropsRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));
		when(serviceInterfaceTemplatePropsRepo.findByServiceInterfaceTemplate(any())).thenReturn(List.of(property));

		final List<Entry<ServiceInstance, List<ServiceInstanceInterface>>> expected = List.of(Map.entry(instance, List.of(instanceInterface)));
		final List<Entry<ServiceInstance, List<ServiceInstanceInterface>>> actual = service.createBulk(List.of(dto));

		assertEquals(expected, actual);
		verify(serviceInterfaceTemplateRepo).saveAndFlush(newTemplate);
		verify(serviceInterfaceTemplatePropsRepo).saveAllAndFlush(List.of(property));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkNewTemplateExtendableIntfPolicyMissingPropertyThrowsInvalidParameterException() {

		when(sysInfo.getServiceDiscoveryInterfacePolicy()).thenReturn(ServiceDiscoveryInterfacePolicy.EXTENDABLE);

		final ServiceInstanceInterfaceRequestDTO requestDTO1 = new ServiceInstanceInterfaceRequestDTO(
			"generic_https",
			"https",
			"RSA_SHA512_JSON_WEB_TOKEN_AUTH",
			Map.of("accessPort", 4041));

		final ServiceInstanceRequestDTO dto1 = new ServiceInstanceRequestDTO(
				"TemperatureManager1",
				"temperatureManagement",
				"5.1.0",
				"2030-11-04T01:53:02Z",
				Map.of("indoor", true),
				List.of(requestDTO1));

		final ServiceInstanceInterfaceRequestDTO requestDTO2 = new ServiceInstanceInterfaceRequestDTO(
				"generic_https",
				"https",
				"RSA_SHA512_JSON_WEB_TOKEN_AUTH",
				Map.of());

			final ServiceInstanceRequestDTO dto2 = new ServiceInstanceRequestDTO(
					"TemperatureManager2",
					"temperatureManagement",
					"5.1.0",
					"2030-11-04T01:53:02Z",
					Map.of("indoor", true),
					List.of(requestDTO2));

		final System system1 = new System("TemperatureManager1", "{\r\n  \"indoor\" : true\r\n}", "5.1.0");
		final System system2 = new System("TemperatureManager2", "{\r\n  \"indoor\" : true\r\n}", "5.1.0");
		final ServiceDefinition serviceDefinition = new ServiceDefinition("temperatureManagement");
		final ServiceInterfaceTemplate template = new ServiceInterfaceTemplate("generic_https", "https");
		final ServiceInterfaceTemplateProperty property = new ServiceInterfaceTemplateProperty(template, "accessPort", true, null);

		when(systemRepo.findAllByNameIn(any())).thenReturn(List.of(system1, system2));
		when(serviceDefinitionRepo.findAllByNameIn(any())).thenReturn(List.of(serviceDefinition));
		when(serviceInterfaceTemplateRepo.findAllByNameIn(any())).thenReturn(List.of());
		when(serviceInstanceRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));
		when(serviceInterfaceTemplateRepo.saveAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));
		when(serviceInterfaceTemplatePropsRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));
		when(serviceInterfaceTemplatePropsRepo.findByServiceInterfaceTemplate(any())).thenReturn(List.of(property));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.createBulk(List.of(dto1, dto2)));

		assertEquals("Mandatory interface property is missing: accessPort", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkDifferentIntfProtocolThanInTheTemplate() {

		when(sysInfo.getServiceDiscoveryInterfacePolicy()).thenReturn(ServiceDiscoveryInterfacePolicy.EXTENDABLE);

		final ServiceInstanceInterfaceRequestDTO requestDTO = new ServiceInstanceInterfaceRequestDTO(
			"generic_https",
			"http",
			"RSA_SHA512_JSON_WEB_TOKEN_AUTH",
			Map.of("accessPort", 4041));

		final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO(
				"TemperatureManager",
				"temperatureManagement",
				"5.1.0",
				"2030-11-04T01:53:02Z",
				Map.of("indoor", true),
				List.of(requestDTO));

		final System system = new System("TemperatureManager", "{\r\n  \"indoor\" : true\r\n}", "5.1.0");
		final ServiceDefinition serviceDefinition = new ServiceDefinition("temperatureManagement");
		final ServiceInterfaceTemplate template = new ServiceInterfaceTemplate("generic_https", "https");

		when(systemRepo.findAllByNameIn(any())).thenReturn(List.of(system));
		when(serviceDefinitionRepo.findAllByNameIn(any())).thenReturn(List.of(serviceDefinition));
		when(serviceInterfaceTemplateRepo.findAllByNameIn(any())).thenReturn(List.of(template));
		when(serviceInstanceRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.createBulk(List.of(dto)));

		assertEquals("Interface has different protocol than generic_https template", ex.getMessage());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkNotRestricedIntfPolicyThrowsInternalServerError() {

		final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO(
				"TemperatureManager",
				"temperatureManagement",
				"5.1.0",
				"2030-11-04T01:53:02Z",
				Map.of("indoor", true),
				List.of());

		doThrow(new LockedException("error")).when(serviceInstanceRepo).deleteAllByServiceInstanceIdIn(any());
		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.createBulk(List.of(dto)));
		assertEquals(DB_ERROR_MSG, ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateBulkNotExistingInstanceThrowsInvalidParameterException() {

		final ServiceInstanceUpdateRequestDTO dto = new ServiceInstanceUpdateRequestDTO(
				"TemperatureManager|temperatureManagement|5.1.0",
				"2030-11-04T01:53:02Z",
				Map.of("indoor", true),
				List.of());

		when(serviceInstanceRepo.findByServiceInstanceId(any())).thenReturn(Optional.empty());
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.updateBulk(List.of(dto)));
		assertEquals("Instance id does not exist: TemperatureManager|temperatureManagement|5.1.0", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateBulkChangeMetadata() {

		final ServiceInstanceInterfaceRequestDTO requestDTO = new ServiceInstanceInterfaceRequestDTO(
				"generic_https",
				"",
				"RSA_SHA512_JSON_WEB_TOKEN_AUTH",
				Map.of("accessPort", 4041));

			final ServiceInstanceUpdateRequestDTO dto = new ServiceInstanceUpdateRequestDTO(
				"TemperatureManager|temperatureManagement|5.1.0",
				"2030-11-04T01:53:02Z",
				Map.of("indoor", true),
				List.of(requestDTO));

			final System system = new System("TemperatureManager", "{\r\n  \"indoor\" : true\r\n}", "5.1.0");
			final ServiceDefinition serviceDefinition = new ServiceDefinition("temperatureManagement");

			final ServiceInstance existing = new ServiceInstance(
				"TemperatureManager|temperatureManagement|5.1.0",
				system,
				serviceDefinition,
				"5.1.0",
				ZonedDateTime.of(2030, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")),
				"{\r\n  \"indoor\" : false\r\n}");

			final ServiceInstance changed = new ServiceInstance(
					"TemperatureManager|temperatureManagement|5.1.0",
					system,
					serviceDefinition,
					"5.1.0",
					ZonedDateTime.of(2030, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")),
					"{\r\n  \"indoor\" : true\r\n}"); // the metadata should be different after update

			final ServiceInterfaceTemplate template = new ServiceInterfaceTemplate("generic_https", "https");
			final ServiceInterfaceTemplateProperty property = new ServiceInterfaceTemplateProperty(template, "accessPort", false, null);
			final ServiceInstanceInterface instanceInterface = new ServiceInstanceInterface(existing, template, "{\r\n  \"accessPort\" : 4041\r\n}", ServiceInterfacePolicy.RSA_SHA512_JSON_WEB_TOKEN_AUTH);

			when(serviceInstanceRepo.findByServiceInstanceId(eq("TemperatureManager|temperatureManagement|5.1.0"))).thenReturn(Optional.of(existing));
			when(serviceInterfaceTemplateRepo.findByName(any())).thenReturn(Optional.of(template));
			when(serviceInterfaceTemplatePropsRepo.findAllByServiceInterfaceTemplate(any())).thenReturn(List.of(property));
			when(serviceInstanceInterfaceRepo.saveAll(any())).thenAnswer(Invocation -> Invocation.getArgument(0));
			when(serviceInstanceRepo.saveAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

			final List<Entry<ServiceInstance, List<ServiceInstanceInterface>>> expected = List.of(Map.entry(changed, List.of(instanceInterface)));
			final List<Entry<ServiceInstance, List<ServiceInstanceInterface>>> actual = service.updateBulk(List.of(dto));

			assertEquals(expected, actual);
			verify(serviceInstanceRepo).saveAndFlush(changed);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateBulkChangeExpiresAt() {

		final ServiceInstanceInterfaceRequestDTO requestDTO = new ServiceInstanceInterfaceRequestDTO(
				"generic_https",
				"",
				"RSA_SHA512_JSON_WEB_TOKEN_AUTH",
				Map.of("accessPort", 4041));

			final ServiceInstanceUpdateRequestDTO dto = new ServiceInstanceUpdateRequestDTO(
				"TemperatureManager|temperatureManagement|5.1.0",
				"2040-11-04T01:53:02Z",
				Map.of("indoor", true),
				List.of(requestDTO));

			final System system = new System("TemperatureManager", "{\r\n  \"indoor\" : true\r\n}", "5.1.0");
			final ServiceDefinition serviceDefinition = new ServiceDefinition("temperatureManagement");

			final ServiceInstance existing = new ServiceInstance(
				"TemperatureManager|temperatureManagement|5.1.0",
				system,
				serviceDefinition,
				"5.1.0",
				ZonedDateTime.of(2030, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")),
				"{\r\n  \"indoor\" : true\r\n}");

			final ServiceInstance changed = new ServiceInstance(
					"TemperatureManager|temperatureManagement|5.1.0",
					system,
					serviceDefinition,
					"5.1.0",
					ZonedDateTime.of(2040, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")), // expiration should be changed after update
					"{\r\n  \"indoor\" : true\r\n}");

			final ServiceInterfaceTemplate template = new ServiceInterfaceTemplate("generic_https", "https");
			final ServiceInterfaceTemplateProperty property = new ServiceInterfaceTemplateProperty(template, "accessPort", false, null);
			final ServiceInstanceInterface instanceInterface = new ServiceInstanceInterface(existing, template, "{\r\n  \"accessPort\" : 4041\r\n}", ServiceInterfacePolicy.RSA_SHA512_JSON_WEB_TOKEN_AUTH);

			when(serviceInstanceRepo.findByServiceInstanceId(eq("TemperatureManager|temperatureManagement|5.1.0"))).thenReturn(Optional.of(existing));
			when(serviceInterfaceTemplateRepo.findByName(any())).thenReturn(Optional.of(template));
			when(serviceInterfaceTemplatePropsRepo.findAllByServiceInterfaceTemplate(any())).thenReturn(List.of(property));
			when(serviceInstanceInterfaceRepo.saveAll(any())).thenAnswer(Invocation -> Invocation.getArgument(0));
			when(serviceInstanceRepo.saveAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

			final List<Entry<ServiceInstance, List<ServiceInstanceInterface>>> expected = List.of(Map.entry(changed, List.of(instanceInterface)));
			final List<Entry<ServiceInstance, List<ServiceInstanceInterface>>> actual = service.updateBulk(List.of(dto));

			assertEquals(expected, actual);
			verify(serviceInstanceRepo).saveAndFlush(changed);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateBulkChangeInterfaceNotExistingTemplateButRestrictedPolicyThrowsInvalidParameterException() {

		when(sysInfo.getServiceDiscoveryInterfacePolicy()).thenReturn(ServiceDiscoveryInterfacePolicy.RESTRICTED);

		final ServiceInstanceInterfaceRequestDTO requestDTO = new ServiceInstanceInterfaceRequestDTO(
			"generic_http",
			"http",
			"RSA_SHA512_JSON_WEB_TOKEN_AUTH",
			Map.of("accessPort", 4041));

		final ServiceInstanceUpdateRequestDTO dto = new ServiceInstanceUpdateRequestDTO(
				"TemperatureManager|temperatureManagement|5.1.0",
				"2030-11-04T01:53:02Z",
				Map.of("indoor", true),
				List.of(requestDTO));

		final System system = new System("TemperatureManager", "{\r\n  \"indoor\" : true\r\n}", "5.1.0");
		final ServiceDefinition serviceDefinition = new ServiceDefinition("temperatureManagement");
		final ServiceInstance existing = new ServiceInstance(
				"TemperatureManager|temperatureManagement|5.1.0",
				system,
				serviceDefinition,
				"5.1.0",
				ZonedDateTime.of(2030, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")),
				"{\r\n  \"indoor\" : true\r\n}");

		when(serviceInstanceRepo.findByServiceInstanceId(any())).thenReturn(Optional.of(existing));
		when(serviceInterfaceTemplateRepo.findByName(any())).thenReturn(Optional.empty());

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.updateBulk(List.of(dto)));
		assertEquals("Interface template does not exist: generic_http", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateBulkChangeInterfaceNotExistingTemplateExtendablePolicy() {

		when(sysInfo.getServiceDiscoveryInterfacePolicy()).thenReturn(ServiceDiscoveryInterfacePolicy.EXTENDABLE);

		final ServiceInstanceInterfaceRequestDTO requestDTO = new ServiceInstanceInterfaceRequestDTO(
			"generic_https",
			"https",
			"RSA_SHA512_JSON_WEB_TOKEN_AUTH",
			Map.of("accessPort", 4041));

		final ServiceInstanceUpdateRequestDTO dto = new ServiceInstanceUpdateRequestDTO(
				"TemperatureManager|temperatureManagement|5.1.0",
				"2030-11-04T01:53:02Z",
				Map.of("indoor", true),
				List.of(requestDTO));

		final System system = new System("TemperatureManager", "{\r\n  \"indoor\" : true\r\n}", "5.1.0");
		final ServiceDefinition serviceDefinition = new ServiceDefinition("temperatureManagement");
		final ServiceInstance existing = new ServiceInstance(
				"TemperatureManager|temperatureManagement|5.1.0",
				system,
				serviceDefinition,
				"5.1.0",
				ZonedDateTime.of(2030, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")),
				"{\r\n  \"indoor\" : true\r\n}");

		final ServiceInterfaceTemplate newTemplate = new ServiceInterfaceTemplate("generic_https", "https");
		final ServiceInterfaceTemplateProperty property = new ServiceInterfaceTemplateProperty(newTemplate, "accessPort", true, null);
		final ServiceInstanceInterface instanceInterface = new ServiceInstanceInterface(existing, newTemplate, "{\r\n  \"accessPort\" : 4041\r\n}", ServiceInterfacePolicy.RSA_SHA512_JSON_WEB_TOKEN_AUTH);

		when(serviceInstanceInterfaceRepo.saveAll(any())).thenAnswer(Invocation -> Invocation.getArgument(0));
		when(serviceInterfaceTemplatePropsRepo.saveAll(any())).thenAnswer(Invocation -> Invocation.getArgument(0));
		when(serviceInstanceRepo.findByServiceInstanceId(any())).thenReturn(Optional.of(existing));
		when(serviceInterfaceTemplateRepo.findByName(any())).thenReturn(Optional.empty());
		when(serviceInstanceRepo.saveAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		final List<Entry<ServiceInstance, List<ServiceInstanceInterface>>> expected = List.of(Map.entry(existing, List.of(instanceInterface)));
		final List<Entry<ServiceInstance, List<ServiceInstanceInterface>>> actual = service.updateBulk(List.of(dto));

		assertEquals(expected, actual);
		final InOrder inOrderTemplate = Mockito.inOrder(serviceInterfaceTemplateRepo);
		final InOrder inOrderTemplateProps = Mockito.inOrder(serviceInterfaceTemplatePropsRepo);
		final InOrder inOrderInterface = Mockito.inOrder(serviceInstanceInterfaceRepo);

		inOrderTemplate.verify(serviceInterfaceTemplateRepo).save(newTemplate);
		inOrderTemplate.verify(serviceInterfaceTemplateRepo).flush();

		inOrderTemplateProps.verify(serviceInterfaceTemplatePropsRepo).saveAll(List.of(property));
		inOrderTemplateProps.verify(serviceInterfaceTemplatePropsRepo).flush();

		inOrderInterface.verify(serviceInstanceInterfaceRepo).deleteAllByServiceInstance(existing);
		inOrderInterface.verify(serviceInstanceInterfaceRepo).saveAll(List.of(instanceInterface));
		inOrderInterface.verify(serviceInstanceInterfaceRepo).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateBulkChangeInterfaceNotExistingTemplateOpenPolicy() {

		when(sysInfo.getServiceDiscoveryInterfacePolicy()).thenReturn(ServiceDiscoveryInterfacePolicy.OPEN);

		final ServiceInstanceInterfaceRequestDTO requestDTO = new ServiceInstanceInterfaceRequestDTO(
			"generic_https",
			"https",
			"RSA_SHA512_JSON_WEB_TOKEN_AUTH",
			Map.of("accessPort", 4041));

		final ServiceInstanceUpdateRequestDTO dto = new ServiceInstanceUpdateRequestDTO(
				"TemperatureManager|temperatureManagement|5.1.0",
				"2030-11-04T01:53:02Z",
				Map.of("indoor", true),
				List.of(requestDTO));

		final System system = new System("TemperatureManager", "{\r\n  \"indoor\" : true\r\n}", "5.1.0");
		final ServiceDefinition serviceDefinition = new ServiceDefinition("temperatureManagement");
		final ServiceInstance existing = new ServiceInstance(
				"TemperatureManager|temperatureManagement|5.1.0",
				system,
				serviceDefinition,
				"5.1.0",
				ZonedDateTime.of(2030, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")),
				"{\r\n  \"indoor\" : true\r\n}");

		final ServiceInterfaceTemplate newTemplate = new ServiceInterfaceTemplate("generic_https", "https");
		final ServiceInstanceInterface instanceInterface = new ServiceInstanceInterface(existing, newTemplate, "{\r\n  \"accessPort\" : 4041\r\n}", ServiceInterfacePolicy.RSA_SHA512_JSON_WEB_TOKEN_AUTH);

		when(serviceInstanceInterfaceRepo.saveAll(any())).thenAnswer(Invocation -> Invocation.getArgument(0));
		when(serviceInstanceRepo.findByServiceInstanceId(any())).thenReturn(Optional.of(existing));
		when(serviceInterfaceTemplateRepo.findByName(any())).thenReturn(Optional.empty());
		when(serviceInstanceRepo.saveAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		final List<Entry<ServiceInstance, List<ServiceInstanceInterface>>> expected = List.of(Map.entry(existing, List.of(instanceInterface)));
		final List<Entry<ServiceInstance, List<ServiceInstanceInterface>>> actual = service.updateBulk(List.of(dto));

		assertEquals(expected, actual);
		final InOrder inOrderTemplate = Mockito.inOrder(serviceInterfaceTemplateRepo);
		final InOrder inOrderInterface = Mockito.inOrder(serviceInstanceInterfaceRepo);

		inOrderTemplate.verify(serviceInterfaceTemplateRepo).save(newTemplate);
		inOrderTemplate.verify(serviceInterfaceTemplateRepo).flush();

		verify(serviceInterfaceTemplatePropsRepo, never()).saveAll(any());

		inOrderInterface.verify(serviceInstanceInterfaceRepo).deleteAllByServiceInstance(existing);
		inOrderInterface.verify(serviceInstanceInterfaceRepo).saveAll(List.of(instanceInterface));
		inOrderInterface.verify(serviceInstanceInterfaceRepo).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateBulkChangeInterfaceExistingTemplateButDifferentProtocolThrowsInvalidParameterException() {

		final ServiceInstanceInterfaceRequestDTO requestDTO = new ServiceInstanceInterfaceRequestDTO(
			"generic_https",
			"http",
			"RSA_SHA512_JSON_WEB_TOKEN_AUTH",
			Map.of("accessPort", 4041));

		final ServiceInstanceUpdateRequestDTO dto = new ServiceInstanceUpdateRequestDTO(
				"TemperatureManager|temperatureManagement|5.1.0",
				"2030-11-04T01:53:02Z",
				Map.of("indoor", true),
				List.of(requestDTO));

		final System system = new System("TemperatureManager", "{\r\n  \"indoor\" : true\r\n}", "5.1.0");
		final ServiceDefinition serviceDefinition = new ServiceDefinition("temperatureManagement");
		final ServiceInstance existing = new ServiceInstance(
				"TemperatureManager|temperatureManagement|5.1.0",
				system,
				serviceDefinition,
				"5.1.0",
				ZonedDateTime.of(2030, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")),
				"{\r\n  \"indoor\" : true\r\n}");
		final ServiceInterfaceTemplate existingTemplate = new ServiceInterfaceTemplate("generic_https", "https");

		when(serviceInstanceRepo.findByServiceInstanceId(any())).thenReturn(Optional.of(existing));
		when(serviceInterfaceTemplateRepo.findByName(any())).thenReturn(Optional.of(existingTemplate));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.updateBulk(List.of(dto)));

		assertEquals("Interface has different protocol than generic_https template", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateBulkChangeInterfaceExistingTemplateButMandatoryPropertyMissingThrowsInvalidParameterException() {

		final ServiceInstanceInterfaceRequestDTO requestDTO1 = new ServiceInstanceInterfaceRequestDTO(
			"generic_https",
			"https",
			"RSA_SHA512_JSON_WEB_TOKEN_AUTH",
			Map.of("accessPort", 4041));

		final ServiceInstanceInterfaceRequestDTO requestDTO2 = new ServiceInstanceInterfaceRequestDTO(
				"generic_https",
				"https",
				"RSA_SHA512_JSON_WEB_TOKEN_AUTH",
				Map.of());

		final ServiceInstanceUpdateRequestDTO dto = new ServiceInstanceUpdateRequestDTO(
			"TemperatureManager1|temperatureManagement|5.1.0",
			"2030-11-04T01:53:02Z",
			Map.of("indoor", true),
			List.of(requestDTO1, requestDTO2));

		final ServiceInterfaceTemplate template = new ServiceInterfaceTemplate("generic_https", "https");
		final ServiceInterfaceTemplateProperty property = new ServiceInterfaceTemplateProperty(template, "accessPort", true, null);

		final System system1 = new System("TemperatureManager1", "{\r\n  \"indoor\" : true\r\n}", "5.1.0");
		final ServiceDefinition serviceDefinition1 = new ServiceDefinition("temperatureManagement");
		final ServiceInstance existing1 = new ServiceInstance(
				"TemperatureManager1|temperatureManagement|5.1.0",
				system1,
				serviceDefinition1,
				"5.1.0",
				ZonedDateTime.of(2030, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")),
				"{\r\n  \"indoor\" : true\r\n}");

		when(serviceInstanceRepo.findByServiceInstanceId(eq("TemperatureManager1|temperatureManagement|5.1.0"))).thenReturn(Optional.of(existing1));
		when(serviceInterfaceTemplateRepo.findByName(any())).thenReturn(Optional.of(template));
		when(serviceInterfaceTemplatePropsRepo.findAllByServiceInterfaceTemplate(any())).thenReturn(List.of(property));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.updateBulk(List.of(dto)));
		assertEquals("Mandatory interface property is missing: accessPort", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateBulkChangeInterfaceExistingTemplateOk() {

		final ServiceInstanceInterfaceRequestDTO requestDTO = new ServiceInstanceInterfaceRequestDTO(
			"generic_https",
			"",
			"RSA_SHA512_JSON_WEB_TOKEN_AUTH",
			Map.of("accessPort", 4041));

		final ServiceInstanceUpdateRequestDTO dto = new ServiceInstanceUpdateRequestDTO(
			"TemperatureManager|temperatureManagement|5.1.0",
			"2030-11-04T01:53:02Z",
			Map.of("indoor", true),
			List.of(requestDTO));

		final System system = new System("TemperatureManager", "{\r\n  \"indoor\" : true\r\n}", "5.1.0");
		final ServiceDefinition serviceDefinition = new ServiceDefinition("temperatureManagement");
		final ServiceInstance existing = new ServiceInstance(
			"TemperatureManager|temperatureManagement|5.1.0",
			system,
			serviceDefinition,
			"5.1.0",
			ZonedDateTime.of(2030, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")),
			"{\r\n  \"indoor\" : true\r\n}");

		final ServiceInterfaceTemplate template = new ServiceInterfaceTemplate("generic_https", "https");
		final ServiceInterfaceTemplateProperty property = new ServiceInterfaceTemplateProperty(template, "accessPort", false, null);
		final ServiceInstanceInterface instanceInterface = new ServiceInstanceInterface(existing, template, "{\r\n  \"accessPort\" : 4041\r\n}", ServiceInterfacePolicy.RSA_SHA512_JSON_WEB_TOKEN_AUTH);

		when(serviceInstanceRepo.findByServiceInstanceId(eq("TemperatureManager|temperatureManagement|5.1.0"))).thenReturn(Optional.of(existing));
		when(serviceInterfaceTemplateRepo.findByName(any())).thenReturn(Optional.of(template));
		when(serviceInterfaceTemplatePropsRepo.findAllByServiceInterfaceTemplate(any())).thenReturn(List.of(property));
		when(serviceInstanceInterfaceRepo.saveAll(any())).thenAnswer(Invocation -> Invocation.getArgument(0));
		when(serviceInstanceRepo.saveAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		final List<Entry<ServiceInstance, List<ServiceInstanceInterface>>> expected = List.of(Map.entry(existing, List.of(instanceInterface)));
		final List<Entry<ServiceInstance, List<ServiceInstanceInterface>>> actual = service.updateBulk(List.of(dto));

		assertEquals(expected, actual);
		final InOrder inOrderInterface = Mockito.inOrder(serviceInstanceInterfaceRepo);

		inOrderInterface.verify(serviceInstanceInterfaceRepo).deleteAllByServiceInstance(existing);
		inOrderInterface.verify(serviceInstanceInterfaceRepo).saveAll(List.of(instanceInterface));
		inOrderInterface.verify(serviceInstanceInterfaceRepo).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateBulkThrowsInternalServerError() {

		final ServiceInstanceUpdateRequestDTO dto = new ServiceInstanceUpdateRequestDTO(
				"TemperatureManager|temperatureManagement|5.1.0",
				"2030-11-04T01:53:02Z",
				Map.of("indoor", true),
				List.of());

		when(serviceInstanceRepo.findByServiceInstanceId(any())).thenThrow(new InternalServerError("error"));
		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.updateBulk(List.of(dto)));
		assertEquals(DB_ERROR_MSG, ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersWithoutFilters() {

		final System system = new System("TemperatureManager", "{\r\n  \"indoor\" : true\r\n}", "5.1.0");
		final ServiceDefinition serviceDefinition = new ServiceDefinition("temperatureManagement");
		final ServiceInstance existing = new ServiceInstance(
				"TemperatureManager|temperatureManagement|5.1.0",
				system,
				serviceDefinition,
				"5.1.0",
				ZonedDateTime.of(2030, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")),
				"{\r\n  \"indoor\" : true\r\n}");

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");

		final ServiceInterfaceTemplate template = new ServiceInterfaceTemplate("generic_https", "https");
		final ServiceInstanceInterface instanceInterface = new ServiceInstanceInterface(existing, template, "{\r\n  \"accessPort\" : 4041\r\n}", ServiceInterfacePolicy.RSA_SHA512_JSON_WEB_TOKEN_AUTH);

		when(serviceInstanceRepo.findAll(eq(pageRequest))).thenReturn(new PageImpl<>(List.of(existing)));
		when(serviceInstanceInterfaceRepo.findAllByServiceInstance(eq(existing))).thenReturn(List.of(instanceInterface));

		final Page<Entry<ServiceInstance, List<ServiceInstanceInterface>>> expected =
				new PageImpl<Entry<ServiceInstance, List<ServiceInstanceInterface>>>(List.of(Map.entry(existing, List.of(instanceInterface))), pageRequest, 1);

		final Page<Entry<ServiceInstance, List<ServiceInstanceInterface>>> actual = service.getPageByFilters(
				pageRequest,
				new ServiceLookupFilterModel(new ServiceInstanceLookupRequestDTO(null, null, null, null, null, null, null, null, null, null)));
		assertEquals(expected, actual);
		verify(serviceInstanceRepo).findAll(eq(pageRequest));
		verify(serviceInstanceInterfaceRepo).findAllByServiceInstance(eq(existing));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersByInstanceIds() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");

		when(serviceInstanceRepo.findAllByServiceInstanceIdIn(any())).thenReturn(List.of());
		when(serviceInstanceRepo.findAllByIdIn(any(), any())).thenReturn(new PageImpl<>(List.of()));

		service.getPageByFilters(
				pageRequest,
				new ServiceLookupFilterModel(new ServiceInstanceLookupRequestDTO(List.of("TemperatureManager|temperatureManagement|5.1.0"), null, null, null, null, null, null, null, null, null)));
		verify(serviceInstanceRepo).findAllByServiceInstanceIdIn(
			argThat(list -> list.contains("TemperatureManager|temperatureManagement|5.1.0"))
		);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersByProviderNames() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");

		when(serviceInstanceRepo.findAllBySystem_NameIn(any())).thenReturn(List.of());
		when(serviceInstanceRepo.findAllByIdIn(any(), any())).thenReturn(new PageImpl<>(List.of()));

		service.getPageByFilters(
				pageRequest,
				new ServiceLookupFilterModel(new ServiceInstanceLookupRequestDTO(null, List.of("TemperatureManager"), null, null, null, null, null, null, null, null)));
		verify(serviceInstanceRepo).findAllBySystem_NameIn(
			argThat(list -> list.contains("TemperatureManager"))
		);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersByServiceDefinitionNames() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");

		when(serviceInstanceRepo.findAllByServiceDefinition_NameIn(any())).thenReturn(List.of());
		when(serviceInstanceRepo.findAllByIdIn(any(), any())).thenReturn(new PageImpl<>(List.of()));

		service.getPageByFilters(
				pageRequest,
				new ServiceLookupFilterModel(new ServiceInstanceLookupRequestDTO(null, null, List.of("temperatureManagement"), null, null, null, null, null, null, null)));
		verify(serviceInstanceRepo).findAllByServiceDefinition_NameIn(
			argThat(list -> list.contains("temperatureManagement"))
		);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersNoBaseFilterMatchingCases() {

		final System system = new System("TemperatureManager", "{\r\n  \"indoor\" : true\r\n}", "5.1.0");
		final ServiceDefinition serviceDefinition = new ServiceDefinition("temperatureManagement");
		final ServiceInstance existing = new ServiceInstance(
				"TemperatureManager|temperatureManagement|5.1.0",
				system,
				serviceDefinition,
				"5.1.0",
				ZonedDateTime.of(2030, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")),
				"{\r\n  \"indoor\" : true\r\n}");

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");

		final ServiceInterfaceTemplate template = new ServiceInterfaceTemplate("generic_https", "https");
		final ServiceInstanceInterface instanceInterface = new ServiceInstanceInterface(existing, template, "{\r\n  \"accessPort\" : 4041\r\n}", ServiceInterfacePolicy.RSA_SHA512_JSON_WEB_TOKEN_AUTH);

		final Page<Entry<ServiceInstance, List<ServiceInstanceInterface>>> expected =
				new PageImpl<Entry<ServiceInstance, List<ServiceInstanceInterface>>>(List.of(Map.entry(existing, List.of(instanceInterface))), pageRequest, 1);

		when(serviceInstanceRepo.findAll()).thenReturn(List.of(existing));
		when(serviceInstanceInterfaceRepo.findAllByServiceInstance(eq(existing))).thenReturn(List.of(instanceInterface));
		when(serviceInstanceRepo.findAllByIdIn(any(), any())).thenReturn(new PageImpl<>(List.of(existing)));

		Assertions.assertAll(
			// versions
			() -> {
				final Page<Entry<ServiceInstance, List<ServiceInstanceInterface>>> actual = service.getPageByFilters(
						pageRequest,
						new ServiceLookupFilterModel(new ServiceInstanceLookupRequestDTO(null, null, null, List.of("5.1.0"), null, null, null, null, null, null)));
				assertEquals(expected, actual);
			},
			// alivesAt
			() -> {
				final Page<Entry<ServiceInstance, List<ServiceInstanceInterface>>> actual = service.getPageByFilters(
						pageRequest,
						new ServiceLookupFilterModel(new ServiceInstanceLookupRequestDTO(null, null, null, null, "2030-11-04T01:53:02Z", null, null, null, null, null)));
				assertEquals(expected, actual);
			},
			// metadata
			() -> {
				final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
				requirement.put("indoor", true);

				final Page<Entry<ServiceInstance, List<ServiceInstanceInterface>>> actual = service.getPageByFilters(
						pageRequest,
						new ServiceLookupFilterModel(new ServiceInstanceLookupRequestDTO(null, null, null, null, null, List.of(requirement), null, null, null, null)));
				assertEquals(expected, actual);
			},
			// address types
			() -> {
				when(interfaceAddressPropertyProcessor.filterOnAddressTypes(any(), any())).thenReturn(true);

				final Page<Entry<ServiceInstance, List<ServiceInstanceInterface>>> actual = service.getPageByFilters(
						pageRequest,
						new ServiceLookupFilterModel(new ServiceInstanceLookupRequestDTO(null, null, null, null, null, null, List.of("IPV4"), null, null, null)));
				assertEquals(expected, actual);
			},
			// interface templates
			() -> {
				final Page<Entry<ServiceInstance, List<ServiceInstanceInterface>>> actual = service.getPageByFilters(
						pageRequest,
						new ServiceLookupFilterModel(new ServiceInstanceLookupRequestDTO(null, null, null, null, null, null, null, List.of("generic_https"), null, null)));
				assertEquals(expected, actual);
			},
			// interface properties
			() -> {
				final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
				requirement.put("accessPort", 4041);

				final Page<Entry<ServiceInstance, List<ServiceInstanceInterface>>> actual = service.getPageByFilters(
						pageRequest,
						new ServiceLookupFilterModel(new ServiceInstanceLookupRequestDTO(null, null, null, null, null, null, null, null, List.of(requirement), null)));
				assertEquals(expected, actual);
			},
			// policies
			() -> {
				final Page<Entry<ServiceInstance, List<ServiceInstanceInterface>>> actual = service.getPageByFilters(
						pageRequest,
						new ServiceLookupFilterModel(new ServiceInstanceLookupRequestDTO(null, null, null, null, null, null, null, null, null, List.of("RSA_SHA512_JSON_WEB_TOKEN_AUTH"))));
				assertEquals(expected, actual);
			}
		);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersNoBaseFilterNotMatchingCases() {

		final System system = new System("TemperatureManager", "{\r\n  \"indoor\" : true\r\n}", "5.1.0");
		final ServiceDefinition serviceDefinition = new ServiceDefinition("temperatureManagement");
		final ServiceInstance existing = new ServiceInstance(
				"TemperatureManager|temperatureManagement|5.1.0",
				system,
				serviceDefinition,
				"5.1.0",
				ZonedDateTime.of(2030, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")),
				"{\r\n  \"indoor\" : true\r\n}");

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");

		final ServiceInterfaceTemplate template = new ServiceInterfaceTemplate("generic_https", "https");
		final ServiceInstanceInterface instanceInterface = new ServiceInstanceInterface(existing, template, "{\r\n  \"accessPort\" : 4041\r\n}", ServiceInterfacePolicy.RSA_SHA512_JSON_WEB_TOKEN_AUTH);

		final Page<Entry<ServiceInstance, List<ServiceInstanceInterface>>> expected = new PageImpl<>(List.of(), pageRequest, 0);

		when(serviceInstanceRepo.findAll()).thenReturn(List.of(existing));
		when(serviceInstanceInterfaceRepo.findAllByServiceInstance(eq(existing))).thenReturn(List.of(instanceInterface));
		when(serviceInstanceRepo.findAllByIdIn(any(), any())).thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

		Assertions.assertAll(
			// versions
			() -> {
				final Page<Entry<ServiceInstance, List<ServiceInstanceInterface>>> actual = service.getPageByFilters(
						pageRequest,
						new ServiceLookupFilterModel(new ServiceInstanceLookupRequestDTO(null, null, null, List.of("5.2.0"), null, null, null, null, null, null)));
				assertEquals(expected, actual);
			},
			// alivesAt
			() -> {
				final Page<Entry<ServiceInstance, List<ServiceInstanceInterface>>> actual = service.getPageByFilters(
						pageRequest,
						new ServiceLookupFilterModel(new ServiceInstanceLookupRequestDTO(null, null, null, null, "2035-11-04T01:53:02Z", null, null, null, null, null)));
				assertEquals(expected, actual);
			},
			// metadata
			() -> {
				final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
				requirement.put("indoor", false);

				final Page<Entry<ServiceInstance, List<ServiceInstanceInterface>>> actual = service.getPageByFilters(
						pageRequest,
						new ServiceLookupFilterModel(new ServiceInstanceLookupRequestDTO(null, null, null, null, null, List.of(requirement), null, null, null, null)));
				assertEquals(expected, actual);
			},
			// address types
			() -> {
				when(interfaceAddressPropertyProcessor.filterOnAddressTypes(any(), any())).thenReturn(true);

				final Page<Entry<ServiceInstance, List<ServiceInstanceInterface>>> actual = service.getPageByFilters(
						pageRequest,
						new ServiceLookupFilterModel(new ServiceInstanceLookupRequestDTO(null, null, null, null, null, null, List.of("IPV6"), null, null, null)));
				assertEquals(expected, actual);
			},
			// interface templates
			() -> {
				final Page<Entry<ServiceInstance, List<ServiceInstanceInterface>>> actual = service.getPageByFilters(
						pageRequest,
						new ServiceLookupFilterModel(new ServiceInstanceLookupRequestDTO(null, null, null, null, null, null, null, List.of("generic_http"), null, null)));
				assertEquals(expected, actual);
			},
			// interface properties
			() -> {
				final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
				requirement.put("accessPort", 4042);

				final Page<Entry<ServiceInstance, List<ServiceInstanceInterface>>> actual = service.getPageByFilters(
						pageRequest,
						new ServiceLookupFilterModel(new ServiceInstanceLookupRequestDTO(null, null, null, null, null, null, null, null, List.of(requirement), null)));
				assertEquals(expected, actual);
			},
			// policies
			() -> {
				final Page<Entry<ServiceInstance, List<ServiceInstanceInterface>>> actual = service.getPageByFilters(
						pageRequest,
						new ServiceLookupFilterModel(new ServiceInstanceLookupRequestDTO(null, null, null, null, null, null, null, null, null, List.of("NONE"))));
				assertEquals(expected, actual);
			}
		);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersDeleteExpiredEntities() {
		
	}
}
