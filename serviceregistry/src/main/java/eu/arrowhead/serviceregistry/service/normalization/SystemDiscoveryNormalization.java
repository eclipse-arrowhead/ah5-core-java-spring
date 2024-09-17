package eu.arrowhead.serviceregistry.service.normalization;

import java.util.ArrayList;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.SystemLookupRequestDTO;
import eu.arrowhead.dto.SystemRequestDTO;
import eu.arrowhead.serviceregistry.service.validation.address.AddressNormalizer;
import eu.arrowhead.serviceregistry.service.validation.version.VersionNormalizer;

@Service
public class SystemDiscoveryNormalization {

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
	public SystemRequestDTO normalizeSystemRequestDTO(final SystemRequestDTO dto) {
		logger.debug("normalizeSystemRequestDTO started");
		Assert.notNull(dto, "SystemRequestDTO is null");

		return new SystemRequestDTO(
				dto.name().trim(),
				dto.metadata(),
				versionNormalizer.normalize(dto.version()),
				Utilities.isEmpty(dto.addresses()) ? new ArrayList<>()
						: dto.addresses().stream()
								.map(a -> new AddressDTO(a.type().trim(), addressNormalizer.normalize(a.address())))
								.collect(Collectors.toList()),
				Utilities.isEmpty(dto.deviceName()) ? null : dto.deviceName().trim());
	}

	//-------------------------------------------------------------------------------------------------
	public SystemLookupRequestDTO normalizeSystemLookupRequestDTO(final SystemLookupRequestDTO dto) {
		logger.debug("normalizeSystemLookupRequestDTO started");
		
		if (dto == null) {
			return new SystemLookupRequestDTO(null, null, null, new ArrayList<MetadataRequirementDTO>(), null, null);
		}

		return new SystemLookupRequestDTO(
				Utilities.isEmpty(dto.systemNames()) ? null : dto.systemNames().stream().map(n -> n.trim()).collect(Collectors.toList()),
				Utilities.isEmpty(dto.addresses()) ? null : dto.addresses().stream().map(a -> addressNormalizer.normalize(a)).collect(Collectors.toList()),
				Utilities.isEmpty(dto.addressType()) ? null : dto.addressType().trim(),
				dto.metadataRequirementList(),
				Utilities.isEmpty(dto.versions()) ? null : dto.versions().stream().map(v -> versionNormalizer.normalize(v)).collect(Collectors.toList()),
				Utilities.isEmpty(dto.deviceNames()) ? null : dto.deviceNames().stream().map(dn -> dn.trim()).collect(Collectors.toList()));
	}

	//-------------------------------------------------------------------------------------------------
	public String normalizeSystemName(final String name) {
		logger.debug("normalizeSystemName started");
		Assert.notNull(name, "System name is null");

		return name.trim();
	}
}
