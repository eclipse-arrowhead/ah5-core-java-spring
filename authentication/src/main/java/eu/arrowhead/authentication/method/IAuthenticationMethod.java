package eu.arrowhead.authentication.method;

public interface IAuthenticationMethod {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public IAuthenticationMethodInputValidator validator(); // to validate input

	//-------------------------------------------------------------------------------------------------
	public IAuthenticationMethodInputNormalizer normalizer(); // to normalize input

	//----------------------------------------------------------------------------- --------------------
	public IAuthenticationMethodService service(); // calling from the service layer

	//-------------------------------------------------------------------------------------------------
	public IAuthenticationMethodDbService dbService(); // calling from the database service layer
}
