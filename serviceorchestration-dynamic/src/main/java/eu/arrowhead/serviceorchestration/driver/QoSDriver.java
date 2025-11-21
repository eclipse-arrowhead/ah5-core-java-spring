/*******************************************************************************
 *
 * Copyright (c) 2025 AITIA
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 *
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  	AITIA - implementation
 *  	Arrowhead Consortia - conceptualization
 *
 *******************************************************************************/
package eu.arrowhead.serviceorchestration.driver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.util.UriComponents;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.common.http.HttpService;
import eu.arrowhead.common.http.HttpUtilities;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.intf.properties.PropertyValidatorType;
import eu.arrowhead.common.intf.properties.PropertyValidators;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.QoSEvaluationFilterResponseDTO;
import eu.arrowhead.dto.QoSEvaluationRequestDTO;
import eu.arrowhead.dto.QoSEvaluationSortResponseDTO;
import eu.arrowhead.dto.QoSRequirementDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceResponseDTO;
import eu.arrowhead.dto.ServiceInstanceListResponseDTO;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.enums.QoSOperation;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationConstants;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationSystemInfo;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationCandidate;

@Service
public class QoSDriver {

	//=================================================================================================
	// members

	@Autowired
	private DynamicServiceOrchestrationSystemInfo sysInfo;

	@Autowired
	private ArrowheadHttpService ahHttpService;

	@Autowired
	private HttpService httpService;

