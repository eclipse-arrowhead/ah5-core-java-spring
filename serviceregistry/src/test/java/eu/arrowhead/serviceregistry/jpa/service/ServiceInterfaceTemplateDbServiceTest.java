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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;

import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.LockedException;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInterfaceTemplate;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInterfaceTemplateProperty;
import eu.arrowhead.serviceregistry.jpa.repository.ServiceInterfaceTemplatePropertyRepository;
import eu.arrowhead.serviceregistry.jpa.repository.ServiceInterfaceTemplateRepository;

@ExtendWith(MockitoExtension.class)
public class ServiceInterfaceTemplateDbServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private ServiceInterfaceTemplateDbService service;

	@Mock
	private ServiceInterfaceTemplateRepository templateRepo;

	@Mock
	private ServiceInterfaceTemplatePropertyRepository templatePropsRepo;

	private static final String DB_ERROR_MSG = "Database operation error";

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByNameOk() {

		when(templateRepo.findByName(any())).thenReturn(Optional.empty());
		final Optional<ServiceInterfaceTemplate> result = service.getByName("generic_http");

		assertEquals(Optional.empty(), result);
		verify(templateRepo).findByName("generic_http");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByNameThrowsInternalServerError() {

		when(templateRepo.findByName(any())).thenThrow(new LockedException("error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.getByName("generic_http"));
		assertEquals(DB_ERROR_MSG, ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPropertiesByTemplateNameExistingTemplate() {

		final ServiceInterfaceTemplate template = new ServiceInterfaceTemplate("generic_http", "http");
		final ServiceInterfaceTemplateProperty property = new ServiceInterfaceTemplateProperty(template, "accessAddresses", true, "NOT_EMPTY_ADDRESS_LIST");

		when(templateRepo.findByName(any())).thenReturn(Optional.of(template));
		when(templatePropsRepo.findByServiceInterfaceTemplate(any())).thenReturn(List.of(property));

		final List<ServiceInterfaceTemplateProperty> actual = service.getPropertiesByTemplateName("generic_http");
		assertEquals(List.of(property), actual);
		verify(templatePropsRepo).findByServiceInterfaceTemplate(template);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPropertiesByTemplateNameNotExistingTemplate() {

		when(templateRepo.findByName(any())).thenReturn(Optional.empty());

		final List<ServiceInterfaceTemplateProperty> actual = service.getPropertiesByTemplateName("generic_http");
		assertEquals(List.of(), actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPropertiesByTemplateNameThrowsInternalServerError() {

		when(templateRepo.findByName(any())).thenThrow(new LockedException("error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.getPropertiesByTemplateName("generic_http"));
		assertEquals(DB_ERROR_MSG, ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersWithoutFilters() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		final ServiceInterfaceTemplate template = new ServiceInterfaceTemplate("generic_http", "http");
		final ServiceInterfaceTemplateProperty property = new ServiceInterfaceTemplateProperty(template, "accessAddresses", true, "NOT_EMPTY_ADDRESS_LIST");

		when(templateRepo.findAll()).thenReturn(List.of(template));
		when(templatePropsRepo.findAllByServiceInterfaceTemplateIn(any())).thenReturn(List.of(property));
		when(templateRepo.findAllByIdIn(any(), any())).thenReturn(new PageImpl<>(List.of(template), pageRequest, 1));

		final Page<Entry<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>>> expected = new PageImpl<>(List.of(Map.entry(template, List.of(property))), pageRequest, 1);
		final Page<Entry<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>>> actual = service.getPageByFilters(pageRequest, null, null);
		verify(templateRepo).findAll();
		assertEquals(expected, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersByTemplateNamesAndMatch() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		final ServiceInterfaceTemplate template = new ServiceInterfaceTemplate("generic_http", "http");
		final ServiceInterfaceTemplateProperty property = new ServiceInterfaceTemplateProperty(template, "accessAddresses", true, "NOT_EMPTY_ADDRESS_LIST");

		when(templateRepo.findAllByNameIn(any())).thenReturn(List.of(template));
		when(templatePropsRepo.findAllByServiceInterfaceTemplateIn(any())).thenReturn(List.of(property));
		when(templateRepo.findAllByIdIn(any(), any())).thenReturn(new PageImpl<>(List.of(template), pageRequest, 1));

		final Page<Entry<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>>> expected = new PageImpl<>(List.of(Map.entry(template, List.of(property))), pageRequest, 1);
		final Page<Entry<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>>> actual = service.getPageByFilters(pageRequest, List.of("generic_http"), null);
		verify(templateRepo).findAllByNameIn(List.of("generic_http"));
		assertEquals(expected, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersByProtocols() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		final ServiceInterfaceTemplate template = new ServiceInterfaceTemplate("generic_http", "http");
		final ServiceInterfaceTemplateProperty property = new ServiceInterfaceTemplateProperty(template, "accessAddresses", true, "NOT_EMPTY_ADDRESS_LIST");

		when(templateRepo.findAllByProtocolIn(any())).thenReturn(List.of(template));
		when(templatePropsRepo.findAllByServiceInterfaceTemplateIn(any())).thenReturn(List.of(property));
		when(templateRepo.findAllByIdIn(any(), any())).thenReturn(new PageImpl<>(List.of(template), pageRequest, 1));

		final Page<Entry<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>>> expected = new PageImpl<>(List.of(Map.entry(template, List.of(property))), pageRequest, 1);
		final Page<Entry<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>>> actual = service.getPageByFilters(pageRequest, null, List.of("http"));
		verify(templateRepo).findAllByProtocolIn(List.of("http"));
		assertEquals(expected, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersBothFilterButNoMatch() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		final ServiceInterfaceTemplate template = new ServiceInterfaceTemplate("generic_http", "http");

		when(templateRepo.findAllByNameIn(List.of("generic_http"))).thenReturn(List.of(template));
		when(templatePropsRepo.findAllByServiceInterfaceTemplateIn(argThat(collection -> collection != null && collection.isEmpty()))).thenReturn(List.of());
		when(templateRepo.findAllByIdIn(argThat(collection -> collection != null && collection.isEmpty()), any())).thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

		final Page<Entry<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>>> expected = new PageImpl<>(List.of(), pageRequest, 0);
		final Page<Entry<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>>> actual = service.getPageByFilters(pageRequest, List.of("generic_http"), List.of("https"));
		assertEquals(expected, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersThrowsInternalServerError() {
		
	}
}
