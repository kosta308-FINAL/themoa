package com.weaone.themoa.config;

import com.weaone.themoa.domain.customerservice.support.StorageProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** 고객센터(FAQ·1:1 문의) 설정값(customerservice.md §6-2). */
@Validated
@ConfigurationProperties(prefix = "app.customer-service")
public record CustomerServiceProperties(
        @NotBlank String privacyPolicyVersion,
        @Valid @NotNull Storage storage
) {

    /**
     * @param provider  LOCAL 또는 S3. 한 실행 환경에서 섞어 쓰지 않는다.
     * @param localRoot LOCAL 프로필의 저장 루트 디렉터리.
     * @param s3Bucket  S3 프로필의 비공개 버킷명.
     * @param s3Region  S3 프로필의 리전.
     */
    public record Storage(
            @NotNull StorageProvider provider,
            @NotBlank String localRoot,
            String s3Bucket,
            String s3Region
    ) {
    }
}
