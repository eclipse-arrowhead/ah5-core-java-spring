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
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.ServiceDefinitionListRequestDTO;
import eu.arrowhead.dto.ServiceDefinitionListResponseDTO;
import eu.arrowhead.dto.SystemListRequestDTO;
import eu.arrowhead.dto.SystemListResponseDTO;
import eu.arrowhead.dto.SystemQueryRequestDTO;
import eu.arrowhead.dto.SystemRequestDTO;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceDefinition;
import eu.arrowhead.serviceregistry.jpa.entity.SystemAddress;
import eu.arrowhead.serviceregistry.jpa.entity.System;
import eu.arrowhead.serviceregistry.jpa.service.ServiceDefinitionDbService;
import eu.arrowhead.serviceregistry.jpa.service.SystemDbService;
import eu.arrowhead.serviceregistry.service.dto.DTOConverter;
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
	
	@Autowired
	private AddressNormalizator addressNormalizator;

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
		
		final List<SystemRequestDTO> normalized = normalizeSystemRequestDTOs(dto);
			
		normalized.forEach(n -> n.addresses().forEach(a -> validator.validateNormalizedAddress(a, origin)));

		try {
			
			final List<Entry<System, List<SystemAddress>>> entities = systemDbService.createBulk(normalized); 
			return dtoConverter.convertSystemEntriesToDTO(entities);
			
		} catch (final InvalidParameterException ex) {
			
			throw new InvalidParameterException(ex.getMessage(), origin);
			
		} catch (final InternalServerError ex) {
			
			throw new InternalServerError(ex.getMessage(), origin);
			
		}

	}
	
	//TODO: ket kulon fuggveny, az egyik a verbose-nak, a masik a terse-nek. A fuggvenyekben ellenorizni kell,
	//hogy az application.properties-ben milyen ertek van beallitva!
	//-------------------------------------------------------------------------------------------------
	public SystemListResponseDTO querySystemsVerbose(final SystemQueryRequestDTO dto, final String origin) {
		
		logger.debug("querySystemsVerbose started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");
		
		validator.validateQuerySystems(dto, origin);
		
		//normalize DTOs
		SystemQueryRequestDTO normalized = normalizeSystemQueryRequestDTO(dto);
		
		//validate the normalized DTO's addresses
		
		//create the pageRequest
		
		//create the page
		
		//convert the page to DTO and return with it
		
		//TODO!!!
		throw new NotImplementedException();
	}
	
	public SystemListResponseDTO querySystemsTerse(final SystemQueryRequestDTO dto, final String origin) {
		throw new NotImplementedException();
	}
	
	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private List<SystemRequestDTO> normalizeSystemRequestDTOs(final SystemListRequestDTO dtoList) {
		
		final List<SystemRequestDTO> normalized = new ArrayList<>(dtoList.systems().size());
		for (final SystemRequestDTO system : dtoList.systems()) {
			normalized.add(new SystemRequestDTO(
					system.name().trim(),
					system.metadata(),
					system.version().trim(),
					Utilities.isEmpty(system.addresses()) ? new ArrayList<>()
							: system.addresses().stream()
									.map(a -> new AddressDTO(a.type().trim(), addressNormalizator.normalize(a.address())))
									.collect(Collectors.toList()),
					system.deviceName().trim()));
		}
		return normalized;
	}
	
	private SystemQueryRequestDTO normalizeSystemQueryRequestDTO(final SystemQueryRequestDTO dto) {
		return new SystemQueryRequestDTO(
				dto.pagination(), //no need to normailze, because it will happen in the getPageRequest method
				Utilities.isEmpty(dto.systemNames()) ? null 
						: dto.systemNames().stream().map(n -> n.trim()).collect(Collectors.toList()), 
				Utilities.isEmpty(dto.addresses()) ? null 
						: dto.addresses().stream().map(n -> n.trim()).collect(Collectors.toList()), 
				Utilities.isEmpty(dto.addressType()) ? null 
						: dto.addressType().trim(),
				dto.metadataRequirementList(), 
				Utilities.isEmpty(dto.versions()) ? null 
						: dto.versions().stream().map(n -> n.trim()).collect(Collectors.toList()), 
				Utilities.isEmpty(dto.deviceNames()) ? null 
						: dto.deviceNames().stream().map(n -> n.trim()).collect(Collectors.toList()));
	}

}
