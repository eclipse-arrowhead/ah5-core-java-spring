package eu.arrowhead.serviceregistry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.jpa.RefreshableRepositoryImpl;

@SpringBootApplication
@ComponentScan(Constants.BASE_PACKAGE)
@EntityScan(ServiceRegistryConstants.DATABASE_ENTITY_PACKAGE)
@EnableJpaRepositories(basePackages = ServiceRegistryConstants.DATABASE_REPOSITORY_PACKAGE, repositoryBaseClass = RefreshableRepositoryImpl.class)
public class ServiceRegistryMain {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static void main(final String[] args) {
		
		SpringApplication.run(ServiceRegistryMain.class, args);
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	protected ServiceRegistryMain() {

	}

}
