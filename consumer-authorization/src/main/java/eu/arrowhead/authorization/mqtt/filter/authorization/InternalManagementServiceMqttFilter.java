package eu.arrowhead.authorization.mqtt.filter.authorization;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.mqtt.filter.ArrowheadMqttFilter;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;
import eu.arrowhead.common.service.validation.name.NameNormalizer;

public class InternalManagementServiceMqttFilter implements ArrowheadMqttFilter {

	//=================================================================================================
	// members

	@Autowired
	private SystemInfo sysInfo;

	@Autowired
	private NameNormalizer nameNormalizer;

	private static final String mgmtPath = "/management";

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public int order() {
		return Constants.REQUEST_FILTER_ORDER_AUTHORIZATION_MGMT_SERVICE;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void doFilter(final String authKey, final MqttRequestModel request) {
		logger.debug("InternalManagementServiceMqttFilter.doFilter started...");

		if (request.getBaseTopic().contains(mgmtPath)) {
			final String normalizedSystemName = nameNormalizer.normalize(request.getRequester());
			boolean allowed = false;

			switch (sysInfo.getManagementPolicy()) {
			case SYSOP_ONLY:
				allowed = request.isSysOp();
				break;

			case WHITELIST:
				allowed = request.isSysOp() || isWhitelisted(normalizedSystemName);
				break;

			case AUTHORIZATION:
				allowed = request.isSysOp() || isWhitelisted(normalizedSystemName) || isAuthorized(normalizedSystemName);
				break;

			default:
				throw new InternalServerError("Unimplemented management policy: " + sysInfo.getManagementPolicy());
			}

			if (!allowed) {
				throw new ForbiddenException("Requester has no management permission");
			}
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	public boolean isWhitelisted(final String systemName) {
		return sysInfo.getManagementWhitelist().contains(systemName);
	}

	//-------------------------------------------------------------------------------------------------
	private boolean isAuthorized(final String systemName) {
		// TODO check in the database
		return false;
	}
}