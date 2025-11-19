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

import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.dto.OrchestrationSubscriptionRequestDTO;
import eu.arrowhead.dto.enums.OrchestrationType;
import eu.arrowhead.serviceorchestration.SimpleStoreServiceOrchestrationConstants;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationStore;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationJobDbService;
import eu.arrowhead.serviceorchestration.jpa.service.SubscriptionDbService;
import eu.arrowhead.serviceorchestration.service.dto.DTOConverter;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationRequest;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationSubscriptionRequest;
import eu.arrowhead.serviceorchestration.service.utils.ServiceOrchestration;
import eu.arrowhead.serviceorchestration.service.validation.OrchestrationServiceValidation;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationResponseDTO;

@Service
public class OrchestrationService {

	//=================================================================================================
	// members

    @Autowired
    private OrchestrationServiceValidation validator;

    @Autowired
    private OrchestrationJobDbService orchJobDbService;

    @Autowired
    private SubscriptionDbService subscriptionDbService;

    @Autowired
    private ServiceOrchestration serviceOrchestration;

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
            final List<OrchestrationStore> orchResult = serviceOrchestration.orchestrate(job.getId(), requesterSystem, normalized);
            return dtoConverter.convertStoreEntitiesToOrchestrationResponseDTO(orchResult, warnings);

        } catch (final InternalServerError ex) {
            throw new InternalServerError(ex.getMessage(), origin);
        } catch (final Exception ex) {
            throw ex;
        }
    }

    //-------------------------------------------------------------------------------------------------
    public Pair<Boolean, String> pushSubscribe(final String requesterSystem, final OrchestrationSubscriptionRequestDTO dto, final Boolean trigger, final String origin) {
        logger.debug("pushSubscribe started...");

        // validate and normalize
        final SimpleOrchestrationSubscriptionRequest normalized = validator.validateAndNormalizePushSubscribe(dto, requesterSystem, origin);

        // save subscription
        Pair<Boolean, String> response = null;
        synchronized (SimpleStoreServiceOrchestrationConstants.SYNC_LOCK_SUBSCRIPTION) {
            final Optional<Subscription> recordOpt = subscriptionDbService.get(
                    requesterSystem,
                    normalized.getTargetSystemName(),
                    normalized.getOrchestrationRequest().getServiceDefinition());

            final boolean isOverride = recordOpt.isPresent();

            final List<Subscription> result = subscriptionDbService.create(List.of(normalized), requesterSystem);
            response = Pair.of(isOverride, result.getFirst().getId().toString());
        }

        return response;

        // TODO: do orchestration
    }
}
