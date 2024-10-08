package eu.arrowhead.serviceregistry.service;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.type.TypeReference;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.SystemListResponseDTO;
import eu.arrowhead.dto.SystemLookupRequestDTO;
import eu.arrowhead.dto.SystemRequestDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.ServiceRegistrySystemInfo;
import eu.arrowhead.serviceregistry.jpa.entity.Device;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceAddress;
import eu.arrowhead.serviceregistry.jpa.entity.System;
import eu.arrowhead.serviceregistry.jpa.entity.SystemAddress;
import eu.arrowhead.serviceregistry.jpa.service.SystemDbService;
import eu.arrowhead.serviceregistry.service.dto.DTOConverter;
import eu.arrowhead.serviceregistry.service.matching.AddressMatching;
import eu.arrowhead.serviceregistry.service.validation.SystemDiscoveryValidation;

@Service
public class SystemDiscoveryService {

	//=================================================================================================
	// members

	@Autowired
	private SystemDiscoveryValidation validator;

	@Autowired
	private AddressMatching addressMatcher;

	@Autowired
	private SystemDbService dbService;

	@Autowired
	private DTOConverter dtoConverter;

	@Autowired
	private ServiceRegistrySystemInfo sysInfo;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Entry<SystemResponseDTO, Boolean> registerSystem(final SystemRequestDTO dto, final String origin) {
		logger.debug("registerSystem started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final SystemRequestDTO normalized = validator.validateAndNormalizeRegisterSystem(dto, origin);

		try {
			final Optional<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> optional = dbService.getByName(normalized.name());

			// Existing system
			if (optional.isPresent()) {
				final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> existingSystem = optional.get();

				// We should check if every property is the same
				checkSameSystemAttributes(existingSystem, normalized);

				// Convert to response and return
				final SystemResponseDTO existingSystemAsResponseDTO = dtoConverter.convertSystemTripletToDTO(existingSystem);
				return Map.entry(existingSystemAsResponseDTO, false);
			}

			// New system
			final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> response = dbService.createBulk(List.of(normalized)).getFirst();
			return Map.entry(dtoConverter.convertSystemTripletToDTO(response), true);

		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);

		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public SystemListResponseDTO lookupSystem(final SystemLookupRequestDTO dto, final boolean verbose, final String origin) {
		logger.debug("lookupSystem started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final SystemLookupRequestDTO normalized = validator.validateAndNormalizeLookupSystem(dto, origin);

		try {
			final PageRequest pageRequest = PageRequest.of(0, Integer.MAX_VALUE, Direction.DESC, System.DEFAULT_SORT_FIELD);
			final Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> page = dbService.getPageByFilters(
					pageRequest,
					normalized.systemNames(),
					normalized.addresses(),
					Utilities.isEmpty(normalized.addressType()) ? null : AddressType.valueOf(normalized.addressType()),
					normalized.metadataRequirementList(),
					normalized.versions(),
					normalized.deviceNames());

			final SystemListResponseDTO result = dtoConverter.convertSystemTripletPageToDTO(page);

			//we do not provide device information (except for the name), if the verbose mode is not enabled, or the user set it false in the query param
			if (!verbose || !sysInfo.isDiscoveryVerbose()) {
				return dtoConverter.convertSystemListResponseDtoToTerse(result);
			}

			return result;
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public boolean revokeSystem(final String name, final String origin) {
		logger.debug("revokeSystem started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final String normalizedName = validator.validateAndNormalizeRevokeSystem(name, origin);

		try {
			return dbService.deleteByName(normalizedName);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	// throws exception, if the two systems doesn't have the same attributes
	private void checkSameSystemAttributes(final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> existing, final SystemRequestDTO dto) {
		logger.debug("checkSameSystemAttributes started");
		Assert.isTrue(existing.getLeft().getName().equals(dto.name()), "The systems are not identical!");

		final System existingSystem = existing.getLeft();

		// metadata
		if (!Utilities.fromJson(existingSystem.getMetadata(), new TypeReference<Map<String, Object>>() { }).equals(dto.metadata())) {
			throw new InvalidParameterException("System with name: " + existingSystem.getName() + " already exists, but provided metadata is not matching");
		}

		// version
		if (!existingSystem.getVersion().equals(dto.version())) {
			throw new InvalidParameterException("System with name: " + existingSystem.getName() + " already exists, but provided version is not matching");
		}

		// addresses
		final List<AddressDTO> existingAddresses = existing.getMiddle()
				.stream()
				.map(a -> new AddressDTO(a.getAddressType().toString(), a.getAddress()))
				.collect(Collectors.toList());
		if (!addressMatcher.isAddressListMatching(existingAddresses, dto.addresses())) {
			throw new InvalidParameterException("System with name: " + existingSystem.getName() + " already exists, but provided address list is not matching");
		}

		// device names
		final String existingName = existing.getRight() == null ? null : existing.getRight().getKey().getName();
		final String dtoName = dto.deviceName();

		if ((existingName == null && dtoName != null) || existingName != null && dtoName == null) {
			throw new InvalidParameterException("System with name: " + existingSystem.getName() + " already exists, but provided device name is not matching");
		}

		if (existingName != null && !existingName.equals(dtoName)) {
			throw new InvalidParameterException("System with name: " + existingSystem.getName() + " already exists, but provided device name is not matching");
		}
	}
}
