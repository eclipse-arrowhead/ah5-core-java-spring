package eu.arrowhead.serviceregistry;

import java.util.List;

import org.springframework.stereotype.Component;

import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.model.SystemModel;

@Component
public class ServiceRegistrySystemInfo extends SystemInfo {

	//-------------------------------------------------------------------------------------------------
	@Override
	public String getSystemName() {
		return ServiceRegistryConstants.SYSTEM_NAME;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public List<ServiceModel> getServices() {
		// TODO Auto-generated method stub
		return null;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public SystemModel getSystemModel() {
		// TODO Auto-generated method stub
		return null;
	}
}