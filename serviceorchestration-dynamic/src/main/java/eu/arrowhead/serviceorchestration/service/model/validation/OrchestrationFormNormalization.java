package eu.arrowhead.serviceorchestration.service.model.validation;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.name.NameNormalizer;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;

@Service
public class OrchestrationFormNormalization {

	//=================================================================================================
	// members

	@Autowired
	private NameNormalizer nameNormalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void normalizeOrchestrationForm(final OrchestrationForm form) {
		logger.debug("normalizeOrchestrationForm started...");
		Assert.notNull(form, "form is null");

		form.setRequesterSystemName(nameNormalizer.normalize(form.getRequesterSystemName()));
		form.setTargetSystemName(nameNormalizer.normalize(form.getTargetSystemName()));
		form.setServiceDefinition(nameNormalizer.normalize(form.getServiceDefinition()));

		if (!Utilities.isEmpty(form.getOrchestrationFlags())) {
			final Map<String, Boolean> normalizedFlags = new HashMap<>();
			form.getOrchestrationFlags().forEach((k, v) -> normalizedFlags.put(k.trim().toUpperCase(), v));
			form.setOrchestrationFlags(normalizedFlags);
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
