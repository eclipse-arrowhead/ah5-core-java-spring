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

package eu.arrowhead.serviceorchestration.service.utils;


import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.enums.OrchestrationFlag;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationStore;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationJobDbService;
import eu.arrowhead.serviceorchestration.jpa.service.SimpleStoreDbService;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ServiceOrchestration {

    //=================================================================================================
    // members

    @Autowired
    private OrchestrationJobDbService orchJobDbService;

    @Autowired
    private SimpleStoreDbService storeDbService;

    private final Logger logger = LogManager.getLogger(this.getClass());

    //=================================================================================================
    // methods

    //-------------------------------------------------------------------------------------------------
    public List<OrchestrationStore> orchestrate(final UUID jobId, final String consumer, final SimpleOrchestrationRequest request) {
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

    //=================================================================================================
    // assistant methods

    //-------------------------------------------------------------------------------------------------
    private List<OrchestrationStore> filterOutNonPreferredProviders(final List<String> preferred, final List<OrchestrationStore> candidates) {
        return candidates
                .stream()
                .filter(candidate -> preferred.contains(candidate.getServiceInstanceId().split(Constants.COMPOSITE_ID_DELIMITER_REGEXP)[0])) // provider name
                .collect(Collectors.toList());
    }
}
