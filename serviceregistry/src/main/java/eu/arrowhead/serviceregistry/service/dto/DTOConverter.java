package eu.arrowhead.serviceregistry.service.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.type.TypeReference;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.DeviceListResponseDTO;
import eu.arrowhead.dto.DeviceResponseDTO;
import eu.arrowhead.dto.KeyValuesDTO;
import eu.arrowhead.dto.ServiceDefinitionListResponseDTO;
import eu.arrowhead.dto.ServiceDefinitionResponseDTO;
import eu.arrowhead.dto.SystemListResponseDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.serviceregistry.jpa.entity.Device;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceAddress;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceDefinition;
import eu.arrowhead.serviceregistry.jpa.entity.SystemAddress;
import eu.arrowhead.serviceregistry.jpa.entity.System;

@Service
public class DTOConverter {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public DeviceListResponseDTO convertDeviceAndDeviceAddressEntriesToDTO(final Iterable<Entry<Device, List<DeviceAddress>>> entries, final long count) {
		logger.debug("convertDeviceAndDeviceAddressEntriesToDTO started...");
		Assert.notNull(entries, "entry list is null");

		final List<DeviceResponseDTO> dtos = new ArrayList<>();
		entries.forEach(entry -> dtos.add(convertDeviceEntityToDeviceResponseDTO(entry.getKey(), entry.getValue())));

		return new DeviceListResponseDTO(dtos, count);
	}

	//-------------------------------------------------------------------------------------------------
	public DeviceResponseDTO convertDeviceEntityToDeviceResponseDTO(final Device deviceEntity, final List<DeviceAddress> addressEntities) {
		logger.debug("convertDeviceAddressEntityListToDTO started...");
		Assert.notNull(deviceEntity, "device entity is null");

		return new DeviceResponseDTO(
				deviceEntity.getName(),
				Utilities.fromJson(deviceEntity.getMetadata(), new TypeReference<Map<String, Object>>() { }),
				Utilities.isEmpty(addressEntities) ? null
						: addressEntities.stream()
								.map(address -> new AddressDTO(address.getAddressType().name(), address.getAddress()))
								.collect(Collectors.toList()),
				Utilities.convertZonedDateTimeToUTCString(deviceEntity.getCreatedAt()),
				Utilities.convertZonedDateTimeToUTCString(deviceEntity.getUpdatedAt()));
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceDefinitionListResponseDTO convertServiceDefinitionEntityListToDTO(final List<ServiceDefinition> entities) {
		logger.debug("convertServiceDefinitionEntityListToDTO started...");

		final List<ServiceDefinitionResponseDTO> converted = entities.stream()
				.map(e -> convertServiceDefinitionEntityToDTO(e))
				.collect(Collectors.toList());
		return new ServiceDefinitionListResponseDTO(converted, converted.size());
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceDefinitionListResponseDTO convertServiceDefinitionEntityPageToDTO(final Page<ServiceDefinition> entities) {
		logger.debug("convertServiceDefinitionEntityPageToDTO started...");

		final List<ServiceDefinitionResponseDTO> converted = entities.stream()
				.map(e -> convertServiceDefinitionEntityToDTO(e))
				.collect(Collectors.toList());
		return new ServiceDefinitionListResponseDTO(converted, entities.getTotalElements());
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceDefinitionResponseDTO convertServiceDefinitionEntityToDTO(final ServiceDefinition entity) {
		logger.debug("convertServiceDefinitionEntityToDTO started...");
		Assert.notNull(entity, "entity is null");
		Assert.isTrue(!Utilities.isEmpty(entity.getName()), "name is empty");

		return new ServiceDefinitionResponseDTO(entity.getName(), Utilities.convertZonedDateTimeToUTCString(entity.getCreatedAt()), Utilities.convertZonedDateTimeToUTCString(entity.getUpdatedAt()));
	}

	//-------------------------------------------------------------------------------------------------
	public SystemListResponseDTO convertSystemTripletPageToDTO(final Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> entities) {
		logger.debug("convertSystemTripletPageToDTO started...");

		final List<SystemResponseDTO> result = new ArrayList<>(entities.getNumberOfElements());

		for (final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> entity : entities) {
			result.add(convertSystemTripletToDTO(entity));
		}

		return new SystemListResponseDTO(result, entities.getTotalElements());
	}

	//-------------------------------------------------------------------------------------------------
	public SystemListResponseDTO convertSystemTripletListToDTO(final List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> entities) {
		logger.debug("convertSystemTripletListToDTO started...");

		final List<SystemResponseDTO> result = new ArrayList<>(entities.size());

		for (final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> entity : entities) {
			result.add(convertSystemTripletToDTO(entity));
		}

		return new SystemListResponseDTO(result, result.size());
	}

	//-------------------------------------------------------------------------------------------------
	public SystemResponseDTO convertSystemTripletToDTO(final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> entity) {
		Assert.notNull(entity.getLeft(), "the System in the triple is null");
		Assert.isTrue(!Utilities.isEmpty(entity.getMiddle()), "the address list in the triple is null");


		final System system = entity.getLeft();
		final List<SystemAddress> systemAddressList = entity.getMiddle();
		final Device device = entity.getRight() == null ? null : entity.getRight().getKey();
		final List<DeviceAddress> deviceAddresses = entity.getRight() == null ? null : entity.getRight().getValue();

		return new SystemResponseDTO(
				system.getName(),
				Utilities.fromJson(system.getMetadata(), new TypeReference<Map<String, Object>>() { }),
				system.getVersion(),
				systemAddressList
					.stream()
					.map(a -> new AddressDTO(a.getAddressType().toString(), a.getAddress()))
					.collect(Collectors.toList()),
				device == null ? null : new DeviceResponseDTO(
						device.getName(),
						Utilities.fromJson(device.getMetadata(), new TypeReference<Map<String, Object>>() { }),
						deviceAddresses.stream().map(a -> new AddressDTO(a.getAddressType().toString(), a.getAddress())).collect(Collectors.toList()),
						Utilities.convertZonedDateTimeToUTCString(device.getCreatedAt()),
						Utilities.convertZonedDateTimeToUTCString(device.getUpdatedAt())),
				Utilities.convertZonedDateTimeToUTCString(system.getCreatedAt()),
				Utilities.convertZonedDateTimeToUTCString(system.getUpdatedAt()));
	}

	//-------------------------------------------------------------------------------------------------
	public SystemListResponseDTO convertSystemListResponseDtoToTerse(final SystemListResponseDTO verbose) {

		final List<SystemResponseDTO> terse = new ArrayList<SystemResponseDTO>(verbose.entries().size());

		for (final SystemResponseDTO systemResponseDTO : verbose.entries()) {

			DeviceResponseDTO device = null;
			if (systemResponseDTO.device() != null) {
				device = new DeviceResponseDTO(systemResponseDTO.device().name(), null, null, null, null);
			}

			terse.add(new SystemResponseDTO(
					systemResponseDTO.name(),
					systemResponseDTO.metadata(),
					systemResponseDTO.version(),
					systemResponseDTO.addresses(),
					device,
					systemResponseDTO.createdAt(),
					systemResponseDTO.updatedAt()
					));
		}

		return new SystemListResponseDTO(terse, verbose.count());

	}

	//-------------------------------------------------------------------------------------------------
	public KeyValuesDTO convertConfigMapToDTO(final Map<String, String> map) {
		return new KeyValuesDTO(map);
	}

	//=================================================================================================
	// assistant methods
}
