package com.weaone.themoa.security.oauth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;
import java.util.Map;

/**
 * 카카오 응답에서 뽑아낸 사용자 ID·닉네임만 담는 최소 principal.
 * 이 시점에는 아직 내부 회원(Member)이 있는지조차 모른다 — 그 판단은 KakaoLoginSuccessHandler가 한다.
 */
public class KakaoOAuth2User implements OAuth2User {

    private final String providerUserId;
    private final String nickname;
    private final Map<String, Object> attributes;

    public KakaoOAuth2User(String providerUserId, String nickname, Map<String, Object> attributes) {
        this.providerUserId = providerUserId;
        this.nickname = nickname;
        this.attributes = attributes;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    public String getNickname() {
        return nickname;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public List<GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getName() {
        return providerUserId;
    }
}
