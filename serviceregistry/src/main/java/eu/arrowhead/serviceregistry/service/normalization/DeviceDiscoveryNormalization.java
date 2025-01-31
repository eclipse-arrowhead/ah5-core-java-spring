package eu.arrowhead.serviceregistry.service.normalization;

import java.util.ArrayList;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.address.AddressNormalizer;
import eu.arrowhead.common.service.validation.address.AddressValidator;
import eu.arrowhead.common.service.validation.name.NameNormalizer;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.DeviceLookupRequestDTO;
import eu.arrowhead.dto.DeviceRequestDTO;
import eu.arrowhead.serviceregistry.service.dto.NormalizedDeviceRequestDTO;

@Service
public class DeviceDiscoveryNormalization {

	//=================================================================================================
	// members

	@Autowired
	private AddressNormalizer addressNormalizer;

	@Autowired
	private AddressValidator addressValidator;

	@Autowired
	private NameNormalizer nameNormalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public NormalizedDeviceRequestDTO normalizeDeviceRequestDTO(final DeviceRequestDTO dto) {
		logger.debug("normalizeDeviceRequestDTO started");
		Assert.notNull(dto, "DeviceRequestDTO is null");
		Assert.isTrue(!Utilities.isEmpty(dto.name()), "DeviceRequestDTO name is empty");

		return new NormalizedDeviceRequestDTO(
				normalizeDeviceName(dto.name()),
				dto.metadata(),
				Utilities.isEmpty(dto.addresses()) ? new ArrayList<>()
						: dto.addresses().stream()
										 .map(a -> addressNormalizer.normalize(a))
										 .map(na -> new AddressDTO(addressValidator.detectType(na).name(), na))
										 .collect(Collectors.toList()));
	}

	//-------------------------------------------------------------------------------------------------
	public DeviceLookupRequestDTO normalizeDeviceLookupRequestDTO(final DeviceLookupRequestDTO dto) {
		logger.debug("normalizeDeviceLookupRequestDTO started");
		Assert.notNull(dto, "DeviceLookupRequestDTO is null");

		return new DeviceLookupRequestDTO(
				Utilities.isEmpty(dto.deviceNames()) ? null : dto.deviceNames().stream().map(n -> normalizeDeviceName(n)).collect(Collectors.toList()),
				Utilities.isEmpty(dto.addresses()) ? null : dto.addresses().stream().map(a -> addressNormalizer.normalize(a)).collect(Collectors.toList()),
				Utilities.isEmpty(dto.addressType()) ? null : dto.addressType().trim().toUpperCase(),
				dto.metadataRequirementList());
	}

	//-------------------------------------------------------------------------------------------------
	public String normalizeDeviceName(final String name) {
		logger.debug("normalizeDeviceName started");
		Assert.notNull(name, "Device name is null");

		return nameNormalizer.normalize(name);
	}
}