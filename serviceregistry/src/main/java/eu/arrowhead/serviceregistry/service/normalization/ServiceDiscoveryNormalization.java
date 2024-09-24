package eu.arrowhead.serviceregistry.service.normalization;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceRequestDTO;
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
						: dto.interfaces().stream()
								.map(i -> new ServiceInstanceInterfaceRequestDTO(i.templateName().trim(), i.protocol().trim(), i.policy().trim(), i.properties())).toList()); // TODO normalize address
	}
}
