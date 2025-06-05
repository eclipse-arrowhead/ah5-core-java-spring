package eu.arrowhead.authorization.jpa.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import eu.arrowhead.authorization.jpa.entity.EncryptionKey;
import eu.arrowhead.common.jpa.RefreshableRepository;

@Repository
public interface EncryptionKeyRepository extends RefreshableRepository<EncryptionKey, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Optional<EncryptionKey> findBySystemName(final String systemName);
}