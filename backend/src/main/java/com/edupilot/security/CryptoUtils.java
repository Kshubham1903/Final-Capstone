package com.edupilot.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class CryptoUtils {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;
    private static final String MASTER_KEY_FILENAME = ".edupilot_master_key";
    private static final String LEGACY_DEV_KEY_FILENAME = ".edupilot_dev_master_key";

    @Value("${EDUPILOT_CREDENTIAL_ENCRYPTION_KEY:${edupilot.credential-encryption-key:}}")
    private String secretKeySource;

    @Autowired(required = false)
    private Environment environment;

    private String cachedResolvedKey;

    public synchronized String resolveMasterKeySource() {
        if (secretKeySource != null && !secretKeySource.isBlank()) {
            return secretKeySource;
        }

        if (cachedResolvedKey != null && !cachedResolvedKey.isBlank()) {
            return cachedResolvedKey;
        }

        String masterKey = getOrGenerateMasterKey();
        cachedResolvedKey = masterKey;
        return masterKey;
    }

    private Path resolveMasterKeyFilePath() {
        Path p1 = Paths.get("backend", MASTER_KEY_FILENAME);
        if (Files.exists(p1)) return p1;

        Path p2 = Paths.get(MASTER_KEY_FILENAME);
        if (Files.exists(p2)) return p2;

        Path p3 = Paths.get("backend", LEGACY_DEV_KEY_FILENAME);
        if (Files.exists(p3)) return p3;

        Path p4 = Paths.get(LEGACY_DEV_KEY_FILENAME);
        if (Files.exists(p4)) return p4;

        if (Files.isDirectory(Paths.get("backend"))) {
            return p1;
        }
        return p2;
    }

    private synchronized String getOrGenerateMasterKey() {
        Path keyFilePath = resolveMasterKeyFilePath();
        if (Files.exists(keyFilePath)) {
            try {
                java.util.List<String> lines = Files.readAllLines(keyFilePath, StandardCharsets.UTF_8);
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("EDUPILOT_MASTER_KEY=") || trimmed.startsWith("EDUPILOT_DEV_MASTER_KEY=")) {
                        String keyVal = trimmed.substring(trimmed.indexOf('=') + 1).trim();
                        if (!keyVal.isBlank()) {
                            return keyVal;
                        }
                    }
                }
                String fullContent = Files.readString(keyFilePath, StandardCharsets.UTF_8).trim();
                if (!fullContent.isBlank() && !fullContent.startsWith("#")) {
                    return fullContent;
                }
                throw new IllegalStateException("EduPilot master key file at " + keyFilePath.toAbsolutePath() + " is empty or missing key value.");
            } catch (Exception ex) {
                if (ex instanceof IllegalStateException) {
                    throw (IllegalStateException) ex;
                }
                throw new IllegalStateException("EduPilot master key file exists at " + keyFilePath.toAbsolutePath() + " but could not be read. Fix or remove the file to reinitialize.", ex);
            }
        }

        try {
            byte[] keyBytes = new byte[32];
            new SecureRandom().nextBytes(keyBytes);
            String generatedKey = Base64.getEncoder().encodeToString(keyBytes);
            String fileContent = "# EduPilot Auto-Generated Master Encryption Key - DO NOT COMMIT TO GIT\nEDUPILOT_MASTER_KEY=" + generatedKey + "\n";
            Files.writeString(keyFilePath, fileContent, StandardCharsets.UTF_8);
            System.out.println("[CryptoUtils] Automatically created master encryption key and saved to: " + keyFilePath.toAbsolutePath());
            return generatedKey;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate and persist master encryption key to " + keyFilePath.toAbsolutePath(), ex);
        }
    }

    private SecretKey getSecretKey() {
        String source = resolveMasterKeySource();
        if (source == null || source.isBlank()) {
            throw new IllegalStateException("EDUPILOT_CREDENTIAL_ENCRYPTION_KEY configuration is missing or empty.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to initialize credential encryption key", ex);
        }
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), parameterSpec);

            byte[] cipherTextBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + cipherTextBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherTextBytes, 0, combined, iv.length, cipherTextBytes.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to encrypt API key", ex);
        }
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isBlank()) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedText);
            if (combined.length < IV_LENGTH_BYTES) {
                return null;
            }

            byte[] iv = new byte[IV_LENGTH_BYTES];
            byte[] cipherTextBytes = new byte[combined.length - IV_LENGTH_BYTES];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTES);
            System.arraycopy(combined, IV_LENGTH_BYTES, cipherTextBytes, 0, cipherTextBytes.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), parameterSpec);

            byte[] plainTextBytes = cipher.doFinal(cipherTextBytes);
            return new String(plainTextBytes, StandardCharsets.UTF_8);
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            System.err.println("[CryptoUtils] Decryption failed or invalid ciphertext: " + ex.getMessage());
            return null;
        }
    }
}
