package com.weaone.themoa.domain.policy.admin.controller;

import com.weaone.themoa.common.exception.GlobalExceptionHandler;
import com.weaone.themoa.common.logging.ErrorLogSanitizer;
import com.weaone.themoa.domain.logging.service.AsyncErrorLogRecorder;
import com.weaone.themoa.domain.policy.admin.dto.AdminJobStatus;
import com.weaone.themoa.domain.policy.admin.dto.AdminStatusResponse;
import com.weaone.themoa.domain.policy.admin.dto.dashboard.AdminDashboardResponse;
import com.weaone.themoa.domain.policy.admin.dto.embedding.AdminEmbeddingPageResponse;
import com.weaone.themoa.domain.policy.admin.dto.response.AdminPolicyRawDataResponse;
import com.weaone.themoa.domain.policy.admin.dto.response.AdminPolicyRawResponse;
import com.weaone.themoa.domain.policy.admin.dto.response.AdminSearchIndexSummaryResponse;
import com.weaone.themoa.domain.policy.admin.service.AdminDashboardFacade;
import com.weaone.themoa.domain.policy.admin.service.AdminEmbeddingReadService;
import com.weaone.themoa.domain.policy.admin.service.AdminJobService;
import com.weaone.themoa.domain.policy.admin.service.AdminPolicyRawService;
import com.weaone.themoa.domain.policy.admin.service.AdminRegionDiagnosticsService;
import com.weaone.themoa.domain.policy.admin.service.AdminRegionQueryService;
import com.weaone.themoa.domain.policy.admin.service.AdminSearchDiagnosticService;
import com.weaone.themoa.domain.policy.admin.service.AdminSearchIndexService;
import com.weaone.themoa.domain.policy.admin.service.AdminStatusService;
import com.weaone.themoa.domain.policy.rag.dto.SearchReadinessResponse;
import com.weaone.themoa.domain.policy.youthcenter.service.YouthCenterDiagnosticService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PolicyAdminControllerTest {

    @Test
    @DisplayName("관리자 Dashboard endpoint URL과 JSON 필드를 유지한다")
    void dashboardEndpointKeepsContract() throws Exception {
        AdminStatusService statusService = mock(AdminStatusService.class);
        AdminDashboardFacade dashboardFacade = mock(AdminDashboardFacade.class);
        AdminDashboardResponse response = new AdminDashboardResponse(
                statusResponse(),
                new AdminSearchIndexSummaryResponse(true, 2, "v1", 2, 0, null, null, List.of(), false),
                new SearchReadinessResponse(true, 2, 2, 2, 2, List.of()),
                null
        );
        given(dashboardFacade.dashboard()).willReturn(response);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new PolicyAdminDashboardController(statusService, dashboardFacade))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        mockMvc.perform(get("/api/policies/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.searchIndex.ready").value(true))
                .andExpect(jsonPath("$.data.searchIndex.documentCount").value(2));
    }

    @Test
    @DisplayName("관리자 Job 실행 endpoint URL과 202 Accepted를 유지한다")
    void jobEndpointKeepsContract() throws Exception {
        AdminJobService jobService = mock(AdminJobService.class);
        AdminJobStatus job = new AdminJobStatus("job-1", "POLICY_COLLECTION", "RUNNING",
                0, 0, 0, 0, 0, 0, 0, "");
        given(jobService.start("POLICY_COLLECTION")).willReturn(job);
        given(jobService.latest()).willReturn(Optional.of(job));
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new PolicyAdminJobController(jobService))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        mockMvc.perform(post("/api/policies/admin/jobs/policy-collection"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.jobId").value("job-1"))
                .andExpect(jsonPath("$.data.jobType").value("POLICY_COLLECTION"));
        mockMvc.perform(get("/api/policies/admin/jobs/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RUNNING"));
    }

    @Test
    @DisplayName("관리자 정책 동기화 Job endpoint는 POLICY_SYNC로 202 Accepted를 반환한다")
    void policySyncJobEndpointKeepsContract() throws Exception {
        AdminJobService jobService = mock(AdminJobService.class);
        AdminJobStatus job = new AdminJobStatus("job-sync-1", "POLICY_SYNC", "RUNNING",
                0, 0, 0, 0, 0, 0, 0, "");
        given(jobService.start("POLICY_SYNC")).willReturn(job);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new PolicyAdminJobController(jobService))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        mockMvc.perform(post("/api/policies/admin/jobs/policy-sync"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobId").value("job-sync-1"))
                .andExpect(jsonPath("$.data.jobType").value("POLICY_SYNC"));
        verify(jobService).start("POLICY_SYNC");
    }

    @Test
    @DisplayName("존재하지 않는 관리자 Job 조회는 POLICY_JOB_NOT_FOUND를 반환한다")
    void missingJobReturnsPolicyJobNotFound() throws Exception {
        AdminJobService jobService = mock(AdminJobService.class);
        given(jobService.find("missing")).willReturn(Optional.empty());
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new PolicyAdminJobController(jobService))
                .setControllerAdvice(new GlobalExceptionHandler(new ErrorLogSanitizer(), mock(AsyncErrorLogRecorder.class)))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        mockMvc.perform(get("/api/policies/admin/jobs/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("POLICY_JOB_NOT_FOUND"));
    }

    @Test
    @DisplayName("관리자 Controller는 Repository를 직접 의존하지 않는다")
    void adminControllersDoNotDependOnRepositories() {
        List<Class<?>> controllers = List.of(
                PolicyAdminDashboardController.class,
                PolicyAdminJobController.class,
                PolicyAdminEmbeddingController.class,
                PolicyAdminSearchController.class,
                PolicyAdminRegionController.class,
                PolicyAdminRawController.class
        );

        for (Class<?> controller : controllers) {
            boolean hasRepositoryField = List.of(controller.getDeclaredFields()).stream()
                    .anyMatch(field -> field.getType().getSimpleName().endsWith("Repository"));

            assertThat(hasRepositoryField).as(controller.getSimpleName()).isFalse();
        }
    }

    @Test
    @DisplayName("관리자 Embedding/Region/Search Controller는 Service 계층만 Mock해 endpoint를 구성한다")
    void splitControllersUseServices() throws Exception {
        AdminEmbeddingReadService embeddingReadService = mock(AdminEmbeddingReadService.class);
        YouthCenterDiagnosticService youthCenterDiagnosticService = mock(YouthCenterDiagnosticService.class);
        AdminRegionDiagnosticsService regionDiagnosticsService = mock(AdminRegionDiagnosticsService.class);
        AdminRegionQueryService regionQueryService = mock(AdminRegionQueryService.class);
        AdminSearchIndexService searchIndexService = mock(AdminSearchIndexService.class);
        AdminSearchDiagnosticService searchDiagnosticService = mock(AdminSearchDiagnosticService.class);
        AdminPolicyRawService rawService = mock(AdminPolicyRawService.class);
        given(embeddingReadService.search(null, null, 0, 20))
                .willReturn(new AdminEmbeddingPageResponse(List.of(), 0, 20, 0, 0, false));
        given(regionDiagnosticsService.anomalies()).willReturn(List.of());
        given(rawService.raw(10)).willReturn(new AdminPolicyRawResponse(
                10,
                "YC-10",
                "YOUTH_CENTER",
                "{\"id\":\"YC-10\"}",
                new AdminPolicyRawDataResponse(1L, "https://api.test", "JSON", "2026-07-20T00:00")
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new PolicyAdminEmbeddingController(embeddingReadService, youthCenterDiagnosticService),
                new PolicyAdminRegionController(regionDiagnosticsService, regionQueryService),
                new PolicyAdminSearchController(searchIndexService, searchDiagnosticService),
                new PolicyAdminRawController(rawService)
        ).setMessageConverters(new MappingJackson2HttpMessageConverter()).build();

        mockMvc.perform(get("/api/policies/admin/embeddings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray());
        mockMvc.perform(get("/api/policies/admin/regions/anomalies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
        mockMvc.perform(get("/api/policies/admin/10/raw"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.policyId").value(10))
                .andExpect(jsonPath("$.data.pageRawData.rawDataId").value(1));
    }

    @Test
    @DisplayName("동작하지 않는 관리자 Qdrant 검색 API는 노출하지 않는다")
    void qdrantSearchActionEndpointIsNotExposed() throws Exception {
        AdminStatusService statusService = mock(AdminStatusService.class);
        AdminDashboardFacade dashboardFacade = mock(AdminDashboardFacade.class);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new PolicyAdminDashboardController(statusService, dashboardFacade))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        mockMvc.perform(post("/api/policies/admin/qdrant/search"))
                .andExpect(status().isNotFound());
    }

    private AdminStatusResponse statusResponse() {
        return new AdminStatusResponse(
                "UP",
                true,
                true,
                true,
                "chat",
                "embedding",
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                "collection",
                2,
                2,
                0,
                0,
                2,
                0,
                null,
                2,
                0,
                1,
                0,
                1,
                0,
                0,
                0,
                true,
                true,
                10,
                1,
                9,
                0,
                null,
                null
        );
    }
}
