package eu.arrowhead.serviceorchestration.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.service.PageService;
import eu.arrowhead.dto.OrchestrationHistoryQueryRequestDTO;
import eu.arrowhead.dto.OrchestrationHistoryResponseDTO;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationJobDbService;
import eu.arrowhead.serviceorchestration.service.dto.DTOConverter;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationJobFilter;

@Service
public class OrchestrationHistoryManagementService {

	//=================================================================================================
	// members

	@Autowired
	private OrchestrationJobDbService jobDbService;

	@Autowired
	private PageService pageService;

	@Autowired
	private DTOConverter dtoConverter;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	public OrchestrationHistoryResponseDTO query(final OrchestrationHistoryQueryRequestDTO dto, final String origin) {
		logger.debug("query started...");

		final OrchestrationHistoryQueryRequestDTO normalized = dto; // TODO validateAndNormalize
		final PageRequest pageRequest = pageService.getPageRequest(normalized.pagination(), Direction.DESC, OrchestrationJob.SORTABLE_FIELDS_BY, OrchestrationJob.DEFAULT_SORT_FIELD, origin);

		final Page<OrchestrationJob> results = jobDbService.query(new OrchestrationJobFilter(normalized), pageRequest);

		return dtoConverter.convertOrchestrationJobPageToHistoryDTO(results);
	}
}
