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
	
	//TODO: atgondolni hogy normalis dolog-e konvertalas miatt adatbazismuveleteket vegezni... (szerintem igen(???))
	@Autowired 
	private DeviceSystemConnectorRepository deviceSystemConnectorRepo;
	
	@Autowired 
	private DeviceAddressRepository deviceAddressRepo;

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
	public SystemListResponseDTO convertSystemEntriesToDTO(final Iterable<Entry<System, List<SystemAddress>>> entries) {
		
		logger.debug("convertSystemEntriesToDTO started");
		Assert.notNull(entries, "entity are null");
		
		long size = 0; //size of the system list
		List<SystemResponseDTO> data = new ArrayList<>(); //system list
		for (Map.Entry<System, List<SystemAddress>> entry : entries) {
			
			System system = entry.getKey();
			
			//converting the system addresses to dtos
			List<SystemAddress> systemAddresses = entry.getValue();
			List<AddressDTO> addressDTOs = new ArrayList<>(systemAddresses.size());
			systemAddresses.forEach(sa -> addressDTOs.add(new AddressDTO(sa.getAddressType().toString(), sa.getAddress())));
			
			//convetring the device to dto
			Optional<Device> device = deviceSystemConnectorRepo.findBySystem(system);
			DeviceResponseDTO deviceDTO = new DeviceResponseDTO(
					device.get().getName(),
					Utilities.fromJson(device.get().getMetadata(), new TypeReference<Map<String, Object>>() { }),
					deviceAddressRepo.findAllByDevice(device.get()).stream().map(d -> new AddressDTO(d.getAddressType().toString(), d.getAddress())).collect(Collectors.toList()),
					device.get().getCreatedAt().toString(),
					device.get().getUpdatedAt().toString()
					);
			
			//creating the system dto
			data.add(new SystemResponseDTO(
					system.getName(), 
					Utilities.fromJson(system.getMetadata(), new TypeReference<Map<String, Object>>() { }),
					system.getVersion(), 
					addressDTOs, 
					deviceDTO, 
					system.getCreatedAt().toString(), 
					system.getUpdatedAt().toString()));
			size++;
		}
		
		return new SystemListResponseDTO(data, size);
		
	}
}
