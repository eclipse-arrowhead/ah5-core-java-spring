package eu.arrowhead.serviceregistry.service.normalization;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.SystemListRequestDTO;
import eu.arrowhead.dto.SystemQueryRequestDTO;
import eu.arrowhead.dto.SystemRequestDTO;
import eu.arrowhead.serviceregistry.service.validation.address.AddressNormalizator;
import eu.arrowhead.serviceregistry.service.validation.version.VersionNormalizator;

@Service
public class ManagementNormalization {
	
import eu.arrowhead.dto.DeviceQueryRequestDTO;
import eu.arrowhead.dto.DeviceRequestDTO;

@Service
public class ManagementNormalization {

	//=================================================================================================
	// members

	@Autowired
	private AddressNormalizator addressNormalizer;
	
	@Autowired
	private VersionNormalizator versionNormalizer;
	

	private final Logger logger = LogManager.getLogger(this.getClass());
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public List<SystemRequestDTO> normalizeSystemRequestDTOs(final SystemListRequestDTO dtoList) {
		logger.debug("normalizeSystemRequestDTOs started");
		Assert.notNull(dtoList, "SystemListRequestDTO is null");
		
		final List<SystemRequestDTO> normalized = new ArrayList<>(dtoList.systems().size());
		for (final SystemRequestDTO system : dtoList.systems()) {
			
			//String normalizedVersion = Utilities.isEmpty(system.version()) ? "1.0.0" : system.version().trim(); 
			
			normalized.add(new SystemRequestDTO(
					system.name().trim(),
					system.metadata(),
					versionNormalizer.normalize(system.version()),
					Utilities.isEmpty(system.addresses()) ? new ArrayList<>()
							: system.addresses().stream()
									.map(a -> new AddressDTO(a.type().trim(), addressNormalizer.normalize(a.address())))
									.collect(Collectors.toList()),
					system.deviceName().trim()));
		}
		return normalized;
	}
	
	//-------------------------------------------------------------------------------------------------
	public SystemQueryRequestDTO normalizeSystemQueryRequestDTO(final SystemQueryRequestDTO dto) {
		logger.debug("normalizeSystemQueryRequestDTO started");
		Assert.notNull(dto, "SystemQueryRequestDTO is null");
		
		return new SystemQueryRequestDTO(
				dto.pagination(), //no need to normailze, because it will happen in the getPageRequest method
				Utilities.isEmpty(dto.systemNames()) ? null 
						: dto.systemNames().stream().map(n -> n.trim()).collect(Collectors.toList()), 
				Utilities.isEmpty(dto.addresses()) ? null 
						: dto.addresses().stream().map(n -> n.trim()).collect(Collectors.toList()), 
				Utilities.isEmpty(dto.addressType()) ? null 
						: dto.addressType().trim(),
				dto.metadataRequirementList(), 
				Utilities.isEmpty(dto.versions()) ? null 
						: dto.versions().stream().map(n -> n.trim()).collect(Collectors.toList()), 
				Utilities.isEmpty(dto.deviceNames()) ? null 
						: dto.deviceNames().stream().map(n -> n.trim()).collect(Collectors.toList()));
	}
	
	//-------------------------------------------------------------------------------------------------
	public List<String> normalizeSystemNames(final List<String> originalNames) {
		return originalNames.stream()
				.filter(n -> !Utilities.isEmpty(n))
				.map(n -> n.trim())
				.collect(Collectors.toList());
	}
	
	

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public List<DeviceRequestDTO> normalizeDeviceRequestDTOList(final List<DeviceRequestDTO> dtoList) {
		logger.debug("normalizeDeviceRequestDTOs started");
		Assert.notNull(dtoList, "DeviceRequestDTO list is null");

		final List<DeviceRequestDTO> normalized = new ArrayList<>(dtoList.size());
		for (final DeviceRequestDTO device : dtoList) {
			Assert.isTrue(!Utilities.isEmpty(device.name()), "Device name is empty");
			normalized.add(new DeviceRequestDTO(
					device.name().trim(),
					device.metadata(),
					Utilities.isEmpty(device.addresses()) ? new ArrayList<>()
							: device.addresses().stream()
									.map(a -> new AddressDTO(a.type().trim(), addressNormalizer.normalize(a.address())))
									.collect(Collectors.toList())));
		}
		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public DeviceQueryRequestDTO normalizeDeviceQueryRequestDTO(final DeviceQueryRequestDTO dto) {
		logger.debug("normalizeDeviceQueryRequestDTO started");
		Assert.notNull(dto, "DeviceQueryRequestDTO list is null");

		return new DeviceQueryRequestDTO(
				dto.pagination(),
				Utilities.isEmpty(dto.deviceNames()) ? null : dto.deviceNames().stream().map(n -> n.trim()).collect(Collectors.toList()),
				Utilities.isEmpty(dto.addresses()) ? null : dto.addresses().stream().map(a -> addressNormalizer.normalize(a)).collect(Collectors.toList()),
				Utilities.isEmpty(dto.addressType()) ? null : dto.addressType().trim(),
				dto.metadataRequirementList());
	}
}
