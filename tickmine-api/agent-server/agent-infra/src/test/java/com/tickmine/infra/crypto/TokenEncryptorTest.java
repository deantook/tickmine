package com.tickmine.infra.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class TokenEncryptorTest {

    private final TokenEncryptor encryptor = new TokenEncryptor(
            Base64.getDecoder().decode("0123456789ABCDEF0123456789ABCDEF"));

    @Test
    void roundTrip() {
        String plain = "ticktick-token-abc123";
        String enc = encryptor.encrypt(plain);
        assertNotEquals(plain, enc);
        assertEquals(plain, encryptor.decrypt(enc));
    }
}
