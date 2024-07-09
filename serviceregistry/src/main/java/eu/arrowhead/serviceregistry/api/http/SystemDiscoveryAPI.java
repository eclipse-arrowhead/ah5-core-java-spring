package eu.arrowhead.serviceregistry.api.http;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.common.Constants;
import eu.arrowhead.serviceregistry.ServiceRegistryConstants;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping(ServiceRegistryConstants.HTTP_API_SYSTEM_DISCOVERY_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class SystemDiscoveryAPI {

	// TODO: implement the following endpoints (all endpoints are public)

	// register-system operation: POST /register (201/200 depending on actual creation)
	// you can register the same system instance twice (everything the same name/version/metadata/addresses/system-device conn) => no overwrite
	// if anything is changed, then throw an error

	// lookup-system operation: POST /lookup (200) (query param verbose)

	// revoke-system operation: DELETE /revoke (200/204 depending on actual delete)
	// you can delete not-existing system => nothing happens
}
