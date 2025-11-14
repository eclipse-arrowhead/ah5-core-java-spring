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
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.enums.OrchestrationFlag;
import eu.arrowhead.serviceorchestration.SimpleStoreServiceOrchestrationConstants;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationRequest;
import eu.arrowhead.serviceorchestration.service.normalization.OrchestrationServiceNormalization;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OrchestrationServiceValidation {

    //=================================================================================================
    // members

    @Autowired
    private OrchestrationServiceNormalization normalizer;

    @Autowired
    private ServiceDefinitionNameValidator serviceDefNameValidator;

    @Autowired
    private SystemNameValidator systemNameValidator;

    private final Logger logger = LogManager.getLogger(this.getClass());

    //=================================================================================================
    // methods

    //-------------------------------------------------------------------------------------------------
    public SimpleOrchestrationRequest validateAndNormalizePull(final OrchestrationRequestDTO dto, final Set<String> warnings, final String origin) {
        logger.debug("validateAndNormalizePull started...");

        final SimpleOrchestrationRequest request = validatePull(dto, warnings, origin);
        normalizer.normalizePull(request);

        try {
            if (!Utilities.isEmpty(request.getServiceDefinition())) {
                serviceDefNameValidator.validateServiceDefinitionName(request.getServiceDefinition());
            }

            if (!Utilities.isEmpty(request.getPreferredProviders())) {
                request.getPreferredProviders().forEach(p -> systemNameValidator.validateSystemName(p));
            }

        } catch (final InvalidParameterException ex) {
            throw new InvalidParameterException(ex.getMessage(), origin);
        }

        if (!Utilities.isEmpty(request.getOrchestrationFlags())) {
            final List<String> ignoredFlags = new ArrayList<>();
            final Map<String, Boolean> acceptedFlags = new HashMap<>();
            request.getOrchestrationFlags().forEach((key, value) -> {
                if (!Utilities.isEnumValue(key, OrchestrationFlag.class)) {
                    throw new InvalidParameterException("Invalid orchestration flag: " + key, origin);
                }

                if (SimpleStoreServiceOrchestrationConstants.SUPPORTED_FLAGS.contains(key)) {
                    acceptedFlags.put(key, value);
                } else {
                    ignoredFlags.add(key);
                }
            });

            request.setOrchestrationFlags(acceptedFlags);

            // add warnings, if any
            if (!Utilities.isEmpty(ignoredFlags)) {
                warnings.add(ignoredFieldsToString(SimpleStoreServiceOrchestrationConstants.ORCH_WARN_IGNORED_FLAGS_KEY, ignoredFlags));
            }
        }

        return request;
    }

    //=================================================================================================
    // assistant methods

    //-----------------------------------------------------------------------------------------------
    private SimpleOrchestrationRequest validatePull(final OrchestrationRequestDTO dto, final Set<String> warnings, final String origin) {
        logger.debug("validatePull started...");

        // instance that does not contain the ignored fields
        SimpleOrchestrationRequest simpleOrchestrationRequest = new SimpleOrchestrationRequest(null, null, null);

        // ignored fields will be added as warnings
        final List<String> ignoredFields = new ArrayList<String>();

        if (dto == null) {
            throw new InvalidParameterException("Request payload is missing", origin);
        }

        if (dto.serviceRequirement() != null) {

            if (!Utilities.isEmpty(dto.serviceRequirement().serviceDefinition())) {
                simpleOrchestrationRequest.setServiceDefinition(dto.serviceRequirement().serviceDefinition());
            }

            if (!Utilities.isEmpty(dto.serviceRequirement().operations())) {
                ignoredFields.add(SimpleStoreServiceOrchestrationConstants.FIELD_OPERATIONS);
            }

            if (!Utilities.isEmpty(dto.serviceRequirement().versions())) {
                ignoredFields.add(SimpleStoreServiceOrchestrationConstants.FIELD_VERSIONS);
            }

            if (!Utilities.isEmpty(dto.serviceRequirement().alivesAt())) {
                ignoredFields.add(SimpleStoreServiceOrchestrationConstants.FIELD_ALIVES_AT);
            }

            if (!Utilities.isEmpty(dto.serviceRequirement().metadataRequirements())) {
                ignoredFields.add(SimpleStoreServiceOrchestrationConstants.FIELD_METADATA_REQ);
            }

            if (!Utilities.isEmpty(dto.serviceRequirement().interfaceTemplateNames())) {
                ignoredFields.add(SimpleStoreServiceOrchestrationConstants.FIELD_INTF_TEMPLATE_NAMES);
            }

            if (!Utilities.isEmpty(dto.serviceRequirement().interfaceAddressTypes())) {
                ignoredFields.add(SimpleStoreServiceOrchestrationConstants.FIELD_INTF_ADDRESS_TYPES);
            }

            if (!Utilities.isEmpty(dto.serviceRequirement().interfacePropertyRequirements())) {
                ignoredFields.add(SimpleStoreServiceOrchestrationConstants.FIELD_INTF_PROP_REQ);
            }

            if (!Utilities.isEmpty(dto.serviceRequirement().securityPolicies())) {
                ignoredFields.add(SimpleStoreServiceOrchestrationConstants.FIELD_SECURITY_POLICIES);
            }

            if (!Utilities.isEmpty(dto.serviceRequirement().preferredProviders())) {
                if (Utilities.containsNullOrEmpty(dto.serviceRequirement().preferredProviders())) {
                    throw new InvalidParameterException("Preferred providers contains null or empty element", origin);
                }

                simpleOrchestrationRequest.setPreferredProviders(dto.serviceRequirement().preferredProviders());
            }
        }

        if (!Utilities.isEmpty(dto.orchestrationFlags())) {
            dto.orchestrationFlags().values().forEach(value -> {
                if (value == null) {
                throw new InvalidParameterException("Orchestration flag map contains null value", origin);
                }
            });

            // ignored flags will be collected after normalization

            simpleOrchestrationRequest.setOrchestrationFlags(dto.orchestrationFlags());
        }

        if (!Utilities.isEmpty(dto.qosRequirements())) {
            ignoredFields.add(SimpleStoreServiceOrchestrationConstants.FIELD_QOS_REQ);
        }

        if (dto.exclusivityDuration() != null) {
            ignoredFields.add(SimpleStoreServiceOrchestrationConstants.FIELD_EXCLUSIVITY_DURATION);
        }

        if (!Utilities.isEmpty(ignoredFields)) {
            warnings.add(ignoredFieldsToString(SimpleStoreServiceOrchestrationConstants.ORCH_WARN_IGNORED_FIELDS_KEY, ignoredFields));
        }

        return simpleOrchestrationRequest;
    }

    //-----------------------------------------------------------------------------------------------
    private String ignoredFieldsToString(final String fieldKey, final List<String> ignoredFields) {
        return fieldKey + ": [" + String.join(", ", ignoredFields) + "]";
    }
}
