package eu.arrowhead.serviceorchestration.service.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.PageValidator;
import eu.arrowhead.common.service.validation.name.NameValidator;
import eu.arrowhead.dto.OrchestrationSimpleStoreListRequestDTO;
import eu.arrowhead.dto.OrchestrationSimpleStoreQueryRequestDTO;
import eu.arrowhead.dto.OrchestrationSimpleStoreRequestDTO;
import eu.arrowhead.dto.PriorityRequestDTO;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationStore;
import eu.arrowhead.serviceorchestration.service.normalization.SimpleStoreManagementServiceNormalization;

@Service
public class SimpleStoreManagementServiceValidation {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private SimpleStoreManagementServiceNormalization normalizer;
	
	@Autowired
	private NameValidator nameValidator;

	@Autowired
	private PageValidator pageValidator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationSimpleStoreRequestDTO> validateAndNormalizeCreateBulk(final OrchestrationSimpleStoreListRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeCreateBulk started...");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is null", origin);
		}

		if (Utilities.containsNull(dto.candidates())) {
			throw new InvalidParameterException("Request payload contains null element", origin);
		}

		final List<OrchestrationSimpleStoreRequestDTO> normalized = new ArrayList<OrchestrationSimpleStoreRequestDTO>(dto.candidates().size());

		try {
			dto.candidates().forEach(c -> normalized.add(validateAndNormalizeCreate(c)));
			checkDuplicates(normalized);

		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationSimpleStoreQueryRequestDTO validateAndNormalizeQuery(final OrchestrationSimpleStoreQueryRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeQuery started...");

		validateQuery(dto, origin);

		final OrchestrationSimpleStoreQueryRequestDTO normalized = normalizer.normalizeQuery(dto);
		try {
			//TODO: replace these with the new implementation
			if (normalized.consumerNames() != null) {
				normalized.consumerNames().forEach(c -> nameValidator.validateName(c));
			}

			if (normalized.serviceDefinitions() != null) {
				normalized.serviceDefinitions().forEach(s -> nameValidator.validateName(s));
			}

			if (normalized.serviceInstanceIds() != null) {
				normalized.serviceInstanceIds().forEach(s -> nameValidator.validateServiceInstanceId(s));
			}

			if (normalized.createdBy() != null) {
				nameValidator.validateName(normalized.createdBy());
			}
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public void validatePriorityMap(final PriorityRequestDTO dto, final String origin) {
		logger.info("validatePriorityMap started...");

		if (dto == null) {
			throw new InvalidParameterException("Priority map is null!", origin);
		}

		if (dto.containsKey(null)) {
			throw new InvalidParameterException("Priority map conatains null key!", origin);
		}

		if (dto.containsValue(null)) {
			throw new InvalidParameterException("Priority map contains null value!", origin);
		}

		final Collection<Integer> priorities = dto.values();
		for (final Integer p : priorities) {
			if (p < 0) {
				throw new InvalidParameterException("Invalid priority: " + p);
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validateUUIDList(final List<UUID> uuids, final String origin) {
		logger.info("validateUUIDList started...");

		if (Utilities.isEmpty(uuids)) {
			throw new InvalidParameterException("UUID list is empty!", origin);
		}

		if (Utilities.containsNull(uuids)) {
			throw new InvalidParameterException("UUID list contains null!", origin);
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private OrchestrationSimpleStoreRequestDTO validateAndNormalizeCreate(final OrchestrationSimpleStoreRequestDTO dto) {
		Assert.notNull(dto, "DTO is null!");

		validateCreate(dto);

		final OrchestrationSimpleStoreRequestDTO normalizedDto = normalizer.normalizeCreate(dto);

		nameValidator.validateName(dto.consumer());

		nameValidator.validateServiceInstanceId(dto.serviceInstanceId());
		return normalizedDto;
	}

	//-------------------------------------------------------------------------------------------------
	private void validateCreate(final OrchestrationSimpleStoreRequestDTO dto) {
		Assert.notNull(dto, "DTO is null!");

		// consumer
		if (Utilities.isEmpty(dto.consumer())) {
			throw new InvalidParameterException("consumer is null or empty");
		}

		// service instance id
		if (Utilities.isEmpty(dto.serviceInstanceId())) {
			throw new InvalidParameterException("serviceInstanceId is null or empty");
		}

		// priority
		if (dto.priority() == null) {
			throw new InvalidParameterException("priority is null");
		}
		if (dto.priority() < 0) {
			throw new InvalidParameterException("priority should be a non-negative integer");
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void checkDuplicates(final List<OrchestrationSimpleStoreRequestDTO> candidates) {
		// consumer, serviceInstanceId and priority must be unique
		final List<Triple<String, String, Integer>> existing = new ArrayList<Triple<String, String, Integer>>();
		for (final OrchestrationSimpleStoreRequestDTO candidate : candidates) {
			final Triple<String, String, Integer> current = Triple.of(candidate.consumer(), candidate.serviceInstanceId(), candidate.priority());
			if (existing.contains(current)) {
				throw new InvalidParameterException("The following fields must be unique: consumer, serviceInstanceId, priority");
			}
			else {
				existing.add(current);
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateQuery(final OrchestrationSimpleStoreQueryRequestDTO dto, final String origin) {
		Assert.notNull(dto, "dto is null");

		if (dto.pagination() == null) {
			throw new InvalidParameterException("Page is null!", origin);
		}
		else {
			pageValidator.validatePageParameter(dto.pagination(), OrchestrationStore.SORTABLE_FIELDS_BY, origin);
		}

		if (Utilities.allEmpty(dto.ids(), dto.consumerNames(), dto.serviceDefinitions(),
				dto.serviceInstanceIds()) && Utilities.isEmpty(dto.createdBy())) {
			throw new InvalidParameterException("At least one of the following fields must be specified: "
					+ "ids, consumerNames, serviceDefinitions, serviceInstanceIds, createdBy.");
		}

		if (!Utilities.isEmpty(dto.ids()) && Utilities.containsNull(dto.ids())) {
			throw new InvalidParameterException("Id list contains null or empty element!", origin);
		}

		if (!Utilities.isEmpty(dto.consumerNames()) && Utilities.containsNullOrEmpty(dto.consumerNames())) {
			throw new InvalidParameterException("Consumer name list contains null or empty element!", origin);
		}

		if (!Utilities.isEmpty(dto.serviceDefinitions()) && Utilities.containsNullOrEmpty(dto.serviceDefinitions())) {
			throw new InvalidParameterException("Service definition name list contains null or empty element!", origin);
		}

		if (!Utilities.isEmpty(dto.serviceInstanceIds()) && Utilities.containsNullOrEmpty(dto.serviceInstanceIds())) {
			throw new InvalidParameterException("Service instance id list contains null or empty element!", origin);
		}

		if (dto.minPriority() != null && dto.minPriority() < 0) {
			throw new InvalidParameterException("Invalid minimum priority: should be a non-negative integer.", origin);
		}

		if (dto.maxPriority() != null && dto.maxPriority() < 0) {
			throw new InvalidParameterException("Invalid maximum priority: should be a non-negative integer.", origin);
		}

		if (dto.maxPriority() != null && dto.minPriority() != null && dto.minPriority() > dto.maxPriority()) {
			throw new InvalidParameterException("Minimum priority should not be greater than maxim priority!");
		}
	}
}
