package eu.arrowhead.serviceregistry.service.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.type.TypeReference;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.DeviceResponseDTO;
import eu.arrowhead.dto.ServiceDefinitionListResponseDTO;
import eu.arrowhead.dto.ServiceDefinitionResponseDTO;
import eu.arrowhead.dto.SystemListResponseDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.serviceregistry.jpa.entity.Device;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceAddress;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceDefinition;
import eu.arrowhead.serviceregistry.jpa.entity.System;
import eu.arrowhead.serviceregistry.jpa.entity.SystemAddress;
import eu.arrowhead.serviceregistry.jpa.repository.DeviceAddressRepository;
import eu.arrowhead.serviceregistry.jpa.repository.DeviceSystemConnectorRepository;

@Service
public class DTOConverter {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());
	
	/*@Autowired 
	private DeviceSystemConnectorRepository deviceSystemConnectorRepo;
	
	@Autowired 
	private DeviceAddressRepository deviceAddressRepo;*/

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public ServiceDefinitionListResponseDTO convertServiceDefinitionEntityListToDTO(final List<ServiceDefinition> entities) {
		logger.debug("convertServiceDefinitionEntityListToDTO");
		Assert.isTrue(!Utilities.isEmpty(entities), "entity list is empty");

		final List<ServiceDefinitionResponseDTO> converted = entities.stream()
				.map(e -> convertServiceDefinitionEntityToDTO(e))
				.collect(Collectors.toList());
		return new ServiceDefinitionListResponseDTO(converted, converted.size());
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceDefinitionResponseDTO convertServiceDefinitionEntityToDTO(final ServiceDefinition entity) {
		logger.debug("convertServiceDefinitionEntityToDTO started");
		Assert.notNull(entity, "entity is null");
		Assert.isTrue(!Utilities.isEmpty(entity.getName()), "name is empty");

		return new ServiceDefinitionResponseDTO(entity.getName(), Utilities.convertZonedDateTimeToUTCString(entity.getCreatedAt()), Utilities.convertZonedDateTimeToUTCString(entity.getUpdatedAt()));
	}
	
	//-------------------------------------------------------------------------------------------------
	/*public SystemListResponseDTO convertSystemEntriesToDTO(final Iterable<Entry<System, List<SystemAddress>>> entries) {
		
		logger.debug("convertSystemEntriesToDTO started");
		Assert.notNull(entries, "entity are null");
		
		long size = 0; //size of the system list
		List<SystemResponseDTO> data = new ArrayList<>(); //system list
		for (Map.Entry<System, List<SystemAddress>> entry : entries) {
			
			System system = entry.getKey();
			
			//converting the system addresses to dtos
			List<SystemAddress> systemAddresses = entry.getValue();
			List<AddressDTO> systemAddressDTOs = new ArrayList<>(systemAddresses.size());
			systemAddresses.forEach(sa -> systemAddressDTOs.add(new AddressDTO(sa.getAddressType().toString(), sa.getAddress())));
			
			//convetring the device to dto
			Device device = deviceSystemConnectorRepo.findBySystem(system).get().getDevice();
			
			List<DeviceAddress> deviceAddresses = deviceAddressRepo.findAllByDevice(device);
			List<AddressDTO> deviceAddressDTOs = new ArrayList<>(deviceAddresses.size());
			deviceAddresses.forEach(da -> deviceAddressDTOs.add(new AddressDTO(da.getAddressType().toString(), da.getAddress())));
			
			DeviceResponseDTO deviceDTO = new DeviceResponseDTO(
					device.getName(),
					Utilities.fromJson(device.getMetadata(), new TypeReference<Map<String, Object>>() { }),
					deviceAddressDTOs,
					Utilities.convertZonedDateTimeToUTCString(device.getCreatedAt()),
					Utilities.convertZonedDateTimeToUTCString(device.getUpdatedAt())
					);
			
			//creating the system dto
			data.add(new SystemResponseDTO(
					system.getName(), 
					Utilities.fromJson(system.getMetadata(), new TypeReference<Map<String, Object>>() { }),
					system.getVersion(), 
					systemAddressDTOs, 
					deviceDTO, 
					Utilities.convertZonedDateTimeToUTCString(system.getCreatedAt()), 
					Utilities.convertZonedDateTimeToUTCString(system.getUpdatedAt())));
			size++;
		}
		
		return new SystemListResponseDTO(data, size);
		
	}*/
}
