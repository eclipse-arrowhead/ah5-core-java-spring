package eu.arrowhead.authorization.jpa.entity;

import java.util.List;

import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.jpa.UnmodifiableArrowheadEntity;
import eu.arrowhead.dto.enums.AuthorizationTokenType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
public class TokenHeader extends UnmodifiableArrowheadEntity {

	//=================================================================================================
	// members

	public static final List<String> SORTABLE_FIELDS_BY = List.of("id", "tokenType", "requester", "consumerCloud", "consumer", "provider", "serviceDefinition", "createdBy");
	public static final String DEFAULT_SORT_FIELD = "createdBy";

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private AuthorizationTokenType tokenType;

	@Column(nullable = false)
	private String token;

	@OneToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "internalAuxiliaryId", referencedColumnName = "id", nullable = false)
	private CryptographerAuxiliary internalAuxiliary;

	@Column(nullable = false, length = VARCHAR_SMALL)
	private String requester;

	@Column(nullable = false, length = VARCHAR_MEDIUM)
	private String consumerCloud = Defaults.DEFAULT_CLOUD;

	@Column(nullable = false, length = VARCHAR_SMALL)
	private String consumer;

	@Column(nullable = false, length = VARCHAR_SMALL)
	private String provider;

	@Column(nullable = false, length = VARCHAR_SMALL)
	private String serviceDefinition;

	@Column(nullable = true, length = VARCHAR_SMALL)
	private String serviceOperation;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public TokenHeader() {

	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:parameternumber")
	public TokenHeader(
			final AuthorizationTokenType tokenType,
			final String token,
			final CryptographerAuxiliary internalAuxiliary,
			final String requester,
			final String consumerCloud,
			final String consumer,
			final String provider,
			final String serviceDefinition,
			final String serviceOperation) {
		this.tokenType = tokenType;
		this.token = token;
		this.internalAuxiliary = internalAuxiliary;
		this.requester = requester;
		this.consumerCloud = consumerCloud;
		this.consumer = consumer;
		this.provider = provider;
		this.serviceDefinition = serviceDefinition;
		this.serviceOperation = Utilities.isEmpty(serviceOperation) ? null : serviceOperation;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "TokenHeader [tokenType=" + tokenType + ", token=" + token + ", internalAuxiliary=" + internalAuxiliary + ", requester=" + requester + ", consumerCloud=" + consumerCloud + ", consumer=" + consumer + ", provider=" + provider
				+ ", serviceDefinition=" + serviceDefinition + ", serviceOperation=" + serviceOperation + ", id=" + id + ", createdAt=" + createdAt + "]";
	}

	//=================================================================================================
	// boilerplate code

	//-------------------------------------------------------------------------------------------------
	public AuthorizationTokenType getTokenType() {
		return tokenType;
	}

	//-------------------------------------------------------------------------------------------------
	public void setTokenType(final AuthorizationTokenType tokenType) {
		this.tokenType = tokenType;
	}

	//-------------------------------------------------------------------------------------------------
	public String getToken() {
		return token;
	}

	//-------------------------------------------------------------------------------------------------
	public void setToken(final String token) {
		this.token = token;
	}

	//-------------------------------------------------------------------------------------------------
	public CryptographerAuxiliary getInternalAuxiliary() {
		return internalAuxiliary;
	}

	//-------------------------------------------------------------------------------------------------
	public void setInternalAuxiliary(final CryptographerAuxiliary internalAuxiliary) {
		this.internalAuxiliary = internalAuxiliary;
	}

	//-------------------------------------------------------------------------------------------------
	public String getRequester() {
		return requester;
	}

	//-------------------------------------------------------------------------------------------------
	public void setRequester(final String requester) {
		this.requester = requester;
	}

	//-------------------------------------------------------------------------------------------------
	public String getConsumerCloud() {
		return consumerCloud;
	}

	//-------------------------------------------------------------------------------------------------
	public void setConsumerCloud(final String consumerCloud) {
		this.consumerCloud = consumerCloud;
	}

	//-------------------------------------------------------------------------------------------------
	public String getConsumer() {
		return consumer;
	}

	//-------------------------------------------------------------------------------------------------
	public void setConsumer(final String consumer) {
		this.consumer = consumer;
	}

	//-------------------------------------------------------------------------------------------------
	public String getProvider() {
		return provider;
	}

	//-------------------------------------------------------------------------------------------------
	public void setProvider(final String provider) {
		this.provider = provider;
	}

	//-------------------------------------------------------------------------------------------------
	public String getServiceDefinition() {
		return serviceDefinition;
	}

	//-------------------------------------------------------------------------------------------------
	public void setServiceDefinition(final String serviceDefinition) {
		this.serviceDefinition = serviceDefinition;
	}

	//-------------------------------------------------------------------------------------------------
	public String getServiceOperation() {
		return serviceOperation;
	}

	//-------------------------------------------------------------------------------------------------
	public void setServiceOperation(final String serviceOperation) {
		this.serviceOperation = serviceOperation;
	}
}