package eu.arrowhead.serviceregistry.service.validation.interf;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.name.NameNormalizer;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplatePropertyDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateRequestDTO;

@Component
public class InterfaceNormalizer {

	//=================================================================================================
	// members

	@Autowired
	private NameNormalizer nameNormalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// members

	//-------------------------------------------------------------------------------------------------
	public ServiceInstanceInterfaceRequestDTO normalizeInterfaceDTO(final ServiceInstanceInterfaceRequestDTO dto) {
		logger.debug("normalizeInterfaceDTO started...");
		Assert.notNull(dto, "Interface instance dto is null");

		return new ServiceInstanceInterfaceRequestDTO(
				nameNormalizer.normalize(dto.templateName()),
				Utilities.isEmpty(dto.protocol()) ? "" : dto.protocol().trim().toLowerCase(),
				dto.policy().trim().toUpperCase(),
				dto.properties());
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceInterfaceTemplateRequestDTO normalizeTemplateDTO(final ServiceInterfaceTemplateRequestDTO dto) {
		logger.debug("normalizeTemplateDTO started...");
		Assert.notNull(dto, "Interface template dto is null");

		return new ServiceInterfaceTemplateRequestDTO(
				nameNormalizer.normalize(dto.name()),
				dto.protocol().trim().toLowerCase(),
				Utilities.isEmpty(dto.propertyRequirements()) ? new ArrayList<>()
						: dto.propertyRequirements()
								.stream()
								.map(prop -> new ServiceInterfaceTemplatePropertyDTO(
										prop.name().trim(),
										prop.mandatory(),
										Utilities.isEmpty(prop.validator()) ? "" : prop.validator().trim().toUpperCase(),
										Utilities.isEmpty(prop.validatorParams()) ? new ArrayList<>()
												: prop.validatorParams()
														.stream()
														.map(param -> param.trim())
														.toList()))
								.toList());
	}
}
