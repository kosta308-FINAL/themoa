package com.weaone.themoa.domain.policy.admin.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.policy.admin.dto.embedding.AdminEmbeddingPageResponse;
import com.weaone.themoa.domain.policy.admin.service.AdminEmbeddingReadService;
import com.weaone.themoa.domain.policy.youthcenter.dto.response.YouthCenterProbeResponse;
import com.weaone.themoa.domain.policy.youthcenter.service.YouthCenterDiagnosticService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("local")
@ConditionalOnProperty(prefix = "app.policy.local-tools", name = "enabled", havingValue = "true")
@RequestMapping("/api/policies/admin")
public class PolicyAdminEmbeddingController {
    private final AdminEmbeddingReadService embeddingReadService;
    private final YouthCenterDiagnosticService diagnosticService;

    public PolicyAdminEmbeddingController(AdminEmbeddingReadService embeddingReadService,
                                          YouthCenterDiagnosticService diagnosticService) {
        this.embeddingReadService = embeddingReadService;
        this.diagnosticService = diagnosticService;
    }

    @GetMapping("/embeddings")
    public ApiResponse<AdminEmbeddingPageResponse> embeddings(@RequestParam(value = "status", required = false) String status,
                                                              @RequestParam(value = "keyword", required = false) String keyword,
                                                              @RequestParam(value = "page", defaultValue = "0") int page,
                                                              @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success(embeddingReadService.search(status, keyword, page, size));
    }

    @PostMapping("/youth-center/probe")
    public ApiResponse<YouthCenterProbeResponse> probe() {
        return ApiResponse.success(diagnosticService.probe());
    }
}
