package eu.arrowhead.serviceregistry.service.normalization;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.AddressDTO;
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
				dto.expiresAt().trim(),
				dto.metadata(),
				Utilities.isEmpty(dto.interfaces()) ? new ArrayList<>()
						: dto.interfaces()
								.stream()
								.map(i -> new ServiceInstanceInterfaceRequestDTO(
										i.templateName().trim(),
										i.protocol().trim(),
										i.policy().trim(),
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
			if (interfaceProperties.containsKey(templateProperty.getPropertyName())) {
				// TODO use enums or constants

				if (templateProperty.getValidator().equals("HTTP_OPERATIONS")) {
					final Object strObj = interfaceProperties.get(templateProperty.getPropertyName());
					if (strObj != null) {
						try {
							final String str = (String) strObj;
							interfaceProperties.put(templateProperty.getPropertyName(), str.trim());
						} catch (final ClassCastException ex) {
							logger.debug(ex);
							logger.error(ex.getMessage());
						}
					}
				}

				if (templateProperty.getValidator().equals("NOT_EMPTY_ADDRESS_LIST")) {
					final Object addressListObj = interfaceProperties.get(templateProperty.getPropertyName());
					if (addressListObj != null) {
						try {
							final List<AddressDTO> addresses = (List<AddressDTO>) addressListObj;
							final List<AddressDTO> normalizedAddresses = addresses.stream().map(a -> new AddressDTO(a.type().trim(), addressNormalizer.normalize(a.address()))).toList();
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
