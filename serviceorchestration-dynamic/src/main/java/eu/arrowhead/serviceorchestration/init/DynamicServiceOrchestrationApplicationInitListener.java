package eu.arrowhead.serviceorchestration.init;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.common.init.ApplicationInitListener;
import eu.arrowhead.common.service.util.ServiceInterfaceAddressPropertyProcessor;
import eu.arrowhead.dto.KeyValuesDTO;
import eu.arrowhead.serviceorchestration.service.thread.PushOrchestrationThread;

@Component
public class DynamicServiceOrchestrationApplicationInitListener extends ApplicationInitListener {

	//=================================================================================================
	// members

	@Autowired
	private ArrowheadHttpService arrowheadHttpService;

	@Autowired
	private PushOrchestrationThread pushOrchestrationThread;

	@Autowired
	private ServiceInterfaceAddressPropertyProcessor serviceInterfaceAddressPropertyProcessor;

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void customInit(final ContextRefreshedEvent event) throws InterruptedException {
		logger.debug("customInit started...");

		if (standaloneMode) {
			return;
		}

		// TODO implement

		initServiceInterfaceAddressTypeFilter();

		pushOrchestrationThread.start();
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void initServiceInterfaceAddressTypeFilter() {
		logger.debug("initServiceInterfaceAddressTypeFilter started...");

		MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>(Map.of(Constants.SERVICE_OP_GET_CONFIG_REQ_PARAM, List.of(Constants.SERVICE_ADDRESS_ALIAS)));

		final KeyValuesDTO srConfigDTO = arrowheadHttpService.consumeService(Constants.SERVICE_DEF_GENERAL_MANAGEMENT, Constants.SERVICE_OP_GET_CONFIG, Constants.SYS_NAME_SERVICE_REGISTRY, KeyValuesDTO.class, queryParams);

		final String serviceAddressAliasListStr = srConfigDTO.map().get(Constants.SERVICE_ADDRESS_ALIAS);

		if (!Utilities.isEmpty(serviceAddressAliasListStr)) {
			final List<String> serviceAddressAliasList = Arrays.asList(serviceAddressAliasListStr.split(","));
			serviceInterfaceAddressPropertyProcessor.setAddressAliasNames(serviceAddressAliasList);
		}
	}

}
