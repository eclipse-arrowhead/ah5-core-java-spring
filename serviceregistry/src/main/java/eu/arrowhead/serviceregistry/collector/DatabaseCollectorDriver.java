package eu.arrowhead.serviceregistry.collector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.arrowhead.common.collector.ICollectorDriver;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.model.ServiceModel;

public class DatabaseCollectorDriver implements ICollectorDriver {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public void init() throws ArrowheadException {
		logger.debug("DatabaseCollectorDriver.init started...");

		// TODO Auto-generated method stub

	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public ServiceModel acquireService(final String serviceDefinitionName, final String interfaceTemplateName) throws ArrowheadException {
		// TODO Auto-generated method stub
		return null;
	}

}
