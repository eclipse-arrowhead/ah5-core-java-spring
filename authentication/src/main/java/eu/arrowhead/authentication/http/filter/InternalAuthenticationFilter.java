package eu.arrowhead.authentication.http.filter;

import java.io.IOException;

import org.springframework.core.annotation.Order;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.http.filter.ArrowheadFilter;
import eu.arrowhead.common.http.filter.authentication.IAuthenticationPolicyFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Order(Constants.REQUEST_FILTER_ORDER_AUTHENTICATION)
public class InternalAuthenticationFilter extends ArrowheadFilter implements IAuthenticationPolicyFilter {

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws IOException, ServletException {
		// TODO

		System.out.println("Internal auth filter");

		chain.doFilter(request, response);

	}
}
