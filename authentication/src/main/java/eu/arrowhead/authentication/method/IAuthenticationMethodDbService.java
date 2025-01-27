package eu.arrowhead.authentication.method;

import java.util.List;

import eu.arrowhead.authentication.service.dto.IdentityData;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.InternalServerError;

public interface IAuthenticationMethodDbService {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// If the method implementation wants to store something in the System entities' extra field it can use the return value list.
	// The return list must use the same ordering than the input.
	// The return list can be null if extra field is not used
	public List<String> createIdentifiableSystemsInBulk(final List<IdentityData> identities) throws InternalServerError, ExternalServerError;

	//-------------------------------------------------------------------------------------------------
	public default void rollbackCreateIdentifiableSystemsInBulk(final List<IdentityData> identities) {
		// intentionally do nothing
	};
}