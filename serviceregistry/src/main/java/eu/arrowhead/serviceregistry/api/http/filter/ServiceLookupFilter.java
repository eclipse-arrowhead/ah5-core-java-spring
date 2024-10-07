package eu.arrowhead.serviceregistry.api.http.filter;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.http.filter.ArrowheadFilter;
import eu.arrowhead.serviceregistry.ServiceRegistryConstants;
import eu.arrowhead.serviceregistry.ServiceRegistrySystemInfo;
import eu.arrowhead.serviceregistry.service.ServiceDiscoveryPolicy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(35)
public class ServiceLookupFilter extends ArrowheadFilter {

	//=================================================================================================
	// members

	@Autowired
	private ServiceRegistrySystemInfo sysInfo;

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws IOException, ServletException {
		try {
			if (sysInfo.getServiceDiscoveryPolicy() == ServiceDiscoveryPolicy.RESTRICTED) {
				final String requestTarget = Utilities.stripEndSlash(request.getRequestURL().toString());
				if (requestTarget.endsWith(ServiceRegistryConstants.HTTP_API_SERVICE_DISCOVERY_PATH + ServiceRegistryConstants.HTTP_API_OP_LOOKUP_PATH)) {
					final Boolean isSysop = Boolean.valueOf(request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_SYSOP_REQUEST).toString());
					final String systemName = request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM).toString();

					final boolean isRestricted = !(isSysop || sysInfo.hasClientDirectAccess(systemName));
					request.setAttribute(ServiceRegistryConstants.HTTP_ATTR_RESTRICTED_SERVICE_LOOKUP, isRestricted);
				}
			}

			chain.doFilter(request, response);
		} catch (final ArrowheadException ex) {
			handleException(ex, response);
		}
	}
}
