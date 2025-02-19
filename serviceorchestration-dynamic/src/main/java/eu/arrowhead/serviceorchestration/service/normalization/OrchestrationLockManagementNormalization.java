package eu.arrowhead.serviceorchestration.service.normalization;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.name.NameNormalizer;
import eu.arrowhead.dto.OrchestrationLockListRequestDTO;
import eu.arrowhead.dto.OrchestrationLockRequestDTO;

@Service
public class OrchestrationLockManagementNormalization {

	//=================================================================================================
	// members

	@Autowired
	private NameNormalizer nameNormalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationLockListRequestDTO normalizeOrchestrationLockListRequestDTO(final OrchestrationLockListRequestDTO dto) {
		logger.debug("normalizeOrchestrationLockListRequestDTO started...");
		Assert.notNull(dto, "dto is null");

		return new OrchestrationLockListRequestDTO(
				dto.locks().stream()
						.map(lock -> new OrchestrationLockRequestDTO(
								nameNormalizer.normalize(lock.serviceInstanceId()),
								nameNormalizer.normalize(lock.owner()),
								Utilities.isEmpty(lock.expiresAt()) ? null : lock.expiresAt().trim()))
						.toList());
	}
}
