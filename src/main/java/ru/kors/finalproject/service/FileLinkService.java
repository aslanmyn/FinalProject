package ru.kors.finalproject.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Service
public class FileLinkService {

    private final String signingSecret;
    private final long ttlMinutes;

    public FileLinkService(
            @Value("${app.storage.signing-secret:change-me-storage-signing-secret}") String signingSecret,
            @Value("${app.storage.link-ttl-minutes:30}") long ttlMinutes) {
        this.signingSecret = signingSecret == null ? "change-me-storage-signing-secret" : signingSecret;
        this.ttlMinutes = Math.max(ttlMinutes, 1);
    }

    public String createAssetDownloadUrl(Long fileAssetId) {
        long exp = Instant.now().plusSeconds(ttlMinutes * 60).getEpochSecond();
        String sig = sign("asset", fileAssetId, exp);
        return "/api/v1/files/download/asset/" + fileAssetId + "?exp=" + exp + "&sig=" + sig;
    }

    public String createMaterialDownloadUrl(Long materialId) {
        long exp = Instant.now().plusSeconds(ttlMinutes * 60).getEpochSecond();
        String sig = sign("material", materialId, exp);
        return "/api/v1/files/download/material/" + materialId + "?exp=" + exp + "&sig=" + sig;
    }

    public boolean isValidAssetSignature(Long fileAssetId, long exp, String sig) {
        return isValid("asset", fileAssetId, exp, sig);
    }

    public boolean isValidMaterialSignature(Long materialId, long exp, String sig) {
        return isValid("material", materialId, exp, sig);
    }

    private boolean isValid(String type, Long id, long exp, String sig) {
        if (id == null || sig == null || sig.isBlank()) {
            return false;
        }
        long now = Instant.now().getEpochSecond();
        if (exp < now) {
            return false;
        }
        String expected = sign(type, id, exp);
        return constantTimeEquals(expected, sig);
    }

    private String sign(String type, Long id, long exp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String payload = type + ":" + id + ":" + exp;
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to sign file link", ex);
        }
    }

    private static boolean constantTimeEquals(String left, String right) {
        byte[] a = left.getBytes(StandardCharsets.UTF_8);
        byte[] b = right.getBytes(StandardCharsets.UTF_8);
        if (a.length != b.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}
