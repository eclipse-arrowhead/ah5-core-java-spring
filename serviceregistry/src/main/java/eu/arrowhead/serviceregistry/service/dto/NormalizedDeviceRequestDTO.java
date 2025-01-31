package eu.arrowhead.serviceregistry.service.dto;

import java.util.List;
import java.util.Map;

import eu.arrowhead.dto.AddressDTO;

public record NormalizedDeviceRequestDTO(
		String name,
		Map<String, Object> metadata,
		List<AddressDTO> addresses) {
}