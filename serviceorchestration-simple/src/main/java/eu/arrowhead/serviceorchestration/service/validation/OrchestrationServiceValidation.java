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
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionRequestDTO;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationRequest;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationSubscriptionRequest;
import eu.arrowhead.serviceorchestration.service.validation.utils.OrchestrationValidation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.UUID;

@Service
public class OrchestrationServiceValidation {

    //=================================================================================================
    // members

    @Autowired
    private OrchestrationValidation orchValidator;

    private final Logger logger = LogManager.getLogger(this.getClass());

    //=================================================================================================
    // methods

    //-------------------------------------------------------------------------------------------------
    public String validateAndNormalizeRequester(final String requesterSystemName, final String origin) {
        Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");
        logger.debug("validateAndNormalizeRequester started...");

        return orchValidator.validateAndNormalizeSystemName(requesterSystemName, origin);
    }

    //-------------------------------------------------------------------------------------------------
    public SimpleOrchestrationRequest validateAndNormalizePull(final OrchestrationRequestDTO dto, final String origin) {
        Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");
        logger.debug("validateAndNormalizePull started...");

        return orchValidator.validateAndNormalizeOrchestrationRequest(dto, origin);
    }

    //-------------------------------------------------------------------------------------------------
    public SimpleOrchestrationSubscriptionRequest validateAndNormalizePushSubscribe(final OrchestrationSubscriptionRequestDTO dto, final String requesterSystemName, final String origin) {
        Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");
        logger.debug("validateAndNormalizePushSubscribe started...");

        return orchValidator.validateAndNormalizePushSubscribe(dto, requesterSystemName, origin);
    }

    //-------------------------------------------------------------------------------------------------
    public UUID validateAndNormalizePushUnsubscribe(final String uuid, final String origin) {
        Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");
        logger.debug("validateAndNormalizePushUnsubscribe started...");

        return orchValidator.validateAndNormalizeUUID(uuid, origin);
    }

}
