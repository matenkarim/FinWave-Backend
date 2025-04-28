package app.finwave.backend.utils;

import app.finwave.backend.utils.params.InvalidParameterException;
import app.finwave.backend.utils.params.validators.IntValidator;
import app.finwave.backend.utils.params.validators.ValidatorFunc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class IntValidatorTest {

    private IntValidator validator;

    @BeforeEach
    void setUp() {
        validator = new IntValidator(10);
    }

    @Test
    void testRange_ValidInput() {
        assertDoesNotThrow(() -> validator.range(5, 15));
    }

    @Test
    void testRange_InvalidInput() {
        validator = new IntValidator(20);
        assertThrows(InvalidParameterException.class, () -> validator.range(5, 15));
    }

    @Test
    void testRange_NullInput() {
        validator = new IntValidator(null);
        assertDoesNotThrow(() -> validator.range(5, 15)); // Should not throw as null is valid
    }

    @Test
    void testMatches_ValidInput() {
        ValidatorFunc<Integer, Boolean> isPositive = input -> input > 0;
        assertDoesNotThrow(() -> validator.matches(isPositive));
    }

    @Test
    void testMatches_InvalidInput() {
        validator = new IntValidator(-5);
        ValidatorFunc<Integer, Boolean> isPositive = input -> input > 0;
        assertThrows(InvalidParameterException.class, () -> validator.matches(isPositive));
    }

    @Test
    void testMatches_ExceptionInValidator() {
        ValidatorFunc<Integer, Boolean> throwsException = input -> {
            throw new RuntimeException("Validation error");
        };
        assertDoesNotThrow(() -> validator.matches(throwsException));
    }

    @Test
    void testMatches_FalseReturn() {
        ValidatorFunc<Integer, Boolean> alwaysFalse = input -> false;
        assertThrows(InvalidParameterException.class, () -> validator.matches(alwaysFalse));
    }

    @Test
    void testNullString() {
        IntValidator validator2 = new IntValidator(null, "null value");
        ValidatorFunc<Integer, Boolean> checkNull = input -> input == null;
        assertDoesNotThrow(() -> validator2.matches(checkNull));
    }
}