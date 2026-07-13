package com.weaone.themoa.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * 인증 관련 설정값. 서명키·메일 비밀번호 같은 시크릿은 소스에 두지 않고 환경변수로 주입한다.
 */
@Validated
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull Refresh refresh,
        @Valid @NotNull EmailVerification emailVerification
) {

    /**
     * @param secret            HMAC-SHA256 서명키의 Base64 인코딩 값. 최소 32바이트(256비트)여야 한다.
     * @param accessTokenValidity Access Token 유효기간(30분).
     */
    public record Jwt(
            @NotBlank String secret,
            @NotNull Duration accessTokenValidity
    ) {
    }

    /**
     * @param validity   Refresh Token 유효기간. 슬라이딩이라 재발급마다 이 값만큼 다시 연장된다(5일).
     * @param cookieName Refresh Token을 담는 HttpOnly 쿠키 이름.
     * @param cookiePath 쿠키 전송 경로. 재발급·로그아웃 요청에만 실려 나가도록 좁힌다.
     * @param cookieSecure HTTPS 전용 여부. 배포는 반드시 true, 로컬 http 개발에서만 false.
     */
    public record Refresh(
            @NotNull Duration validity,
            @NotBlank String cookieName,
            @NotBlank String cookiePath,
            boolean cookieSecure
    ) {
    }

    /**
     * @param codeTtl           인증 코드 유효기간(5분).
     * @param resendCooldown    재발송 쿨다운(60초).
     * @param maxVerifyAttempts 코드 오입력 허용 횟수. 도달하면 코드를 폐기하고 재발송을 요구한다.
     * @param verifiedTtl       인증 통과 상태를 유지하는 기간. 이 안에 회원가입을 마쳐야 한다.
     * @param from              발신 메일 주소.
     */
    public record EmailVerification(
            @NotNull Duration codeTtl,
            @NotNull Duration resendCooldown,
            int maxVerifyAttempts,
            @NotNull Duration verifiedTtl,
            @NotBlank String from
    ) {
    }
}