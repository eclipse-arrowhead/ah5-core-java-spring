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

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionRequestDTO;
import eu.arrowhead.dto.enums.NotifyProtocol;
import eu.arrowhead.dto.enums.OrchestrationFlag;
import eu.arrowhead.serviceorchestration.SimpleStoreServiceOrchestrationConstants;
import eu.arrowhead.serviceorchestration.SimpleStoreServiceOrchestrationSystemInfo;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationRequest;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationSubscriptionRequest;
import eu.arrowhead.serviceorchestration.service.normalization.OrchestrationServiceNormalization;
import io.swagger.v3.oas.models.PathItem;
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

    @Autowired
    private SystemNameNormalizer systemNameNormalizer;

    @Autowired
    private SimpleStoreServiceOrchestrationSystemInfo sysInfo;

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

            filterOutNotSupportedFlags(request, warnings);

        } catch (final InvalidParameterException ex) {
            throw new InvalidParameterException(ex.getMessage(), origin);
        }

        return request;
    }

    //-------------------------------------------------------------------------------------------------
    public SimpleOrchestrationSubscriptionRequest validateAndNormalizePushSubscribe(final OrchestrationSubscriptionRequestDTO dto, final String requesterSystemName, final Set<String> warnings, final String origin) {
        logger.debug("validateAndNormalizePushSubscribe started...");

        final SimpleOrchestrationSubscriptionRequest subscriptionRequest = validatePushSubscribe(dto, warnings, origin);
        normalizer.normalizeSubscribe(subscriptionRequest);

        // target system name
        if (Utilities.isEmpty(subscriptionRequest.getTargetSystemName())) {
            subscriptionRequest.setTargetSystemName(requesterSystemName);
        } else if (!subscriptionRequest.getTargetSystemName().equals(requesterSystemName)) {
            throw new InvalidParameterException("Target system cannot be different than the requester system", origin);
        }

        // orchestration request
        try {
            if (!Utilities.isEmpty(subscriptionRequest.getOrchestrationRequest().getServiceDefinition())) {
                serviceDefNameValidator.validateServiceDefinitionName(subscriptionRequest.getOrchestrationRequest().getServiceDefinition());
            }

            if (!Utilities.isEmpty(subscriptionRequest.getOrchestrationRequest().getPreferredProviders())) {
                subscriptionRequest.getOrchestrationRequest().getPreferredProviders().forEach(p -> systemNameValidator.validateSystemName(p));
            }

            filterOutNotSupportedFlags(subscriptionRequest.getOrchestrationRequest(), warnings);

        } catch (final InvalidParameterException ex) {
            throw new InvalidParameterException(ex.getMessage(), origin);
        }

        // notify interface
        if (!Utilities.isEnumValue(subscriptionRequest.getNotifyInterface().protocol(), NotifyProtocol.class)) {
            throw new InvalidParameterException("Unsupported notify protocol: " + subscriptionRequest.getNotifyInterface().protocol(), origin);
        }

        if (subscriptionRequest.getNotifyInterface().protocol().equals(NotifyProtocol.HTTP.name())
                || subscriptionRequest.getNotifyInterface().protocol().equals(NotifyProtocol.HTTPS.name())) {
            validateNormalizedNotifyPropertiesForHTTP(subscriptionRequest.getNotifyInterface().properties(), origin);
        } else if (subscriptionRequest.getNotifyInterface().protocol().equals(NotifyProtocol.MQTT.name())
                || subscriptionRequest.getNotifyInterface().protocol().equals(NotifyProtocol.MQTTS.name())) {
            if (!sysInfo.isMqttApiEnabled()) {
                throw new InvalidParameterException("MQTT notify protocol required, but MQTT is not enabled", origin);
            }
            validateNormalizedNotifyPropertiesForMQTT(subscriptionRequest.getNotifyInterface().properties(), origin);
        }

        return subscriptionRequest;
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

        if (!Utilities.isEmpty(ignoredFields) && warnings != null) {
            warnings.add(ignoredFieldsToString(SimpleStoreServiceOrchestrationConstants.ORCH_WARN_IGNORED_FIELDS_KEY, ignoredFields));
        }

        return simpleOrchestrationRequest;
    }

    //-----------------------------------------------------------------------------------------------
    private SimpleOrchestrationSubscriptionRequest validatePushSubscribe(final OrchestrationSubscriptionRequestDTO dto, final Set<String> warnings, final String origin) {
        logger.debug("validatePushSubscribe started...");

        if (dto == null) {
            throw new InvalidParameterException("Request payload is missing");
        }

        // orchestration request
        if (dto.orchestrationRequest() == null) {
            throw new InvalidParameterException("Orchestration request is missing", origin);
        }

        final SimpleOrchestrationRequest validatedOrchestrationRequest = validateOrchestrationRequest(dto.orchestrationRequest(), warnings, origin);

        // notify interface
        if (Utilities.isEmpty(dto.notifyInterface().protocol())) {
            throw new InvalidParameterException("Notify protocol is missing", origin);
        }

        if (Utilities.isEmpty(dto.notifyInterface().properties())) {
            throw new InvalidParameterException("Notify properties are missing", origin);
        }

        dto.notifyInterface().properties().forEach((k, v) -> {
            if (Utilities.isEmpty(k)) {
                throw new InvalidParameterException("Notify properties contains empty key", origin);
            }
            if (Utilities.isEmpty(v)) {
                throw new InvalidParameterException("Notify properties contains empty value", origin);
            }
        });

        // duration
        if (dto.duration() != null && dto.duration() <= 0) {
            throw new InvalidParameterException("Subscription duration must be greater than 0", origin);
        }

        return new SimpleOrchestrationSubscriptionRequest(dto.targetSystemName(), validatedOrchestrationRequest, dto.notifyInterface(), dto.duration());
    }

    //-----------------------------------------------------------------------------------------------
    private void filterOutNotSupportedFlags(final SimpleOrchestrationRequest request, final Set<String> warnings) {
        if (!Utilities.isEmpty(request.getOrchestrationFlags())) {
            final List<String> ignoredFlags = new ArrayList<>();
            final Map<String, Boolean> acceptedFlags = new HashMap<>();
            request.getOrchestrationFlags().forEach((key, value) -> {
                if (!Utilities.isEnumValue(key, OrchestrationFlag.class)) {
                    throw new InvalidParameterException("Invalid orchestration flag: " + key);
                }

                if (SimpleStoreServiceOrchestrationConstants.SUPPORTED_FLAGS.contains(key)) {
                    acceptedFlags.put(key, value);
                } else {
                    ignoredFlags.add(key);
                }
            });

            request.setOrchestrationFlags(acceptedFlags);

            // add warnings, if any
            if (!Utilities.isEmpty(ignoredFlags) && warnings != null) {
                warnings.add(ignoredFieldsToString(SimpleStoreServiceOrchestrationConstants.ORCH_WARN_IGNORED_FLAGS_KEY, ignoredFlags));
            }
        }
    }

    //-----------------------------------------------------------------------------------------------
    private String ignoredFieldsToString(final String fieldKey, final List<String> ignoredFields) {
        return fieldKey + ": [" + String.join(", ", ignoredFields) + "]";
    }

    //-------------------------------------------------------------------------------------------------
    private SimpleOrchestrationRequest validateOrchestrationRequest(final OrchestrationRequestDTO dto, final Set<String> warnings, final String origin) {
        return validatePull(dto, warnings, origin);
    }

    //-------------------------------------------------------------------------------------------------
    private void validateNormalizedNotifyPropertiesForHTTP(final Map<String, String> props, final String origin) {
        logger.debug("validateNormalizedNotifyPropertiesForHTTP started...");

        if (!props.containsKey(SimpleStoreServiceOrchestrationConstants.NOTIFY_KEY_ADDRESS)) {
            throw new InvalidParameterException("Notify properties has no " + SimpleStoreServiceOrchestrationConstants.NOTIFY_KEY_ADDRESS + " property", origin);
        }

        if (!props.containsKey(SimpleStoreServiceOrchestrationConstants.NOTIFY_KEY_PORT)) {
            throw new InvalidParameterException("Notify properties has no " + SimpleStoreServiceOrchestrationConstants.NOTIFY_KEY_PORT + " property", origin);
        }

        try {
            final int port = Integer.parseInt(props.get(SimpleStoreServiceOrchestrationConstants.NOTIFY_KEY_PORT));
            if (port < Constants.MIN_PORT || port > Constants.MAX_PORT) {
                throw new InvalidParameterException("Notify port is out of the valid range", origin);
            }
        } catch (final NumberFormatException ex) {
            throw new InvalidParameterException("Notify port is not a number", origin);
        }

        if (!props.containsKey(SimpleStoreServiceOrchestrationConstants.NOTIFY_KEY_METHOD)) {
            throw new InvalidParameterException("Notify properties has no " + SimpleStoreServiceOrchestrationConstants.NOTIFY_KEY_METHOD + " property", origin);
        }

        if (!(props.get(SimpleStoreServiceOrchestrationConstants.NOTIFY_KEY_METHOD).equalsIgnoreCase(PathItem.HttpMethod.POST.name())
                || props.get(SimpleStoreServiceOrchestrationConstants.NOTIFY_KEY_METHOD).equalsIgnoreCase(PathItem.HttpMethod.PUT.name())
                || props.get(SimpleStoreServiceOrchestrationConstants.NOTIFY_KEY_METHOD).equalsIgnoreCase(PathItem.HttpMethod.PATCH.name()))) {
            throw new InvalidParameterException("Unsupported notify HTTP method: " + props.get(SimpleStoreServiceOrchestrationConstants.NOTIFY_KEY_METHOD), origin);
        }

        if (!props.containsKey(SimpleStoreServiceOrchestrationConstants.NOTIFY_KEY_PATH)) {
            throw new InvalidParameterException("Notify properties has no " + SimpleStoreServiceOrchestrationConstants.NOTIFY_KEY_PATH + " property", origin);
        }
    }

    //-------------------------------------------------------------------------------------------------
    private void validateNormalizedNotifyPropertiesForMQTT(final Map<String, String> props, final String origin) {
        logger.debug("validateNormalizedNotifyPropertiesForMQTT...");

        // Sending MQTT notification is supported only via the main broker. Orchestrator does not connect to unknown brokers to send the orchestration results, so no address and port is required.

        if (!props.containsKey(SimpleStoreServiceOrchestrationConstants.NOTIFY_KEY_TOPIC)) {
            throw new InvalidParameterException("Notify properties has no " + SimpleStoreServiceOrchestrationConstants.NOTIFY_KEY_TOPIC + " member", origin);
        }
    }
}
