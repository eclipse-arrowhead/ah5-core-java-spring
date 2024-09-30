package eu.arrowhead.serviceregistry.api.http.filter;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.http.filter.ArrowheadFilter;
import eu.arrowhead.serviceregistry.ServiceRegistryConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@ConditionalOnProperty(name = ServiceRegistryConstants.SERVICE_DISCOVERY_POLICY, havingValue = "restricted", matchIfMissing = false)
@Order(25)
public class ServiceLookupFilter extends ArrowheadFilter {

	//=================================================================================================
	// members

	@Value(ServiceRegistryConstants.SERVICE_DISCOVERY_DIRECT_ACCESS)
	private List<String> serviceDiscoveryDirectAccess;

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws IOException, ServletException {
		try {
			final String requestTarget = Utilities.stripEndSlash(request.getRequestURL().toString());
			if (requestTarget.endsWith(ServiceRegistryConstants.HTTP_API_SERVICE_DISCOVERY_PATH + ServiceRegistryConstants.HTTP_API_OP_LOOKUP_PATH)) {
				final Boolean isSysop = Boolean.valueOf(request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_SYSOP_REQUEST).toString());
				final String systemName = request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM).toString();

				final boolean isRestricted = !(isSysop || serviceDiscoveryDirectAccess.contains(systemName));
				request.setAttribute(ServiceRegistryConstants.HTTP_ATTR_RESTRICTED_SERVICE_LOOKUP, isRestricted);
			}

			chain.doFilter(request, response);
		} catch (final ArrowheadException ex) {
			handleException(ex, response);
		}
	}
}
