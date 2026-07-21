package com.weaone.themoa.domain.policy.bookmark.controller;

import com.weaone.themoa.domain.policy.bookmark.dto.response.PolicyBookmarkListResponse;
import com.weaone.themoa.domain.policy.bookmark.dto.response.PolicyBookmarkResponse;
import com.weaone.themoa.domain.policy.bookmark.service.PolicyBookmarkService;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PolicyBookmarkControllerTest {

    @Test
    void listReturnsApiResponse() throws Exception {
        PolicyBookmarkService service = mock(PolicyBookmarkService.class);
        given(service.list(7L)).willReturn(new PolicyBookmarkListResponse(List.of(response())));
        MockMvc mockMvc = mockMvc(service);

        mockMvc.perform(get("/api/policies/bookmarks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[0].policyId").value(12));
    }

    @Test
    void addReturnsBookmark() throws Exception {
        PolicyBookmarkService service = mock(PolicyBookmarkService.class);
        given(service.add(7L, 12)).willReturn(response());
        MockMvc mockMvc = mockMvc(service);

        mockMvc.perform(post("/api/policies/bookmarks/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.policyId").value(12))
                .andExpect(jsonPath("$.data.applyStatus").value("INTERESTED"))
                .andExpect(jsonPath("$.data.notificationEnabled").value(false));
    }

    @Test
    void removeReturnsNoContent() throws Exception {
        PolicyBookmarkService service = mock(PolicyBookmarkService.class);
        MockMvc mockMvc = mockMvc(service);

        mockMvc.perform(delete("/api/policies/bookmarks/12"))
                .andExpect(status().isNoContent());

        verify(service).remove(7L, 12);
    }

    @Test
    void controllerDependsOnlyOnService() {
        boolean hasRepositoryField = List.of(PolicyBookmarkController.class.getDeclaredFields()).stream()
                .anyMatch(field -> field.getType().getSimpleName().endsWith("Repository"));

        assertThat(hasRepositoryField).isFalse();
    }

    private MockMvc mockMvc(PolicyBookmarkService service) {
        return MockMvcBuilders
                .standaloneSetup(new PolicyBookmarkController(service))
                .setCustomArgumentResolvers(new TestAuthenticationPrincipalResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    private PolicyBookmarkResponse response() {
        return new PolicyBookmarkResponse(
                3,
                12,
                "경기도 청년 취업지원금",
                "일자리",
                "경기도",
                "미취업 청년을 위한 지원 정책",
                "https://example.com",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                false,
                true,
                "신청중",
                "INTERESTED",
                false,
                null
        );
    }

    private static class TestAuthenticationPrincipalResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                    && parameter.getParameterType().equals(Long.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter,
                                      ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest,
                                      WebDataBinderFactory binderFactory) {
            return 7L;
        }
    }
}
