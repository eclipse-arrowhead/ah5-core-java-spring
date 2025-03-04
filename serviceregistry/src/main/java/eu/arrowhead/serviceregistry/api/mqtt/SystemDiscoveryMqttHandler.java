package eu.arrowhead.serviceregistry.api.mqtt;

import java.security.InvalidParameterException;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.mqtt.MqttStatus;
import eu.arrowhead.common.mqtt.handler.MqttTopicHandler;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;
import eu.arrowhead.dto.SystemListResponseDTO;
import eu.arrowhead.dto.SystemLookupRequestDTO;
import eu.arrowhead.dto.SystemRegisterRequestDTO;
import eu.arrowhead.dto.SystemRequestDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.serviceregistry.ServiceRegistryConstants;
import eu.arrowhead.serviceregistry.service.SystemDiscoveryService;

@Service
@ConditionalOnProperty(name = Constants.MQTT_API_ENABLED, matchIfMissing = false)
public class SystemDiscoveryMqttHandler extends MqttTopicHandler {

	//=================================================================================================
	// members

	@Autowired
	private SystemDiscoveryService sdService;

	private final Logger logger = LogManager.getLogger(getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String baseTopic() {
		return ServiceRegistryConstants.MQTT_API_SYSTEM_DISCOVERY_TOPIC;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void handle(final MqttRequestModel request) throws ArrowheadException {
		logger.debug("SystemDiscoveryMqttHandler.handle started");
		Assert.isTrue(request.getBaseTopic().equals(baseTopic()), "MQTT topic-handler mismatch");

		MqttStatus responseStatus = MqttStatus.OK;
		Object responsePayload = null;

		switch (request.getOperation()) {
		case Constants.SERVICE_OP_REGISTER:
			final SystemRegisterRequestDTO registerDTO = readPayload(request.getPayload(), SystemRegisterRequestDTO.class);
			final Pair<SystemResponseDTO, MqttStatus> registerResult = register(request.getRequester(), registerDTO);
			responseStatus = registerResult.getRight();
			responsePayload = registerResult.getLeft();
			break;

		case Constants.SERVICE_OP_LOOKUP:
			final SystemLookupRequestDTO lookupDTO = readPayload(request.getPayload(), SystemLookupRequestDTO.class);
			final Boolean verbose = Boolean.valueOf(request.getParams().get("verbose"));
			responsePayload = lookup(lookupDTO, verbose);
			break;

		case Constants.SERVICE_OP_REVOKE:
			responseStatus = revoke(request.getRequester());
			break;

		default:
			throw new InvalidParameterException("Unknown operation: " + request.getOperation());
		}

		successResponse(request, responseStatus, responsePayload);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private Pair<SystemResponseDTO, MqttStatus> register(final String identifiedRequester, final SystemRegisterRequestDTO dto) {
		logger.debug("SystemDiscoveryMqttHandler.register started");

		final Entry<SystemResponseDTO, Boolean> result = sdService.registerSystem(new SystemRequestDTO(identifiedRequester, dto.metadata(), dto.version(), dto.addresses(), dto.deviceName()), baseTopic());

		return Pair.of(result.getKey(), result.getValue() ? MqttStatus.CREATED : MqttStatus.OK);
	}

	//-------------------------------------------------------------------------------------------------
	private SystemListResponseDTO lookup(final SystemLookupRequestDTO dto, final boolean verbose) {
		logger.debug("SystemDiscoveryMqttHandler.lookup started");

		return sdService.lookupSystem(dto, verbose, baseTopic());
	}

	//-------------------------------------------------------------------------------------------------
	private MqttStatus revoke(final String systemName) {
		logger.debug("SystemDiscoveryMqttHandler.revoke started");

		final boolean result = sdService.revokeSystem(systemName, baseTopic());

		return result ? MqttStatus.OK : MqttStatus.NO_CONTENT;
	}
}