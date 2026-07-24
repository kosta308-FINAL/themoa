package com.weaone.themoa.domain.policy.recommendation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.weaone.themoa.domain.policy.rag.dto.UserEmploymentStatus;
import com.weaone.themoa.domain.policy.recommendation.dto.response.PolicyRecommendationListResponse;
import com.weaone.themoa.domain.policy.recommendation.dto.response.PolicyRecommendationProfileResponse;
import com.weaone.themoa.domain.policy.recommendation.dto.response.PolicyRecommendationProfileSummaryResponse;
import com.weaone.themoa.domain.policy.recommendation.dto.response.PolicyRecommendationRegionOptionsResponse;
import com.weaone.themoa.domain.policy.recommendation.dto.response.PolicyRecommendationSidoOptionResponse;
import com.weaone.themoa.domain.policy.recommendation.service.PolicyRecommendationProfileService;
import com.weaone.themoa.domain.policy.recommendation.service.PolicyRecommendationRegionService;
import com.weaone.themoa.domain.policy.recommendation.service.PolicyRecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PolicyRecommendationControllerTest {

    @Test
    void profileReturnsConfiguredFalse() throws Exception {
        PolicyRecommendationProfileService profileService = mock(PolicyRecommendationProfileService.class);
        PolicyRecommendationService recommendationService = mock(PolicyRecommendationService.class);
        PolicyRecommendationRegionService regionService = mock(PolicyRecommendationRegionService.class);
        given(profileService.get(7L)).willReturn(profile(false));

        mockMvc(profileService, recommendationService, regionService)
                .perform(get("/api/policy-recommendations/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.configured").value(false))
                .andExpect(jsonPath("$.data.age").value(27));
    }

    @Test
    void createProfileReturnsCreatedAndRefreshesRecommendations() throws Exception {
        PolicyRecommendationProfileService profileService = mock(PolicyRecommendationProfileService.class);
        PolicyRecommendationService recommendationService = mock(PolicyRecommendationService.class);
        PolicyRecommendationRegionService regionService = mock(PolicyRecommendationRegionService.class);
        given(profileService.create(any(), any())).willReturn(profile(true));
        given(recommendationService.refreshForMember(7L)).willReturn(recommendations(true));

        mockMvc(profileService, recommendationService, regionService)
                .perform(post("/api/policy-recommendations/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "residenceSido": "경기도",
                                  "residenceSigungu": "수원시",
                                  "employmentStatus": "UNEMPLOYED"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.profile.configured").value(true))
                .andExpect(jsonPath("$.data.recommendationRefreshed").value(true))
                .andExpect(jsonPath("$.data.recommendations.configured").value(true));

        verify(recommendationService).refreshForMember(7L);
    }

    @Test
    void createProfileReturnsCreatedWhenRecommendationRefreshFails() throws Exception {
        PolicyRecommendationProfileService profileService = mock(PolicyRecommendationProfileService.class);
        PolicyRecommendationService recommendationService = mock(PolicyRecommendationService.class);
        PolicyRecommendationRegionService regionService = mock(PolicyRecommendationRegionService.class);
        given(profileService.create(any(), any())).willReturn(profile(true));
        given(recommendationService.refreshForMember(7L)).willThrow(new IllegalStateException("failed"));

        mockMvc(profileService, recommendationService, regionService)
                .perform(post("/api/policy-recommendations/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "residenceSido": "경기도",
                                  "residenceSigungu": "수원시",
                                  "employmentStatus": "UNEMPLOYED"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.profile.configured").value(true))
                .andExpect(jsonPath("$.data.recommendationRefreshed").value(false))
                .andExpect(jsonPath("$.data.recommendationMessage")
                        .value("기본정보는 저장했지만 추천 정책을 계산하지 못했어요."));
    }

    @Test
    void updateProfileReturnsOk() throws Exception {
        PolicyRecommendationProfileService profileService = mock(PolicyRecommendationProfileService.class);
        PolicyRecommendationService recommendationService = mock(PolicyRecommendationService.class);
        PolicyRecommendationRegionService regionService = mock(PolicyRecommendationRegionService.class);
        given(profileService.update(any(), any())).willReturn(profile(true));
        given(recommendationService.refreshForMember(7L)).willReturn(recommendations(true));

        mockMvc(profileService, recommendationService, regionService)
                .perform(patch("/api/policy-recommendations/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "residenceSido": "경기도",
                                  "residenceSigungu": "수원시",
                                  "employmentStatus": "UNEMPLOYED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile.configured").value(true))
                .andExpect(jsonPath("$.data.recommendationRefreshed").value(true));
    }

    @Test
    void updateProfileReturnsOkWhenRecommendationRefreshFails() throws Exception {
        PolicyRecommendationProfileService profileService = mock(PolicyRecommendationProfileService.class);
        PolicyRecommendationService recommendationService = mock(PolicyRecommendationService.class);
        PolicyRecommendationRegionService regionService = mock(PolicyRecommendationRegionService.class);
        given(profileService.update(any(), any())).willReturn(profile(true));
        given(recommendationService.refreshForMember(7L)).willThrow(new IllegalStateException("failed"));

        mockMvc(profileService, recommendationService, regionService)
                .perform(patch("/api/policy-recommendations/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "residenceSido": "경기도",
                                  "residenceSigungu": "수원시",
                                  "employmentStatus": "UNEMPLOYED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile.configured").value(true))
                .andExpect(jsonPath("$.data.recommendationRefreshed").value(false));
    }

    @Test
    void recommendationsReturnsApiResponse() throws Exception {
        PolicyRecommendationProfileService profileService = mock(PolicyRecommendationProfileService.class);
        PolicyRecommendationService recommendationService = mock(PolicyRecommendationService.class);
        PolicyRecommendationRegionService regionService = mock(PolicyRecommendationRegionService.class);
        given(recommendationService.list(7L)).willReturn(new PolicyRecommendationListResponse(false, null, null, List.of()));

        mockMvc(profileService, recommendationService, regionService)
                .perform(get("/api/policy-recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configured").value(false))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void refreshReturnsApiResponse() throws Exception {
        PolicyRecommendationProfileService profileService = mock(PolicyRecommendationProfileService.class);
        PolicyRecommendationService recommendationService = mock(PolicyRecommendationService.class);
        PolicyRecommendationRegionService regionService = mock(PolicyRecommendationRegionService.class);
        given(recommendationService.refreshForMember(7L))
                .willReturn(new PolicyRecommendationListResponse(true, LocalDateTime.now(), null, List.of()));

        mockMvc(profileService, recommendationService, regionService)
                .perform(post("/api/policy-recommendations/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configured").value(true));
    }

    @Test
    void regionsReturnsOptions() throws Exception {
        PolicyRecommendationProfileService profileService = mock(PolicyRecommendationProfileService.class);
        PolicyRecommendationService recommendationService = mock(PolicyRecommendationService.class);
        PolicyRecommendationRegionService regionService = mock(PolicyRecommendationRegionService.class);
        given(regionService.options()).willReturn(new PolicyRecommendationRegionOptionsResponse(
                List.of(new PolicyRecommendationSidoOptionResponse("경기도", List.of("수원시")))
        ));

        mockMvc(profileService, recommendationService, regionService)
                .perform(get("/api/policy-recommendations/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].sido").value("경기도"));
    }

    @Test
    void controllerDependsOnlyOnServices() {
        boolean hasRepositoryField = List.of(PolicyRecommendationController.class.getDeclaredFields()).stream()
                .anyMatch(field -> field.getType().getSimpleName().endsWith("Repository"));

        assertThat(hasRepositoryField).isFalse();
    }

    private MockMvc mockMvc(PolicyRecommendationProfileService profileService,
                            PolicyRecommendationService recommendationService,
                            PolicyRecommendationRegionService regionService) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return MockMvcBuilders
                .standaloneSetup(new PolicyRecommendationController(profileService, recommendationService, regionService))
                .setCustomArgumentResolvers(new TestAuthenticationPrincipalResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    private PolicyRecommendationProfileResponse profile(boolean configured) {
        return new PolicyRecommendationProfileResponse(
                configured,
                LocalDate.of(1999, 3, 12),
                27,
                configured ? "경기도" : null,
                configured ? "수원시" : null,
                configured ? UserEmploymentStatus.UNEMPLOYED : null,
                configured ? LocalDateTime.of(2026, 7, 24, 10, 0) : null
        );
    }

    private PolicyRecommendationListResponse recommendations(boolean configured) {
        return new PolicyRecommendationListResponse(
                configured,
                configured ? LocalDateTime.of(2026, 7, 24, 10, 1) : null,
                configured ? profileSummary() : null,
                List.of()
        );
    }

    private PolicyRecommendationProfileSummaryResponse profileSummary() {
        return new PolicyRecommendationProfileSummaryResponse(
                LocalDate.of(1999, 3, 12),
                27,
                "경기도",
                "수원시",
                UserEmploymentStatus.UNEMPLOYED
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
