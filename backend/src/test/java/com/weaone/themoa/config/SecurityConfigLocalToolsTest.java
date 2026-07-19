package com.weaone.themoa.config;

import com.weaone.themoa.security.handler.JwtAccessDeniedHandler;
import com.weaone.themoa.security.handler.JwtAuthenticationEntryPoint;
import com.weaone.themoa.security.handler.SecurityErrorResponder;
import com.weaone.themoa.security.jwt.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

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
        SecurityConfigTest.SecurityTestBeans.class
})
class SecurityConfigLocalToolsTest {

    @Autowired
    private MockMvc mockMvc;

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
    @DisplayName("local tools enabled면 인증 없이 정책 원문 API에 접근 가능하다")
    void policyRawIsPublicWhenLocalToolsEnabled() throws Exception {
        mockMvc.perform(get("/api/policies/1/raw"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("raw"));
    }

    @Test
    @DisplayName("local tools enabled면 인증 없이 정책 관리자 API에 접근 가능하다")
    void policyAdminIsPublicWhenLocalToolsEnabled() throws Exception {
        mockMvc.perform(get("/api/policies/admin/status"))
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
