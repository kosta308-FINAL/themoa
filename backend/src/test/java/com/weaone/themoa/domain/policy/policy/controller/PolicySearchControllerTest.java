package com.weaone.themoa.domain.policy.policy.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.common.exception.GlobalExceptionHandler;
import com.weaone.themoa.domain.policy.policy.dto.response.PolicyDetailResponse;
import com.weaone.themoa.domain.policy.policy.repository.PolicyRepository;
import com.weaone.themoa.domain.policy.policy.service.PolicyDetailService;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchRequest;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchResponse;
import com.weaone.themoa.domain.policy.rag.service.PolicyRagSearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PolicySearchControllerTest {
    private final PolicyRagSearchService searchService = mock(PolicyRagSearchService.class);
    private final PolicyDetailService detailService = mock(PolicyDetailService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new PolicySearchController(searchService, detailService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(new ObjectMapper().findAndRegisterModules()))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Controller는 Repository가 아니라 Service만 의존한다")
    void controllerDoesNotDependOnRepository() {
        boolean hasRepositoryField = List.of(PolicySearchController.class.getDeclaredFields()).stream()
                .anyMatch(field -> PolicyRepository.class.isAssignableFrom(field.getType()));

        assertThat(hasRepositoryField).isFalse();
    }

    @Test
    @DisplayName("Controller는 관리자 패키지에 의존하지 않는다")
    void controllerDoesNotDependOnAdminPackage() {
        boolean hasAdminField = List.of(PolicySearchController.class.getDeclaredFields()).stream()
                .map(field -> field.getType().getPackageName())
                .anyMatch(packageName -> packageName.startsWith("com.weaone.themoa.domain.policy.admin"));

        assertThat(hasAdminField).isFalse();
    }

    @Test
    @DisplayName("검색 정상 응답의 ApiResponse와 JSON 필드 계약을 유지한다")
    void searchReturnsApiResponse() throws Exception {
        given(searchService.search(any(PolicySearchRequest.class)))
                .willReturn(new PolicySearchResponse("answer", null, "RULE", false, "LEXICAL", 1, 0, List.of(), null));

        mockMvc.perform(post("/api/policies/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PolicySearchRequest("청년", null, 0, 10))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.answer").value("answer"))
                .andExpect(jsonPath("$.data.searchMode").value("LEXICAL"));
    }

    @Test
    @DisplayName("정책 검색 준비가 되지 않으면 POLICY_SEARCH_NOT_READY를 반환한다")
    void searchNotReadyReturnsBusinessError() throws Exception {
        given(searchService.search(any(PolicySearchRequest.class)))
                .willThrow(new BusinessException(ErrorCode.POLICY_SEARCH_NOT_READY));

        mockMvc.perform(post("/api/policies/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PolicySearchRequest("청년", null, 0, 10))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("POLICY_SEARCH_NOT_READY"));
    }

    @Test
    @DisplayName("상세 정상 응답의 기존 JSON 필드명을 유지한다")
    void detailReturnsExistingJsonFields() throws Exception {
        given(detailService.detail(10)).willReturn(new PolicyDetailResponse(
                10, "YC-10", "청년 정책", "일자리", "청년기관", "요약", "", "OPEN", List.of("경기도 수원시")));

        mockMvc.perform(get("/api/policies/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.policyId").value(10))
                .andExpect(jsonPath("$.data.sourcePolicyId").value("YC-10"))
                .andExpect(jsonPath("$.data.title").value("청년 정책"))
                .andExpect(jsonPath("$.data.regions[0]").value("경기도 수원시"));
    }

    @Test
    @DisplayName("정책 없음 오류는 404와 POLICY_NOT_FOUND를 유지한다")
    void detailNotFoundReturnsContract() throws Exception {
        given(detailService.detail(404)).willThrow(new BusinessException(ErrorCode.POLICY_NOT_FOUND));

        mockMvc.perform(get("/api/policies/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("POLICY_NOT_FOUND"));
    }

    @Test
    @DisplayName("일반 사용자 정책 Controller는 raw API를 노출하지 않는다")
    void rawEndpointIsNotExposedOnPublicController() {
        boolean exposesRawGet = List.of(PolicySearchController.class.getDeclaredMethods()).stream()
                .filter(method -> method.isAnnotationPresent(GetMapping.class))
                .flatMap(method -> List.of(method.getAnnotation(GetMapping.class).value()).stream())
                .anyMatch(path -> path.contains("raw"));

        assertThat(exposesRawGet).isFalse();
    }
}
