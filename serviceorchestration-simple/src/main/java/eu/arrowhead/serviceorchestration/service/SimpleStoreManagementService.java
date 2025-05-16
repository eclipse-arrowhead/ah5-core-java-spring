package eu.arrowhead.serviceorchestration.service;


import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.PageService;
import eu.arrowhead.dto.OrchestrationSimpleStoreListRequestDTO;
import eu.arrowhead.dto.OrchestrationSimpleStoreListResponseDTO;
import eu.arrowhead.dto.OrchestrationSimpleStoreQueryRequestDTO;
import eu.arrowhead.dto.OrchestrationSimpleStoreRequestDTO;
import eu.arrowhead.dto.PriorityRequestDTO;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationStore;
import eu.arrowhead.serviceorchestration.jpa.service.SimpleStoreDbService;
import eu.arrowhead.serviceorchestration.service.dto.DTOConverter;
import eu.arrowhead.serviceorchestration.service.validation.SimpleStoreManagementServiceValidation;

@Service
public class SimpleStoreManagementService {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private SimpleStoreManagementServiceValidation validator;

	@Autowired
	private SimpleStoreDbService dbService;

	@Autowired
	private PageService pageService;

	@Autowired
	private DTOConverter dtoConverter;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationSimpleStoreListResponseDTO createSimpleStoreEntries(final OrchestrationSimpleStoreListRequestDTO dto, final String requesterName, final String origin) {
		logger.info("createSimpleStoreEntries started...");

		final List<OrchestrationSimpleStoreRequestDTO> normalized = validator.validateAndNormalizeCreateBulk(dto, origin);
		try {
			final List<OrchestrationStore> created = dbService.createBulk(normalized
					.stream().map(n -> new OrchestrationStore(
							n.consumer(),
							n.serviceInstanceId().split(Constants.NAME_SEPARATOR)[1], // service definition
							n.serviceInstanceId(),
							n.priority(),
							requesterName)).collect(Collectors.toList()));
			return dtoConverter.convertStoreEntityListToResponseListDTO(created);
		} catch (InvalidParameterException ex){
			throw new InvalidParameterException(ex.getMessage(), origin);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationSimpleStoreListResponseDTO querySimpleStoreEntries(final OrchestrationSimpleStoreQueryRequestDTO dto, final String origin) {
		logger.info("querySimpleStoreEntries started...");

		final OrchestrationSimpleStoreQueryRequestDTO normalized = validator.validateAndNormalizeQuery(dto, origin);
		final PageRequest pageRequest = pageService.getPageRequest(normalized.pagination(), Direction.DESC, OrchestrationStore.SORTABLE_FIELDS_BY, OrchestrationStore.DEFAULT_SORT_FIELD, origin);

		try {
			final Page<OrchestrationStore> results = dbService.getPage(
					pageRequest,
					normalized.ids(),
					normalized.consumerNames(),
					normalized.serviceDefinitions(),
					normalized.serviceInstanceIds(),
					normalized.minPriority(),
					normalized.maxPriority(),
					normalized.createdBy());

			return dtoConverter.convertStoreEntityPageToResponseListTO(results);

		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationSimpleStoreListResponseDTO modifyPriorities(final PriorityRequestDTO dto, final String requesterName, final String origin) {
		logger.info("modifyPriorities started...");

		validator.validatePriorityMap(dto, origin);
		try {
			final List<OrchestrationStore> modified = dbService.setPriorities(dto, requesterName);
			return dtoConverter.convertStoreEntityListToResponseListDTO(modified);

		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void removeSimpleStoreEntries(final List<UUID> uuids, final String origin) {
		logger.info("removeSimpleStoreEntries started...");

		validator.validateUUIDList(uuids, origin);

		try {
			dbService.deleteBulk(uuids);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}
}
