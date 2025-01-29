package eu.arrowhead.authentication.method;

import eu.arrowhead.dto.enums.AuthenticationMethod;

public interface IAuthenticationMethod {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public AuthenticationMethod type(); // the supported type

	//-------------------------------------------------------------------------------------------------
	public IAuthenticationMethodInputValidator validator(); // to validate input

	//-------------------------------------------------------------------------------------------------
	public IAuthenticationMethodInputNormalizer normalizer(); // to normalize input

	//----------------------------------------------------------------------------- --------------------
	public IAuthenticationMethodService service(); // calling from the service layer

	//-------------------------------------------------------------------------------------------------
	public IAuthenticationMethodDbService dbService(); // calling from the database service layer
}
