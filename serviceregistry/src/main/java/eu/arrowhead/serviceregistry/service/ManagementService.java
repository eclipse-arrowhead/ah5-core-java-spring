package eu.arrowhead.serviceregistry.service;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.dto.ServiceDefinitionListRequestDTO;
import eu.arrowhead.dto.ServiceDefinitionListResponseDTO;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceDefinition;
import eu.arrowhead.serviceregistry.jpa.service.ServiceDefinitionDbService;
import eu.arrowhead.serviceregistry.service.dto.DTOConverter;
import eu.arrowhead.serviceregistry.service.validation.ManagementValidation;

@Service
public class ManagementService {

	//=================================================================================================
	// members

	@Autowired
	private ManagementValidation validator;

	@Autowired
	private ServiceDefinitionDbService serviceDefinitionDbService;

	@Autowired
	private DTOConverter dtoConverter;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public ServiceDefinitionListResponseDTO createServiceDefinitions(final ServiceDefinitionListRequestDTO dto, final String origin) {
		logger.debug("createServiceDefinitions started");

		validator.validateCreateServiceDefinition(dto, origin);

		final List<String> normalizedNames = dto.serviceDefinitionNames()
				.stream()
				.map(n -> n.trim())
				.collect(Collectors.toList());
		final List<ServiceDefinition> entities = serviceDefinitionDbService.createBulk(normalizedNames);
		return dtoConverter.convertServiceDefinitionEntityListToDTO(entities);
	}

}
