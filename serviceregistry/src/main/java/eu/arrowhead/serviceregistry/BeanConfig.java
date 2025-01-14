package eu.arrowhead.serviceregistry;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import eu.arrowhead.common.collector.ICollectorDriver;
import eu.arrowhead.serviceregistry.collector.DatabaseCollectorDriver;

@Configuration
public class BeanConfig {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Primary
	@Bean
	ICollectorDriver getCollectorDriver() {
		return new DatabaseCollectorDriver();
	}
}