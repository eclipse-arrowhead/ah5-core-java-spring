package eu.arrowhead.serviceorchestration.service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.OrchestrationLockListRequestDTO;
import eu.arrowhead.dto.OrchestrationLockListResponseDTO;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationLock;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationLockDbService;
import eu.arrowhead.serviceorchestration.service.dto.DTOConverter;
import eu.arrowhead.serviceorchestration.service.validation.OrchestrationLockManagementValidation;

@Service
public class OrchestrationLockManagementService {

	//=================================================================================================
	// members

	@Autowired
	private OrchestrationLockManagementValidation validator;

	@Autowired
	private OrchestrationLockDbService lockDbService;

	@Autowired
	private DTOConverter dtoConverter;

	private static final Object LOCK = new Object();

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods
	public OrchestrationLockListResponseDTO create(final OrchestrationLockListRequestDTO dto, final String origin) {
		logger.debug("create started..");

		final OrchestrationLockListRequestDTO normalized = validator.validateAndNormalizeCreateService(dto, origin);
		final ZonedDateTime now = Utilities.utcNow();

		synchronized (LOCK) {
			try {
				final List<OrchestrationLock> existingLocks = lockDbService.getByServiceInstanceId(normalized.locks().stream().map(l -> l.serviceInstanceId()).toList());
				final List<Long> expiredLockIds = new ArrayList<>();
				final List<String> alreadyLockedServices = new ArrayList<>();
				for (OrchestrationLock existingLock : existingLocks) {
					if (!Utilities.isEmpty(existingLock.getOrchestrationJobId())
							&& existingLock.getExpiresAt() != null
							&& existingLock.getExpiresAt().isBefore(now)) {
						expiredLockIds.add(existingLock.getId());
					} else {
						alreadyLockedServices.add(existingLock.getServiceInstanceId());
					}
				}
				if (!Utilities.isEmpty(alreadyLockedServices)) {
					throw new InvalidParameterException("Already locked: " + alreadyLockedServices.stream().collect(Collectors.joining(", ")), origin);
				}

				lockDbService.deleteInBatch(expiredLockIds);
				final List<OrchestrationLock> candidates = normalized.locks().stream().map(l -> new OrchestrationLock(l.serviceInstanceId(), l.owner(), Utilities.parseUTCStringToZonedDateTime(l.expiresAt()))).toList();
				final List<OrchestrationLock> saved = lockDbService.create(candidates);

				return dtoConverter.converOrchestartionLockListToDTO(saved, saved.size());

			} catch (final InternalServerError ex) {
				throw new InternalServerError(ex.getMessage(), origin);
			}
		}
	}
}
