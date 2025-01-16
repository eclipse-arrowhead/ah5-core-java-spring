package eu.arrowhead.authentication.method;

public interface IAuthenticationMethod {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public IAuthenticationMethodInputValidator validator();

	//-------------------------------------------------------------------------------------------------
	public IAuthenticationMethodInputNormalizer normalizer();

	//-------------------------------------------------------------------------------------------------
	public IAuthenticationMethodService service();

	//-------------------------------------------------------------------------------------------------
	public IAuthenticationMethodDbService dbService();
}
