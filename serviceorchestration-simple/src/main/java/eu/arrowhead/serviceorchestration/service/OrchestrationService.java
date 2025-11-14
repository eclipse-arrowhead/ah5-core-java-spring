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
package eu.arrowhead.serviceorchestration.service;

import java.util.*;
import java.util.stream.Collectors;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.dto.enums.OrchestrationType;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationStore;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationJobDbService;
import eu.arrowhead.serviceorchestration.jpa.service.SimpleStoreDbService;
import eu.arrowhead.serviceorchestration.service.dto.DTOConverter;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationRequest;
import eu.arrowhead.serviceorchestration.service.validation.OrchestrationServiceValidation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.dto.enums.OrchestrationFlag;

@Service
public class OrchestrationService {

	//=================================================================================================
	// members

    @Autowired
    private OrchestrationServiceValidation validator;

    @Autowired
    private OrchestrationJobDbService orchJobDbService;

    @Autowired
    private SimpleStoreDbService storeDbService;

    @Autowired
    private DTOConverter dtoConverter;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

    //-------------------------------------------------------------------------------------------------
    public OrchestrationResponseDTO pull(final String requesterSystem, final OrchestrationRequestDTO dto, final String origin) {
        logger.debug("pull started...");

        final Set<String> warnings = new HashSet<>();

        // validate and normalize
        final SimpleOrchestrationRequest normalized = validator.validateAndNormalizePull(dto, warnings, origin);

        try {

            // create job
            final OrchestrationJob job = orchJobDbService.create(List.of(
                            new OrchestrationJob(
                                    OrchestrationType.PULL,
                                    requesterSystem,
                                    requesterSystem, // target consumer system is the requester
                                    normalized.getServiceDefinition(),
                                    null)))
                    .getFirst();

            // orchestrate
            final List<OrchestrationStore> orchResult = orchestrate(job.getId(), requesterSystem, normalized);
            return dtoConverter.convertStoreEntitiesToOrchestrationResponseDTO(orchResult, warnings);

        } catch (final InternalServerError ex) {
            throw new InternalServerError(ex.getMessage(), origin);
        } catch (final Exception ex) {
            throw ex;
        }

    }

    //=================================================================================================
    // assistant methods

    //-------------------------------------------------------------------------------------------------
    private List<OrchestrationStore> orchestrate(final UUID jobId, final String consumer, final SimpleOrchestrationRequest request) {
        logger.debug("orchestrate started...");

        orchJobDbService.setStatus(jobId, OrchestrationJobStatus.IN_PROGRESS, null);

        final boolean hasServiceDefinition = request.getServiceDefinition() != null;
        final boolean onlyPreferred = request.getOrchestrationFlags() != null
                && request.getOrchestrationFlags().getOrDefault(OrchestrationFlag.ONLY_PREFERRED.toString(), false);
        final boolean hasPreferredProviders = request.getPreferredProviders() != null;
        final boolean matchmaking = request.getOrchestrationFlags() != null
                && request.getOrchestrationFlags().getOrDefault(OrchestrationFlag.MATCHMAKING.toString(), false);

        List<OrchestrationStore> sortedMatchingEntries = hasServiceDefinition
                ? storeDbService.getByConsumerAndServiceDefinition(consumer, request.getServiceDefinition())
                : storeDbService.getByConsumer(consumer);

        // Dealing with preferences
        if (hasPreferredProviders || onlyPreferred) {
            List<OrchestrationStore> preferredEntries = filterOutNonPreferredProviders(hasPreferredProviders ? request.getPreferredProviders() : List.of(), sortedMatchingEntries);

            if (!Utilities.isEmpty(preferredEntries) || onlyPreferred) {
                sortedMatchingEntries = preferredEntries;
            }
        }

        // Matchmaking
        if (!Utilities.isEmpty(sortedMatchingEntries) && matchmaking) {
                sortedMatchingEntries = List.of(sortedMatchingEntries.getFirst());
        }

        orchJobDbService.setStatus(jobId, OrchestrationJobStatus.DONE, sortedMatchingEntries.size() + " local result");

        return sortedMatchingEntries;
    }

    //-------------------------------------------------------------------------------------------------
    private List<OrchestrationStore> filterOutNonPreferredProviders(final List<String> preferred, final List<OrchestrationStore> candidates) {
        return candidates
                .stream()
                .filter(candidate -> preferred.contains(candidate.getServiceInstanceId().split(Constants.COMPOSITE_ID_DELIMITER_REGEXP)[0])) // provider name
                .collect(Collectors.toList());
    }
}
