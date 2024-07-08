package eu.arrowhead.serviceregistry.api.http;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.common.Constants;
import eu.arrowhead.serviceregistry.ServiceRegistryConstants;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping(ServiceRegistryConstants.HTTP_API_DEVICE_DISCOVERY_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class DeviceDiscoveryAPI {

	// TODO: implement the following endpoints (all endpoints are public)

	// register-device operation: POST /register (201/200)
	// you can register the same device instance twice (everything the same name/metadata/addresses) => no overwrite
	// if anything is changed, then throw an error

	// lookup-device operation: POST /lookup (200)

	// revoke-device operation: DELETE /revoke (200/204 depending on actual delete)
	// you can delete not-existing device => nothing happens
}
