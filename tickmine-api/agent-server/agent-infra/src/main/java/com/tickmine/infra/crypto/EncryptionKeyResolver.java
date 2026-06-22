package com.tickmine.infra.crypto;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class EncryptionKeyResolver {

    private EncryptionKeyResolver() {}

    public static byte[] resolve(String configuredKey) {
        if (configuredKey == null || configuredKey.isBlank()) {
            throw new IllegalArgumentException(
                    "tickmine.encryption.secret-key is required. "
                            + "Set TICKMINE_ENCRYPTION_KEY to a Base64-encoded 32-byte AES key.");
        }
        if (configuredKey.contains("${")) {
            throw new IllegalArgumentException(
                    "tickmine.encryption.secret-key is unresolved. "
                            + "Set environment variable TICKMINE_ENCRYPTION_KEY or configure tickmine.encryption.secret-key.");
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(configuredKey);
            if (decoded.length == 16 || decoded.length == 24 || decoded.length == 32) {
                return decoded;
            }
        } catch (IllegalArgumentException ignored) {
            // fall through to raw UTF-8 interpretation
        }

        byte[] raw = configuredKey.getBytes(StandardCharsets.UTF_8);
        if (raw.length == 16 || raw.length == 24 || raw.length == 32) {
            return raw;
        }

        throw new IllegalArgumentException(
                "tickmine.encryption.secret-key must be Base64-encoded 16/24/32-byte key "
                        + "or a raw 16/24/32-character string.");
    }
}
