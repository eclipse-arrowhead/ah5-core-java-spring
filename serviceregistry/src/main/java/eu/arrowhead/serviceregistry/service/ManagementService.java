package eu.arrowhead.serviceregistry.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

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
import eu.arrowhead.dto.DeviceResponseDTO;
import eu.arrowhead.dto.ServiceDefinitionListRequestDTO;
import eu.arrowhead.dto.ServiceDefinitionListResponseDTO;
import eu.arrowhead.dto.SystemListRequestDTO;
import eu.arrowhead.dto.SystemListResponseDTO;
import eu.arrowhead.dto.SystemQueryRequestDTO;
import eu.arrowhead.dto.SystemRequestDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceDefinition;
import eu.arrowhead.serviceregistry.jpa.entity.System;
import eu.arrowhead.serviceregistry.jpa.entity.SystemAddress;
import eu.arrowhead.dto.DeviceListRequestDTO;
import eu.arrowhead.dto.DeviceListResponseDTO;
import eu.arrowhead.dto.DeviceQueryRequestDTO;
import eu.arrowhead.dto.DeviceRequestDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.jpa.entity.Device;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceAddress;
import eu.arrowhead.serviceregistry.jpa.service.DeviceDbService;
import eu.arrowhead.serviceregistry.jpa.service.ServiceDefinitionDbService;
import eu.arrowhead.serviceregistry.jpa.service.SystemDbService;
import eu.arrowhead.serviceregistry.service.dto.DTOConverter;
import eu.arrowhead.serviceregistry.service.validation.ManagementValidation;
import org.apache.commons.lang3.tuple.Triple;


@Service
public class ManagementService {

	//=================================================================================================
	// members

	@Autowired
	private ManagementValidation validator;

	@Autowired
	private PageService pageService;

	@Autowired
	private DeviceDbService deviceDbService;

	@Autowired
	private ServiceDefinitionDbService serviceDefinitionDbService;

	@Autowired
	private SystemDbService systemDbService;

	@Autowired
	private DTOConverter dtoConverter;

    @Value("${service.discovery.verbose}")
    private boolean verboseEnabled;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	// DEVICES

	//-------------------------------------------------------------------------------------------------
	public DeviceListResponseDTO createDevices(final DeviceListRequestDTO dto, final String origin) {
		logger.debug("createDevices started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final List<DeviceRequestDTO> normalized = validator.validateAndNormalizeCreateDevices(dto, origin);

		try {
			final List<Entry<Device, List<DeviceAddress>>> entities = deviceDbService.createBulk(normalized);
			return dtoConverter.convertDeviceAndDeviceAddressEntriesToDTO(entities, entities.size());

		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);

		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public DeviceListResponseDTO updateDevices(final DeviceListRequestDTO dto, final String origin) {
		logger.debug("updateDevices started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final List<DeviceRequestDTO> normalized = validator.validateAndNormalizeUpdateDevices(dto, origin);

		try {
			final List<Entry<Device, List<DeviceAddress>>> entities = deviceDbService.updateBulk(normalized);
			return dtoConverter.convertDeviceAndDeviceAddressEntriesToDTO(entities, entities.size());

		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);

		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public DeviceListResponseDTO queryDevices(final DeviceQueryRequestDTO dto, final String origin) {
		logger.debug("queryDevices started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");
		final DeviceQueryRequestDTO normalized = validator.validateAndNormalizeQueryDevices(dto, origin);

		final PageRequest pageRequest = pageService.getPageRequest(normalized.pagination(), Direction.DESC, Device.SORTABLE_FIELDS_BY, Device.DEFAULT_SORT_FIELD, origin);

		final Page<Entry<Device, List<DeviceAddress>>> page = deviceDbService.getPage(pageRequest, normalized.deviceNames(), normalized.addresses(),
				Utilities.isEmpty(normalized.addressType()) ? null : AddressType.valueOf(normalized.addressType()), normalized.metadataRequirementList());
		return dtoConverter.convertDeviceAndDeviceAddressEntriesToDTO(page, page.getTotalElements());
	}

	//-------------------------------------------------------------------------------------------------
	public void removeDevices(final List<String> names, final String origin) {
		logger.debug("removeDevices started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");
		try {
			final List<String> normalized = validator.validateAndNormalizeRemoveDevices(names, origin);
			deviceDbService.deleteByNameList(normalized);

		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}
	}

	// SERVICES DEFINITIONS

	//-------------------------------------------------------------------------------------------------
	public ServiceDefinitionListResponseDTO createServiceDefinitions(final ServiceDefinitionListRequestDTO dto, final String origin) {
		logger.debug("createServiceDefinitions started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validator.validateCreateServiceDefinition(dto, origin);

		final List<String> normalizedNames = dto.serviceDefinitionNames()
				.stream()
				.map(n -> n.trim())
				.collect(Collectors.toList());
		final List<ServiceDefinition> entities = serviceDefinitionDbService.createBulk(normalizedNames);
		return dtoConverter.convertServiceDefinitionEntityListToDTO(entities);
	}

	// SYSTEMS

	//-------------------------------------------------------------------------------------------------
	public SystemListResponseDTO createSystems(final SystemListRequestDTO dto, final String origin) {
		logger.debug("createSystems started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final List<SystemRequestDTO> normalized = validator.validateAndNormalizeCreateSystems(dto, origin);

		try {

			final List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> entities = systemDbService.createBulk(normalized);
			return dtoConverter.convertSystemTriplesToDTO(entities);

		} catch (final InvalidParameterException ex) {

			throw new InvalidParameterException(ex.getMessage(), origin);

		} catch (final InternalServerError ex) {

			throw new InternalServerError(ex.getMessage(), origin);

		}

	}

	//-------------------------------------------------------------------------------------------------
	public SystemListResponseDTO querySystems(final SystemQueryRequestDTO dto, final boolean verbose, final String origin) {

		logger.debug("querySystems started, verbose = " + verbose);
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final SystemQueryRequestDTO normalized = validator.validateAndNormalizeQuerySystems(dto, origin);

		try {
			final SystemListResponseDTO result = systemDbService.getPageByFilters(normalized, origin);

			//we do not provide device information (except for the name), if the verbose mode is not enabled, or the user set it false in the query param
			if (!verbose || !verboseEnabled) {

				final List<SystemResponseDTO> resultTerse = new ArrayList<>();

				for (final SystemResponseDTO systemResponseDTO : result.entries()) {

					DeviceResponseDTO device = null;
					if (systemResponseDTO.device() != null) {
						device = new DeviceResponseDTO(systemResponseDTO.device().name(), null, null, null, null);
					}

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

				return new SystemListResponseDTO(resultTerse, result.count());
			}

			return result;
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public SystemListResponseDTO updateSystems(final SystemListRequestDTO dto, final String origin) {

		logger.debug("updateSystems started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final List<SystemRequestDTO> normalized = validator.validateAndNormalizeUpdateSystems(dto, origin);

		try {
			return systemDbService.updateBulk(normalized);

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

		final List<String> normalizedNames = validator.validateAndNormalizeRemoveSystems(names, origin);

		try {
			systemDbService.deleteByNameList(normalizedNames);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}
}
