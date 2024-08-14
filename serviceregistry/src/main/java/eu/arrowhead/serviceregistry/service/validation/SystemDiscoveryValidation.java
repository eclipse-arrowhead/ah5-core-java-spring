package eu.arrowhead.serviceregistry.service.validation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.jpa.ArrowheadEntity;
import eu.arrowhead.dto.SystemRequestDTO;

@Service
public class SystemDiscoveryValidation {
	
	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	/*public void validateRegisterSystem(final SystemRequestDTO dto, final String origin) {
		logger.debug("validateRegisterSystem started");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.isEmpty(dto.name())) {
			throw new InvalidParameterException("System name is empty", origin);
		}

		if (dto.name().length() > ArrowheadEntity.VARCHAR_SMALL) {
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

				if (!Utilities.isEnumValue(address.type(), AddressType.class)) {
					throw new InvalidParameterException("Invalid address type: " + address.type(), origin);
				}

				if (Utilities.isEmpty(address.address())) {
					throw new InvalidParameterException("Address value is missing", origin);
				}
			}
		}
	}*/
}
