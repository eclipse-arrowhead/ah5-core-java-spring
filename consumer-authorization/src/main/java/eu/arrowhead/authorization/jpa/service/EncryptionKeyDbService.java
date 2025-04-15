package eu.arrowhead.authorization.jpa.service;

import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.jpa.entity.CryptographerIV;
import eu.arrowhead.authorization.jpa.entity.EncryptionKey;
import eu.arrowhead.authorization.jpa.repository.CryptographerIVRepository;
import eu.arrowhead.authorization.jpa.repository.EncryptionKeyRepository;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;

@Service
public class EncryptionKeyDbService {

	//=================================================================================================
	// members	
	
	@Autowired
	private EncryptionKeyRepository keyRepo;
	
	@Autowired
	private CryptographerIVRepository ivRepo;
	
	private final Logger logger = LogManager.getLogger(this.getClass());
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public Optional<EncryptionKey> get(final String systemName) {
		Assert.isTrue(!Utilities.isEmpty(systemName), "systemName is empty");
		
		try {
			return keyRepo.findBySystemName(systemName);
			
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public Pair<EncryptionKey, Boolean> save(final String systemName, String keyBase64, final String ivBase64) {
		Assert.isTrue(!Utilities.isEmpty(systemName), "systemName is empty");
		Assert.isTrue(!Utilities.isEmpty(keyBase64), "keyBase64 is empty");
		Assert.isTrue(!Utilities.isEmpty(ivBase64), "ivBase64 is empty");

		try {
			boolean override = false;
			final CryptographerIV ivRecord = ivRepo.saveAndFlush(new CryptographerIV(ivBase64));

			EncryptionKey toSave = new EncryptionKey(systemName, keyBase64, ivRecord);
			final Optional<EncryptionKey> optional = keyRepo.findBySystemName(systemName);
			if (optional.isPresent()) {
				toSave = optional.get();
				toSave.setKey(keyBase64);
				toSave.setIv(ivRecord);
				override = true;
			}
			return Pair.of(keyRepo.saveAndFlush(toSave), !override);
			
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public boolean delete(final String systemName) {
		Assert.isTrue(!Utilities.isEmpty(systemName), "systemName is empty");
		
		try {
			final Optional<EncryptionKey> optional = keyRepo.findBySystemName(systemName);
			if (optional.isEmpty()) {
				return false;
			}
			
			keyRepo.deleteById(optional.get().getId());
			return true;
			
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}
}
