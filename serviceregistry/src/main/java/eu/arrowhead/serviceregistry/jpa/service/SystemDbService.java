package eu.arrowhead.serviceregistry.jpa.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.type.TypeReference;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.PageService;
import eu.arrowhead.common.service.validation.MetadataRequirementsMatcher;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.DeviceResponseDTO;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.SystemQueryRequestDTO;
import eu.arrowhead.dto.SystemRequestDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.jpa.entity.SystemAddress;
import eu.arrowhead.serviceregistry.jpa.repository.DeviceAddressRepository;
import eu.arrowhead.serviceregistry.jpa.repository.DeviceRepository;
import eu.arrowhead.serviceregistry.jpa.repository.DeviceSystemConnectorRepository;
import eu.arrowhead.serviceregistry.jpa.repository.SystemAddressRepository;
import eu.arrowhead.serviceregistry.jpa.repository.SystemRepository;
import eu.arrowhead.serviceregistry.jpa.entity.Device;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceSystemConnector;
import eu.arrowhead.serviceregistry.jpa.entity.System;

@Service
public class SystemDbService {

	//=================================================================================================
	// members

	@Autowired
	private SystemRepository systemRepo;

	@Autowired
	private PageService pageService;

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
	public List<SystemResponseDTO> createBulk(final List<SystemRequestDTO> candidates) {

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
			deviceSystemConnectorRepo.saveAllAndFlush(deviceSystemConnectorEntities);

			return createSystemResponseDTOs(systemEntities);

		} catch (final InvalidParameterException e) {
			throw e;
		} catch (final Exception e) {
			logger.error(e.getMessage());
			logger.debug(e);
			throw new InternalServerError("Database operation error");
		}

	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public List<SystemResponseDTO> updateBulk(final List<SystemRequestDTO> toUpdate) {
		logger.debug("updateBulk started");
		Assert.isTrue(!Utilities.isEmpty(toUpdate), "The list of systems to update is empty or missing.");

		try {

			checkSystemNamesExist(toUpdate); // system name can't be changed

			checkDeviceNamesExist(toUpdate); // checks if device names already exist

			//writing the updated system entities to the database
			List<System> systemEntities = systemRepo.findAllByNameIn(toUpdate.stream().map(s -> s.name()).collect(Collectors.toList()));
			for (final System systemEntity : systemEntities) {
				final Map<String, Object> metadata = toUpdate.stream()
						.filter(s -> s.name().equals(systemEntity.getName()))
						.findFirst()
						.get()
						.metadata();

				final String version = toUpdate.stream()
						.filter(s -> s.name().equals(systemEntity.getName()))
						.findFirst()
						.get()
						.version();

				systemEntity.setMetadata(Utilities.toJson(metadata));
				systemEntity.setVersion(version);
			}
			systemEntities = systemRepo.saveAllAndFlush(systemEntities);

			//removing the old system addresses from the database
			systemAddressRepo.deleteAllBySystemIn(systemEntities);

			//writing the new system address entities to the database
			final List<SystemAddress> systemAddressEntities = createSystemAddressEntities(toUpdate, systemEntities);
			systemAddressRepo.saveAllAndFlush(systemAddressEntities);

			//updating the old device-system connections
			final List<DeviceSystemConnector> connectionsToUpdate = new ArrayList<>();
			for (final System systemEntity : systemEntities) {
				final DeviceSystemConnector connection = deviceSystemConnectorRepo.findBySystem(systemEntity).get();
				connectionsToUpdate.add(connection);
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

			//writing the new device-system connections to the database
			deviceSystemConnectorRepo.saveAllAndFlush(connectionsToUpdate);

			return createSystemResponseDTOs(systemEntities);

		} catch (final InvalidParameterException e) {
			throw e;
		} catch (final Exception e) {
			logger.error(e.getMessage());
			logger.debug(e);
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
	public Optional<SystemRequestDTO> getByName(final String name) {
		logger.debug("getByName started");
		Assert.isTrue(!Utilities.isEmpty(name), "system name is missing or empty");

		synchronized (LOCK) {

			final Optional<System> system = systemRepo.findByName(name);
			if (system.isEmpty()) {
				return Optional.empty();
			}

			final List<AddressDTO> systemAddresses = systemAddressRepo.findAllBySystem(system.get()).stream()
					.map(sa -> new AddressDTO(sa.getAddressType().toString(), sa.getAddress()))
					.collect(Collectors.toList());

			final Device device = deviceSystemConnectorRepo.findBySystem(system.get()).get().getDevice();

			return Optional.of(new SystemRequestDTO(
					name,
					Utilities.fromJson(system.get().getMetadata(), new TypeReference<Map<String, Object>>() { }),
					system.get().getVersion(),
					systemAddresses,
					device.getName()));
		}

	}

	//-------------------------------------------------------------------------------------------------
	public Optional<System> getSystemByName(final String name) {
		logger.debug("getByName started");
		Assert.isTrue(!Utilities.isEmpty(name), "system name is missing or empty");

		return systemRepo.findByName(name);
	}

	//-------------------------------------------------------------------------------------------------
	public SystemResponseDTO createSystemResponseDTO(final System system) {

		synchronized (LOCK) {
			final List<AddressDTO> systemAddresses = systemAddressRepo.findAllBySystem(system).stream()
					.map(sa -> new AddressDTO(sa.getAddressType().toString(), sa.getAddress()))
					.collect(Collectors.toList());

			final Device device = deviceSystemConnectorRepo.findBySystem(system).get().getDevice();

			final List<AddressDTO> deviceAddresses = deviceAddressRepo.findAllByDevice(device).stream()
					.map(sa -> new AddressDTO(sa.getAddressType().toString(), sa.getAddress()))
					.collect(Collectors.toList());

			final DeviceResponseDTO deviceResponseDTO = new DeviceResponseDTO(
					device.getName(),
					Utilities.fromJson(device.getMetadata(), new TypeReference<Map<String, Object>>() { }),
					deviceAddresses,
					device.getCreatedAt().toString(),
					device.getUpdatedAt().toString());

			final SystemResponseDTO systemResponseDTO = new SystemResponseDTO(
					system.getName(),
					Utilities.fromJson(system.getMetadata(), new TypeReference<Map<String, Object>>() { }),
					system.getVersion(),
					systemAddresses,
					deviceResponseDTO,
					system.getCreatedAt().toString(),
					system.getUpdatedAt().toString()
					);

			return systemResponseDTO;
		}
	}

	//-------------------------------------------------------------------------------------------------
	public List<SystemResponseDTO> getPageByFilters(final SystemQueryRequestDTO dto, final String origin) {
		logger.debug("getPageByFilters for system query started");
		Assert.notNull(dto, "SystemQueryRequestDTO dto is null");

		PageRequest pageRequest = null;
		if (dto.pagination() != null) {
			pageRequest = pageService.getPageRequest(dto.pagination(), Direction.DESC, System.SORTABLE_FIELDS_BY, System.DEFAULT_SORT_FIELD, origin);
		}
		try {

			List<System> systemEntries = null;

			// without filter
			if (Utilities.isEmpty(dto.systemNames())
					&& Utilities.isEmpty(dto.addresses())
					&& Utilities.isEmpty(dto.addressType())
					&& Utilities.isEmpty(dto.metadataRequirementList())
					&& Utilities.isEmpty(dto.versions())
					&& Utilities.isEmpty(dto.deviceNames())) {

				if (pageRequest != null) {
					systemEntries = systemRepo.findAll(pageRequest).toList();
				} else {
					systemEntries = systemRepo.findAll();
				}
			}

			// with filters
			if (systemEntries == null) {

				final List<String> matchings = new ArrayList<>();

				synchronized (LOCK) {

					final List<System> toFilter = Utilities.isEmpty(dto.systemNames()) ? systemRepo.findAll() : systemRepo.findAllByNameIn(dto.systemNames());

					for (final System system : toFilter) {
						if (!Utilities.isEmpty(dto.addresses()) && Utilities.isEmpty(systemAddressRepo.findAllBySystemAndAddressIn(system, dto.addresses()))) {
							continue;
						}
						if (dto.addressType() != null && Utilities.isEmpty(systemAddressRepo.findAllBySystemAndAddressType(system, Enum.valueOf(AddressType.class, dto.addressType())))) {
							continue;
						}
						if (!Utilities.isEmpty(dto.metadataRequirementList())) {
							final Map<String, Object> metadata = Utilities.fromJson(system.getMetadata(), new TypeReference<Map<String, Object>>() { });
							boolean metaDataMatch = false;
							for (final MetadataRequirementDTO requirement : dto.metadataRequirementList()) {
								if (MetadataRequirementsMatcher.isMetadataMatch(metadata, requirement)) {
									metaDataMatch = true;
									break;
								}
							}
							if (!metaDataMatch) {
								continue;
							}
						}
						if (!Utilities.isEmpty(dto.versions()) && !systemRepo.findAllByVersionIn(dto.versions()).contains(system)) {
							continue;
						}
						if (!Utilities.isEmpty(dto.deviceNames()) && !dto.deviceNames().contains(deviceSystemConnectorRepo.findBySystem(system).get().getDevice().getName())) {
							continue;
						}

						matchings.add(system.getName());
					}
				}

				if (pageRequest != null) {
					systemEntries = systemRepo.findAllByNameIn(matchings, pageRequest).toList();
				} else {
					systemEntries = systemRepo.findAllByNameIn(matchings);
				}

			}

			return createSystemResponseDTOs(systemEntries);

		} catch (final Exception ex) {

			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");

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
		if (notExistingDeviceNames.size() != 0) {
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
			return candidates.stream()
					.map(c -> new DeviceSystemConnector(
							deviceRepo.findAllByNameIn(Arrays.asList(c.deviceName())).getFirst(),
							systemRepo.findAllByNameIn(Arrays.asList(c.name())).getFirst()
							))
					.collect(Collectors.toList());
		}
	}

	//-------------------------------------------------------------------------------------------------
	private List<SystemResponseDTO> createSystemResponseDTOs(final List<System> systems) {
		final List<SystemResponseDTO> result = new ArrayList<>();

		for (final System system : systems) {

			synchronized (LOCK) {

				final List<AddressDTO> systemAddresses = systemAddressRepo.findAllBySystem(system).stream()
						.map(sa -> new AddressDTO(sa.getAddressType().toString(), sa.getAddress()))
						.collect(Collectors.toList());

				// There may not be a device associated with the system because it has been deleted in the meantime
				Device device = null;
				final Optional<DeviceSystemConnector> optionalConnector = deviceSystemConnectorRepo.findBySystem(system);
				if (optionalConnector.isPresent()) {
					device = optionalConnector.get().getDevice();
				}

				DeviceResponseDTO deviceResponseDTO = null;
				if (device != null) {
					final List<AddressDTO> deviceAddresses = deviceAddressRepo.findAllByDevice(device).stream()
							.map(sa -> new AddressDTO(sa.getAddressType().toString(), sa.getAddress()))
							.collect(Collectors.toList());

					 deviceResponseDTO = new DeviceResponseDTO(
							device.getName(),
							Utilities.fromJson(device.getMetadata(), new TypeReference<Map<String, Object>>() { }),
							deviceAddresses,
							device.getCreatedAt().toString(),
							device.getUpdatedAt().toString());
				}

				final SystemResponseDTO systemResponseDTO = new SystemResponseDTO(
						system.getName(),
						Utilities.fromJson(system.getMetadata(), new TypeReference<Map<String, Object>>() { }),
						system.getVersion(),
						systemAddresses,
						deviceResponseDTO,
						system.getCreatedAt().toString(),
						system.getUpdatedAt().toString()
						);

				result.add(systemResponseDTO);
			}
		}

		return result;
	}


}
