package eu.arrowhead.serviceregistry.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.DeviceListRequestDTO;
import eu.arrowhead.dto.DeviceListResponseDTO;
import eu.arrowhead.dto.DeviceRequestDTO;
import eu.arrowhead.dto.ServiceDefinitionListRequestDTO;
import eu.arrowhead.dto.ServiceDefinitionListResponseDTO;
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

		validator.validateCreateDevice(dto, origin);

		final List<DeviceRequestDTO> normalized = new ArrayList<>(dto.devices().size());
		for (final DeviceRequestDTO device : dto.devices()) {
			normalized.add(new DeviceRequestDTO(
					device.name().trim(),
					device.metadata(),
					Utilities.isEmpty(device.addresses()) ? new ArrayList<>()
							: device.addresses().stream()
									.map(a -> new AddressDTO(a.type().trim(), addressNormalizator.normalize(a.address())))
									.collect(Collectors.toList())));

			normalized.getLast().addresses().forEach(address -> validator.validateNormalizedAddress(address, origin));
		}

		try {
			final Map<Device, List<DeviceAddress>> entities = deviceDbService.createBulk(normalized);
			return dtoConverter.convertDeviceAddressEntityMapToDTO(entities);

		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);

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

}
