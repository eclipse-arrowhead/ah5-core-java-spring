package eu.arrowhead.serviceorchestration.api.http;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.common.Constants;
import eu.arrowhead.dto.ErrorMessageDTO;
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionRequestDTO;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationConstants;
import eu.arrowhead.serviceorchestration.api.http.utils.SystemNamePreprocessor;
import eu.arrowhead.serviceorchestration.service.OrchestrationService;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationSubscription;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(DynamicServiceOrchestrationConstants.HTTP_API_ORCHESTRATION_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class OrchestrationAPI {

	//=================================================================================================
	// members

	@Autowired
	private SystemNamePreprocessor sysNamePreprocessor;

	@Autowired
	private OrchestrationService orchService;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Returns the dynamically proceeded orchestration results")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_OK, description = Constants.SWAGGER_HTTP_200_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OrchestrationResponseDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_BAD_REQUEST, description = Constants.SWAGGER_HTTP_400_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_UNAUTHORIZED, description = Constants.SWAGGER_HTTP_401_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_FORBIDDEN, description = Constants.SWAGGER_HTTP_403_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_INTERNAL_SERVER_ERROR, description = Constants.SWAGGER_HTTP_500_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) })
	})
	@PostMapping(path = DynamicServiceOrchestrationConstants.HTTP_API_OP_PULL_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody OrchestrationResponseDTO pull(final HttpServletRequest httpServletRequest, @RequestBody final OrchestrationRequestDTO dto) {
		logger.debug("pull started...");

		final String origin = HttpMethod.POST.name() + " " + DynamicServiceOrchestrationConstants.HTTP_API_ORCHESTRATION_PATH + DynamicServiceOrchestrationConstants.HTTP_API_OP_PULL_PATH;

		final String requesterSystem = sysNamePreprocessor.process(httpServletRequest, origin);
		return orchService.pull(new OrchestrationForm(requesterSystem, dto), origin);
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Returns a subscription id")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_CREATED, description = Constants.SWAGGER_HTTP_201_MESSAGE, content = {
					@Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(implementation = String.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_OK, description = Constants.SWAGGER_HTTP_200_MESSAGE, content = {
					@Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(implementation = String.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_BAD_REQUEST, description = Constants.SWAGGER_HTTP_400_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_UNAUTHORIZED, description = Constants.SWAGGER_HTTP_401_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_FORBIDDEN, description = Constants.SWAGGER_HTTP_403_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_INTERNAL_SERVER_ERROR, description = Constants.SWAGGER_HTTP_500_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) })
	})
	@PostMapping(path = DynamicServiceOrchestrationConstants.HTTP_API_OP_PUSH_SUBSCRIBE_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> pushSubscribe(final HttpServletRequest httpServletRequest, @RequestBody final OrchestrationSubscriptionRequestDTO dto) {
		logger.debug("pushSubscribe started...");

		final String origin = HttpMethod.POST.name() + " " + DynamicServiceOrchestrationConstants.HTTP_API_ORCHESTRATION_PATH + DynamicServiceOrchestrationConstants.HTTP_API_OP_PUSH_SUBSCRIBE_PATH;

		final String requesterSystem = sysNamePreprocessor.process(httpServletRequest, origin);
		final Pair<Boolean, String> result = orchService.pushSubscribe(new OrchestrationSubscription(requesterSystem, dto), origin);
		return new ResponseEntity<String>(result.getRight(), result.getLeft() ? HttpStatus.CREATED : HttpStatus.OK);
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Removes a the subscription by the given id")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_OK, description = Constants.SWAGGER_HTTP_200_MESSAGE),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_BAD_REQUEST, description = Constants.SWAGGER_HTTP_400_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_UNAUTHORIZED, description = Constants.SWAGGER_HTTP_401_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_FORBIDDEN, description = Constants.SWAGGER_HTTP_403_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_INTERNAL_SERVER_ERROR, description = Constants.SWAGGER_HTTP_500_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) })
	})
	@DeleteMapping(path = DynamicServiceOrchestrationConstants.HTTP_API_OP_PUSH_UNSUBSCRIBE_PATH)
	public void pushUnsubscribe(final HttpServletRequest httpServletRequest, @PathVariable final String id) {
		logger.debug("pushUnsubscribe started...");

		final String origin = HttpMethod.POST.name() + " " + DynamicServiceOrchestrationConstants.HTTP_API_ORCHESTRATION_PATH + DynamicServiceOrchestrationConstants.HTTP_API_OP_PUSH_UNSUBSCRIBE_PATH;

		final String requesterSystem = sysNamePreprocessor.process(httpServletRequest, origin);
		orchService.pushUnsubscribe(requesterSystem, id, origin);
	}
}
