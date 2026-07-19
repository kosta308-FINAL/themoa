package com.weaone.themoa.domain.policy.admin.service;

import com.weaone.themoa.domain.policy.rag.dto.SearchReadinessResponse;
import com.weaone.themoa.domain.policy.rag.service.SearchReadinessService;
import com.weaone.themoa.domain.policy.admin.dto.response.AdminSearchIndexStatusResponse;
import com.weaone.themoa.domain.policy.policy.repository.PolicySearchProjectionRepository;
import com.weaone.themoa.domain.policy.rag.service.PolicyLexicalIndexBuilder;
import com.weaone.themoa.domain.policy.rag.service.PolicySearchProjectionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class AdminSearchIndexServiceTest {

    @Test
    @DisplayName("검색 인덱스 상태 조회는 Repository 값을 API 응답 record로 변환한다")
    void searchIndexStatusReturnsRecord() {
        PolicySearchProjectionService projectionService = mock(PolicySearchProjectionService.class);
        PolicyLexicalIndexBuilder lexicalIndexBuilder = mock(PolicyLexicalIndexBuilder.class);
        PolicySearchProjectionRepository projectionRepository = mock(PolicySearchProjectionRepository.class);
        SearchReadinessService readinessService = mock(SearchReadinessService.class);
        AdminSearchIndexService service = new AdminSearchIndexService(
                projectionService,
                lexicalIndexBuilder,
                projectionRepository,
                readinessService
        );
        given(readinessService.readiness()).willReturn(new SearchReadinessResponse(false, 10, 8, 7, 6, List.of("SEARCH_INDEX_REFRESH")));
        given(lexicalIndexBuilder.cachedBuiltAt()).willReturn(Instant.parse("2026-07-20T00:00:00Z"));
        given(projectionRepository.countByMissingSnapshotTrue()).willReturn(2L);

        AdminSearchIndexStatusResponse response = service.searchIndexStatus();

        assertThat(response.ready()).isFalse();
        assertThat(response.documentCount()).isEqualTo(7);
        assertThat(response.projectionCount()).isEqualTo(8);
        assertThat(response.missingSnapshotCount()).isEqualTo(2);
        assertThat(response.builtAt()).isEqualTo("2026-07-20T00:00:00Z");
        assertThat(response.missingSteps()).containsExactly("SEARCH_INDEX_REFRESH");
    }
}
