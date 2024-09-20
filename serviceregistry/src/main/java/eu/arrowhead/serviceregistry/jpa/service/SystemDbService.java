package eu.arrowhead.serviceregistry.jpa.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.type.TypeReference;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.MetadataRequirementsMatcher;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.SystemRequestDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.jpa.entity.SystemAddress;
import eu.arrowhead.serviceregistry.jpa.repository.DeviceAddressRepository;
import eu.arrowhead.serviceregistry.jpa.repository.DeviceRepository;
import eu.arrowhead.serviceregistry.jpa.repository.DeviceSystemConnectorRepository;
import eu.arrowhead.serviceregistry.jpa.repository.SystemAddressRepository;
import eu.arrowhead.serviceregistry.jpa.repository.SystemRepository;
import eu.arrowhead.serviceregistry.jpa.entity.Device;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceAddress;
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
	private DeviceAddressRepository deviceAddressRepo;

	@Autowired
	private DeviceRepository deviceRepo;

	@Autowired
	private DeviceSystemConnectorRepository deviceSystemConnectorRepo;

	private static final Object LOCK = new Object();

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> createBulk(final List<SystemRequestDTO> candidates) {

		logger.debug("createBulk started");
		Assert.isTrue(!Utilities.isEmpty(candidates), "system candidate list is empty");

		try {
			checkSystemNamesNotExist(candidates); //checks if none of the system names exist (system name has to be unique)

			checkDeviceNamesExist(candidates); //checks if device names already exist

			//writing the system entities to the database
			List<System> systemEntities = createSystemEntities(candidates);
			systemEntities = systemRepo.saveAllAndFlush(systemEntities);

			//writing the system address entities to the database
			final List<SystemAddress> systemAddressEntities = createSystemAddressEntities(candidates, systemEntities);
			systemAddressRepo.saveAllAndFlush(systemAddressEntities);

			//writing the device-system connections to the database
			final List<DeviceSystemConnector> deviceSystemConnectorEntities = createDeviceSystemConnectorEntities(candidates);
			if (deviceSystemConnectorEntities != null) {
				deviceSystemConnectorRepo.saveAllAndFlush(deviceSystemConnectorEntities);
			}

			return createTriples(systemEntities, systemAddressEntities, deviceSystemConnectorEntities);

		} catch (final InvalidParameterException ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}

	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> updateBulk(final List<SystemRequestDTO> toUpdate) {
		logger.debug("updateBulk started");
		Assert.isTrue(!Utilities.isEmpty(toUpdate), "The list of systems to update is empty or missing.");

		try {

			synchronized (LOCK) {

				checkSystemNamesExist(toUpdate); // system name can't be changed

				checkDeviceNamesExist(toUpdate); // checks if device names already exist

				//writing the updated system entities to the database
				List<System> systemEntities = systemRepo.findAllByNameIn(toUpdate.stream().map(s -> s.name()).collect(Collectors.toList()));

				for (final System systemEntity : systemEntities) {

					final SystemRequestDTO dto = toUpdate.stream()
							.filter(s -> s.name().equals(systemEntity.getName()))
							.findFirst()
							.get();

					systemEntity.setMetadata(Utilities.toJson(dto.metadata()));

					systemEntity.setVersion(dto.version());
				}

				systemEntities = systemRepo.saveAllAndFlush(systemEntities);

				//removing the old system addresses from the database
				systemAddressRepo.deleteAllBySystemIn(systemEntities);

				//writing the new system address entities to the database
				final List<SystemAddress> systemAddressEntities = createSystemAddressEntities(toUpdate, systemEntities);
				systemAddressRepo.saveAllAndFlush(systemAddressEntities);

				//updating the old device-system connections
				final List<DeviceSystemConnector> connectionsToUpdate = new ArrayList<>();
				final List<DeviceSystemConnector> connectionsToDelete = new ArrayList<>();

				for (final SystemRequestDTO dto : toUpdate) {
					final System system = systemEntities.stream().filter(s -> s.getName().equals(dto.name())).findFirst().get();
					final Optional<DeviceSystemConnector> connection = deviceSystemConnectorRepo.findBySystem(system);
					if (connection.isPresent()) {
						if (dto.deviceName() == null) {
							connectionsToDelete.add(connection.get());
						} else {
							connectionsToUpdate.add(connection.get());
						}
					} else {
						// if the device is not null, we have to create a new connection
						if (dto.deviceName() != null) {
							connectionsToUpdate.add(new DeviceSystemConnector(null, system)); // we set the device later
						}
					}
				}

				for (final DeviceSystemConnector connection : connectionsToUpdate) {
					final String deviceName = toUpdate.stream()
							.filter(s -> s.name().equals(connection.getSystem().getName()))
							.findFirst()
							.get()
							.deviceName();
					final Device device = deviceRepo.findByName(deviceName).get();

					connection.setDevice(device);
				}

				//writing the changes into the database
				deviceSystemConnectorRepo.deleteAll(connectionsToDelete);
				deviceSystemConnectorRepo.saveAll(connectionsToUpdate);
				deviceSystemConnectorRepo.flush();

				return createTriples(systemEntities, systemAddressEntities, connectionsToUpdate);
			}

		} catch (final InvalidParameterException ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public void deleteByNameList(final List<String> names) {
		logger.debug("deleteByNameList started");
		Assert.isTrue(!Utilities.isEmpty(names), "system name list is missing or empty");

		try {
			final List<System> entries = systemRepo.findAllByNameIn(names);
			systemRepo.deleteAll(entries);
			systemRepo.flush();

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public Optional<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> getByName(final String name) {
		logger.debug("getByName started");
		Assert.isTrue(!Utilities.isEmpty(name), "system name is missing or empty");

		synchronized (LOCK) {

			// system
			final Optional<System> system = systemRepo.findByName(name);
			if (system.isEmpty()) {
				return Optional.empty();
			}

			// system addresses
			final List<SystemAddress> systemAddresses = systemAddressRepo.findAllBySystem(system.get());

			// device
			final Optional<DeviceSystemConnector> deviceSystemConnection = deviceSystemConnectorRepo.findBySystem(system.get());
			final Device device = deviceSystemConnection.isEmpty() ? null : deviceSystemConnection.get().getDevice();

			// device addresses
			final List<DeviceAddress> deviceAddresses = device == null ? new ArrayList<DeviceAddress>() : deviceAddressRepo.findAllByDevice(device);

			return Optional.of(Triple.of(
					system.get(),
					systemAddresses,
					device == null ? null : Map.entry(device, deviceAddresses)));
		}

	}

	//-------------------------------------------------------------------------------------------------
	public Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> getPageByFilters(
			final PageRequest pagination, final List<String> systemNames, final List<String> addresses, final AddressType addressType,
			final List<MetadataRequirementDTO> metadataRequirementList, final List<String> versions, final List<String> deviceNames) {
		logger.debug("getPageByFilters started");
		Assert.notNull(pagination, "page is null");

		synchronized (LOCK) {
			try {
				Page<System> systemEntries = new PageImpl<System>(new ArrayList<System>());

				// without filter
				if (Utilities.isEmpty(systemNames)
						&& Utilities.isEmpty(addresses)
						&& addressType == null
						&& Utilities.isEmpty(metadataRequirementList)
						&& Utilities.isEmpty(versions)
						&& Utilities.isEmpty(deviceNames)) {

					systemEntries = systemRepo.findAll(pagination);
				// with filter
				} else {

					final List<String> matchings = new ArrayList<>();

						final List<System> toFilter = Utilities.isEmpty(systemNames) ? systemRepo.findAll() : systemRepo.findAllByNameIn(systemNames);

						for (final System system : toFilter) {

							// address type
							if (addressType != null && Utilities.isEmpty(systemAddressRepo.findAllBySystemAndAddressType(system, addressType))) {
								continue;
							}

							// addresses
							if (!Utilities.isEmpty(addresses) && Utilities.isEmpty(systemAddressRepo.findAllBySystemAndAddressIn(system, addresses))) {
								continue;
							}

							// version
							if (!Utilities.isEmpty(versions) && !systemRepo.findAllByVersionIn(versions).contains(system)) {
								continue;
							}

							// device names
							if (!Utilities.isEmpty(deviceNames) && deviceSystemConnectorRepo.findBySystem(system).isPresent()
									&& !deviceNames.contains(deviceSystemConnectorRepo.findBySystem(system).get().getDevice().getName())) {
								continue;
							}

							// metadata
							if (!Utilities.isEmpty(metadataRequirementList)) {
								boolean metaDataMatch = false;
								for (final MetadataRequirementDTO requirement : metadataRequirementList) {
									if (MetadataRequirementsMatcher.isMetadataMatch(Utilities.fromJson(system.getMetadata(), new TypeReference<Map<String, Object>>() { }), requirement)) {
										metaDataMatch = true;
										break;
									}
								}
								if (!metaDataMatch) {
									continue;
								}
							}
							matchings.add(system.getName());
						}

					systemEntries = systemRepo.findAllByNameIn(matchings, pagination);
				}

				// system addresses
				final List<SystemAddress> systemAddresses = systemAddressRepo.findAllBySystemIn(systemEntries.toList());

				// device-system connections
				final List<DeviceSystemConnector> deviceSystemConnections = deviceSystemConnectorRepo.findBySystemIn(systemEntries.toList());

				final List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> result = createTriples(systemEntries.toList(), systemAddresses, deviceSystemConnections);

				return new PageImpl<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>>(
					result,
					pagination,
					systemEntries.getTotalElements());

			} catch (final Exception ex) {
				logger.error(ex.getMessage());
				logger.debug(ex);
				throw new InternalServerError("Database operation error");
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public boolean deleteByName(final String name) {
		logger.debug("deleteByName started");
		Assert.isTrue(!Utilities.isEmpty(name), "system name is empty");

		try {
			final Optional<System> optional = systemRepo.findByName(name);
			if (optional.isPresent()) {
				systemRepo.delete(optional.get());
				systemRepo.flush();
				return true;
			}

			return false;

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	//checks if system name already exists, throws exception if it does
	private void checkSystemNamesNotExist(final List<SystemRequestDTO> candidates) {

		final List<String> candidateNames = candidates.stream()
				.map(c -> c.name())
				.collect(Collectors.toList());

		final List<System> existingSystems = systemRepo.findAllByNameIn(candidateNames);

		if (!Utilities.isEmpty(existingSystems)) {
			final String existingSystemNames = existingSystems.stream()
					.map(e -> e.getName())
					.collect(Collectors.joining(", "));
			throw new InvalidParameterException("Systems with names already exist: " + existingSystemNames);
		}
	}

	//-------------------------------------------------------------------------------------------------
	//checks if system name already exists, throws exception if it does not
	private void checkSystemNamesExist(final List<SystemRequestDTO> requested) {

		final List<String> requestedNames = requested.stream()
				.map(s -> s.name())
				.collect(Collectors.toList());

		final List<System> givenSystems = systemRepo.findAllByNameIn(requestedNames);

		if (requestedNames.size() > givenSystems.size()) {

			final List<String> existingNames = givenSystems.stream()
					.map(s -> s.getName())
					.collect(Collectors.toList());
			final List<String> notExistingNames = new ArrayList<String>(requestedNames.size() - existingNames.size());
			for (final String name : requestedNames) {
				if (!existingNames.contains(name)) {
					notExistingNames.add(name);
				}
			}
			throw new InvalidParameterException("System(s) not exists: " + notExistingNames.stream().collect(Collectors.joining(", ")));
		}
	}

	//-------------------------------------------------------------------------------------------------
	//checks if device name already exists, throws exception if it does not
	private void checkDeviceNamesExist(final List<SystemRequestDTO> candidates) {

		final List<String> candidateDeviceNames = new ArrayList<String>();

		for (final SystemRequestDTO candidate : candidates) {
			if (candidate.deviceName() != null) {
				candidateDeviceNames.add(candidate.deviceName());
			}
		}

		final List<String> existingDeviceNames = deviceRepo.findAllByNameIn(candidateDeviceNames).stream()
				.map(e -> e.getName())
				.collect(Collectors.toList());

		final List<String> notExistingDeviceNames = new ArrayList<>();
		for (final String candidateDeviceName : candidateDeviceNames) {
			if (!existingDeviceNames.contains(candidateDeviceName)) {
				notExistingDeviceNames.add(candidateDeviceName);
			}
		}
		if (!notExistingDeviceNames.isEmpty()) {
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
		synchronized (LOCK) {

			final List<DeviceSystemConnector> connections = new ArrayList<>();

			for (final SystemRequestDTO candidate : candidates) {
				if (candidate.deviceName() != null) {
					final Device device = deviceRepo.findByName(candidate.deviceName()).get();
					final System system = systemRepo.findByName(candidate.name()).get();
					connections.add(new DeviceSystemConnector(device, system));
				}
			}

			return connections;
		}
	}

	//-------------------------------------------------------------------------------------------------
	private List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> createTriples(
			final List<System> systems, final List<SystemAddress> addresses, final List<DeviceSystemConnector> deviceConnections) {

		Assert.notNull(systems, "systems are null");

		final List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> result = new ArrayList<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>>();

		for (final System system : systems) {

			// corresponding addresses
			final List<SystemAddress> systemAddresses = addresses
					.stream()
					.filter(a -> a.getSystem().equals(system))
					.collect(Collectors.toList());

			// corresponding device
			final Optional<DeviceSystemConnector> connection = deviceConnections
					.stream()
					.filter(c -> c.getSystem().equals(system))
					.findFirst();

			final Device device = connection.isEmpty() ? null : connection.get().getDevice();

			// corresponding device addresses
			final List<DeviceAddress> deviceAddressList = device == null ? new ArrayList<DeviceAddress>() : deviceAddressRepo.findAllByDevice(device);

			result.add(Triple.of(system, systemAddresses, device == null ? null : Map.entry(device, deviceAddressList)));
		}

		return result;
	}
}
