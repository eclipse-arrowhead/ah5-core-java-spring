package eu.arrowhead.authentication.method;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import eu.arrowhead.authentication.method.password.PasswordAuthenticationMethod;
import eu.arrowhead.dto.enums.AuthenticationMethod;
import jakarta.annotation.PostConstruct;

@Service
public class AuthenticationMethods {

	//=================================================================================================
	// members

	private final Map<AuthenticationMethod, IAuthenticationMethod> methods = new ConcurrentHashMap<>();

	@Autowired
	private ApplicationContext appContext;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public IAuthenticationMethod method(final AuthenticationMethod type) {
		return methods.get(type);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@PostConstruct
	private void init() {
		methods.put(AuthenticationMethod.PASSWORD, appContext.getBean(PasswordAuthenticationMethod.class));
	}
}