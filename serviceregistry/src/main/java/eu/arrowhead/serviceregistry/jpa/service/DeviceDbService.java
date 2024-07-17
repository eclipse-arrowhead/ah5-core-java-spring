package eu.arrowhead.serviceregistry.jpa.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
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
import eu.arrowhead.dto.DeviceRequestDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.jpa.entity.Device;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceAddress;
import eu.arrowhead.serviceregistry.jpa.repository.DeviceAddressRepository;
import eu.arrowhead.serviceregistry.jpa.repository.DeviceRepository;

@Service
public class DeviceDbService {

	//=================================================================================================
	// members

	@Autowired
	private DeviceRepository deviceRepo;

	@Autowired
	private DeviceAddressRepository addressRepo;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Optional<Entry<Device, List<DeviceAddress>>> getDeviceByName(final String name) {
		logger.debug("getDeviceByName started");

		try {
			final Optional<Device> deviceOpt = deviceRepo.findByName(name);
			if (deviceOpt.isEmpty()) {
				return Optional.empty();
			}

			return Optional.of(Map.entry(deviceOpt.get(), addressRepo.findAllByDevice(deviceOpt.get())));

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public Map<Device, List<DeviceAddress>> createBulk(final List<DeviceRequestDTO> candidates) {
		logger.debug("createBulk started");
		Assert.isTrue(!Utilities.isEmpty(candidates), "device candidate list is empty");

		try {
			final List<String> names = candidates.stream()
					.map(d -> d.name())
					.collect(Collectors.toList());

			final List<Device> existing = deviceRepo.findAllByNameIn(names);
			if (!Utilities.isEmpty(existing)) {
				final String existingNames = existing.stream()
						.map(e -> e.getName())
						.collect(Collectors.joining(", "));
				throw new InvalidParameterException(
						"Device names already exists: " + existingNames);
			}

			List<Device> deviceEntities = candidates.stream()
					.map(d -> new Device(d.name(), Utilities.toJson(d.metadata())))
					.collect(Collectors.toList());

			deviceEntities = deviceRepo.saveAllAndFlush(deviceEntities);

			final List<DeviceAddress> deviceAddressEntities = new ArrayList<>();
			for (final DeviceRequestDTO candidate : candidates) {
				if (!Utilities.isEmpty(candidate.addresses())) {
					final Device device = deviceEntities.stream()
							.filter(d -> d.getName().equals(candidate.name()))
							.findFirst()
							.get();

					final List<DeviceAddress> addresses = candidate.addresses().stream()
							.map(a -> new DeviceAddress(
									device,
									AddressType.valueOf(a.type()),
									a.address()))
							.collect(Collectors.toList());
					deviceAddressEntities.addAll(addresses);
				}
			}
			addressRepo.saveAllAndFlush(deviceAddressEntities);

			final Map<Device, List<DeviceAddress>> results = new HashMap<>();
			for (final Device device : deviceEntities) {
				results.put(device, addressRepo.findAllByDevice(device));
			}
			return results;

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
	public Entry<Device, List<DeviceAddress>> create(final DeviceRequestDTO candidate) {
		logger.debug("create started");
		Assert.notNull(candidate, "device candidate is null");
		Assert.isTrue(!Utilities.isEmpty(candidate.name()), "device candidate name is empty");

		try {

			final Optional<Device> existingOpt = deviceRepo.findByName(candidate.name());
			if (existingOpt.isPresent()) {
				throw new InvalidParameterException("Device with name '" + candidate.name() + "' already exists");
			}

			final Device deviceEntity = deviceRepo.saveAndFlush(new Device(candidate.name(), Utilities.toJson(candidate.metadata())));

			final Entry<Device, List<DeviceAddress>> result = Map.entry(deviceEntity, new ArrayList<>());
			if (!Utilities.isEmpty(candidate.addresses())) {
				final List<DeviceAddress> addresses = candidate.addresses().stream()
						.map(a -> new DeviceAddress(deviceEntity, AddressType.valueOf(a.type()), a.address()))
						.collect(Collectors.toList());

				result.getValue().addAll(addressRepo.saveAllAndFlush(addresses));
			}

			return result;

		} catch (final InvalidParameterException ex) {
			throw ex;

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}
}
