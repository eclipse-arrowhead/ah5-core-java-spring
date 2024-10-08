package eu.arrowhead.serviceregistry.service.validation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.MetadataValidation;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.SystemLookupRequestDTO;
import eu.arrowhead.dto.SystemRequestDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.ServiceRegistryConstants;
import eu.arrowhead.serviceregistry.service.normalization.SystemDiscoveryNormalization;
import eu.arrowhead.serviceregistry.service.validation.address.AddressValidator;
import eu.arrowhead.serviceregistry.service.validation.name.NameValidator;
import eu.arrowhead.serviceregistry.service.validation.version.VersionValidator;

@Service
public class SystemDiscoveryValidation {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private SystemDiscoveryNormalization normalizer;

	@Autowired
	private AddressValidator addressValidator;

	@Autowired
	private NameValidator nameValidator;

	@Autowired
	private VersionValidator versionValidator;

	//=================================================================================================
	// methods

	// VALIDATION

	//-------------------------------------------------------------------------------------------------
	public void validateRegisterSystem(final SystemRequestDTO dto, final String origin) {
		logger.debug("validateRegisterSystem started");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.isEmpty(dto.name())) {
			throw new InvalidParameterException("System name is empty", origin);
		}

		if (dto.name().length() > ServiceRegistryConstants.SYSTEM_NAME_LENGTH) {
			throw new InvalidParameterException("System name is too long", origin);
		}

		if (!Utilities.isEmpty(dto.addresses())) {
			for (final AddressDTO address : dto.addresses()) {
				if (address == null) {
					throw new InvalidParameterException("Address list contains null element", origin);
				}

				if (Utilities.isEmpty(address.type())) {
					throw new InvalidParameterException("Address type is missing", origin);
				}

				if (!Utilities.isEnumValue(address.type().toUpperCase(), AddressType.class)) {
					throw new InvalidParameterException("Invalid address type: " + address.type(), origin);
				}

				if (Utilities.isEmpty(address.address())) {
					throw new InvalidParameterException("Address value is missing", origin);
				}
			}
		}
		if (!Utilities.isEmpty(dto.metadata())) {
			MetadataValidation.validateMetadataKey(dto.metadata());
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validateLookupSystem(final SystemLookupRequestDTO dto, final String origin) {
		logger.debug("validateLookupSystem started");

		if (dto != null) {

			//system name list
			if (!Utilities.isEmpty(dto.systemNames()) && Utilities.containsNullOrEmpty(dto.systemNames())) {
				throw new InvalidParameterException("System name list contains null or empty element", origin);
			}

			//address list
			if (!Utilities.isEmpty(dto.addresses()) && Utilities.containsNullOrEmpty(dto.addresses())) {
				throw new InvalidParameterException("Address list contains null or empty element", origin);
			}

			//address type
			if (!Utilities.isEmpty(dto.addressType()) && !Utilities.isEnumValue(dto.addressType().toUpperCase(), AddressType.class)) {
				throw new InvalidParameterException("Invalid address type: " + dto.addressType(), origin);
			}

			//metadata
			if (!Utilities.isEmpty(dto.metadataRequirementList()) && Utilities.containsNull(dto.metadataRequirementList())) {
				throw new InvalidParameterException("Metadata requirement list contains null element", origin);
			}

			//version list
			if (!Utilities.isEmpty(dto.versions()) && Utilities.containsNull(dto.versions())) {
				throw new InvalidParameterException("Version list contains null element", origin);
			}

			//device name list
			if (!Utilities.isEmpty(dto.deviceNames()) && Utilities.containsNullOrEmpty(dto.deviceNames())) {
				throw new InvalidParameterException("Device name list contains null or empty element", origin);
			}
		}
	}

	// VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public SystemRequestDTO validateAndNormalizeRegisterSystem(final SystemRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeRegisterSystem started");

		validateRegisterSystem(dto, origin);

		final SystemRequestDTO normalized = normalizer.normalizeSystemRequestDTO(dto);

		try {
			normalized.addresses().forEach(address -> addressValidator.validateNormalizedAddress(AddressType.valueOf(address.type()), address.address()));
			versionValidator.validateNormalizedVersion(normalized.version());
			nameValidator.validateName(normalized.name());
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public SystemLookupRequestDTO validateAndNormalizeLookupSystem(final SystemLookupRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeLookupSystem started");

		validateLookupSystem(dto, origin);

		final SystemLookupRequestDTO normalized = normalizer.normalizeSystemLookupRequestDTO(dto);

		try {
			if (!Utilities.isEmpty(normalized.addressType()) && !Utilities.isEmpty(normalized.addresses())) {
				normalized.addresses().forEach(a -> addressValidator.validateNormalizedAddress(AddressType.valueOf(normalized.addressType()), a));
			}

			if (!Utilities.isEmpty(normalized.versions())) {
				normalized.versions().forEach(v -> versionValidator.validateNormalizedVersion(v));
			}

		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public void validateRevokeSystem(final String name, final String origin) {
		logger.debug("validateRevokeSystem started");

		if (Utilities.isEmpty(name)) {
			throw new InvalidParameterException("System name is empty", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public String validateAndNormalizeRevokeSystem(final String name, final String origin) {
		logger.debug("validateRevokeSystem started");

		validateRevokeSystem(name, origin);

		return normalizer.normalizeRevokeSystemName(name);
	}

}
