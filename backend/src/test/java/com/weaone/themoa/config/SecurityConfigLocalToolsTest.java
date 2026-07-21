package com.weaone.themoa.config;

import com.weaone.themoa.security.handler.JwtAccessDeniedHandler;
import com.weaone.themoa.security.handler.JwtAuthenticationEntryPoint;
import com.weaone.themoa.security.handler.SecurityErrorResponder;
import com.weaone.themoa.security.jwt.AccessTokenClaims;
import com.weaone.themoa.security.jwt.JwtAuthenticationFilter;
import com.weaone.themoa.security.jwt.JwtTokenProvider;
import com.weaone.themoa.security.jwt.TokenVersionCache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import testsupport.SecurityTestController;

import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityTestController.class)
@ActiveProfiles("local")
@TestPropertySource(properties = "app.policy.local-tools.enabled=true")
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class,
        SecurityErrorResponder.class,
        SecurityConfigTest.SecurityTestBeans.class,
        SecurityTestController.class
})
class SecurityConfigLocalToolsTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private TokenVersionCache tokenVersionCache;

    @Test
    @DisplayName("local tools enabled면 인증 없이 정책 검색 API에 접근 가능하다")
    void policySearchIsPublicWhenLocalToolsEnabled() throws Exception {
        mockMvc.perform(post("/api/policies/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("search"));
    }

    @Test
    @DisplayName("local tools enabled면 인증 없이 정책 상세 API에 접근 가능하다")
    void policyDetailIsPublicWhenLocalToolsEnabled() throws Exception {
        mockMvc.perform(get("/api/policies/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("detail"));
    }

    @Test
    @DisplayName("local tools enabled여도 일반 사용자 정책 원문 API는 공개하지 않는다")
    void policyRawIsNotPublicWhenLocalToolsEnabled() throws Exception {
        mockMvc.perform(get("/api/policies/1/raw"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_ACCESS_TOKEN"));
    }

    @Test
    @DisplayName("local tools enabled여도 정책 관리자 API는 인증을 요구한다")
    void policyAdminRequiresAuthenticationWhenLocalToolsEnabled() throws Exception {
        mockMvc.perform(get("/api/policies/admin/status"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_ACCESS_TOKEN"));
    }

    @Test
    @DisplayName("local tools enabled여도 USER는 정책 관리자 API에 접근할 수 없다")
    void policyAdminRejectsUserWhenLocalToolsEnabled() throws Exception {
        reset(jwtTokenProvider, tokenVersionCache);
        given(jwtTokenProvider.parse("user-token"))
                .willReturn(new AccessTokenClaims(7L, 3, "USER"));
        given(tokenVersionCache.find(7L)).willReturn(Optional.of(3));

        mockMvc.perform(get("/api/policies/admin/status").header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("local tools enabled에서 ADMIN은 정책 관리자 API에 접근할 수 있다")
    void policyAdminAllowsAdminWhenLocalToolsEnabled() throws Exception {
        reset(jwtTokenProvider, tokenVersionCache);
        given(jwtTokenProvider.parse("admin-token"))
                .willReturn(new AccessTokenClaims(9L, 5, "ADMIN"));
        given(tokenVersionCache.find(9L)).willReturn(Optional.of(5));

        mockMvc.perform(get("/api/policies/admin/status").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("admin"));
    }

    @Test
    @DisplayName("local tools enabled여도 향후 회원 정책 경로는 자동 공개하지 않는다")
    void futurePolicyMemberPathsAreNotPublic() throws Exception {
        mockMvc.perform(get("/api/policies/bookmarks/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_ACCESS_TOKEN"));
    }

    @Test
    @DisplayName("local tools enabled여도 비정책 API는 인증을 요구한다")
    void otherApiStillRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_ACCESS_TOKEN"));
    }
}
