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
import eu.arrowhead.common.service.LogService;
import eu.arrowhead.dto.DeviceListRequestDTO;
import eu.arrowhead.dto.DeviceListResponseDTO;
import eu.arrowhead.dto.DeviceQueryRequestDTO;
import eu.arrowhead.dto.KeyValuesDTO;
import eu.arrowhead.dto.LogEntryListResponseDTO;
import eu.arrowhead.dto.LogRequestDTO;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.ServiceDefinitionListRequestDTO;
import eu.arrowhead.dto.ServiceDefinitionListResponseDTO;
import eu.arrowhead.dto.ServiceInstanceCreateListRequestDTO;
import eu.arrowhead.dto.ServiceInstanceListResponseDTO;
import eu.arrowhead.dto.ServiceInstanceQueryRequestDTO;
import eu.arrowhead.dto.ServiceInstanceUpdateListRequestDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateListRequestDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateListResponseDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateQueryRequestDTO;
import eu.arrowhead.dto.SystemListRequestDTO;
import eu.arrowhead.dto.SystemListResponseDTO;
import eu.arrowhead.dto.SystemQueryRequestDTO;
import eu.arrowhead.serviceregistry.ServiceRegistryConstants;
import eu.arrowhead.serviceregistry.service.ConfigService;
import eu.arrowhead.serviceregistry.service.ManagementService;

@Service
@ConditionalOnProperty(name = Constants.MQTT_API_ENABLED, matchIfMissing = false)
public class ManagementMqttHandler extends MqttTopicHandler {

	//=================================================================================================
	// members

	@Autowired
	private ManagementService mgmtService;

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
		return ServiceRegistryConstants.MQTT_API_MANAGEMENT_TOPIC;
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

		case Constants.SERVICE_OP_DEVICE_QUERY:
			final DeviceQueryRequestDTO deviceQueryDTO = readPayload(request.getPayload(), DeviceQueryRequestDTO.class);
			responsePayload = deviceQuery(deviceQueryDTO);
			break;

		case Constants.SERVICE_OP_DEVICE_CREATE:
			final DeviceListRequestDTO deviceCreateDTO = readPayload(request.getPayload(), DeviceListRequestDTO.class);
			responsePayload = deviceCreate(deviceCreateDTO);
			responseStatus = MqttStatus.CREATED;
			break;

		case Constants.SERVICE_OP_DEVICE_UPDATE:
			final DeviceListRequestDTO deviceUpdateDTO = readPayload(request.getPayload(), DeviceListRequestDTO.class);
			responsePayload = deviceUpdate(deviceUpdateDTO);
			break;

		case Constants.SERVICE_OP_DEVICE_REMOVE:
			final List<String> deviceRemoveDTO = readPayload(request.getPayload(), new TypeReference<List<String>>() { });
			deviceRemove(deviceRemoveDTO);
			break;

		case Constants.SERVICE_OP_SYSTEM_QUERY:
			final SystemQueryRequestDTO systemQueryDTO = readPayload(request.getPayload(), SystemQueryRequestDTO.class);
			final Boolean systemQueryVerbose = Boolean.valueOf(request.getParams().get(Constants.VERBOSE));
			responsePayload = systemQuery(systemQueryDTO, systemQueryVerbose);
			break;

		case Constants.SERVICE_OP_SYSTEM_CREATE:
			final SystemListRequestDTO systemCreateDTO = readPayload(request.getPayload(), SystemListRequestDTO.class);
			responsePayload = systemCreate(systemCreateDTO);
			responseStatus = MqttStatus.CREATED;
			break;

		case Constants.SERVICE_OP_SYSTEM_UPDATE:
			final SystemListRequestDTO systemUpdateDTO = readPayload(request.getPayload(), SystemListRequestDTO.class);
			responsePayload = systemUpdate(systemUpdateDTO);
			break;

		case Constants.SERVICE_OP_SYSTEM_REMOVE:
			final List<String> systemRemoveDTO = readPayload(request.getPayload(), new TypeReference<List<String>>() { });
			systemRemove(systemRemoveDTO);
			break;

