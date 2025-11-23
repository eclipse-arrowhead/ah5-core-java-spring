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

package eu.arrowhead.serviceorchestration.service.validation;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.OrchestrationPushTriggerDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionListRequestDTO;
import eu.arrowhead.serviceorchestration.service.model.NormalizedOrchestrationPushTrigger;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationSubscriptionRequest;
import eu.arrowhead.serviceorchestration.service.validation.utils.OrchestrationValidation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrchestrationPushManagementServiceValidation {

    //=================================================================================================
    // members

    @Autowired
    private OrchestrationValidation orchValidator;

    private final Logger logger = LogManager.getLogger(this.getClass());

    //=================================================================================================
    // methods

    //-------------------------------------------------------------------------------------------------
    public String validateAndNormalizeRequester(final String requesterSystemName, final String origin) {
        logger.debug("validateAndNormalizeRequester started...");

        return orchValidator.validateAndNormalizeSystemName(requesterSystemName, origin);
    }

    //-------------------------------------------------------------------------------------------------
    public List<SimpleOrchestrationSubscriptionRequest> validateAndNormalizePushSubscribeBulk(final OrchestrationSubscriptionListRequestDTO dto, final String origin) {
        logger.debug("validateAndNormalizePushSubscribeBulk started...");

        return orchValidator.validateAndNormalizePushSubscribeBulk(dto, origin);
    }

    //-------------------------------------------------------------------------------------------------
    public NormalizedOrchestrationPushTrigger validateAndNormalizePushTrigger(final OrchestrationPushTriggerDTO dto, final String origin) {
        logger.debug("validateAndNormalizePushTrigger started...");

        if (dto == null) {
            throw new InvalidParameterException("Request payload is missing", origin);
        }

        final NormalizedOrchestrationPushTrigger normalized = new NormalizedOrchestrationPushTrigger();

        // target system names
        if (!Utilities.isEmpty(dto.targetSystems())) {
            if (Utilities.containsNullOrEmpty(dto.targetSystems())) {
                throw new InvalidParameterException("Target system list contains null or empty element", origin);
            }

            // validate and normalize
            final List<String> normalizedTargets = dto.targetSystems().stream().map(target -> orchValidator.validateAndNormalizeSystemName(target, origin)).toList();

            // check duplications
            final List<String> checked = new ArrayList<>(normalizedTargets.size());
            for (final String systemName : normalizedTargets) {
                if (checked.contains(systemName)) {
                    throw new InvalidParameterException("Duplicated target system name: " + systemName, origin);
                }
                checked.add(systemName);
            }

            normalized.setTargetSystems(normalizedTargets);
        }

        // subscription ids
        if (!Utilities.isEmpty(dto.subscriptionIds())) {
            if (Utilities.containsNullOrEmpty(dto.subscriptionIds())) {
                throw new InvalidParameterException("Subscription id list contains null or empty element", origin);
            }

            // validate and normalize
            final List<UUID> normalizedIds = dto.subscriptionIds().stream().map(id -> orchValidator.validateAndNormalizeUUID(id, origin)).toList();

            // check duplications
            final List<UUID> checked = new ArrayList<>(normalizedIds.size());
            for (final UUID id : normalizedIds) {
                if (checked.contains(id)) {
                    throw new InvalidParameterException("Duplicated subscription id: " + id.toString(), origin);
                }
            }

            normalized.setSubscriptionIds(normalizedIds);
        }

        return normalized;
    }

}
