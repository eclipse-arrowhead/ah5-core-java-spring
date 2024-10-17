package eu.arrowhead.serviceregistry.api.mqtt;

import java.security.InvalidParameterException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.mqtt.handler.MqttTopicHandler;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;
import eu.arrowhead.serviceregistry.ServiceRegistryConstants;

@Service
public class SystemDiscoveryMqttHandler extends MqttTopicHandler {

	//=================================================================================================
	// members

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
	public void handle(final MqttRequestModel request) {
		logger.debug("SystemDiscoveryMqttHandler.handle started");
		Assert.isTrue(request.getRequestTopic().equals(topic()), "MQTT topic-handler mismatch");

		switch (request.getOperation()) {
		case Constants.SERVICE_OP_REGISTER:
			// TODO
			break;

		case Constants.SERVICE_OP_LOOKUP:
			// TODO
			break;

		case Constants.SERVICE_OP_REVOKE:
			// TODO
			break;

		default:
			throw new InvalidParameterException("Unknown operation: " + request.getOperation());
		}
	}
}
