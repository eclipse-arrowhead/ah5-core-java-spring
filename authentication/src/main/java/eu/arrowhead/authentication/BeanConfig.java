package eu.arrowhead.authentication;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

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

	//-------------------------------------------------------------------------------------------------
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(AuthenticationConstants.ENCODER_STRENGTH);
	}
}