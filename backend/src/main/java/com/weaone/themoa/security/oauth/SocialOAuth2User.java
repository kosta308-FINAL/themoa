package com.weaone.themoa.security.oauth;

import com.weaone.themoa.domain.auth.entity.SocialProvider;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;
import java.util.Map;

/**
 * 소셜 provider 응답에서 뽑아낸 provider·사용자 ID·닉네임만 담는 최소 principal.
 * 이 시점에는 아직 내부 회원(Member)이 있는지조차 모른다 — 그 판단은 SocialLoginSuccessHandler가 한다.
 */
public class SocialOAuth2User implements OAuth2User {

    private final SocialProvider provider;
    private final String providerUserId;
    private final String nickname;
    private final String email;
    private final Map<String, Object> attributes;

    public SocialOAuth2User(SocialProvider provider, String providerUserId, String nickname, String email,
                             Map<String, Object> attributes) {
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.nickname = nickname;
        this.email = email;
        this.attributes = attributes;
    }

    public SocialProvider getProvider() {
        return provider;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    public String getNickname() {
        return nickname;
    }

    /** 카카오는 항상 null(미검수 앱이라 이메일이 내려오지 않음). 구글은 검증된 이메일일 때만 채워진다. */
    public String getEmail() {
        return email;
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
