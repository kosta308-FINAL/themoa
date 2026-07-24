package com.weaone.themoa.security.oauth;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 카카오 사용자 조회(auth.md §6-1). 토큰 교환·인가 URL 생성은 spring-boot-starter-oauth2-client가
 * 대신하므로, 여기서는 응답에서 카카오 사용자 ID·닉네임만 꺼내 {@link KakaoOAuth2User}로 감싼다.
 * 미검수 앱이라 이메일은 내려오지 않으므로 조회하지 않는다.
 */
@Service
public class KakaoOAuth2UserService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        Object rawId = attributes.get("id");
        if (rawId == null) {
            throw new OAuth2AuthenticationException("카카오 사용자 ID를 확인할 수 없습니다.");
        }
        String providerUserId = String.valueOf(rawId);
        String nickname = extractNickname(attributes);

        return new KakaoOAuth2User(providerUserId, nickname, attributes);
    }

    /**
     * {@code kakao_account.profile.nickname}(별도 동의 필요)을 우선 쓰고, 없으면 앱 동의 여부와
     * 무관하게 내려오는 레거시 필드 {@code properties.nickname}으로 대체한다.
     */
    @SuppressWarnings("unchecked")
    private String extractNickname(Map<String, Object> attributes) {
        Object kakaoAccount = attributes.get("kakao_account");
        if (kakaoAccount instanceof Map<?, ?> account) {
            Object profile = account.get("profile");
            if (profile instanceof Map<?, ?> profileMap) {
                Object nickname = profileMap.get("nickname");
                if (nickname instanceof String value && !value.isBlank()) {
                    return value;
                }
            }
        }
        Object properties = attributes.get("properties");
        if (properties instanceof Map<?, ?> propertiesMap) {
            Object nickname = propertiesMap.get("nickname");
            if (nickname instanceof String value && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
