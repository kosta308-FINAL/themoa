package com.weaone.themoa.config;

import io.codef.api.EasyCodefServiceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * CODEF 연동 설정값. clientId/secret/publicKey는 우리 서비스↔CODEF 인증용이며
 * 카드사 로그인 자격증명(카드사 아이디/비밀번호)과는 별개다. 소스에 두지 않고 환경변수로 주입한다.
 *
 * @param callTimeout EasyCodef SDK가 자체 커넥션/응답 타임아웃을 노출하지 않아, 블로킹 호출을 별도 스레드에서 실행해
 *                    이 값으로 우리가 직접 데드라인을 강제한다.
 */
@Validated
@ConfigurationProperties(prefix = "app.codef")
public record CodefProperties(
        @NotBlank String clientId,
        @NotBlank String clientSecret,
        @NotBlank String publicKey,
        @NotNull EasyCodefServiceType serviceType,
        @NotNull Duration callTimeout
) {
}
