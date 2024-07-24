package eu.arrowhead.serviceregistry.jpa.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.SystemRequestDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.jpa.entity.SystemAddress;
import eu.arrowhead.serviceregistry.jpa.repository.DeviceRepository;
import eu.arrowhead.serviceregistry.jpa.repository.DeviceSystemConnectorRepository;
import eu.arrowhead.serviceregistry.jpa.repository.SystemAddressRepository;
import eu.arrowhead.serviceregistry.jpa.repository.SystemRepository;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceSystemConnector;
import eu.arrowhead.serviceregistry.jpa.entity.System;

@Service
public class SystemDbService {

	//=================================================================================================
	// members
	
	@Autowired
	private SystemRepository systemRepo;
	
	@Autowired
	private SystemAddressRepository systemAddressRepo;
	
	@Autowired
	private DeviceRepository deviceRepo;
	
	@Autowired 
	private DeviceSystemConnectorRepository deviceSystemConnectorRepo;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public List<Entry<System, List<SystemAddress>>> createBulk(final List<SystemRequestDTO> candidates) {
		
		logger.debug("createBulk started");
		Assert.isTrue(!Utilities.isEmpty(candidates), "system candidate list is empty");
		
		try {
			checkSystemNames(candidates); //checks if none of the system names exist (system name has to be unique)
			
			checkDeviceNames(candidates); //checks if device names already exist
			
			//writing the system entities to the database
			List<System> systemEntities = createSystemEntities(candidates);
			systemEntities = systemRepo.saveAllAndFlush(systemEntities);
			
			//writing the system address entities to the database
			final List<SystemAddress> systemAddressEntities = createSystemAddressEntities(candidates, systemEntities);
			systemAddressRepo.saveAllAndFlush(systemAddressEntities);
			
			//writing the devices-system connections to the database
			final List<DeviceSystemConnector> deviceSystemConnectorEntities = createDeviceSystemConnectorEntities(candidates);
			deviceSystemConnectorRepo.saveAllAndFlush(deviceSystemConnectorEntities);
			
			//returning the entries of the created entities
			final List<Entry<System, List<SystemAddress>>> result = new ArrayList<>(systemEntities.size());
			systemEntities.stream().map(s -> result.add(Map.entry(s, systemAddressRepo.findAllBySystem(s))));
			return result;
			
		} catch (final InvalidParameterException e) {
			throw e;
		} catch (final Exception e) {
			logger.error(e.getMessage());
			logger.debug(e);
			throw new InternalServerError("Database operation error");
		}
		
	}
	
	//=================================================================================================
	// assistant methods
	
	//-------------------------------------------------------------------------------------------------
	//checks if system name already exists, throws exception if it does
	private void checkSystemNames(final List<SystemRequestDTO> candidates) {
		
		final List<String> candidateNames = candidates.stream()
				.map(c -> c.name())
				.collect(Collectors.toList());
		
		final List<System> existingSystems = systemRepo.findAllByNameIn(candidateNames);
		
		if (!Utilities.isEmpty(existingSystems)) {
			final String existingSystemNames = existingSystems.stream()
					.map(e -> e.getName())
					.collect(Collectors.joining(", "));
			throw new InvalidParameterException("System names already exists: " + existingSystemNames);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	//checks if device name already exists, throws exception if it does NOT
	private void checkDeviceNames(final List<SystemRequestDTO> candidates) {
		final List<String> candidateDeviceNames = candidates.stream()
				.map(c -> c.deviceName())
				.collect(Collectors.toList())
				.stream()
				.distinct()
				.collect(Collectors.toList());
		
		final List<String> existingDeviceNames = deviceRepo.findAllByNameIn(candidateDeviceNames).stream()
				.map(e -> e.getName())
				.collect(Collectors.toList());
		
		final List<String> notExistingDeviceNames = new ArrayList<>();
		for (final String candidateDeviceName : candidateDeviceNames) {
			if (!existingDeviceNames.contains(candidateDeviceName)) {
				notExistingDeviceNames.add(candidateDeviceName);
			}
		}
		if (notExistingDeviceNames.size()!=0) {
			throw new InvalidParameterException("Device names do not exist: " + notExistingDeviceNames.stream()
				.collect(Collectors.joining(", ")));
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	private List<System> createSystemEntities(final List<SystemRequestDTO> candidates) {
		return candidates.stream()
		.map(c -> new System(c.name(), Utilities.toJson(c.metadata()), c.version()))
		.collect(Collectors.toList());
	}
	
	//-------------------------------------------------------------------------------------------------
	private final List<SystemAddress> createSystemAddressEntities(final List<SystemRequestDTO> candidates, final List<System> systemEntities) {
		final List<SystemAddress> systemAddressEntities = new ArrayList<>();
		for (final SystemRequestDTO candidate : candidates) {
			if (!Utilities.isEmpty(candidate.addresses())) {
				final System system = systemEntities.stream()
						.filter(s -> s.getName().equals(candidate.name()))
						.findFirst()
						.get();
				
				final List<SystemAddress> systemAddresses = candidate.addresses().stream()
						.map(a -> new SystemAddress(
								system, 
								AddressType.valueOf(a.type()),
								a.address()))
						.collect(Collectors.toList());
				
				systemAddressEntities.addAll(systemAddresses);
			}
		}
		
		return systemAddressEntities;
	}
	
	//-------------------------------------------------------------------------------------------------
	private final List<DeviceSystemConnector> createDeviceSystemConnectorEntities(final List<SystemRequestDTO> candidates) {
		return candidates.stream()
				.map(c -> new DeviceSystemConnector(
						deviceRepo.findAllByNameIn(Arrays.asList(c.deviceName())).getFirst(),
						systemRepo.findAllByNameIn(Arrays.asList(c.name())).getFirst()
						))
				.collect(Collectors.toList());
	}

}
