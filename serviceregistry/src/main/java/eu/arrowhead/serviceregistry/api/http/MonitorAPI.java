package eu.arrowhead.serviceregistry.api.http;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.serviceregistry.ServiceRegistryConstants;

@RestController
@RequestMapping(ServiceRegistryConstants.HTTP_API_MONITOR_PATH)
public class MonitorAPI {

	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	@GetMapping(path = "/echo")
	public String echoService() {
		return "Got it!";
	}
}
