package eu.arrowhead.serviceregistry.api.http;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.common.Constants;
import eu.arrowhead.serviceregistry.ServiceRegistryConstants;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping(ServiceRegistryConstants.HTTP_API_MANAGEMENT_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class ManagementAPI {

	// TODO: implement the following endpoints

	// LOG

	// query-logs operation POST /logs
	// * paging: page, size, direction, sort
	// * filter to: from, to, severity, logger

	// CONFIG

	// get-config GET /config(list of config keys as query params) -> Empty input list means all is required

	// DEVICES

	// query-devices POST /devices
	// * paging: page, size, direction, sort
	// * filter to: device name list, address list, address type, metadata requirement list

	// create-devices POST /devices(bulk)

	// update-devices PUT /devices(bulk) -> device name can't be changed

	// remove-devices DELETE /devices(list of device names as query params)

	// SYSTEMS

	// query-systems POST /systems
	// * paging: page, size, direction, sort
	// * filter to: name list, metadata requirement list, version list, address list, address type, device name list

	// create-systems POST /systems(bulk)

	// update-systems PUT /systems(bulk) -> system name can't be changed

	// remove-systems DELETE /systems(list of system names as query params)

	// SERVICES DEFINITIONS

	// get-service-definitions GET /service-definitions

	// create-service-definitions POST /service-definitions(bulk)

	// remove-service-definitions DELETE /service-definitions(list of service definition names as query params)

	// SERVICE INSTANCES

	// query-service-instances POST /service-instances
	// * paging: page, size, direction, sort
	// * filter to: instance id list, system name list. service def list, version list, aliveAt, metadata requirement list, interface name list, policy list

	// create-service-instances POST /service-instances(bulk)

	// update-service-instances PUT /service-instances(bulk) -> only metadata, expiresAt and interface can be changed

	// remove-service-instances DELETE /service-instances(list of service instance ids as query params)

	// INTERFACE TEMPLATES

	// query-interface-templates POST /interface-templates
	// * paging: page, size, direction, sort
	// * filter to: name list, protocoll list

	// create-interface-templates POST /interface-templates

	// remove-interface-templates DELETE /interface-templates(list of interface template names as query params)
}
