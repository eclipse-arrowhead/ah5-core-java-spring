package eu.arrowhead.serviceregistry.service.validation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.DeviceLookupRequestDTO;
import eu.arrowhead.dto.DeviceRequestDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.ServiceRegistryConstants;
import eu.arrowhead.serviceregistry.service.normalization.DeviceDiscoveryNormalization;
import eu.arrowhead.serviceregistry.service.validation.address.AddressValidator;

@Service
public class DeviceDiscoveryValidation {

	//=================================================================================================
	// members

	@Autowired
	private AddressValidator addressValidator;

	@Autowired
	private DeviceDiscoveryNormalization normalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	// VALIDATION

	//-------------------------------------------------------------------------------------------------
	public void validateRegisterDevice(final DeviceRequestDTO dto, final String origin) {
		logger.debug("validateRegisterDevice started");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.isEmpty(dto.name())) {
			throw new InvalidParameterException("Device name is empty", origin);
		}

		if (dto.name().length() > ServiceRegistryConstants.DEVICE_NAME_LENGTH) {
			throw new InvalidParameterException("Device name is too long", origin);
		}

		if (!Utilities.isEmpty(dto.addresses())) {
			for (final AddressDTO address : dto.addresses()) {
				if (address == null) {
					throw new InvalidParameterException("Address list contains null element", origin);
				}

				if (Utilities.isEmpty(address.type())) {
					throw new InvalidParameterException("Address type is missing", origin);
				}

				if (!Utilities.isEnumValue(address.type(), AddressType.class)) {
					throw new InvalidParameterException("Invalid address type: " + address.type(), origin);
				}

				if (Utilities.isEmpty(address.address())) {
					throw new InvalidParameterException("Address value is missing", origin);
				}
				if (address.type().length() > ServiceRegistryConstants.ADDRESS_TYPE_LENGTH) {
					throw new InvalidParameterException("Address type is too long", origin);
				}

				if (address.address().length() > ServiceRegistryConstants.ADDRESS_ADDRESS_LENGTH) {
					throw new InvalidParameterException("Address is too long", origin);
				}
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validateLookupDevice(final DeviceLookupRequestDTO dto, final String origin) {
		logger.debug("validateLookupDevice started");

		if (dto != null) {

			if (!Utilities.isEmpty(dto.deviceNames()) && Utilities.containsNullOrEmpty(dto.deviceNames())) {
				throw new InvalidParameterException("Device name list contains null or empty element", origin);
			}

			if (!Utilities.isEmpty(dto.addresses()) && Utilities.containsNullOrEmpty(dto.addresses())) {
				throw new InvalidParameterException("Address list contains null or empty element", origin);
			}

			if (!Utilities.isEmpty(dto.addressType()) && !Utilities.isEnumValue(dto.addressType(), AddressType.class)) {
				throw new InvalidParameterException("Invalid address type: " + dto.addressType(), origin);
			}

			if (!Utilities.isEmpty(dto.metadataRequirementList()) && Utilities.containsNull(dto.metadataRequirementList())) {
				throw new InvalidParameterException("Metadata requirement list contains null element", origin);
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validateRevokeDevice(final String name, final String origin) {
		logger.debug("validateRevokeDevice started");

		if (Utilities.isEmpty(name)) {
			throw new InvalidParameterException("Device name is empty", origin);
		}
	}

	// VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public DeviceRequestDTO validateAndNormalizeRegisterDevice(final DeviceRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeRegisterDevice started");

		validateRegisterDevice(dto, origin);

		final DeviceRequestDTO normalized = normalizer.normalizeDeviceRequestDTO(dto);
		normalized.addresses().forEach(address -> addressValidator.validateNormalizedAddress(AddressType.valueOf(address.type()), address.address()));

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public DeviceLookupRequestDTO validateAndNormalizeLookupDevice(final DeviceLookupRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeLookupDevice started");

		validateLookupDevice(dto, origin);
		final DeviceLookupRequestDTO normalized = dto == null ? new DeviceLookupRequestDTO(null, null, null, null) : normalizer.normalizeDeviceLookupRequestDTO(dto);

		if (!Utilities.isEmpty(normalized.addressType()) && !Utilities.isEmpty(normalized.addresses())) {
			normalized.addresses().forEach(a -> addressValidator.validateNormalizedAddress(AddressType.valueOf(normalized.addressType()), a));
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public String validateAndNormalizeRevokeDevice(final String name, final String origin) {
		logger.debug("validateAndNormalizeRevokeDevice started");

		validateRevokeDevice(name, origin);

		return normalizer.normalizeDeviceName(name);

	}
}