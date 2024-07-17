package eu.arrowhead.serviceregistry.service.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.type.TypeReference;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.DeviceListResponseDTO;
import eu.arrowhead.dto.DeviceResponseDTO;
import eu.arrowhead.dto.ServiceDefinitionListResponseDTO;
import eu.arrowhead.dto.ServiceDefinitionResponseDTO;
import eu.arrowhead.serviceregistry.jpa.entity.Device;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceAddress;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceDefinition;

@Service
public class DTOConverter {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public DeviceListResponseDTO convertDeviceAddressEntriesToDTO(final Iterable<Entry<Device, List<DeviceAddress>>> entries, final long count) {
		logger.debug("convertDeviceAddressEntityListToDTO started...");
		Assert.notNull(entries, "entry list is null");

		final List<DeviceResponseDTO> dtos = new ArrayList<>();
		entries.forEach(entry -> dtos.add(convertDeviceEntityToDeviceResponseDTO(entry.getKey(), entry.getValue())));

		return new DeviceListResponseDTO(dtos, count);
	}

	//-------------------------------------------------------------------------------------------------
	public DeviceResponseDTO convertDeviceEntityToDeviceResponseDTO(final Device deviceEntitiy, final List<DeviceAddress> addressEntities) {
		logger.debug("convertDeviceAddressEntityListToDTO started...");
		Assert.notNull(deviceEntitiy, "device entity is null");

		return new DeviceResponseDTO(
				deviceEntitiy.getName(),
				Utilities.fromJson(deviceEntitiy.getMetadata(), new TypeReference<Map<String, Object>>() { }),
				Utilities.isEmpty(addressEntities) ? null
						: addressEntities.stream()
								.map(address -> new AddressDTO(address.getAddressType().name(), address.getAddress()))
								.collect(Collectors.toList()),
				Utilities.convertZonedDateTimeToUTCString(deviceEntitiy.getCreatedAt()),
				Utilities.convertZonedDateTimeToUTCString(deviceEntitiy.getUpdatedAt()));
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceDefinitionListResponseDTO convertServiceDefinitionEntityListToDTO(final List<ServiceDefinition> entities) {
		logger.debug("convertServiceDefinitionEntityListToDTO started...");
		Assert.isTrue(!Utilities.isEmpty(entities), "entity list is empty");

		final List<ServiceDefinitionResponseDTO> converted = entities.stream()
				.map(e -> convertServiceDefinitionEntityToDTO(e))
				.collect(Collectors.toList());
		return new ServiceDefinitionListResponseDTO(converted, converted.size());
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceDefinitionResponseDTO convertServiceDefinitionEntityToDTO(final ServiceDefinition entity) {
		logger.debug("convertServiceDefinitionEntityToDTO started...");
		Assert.notNull(entity, "entity is null");
		Assert.isTrue(!Utilities.isEmpty(entity.getName()), "name is empty");

		return new ServiceDefinitionResponseDTO(entity.getName(), Utilities.convertZonedDateTimeToUTCString(entity.getCreatedAt()), Utilities.convertZonedDateTimeToUTCString(entity.getUpdatedAt()));
	}
}