		case Constants.SERVICE_OP_SERVICE_DEF_QUERY:
			final PageDTO serviceDefQueryDTO = readPayload(request.getPayload(), PageDTO.class);
			responsePayload = serviceDefinitionQuery(serviceDefQueryDTO);
			break;

		case Constants.SERVICE_OP_SERVICE_DEF_CREATE:
			final ServiceDefinitionListRequestDTO serviceDefCreateDTO = readPayload(request.getPayload(), ServiceDefinitionListRequestDTO.class);
			responsePayload = serviceDefinitionCreate(serviceDefCreateDTO);
			responseStatus = MqttStatus.CREATED;
			break;

		case Constants.SERVICE_OP_SERVICE_DEF_REMOVE:
			final List<String> serviceDefRemoveDTO = readPayload(request.getPayload(), new TypeReference<List<String>>() { });
			serviceDefinitionRemove(serviceDefRemoveDTO);
			break;

		case Constants.SERVICE_OP_SERVICE_QUERY:
			 final ServiceInstanceQueryRequestDTO serviceInstanceQueryDTO = readPayload(request.getPayload(), ServiceInstanceQueryRequestDTO.class);
			 final Boolean serviceQueryVerbose = Boolean.valueOf(request.getParams().get(Constants.VERBOSE));
			 responsePayload = serviceInstanceQuery(serviceInstanceQueryDTO, serviceQueryVerbose);
			break;

		case Constants.SERVICE_OP_SERVICE_CREATE:
			final ServiceInstanceCreateListRequestDTO serviceInstanceCreateDTO = readPayload(request.getPayload(), ServiceInstanceCreateListRequestDTO.class);
			responsePayload = serviceInstanceCreate(serviceInstanceCreateDTO);
			responseStatus = MqttStatus.CREATED;
			break;

		case Constants.SERVICE_OP_SERVICE_UPDATE:
			final ServiceInstanceUpdateListRequestDTO serviceInstanceUpdateDTO = readPayload(request.getPayload(), ServiceInstanceUpdateListRequestDTO.class);
			responsePayload = serviceInstanceUpdate(serviceInstanceUpdateDTO);
			break;

		case Constants.SERVICE_OP_SERVICE_REMOVE:
			final List<String> serviceInstanceRemoveDTO = readPayload(request.getPayload(), new TypeReference<List<String>>() { });
			serviceInstanceRemove(serviceInstanceRemoveDTO);
			break;

		case Constants.SERVICE_OP_INTERFACE_TEMPLATE_QUERY:
			final ServiceInterfaceTemplateQueryRequestDTO interfTemplateQueryDTO = readPayload(request.getPayload(), ServiceInterfaceTemplateQueryRequestDTO.class);
			responsePayload = interfaceTemplateQuery(interfTemplateQueryDTO);
			break;

		case Constants.SERVICE_OP_INTERFACE_TEMPLATE_CREATE:
			final ServiceInterfaceTemplateListRequestDTO interfTemplateCreateDTO = readPayload(request.getPayload(), ServiceInterfaceTemplateListRequestDTO.class);
			responsePayload = interfaceTemplateCreate(interfTemplateCreateDTO);
			responseStatus = MqttStatus.CREATED;
			break;

