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
package eu.arrowhead.serviceorchestration.service.dto;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.dto.OrchestrationSimpleStoreListResponseDTO;
import eu.arrowhead.dto.OrchestrationSimpleStoreResponseDTO;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationStore;

@Service
public class DTOConverter {
	
	//=================================================================================================
	// members
	
	private final Logger logger = LogManager.getLogger(this.getClass());
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public OrchestrationSimpleStoreListResponseDTO convertStoreEntityListToResponseListDTO(final List<OrchestrationStore> entities) {
		logger.debug("convertOrchestrationStoreListToResponseListDTO started...");
		Assert.notNull(entities, "entities is null");
		
		return new OrchestrationSimpleStoreListResponseDTO(
				entities.stream().map(e -> convertOrchestrationStoreEntityToResponseDTO(e)).collect(Collectors.toList()), 
				entities.size());
	}
	
	//-------------------------------------------------------------------------------------------------
	public OrchestrationSimpleStoreListResponseDTO convertStoreEntityPageToResponseListTO(final Page<OrchestrationStore> results) {
		logger.debug("convertStoreEntityPageToResponesListTO started...");
		Assert.notNull(results, "results is null");
		
		return new OrchestrationSimpleStoreListResponseDTO(
				results.stream().map(e -> convertOrchestrationStoreEntityToResponseDTO(e)).collect(Collectors.toList()), 
				results.getTotalElements());
	}
	
	//=================================================================================================
	// assistant methods
	
	//-------------------------------------------------------------------------------------------------
	private OrchestrationSimpleStoreResponseDTO convertOrchestrationStoreEntityToResponseDTO(final OrchestrationStore entity) {
		Assert.notNull(entity, "entity is null");
		
		return new OrchestrationSimpleStoreResponseDTO(
				entity.getId().toString(),
				entity.getConsumer(),
				entity.getServiceDefinition(),
				entity.getServiceInstanceId(),
				entity.getPriority(),
				entity.getCreatedBy(),
				entity.getUpdatedBy(),
				entity.getCreatedAt().toString(),
				entity.getUpdatedAt().toString());
	}

}
