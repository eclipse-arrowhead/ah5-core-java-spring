package eu.arrowhead.serviceregistry.service.normalization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.intf.properties.PropertyValidatorType;
import eu.arrowhead.dto.HttpOperationDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceRequestDTO;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInterfaceTemplateProperty;
import eu.arrowhead.serviceregistry.jpa.service.ServiceInterfaceTemplateDbService;
import eu.arrowhead.serviceregistry.service.validation.address.AddressNormalizer;
import eu.arrowhead.serviceregistry.service.validation.version.VersionNormalizer;

@Service
public class ServiceDiscoveryNormalization {

	//=================================================================================================
	// members

	@Autowired
	private AddressNormalizer addressNormalizer;

	@Autowired
	private VersionNormalizer versionNormalizer;

	@Autowired
	private ServiceInterfaceTemplateDbService interfaceTemplateDbService;

	private static final ObjectMapper mapper = new ObjectMapper();

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public ServiceInstanceRequestDTO normalizeServiceInstanceRequestDTO(final ServiceInstanceRequestDTO dto) {
		logger.debug("normalizeServiceInstanceRequestDTO started");
		Assert.notNull(dto, "ServiceInstanceRequestDTO is null");

		return new ServiceInstanceRequestDTO(
				dto.systemName().trim(),
				dto.serviceDefinitionName().trim().toLowerCase(),
				versionNormalizer.normalize(dto.version()),
				Utilities.isEmpty(dto.expiresAt()) ? "" : dto.expiresAt().trim(),
				dto.metadata(),
				Utilities.isEmpty(dto.interfaces()) ? new ArrayList<>()
						: dto.interfaces()
								.stream()
								.map(i -> new ServiceInstanceInterfaceRequestDTO(
										i.templateName().trim().toUpperCase(),
										i.protocol().trim().toLowerCase(),
										i.policy().trim().toUpperCase(),
										normalizeInterfaceProperties(i.templateName(), i.properties())))
								.toList());
	}

	//-------------------------------------------------------------------------------------------------
	public String normalizeServiceInstanceId(final String instanceId) {
		logger.debug("normalizeServiceInstanceRequestDTO started");
		Assert.isTrue(!Utilities.isEmpty(instanceId), "Service instance id is empty");

		return instanceId.trim().toLowerCase();
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private Map<String, Object> normalizeInterfaceProperties(final String templateName, final Map<String, Object> interfaceProperties) {
		final List<ServiceInterfaceTemplateProperty> templateProps = interfaceTemplateDbService.getPropertiesByName(templateName);

		for (final ServiceInterfaceTemplateProperty templateProperty : templateProps) {
			if (interfaceProperties.containsKey(templateProperty.getPropertyName()) && !Utilities.isEmpty(templateProperty.getValidator())) {

				if (templateProperty.getValidator().equals(PropertyValidatorType.HTTP_OPERATIONS.name())) {
					final Object operationsObj = interfaceProperties.get(templateProperty.getPropertyName());
					if (operationsObj != null) {
						try {
							final Map<String, HttpOperationDTO> operations = mapper.readValue(mapper.writeValueAsBytes(operationsObj), new TypeReference<Map<String, HttpOperationDTO>>() { });
							final Map<String, HttpOperationDTO> normalizedOperations = new HashMap<>();
							for (final Entry<String, HttpOperationDTO> entry : operations.entrySet()) {
								normalizedOperations.put(
										entry.getKey().trim().toLowerCase(),
										entry.getValue() == null ? null : new HttpOperationDTO(
												Utilities.isEmpty(entry.getValue().path()) ? "" : entry.getValue().path().trim(),
												Utilities.isEmpty(entry.getValue().method()) ? "" : entry.getValue().method().trim().toUpperCase())
								);
							}
							interfaceProperties.put(templateProperty.getPropertyName(), normalizedOperations);
						} catch (final Exception ex) {
							logger.debug(ex);
							logger.error("HTTP_OPERATIONS normalization error: " + ex.getMessage());
						}
					}
				}

				if (templateProperty.getValidator().equals(PropertyValidatorType.NOT_EMPTY_ADDRESS_LIST.name())) {
					final Object addressListObj = interfaceProperties.get(templateProperty.getPropertyName());
					if (addressListObj != null) {
						try {
							final List<String> addresses = mapper.readValue(mapper.writeValueAsBytes(addressListObj), new TypeReference<List<String>>() { });
							final List<String> normalizedAddresses = addresses.stream().map(a -> addressNormalizer.normalize(a)).toList();
							interfaceProperties.put(templateProperty.getPropertyName(), normalizedAddresses);
						} catch (final Exception ex) {
							logger.debug(ex);
							logger.error("NOT_EMPTY_ADDRESS_LIST normalization error " + ex.getMessage());
						}

					}
				}
			}
		}

		return interfaceProperties;
	}
}
