package app.finwave.backend.utils;

import app.finwave.backend.utils.params.InvalidParameterException;
import app.finwave.backend.utils.params.validators.URLValidator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

class URLValidatorTest {

    private URLValidator validator;

    @BeforeEach
    void setUp() throws MalformedURLException {
        validator = new URLValidator(new URL("http://example.com"));
    }

    @Test
    void testProtocolAnyMatches_ValidInput() {
        assertDoesNotThrow(() -> validator.protocolAnyMatches("http", "https"));
    }

    @Test
    void testProtocolAnyMatches_InvalidInput() {
        assertThrows(InvalidParameterException.class, () -> validator.protocolAnyMatches("ftp"));
    }

    @Test
    void testNotLocalAddress_ValidInput() {
        assertDoesNotThrow(() -> validator.notLocalAddress());
    }

    @Test
    void testNotLocalAddress_InvalidInput() throws MalformedURLException {
        validator = new URLValidator(new URL("http://localhost"));
        assertThrows(InvalidParameterException.class, () -> validator.notLocalAddress());
    }

    @Test
    void testNotLocalAddress_InvalidInput_Loopback() throws MalformedURLException {
        validator = new URLValidator(new URL("http://127.0.0.1"));
        assertThrows(InvalidParameterException.class, () -> validator.notLocalAddress());
    }

    @Test
    void testNotLocalAddress_InvalidInput_SiteLocal() throws MalformedURLException {
        validator = new URLValidator(new URL("http://192.168.1.1")); // Example of a site-local address
        assertThrows(InvalidParameterException.class, () -> validator.notLocalAddress());
    }

    @Test
    void testNotLocalAddress_ExceptionInLookup() throws MalformedURLException {
        validator = new URLValidator(new URL("http://invalid-url")); // Invalid URL to trigger exception
        assertThrows(InvalidParameterException.class, () -> validator.notLocalAddress());
    }
}
