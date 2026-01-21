package com.cas.server.config;

import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class JwtKeyConfig {

    private final CasProperties casProperties;

    @Value("${cas.jwt.private-key-path:#{null}}")
    private Resource privateKeyResource;

    @Value("${cas.jwt.public-key-path:#{null}}")
    private Resource publicKeyResource;

    @Getter
    private KeyPair keyPair;

    @Getter
    private RSAPublicKey publicKey;

    @Getter
    private RSAPrivateKey privateKey;

    @PostConstruct
    public void init() throws Exception {
        if (privateKeyResource != null && privateKeyResource.exists() &&
                publicKeyResource != null && publicKeyResource.exists()) {
            log.info("Loading RSA keys from files");
            loadKeysFromFiles();
        } else {
            log.warn("RSA key files not found, generating new key pair. NOT FOR PRODUCTION!");
            generateKeyPair();
        }

        log.info("JWT key configuration initialized with key ID: {}", casProperties.getJwt().getKeyId());
    }

    private void loadKeysFromFiles() throws Exception {
        String privateKeyPem = new String(privateKeyResource.getInputStream().readAllBytes())
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        String publicKeyPem = new String(publicKeyResource.getInputStream().readAllBytes())
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyPem);
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyPem);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        this.privateKey = (RSAPrivateKey) keyFactory.generatePrivate(privateSpec);

        X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(publicKeyBytes);
        this.publicKey = (RSAPublicKey) keyFactory.generatePublic(publicSpec);

        this.keyPair = new KeyPair(publicKey, privateKey);
    }

    private void generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        this.keyPair = generator.generateKeyPair();
        this.publicKey = (RSAPublicKey) keyPair.getPublic();
        this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
    }

    public String getKeyId() {
        return casProperties.getJwt().getKeyId();
    }
}
