package eu.arrowhead.serviceorchestration.service.validation;

import java.time.DateTimeException;

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
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;
import eu.arrowhead.serviceorchestration.service.normalization.OrchestrationServiceNormalization;

@Service
public class OrchestrationValidation {

	//=================================================================================================
	// members

	@Autowired
	private NameValidator nameValidator;

	@Autowired
	private OrchestrationServiceNormalization normalization;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	// VALIDATION

	//-------------------------------------------------------------------------------------------------
	public void validatePullService(final OrchestrationForm form, final String origin) {
		logger.debug("validatePull started...");

		validateOrchestrationForm(form, origin);
	}

	// VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public void validateAndNormalizePullService(final OrchestrationForm form, final String origin) {
		logger.debug("validatePull started...");

		validatePullService(form, origin);
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

}
