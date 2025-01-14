package eu.arrowhead.authentication;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import eu.arrowhead.authentication.http.filter.InternalAuthenticationFilter;
import eu.arrowhead.common.http.filter.authentication.IAuthenticationPolicyFilter;

@Configuration
public class BeanConfig {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Bean
	IAuthenticationPolicyFilter internalAuthenticationPolicyFilter() {
		return new InternalAuthenticationFilter();
	}
}
