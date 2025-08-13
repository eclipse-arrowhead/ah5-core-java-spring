package eu.arrowhead.serviceregistry.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.serviceregistry.jpa.service.DeviceDbService;
import eu.arrowhead.serviceregistry.service.dto.DTOConverter;
import eu.arrowhead.serviceregistry.service.matching.AddressMatching;
import eu.arrowhead.serviceregistry.service.validation.DeviceDiscoveryValidation;

@ExtendWith(MockitoExtension.class)
public class DeviceDiscoveryServiceTest {

	//=================================================================================================
	// members
	
	@InjectMocks
	private DeviceDiscoveryService service;

	@Mock
	private DeviceDbService dbService;

	@Mock
	private DeviceDiscoveryValidation validator;

	@Mock
	private DTOConverter dtoConverter;

	@Mock
	private AddressMatching addressMatcher;
	
	//=================================================================================================
	// methods
	
	
}
