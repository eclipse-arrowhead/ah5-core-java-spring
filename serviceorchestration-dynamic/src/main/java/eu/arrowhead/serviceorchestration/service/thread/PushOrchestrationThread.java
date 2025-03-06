package eu.arrowhead.serviceorchestration.service.thread;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.http.HttpService;
import eu.arrowhead.common.http.HttpUtilities;
import eu.arrowhead.common.mqtt.MqttQoS;
import eu.arrowhead.common.mqtt.MqttService;
import eu.arrowhead.dto.MqttNotifyTemplate;
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.dto.enums.OrchestrationFlag;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationConstants;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationSystemInfo;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationJobDbService;
import eu.arrowhead.serviceorchestration.jpa.service.SubscriptionDbService;
import eu.arrowhead.serviceorchestration.service.enums.NotifyProtocol;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;
import eu.arrowhead.serviceorchestration.service.utils.InterCloudServiceOrchestration;
import eu.arrowhead.serviceorchestration.service.utils.LocalServiceOrchestration;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;

@Component
public class PushOrchestrationThread extends Thread {

	//=================================================================================================
	// members

	@Resource(name = DynamicServiceOrchestrationConstants.JOB_QUEUE_PUSH_ORCHESTRATION)
	private BlockingQueue<UUID> pushOrchJobQueue;

	@Autowired
	private OrchestrationJobDbService orchJobDbService;

	@Autowired
	private SubscriptionDbService subscriptionDbService;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private LocalServiceOrchestration localOrch;

	@Autowired
	private InterCloudServiceOrchestration interCloudOrch;

	@Autowired
	private HttpService httpService;

	@Autowired(required = false)
	private MqttService mqttService;

	@Autowired
	private DynamicServiceOrchestrationSystemInfo sysInfo;

	private boolean doWork = false;

