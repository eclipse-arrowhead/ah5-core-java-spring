package eu.arrowhead.serviceorchestration.service.validation;

import java.time.DateTimeException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.NameValidator;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.dto.enums.OrchestrationFlag;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationConstants;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationSystemInfo;
import eu.arrowhead.serviceorchestration.service.enums.NotifyProtocol;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationSubscription;
import eu.arrowhead.serviceorchestration.service.normalization.OrchestrationServiceNormalization;
import io.swagger.v3.oas.models.PathItem.HttpMethod;

@Service
public class OrchestrationValidation {

	//=================================================================================================
	// members

	@Autowired
	private NameValidator nameValidator;

	@Autowired
	private OrchestrationServiceNormalization normalization;

	@Autowired
	private DynamicServiceOrchestrationSystemInfo sysInfo;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	// VALIDATION

	//-------------------------------------------------------------------------------------------------
	public void validatePullService(final OrchestrationForm form, final String origin) {
		logger.debug("validatePull started...");

		validateOrchestrationForm(form, origin);
	}

	//-------------------------------------------------------------------------------------------------
	public void validatePushSubscribeService(final OrchestrationSubscription subscription, final String origin) {
		logger.debug("validatePushSubscribeService started...");

		if (subscription == null) {
			throw new InvalidParameterException("Request payload is missing.", origin);
		}

		if (subscription.getDuration() != null && subscription.getDuration() <= 0) {
			throw new InvalidParameterException("Subscription duration must be greater than 0.", origin);
		}

		if (Utilities.isEmpty(subscription.getNotifyProtocol())) {
			throw new InvalidParameterException("Notify protocol is missing.", origin);
		}

		if (Utilities.isEmpty(subscription.getNotifyProperties())) {
			throw new InvalidParameterException("Notify properties are missing.", origin);
		}

		subscription.getNotifyProperties().forEach((k, v) -> {
			if (Utilities.isEmpty(k)) {
				throw new InvalidParameterException("Notify properties contains empty key.", origin);
			}
			if (Utilities.isEmpty(v)) {
				throw new InvalidParameterException("Notify properties contains empty value.", origin);
			}
		});

		validateOrchestrationForm(subscription.getOrchestrationForm(), origin);
	}

