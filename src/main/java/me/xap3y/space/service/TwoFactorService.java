package me.xap3y.space.service;

import jakarta.transaction.Transactional;
import me.xap3y.space.api.exception.BadRequestException;
import me.xap3y.space.api.exception.ResourceAccessForbiddenException;
import me.xap3y.space.entity.TwoFactorBackupCode;
import me.xap3y.space.entity.TwoFactorLoginChallenge;
import me.xap3y.space.entity.User;
import me.xap3y.space.entity.UserTwoFactor;
import me.xap3y.space.repository.TwoFactorLoginChallengeRepository;
import me.xap3y.space.repository.UserTwoFactorRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class TwoFactorService {
    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final String BACKUP_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final UserTwoFactorRepository configRepository;
    private final TwoFactorLoginChallengeRepository challengeRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();
    private final byte[] encryptionKey;

    public TwoFactorService(UserTwoFactorRepository configRepository,
                            TwoFactorLoginChallengeRepository challengeRepository,
                            PasswordEncoder passwordEncoder,
                            @Value("${security.two-factor.encryption-key}") String encryptionKey) {
        this.configRepository = configRepository;
        this.challengeRepository = challengeRepository;
        this.passwordEncoder = passwordEncoder;
        try { this.encryptionKey = MessageDigest.getInstance("SHA-256").digest(encryptionKey.getBytes(StandardCharsets.UTF_8)); }
        catch (Exception e) { throw new IllegalStateException("Unable to initialize 2FA encryption", e); }
    }

    public boolean isEnabled(User user) {
        return configRepository.findByUserId(user.getId()).map(UserTwoFactor::isEnabled).orElse(false);
    }

    @Transactional
    public void verifySensitiveAction(User user, String code) {
        if (!isEnabled(user)) return;
        if (code == null || code.isBlank()) {
            throw new BadRequestException("Two-factor authentication code is required");
        }
        if (!verifyCredential(enabledConfig(user), code, true)) {
            throw new BadRequestException("Invalid authentication or backup code");
        }
    }

    @Transactional
    public Map<String, Object> beginSetup(User user) {
        byte[] secretBytes = new byte[20]; random.nextBytes(secretBytes);
        String secret = base32Encode(secretBytes);
        UserTwoFactor config = configRepository.findByUserId(user.getId()).orElseGet(UserTwoFactor::new);
        if (config.isEnabled()) throw new BadRequestException("Two-factor authentication is already enabled");
        config.setUser(user); config.setEncryptedSecret(encrypt(secret)); config.setEnabled(false); config.setEnabledAt(null);
        config.getBackupCodes().clear();
        configRepository.save(config);
        String issuer = "Space";
        String uri = "otpauth://totp/" + url(issuer) + ":" + url(user.getEmail()) + "?secret=" + secret + "&issuer=" + url(issuer) + "&algorithm=SHA1&digits=6&period=30";
        return Map.of("secret", secret, "otpauthUri", uri);
    }

    @Transactional
    public List<String> confirmSetup(User user, String code) {
        UserTwoFactor config = configRepository.findByUserId(user.getId()).orElseThrow(() -> new BadRequestException("Start 2FA setup first"));
        if (!verifyTotp(decrypt(config.getEncryptedSecret()), code)) throw new BadRequestException("Invalid authentication code");
        config.setEnabled(true); config.setEnabledAt(LocalDateTime.now());
        List<String> codes = replaceBackupCodes(config);
        configRepository.save(config);
        return codes;
    }

    @Transactional
    public List<String> regenerateBackupCodes(User user, String code) {
        UserTwoFactor config = enabledConfig(user);
        if (!verifyCredential(config, code, false)) throw new BadRequestException("Invalid authentication code");
        List<String> codes = replaceBackupCodes(config);
        configRepository.save(config);
        return codes;
    }

    @Transactional
    public void disable(User user, String code) {
        UserTwoFactor config = enabledConfig(user);
        if (!verifyCredential(config, code, true)) throw new BadRequestException("Invalid authentication or backup code");
        configRepository.delete(config);
    }

    @Transactional
    public boolean forceDisable(User user) {
        Optional<UserTwoFactor> config = configRepository.findByUserId(user.getId());
        if (config.isEmpty() || !config.get().isEnabled()) return false;
        configRepository.delete(config.get());
        return true;
    }

    public String createLoginChallenge(User user) {
        byte[] tokenBytes = new byte[32]; random.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        TwoFactorLoginChallenge challenge = new TwoFactorLoginChallenge();
        challenge.setUser(user); challenge.setTokenHash(sha256(token)); challenge.setExpiresAt(LocalDateTime.now().plusMinutes(5)); challenge.setUsed(false); challenge.setFailedAttempts(0);
        challengeRepository.save(challenge);
        return token;
    }

    @Transactional
    public User verifyLoginChallenge(String token, String code) {
        if (token == null || token.isBlank() || code == null || code.isBlank()) throw new BadRequestException("Challenge and authentication code are required");
        TwoFactorLoginChallenge challenge = challengeRepository.findByTokenHashAndUsedFalse(sha256(token))
                .orElseThrow(() -> new ResourceAccessForbiddenException("Invalid or expired 2FA challenge"));
        if (challenge.getExpiresAt().isBefore(LocalDateTime.now())) {
            challenge.setUsed(true); challengeRepository.save(challenge);
            throw new ResourceAccessForbiddenException("The 2FA challenge expired. Please log in again");
        }
        UserTwoFactor config = enabledConfig(challenge.getUser());
        if (!verifyCredential(config, code, true)) {
            challenge.setFailedAttempts(challenge.getFailedAttempts() + 1);
            if (challenge.getFailedAttempts() >= 5) challenge.setUsed(true);
            challengeRepository.save(challenge);
            throw new ResourceAccessForbiddenException(challenge.isUsed() ? "Too many invalid codes. Please log in again" : "Invalid authentication or backup code");
        }
        challenge.setUsed(true); challengeRepository.save(challenge);
        return challenge.getUser();
    }

    @Transactional
    public Map<String, Object> status(User user) {
        return configRepository.findByUserId(user.getId()).filter(UserTwoFactor::isEnabled)
                .<Map<String, Object>>map(c -> Map.of("enabled", true, "enabledAt", c.getEnabledAt(), "backupCodesRemaining", c.getBackupCodes().stream().filter(b -> b.getUsedAt() == null).count()))
                .orElseGet(() -> Map.of("enabled", false, "backupCodesRemaining", 0));
    }

    private UserTwoFactor enabledConfig(User user) {
        return configRepository.findByUserId(user.getId()).filter(UserTwoFactor::isEnabled)
                .orElseThrow(() -> new BadRequestException("Two-factor authentication is not enabled"));
    }

    private boolean verifyCredential(UserTwoFactor config, String input, boolean allowBackup) {
        String normalized = input == null ? "" : input.replace(" ", "").trim();
        if (verifyTotp(decrypt(config.getEncryptedSecret()), normalized)) return true;
        if (!allowBackup) return false;
        String backup = normalized.replace("-", "").toUpperCase(Locale.ROOT);
        for (TwoFactorBackupCode candidate : config.getBackupCodes()) {
            if (candidate.getUsedAt() == null && passwordEncoder.matches(backup, candidate.getCodeHash())) {
                candidate.setUsedAt(LocalDateTime.now());
                configRepository.save(config);
                return true;
            }
        }
        return false;
    }

    private List<String> replaceBackupCodes(UserTwoFactor config) {
        config.getBackupCodes().clear();
        List<String> plain = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String raw = randomChars(10);
            String display = raw.substring(0, 5) + "-" + raw.substring(5);
            TwoFactorBackupCode entity = new TwoFactorBackupCode();
            entity.setTwoFactor(config); entity.setCodeHash(passwordEncoder.encode(raw));
            config.getBackupCodes().add(entity); plain.add(display);
        }
        return plain;
    }

    private boolean verifyTotp(String secret, String input) {
        if (input == null || !input.matches("\\d{6}")) return false;
        long counter = Instant.now().getEpochSecond() / 30;
        for (long offset = -1; offset <= 1; offset++) if (totp(secret, counter + offset).equals(input)) return true;
        return false;
    }

    private String totp(String secret, long counter) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(base32Decode(secret), "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array());
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24) | ((hash[offset + 1] & 0xff) << 16) | ((hash[offset + 2] & 0xff) << 8) | (hash[offset + 3] & 0xff);
            return String.format("%06d", binary % 1_000_000);
        } catch (Exception e) { throw new IllegalStateException("Unable to verify TOTP", e); }
    }

    private String encrypt(String value) {
        try {
            byte[] iv = new byte[12]; random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
        } catch (Exception e) { throw new IllegalStateException("Unable to encrypt 2FA secret", e); }
    }

    private String decrypt(String value) {
        try {
            byte[] all = Base64.getDecoder().decode(value); byte[] iv = Arrays.copyOfRange(all, 0, 12); byte[] encrypted = Arrays.copyOfRange(all, 12, all.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) { throw new IllegalStateException("Unable to decrypt 2FA secret", e); }
    }

    private String base32Encode(byte[] bytes) {
        StringBuilder out = new StringBuilder(); int buffer = 0, bits = 0;
        for (byte b : bytes) { buffer = (buffer << 8) | (b & 255); bits += 8; while (bits >= 5) { out.append(BASE32.charAt((buffer >> (bits - 5)) & 31)); bits -= 5; } }
        if (bits > 0) out.append(BASE32.charAt((buffer << (5 - bits)) & 31));
        return out.toString();
    }

    private byte[] base32Decode(String input) {
        ByteArrayOutputStreamEx out = new ByteArrayOutputStreamEx(); int buffer = 0, bits = 0;
        for (char c : input.toUpperCase(Locale.ROOT).toCharArray()) { int value = BASE32.indexOf(c); if (value < 0) continue; buffer = (buffer << 5) | value; bits += 5; if (bits >= 8) { out.write((buffer >> (bits - 8)) & 255); bits -= 8; } }
        return out.toByteArray();
    }

    private String randomChars(int length) { StringBuilder b = new StringBuilder(); for (int i = 0; i < length; i++) b.append(BACKUP_ALPHABET.charAt(random.nextInt(BACKUP_ALPHABET.length()))); return b.toString(); }
    private String sha256(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception e) { throw new IllegalStateException(e); } }
    private String url(String value) { return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20"); }
    private static class ByteArrayOutputStreamEx extends java.io.ByteArrayOutputStream { public byte[] toByteArray() { return super.toByteArray(); } }
}
