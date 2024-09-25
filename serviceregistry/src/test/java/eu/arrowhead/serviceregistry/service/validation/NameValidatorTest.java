package eu.arrowhead.serviceregistry.service.validation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.serviceregistry.service.validation.name.NameValidator;

public class NameValidatorTest {
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void validateNameTest() {
		assertThrows(InvalidParameterException.class, () -> { NameValidator.validateName("cont@ins-invalid-character"); });
		assertThrows(InvalidParameterException.class, () -> { NameValidator.validateName("ends-with-dash-"); });
		assertThrows(InvalidParameterException.class, () -> { NameValidator.validateName("-starts-with-dash"); });
		assertThrows(InvalidParameterException.class, () -> { NameValidator.validateName("1-starts-with-number"); });
		assertAll(
			() -> NameValidator.validateName("correct-123-name"),
			() -> NameValidator.validateName("Correct-123-Name"),
			() -> NameValidator.validateName("ends-with-number-88"),
			() -> NameValidator.validateName("x"));
	}
}
