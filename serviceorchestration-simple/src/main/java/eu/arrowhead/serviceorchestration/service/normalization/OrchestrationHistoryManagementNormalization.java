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

package eu.arrowhead.serviceorchestration.service.normalization;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.PageService;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.dto.OrchestrationHistoryQueryRequestDTO;
import eu.arrowhead.dto.enums.OrchestrationType;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;
import eu.arrowhead.serviceorchestration.service.model.NormalizedOrchestrationJobQueryRequest;
import eu.arrowhead.serviceorchestration.service.normalization.utils.OrchestrationNormalization;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class OrchestrationHistoryManagementNormalization {

    //=================================================================================================
    // members

    @Autowired
    private SystemNameNormalizer systemNameNormalizer;

    @Autowired
    private OrchestrationNormalization orchNormalizer;

    @Autowired
    private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

    @Autowired
    private PageService pageService;

    private final Logger logger = LogManager.getLogger(this.getClass());

    //=================================================================================================
    // methods

    //-------------------------------------------------------------------------------------------------
    public NormalizedOrchestrationJobQueryRequest normalizeOrchestrationHistoryQueryRequestDTO(final OrchestrationHistoryQueryRequestDTO dto, final String origin) {
        logger.debug("normalizeOrchestrationHistoryQueryRequestDTO started...");

        if (dto == null) {
            return new NormalizedOrchestrationJobQueryRequest(null, new ArrayList<>(), new ArrayList<>(), null, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        final PageRequest pageRequest = pageService.getPageRequest(dto.pagination(), Sort.Direction.DESC, OrchestrationJob.SORTABLE_FIELDS_BY, OrchestrationJob.DEFAULT_SORT_FIELD, origin);

        return new NormalizedOrchestrationJobQueryRequest(
                pageRequest, // no need to normalize, because it happened in the getPageRequest method
                Utilities.isEmpty(dto.ids()) ? new ArrayList<>() : dto.ids().stream().map(id -> orchNormalizer.normalizeUUID(id)).toList(),
                Utilities.isEmpty(dto.statuses()) ? new ArrayList<>() : dto.statuses().stream().map(status -> OrchestrationJobStatus.valueOf(status.trim().toUpperCase())).toList(),
                Utilities.isEmpty(dto.type()) ? null : OrchestrationType.valueOf(dto.type().trim().toUpperCase()),
                Utilities.isEmpty(dto.requesterSystems()) ? new ArrayList<>() : dto.requesterSystems().stream().map(sys -> systemNameNormalizer.normalize(sys)).toList(),
                Utilities.isEmpty(dto.targetSystems()) ? new ArrayList<>() : dto.targetSystems().stream().map(sys -> systemNameNormalizer.normalize(sys)).toList(),
                Utilities.isEmpty(dto.serviceDefinitions()) ? new ArrayList<>() : dto.serviceDefinitions().stream().map(def -> serviceDefNameNormalizer.normalize(def)).toList(),
                Utilities.isEmpty(dto.subscriptionIds()) ? new ArrayList<>() : dto.subscriptionIds().stream().map(id -> orchNormalizer.normalizeUUID(id)).toList());
    }

}
