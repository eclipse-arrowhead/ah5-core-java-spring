package eu.arrowhead.authentication.method.password;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.arrowhead.authentication.jpa.entity.PasswordAuthentication;
import eu.arrowhead.authentication.jpa.repository.PasswordAuthenticationRepository;
import eu.arrowhead.authentication.method.IAuthenticationMethodDbService;
import eu.arrowhead.authentication.service.dto.IdentityData;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;

@Service
public class PasswordAuthenticationMethodDbService implements IAuthenticationMethodDbService {

	//=================================================================================================
	// members

	@Autowired
	private PasswordEncoder encoder;

	@Autowired
	private PasswordAuthenticationRepository paRepository;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	@Transactional(rollbackFor = ArrowheadException.class, propagation = Propagation.REQUIRED)
	public List<String> createIdentifiableSystemsInBulk(final List<IdentityData> identities) {
		logger.debug("PasswordAuthenticationMethodDbService.createIdentifiableSystemsInBulk started...");

		try {
			final List<PasswordAuthentication> entities = createEntities(identities);
			paRepository.saveAllAndFlush(entities);

			// intentionally
			return null;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private List<PasswordAuthentication> createEntities(final List<IdentityData> identities) {
		logger.debug("PasswordAuthenticationMethodDbService.createEntities started...");
		Assert.notNull(identities, "Identities list is missing");
		Assert.isTrue(!Utilities.containsNull(identities), "Identities list contains null value");

		final List<PasswordAuthentication> result = new ArrayList<>(identities.size());
		for (final IdentityData identityData : identities) {
			Assert.notNull(identityData.system(), "system is null");
			Assert.notNull(identityData.credentials(), "credentials is null");
			Assert.isTrue(!Utilities.isEmpty(identityData.credentials().get(PasswordAuthenticationMethod.KEY_PASSWORD)), "password field is missing or empty");

			final String encodedPassword = encoder.encode(identityData.credentials().get(PasswordAuthenticationMethod.KEY_PASSWORD));
			result.add(new PasswordAuthentication(identityData.system(), encodedPassword));
		}

		return result;
	}
}