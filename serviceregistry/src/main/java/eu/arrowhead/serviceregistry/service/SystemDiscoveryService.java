package eu.arrowhead.serviceregistry.service;

import java.util.Map.Entry;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.SystemRequestDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.serviceregistry.service.validation.SystemDiscoveryValidation;

@Service
public class SystemDiscoveryService {
	
	//=================================================================================================
	// members
	
	@Autowired
	private SystemDiscoveryValidation validator;
	
	private final Logger logger = LogManager.getLogger(this.getClass());
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public Entry<SystemResponseDTO, Boolean> registerSystem(final SystemRequestDTO dto, final String origin) {
		throw new NotImplementedException();
	}
		/*logger.debug("registerSystem started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validator.validateRegisterSystem(dto, origin);
		final DeviceRequestDTO normalized = normalizer.normalizeDeviceRequestDTO(dto);
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
						.map(a -> a.getAddressType().name() + "-" + a.getAddress())
						.collect(Collectors.toSet());
				final Set<String> candidateAddressesSTR = Utilities.isEmpty(normalized.addresses()) ? Set.of()
						: normalized.addresses()
								.stream()
								.map(a -> a.type() + "-" + a.address())
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
	}*/
}