	//-------------------------------------------------------------------------------------------------
	public void validatePushUnsubscribeService(final String requesterSystem, final String subscriptionId, final String origin) {
		logger.debug("validatePushUnsubscribeService started...");

		if (Utilities.isEmpty(requesterSystem)) {
			throw new InvalidParameterException("Requester system is missing.", origin);
		}

		if (Utilities.isEmpty(subscriptionId)) {
			throw new InvalidParameterException("Subscription id system is missing.", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validateNormalizedOrchestrationFormContext(final OrchestrationForm form, final String origin) {
		logger.debug("validateNormalizedOrchestrationFormContext started...");

		if (form.hasFlag(OrchestrationFlag.ONLY_INTERCLOUD) && !sysInfo.isInterCloudEnabled()) {
			throw new InvalidParameterException("ONLY_INTERCLOUD flag is present, but intercloud orchestration is not enabled.", origin);
		}

		if (form.hasFlag(OrchestrationFlag.ONLY_INTERCLOUD) && form.hasFlag(OrchestrationFlag.ALLOW_TRANSLATION)) {
			// Inter-cloud translation is not supported
			throw new InvalidParameterException("ONLY_INTERCLOUD and ALLOW_TRANSLATION flags cannot be present at the same time.", origin);
		}

		if (form.hasFlag(OrchestrationFlag.ONLY_INTERCLOUD) && !Utilities.isEmpty(form.getOperations())) {
			// The creation of inter-cloud bridges is limited to the actual operations that the requester wants to use.
			throw new InvalidParameterException("Operations must be defined, when only inter-cloud orchestration is required.", origin);
		}

		if (form.hasFlag(OrchestrationFlag.ALLOW_INTERCLOUD) && !Utilities.isEmpty(form.getOperations())) {
			// The creation of inter-cloud bridges is limited to the actual operations that the requester wants to use.
			throw new InvalidParameterException("Operations must be defined, when inter-cloud orchestration is allowed.", origin);
		}

		if (form.hasFlag(OrchestrationFlag.ALLOW_TRANSLATION) && !Utilities.isEmpty(form.getOperations())) {
			// The creation of translation bridges is limited to the actual operations that the requester wants to use.
			throw new InvalidParameterException("Operations must be defined, when translation is allowed", origin);
		}

		if (form.hasFlag(OrchestrationFlag.ONLY_PREFERRED) && !form.hasPreferredProviders()) {
			throw new InvalidParameterException("ONLY_PREFERRED falg is present, but no preferred provider is defined.", origin);
		}

		if (form.hasQoSRequirements() && !sysInfo.isQoSEnabled()) {
			throw new InvalidParameterException("QoS requirements are present, but QoS support is not enabled.", origin);
		}
	}

	// VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public void validateAndNormalizePullService(final OrchestrationForm form, final String origin) {
		logger.debug("validatePull started...");

		validatePullService(form, origin);
		validateAndNormalizeOrchestrationForm(form, origin);
	}

	//-------------------------------------------------------------------------------------------------
	public void validateAndNormalizePushSubscribeService(final OrchestrationSubscription subscription, final String origin) {
		logger.debug("validateAndNormalizePushSubscribeService started...");

		validatePushSubscribeService(subscription, origin);
		validateAndNormalizeOrchestrationForm(subscription.getOrchestrationForm(), origin);

		subscription.setNotifyProtocol(subscription.getNotifyProtocol().trim().toUpperCase());
		if (!Utilities.isEnumValue(subscription.getNotifyProtocol(), NotifyProtocol.class)) {
			throw new InvalidParameterException("Unsupported notify protocol: " + subscription.getNotifyProtocol(), origin);
		}

		final Map<String, String> normalizedNotifyProps = new HashMap<>();
		subscription.getNotifyProperties().forEach((k, v) -> {
			normalizedNotifyProps.put(k.trim().toLowerCase(), v.trim());
		});
		subscription.setNotifyProperties(normalizedNotifyProps);

		if (subscription.getNotifyProtocol().equals(NotifyProtocol.HTTP.name())
				|| subscription.getNotifyProtocol().equals(NotifyProtocol.HTTP.name())) {
			validateNormalizedNotifyPropertiesForHTTP(subscription.getNotifyProperties(), origin);
		}

		if (subscription.getNotifyProtocol().equals(NotifyProtocol.MQTT.name())
				|| subscription.getNotifyProtocol().equals(NotifyProtocol.MQTTS.name())) {
			validateNormalizedNotifyPropertiesForMQTT(subscription.getNotifyProperties(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public Pair<String, UUID> validateAndNormalizePushUnsubscribeService(final String requesterSystem, final String subscriptionId, final String origin) {
		logger.debug("validateAndNormalizePushUnsubscribeService started...");

		validateAndNormalizePushUnsubscribeService(requesterSystem, subscriptionId, origin);
		final Pair<String, String> normalized = normalization.normalizePushUnsubscribe(requesterSystem, subscriptionId);

		if (!Utilities.isUUID(normalized.getRight())) {
			throw new InvalidParameterException("Invalid subscription id.", origin);
		}

		return Pair.of(normalized.getLeft(), UUID.fromString(normalized.getRight()));

	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void validateOrchestrationForm(final OrchestrationForm form, final String origin) {
		if (form == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.isEmpty(form.getRequesterSystemName())) {
			throw new InvalidParameterException("Requester system name is empty", origin);
		}

		if (Utilities.isEmpty(form.getTargetSystemName())) {
			throw new InvalidParameterException("Target system name is empty", origin);
		}

		if (Utilities.isEmpty(form.getServiceDefinition())) {
			throw new InvalidParameterException("Service definition is empty", origin);
		}

		if (!Utilities.isEmpty(form.getOperations())) {
			form.getOperations().forEach(o -> {
				if (Utilities.isEmpty(o)) {
					throw new InvalidParameterException("Operation list contains empty element", origin);
				}
			});
		}

		if (!Utilities.isEmpty(form.getVersions())) {
			form.getVersions().forEach(v -> {
				if (Utilities.isEmpty(v)) {
					throw new InvalidParameterException("Version list contains empty element", origin);
				}
			});
		}

		if (!Utilities.isEmpty(form.getOrchestrationFlags())) {
			form.getOrchestrationFlags().forEach(f -> {
				if (Utilities.isEmpty(f)) {
					throw new InvalidParameterException("Orchestration flag list contains empty element", origin);
				}
			});
		}

		if (form.getExclusivityDuration() != null && form.getExclusivityDuration() <= 0) {
			throw new InvalidParameterException("Exclusivity duration must be grather than 0.", origin);
		}

		if (!Utilities.isEmpty(form.getAlivesAt())) {
			try {
				Utilities.parseUTCStringToZonedDateTime(form.getAlivesAt());
			} catch (final DateTimeException ex) {
				throw new InvalidParameterException("Alives at time has an invalid time format", origin);
			}
		}

		if (!Utilities.isEmpty(form.getMetadataRequirements()) && Utilities.containsNull(form.getMetadataRequirements())) {
			throw new InvalidParameterException("Metadata requirement list contains null element", origin);
		}

		if (!Utilities.isEmpty(form.getInterfaceTemplateNames())) {
			form.getInterfaceTemplateNames().forEach(itn -> {
				if (Utilities.isEmpty(itn)) {
					throw new InvalidParameterException("Interface template name list contains empty element", origin);
				}
			});
		}

		if (!Utilities.isEmpty(form.getInterfaceAddressTypes())) {
			form.getInterfaceAddressTypes().forEach(iat -> {
				if (Utilities.isEmpty(iat)) {
					throw new InvalidParameterException("Interface address type list contains empty element", origin);
				}
			});
		}

		if (!Utilities.isEmpty(form.getInterfacePropertyRequirements()) && Utilities.containsNull(form.getInterfacePropertyRequirements())) {
			throw new InvalidParameterException("Interface property requirement list contains null element", origin);
		}

		if (!Utilities.isEmpty(form.getSecurityPolicies())) {
			form.getSecurityPolicies().forEach(sp -> {
				if (Utilities.isEmpty(sp)) {
					throw new InvalidParameterException("Security policy list contains empty element", origin);
				}
			});
		}

		if (!Utilities.isEmpty(form.getPrefferedProviders())) {
			form.getPrefferedProviders().forEach(pp -> {
				if (Utilities.isEmpty(pp)) {
					throw new InvalidParameterException("Prefferd provider list contains empty element", origin);
				}
			});
		}

		if (!Utilities.isEmpty(form.getQosRequirements())) {
			form.getQosRequirements().forEach((k, v) -> {
				if (Utilities.isEmpty(v)) {
					throw new InvalidParameterException("QoS Requirement map contains empty value", origin);
				}
			});
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateAndNormalizeOrchestrationForm(final OrchestrationForm form, final String origin) {
		logger.debug("validateAndNormalizeOrchestrationForm...");

		normalization.normalizeOrchestrationForm(form);

		try {
			nameValidator.validateName(form.getRequesterSystemName());
			nameValidator.validateName(form.getTargetSystemName());
			nameValidator.validateName(form.getServiceDefinition());

			if (!Utilities.isEmpty(form.getOrchestrationFlags())) {
				form.getOrchestrationFlags().forEach(f -> {
					if (!Utilities.isEnumValue(f, OrchestrationFlag.class)) {
						throw new InvalidParameterException("Invalid orchestration flag: " + f);
					}
				});
			}

			if (!Utilities.isEmpty(form.getInterfaceTemplateNames())) {
				form.getInterfaceTemplateNames().forEach(itn -> {
					nameValidator.validateName(itn);
				});
			}

			if (!Utilities.isEmpty(form.getInterfaceAddressTypes())) {
				form.getInterfaceAddressTypes().forEach(iat -> {
					if (!Utilities.isEnumValue(iat, AddressType.class)) {
						throw new InvalidParameterException("Invalid interface address type: " + iat);
					}
				});
			}

			if (!Utilities.isEmpty(form.getSecurityPolicies())) {
				form.getSecurityPolicies().forEach(sp -> {
					if (!Utilities.isEnumValue(sp, ServiceInterfacePolicy.class)) {
						throw new InvalidParameterException("Invalid security policy: " + sp);
					}
				});
			}

			if (!Utilities.isEmpty(form.getPrefferedProviders())) {
				form.getPrefferedProviders().forEach(pp -> {
					nameValidator.validateName(pp);
				});
			}

		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validateNormalizedNotifyPropertiesForHTTP(final Map<String, String> props, final String origin) {
		logger.debug("validateNormalizedNotifyPropertiesForHTTP...");

		if (!props.containsKey(DynamicServiceOrchestrationConstants.NOTIFY_KEY_ADDRESS)) {
			throw new InvalidParameterException("Notify properties has no " + DynamicServiceOrchestrationConstants.NOTIFY_KEY_ADDRESS + " member.", origin);
		}

		if (!props.containsKey(DynamicServiceOrchestrationConstants.NOTIFY_KEY_PORT)) {
			throw new InvalidParameterException("Notify properties has no " + DynamicServiceOrchestrationConstants.NOTIFY_KEY_PORT + " member.", origin);
		}

		try {
			Integer.valueOf(props.get(DynamicServiceOrchestrationConstants.NOTIFY_KEY_PORT));
		} catch (final NumberFormatException ex) {
			throw new InvalidParameterException("Notify port is not a number", origin);
		}

		if (!props.containsKey(DynamicServiceOrchestrationConstants.NOTIFY_KEY_METHOD)) {
			throw new InvalidParameterException("Notify properties has no " + DynamicServiceOrchestrationConstants.NOTIFY_KEY_METHOD + " member.", origin);
		}

		if (!props.get(DynamicServiceOrchestrationConstants.NOTIFY_KEY_METHOD).equalsIgnoreCase(HttpMethod.POST.name())
				|| !props.get(DynamicServiceOrchestrationConstants.NOTIFY_KEY_METHOD).equalsIgnoreCase(HttpMethod.PUT.name())
				|| !props.get(DynamicServiceOrchestrationConstants.NOTIFY_KEY_METHOD).equalsIgnoreCase(HttpMethod.PATCH.name())) {
			throw new InvalidParameterException("Unsupported notify http method: " + props.get(DynamicServiceOrchestrationConstants.NOTIFY_KEY_METHOD), origin);
		}

		if (!props.containsKey(DynamicServiceOrchestrationConstants.NOTIFY_KEY_PATH)) {
			throw new InvalidParameterException("Notify properties has no " + DynamicServiceOrchestrationConstants.NOTIFY_KEY_PATH + " member.", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validateNormalizedNotifyPropertiesForMQTT(final Map<String, String> props, final String origin) {
		logger.debug("validateNormalizedNotifyPropertiesForMQTT...");

		if (!props.containsKey(DynamicServiceOrchestrationConstants.NOTIFY_KEY_ADDRESS)) {
			throw new InvalidParameterException("Notify properties has no " + DynamicServiceOrchestrationConstants.NOTIFY_KEY_ADDRESS + " member.", origin);
		}

		if (!props.containsKey(DynamicServiceOrchestrationConstants.NOTIFY_KEY_PORT)) {
			throw new InvalidParameterException("Notify properties has no " + DynamicServiceOrchestrationConstants.NOTIFY_KEY_PORT + " member.", origin);
		}

		try {
			Integer.valueOf(props.get(DynamicServiceOrchestrationConstants.NOTIFY_KEY_PORT));
		} catch (final NumberFormatException ex) {
			throw new InvalidParameterException("Notify port is not a number", origin);
		}

		if (!props.containsKey(DynamicServiceOrchestrationConstants.NOTIFY_KEY_TOPIC)) {
			throw new InvalidParameterException("Notify properties has no " + DynamicServiceOrchestrationConstants.NOTIFY_KEY_TOPIC + " member.", origin);
		}
	}
}
