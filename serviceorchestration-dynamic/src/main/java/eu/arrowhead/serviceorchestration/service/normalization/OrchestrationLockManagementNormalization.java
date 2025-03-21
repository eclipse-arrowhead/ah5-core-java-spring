package eu.arrowhead.serviceorchestration.service.normalization;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.name.NameNormalizer;
import eu.arrowhead.dto.OrchestrationLockListRequestDTO;
import eu.arrowhead.dto.OrchestrationLockQueryRequestDTO;
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
								lock.expiresAt().trim()))
						.toList());
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationLockQueryRequestDTO normalizeOrchestrationLockQueryRequestDTO(final OrchestrationLockQueryRequestDTO dto) {
		logger.debug("normalizeOrchestrationLockQueryRequestDTO started...");

		if (dto == null) {
			return new OrchestrationLockQueryRequestDTO(null, null, null, null, null, null, null);
		}

		return new OrchestrationLockQueryRequestDTO(
				dto.pagination(), // no need to normalize, because it will happen in the getPageRequest method
				Utilities.isEmpty(dto.ids()) ? new ArrayList<>() : dto.ids(),
				Utilities.isEmpty(dto.orchestrationJobIds()) ? new ArrayList<>() : dto.orchestrationJobIds().stream().map(id -> id.trim().toUpperCase()).toList(),
				Utilities.isEmpty(dto.serviceInstanceIds()) ? new ArrayList<>() : dto.serviceInstanceIds().stream().map(instance -> nameNormalizer.normalize(instance)).toList(),
				Utilities.isEmpty(dto.owners()) ? new ArrayList<>() : dto.owners().stream().map(owner -> nameNormalizer.normalize(owner)).toList(),
				Utilities.isEmpty(dto.expiresBefore()) ? null : dto.expiresBefore().trim(),
				Utilities.isEmpty(dto.expiresAfter()) ? null : dto.expiresAfter().trim());

	}

	//-------------------------------------------------------------------------------------------------
	public List<String> normalizeServiceInstanceIds(final List<String> instanceIds) {
		logger.debug("normalizeServiceInstanceIds started...");
		Assert.isTrue(!Utilities.isEmpty(instanceIds), "Service instance id list is empty");

		return instanceIds.stream().map(id -> nameNormalizer.normalize(id)).toList();
	}

	//-------------------------------------------------------------------------------------------------
	public String normalizeSystemName(final String name) {
		logger.debug("normalizeSystemName started...");
		Assert.isTrue(!Utilities.isEmpty(name), "System name is empty");

		return nameNormalizer.normalize(name);
	}
}
