package eu.arrowhead.serviceregistry.api.mqtt;

import java.security.InvalidParameterException;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.mqtt.MqttStatus;
import eu.arrowhead.common.mqtt.handler.MqttTopicHandler;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;
import eu.arrowhead.dto.SystemListResponseDTO;
import eu.arrowhead.dto.SystemLookupRequestDTO;
import eu.arrowhead.dto.SystemRequestDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.serviceregistry.ServiceRegistryConstants;
import eu.arrowhead.serviceregistry.service.SystemDiscoveryService;

@Service
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
	public String topic() {
		return ServiceRegistryConstants.MQTT_API_SYSTEM_DISCOVERY_TOPIC;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void handle(final MqttRequestModel request) throws ArrowheadException {
		logger.debug("SystemDiscoveryMqttHandler.handle started");
		Assert.isTrue(request.getRequestTopic().equals(topic()), "MQTT topic-handler mismatch");

		MqttStatus responseStatus = MqttStatus.OK;
		Object responsePayload = null;

		switch (request.getOperation()) {
		case Constants.SERVICE_OP_REGISTER:
			final SystemRequestDTO registerDTO = readPayload(request.getPayload(), SystemRequestDTO.class);
			final Pair<SystemResponseDTO, MqttStatus> registerResult = register(registerDTO);
			responseStatus = registerResult.getRight();
			responsePayload = registerResult.getLeft();
			break;

		case Constants.SERVICE_OP_LOOKUP:
			final SystemLookupRequestDTO lookupDTO = readPayload(request.getPayload(), SystemLookupRequestDTO.class);
			final Boolean verbose = Boolean.valueOf(request.getParams().get("verbose"));
			final Pair<SystemListResponseDTO, MqttStatus> lookupResult = lookup(lookupDTO, verbose);
			responseStatus = lookupResult.getRight();
			responsePayload = lookupResult.getLeft();
			break;

		case Constants.SERVICE_OP_REVOKE:
			final String name = readPayload(request.getPayload(), String.class);
			responseStatus = revoke(name);
			break;

		default:
			throw new InvalidParameterException("Unknown operation: " + request.getOperation());
		}

		successResponse(request, responseStatus, responsePayload);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private Pair<SystemResponseDTO, MqttStatus> register(final SystemRequestDTO dto) {
		logger.debug("SystemDiscoveryMqttHandler.register started");

		final Entry<SystemResponseDTO, Boolean> result = sdService.registerSystem(dto, topic());
		return Pair.of(result.getKey(), result.getValue() ? MqttStatus.CREATED : MqttStatus.OK);
	}

	//-------------------------------------------------------------------------------------------------
	private Pair<SystemListResponseDTO, MqttStatus> lookup(final SystemLookupRequestDTO dto, final boolean verbose) {
		logger.debug("SystemDiscoveryMqttHandler.lookup started");

		final SystemListResponseDTO result = sdService.lookupSystem(dto, verbose, topic());
		return Pair.of(result, MqttStatus.OK);
	}

	//-------------------------------------------------------------------------------------------------
	private MqttStatus revoke(final String systemName) {
		logger.debug("SystemDiscoveryMqttHandler.revoke started");

		boolean result = sdService.revokeSystem(systemName, topic());
		return result ? MqttStatus.OK : MqttStatus.NO_CONTENT;
	}
}
