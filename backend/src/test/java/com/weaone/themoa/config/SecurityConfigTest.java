package com.weaone.themoa.config;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.security.handler.JwtAccessDeniedHandler;
import com.weaone.themoa.security.handler.JwtAuthenticationEntryPoint;
import com.weaone.themoa.security.handler.SecurityErrorResponder;
import com.weaone.themoa.security.jwt.AccessTokenClaims;
import com.weaone.themoa.security.jwt.JwtAuthenticationFilter;
import com.weaone.themoa.security.jwt.JwtTokenProvider;
import com.weaone.themoa.security.jwt.TokenVersionCache;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import testsupport.SecurityTestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

@WebMvcTest(controllers = SecurityTestController.class)
@ActiveProfiles("test")
@TestPropertySource(properties = "app.policy.local-tools.enabled=false")
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class,
        SecurityErrorResponder.class,
        SecurityConfigTest.SecurityTestBeans.class,
        SecurityTestController.class
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private TokenVersionCache tokenVersionCache;
    @Autowired
    private JwtAccessDeniedHandler accessDeniedHandler;

    @Test
    @DisplayName("local tools disabled면 정책 검색 API는 인증 없이 접근할 수 없다")
    void policySearchRequiresAuthenticationWhenLocalToolsDisabled() throws Exception {
        mockMvc.perform(post("/api/policies/search"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_ACCESS_TOKEN"));
    }

    @Test
    @DisplayName("non-local profile에서는 정책 상세 API가 인증 없이 접근할 수 없다")
    void policyDetailRequiresAuthenticationInNonLocalProfile() throws Exception {
        mockMvc.perform(get("/api/policies/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_ACCESS_TOKEN"));
    }

    @Test
    @DisplayName("non-local profile에서는 기존 Auth 공개 API가 정상 접근 가능하다")
    void publicAuthApiIsStillPublicInNonLocalProfile() throws Exception {
        mockMvc.perform(post("/api/auth/login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("login"));
    }

    @Test
    @DisplayName("잘못된 토큰이 비정책 API에 들어오면 기존 오류 계약으로 401을 반환하고 토큰 원문을 노출하지 않는다")
    void invalidTokenOnProtectedApiReturnsStableUnauthorizedContract() throws Exception {
        reset(jwtTokenProvider, tokenVersionCache);
        given(jwtTokenProvider.parse("expired-token"))
                .willThrow(new BusinessException(ErrorCode.AUTH_INVALID_ACCESS_TOKEN));

        mockMvc.perform(get("/api/protected").header("Authorization", "Bearer expired-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_ACCESS_TOKEN"))
                .andExpect(content().string(not(containsString("expired-token"))));
    }

    @Test
    @DisplayName("유효 JWT는 Security Chain의 JwtAuthenticationFilter를 거쳐 인증 사용자 정보를 저장한다")
    void validTokenAuthenticatesThroughSecurityFilterChain() throws Exception {
        reset(jwtTokenProvider, tokenVersionCache);
        given(jwtTokenProvider.parse("valid-token")).willReturn(new AccessTokenClaims(7L, 3));
        given(tokenVersionCache.find(7L)).willReturn(Optional.of(3));

        mockMvc.perform(get("/api/protected").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("7"));
    }

    @Test
    @DisplayName("권한 부족 응답은 Security 전용 처리기에서 403과 안정적인 ErrorCode를 반환한다")
    void accessDeniedHandlerReturnsStableForbiddenContract() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(
                new MockHttpServletRequest(),
                response,
                new AccessDeniedException("denied-secret-token")
        );

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
        assertThat(response.getContentAsString()).contains("\"code\":\"ACCESS_DENIED\"");
        assertThat(response.getContentAsString()).doesNotContain("denied-secret-token");
    }

    @TestConfiguration
    static class SecurityTestBeans {

        @Bean
        JwtTokenProvider jwtTokenProvider() {
            return mock(JwtTokenProvider.class);
        }

        @Bean
        TokenVersionCache tokenVersionCache() {
            return mock(TokenVersionCache.class);
        }
    }

}
