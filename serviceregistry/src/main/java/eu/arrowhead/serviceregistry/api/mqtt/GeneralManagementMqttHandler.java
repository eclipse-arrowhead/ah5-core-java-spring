package eu.arrowhead.serviceregistry.api.mqtt;

import java.security.InvalidParameterException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.type.TypeReference;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.mqtt.MqttStatus;
import eu.arrowhead.common.mqtt.handler.MqttTopicHandler;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;
import eu.arrowhead.common.service.ConfigService;
import eu.arrowhead.common.service.LogService;
import eu.arrowhead.dto.KeyValuesDTO;
import eu.arrowhead.dto.LogEntryListResponseDTO;
import eu.arrowhead.dto.LogRequestDTO;
import eu.arrowhead.serviceregistry.ServiceRegistryConstants;

@Service
@ConditionalOnProperty(name = Constants.MQTT_API_ENABLED, matchIfMissing = false)
public class GeneralManagementMqttHandler extends MqttTopicHandler {

	//=================================================================================================
	// members

	@Autowired
	private LogService logService;

	@Autowired
	private ConfigService configService;

	private final Logger logger = LogManager.getLogger(getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String topic() {
		return ServiceRegistryConstants.MQTT_API_GENERAL_MANAGEMENT_TOPIC;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void handle(final MqttRequestModel request) throws ArrowheadException {
		logger.debug("ManagementMqttHandler.handle started");
		Assert.isTrue(request.getRequestTopic().equals(topic()), "MQTT topic-handler mismatch");

		MqttStatus responseStatus = MqttStatus.OK;
		Object responsePayload = null;

		switch (request.getOperation()) {
		case Constants.SERVICE_OP_GET_LOG:
			final LogRequestDTO getLogDTO = readPayload(request.getPayload(), LogRequestDTO.class);
			responsePayload = getLog(getLogDTO);
			break;

		case Constants.SERVICE_OP_GET_CONFIG:
			final List<String> getConfigDTO = readPayload(request.getPayload(), new TypeReference<List<String>>() { });
			responsePayload = getConfig(getConfigDTO);
			break;

		default:
			throw new InvalidParameterException("Unknown operation: " + request.getOperation());
		}

		successResponse(request, responseStatus, responsePayload);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private LogEntryListResponseDTO getLog(final LogRequestDTO dto) {
		logger.debug("ManagementMqttHandler.getLog started");
		return logService.getLogEntries(dto, topic());
	}

	//-------------------------------------------------------------------------------------------------
	private KeyValuesDTO getConfig(final List<String> dto) {
		logger.debug("ManagementMqttHandler.getConfig started");
		return configService.getConfig(dto, ServiceRegistryConstants.FORBIDDEN_KEYS, topic());
	}
}