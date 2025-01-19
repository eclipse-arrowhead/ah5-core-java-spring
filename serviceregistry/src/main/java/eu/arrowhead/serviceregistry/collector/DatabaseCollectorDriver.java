package eu.arrowhead.serviceregistry.collector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;

import com.fasterxml.jackson.core.type.TypeReference;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.collector.ICollectorDriver;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.model.InterfaceModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.mqtt.model.MqttInterfaceModel;
import eu.arrowhead.common.service.validation.name.NameNormalizer;
import eu.arrowhead.common.service.validation.name.NameValidator;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInstance;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInstanceInterface;
import eu.arrowhead.serviceregistry.jpa.service.ServiceInstanceDbService;
import eu.arrowhead.serviceregistry.service.model.ServiceLookupFilterModel;

public class DatabaseCollectorDriver implements ICollectorDriver {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());
	
	@Autowired
	private NameValidator nameValidator;
	
	@Autowired
	private NameNormalizer nameNormalizer;
	
	private static final String PATH_KEY = "path";
	private static final String METHOD_KEY = "method";
	
	@Autowired
	private ServiceInstanceDbService instanceDbService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public void init() throws ArrowheadException {
		logger.debug("DatabaseCollectorDriver.init started...");

		// TODO Auto-generated method stub

	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public ServiceModel acquireService(final String serviceDefinitionName, final String interfaceTemplateName) throws ArrowheadException {
		
		// get the service instance entries from the database
		final Page<Map.Entry<ServiceInstance, List<ServiceInstanceInterface>>> instanceEntries = getInstanceEntries(serviceDefinitionName, interfaceTemplateName);
		if (instanceEntries.isEmpty()) {
			throw new ArrowheadException("the acquired service definition/operation is not found");
		}
		
		// only the first instance entry will be returned
		final ServiceInstance instance = instanceEntries.getContent().getFirst().getKey();
		final List<ServiceInstanceInterface> interfaces = instanceEntries.getContent().getFirst().getValue();
		
		// create the list of interface models 
		List<InterfaceModel> interfaceModelList = new ArrayList<>(interfaces.size());
		for (ServiceInstanceInterface interface_ : interfaces) {
			
			final String templateName = interface_.getServiceInterfaceTemplate().getName();
			final Map<String, Object> properties = Utilities.fromJson(interface_.getProperties(), new TypeReference<Map<String, Object>>() {
			}); 
			
			// http or https
			if (templateName.contains(Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME)) {
				interfaceModelList.add(createHttpInterfaceModel(templateName, properties));
			}
				
			
			// mqtt or mqtts
			if (templateName.contains(Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME)) {
				interfaceModelList.add(creatMqttInterfaceModel(templateName, properties));
			}
			
			// (add new interface templates here...)
		}
		
		final ServiceModel serviceModel = new ServiceModel
				.Builder()
				.serviceDefinition(instance.getServiceDefinition().getName())
				.version(instance.getVersion())
				.serviceInterfaces(interfaceModelList)
				.metadata(Utilities.fromJson(instance.getMetadata(), new TypeReference<Map<String, Object>>() {
				}))
				.build();
		
		return serviceModel;
	}
	
	//=================================================================================================
	// assistant methods
	
	//-------------------------------------------------------------------------------------------------
	private Page<Map.Entry<ServiceInstance, List<ServiceInstanceInterface>>> getInstanceEntries(final String serviceDefinitionName, final String interfaceTemplateName) {
		
		// validate and normalize
		nameValidator.validateName(serviceDefinitionName);
		nameValidator.validateName(interfaceTemplateName);
		nameNormalizer.normalize(serviceDefinitionName);
		nameNormalizer.normalize(interfaceTemplateName);
		
		
		final PageRequest pagination = PageRequest.of(0, Integer.MAX_VALUE, Direction.DESC, ServiceInstance.DEFAULT_SORT_FIELD);
		
		final ServiceLookupFilterModel filterModel = new ServiceLookupFilterModel(
				new ServiceInstanceLookupRequestDTO
				.Builder()
				.serviceDefinitionName(serviceDefinitionName)
				.interfaceTemplateName(interfaceTemplateName)
				.build());
		
		// get the instances from the database
		return instanceDbService.getPageByFilters(pagination, filterModel);
	}
	
	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	private HttpInterfaceModel createHttpInterfaceModel(final String templateName, final Map<String, Object> properties) {
		
		// access addresses
		final List<String> accessAddresses = (List<String>) properties.get(HttpInterfaceModel.PROP_NAME_ACCESS_ADDRESSES); 
		
		// access port
		final int accessPort = (int)properties.get(HttpInterfaceModel.PROP_NAME_ACCESS_PORT); 
		
		// base path
		final String basePath = (String) properties.get(HttpInterfaceModel.PROP_NAME_BASE_PATH);
		
		// operations
		final Map<String, Object> operations = (Map<String, Object>) properties.get(HttpInterfaceModel.PROP_NAME_OPERATIONS);
		
		// create the operation model map
		final Map<String, HttpOperationModel> operationModelMap = new HashMap<>(operations.size()); // expected type
		for (Map.Entry<String, Object> operation : operations.entrySet()) {
			final String operationName = operation.getKey();
			final Map<String, Object> operationProps = (Map<String, Object>) operation.getValue();
			operationModelMap.put(operationName, new HttpOperationModel
														.Builder()
														.method((String)operationProps.get(METHOD_KEY))
														.path((String)operationProps.get(PATH_KEY))
														.build());
		}
		
		// create the interface model
		HttpInterfaceModel model = new HttpInterfaceModel
				.Builder(templateName)
				.accessAddresses(accessAddresses)
				.accessPort(accessPort)
				.basePath(basePath)
				.operations(operationModelMap)
				.build();
		
		return model;
	}
	
	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	private MqttInterfaceModel creatMqttInterfaceModel(final String templateName, final Map<String, Object> properties) {
		
		// access addresses
		final List<String> accessAddresses = (List<String>) properties.get(MqttInterfaceModel.PROP_NAME_ACCESS_ADDRESSES); 
		
		// access port
		final int accessPort = (int)properties.get(MqttInterfaceModel.PROP_NAME_ACCESS_PORT); 
		
		// topic
		final String topic = (String) properties.get(MqttInterfaceModel.PROP_NAME_TOPIC);
		
		// operations
		final Set<String> operations = (Set<String>) properties.get(MqttInterfaceModel.PROP_NAME_OPERATIONS);
		
		// create the interface model
		MqttInterfaceModel model = new MqttInterfaceModel
				.Builder(templateName)
				.accessAddresses(accessAddresses)
				.accessPort(accessPort)
				.topic(topic)
				.operations(operations)
				.build();
		
		return model;
	}
	
}