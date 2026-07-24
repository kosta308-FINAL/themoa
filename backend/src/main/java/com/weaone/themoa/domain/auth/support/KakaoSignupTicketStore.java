package com.weaone.themoa.domain.auth.support;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.weaone.themoa.config.AuthProperties;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 카카오 신규 회원의 신원(카카오 사용자 ID·닉네임)을, 이메일 인증·성별·출생일 제출(가입 완료) 때까지
 * 짧은 수명으로 들고 있는다(auth.md §6-2). 정식 Access/Refresh Token이 아니고 회원 생성 외 용도로
 * 쓰지 않으며, 콜백 URL의 query parameter에도 싣지 않는다 — 교환코드(OAuthLoginExchangeStore)를
 * 거쳐 응답 본문으로만 프론트에 전달된다.
 */
@Component
public class KakaoSignupTicketStore {

    private final Cache<String, Ticket> tickets;
    private final RefreshTokenGenerator tokenGenerator;

    public KakaoSignupTicketStore(AuthProperties properties, RefreshTokenGenerator tokenGenerator) {
        this.tickets = Caffeine.newBuilder()
                .expireAfterWrite(properties.oauth().signupTicketTtl())
                .build();
        this.tokenGenerator = tokenGenerator;
    }

    public String issue(String providerUserId, String nickname) {
        String token = tokenGenerator.generate();
        tickets.put(token, new Ticket(providerUserId, nickname));
        return token;
    }

    /** 가입 완료(회원 생성) 1회에만 쓰이고 소비된다. */
    public Optional<Ticket> consume(String token) {
        Ticket ticket = tickets.getIfPresent(token);
        if (ticket != null) {
            tickets.invalidate(token);
        }
        return Optional.ofNullable(ticket);
    }

    public record Ticket(String providerUserId, String nickname) {
    }
}
