package eu.arrowhead.serviceregistry.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
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
import eu.arrowhead.dto.DeviceListRequestDTO;
import eu.arrowhead.dto.DeviceListResponseDTO;
import eu.arrowhead.dto.DeviceQueryRequestDTO;
import eu.arrowhead.dto.DeviceRequestDTO;
import eu.arrowhead.dto.ServiceDefinitionListRequestDTO;
import eu.arrowhead.dto.ServiceDefinitionListResponseDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.jpa.entity.Device;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceAddress;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceDefinition;
import eu.arrowhead.serviceregistry.jpa.service.DeviceDbService;
import eu.arrowhead.serviceregistry.jpa.service.ServiceDefinitionDbService;
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
	private PageService pageService;

	@Autowired
	private DeviceDbService deviceDbService;

	@Autowired
	private ServiceDefinitionDbService serviceDefinitionDbService;

	@Autowired
	private AddressNormalizator addressNormalizator;

	@Autowired
	private DTOConverter dtoConverter;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	// DEVICES

	//-------------------------------------------------------------------------------------------------
	public DeviceListResponseDTO createDevices(final DeviceListRequestDTO dto, final String origin) {
		logger.debug("createDevices started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validator.validateCreateDevices(dto, origin);

		final List<DeviceRequestDTO> normalized = normalizeDeviceRequestDTOs(dto.devices());
		normalized.forEach(n -> n.addresses().forEach(address -> validator.validateNormalizedAddress(address, origin)));

		try {
			final List<Entry<Device, List<DeviceAddress>>> entities = deviceDbService.createBulk(normalized);
			return dtoConverter.convertDeviceAddressEntriesToDTO(entities, entities.size());

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

		validator.validateUpdateDevices(dto, origin);

		final List<DeviceRequestDTO> normalized = normalizeDeviceRequestDTOs(dto.devices());
		normalized.forEach(n -> n.addresses().forEach(address -> validator.validateNormalizedAddress(address, origin)));

		try {
			final List<Entry<Device, List<DeviceAddress>>> entities = deviceDbService.updateBulk(normalized);
			return dtoConverter.convertDeviceAddressEntriesToDTO(entities, entities.size());

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

		validator.validateQueryDevices(dto, origin);

		final DeviceQueryRequestDTO normalized = dto == null ? new DeviceQueryRequestDTO(null, null, null, null, null)
				: new DeviceQueryRequestDTO(
						dto.pagination(),
						Utilities.isEmpty(dto.deviceNames()) ? null : dto.deviceNames().stream().map(n -> n.trim()).collect(Collectors.toList()),
						Utilities.isEmpty(dto.addresses()) ? null : dto.addresses().stream().map(a -> addressNormalizator.normalize(a)).collect(Collectors.toList()),
						Utilities.isEmpty(dto.addressType()) ? null : dto.addressType().trim(),
						dto.metadataRequirementList());

		if (!Utilities.isEmpty(normalized.addressType()) && !Utilities.isEmpty(normalized.addresses())) {
			normalized.addresses().forEach(a -> validator.validateNormalizedAddress(new AddressDTO(normalized.addressType(), a), origin));
		}

		final PageRequest pageRequest = pageService.getPageRequest(normalized.pagination(), Direction.DESC, Device.SORTABLE_FIELDS_BY, Device.DEFAULT_SORT_FIELD, origin);

		final Page<Entry<Device, List<DeviceAddress>>> page = deviceDbService.getPage(pageRequest, normalized.deviceNames(), normalized.addresses(),
				Utilities.isEmpty(normalized.addressType()) ? null : AddressType.valueOf(normalized.addressType()), normalized.metadataRequirementList());
		return dtoConverter.convertDeviceAddressEntriesToDTO(page, page.getTotalElements());
	}

	//-------------------------------------------------------------------------------------------------
	public void removeDevices(final List<String> names, final String origin) {
		logger.debug("removeDevices started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final List<String> normalized = names.stream()
				.filter(n -> !Utilities.isEmpty(n))
				.map(n -> n.trim())
				.collect(Collectors.toList());

		try {
			deviceDbService.deleteByNameList(normalized);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
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
		try {
			final List<ServiceDefinition> entities = serviceDefinitionDbService.createBulk(normalizedNames);
			return dtoConverter.convertServiceDefinitionEntityListToDTO(entities);

		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);

		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private List<DeviceRequestDTO> normalizeDeviceRequestDTOs(final List<DeviceRequestDTO> dtoList) {
		final List<DeviceRequestDTO> normalized = new ArrayList<>(dtoList.size());
		for (final DeviceRequestDTO device : dtoList) {
			normalized.add(new DeviceRequestDTO(
					device.name().trim(),
					device.metadata(),
					Utilities.isEmpty(device.addresses()) ? new ArrayList<>()
							: device.addresses().stream()
									.map(a -> new AddressDTO(a.type().trim(), addressNormalizator.normalize(a.address())))
									.collect(Collectors.toList())));
		}
		return normalized;
	}
}
