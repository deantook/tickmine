package com.tickmine.infra.config;

import com.tickmine.infra.crypto.EncryptionKeyResolver;
import com.tickmine.infra.crypto.TokenEncryptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TickMineProperties.class)
public class CryptoConfig {

    @Bean
    public TokenEncryptor tokenEncryptor(TickMineProperties props) {
        byte[] key = EncryptionKeyResolver.resolve(props.getEncryption().getSecretKey());
        return new TokenEncryptor(key);
    }
}
