package eu.arrowhead.serviceregistry.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.PageService;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.DeviceResponseDTO;
import eu.arrowhead.dto.ServiceDefinitionListRequestDTO;
import eu.arrowhead.dto.ServiceDefinitionListResponseDTO;
import eu.arrowhead.dto.SystemListRequestDTO;
import eu.arrowhead.dto.SystemListResponseDTO;
import eu.arrowhead.dto.SystemQueryRequestDTO;
import eu.arrowhead.dto.SystemRequestDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceDefinition;
import eu.arrowhead.serviceregistry.jpa.entity.SystemAddress;
import eu.arrowhead.serviceregistry.jpa.entity.System;
import eu.arrowhead.serviceregistry.jpa.service.ServiceDefinitionDbService;
import eu.arrowhead.serviceregistry.jpa.service.SystemDbService;
import eu.arrowhead.serviceregistry.service.dto.DTOConverter;
import eu.arrowhead.serviceregistry.service.normalization.ManagementNormalization;
import eu.arrowhead.serviceregistry.service.validation.ManagementValidation;
import eu.arrowhead.serviceregistry.service.validation.address.AddressNormalizator;


@Service
public class ManagementService {

	//=================================================================================================
	// members

	@Autowired
	private ManagementValidation validator;

	@Autowired
	private ServiceDefinitionDbService serviceDefinitionDbService;
	
	@Autowired
	private SystemDbService systemDbService;
	
	//@Autowired
	//private AddressNormalizator addressNormalizer;
	
	@Autowired
	private ManagementNormalization managementNormalizer;

	@Autowired
	private DTOConverter dtoConverter;
	
    @Value("${service.discovery.verbose}")
    private boolean verboseEnabled;

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
	
	//-------------------------------------------------------------------------------------------------
	public SystemListResponseDTO createSystems(final SystemListRequestDTO dto, final String origin) {
		logger.debug("createSystems started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");
		
		validator.validateCreateSystem(dto, origin);
		
		final List<SystemRequestDTO> normalized = managementNormalizer.normalizeSystemRequestDTOs(dto);
			
		normalized.forEach(n -> n.addresses().forEach(a -> validator.validateNormalizedAddress(a, origin)));

		try {
			
			final List<SystemResponseDTO> created = systemDbService.createBulk(normalized);
			return new SystemListResponseDTO(created, created.size());
			
		} catch (final InvalidParameterException ex) {
			
			throw new InvalidParameterException(ex.getMessage(), origin);
			
		} catch (final InternalServerError ex) {
			
			throw new InternalServerError(ex.getMessage(), origin);
			
		}

	}
	
	//-------------------------------------------------------------------------------------------------
	public SystemListResponseDTO querySystems(final SystemQueryRequestDTO dto, boolean verbose, final String origin) {
		
		logger.debug("querySystems started, verbose = " + verbose);
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");
		
		validator.validateQuerySystems(dto, origin);
		
		//normalize DTOs
		SystemQueryRequestDTO normalized = managementNormalizer.normalizeSystemQueryRequestDTO(dto);
		
		//validate the normalized DTO's addresses
		if (!Utilities.isEmpty(normalized.addressType())&&!Utilities.isEmpty(normalized.addresses())) {
			normalized.addresses().forEach(na -> validator.validateNormalizedAddress(new AddressDTO(normalized.addressType(), na), origin));
		}
				
		//get the response list
		List<SystemResponseDTO> result = systemDbService.getPage(normalized, origin);
		
		//we do not provide device information (except for the name), if the verbose mode is not enabled, or the user set it false in the query param
		if (!verbose || !verboseEnabled) {
			List<SystemResponseDTO> resultTerse = new ArrayList<>();
			
			for (SystemResponseDTO systemResponseDTO : result) {
				
				DeviceResponseDTO device = new DeviceResponseDTO(systemResponseDTO.device().name(), null, null, null, null);
				
				resultTerse.add(new SystemResponseDTO(
						systemResponseDTO.name(),
						systemResponseDTO.metadata(),
						systemResponseDTO.version(),
						systemResponseDTO.addresses(),
						device,
						systemResponseDTO.createdAt(),
						systemResponseDTO.updatedAt()
						));
			}
			
			result = resultTerse;
		}
		
		return new SystemListResponseDTO(result, result.size());
	}
	
	//-------------------------------------------------------------------------------------------------
	public SystemListResponseDTO updateSystems(final SystemListRequestDTO dto, final String origin) {
		
		logger.debug("updateSystems started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");
		
		validator.validateCreateSystem(dto, origin);
		
		final List<SystemRequestDTO> normalized = managementNormalizer.normalizeSystemRequestDTOs(dto);
			
		normalized.forEach(n -> n.addresses().forEach(a -> validator.validateNormalizedAddress(a, origin)));
		
		try {
			final List<SystemResponseDTO> entities = systemDbService.updateBulk(normalized);
			return new SystemListResponseDTO(entities, entities.size());

		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);

		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
		
	}
	
	//-------------------------------------------------------------------------------------------------
	public void removeSystems(final List<String> names, final String origin) {
		logger.debug("removeSystems started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		List<String> normalizedNames = managementNormalizer.normalizeSystemNames(names);

		try {
			systemDbService.deleteByNameList(normalizedNames);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}
	
	//=================================================================================================
	// assistant methods

}
