package com.tickmine.infra.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EncryptionKeyResolverTest {

    @Test
    void resolvesBase64Key() {
        byte[] key = EncryptionKeyResolver.resolve("MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=");
        assertThat(key).hasSize(32);
    }

    @Test
    void resolvesRaw32CharKey() {
        byte[] key = EncryptionKeyResolver.resolve("dev-key-32bytes-long!!!!!!!!!!!!");
        assertThat(key).hasSize(32);
    }

    @Test
    void rejectsUnresolvedPlaceholder() {
        assertThatThrownBy(() -> EncryptionKeyResolver.resolve("${TICKMINE_ENCRYPTION_KEY}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unresolved");
    }
}