	private ThreadPoolExecutor threadpool;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public void run() {
		logger.debug("run started...");

		doWork = true;
		while (doWork) {
			try {
				final UUID jobId = pushOrchJobQueue.take();
				threadpool.execute(() -> {
					doPushOrchestration(jobId);
				});

			} catch (final InterruptedException ex) {
				logger.debug(ex);
				logger.error(ex.getMessage());
				interrupt();
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void interrupt() {
		logger.debug("interrupt started...");
		doWork = false;
		super.interrupt();
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@PostConstruct
	private void init() {
		logger.debug("init started...");

		threadpool = (ThreadPoolExecutor) Executors.newFixedThreadPool(sysInfo.getPushOrchestrationMaxThread());
	}

	//-------------------------------------------------------------------------------------------------
	private void doPushOrchestration(final UUID jobId) {
		logger.debug("doPushOrchestration started...");

		try {
			final Optional<OrchestrationJob> jobOpt = orchJobDbService.getById(jobId);
			if (jobOpt.isEmpty()) {
				logger.error("Orchestration push job doesn't exists: " + jobId);
				return;
			}

			final OrchestrationJob job = orchJobDbService.setStatus(jobId, OrchestrationJobStatus.IN_PROGRESS, null);

			if (Utilities.isEmpty(job.getSubscriptionId())) {
				final String errorMsg = "Orchestration job " + jobId + " has no subscription id.";
				logger.error(errorMsg);
				orchJobDbService.setStatus(jobId, OrchestrationJobStatus.ERROR, errorMsg);
				return;
			}

			final Optional<Subscription> subscriptionOpt = subscriptionDbService.get(UUID.fromString(job.getSubscriptionId()));
			if (subscriptionOpt.isEmpty()) {
				final String errorMsg = "Orchestration job " + jobId + " has no subscription with " + job.getSubscriptionId() + " subscription id.";
				logger.error(errorMsg);
				orchJobDbService.setStatus(jobId, OrchestrationJobStatus.ERROR, errorMsg);
				return;
			}

			final Subscription subscription = subscriptionOpt.get();

			final OrchestrationRequestDTO orchestrationRequest = mapper.readValue(subscription.getOrchestrationRequest(), OrchestrationRequestDTO.class);
			final OrchestrationForm orchestrationForm = new OrchestrationForm(job.getRequesterSystem(), job.getTargetSystem(), orchestrationRequest);

			final OrchestrationResponseDTO result;
			if (orchestrationForm.hasFlag(OrchestrationFlag.ONLY_INTERCLOUD)) {
				result = interCloudOrch.doInterCloudServiceOrchestration(jobId, orchestrationForm);
			} else {
				result = localOrch.doLocalServiceOrchestration(jobId, orchestrationForm);
			}

			if (subscription.getNotifyProtocol().equals(NotifyProtocol.HTTP.name())
					|| subscription.getNotifyProtocol().equals(NotifyProtocol.HTTPS.name())) {
				notifyViaHttp(subscription.getId(), subscription.getNotifyProtocol(), subscription.getNotifyProperties(), result);
				return;
			}

			if (subscription.getNotifyProtocol().equals(NotifyProtocol.MQTT.name())
					|| subscription.getNotifyProtocol().equals(NotifyProtocol.MQTTS.name())) {
				notifyViaMqtt(subscription.getId(), subscription.getTargetSystem(), subscription.getNotifyProperties(), result);
				return;
			}

		} catch (final Exception ex) {
			logger.debug(ex);
			logger.error(ex.getMessage());
			orchJobDbService.setStatus(jobId, OrchestrationJobStatus.ERROR, ex.getMessage());
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void notifyViaHttp(final UUID subscriptionId, final String protocol, final String properties, final OrchestrationResponseDTO result) {
		logger.debug("notifyViaHttp starterd...");

		final Map<String, String> propsMap = readNotifyProperties(properties);
		final String address = propsMap.get(DynamicServiceOrchestrationConstants.NOTIFY_KEY_ADDRESS);
		final int port = Integer.parseInt(propsMap.get(DynamicServiceOrchestrationConstants.NOTIFY_KEY_PORT));
		final String method = propsMap.get(DynamicServiceOrchestrationConstants.NOTIFY_KEY_METHOD);
		final String path = propsMap.get(DynamicServiceOrchestrationConstants.NOTIFY_KEY_PATH);

		try {
			httpService.sendRequest(HttpUtilities.createURI(protocol, address, port, path), HttpMethod.valueOf(method), Void.class, result);
		} catch (final Exception ex) {
			logger.debug(ex);
			throw new ArrowheadException("Error occured while sending push orchestration via HTTP to subscription: " + subscriptionId.toString() + ". Reason: " + ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void notifyViaMqtt(final UUID subscriptionId, final String targetSystem, final String properties, final OrchestrationResponseDTO result) {
		logger.debug("notifyViaMqtt started...");

		if (!sysInfo.isMqttApiEnabled()) {
			logger.error("Orchestration push notification via MQTT is required for subscripiton: " + subscriptionId.toString() + ", but MQTT is not enabled");
			return;
		}

		// Sending MQTT notification is supported only via the main broker. Orchestrator does not connect to unknown brokers to send the orchestration results.
		final MqttClient mqttClient = mqttService.client(Constants.MQTT_SERVICE_PROVIDING_BROKER_CONNECT_ID);
		final Map<String, String> propsMap = readNotifyProperties(properties);

		try {
			final MqttNotifyTemplate template = new MqttNotifyTemplate(targetSystem, sysInfo.getSystemName(), result);
			final MqttMessage msg = new MqttMessage(mapper.writeValueAsBytes(template));
			msg.setQos(MqttQoS.EXACTLY_ONCE.value());
			mqttClient.publish(propsMap.get(DynamicServiceOrchestrationConstants.NOTIFY_KEY_TOPIC), msg);
		} catch (final Exception ex) {
			logger.debug(ex);
			throw new ArrowheadException("Error occured while sending push orchestration via MQTT to subscription: " + subscriptionId.toString() + ". Reason: " + ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	private Map<String, String> readNotifyProperties(final String properties) {
		logger.debug("readNotifyProperties starterd...");

		final TypeReference<Map<String, String>> typeReference = new TypeReference<Map<String, String>>() {
		};

		try {
			return mapper.readValue(properties, typeReference);
		} catch (final JsonProcessingException ex) {
			logger.debug(ex);
			throw new IllegalArgumentException(ex.getMessage());
		}
	}
}
