package eu.arrowhead.serviceorchestration.service.validation;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.NameValidator;
import eu.arrowhead.dto.OrchestrationLockListRequestDTO;
import eu.arrowhead.serviceorchestration.service.normalization.OrchestrationLockManagementNormalization;

@Service
public class OrchestrationLockManagementValidation {

	//=================================================================================================
	// members

	@Autowired
	private OrchestrationLockManagementNormalization normalization;

	@Autowired
	private NameValidator nameValidator;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	// VALIDATION

	//-------------------------------------------------------------------------------------------------
	public void validateCreateService(final OrchestrationLockListRequestDTO dto, final String origin) {
		logger.debug("validateCreateService started...");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is null", origin);
		}

		if (Utilities.containsNull(dto.locks())) {
			throw new InvalidParameterException("Request payload contains null element", origin);
		}

		final ZonedDateTime now = Utilities.utcNow();
		dto.locks().forEach(lock -> {
			if (Utilities.isEmpty(lock.serviceInstanceId())) {
				throw new InvalidParameterException("Service instance id is missing", origin);
			}

			if (Utilities.isEmpty(lock.owner())) {
				throw new InvalidParameterException("Owner is missing", origin);
			}

			if (!Utilities.isEmpty(lock.expiresAt())) {
				try {
					final ZonedDateTime expiresAt = Utilities.parseUTCStringToZonedDateTime(lock.expiresAt().trim());
					if (expiresAt.isBefore(now)) {
						throw new InvalidParameterException("Expires at is in the past: " + lock.expiresAt(), origin);
					}
				} catch (final DateTimeParseException ex) {
					throw new InvalidParameterException("Invalid expires at format: " + lock.expiresAt(), origin);
				}
			}
		});
	}

	// VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public OrchestrationLockListRequestDTO validateAndNormalizeCreateService(final OrchestrationLockListRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeCreateService started...");

		validateAndNormalizeCreateService(dto, origin);

		final OrchestrationLockListRequestDTO normalized = normalization.normalizeOrchestrationLockListRequestDTO(dto);

		try {
			normalized.locks().forEach(lock -> {
				nameValidator.validateName(lock.serviceInstanceId());
				nameValidator.validateName(lock.owner());
			});

		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}
}
