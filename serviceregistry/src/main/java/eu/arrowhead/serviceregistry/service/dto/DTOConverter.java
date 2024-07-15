package eu.arrowhead.serviceregistry.service.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
	public DeviceListResponseDTO convertDeviceAddressEntityListToDTO(final List<DeviceAddress> entities) {
		logger.debug("convertDeviceAddressEntityListToDTO started...");
		Assert.isTrue(!Utilities.isEmpty(entities), "entity list is empty");

		final List<DeviceResponseDTO> entries = new ArrayList<>();
		entities.forEach(da -> {
			final Optional<DeviceResponseDTO> optional = entries.stream()
					.filter(e -> e.name().equals(da.getDevice().getName()))
					.findFirst();

			final DeviceResponseDTO dto = optional.isPresent() ? optional.get() : convertDeviceEntityToDeviceResponseDTO(da.getDevice());
			if (optional.isEmpty()) {
				entries.add(dto);
			}

			dto.addresses().add(new AddressDTO(da.getAddressType().name(), da.getAddress()));
		});

		return new DeviceListResponseDTO(entries, entries.size());
	}

	//-------------------------------------------------------------------------------------------------
	public DeviceResponseDTO convertDeviceEntityToDeviceResponseDTO(final Device entity) {
		logger.debug("convertDeviceAddressEntityListToDTO started...");
		Assert.notNull(entity, "device entity is null");

		return new DeviceResponseDTO(
				entity.getName(),
				Utilities.fromJson(entity.getMetadata(), new TypeReference<Map<String, Object>>() { }),
				new ArrayList<>(),
				Utilities.convertZonedDateTimeToUTCString(entity.getCreatedAt()),
				Utilities.convertZonedDateTimeToUTCString(entity.getUpdatedAt()));
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
