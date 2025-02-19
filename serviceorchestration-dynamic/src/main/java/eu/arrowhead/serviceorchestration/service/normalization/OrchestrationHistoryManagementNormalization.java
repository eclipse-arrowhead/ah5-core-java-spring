package eu.arrowhead.serviceorchestration.service.normalization;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.name.NameNormalizer;
import eu.arrowhead.dto.OrchestrationHistoryQueryRequestDTO;

@Service
public class OrchestrationHistoryManagementNormalization {

	//=================================================================================================
	// members

	@Autowired
	private NameNormalizer nameNormalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationHistoryQueryRequestDTO normalizeOrchestrationHistoryQueryRequestDTO(final OrchestrationHistoryQueryRequestDTO dto) {
		logger.debug("normalizeOrchestrationHistoryQueryRequestDTO started...");

		if (dto == null) {
			return new OrchestrationHistoryQueryRequestDTO(null, new ArrayList<>(), new ArrayList<>(), null, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
		}

		return new OrchestrationHistoryQueryRequestDTO(
				dto.pagination(), // no need to normalize, because it will happen in the getPageRequest method
				Utilities.isEmpty(dto.ids()) ? new ArrayList<String>() : dto.ids().stream().map(id -> id.trim().toUpperCase()).toList(),
				Utilities.isEmpty(dto.statuses()) ? new ArrayList<String>() : dto.statuses().stream().map(status -> status.trim().toUpperCase()).toList(),
				Utilities.isEmpty(dto.type()) ? null : dto.type().trim().toUpperCase(),
				Utilities.isEmpty(dto.requesterSystrems()) ? new ArrayList<String>() : dto.requesterSystrems().stream().map(sys -> nameNormalizer.normalize(sys)).toList(),
				Utilities.isEmpty(dto.targetSystems()) ? new ArrayList<String>() : dto.targetSystems().stream().map(sys -> nameNormalizer.normalize(sys)).toList(),
				Utilities.isEmpty(dto.serviceDefinitions()) ? new ArrayList<String>() : dto.serviceDefinitions().stream().map(def -> nameNormalizer.normalize(def)).toList(),
				Utilities.isEmpty(dto.subscriptionIds()) ? new ArrayList<String>() : dto.subscriptionIds().stream().map(id -> id.trim().toUpperCase()).toList());
	}
}
