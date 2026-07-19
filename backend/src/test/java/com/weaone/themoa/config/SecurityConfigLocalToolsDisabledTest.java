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
import testsupport.SecurityTestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityTestController.class)
@ActiveProfiles("local")
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
class SecurityConfigLocalToolsDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("local profile이어도 local tools disabled면 정책 검색 API는 401이다")
    void policySearchRequiresAuthenticationWhenLocalToolsDisabled() throws Exception {
        mockMvc.perform(post("/api/policies/search"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_ACCESS_TOKEN"));
    }

    @Test
    @DisplayName("local profile이어도 local tools disabled면 정책 상세 API는 401이다")
    void policyDetailRequiresAuthenticationWhenLocalToolsDisabled() throws Exception {
        mockMvc.perform(get("/api/policies/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_ACCESS_TOKEN"));
    }

    @Test
    @DisplayName("local profile이어도 local tools disabled면 정책 관리자 API는 401이다")
    void policyAdminRequiresAuthenticationWhenLocalToolsDisabled() throws Exception {
        mockMvc.perform(get("/api/policies/admin/status"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_ACCESS_TOKEN"));
    }

    @Test
    @DisplayName("local profile이어도 local tools disabled면 비정책 보호 API는 401이다")
    void otherProtectedApiStillRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_ACCESS_TOKEN"));
    }

    @Test
    @DisplayName("local tools disabled여도 기존 Auth 공개 API는 정상 접근 가능하다")
    void publicAuthApiIsStillPublic() throws Exception {
        mockMvc.perform(post("/api/auth/login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("login"));
    }
}