		case Constants.SERVICE_OP_INTERFACE_TEMPLATE_REMOVE:
			final List<String> interfTemplateRemoveDTO = readPayload(request.getPayload(), new TypeReference<List<String>>() { });
			interfaceTemplateRemove(interfTemplateRemoveDTO);
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
		return configService.getConfig(dto, topic());
	}

	//-------------------------------------------------------------------------------------------------
	private DeviceListResponseDTO deviceQuery(final DeviceQueryRequestDTO dto) {
		logger.debug("ManagementMqttHandler.deviceQuery started");
		return mgmtService.queryDevices(dto, topic());
	}

	//-------------------------------------------------------------------------------------------------
	private DeviceListResponseDTO deviceCreate(final DeviceListRequestDTO dto) {
		logger.debug("ManagementMqttHandler.deviceCreate started");
		return mgmtService.createDevices(dto, topic());
	}

	//-------------------------------------------------------------------------------------------------
	private DeviceListResponseDTO deviceUpdate(final DeviceListRequestDTO dto) {
		logger.debug("ManagementMqttHandler.deviceUpdate started");
		return mgmtService.updateDevices(dto, topic());
	}

	//-------------------------------------------------------------------------------------------------
	private void deviceRemove(final List<String> names) {
		logger.debug("ManagementMqttHandler.deviceRemove started");
		mgmtService.removeDevices(names, topic());
	}

	//-------------------------------------------------------------------------------------------------
	private SystemListResponseDTO systemQuery(final SystemQueryRequestDTO dto, final boolean verbose) {
		logger.debug("ManagementMqttHandler.systemQuery started");
		return mgmtService.querySystems(dto, verbose, topic());
	}

	//-------------------------------------------------------------------------------------------------
	private SystemListResponseDTO systemCreate(final SystemListRequestDTO dto) {
		logger.debug("ManagementMqttHandler.systemCreate started");
		return mgmtService.createSystems(dto, topic());
	}

	//-------------------------------------------------------------------------------------------------
	private SystemListResponseDTO systemUpdate(final SystemListRequestDTO dto) {
		logger.debug("ManagementMqttHandler.systemUpdate started");
		return mgmtService.updateSystems(dto, topic());
	}

	//-------------------------------------------------------------------------------------------------
	private void systemRemove(final List<String> names) {
		logger.debug("ManagementMqttHandler.systemRemove started");
		mgmtService.removeSystems(names, topic());
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceDefinitionListResponseDTO serviceDefinitionQuery(final PageDTO dto) {
		logger.debug("ManagementMqttHandler.serviceDefinitionQuery started");
		return mgmtService.getServiceDefinitions(dto, topic());
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceDefinitionListResponseDTO serviceDefinitionCreate(final ServiceDefinitionListRequestDTO dto) {
		logger.debug("ManagementMqttHandler.serviceDefinitionCreate started");
		return mgmtService.createServiceDefinitions(dto, topic());
	}

	//-------------------------------------------------------------------------------------------------
	private void serviceDefinitionRemove(final List<String> names) {
		logger.debug("ManagementMqttHandler.serviceDefinitionRemove started");
		mgmtService.removeServiceDefinitions(names, topic());
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceInstanceListResponseDTO serviceInstanceQuery(final ServiceInstanceQueryRequestDTO dto, final boolean verbose) {
		logger.debug("ManagementMqttHandler.serviceInstanceQuery started");
		return mgmtService.queryServiceInstances(dto, verbose, topic());
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceInstanceListResponseDTO serviceInstanceCreate(final ServiceInstanceCreateListRequestDTO dto) {
		logger.debug("ManagementMqttHandler.serviceInstanceCreate started");
		return mgmtService.createServiceInstances(dto, topic());
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceInstanceListResponseDTO serviceInstanceUpdate(final ServiceInstanceUpdateListRequestDTO dto) {
		logger.debug("ManagementMqttHandler.serviceInstanceUpdate started");
		return mgmtService.updateServiceInstance(dto, topic());
	}

	//-------------------------------------------------------------------------------------------------
	private void serviceInstanceRemove(final List<String> serviceInstances) {
		logger.debug("ManagementMqttHandler.serviceInstanceRemove started");
		mgmtService.removeServiceInstances(serviceInstances, topic());
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceInterfaceTemplateListResponseDTO interfaceTemplateQuery(final ServiceInterfaceTemplateQueryRequestDTO dto) {
		logger.debug("ManagementMqttHandler.interfaceTemplateQuery started");
		return mgmtService.queryInterfaceTemplates(dto, topic());
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceInterfaceTemplateListResponseDTO interfaceTemplateCreate(final ServiceInterfaceTemplateListRequestDTO dto) {
		logger.debug("ManagementMqttHandler.interfaceTemplateCreate started");
		return mgmtService.createInterfaceTemplates(dto, topic());
	}

	//-------------------------------------------------------------------------------------------------
	private void interfaceTemplateRemove(final List<String> names) {
		logger.debug("ManagementMqttHandler.interfaceTemplateRemove started");
		mgmtService.removeInterfaceTemplates(names, topic());
	}
}
