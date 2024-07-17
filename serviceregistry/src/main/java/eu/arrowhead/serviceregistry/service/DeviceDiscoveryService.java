package eu.arrowhead.serviceregistry.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
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
import eu.arrowhead.dto.DeviceRequestDTO;
import eu.arrowhead.dto.DeviceResponseDTO;
import eu.arrowhead.serviceregistry.jpa.entity.Device;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceAddress;
import eu.arrowhead.serviceregistry.jpa.service.DeviceDbService;
import eu.arrowhead.serviceregistry.service.dto.DTOConverter;
import eu.arrowhead.serviceregistry.service.validation.DeviceDiscoveryValidation;
import eu.arrowhead.serviceregistry.service.validation.address.AddressNormalizator;

@Service
public class DeviceDiscoveryService {

	//=================================================================================================
	// members

	@Autowired
	private DeviceDbService dbService;

	@Autowired
	private DeviceDiscoveryValidation validator;

	@Autowired
	private AddressNormalizator addressNormalizator;

	@Autowired
	private DTOConverter dtoConverter;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Entry<DeviceResponseDTO, Boolean> registerDevice(final DeviceRequestDTO dto, final String origin) {
		logger.debug("registerDevice started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validator.validateRegisterDevice(dto, origin);

		final DeviceRequestDTO normalized = new DeviceRequestDTO(
				dto.name().trim(),
				dto.metadata(),
				Utilities.isEmpty(dto.addresses()) ? new ArrayList<>()
						: dto.addresses().stream()
								.map(a -> new AddressDTO(a.type().trim(), addressNormalizator.normalize(a.address())))
								.collect(Collectors.toList()));

		normalized.addresses().forEach(address -> validator.validateNormalizedAddress(address, origin));

		try {
			final Optional<Entry<Device, List<DeviceAddress>>> optional = dbService.getByName(normalized.name());

			// Existing device
			if (optional.isPresent()) {
				final Device existing = optional.get().getKey();

				if (!Utilities.isEmpty(existing.getMetadata()) || !Utilities.isEmpty(dto.metadata())) {
					if ((!Utilities.isEmpty(existing.getMetadata()) && Utilities.isEmpty(dto.metadata()))
							|| (Utilities.isEmpty(existing.getMetadata()) && !Utilities.isEmpty(dto.metadata()))) {
						throw new InvalidParameterException("Device with name '" + normalized.name() + "' already exists, but provided metadata is not matching");
					}

					if (!existing.getMetadata().equals(Utilities.toJson(normalized.metadata()))) {
						throw new InvalidParameterException("Device with name '" + normalized.name() + "' already exists, but provided metadata is not matching");
					}
				}

				final List<DeviceAddress> existingAddresses = optional.get().getValue();
				final Set<String> existingAddressesSTR = existingAddresses
						.stream()
						.map(a -> a.getAddressType().name() + a.getAddress())
						.collect(Collectors.toSet());
				final Set<String> candidateAddressesSTR = Utilities.isEmpty(normalized.addresses()) ? Set.of()
						: normalized.addresses()
								.stream()
								.map(a -> a.type() + a.address())
								.collect(Collectors.toSet());

				if (!existingAddressesSTR.equals(candidateAddressesSTR)) {
					throw new InvalidParameterException("Device with name '" + normalized.name() + "' already exists, but provided interfaces are not matching");
				}

				return Map.entry(dtoConverter.convertDeviceEntityToDeviceResponseDTO(existing, existingAddresses), false);
			}

			// New device
			final Entry<Device, List<DeviceAddress>> entities = dbService.create(normalized);
			return Map.entry(dtoConverter.convertDeviceEntityToDeviceResponseDTO(entities.getKey(), entities.getValue()), true);

		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);

		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public boolean revokeDevice(final String name, final String origin) {
		logger.debug("revokeDevice started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validator.validateRevokeDevice(name, origin);

		try {
			return dbService.deleteByName(name);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}
}
