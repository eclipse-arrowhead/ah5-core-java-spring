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
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrchestrationServiceNormalization {

    //=================================================================================================
    // members

    @Autowired
    private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

    @Autowired
    private SystemNameNormalizer systemNameNormalizer;

    private final Logger logger = LogManager.getLogger(this.getClass());

    //=================================================================================================
    // methods

    //-------------------------------------------------------------------------------------------------
    public void normalizePull(final SimpleOrchestrationRequest toNormalize) {
        logger.debug("normalizePull started...");

        // service definition
        toNormalize.setServiceDefinition(serviceDefNameNormalizer.normalize(toNormalize.getServiceDefinition()));


        // preferred providers
        if (!Utilities.isEmpty(toNormalize.getPreferredProviders())) {
            toNormalize.setPreferredProviders(toNormalize.getPreferredProviders().stream().map(p -> systemNameNormalizer.normalize(p)).collect(Collectors.toList()));
        }

        // orchestration flags
        if (!Utilities.isEmpty(toNormalize.getOrchestrationFlags())) {
            final Map<String, Boolean> normalizedFlags = new HashMap<>();
            toNormalize.getOrchestrationFlags().forEach((k, v) -> normalizedFlags.put(k.trim().toUpperCase(), v));
            toNormalize.setOrchestrationFlags(normalizedFlags);
        }
    }

}
