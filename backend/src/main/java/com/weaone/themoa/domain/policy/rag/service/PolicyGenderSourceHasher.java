package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.entity.PolicySearchProjection;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class PolicyGenderSourceHasher {
    public String hash(PolicySearchProjection projection) {
        String source = String.join("\n",
                nullToEmpty(projection.getTitleText()),
                nullToEmpty(projection.getTargetText()),
                nullToEmpty(projection.getQualificationText()),
                nullToEmpty(projection.getApplicationText()),
                nullToEmpty(projection.getDescriptionText()),
                nullToEmpty(projection.getSupportText()),
                nullToEmpty(projection.getInstitutionText()),
                nullToEmpty(projection.getProjectionVersion()));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable.", ex);
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
