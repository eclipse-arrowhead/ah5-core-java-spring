package eu.arrowhead.serviceregistry.service.validation.interf;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.intf.properties.IPropertyValidator;
import eu.arrowhead.common.intf.properties.PropertyValidatorType;
import eu.arrowhead.common.intf.properties.PropertyValidators;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateRequestDTO;
import eu.arrowhead.serviceregistry.ServiceRegistryConstants;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInterfaceTemplate;
import eu.arrowhead.serviceregistry.jpa.service.ServiceInterfaceTemplateDbService;
import eu.arrowhead.serviceregistry.service.validation.name.NameValidator;

@Component
public class InterfaceValidator {

	//=================================================================================================
	// members

	@Autowired
	private ServiceInterfaceTemplateDbService interfaceTemplateDbService;

	@Autowired
	private NameValidator nameValidator;

	@Autowired
	private PropertyValidators interfacePropertyValidator;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void validateNormalizedInterfaceInstances(final List<ServiceInstanceInterfaceRequestDTO> interfaces) throws InvalidParameterException {
		logger.debug("validateNormalizedInterfaceInstances started...");
		Assert.isTrue(!Utilities.isEmpty(interfaces), "Interface instance list is empty");

		interfaces.forEach(interfaceInstance -> {
			nameValidator.validateName(interfaceInstance.templateName());

			final Optional<ServiceInterfaceTemplate> templateOpt = interfaceTemplateDbService.getByName(interfaceInstance.templateName());
			if (templateOpt.isPresent()) {
				if (!Utilities.isEmpty(interfaceInstance.protocol()) && !interfaceInstance.protocol().equals(templateOpt.get().getProtocol())) {
					throw new InvalidParameterException(interfaceInstance.protocol() + " protocol is invalid for " + interfaceInstance.templateName());
				}

				interfaceTemplateDbService.getPropertiesByTemplateName(interfaceInstance.templateName())
						.forEach(templateProp -> {
							final Object instanceProp = interfaceInstance.properties().get(templateProp.getPropertyName());

							if (instanceProp == null && templateProp.isMandatory()) {
								throw new InvalidParameterException(templateProp.getPropertyName() + " interface property is missing for " + templateProp.getServiceInterfaceTemplate().getName());
							}

							if (!Utilities.isEmpty(templateProp.getValidator())) {
								final String[] validatorWithArgs = templateProp.getValidator().split("\\" + ServiceRegistryConstants.INTERFACE_PROPERTY_VALIDATOR_DELIMITER);
								final IPropertyValidator validator = interfacePropertyValidator.getValidator(PropertyValidatorType.valueOf(validatorWithArgs[0]));
								if (validator != null) {
									final Object normalizedProp = validator.validateNormalize(
											instanceProp,
											validatorWithArgs.length <= 1 ? new String[0] : Arrays.copyOfRange(validatorWithArgs, 1, validatorWithArgs.length));
									interfaceInstance.properties().put(templateProp.getPropertyName(), normalizedProp);
								}
							}
						});
			}
		});
	}

	//-------------------------------------------------------------------------------------------------
	public void validateNormalizedInterfaceTemplates(final List<ServiceInterfaceTemplateRequestDTO> templates) throws InvalidParameterException {
		logger.debug("validateNormalizedInterfaceTemplates started...");
		Assert.isTrue(!Utilities.isEmpty(templates), "Interface template list is empty");

		templates.forEach(template -> {
			nameValidator.validateName(template.name());

			template.propertyRequirements().forEach(prop -> {
				if (Utilities.isEnumValue(prop.validator(), PropertyValidatorType.class)) {
					throw new InvalidParameterException("Unknown property validator: " + prop.validator());
				}
			});
		});
	}
}
