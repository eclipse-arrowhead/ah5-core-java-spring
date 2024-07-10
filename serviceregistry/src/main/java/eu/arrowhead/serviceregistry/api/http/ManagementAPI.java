package eu.arrowhead.serviceregistry.api.http;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.common.Constants;
import eu.arrowhead.dto.ErrorMessageDTO;
import eu.arrowhead.dto.ServiceDefinitionListRequestDTO;
import eu.arrowhead.dto.ServiceDefinitionListResponseDTO;
import eu.arrowhead.serviceregistry.ServiceRegistryConstants;
import eu.arrowhead.serviceregistry.service.ManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping(ServiceRegistryConstants.HTTP_API_MANAGEMENT_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class ManagementAPI {

	// TODO: implement the following endpoints

	//=================================================================================================
	// members

	@Autowired
	private ManagementService mgmtService;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

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

	// query-systems POST /systems (query param verbose)
	// * paging: page, size, direction, sort
	// * filter to: name list, metadata requirement list, version list, address list, address type, device name list

	// create-systems POST /systems(bulk)

	// update-systems PUT /systems(bulk) -> system name can't be changed

	// remove-systems DELETE /systems(list of system names as query params)

	// SERVICES DEFINITIONS

	// get-service-definitions GET /service-definitions

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Returns the created service definition entries")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_CREATED, description = Constants.SWAGGER_HTTP_201_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ServiceDefinitionListResponseDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_BAD_REQUEST, description = Constants.SWAGGER_HTTP_400_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_UNAUTHORIZED, description = Constants.SWAGGER_HTTP_401_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_FORBIDDEN, description = Constants.SWAGGER_HTTP_403_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_INTERNAL_SERVER_ERROR, description = Constants.SWAGGER_HTTP_500_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) })
	})
	@ResponseStatus(code = HttpStatus.CREATED)
	@PostMapping(path = ServiceRegistryConstants.HTTP_API_OP_SERVICE_DEFINITION_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody ServiceDefinitionListResponseDTO createServiceDefinitions(@RequestBody final ServiceDefinitionListRequestDTO dto) {
		logger.debug("createServiceDefinitions started");

		final String origin = HttpMethod.POST.name() + " " + ServiceRegistryConstants.HTTP_API_MANAGEMENT_PATH + ServiceRegistryConstants.HTTP_API_OP_SERVICE_DEFINITION_PATH;
		return mgmtService.createServiceDefinitions(dto, origin);

	}

	// remove-service-definitions DELETE /service-definitions(list of service definition names as query params)

	// SERVICE INSTANCES

	// query-service-instances POST /service-instances (query param verbose)
	// * paging: page, size, direction, sort
	// * filter to: instance id list, system name list. service def list, version list, aliveAt, metadata requirement list, interface name list, policy list

	// create-service-instances POST /service-instances(bulk)

	// update-service-instances PUT /service-instances(bulk) -> only metadata, expiresAt and interface can be changed

	// remove-service-instances DELETE /service-instances(list of service instance ids as query params)

	// INTERFACE TEMPLATES

	// query-interface-templates POST /interface-templates
	// * paging: page, size, direction, sort
	// * filter to: name list, protocol list

	// create-interface-templates POST /interface-templates

	// remove-interface-templates DELETE /interface-templates(list of interface template names as query params)
}
