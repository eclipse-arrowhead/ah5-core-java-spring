package eu.arrowhead.serviceorchestration.service.model;

import java.util.Map;

import eu.arrowhead.dto.ServiceInstanceResponseDTO;

public class OrchestrationCandidate {

	//=================================================================================================
	// members

	private final ServiceInstanceResponseDTO serviceInstance;
	private final boolean isLocal;

	// private Cloud cloud; not supported yet
	private boolean isLocked;
	private boolean canBeExclusive;
	private boolean translationNeeded;
	private Map<String, String> authorizationTokens;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationCandidate(final ServiceInstanceResponseDTO serviceInstance, final boolean isLocal) {
		this.serviceInstance = serviceInstance;
		this.isLocal = isLocal;
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceInstanceResponseDTO getServiceInstance() {
		return serviceInstance;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isLocal() {
		return isLocal;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isLocked() {
		return isLocked;
	}

	//-------------------------------------------------------------------------------------------------
	public void setLocked(final boolean locked) {
		isLocked = locked;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean canBeExclusive() {
		return canBeExclusive;
	}

	//-------------------------------------------------------------------------------------------------
	public void setCanBeExclusive(final boolean canBeExclusive) {
		this.canBeExclusive = canBeExclusive;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isTranslationNeeded() {
		return translationNeeded;
	}

	//-------------------------------------------------------------------------------------------------
	public void setTranslationNeeded(final boolean translationNeeded) {
		this.translationNeeded = translationNeeded;
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, String> getAuthorizationTokens() {
		return authorizationTokens;
	}

	//-------------------------------------------------------------------------------------------------
	public void setAuthorizationTokens(final Map<String, String> authorizationTokens) {
		this.authorizationTokens = authorizationTokens;
	}
}
