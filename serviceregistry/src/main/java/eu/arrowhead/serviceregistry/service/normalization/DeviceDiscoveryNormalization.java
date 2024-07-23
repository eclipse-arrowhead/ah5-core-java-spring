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
import eu.arrowhead.dto.DeviceLookupRequestDTO;
import eu.arrowhead.dto.DeviceRequestDTO;
import eu.arrowhead.serviceregistry.service.validation.address.AddressNormalizator;

@Service
public class DeviceDiscoveryNormalization {

	//=================================================================================================
	// members

	@Autowired
	private AddressNormalizator addressNormalizator;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public DeviceRequestDTO normalizeDeviceRequestDTO(final DeviceRequestDTO dto) {
		logger.debug("normalizeDeviceRequestDTO started");
		Assert.notNull(dto, "DeviceRequestDTO is null");
		Assert.isTrue(!Utilities.isEmpty(dto.name()), "DeviceRequestDTO name is empty");

		return new DeviceRequestDTO(
				dto.name().trim(),
				dto.metadata(),
				Utilities.isEmpty(dto.addresses()) ? new ArrayList<>()
						: dto.addresses().stream()
								.map(a -> new AddressDTO(a.type().trim(), addressNormalizator.normalize(a.address())))
								.collect(Collectors.toList()));
	}

	//-------------------------------------------------------------------------------------------------
	public DeviceLookupRequestDTO normalizeDeviceLookupRequestDTO(final DeviceLookupRequestDTO dto) {
		logger.debug("normalizeDeviceLookupRequestDTO started");
		Assert.notNull(dto, "DeviceLookupRequestDTO is null");

		return new DeviceLookupRequestDTO(
				Utilities.isEmpty(dto.deviceNames()) ? null : dto.deviceNames().stream().map(n -> n.trim()).collect(Collectors.toList()),
				Utilities.isEmpty(dto.addresses()) ? null : dto.addresses().stream().map(a -> addressNormalizator.normalize(a)).collect(Collectors.toList()),
				Utilities.isEmpty(dto.addressType()) ? null : dto.addressType().trim(),
				dto.metadataRequirementList());
	}
}
