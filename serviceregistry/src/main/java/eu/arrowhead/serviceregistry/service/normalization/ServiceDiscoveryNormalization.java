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

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.intf.properties.PropertyValidatorType;
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

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public ServiceInstanceRequestDTO normalizeServiceInstanceRequestDTO(final ServiceInstanceRequestDTO dto) {
		logger.debug("normalizeServiceInstanceRequestDTO started");
		Assert.notNull(dto, "ServiceInstanceRequestDTO is null");

		return new ServiceInstanceRequestDTO(
				dto.systemName().trim(),
				dto.serviceDefinitionName().trim(),
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

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	private Map<String, Object> normalizeInterfaceProperties(final String templateName, final Map<String, Object> interfaceProperties) {
		final List<ServiceInterfaceTemplateProperty> templateProps = interfaceTemplateDbService.getPropertiesByName(templateName);

		for (final ServiceInterfaceTemplateProperty templateProperty : templateProps) {
			if (interfaceProperties.containsKey(templateProperty.getPropertyName()) && !Utilities.isEmpty(templateProperty.getValidator())) {

				if (templateProperty.getValidator().equals(PropertyValidatorType.HTTP_OPERATIONS.name())) {
					final Object operationsObj = interfaceProperties.get(templateProperty.getPropertyName());
					if (operationsObj != null) {
						try {
//							final Map<String, Map<String, String>> operations = Utilities.fromJson(Utilities.toJson(operationsObj), new TypeReference<Map<String, Map<String, String>>>() { });
//							final Map<String, Object> normalizedOperations = new HashMap<>();
//							for (final Entry<String, Map<String, String>> entry : operations.entrySet()) {
//								normalizedOperations.put(entry.getKey().trim().toLowerCase(), Utilities.toJson(new HttpOperationModel(entry.getValue().path().trim(), entry.getValue().method())));
//							}
//							interfaceProperties.put(templateProperty.getPropertyName(), normalizedOperations);
						} catch (final ClassCastException ex) {
							logger.debug(ex);
							logger.error(ex.getMessage());
						}
					}
				}

				if (templateProperty.getValidator().equals(PropertyValidatorType.NOT_EMPTY_ADDRESS_LIST.name())) {
					final Object addressListObj = interfaceProperties.get(templateProperty.getPropertyName());
					if (addressListObj != null) {
						try {
							final List<String> addresses = (List<String>) addressListObj;
							final List<String> normalizedAddresses = addresses.stream().map(a -> addressNormalizer.normalize(a)).toList();
							interfaceProperties.put(templateProperty.getPropertyName(), normalizedAddresses);
						} catch (final ClassCastException ex) {
							logger.debug(ex);
							logger.error(ex.getMessage());
						}

					}
				}
			}
		}

		return interfaceProperties;
	}
}
