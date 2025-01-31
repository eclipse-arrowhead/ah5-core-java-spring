package eu.arrowhead.serviceregistry.service.normalization;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import eu.arrowhead.dto.ServiceInstanceRequestDTO;

@SpringBootTest
public class ServiceDiscoveryNormalizationTest {
	
	//=================================================================================================
	// members
	
	@Autowired
	private ServiceDiscoveryNormalization normalizator;
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeServiceInstanceRequestDTOTest1() {
		
		// TODO!
		// not finished
		
		ServiceInstanceRequestDTO toNormalize = new ServiceInstanceRequestDTO(
				// system name
				" \tsYSTEM-NAME\n \n ", 
				// service definiton name
				" \tsERVICE-DEFINITION-NAME\n \n ", 
				// version
				" 1\n", 
				// expires at
				"\n 2025-01-31T12:00:00Z \n", 
				// metadata
				Map.of("key", "value"), 
				// interfaces: TODO
				null);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeServiceInstanceRequestDTOTest2_CasesOfNull() {
		
		// TODO!
		// not finished
		
		// dto is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {normalizator.normalizeServiceInstanceRequestDTO(null);});
		
		// system name is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {normalizator.normalizeServiceInstanceRequestDTO(
				new ServiceInstanceRequestDTO(null, "service-def", "1.0.0", "2025-01-31T12:00:00Z", Map.of("key", "value"), null));});
		
		// dto contains null members
		ServiceInstanceRequestDTO toNormalize = new ServiceInstanceRequestDTO(
				// system name
				" \tsYSTEM-NAME\n \n ", 
				// service definiton name
				" \tsERVICE-DEFINITION-NAME\n \n ", 
				// version
				" 1\n", 
				// expires at
				"\n 2025-01-31T12:00:00Z \n", 
				// metadata
				Map.of("key", "value"), 
				// interfaces: TODO
				null);
	}
}
