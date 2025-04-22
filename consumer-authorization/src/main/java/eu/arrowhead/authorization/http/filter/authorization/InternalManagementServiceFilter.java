package eu.arrowhead.authorization.http.filter.authorization;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.http.filter.ArrowheadFilter;
import eu.arrowhead.common.service.validation.name.NameNormalizer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Order(Constants.REQUEST_FILTER_ORDER_AUTHORIZATION_MGMT_SERVICE)
public class InternalManagementServiceFilter extends ArrowheadFilter {

	//=================================================================================================
	// members

	@Autowired
	private SystemInfo sysInfo;

	@Autowired
	private NameNormalizer nameNormalizer;

	private static final String mgmtPath = "/mgmt/";

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws IOException, ServletException {
		logger.debug("InternalManagementServiceFilter.doFilterInternal started...");

		final String requestTarget = request.getRequestURL().toString();
		if (requestTarget.contains(mgmtPath)) {
			final String systemName = (String) request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM);
			final String normalizedSystemName = nameNormalizer.normalize(systemName);
			boolean allowed = false;

			switch (sysInfo.getManagementPolicy()) {
			case SYSOP_ONLY:
				allowed = isSystemOperator(request);
				break;

			case WHITELIST:
				allowed = isSystemOperator(request) || isWhitelisted(normalizedSystemName);
				break;

			case AUTHORIZATION:
				allowed = isSystemOperator(request) || isWhitelisted(normalizedSystemName) || isAuthorized(normalizedSystemName);
				break;

			default:
				throw new InternalServerError("Unimplemented management policy: " + sysInfo.getManagementPolicy(), requestTarget);
			}

			if (!allowed) {
				throw new ForbiddenException("Requester has no management permission", requestTarget);
			}

		}

		super.doFilterInternal(request, response, chain);

	}

	//-------------------------------------------------------------------------------------------------
	private boolean isSystemOperator(final HttpServletRequest request) {
		final Object isSysOp = request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_SYSOP_REQUEST);
		return isSysOp == null ? false : (Boolean) isSysOp;
	}

	//-------------------------------------------------------------------------------------------------
	private boolean isWhitelisted(final String systemName) {
		return sysInfo.getManagementWhitelist().contains(systemName);
	}

	//-------------------------------------------------------------------------------------------------
	private boolean isAuthorized(final String systemName) {
		// TODO check in the database
		return false;
	}
}