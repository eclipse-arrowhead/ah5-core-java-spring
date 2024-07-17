package eu.arrowhead.serviceregistry.service.validation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.DeviceRequestDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.service.validation.address.AddressTypeValidator;

@Service
public class DeviceDiscoveryValidation {

	//=================================================================================================
	// members

	@Autowired
	private AddressTypeValidator addressTypeValidator;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void validateRegisterDevice(final DeviceRequestDTO dto, final String origin) {
		logger.debug("validateRegisterDevice started");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.isEmpty(dto.name())) {
			throw new InvalidParameterException("Device name is empty", origin);
		}

		if (!Utilities.isEmpty(dto.addresses())) {
			for (final AddressDTO address : dto.addresses()) {

				if (Utilities.isEmpty(address.type())) {
					throw new InvalidParameterException("Address type is missing", origin);
				}

				if (!Utilities.isEnumValue(address.type(), AddressType.class)) {
					throw new InvalidParameterException("Invalid address type: " + address.type(), origin);
				}

				if (Utilities.isEmpty(address.address())) {
					throw new InvalidParameterException("Address value is missing", origin);
				}
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validateNormalizedAddress(final AddressDTO dto, final String origin) {
		logger.debug("validateNormalizedAddress started");
		Assert.isTrue(Utilities.isEnumValue(dto.type(), AddressType.class), "address type is invalid");

		try {
			addressTypeValidator.validateNormalizedAddress(AddressType.valueOf(dto.type()), dto.address());
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validateRevokeDevice(final String name, final String origin) {
		logger.debug("validateRevokeDevice started");

		if (Utilities.isEmpty(name)) {
			throw new InvalidParameterException("Device name is empty", origin);
		}
	}
}
