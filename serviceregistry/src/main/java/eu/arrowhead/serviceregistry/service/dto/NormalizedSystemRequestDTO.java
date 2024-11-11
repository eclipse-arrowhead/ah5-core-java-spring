package eu.arrowhead.serviceregistry.service.dto;

import java.util.List;
import java.util.Map;

import eu.arrowhead.dto.AddressDTO;

public record NormalizedSystemRequestDTO(
		String name,
		Map<String, Object> metadata,
		String version,
		List<AddressDTO> addresses,
		String deviceName) {

}
