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

import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.dto.OrchestrationSubscriptionListRequestDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionListResponseDTO;
import eu.arrowhead.serviceorchestration.SimpleStoreServiceOrchestrationConstants;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;
import eu.arrowhead.serviceorchestration.jpa.service.SubscriptionDbService;
import eu.arrowhead.serviceorchestration.service.dto.DTOConverter;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationSubscriptionRequest;
import eu.arrowhead.serviceorchestration.service.validation.OrchestrationValidation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class OrchestrationPushManagementService {

    //=================================================================================================
    // members

    @Autowired
    private OrchestrationValidation validator;

    @Autowired
    private SubscriptionDbService subscriptionDbService;

    @Autowired
    private DTOConverter dtoConverter;

    private final Logger logger = LogManager.getLogger(this.getClass());

    //=================================================================================================
    // methods

    //-------------------------------------------------------------------------------------------------
    public OrchestrationSubscriptionListResponseDTO pushSubscribeBulk(final String requesterSystem, final OrchestrationSubscriptionListRequestDTO dto, final String origin) {
        logger.debug("pushSubscribeBulk started...");

        final List<Set<String>> warnings = new ArrayList<Set<String>>();
        for (int i = 0; i < dto.subscriptions().size(); ++i) {
            warnings.add(new HashSet<>());
        }

        final String normalizedRequester = validator.validateAndNormalizeRequester(requesterSystem, origin);
        final List<SimpleOrchestrationSubscriptionRequest> normalized = validator.validateAndNormalizePushSubscribeBulk(dto, origin);

        try {
            final List<Subscription> result;
            synchronized (SimpleStoreServiceOrchestrationConstants.SYNC_LOCK_SUBSCRIPTION) {
                result = subscriptionDbService.create(normalized, normalizedRequester);
            }

            return dtoConverter.convertSubscriptionListToDTO(result, result.size());
        } catch (final InternalServerError ex) {
            throw new InternalServerError(ex.getMessage(), origin);
        }
    }

}
