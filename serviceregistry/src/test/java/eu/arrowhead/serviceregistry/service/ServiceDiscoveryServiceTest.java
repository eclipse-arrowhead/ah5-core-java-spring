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
package eu.arrowhead.serviceregistry.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.serviceregistry.ServiceRegistrySystemInfo;
import eu.arrowhead.serviceregistry.jpa.service.ServiceInstanceDbService;
import eu.arrowhead.serviceregistry.jpa.service.SystemDbService;
import eu.arrowhead.serviceregistry.service.dto.DTOConverter;
import eu.arrowhead.serviceregistry.service.validation.ServiceDiscoveryValidation;

@ExtendWith(MockitoExtension.class)
public class ServiceDiscoveryServiceTest {

	//=================================================================================================
	// members
	
	@InjectMocks
	private ServiceDiscoveryService service;

	@Mock
	private ServiceRegistrySystemInfo sysInfo;

	@Mock
	private ServiceDiscoveryValidation validator;

	@Mock
	private ServiceInstanceDbService instanceDbService;

	@Mock
	private SystemDbService systemDbService;

	@Mock
	private DTOConverter dtoConverter;
	
	//=================================================================================================
	// methods
}
