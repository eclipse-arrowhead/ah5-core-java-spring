package eu.arrowhead.serviceorchestration.api.http;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.common.Constants;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationConstants;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping(DynamicServiceOrchestrationConstants.HTTP_API_ORCHESTRATION_HISTORY_MANAGEMENT_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class OrchestrationHistoryManagementAPI {

	//=================================================================================================
	// methods

	// TODO query

}
