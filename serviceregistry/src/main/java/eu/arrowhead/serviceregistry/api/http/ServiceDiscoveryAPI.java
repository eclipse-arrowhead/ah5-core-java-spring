package eu.arrowhead.serviceregistry.api.http;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.common.Constants;
import eu.arrowhead.serviceregistry.ServiceRegistryConstants;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping(ServiceRegistryConstants.HTTP_API_SERVICE_DISCOVERY_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class ServiceDiscoveryAPI {

	// TODO: implement the following endpoints (all endpoints are public)

	// register-service operation: POST /register (201)
	// you can register the same service instance (system, service def, version) twice => the new one overwrite the old (serviceinstanceid stays the same)
	// when register you can reference an existing interface template with the actual values and policy, or
	//               (only in case of open interface policy) you can add a new interface template: name, protocol, policy and actual values (actual values are using to create the template, all fields
	//               are mandatory)

	// lookup-service operation: POST /lookup (200) (query param verbose)

	// revoke-service operation: DELETE /revoke (200/204 depending on actual delete)
	// you can delete not-existing service instance => nothing happens
}
