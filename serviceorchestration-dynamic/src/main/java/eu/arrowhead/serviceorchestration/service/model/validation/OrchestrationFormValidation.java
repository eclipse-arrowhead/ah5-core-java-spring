package eu.arrowhead.serviceorchestration.service.model.validation;

import java.time.DateTimeException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.NameNormalizer;
import eu.arrowhead.common.service.validation.name.NameValidator;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.dto.enums.OrchestrationFlag;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;

@Service
public class OrchestrationFormValidation {

	//=================================================================================================
	// members

	@Autowired
	private NameValidator nameValidator;

	@Autowired
	private NameNormalizer nameNormalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void validateOrchestrationForm(final OrchestrationForm form, final String origin) {
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
			form.getOperations().forEach(op -> {
				if (Utilities.isEmpty(op)) {
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
	public void validateAndNormalizeOrchestrationForm(final OrchestrationForm form, final String origin) {
		logger.debug("validateAndNormalizeOrchestrationForm...");

		normalizeOrchestrationForm(form);

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
	private void normalizeOrchestrationForm(final OrchestrationForm form) {
		logger.debug("normalizeOrchestrationForm started...");
		Assert.notNull(form, "form is null");

		form.setRequesterSystemName(nameNormalizer.normalize(form.getRequesterSystemName()));
		form.setTargetSystemName(nameNormalizer.normalize(form.getTargetSystemName()));
		form.setServiceDefinition(nameNormalizer.normalize(form.getServiceDefinition()));

		if (!Utilities.isEmpty(form.getOrchestrationFlags())) {
			form.setOrchestrationFlags(form.getOrchestrationFlags().stream().map(f -> f.trim().toUpperCase()).toList());
		}

		if (!Utilities.isEmpty(form.getOperations())) {
			form.setOperations(form.getOperations().stream().map(o -> nameNormalizer.normalize(o)).toList());
		}

		if (!Utilities.isEmpty(form.getInterfaceTemplateNames())) {
			form.setInterfaceTemplateNames(form.getInterfaceTemplateNames().stream().map(itn -> nameNormalizer.normalize(itn)).toList());
		}

		if (!Utilities.isEmpty(form.getInterfaceAddressTypes())) {
			form.setInterfaceAddressTypes(form.getInterfaceAddressTypes().stream().map(iat -> iat.trim().toUpperCase()).toList());
		}

		if (!Utilities.isEmpty(form.getSecurityPolicies())) {
			form.setSecurityPolicies(form.getSecurityPolicies().stream().map(sp -> sp.trim().toUpperCase()).toList());
		}

		if (!Utilities.isEmpty(form.getPrefferedProviders())) {
			form.setPrefferedProviders(form.getPrefferedProviders().stream().map(pp -> nameNormalizer.normalize(pp)).toList());
		}

		if (!Utilities.isEmpty(form.getQosRequirements())) {
			final Map<String, String> normalizedQoSReq = new HashMap<>();
			form.getQosRequirements().forEach((k, v) -> normalizedQoSReq.put(k.trim(), v.trim()));
			form.setQosRequirements(normalizedQoSReq);
		}
	}
}
