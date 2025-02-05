package eu.arrowhead.serviceorchestration.service.validation;

import java.time.DateTimeException;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.OrchestrationSubscriptionListRequestDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionRequestDTO;

@Service
public class OrchestrationPushManagementValidation {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	//-------------------------------------------------------------------------------------------------
	// methods

	// VALIDATION

	//-------------------------------------------------------------------------------------------------
	public void validatePushSubscribeService(final String requesterSystem, final OrchestrationSubscriptionListRequestDTO dto, final String origin) {
		logger.debug("validatePushSubscribeService started");

		if (Utilities.isEmpty(requesterSystem)) {
			throw new InvalidParameterException("Requester system is missing.", origin);
		}

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing.", origin);
		}

		if (Utilities.isEmpty(dto.subscriptions())) {
			throw new InvalidParameterException("Subscription list is empty.", origin);
		}

		for (final OrchestrationSubscriptionRequestDTO subscribeDTO : dto.subscriptions()) {
			if (Utilities.isEmpty(subscribeDTO.targetSystemName())) {
				throw new InvalidParameterException("Target system is missing.", origin);
			}

			if (subscribeDTO.orchestrationRequest() == null) {
				throw new InvalidParameterException("Orchestrationrequest is missing", origin);
			}

			if (!Utilities.isEmpty(subscribeDTO.orchestrationRequest().orchestrationFlags())) {
				subscribeDTO.orchestrationRequest().orchestrationFlags().forEach(f -> {
					if (Utilities.isEmpty(f)) {
						throw new InvalidParameterException("Orchestration flag list contains empty element", origin);
					}
				});
			}

			if (subscribeDTO.orchestrationRequest().exclusivityDuration() != null && subscribeDTO.orchestrationRequest().exclusivityDuration() <= 0) {
				throw new InvalidParameterException("Exclusivity duration must be grather than 0.", origin);
			}

			if (!Utilities.isEmpty(subscribeDTO.orchestrationRequest().qosRequirements())) {
				subscribeDTO.orchestrationRequest().qosRequirements().forEach((k, v) -> {
					if (Utilities.isEmpty(v)) {
						throw new InvalidParameterException("QoS Requirement map contains empty value", origin);
					}
				});
			}

			if (subscribeDTO.orchestrationRequest().serviceRequirement() == null) {
				throw new InvalidParameterException("Service requirement is missing", origin);
			}

			if (Utilities.isEmpty(subscribeDTO.orchestrationRequest().serviceRequirement().serviceDefinition())) {
				throw new InvalidParameterException("Service definition is empty", origin);
			}

			if (!Utilities.isEmpty(subscribeDTO.orchestrationRequest().serviceRequirement().operations())) {
				subscribeDTO.orchestrationRequest().serviceRequirement().operations().forEach(o -> {
					if (Utilities.isEmpty(o)) {
						throw new InvalidParameterException("Operation list contains empty element", origin);
					}
				});
			}

			if (!Utilities.isEmpty(subscribeDTO.orchestrationRequest().serviceRequirement().versions())) {
				subscribeDTO.orchestrationRequest().serviceRequirement().versions().forEach(v -> {
					if (Utilities.isEmpty(v)) {
						throw new InvalidParameterException("Version list contains empty element", origin);
					}
				});
			}

			if (!Utilities.isEmpty(subscribeDTO.orchestrationRequest().serviceRequirement().alivesAt())) {
				try {
					Utilities.parseUTCStringToZonedDateTime(subscribeDTO.orchestrationRequest().serviceRequirement().alivesAt());
				} catch (final DateTimeException ex) {
					throw new InvalidParameterException("Alives at time has an invalid time format", origin);
				}
			}

			if (!Utilities.isEmpty(subscribeDTO.orchestrationRequest().serviceRequirement().metadataRequirements()) && Utilities.containsNull(subscribeDTO.orchestrationRequest().serviceRequirement().metadataRequirements())) {
				throw new InvalidParameterException("Metadata requirement list contains null element", origin);
			}

			if (!Utilities.isEmpty(subscribeDTO.orchestrationRequest().serviceRequirement().interfaceTemplateNames())) {
				subscribeDTO.orchestrationRequest().serviceRequirement().interfaceTemplateNames().forEach(itn -> {
					if (Utilities.isEmpty(itn)) {
						throw new InvalidParameterException("Interface template name list contains empty element", origin);
					}
				});
			}

			if (!Utilities.isEmpty(subscribeDTO.orchestrationRequest().serviceRequirement().interfaceAddressTypes())) {
				subscribeDTO.orchestrationRequest().serviceRequirement().interfaceAddressTypes().forEach(iat -> {
					if (Utilities.isEmpty(iat)) {
						throw new InvalidParameterException("Interface address type list contains empty element", origin);
					}
				});
			}

			if (!Utilities.isEmpty(subscribeDTO.orchestrationRequest().serviceRequirement().interfacePropertyRequirements())
					&& Utilities.containsNull(subscribeDTO.orchestrationRequest().serviceRequirement().interfacePropertyRequirements())) {
				throw new InvalidParameterException("Interface property requirement list contains null element", origin);
			}

			if (!Utilities.isEmpty(subscribeDTO.orchestrationRequest().serviceRequirement().securityPolicies())) {
				subscribeDTO.orchestrationRequest().serviceRequirement().securityPolicies().forEach(sp -> {
					if (Utilities.isEmpty(sp)) {
						throw new InvalidParameterException("Security policy list contains empty element", origin);
					}
				});
			}

			if (!Utilities.isEmpty(subscribeDTO.orchestrationRequest().serviceRequirement().prefferedProviders())) {
				subscribeDTO.orchestrationRequest().serviceRequirement().prefferedProviders().forEach(pp -> {
					if (Utilities.isEmpty(pp)) {
						throw new InvalidParameterException("Prefferd provider list contains empty element", origin);
					}
				});
			}

		}
	}

	// VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public Pair<String, List<OrchestrationSubscriptionRequestDTO>> validateAndNormalizePushSubscribeService(final String requesterSystem, final OrchestrationSubscriptionListRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizePushSubscribeService started");

		//TODO
		return null;
	}
}
