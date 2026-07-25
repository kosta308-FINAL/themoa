package com.weaone.themoa.security.oauth;

import com.weaone.themoa.domain.auth.entity.SocialProvider;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 카카오·구글 사용자 조회(auth.md §6-1). 토큰 교환·인가 URL 생성은 spring-boot-starter-oauth2-client가
 * 대신하므로, 여기서는 응답에서 provider별 사용자 ID·닉네임만 꺼내 {@link SocialOAuth2User}로 감싼다.
 * 구글 registration은 openid 스코프를 요청하지 않으므로(application.yaml) OIDC가 아니라 이 순수
 * OAuth2 흐름을 카카오와 동일하게 탄다.
 */
@Service
public class SocialOAuth2UserService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();
        SocialProvider provider = resolveProvider(userRequest.getClientRegistration().getRegistrationId());

        return switch (provider) {
            case KAKAO -> loadKakaoUser(attributes);
            case GOOGLE -> loadGoogleUser(attributes);
        };
    }

    private SocialProvider resolveProvider(String registrationId) {
        try {
            return SocialProvider.valueOf(registrationId.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인입니다: " + registrationId);
        }
    }

    private SocialOAuth2User loadKakaoUser(Map<String, Object> attributes) {
        Object rawId = attributes.get("id");
        if (rawId == null) {
            throw new OAuth2AuthenticationException("카카오 사용자 ID를 확인할 수 없습니다.");
        }
        return new SocialOAuth2User(SocialProvider.KAKAO, String.valueOf(rawId),
                extractKakaoNickname(attributes), null, attributes);
    }

    /**
     * {@code kakao_account.profile.nickname}(별도 동의 필요)을 우선 쓰고, 없으면 앱 동의 여부와
     * 무관하게 내려오는 레거시 필드 {@code properties.nickname}으로 대체한다.
     */
    @SuppressWarnings("unchecked")
    private String extractKakaoNickname(Map<String, Object> attributes) {
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

    private SocialOAuth2User loadGoogleUser(Map<String, Object> attributes) {
        Object rawId = attributes.get("sub");
        if (rawId == null) {
            throw new OAuth2AuthenticationException("구글 사용자 ID를 확인할 수 없습니다.");
        }
        Object rawName = attributes.get("name");
        String nickname = rawName instanceof String value && !value.isBlank() ? value : null;
        return new SocialOAuth2User(SocialProvider.GOOGLE, String.valueOf(rawId), nickname,
                extractVerifiedGoogleEmail(attributes), attributes);
    }

    /**
     * 구글이 이미 검증한 이메일만 신뢰한다({@code email_verified}). 앱 자체 인증 코드 발송·확인을
     * 건너뛰고 바로 회원 이메일로 쓰기 위한 값이라(SocialAuthService), 검증되지 않았으면 null을
     * 돌려줘 카카오와 동일하게 사용자가 직접 입력·인증하게 만든다.
     */
    private String extractVerifiedGoogleEmail(Map<String, Object> attributes) {
        Object rawEmail = attributes.get("email");
        Object rawVerified = attributes.get("email_verified");
        boolean verified = Boolean.TRUE.equals(rawVerified) || "true".equals(String.valueOf(rawVerified));
        if (rawEmail instanceof String value && !value.isBlank() && verified) {
            return value;
        }
        return null;
    }
}
