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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.dto.OrchestrationResultDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.OrchestrationSimpleStoreListResponseDTO;
import eu.arrowhead.dto.OrchestrationSimpleStoreResponseDTO;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationStore;

import static org.hibernate.validator.internal.util.Contracts.assertTrue;

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

    //-------------------------------------------------------------------------------------------------
    public OrchestrationResponseDTO convertStoreEntitiesToOrchestrationResponseDTO(final List<OrchestrationStore> entities, final Set<String> warnings) {
        logger.debug("convertStoreEntitiesToOrchestrationResponseDTO started...");
        Assert.notNull(entities, "entities is null");
        Assert.notNull(warnings, "warnings is null");

        final List<OrchestrationResultDTO> responseDTOS = new ArrayList<>(entities.size());
        entities.forEach(entity -> {
            final String[] instanceIdParts = entity.getServiceInstanceId().split(Constants.COMPOSITE_ID_DELIMITER_REGEXP);
            assertTrue(instanceIdParts.length == 3, "Invalid service instance identifier");

            responseDTOS.add(new OrchestrationResultDTO(
                    entity.getServiceInstanceId(),
                    Defaults.DEFAULT_CLOUD,
                    instanceIdParts[0], // providerName
                    entity.getServiceDefinition(),
                    instanceIdParts[2], // version
                    null,
                    null,
                    null,
                    null,
                    null
            ));
        });

        return new OrchestrationResponseDTO(responseDTOS, Utilities.isEmpty(warnings) ? null : warnings.stream().toList());
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
				Utilities.convertZonedDateTimeToUTCString(entity.getCreatedAt()),
				Utilities.convertZonedDateTimeToUTCString(entity.getUpdatedAt()));
	}

}
