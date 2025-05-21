package eu.arrowhead.authorization.service.model;

import java.time.ZonedDateTime;

import eu.arrowhead.authorization.jpa.entity.SelfContainedToken;
import eu.arrowhead.authorization.jpa.entity.TimeLimitedToken;
import eu.arrowhead.authorization.jpa.entity.TokenHeader;
import eu.arrowhead.authorization.jpa.entity.UsageLimitedToken;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.dto.enums.AuthorizationTokenType;

public class TokenModel {

	//=================================================================================================
	// members
	
	private final TokenHeader header;
	
	private final String rawToken;
	private String enrcyptedToken;
	
	private String variant;
	private Integer usageLimit;
	private Integer usageLeft;
	private ZonedDateTime expiresAt;
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public TokenModel(final TokenHeader header) {
		this.header = header;
		this.rawToken = null;
	}
	
	//-------------------------------------------------------------------------------------------------
	public TokenModel(final TokenHeader header, final String rawToken) {
		this.header = header;
		this.rawToken = rawToken;
	}
	
	//-------------------------------------------------------------------------------------------------
	public TokenModel(final UsageLimitedToken token) {
		this.header = token.getHeader();
		this.usageLimit = token.getUsageLimit();
		this.usageLeft = token.getUsageLeft();
		this.rawToken = null;
	}
	
	//-------------------------------------------------------------------------------------------------
	public TokenModel(final UsageLimitedToken token, final String rawToken) {
		this.header = token.getHeader();
		this.usageLimit = token.getUsageLimit();
		this.usageLeft = token.getUsageLeft();
		this.rawToken = rawToken;
	}
	
	//-------------------------------------------------------------------------------------------------
	public TokenModel(final TimeLimitedToken token) {
		this.header = token.getHeader();
		this.expiresAt = token.getExpiresAt();
		this.rawToken = null;
	}
	
	//-------------------------------------------------------------------------------------------------
	public TokenModel(final TimeLimitedToken token, final String rawToken) {
		this.header = token.getHeader();
		this.expiresAt = token.getExpiresAt();
		this.rawToken = rawToken;
	}
	
	//-------------------------------------------------------------------------------------------------
	public TokenModel(final SelfContainedToken token) {
		this.header = token.getHeader();
		this.variant = token.getVariant();
		this.expiresAt = token.getExpiresAt();
		this.rawToken = null;
	}
	
	//-------------------------------------------------------------------------------------------------
	public TokenModel(final SelfContainedToken token, final String rawToken) {
		this.header = token.getHeader();
		this.variant = token.getVariant();
		this.expiresAt = token.getExpiresAt();
		this.rawToken = rawToken;
	}
	
	//-------------------------------------------------------------------------------------------------
	public boolean isEncrypted() {
		return !Utilities.isEmpty(enrcyptedToken);
	}

	//=================================================================================================
	// boilerplate methods
	
	//-------------------------------------------------------------------------------------------------
	public AuthorizationTokenType getTokenType() {
		return header.getTokenType();
	}

	//-------------------------------------------------------------------------------------------------
	public String getRawToken() {
		return rawToken;
	}
	
	//-------------------------------------------------------------------------------------------------
	public String getHashedToken() {
		return header.getTokenHash();
	}

	//-------------------------------------------------------------------------------------------------
	public String getRequester() {
		return header.getRequester();
	}

	//-------------------------------------------------------------------------------------------------
	public String getConsumerCloud() {
		return header.getConsumerCloud();
	}
	
	//-------------------------------------------------------------------------------------------------
	public String getConsumer() {
		return header.getConsumer();
	}

	//-------------------------------------------------------------------------------------------------
	public String getProvider() {
		return header.getProvider();
	}
	
	//-------------------------------------------------------------------------------------------------
	public AuthorizationTargetType getTargetType() {
		return header.getTargetType();
	}
	
	//-------------------------------------------------------------------------------------------------
	public String getTarget() {
		return header.getTarget();
	}

	//-------------------------------------------------------------------------------------------------
	public String getScope() {
		return header.getScope();
	}

	//-------------------------------------------------------------------------------------------------
	public ZonedDateTime getCreatedAt() {
		return header.getCreatedAt();
	}
	
	//-------------------------------------------------------------------------------------------------
	public String getEnrcyptedToken() {
		return enrcyptedToken;
	}

	//-------------------------------------------------------------------------------------------------
	public void setEnrcyptedToken(final String enrcyptedToken) {
		this.enrcyptedToken = enrcyptedToken;
	}

	//-------------------------------------------------------------------------------------------------
	public String getVariant() {
		return variant;
	}

	//-------------------------------------------------------------------------------------------------
	public Integer getUsageLimit() {
		return usageLimit;
	}

	//-------------------------------------------------------------------------------------------------
	public Integer getUsageLeft() {
		return usageLeft;
	}

	//-------------------------------------------------------------------------------------------------
	public ZonedDateTime getExpiresAt() {
		return expiresAt;
	}
}
