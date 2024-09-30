package eu.arrowhead.serviceregistry.service.validation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.serviceregistry.service.validation.name.NameValidator;

@SpringBootTest
public class NameValidatorTest {

	//=================================================================================================
	// members

	@Autowired
	private NameValidator nameValidator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void validateNameTest() {
		assertThrows(InvalidParameterException.class, () -> {
			nameValidator.validateName("cont@ins-invalid-character");
			});
		assertThrows(InvalidParameterException.class, () -> {
			nameValidator.validateName("ends-with-dash-");
		});
		assertThrows(InvalidParameterException.class, () -> {
			nameValidator.validateName("-starts-with-dash");
		});
		assertThrows(InvalidParameterException.class, () -> {
			nameValidator.validateName("1-starts-with-number");
		});
		assertAll(
			() -> nameValidator.validateName("correct-123-name"),
			() -> nameValidator.validateName("Correct-123-Name"),
			() -> nameValidator.validateName("ends-with-number-88"),
			() -> nameValidator.validateName("x"));
	}
}
