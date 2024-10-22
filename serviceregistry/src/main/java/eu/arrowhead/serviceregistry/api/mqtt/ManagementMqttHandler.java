package eu.arrowhead.serviceregistry.api.mqtt;

import java.security.InvalidParameterException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.mqtt.handler.MqttTopicHandler;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;
import eu.arrowhead.serviceregistry.ServiceRegistryConstants;

@Service
@ConditionalOnProperty(name = Constants.MQTT_API_ENABLED, matchIfMissing = false)
public class ManagementMqttHandler extends MqttTopicHandler {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String topic() {
		return ServiceRegistryConstants.MQTT_API_MANAGEMENT_TOPIC;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void handle(final MqttRequestModel request) throws ArrowheadException {
		logger.debug("ManagementMqttHandler.handle started");
		Assert.isTrue(request.getRequestTopic().equals(topic()), "MQTT topic-handler mismatch");

		switch (request.getOperation()) {
		// TODO

		default:
			throw new InvalidParameterException("Unknown operation: " + request.getOperation());
		}
	}
}