	@Autowired
	private PropertyValidators validators;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationCandidate> doQoSCompliance(final List<OrchestrationCandidate> candidates, final List<QoSRequirementDTO> qosRequirements, final Set<String> warnings) {
		logger.debug("doQoSCompliance started...");

		// TODO implement when QoS Evaluator is ready
		final List<String> systemNames = candidates.stream().map(c -> c.getServiceInstance().provider().name()).toList();
		List<OrchestrationCandidate> compliedSystem = new ArrayList<>(candidates);

		for (final QoSRequirementDTO qosReq : qosRequirements) {
			final ServiceInstanceResponseDTO evaluator = findQoSEvaluator(qosReq.type());
			if (evaluator == null) {
				warnings.add(DynamicServiceOrchestrationConstants.ORCH_WARN_QOS_COMPLIANCE_FAILURE);
				continue;
			}

			List<String> evaluationResult = null;
			if (qosReq.operation().equalsIgnoreCase(QoSOperation.SORT.name())) {
				final QoSEvaluationSortResponseDTO result = consumeQosEvaluationService(evaluator, qosReq.operation().toLowerCase(),
						new QoSEvaluationRequestDTO(systemNames, qosReq.requirements()), QoSEvaluationSortResponseDTO.class);
				if (result != null) {
					// TODO save qosResults into DB to orch job
					evaluationResult = result.sortedProviders();
				}
			} else {
				final QoSEvaluationFilterResponseDTO result = consumeQosEvaluationService(evaluator, qosReq.operation().toLowerCase(),
						new QoSEvaluationRequestDTO(systemNames, qosReq.requirements()), QoSEvaluationFilterResponseDTO.class);
				if (result != null) {
					// TODO save qosResults into DB to orch job
					evaluationResult = result.passedProviders();
				}
			}

			if (evaluationResult == null) {
				warnings.add(DynamicServiceOrchestrationConstants.ORCH_WARN_QOS_COMPLIANCE_FAILURE);
				continue;
			}

			final List<OrchestrationCandidate> tempList = new ArrayList<>(evaluationResult.size());
			for (final String sysName : evaluationResult) {
				for (final OrchestrationCandidate candidate : compliedSystem) {
					if (sysName.equalsIgnoreCase(candidate.getServiceInstance().provider().name())) {
						tempList.add(candidate);
						break;
					}
				}
			}

			compliedSystem = tempList;
		}

		return candidates;
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceInstanceResponseDTO findQoSEvaluator(final String qosEvaluationType) {
		logger.debug("findQoSEvaluator started...");

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put(Constants.METADATA_KEY_EVALUATION_TYPE, qosEvaluationType);

		final ServiceInstanceLookupRequestDTO lookupDTO = new ServiceInstanceLookupRequestDTO.Builder()
				.serviceDefinitionName(Constants.SERVICE_DEF_QUALITY_EVALUATION)
				.metadataRequirements(metadataReq)
				.policies(sysInfo.isSslEnabled() ? List.of(ServiceInterfacePolicy.CERT_AUTH.name(), ServiceInterfacePolicy.NONE.name()) : List.of(ServiceInterfacePolicy.NONE.name()))
				.interfaceTemplateNames(sysInfo.isSslEnabled() ? List.of(Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME, Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME) : List.of(Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME))
				.build();

		try {
			final ServiceInstanceListResponseDTO instances = ahHttpService.consumeService(
					Constants.SERVICE_DEF_SERVICE_DISCOVERY,
					Constants.SERVICE_OP_LOOKUP,
					Constants.SYS_NAME_SERVICE_REGISTRY,
					ServiceInstanceListResponseDTO.class,
					lookupDTO,
					new LinkedMultiValueMap<>(Map.of(Constants.VERBOSE, List.of(Boolean.FALSE.toString()))));

			if (Utilities.isEmpty(instances.entries())) {
				return null;
			}
			return instances.entries().get(ThreadLocalRandom.current().nextInt(instances.entries().size()));

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			return null;
		}
	}

	//-------------------------------------------------------------------------------------------------
	private <T> T consumeQosEvaluationService(final ServiceInstanceResponseDTO evaluator, final String qosOperation, final QoSEvaluationRequestDTO qosEvalRequest, final Class<T> responseType) {
		logger.debug("consumeQosEvaluationService started...");

		ServiceInstanceInterfaceResponseDTO interf = evaluator.interfaces().getFirst();
		for (final ServiceInstanceInterfaceResponseDTO i : evaluator.interfaces()) {
			if (sysInfo.isSslEnabled() && i.protocol().equalsIgnoreCase(Constants.HTTPS)) {
				interf = i;
			}
		}
		final HttpInterfaceModel interfaceModel = createHttpInterfaceModel(interf.templateName(), interf.properties());

		final UriComponents uri = HttpUtilities.createURI(interfaceModel.protocol(), interfaceModel.accessAddresses().getFirst(), interfaceModel.accessPort(),
				interfaceModel.basePath() + interfaceModel.operations().get(qosOperation).path());
		final HttpMethod method = HttpMethod.valueOf(interfaceModel.operations().get(qosOperation).method());
		final String authorizationHeader = HttpUtilities.calculateAuthorizationHeader(sysInfo);
		final Map<String, String> headers = new HashMap<>();
		if (authorizationHeader != null) {
			headers.put(HttpHeaders.AUTHORIZATION, authorizationHeader);
		}

		try {
			return httpService.sendRequest(uri, method, responseType, qosEvalRequest, null, headers);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			return null;
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	private HttpInterfaceModel createHttpInterfaceModel(final String templateName, final Map<String, Object> properties) {

		// access addresses
		final List<String> accessAddresses = (List<String>) properties.get(HttpInterfaceModel.PROP_NAME_ACCESS_ADDRESSES);

		// access port
		final int accessPort = (int) properties.get(HttpInterfaceModel.PROP_NAME_ACCESS_PORT);

		// base path
		final String basePath = (String) properties.get(HttpInterfaceModel.PROP_NAME_BASE_PATH);

		// operations
		if (!properties.containsKey(HttpInterfaceModel.PROP_NAME_OPERATIONS)) {
			return null;
		}

		final Map<String, HttpOperationModel> operations = (Map<String, HttpOperationModel>) validators.getValidator(PropertyValidatorType.HTTP_OPERATIONS)
				.validateAndNormalize(properties.get(HttpInterfaceModel.PROP_NAME_OPERATIONS));

		// create the interface model
		final HttpInterfaceModel model = new HttpInterfaceModel.Builder(templateName)
				.accessAddresses(accessAddresses)
				.accessPort(accessPort)
				.basePath(basePath)
				.operations(operations)
				.build();

		return model;
	}
}
