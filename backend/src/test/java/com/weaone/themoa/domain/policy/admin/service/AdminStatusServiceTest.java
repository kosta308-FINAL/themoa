package com.weaone.themoa.domain.policy.admin.service;

import com.weaone.themoa.domain.policy.admin.dto.AdminStatusResponse;
import com.weaone.themoa.domain.policy.common.config.LocalSecretConfigurationStatus;
import com.weaone.themoa.domain.policy.policy.repository.PolicyEmbeddingSyncRepository;
import com.weaone.themoa.domain.policy.policy.repository.PolicyRepository;
import com.weaone.themoa.domain.policy.policy.repository.RegionCodeRepository;
import com.weaone.themoa.domain.policy.policy.repository.PolicySourceSnapshotRepository;
import com.weaone.themoa.domain.policy.region.config.RegionSyncProperties;
import com.weaone.themoa.domain.policy.region.service.RegionSynchronizationState;
import com.weaone.themoa.domain.policy.rag.config.RagProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminStatusServiceTest {
    @Test
    void statusDoesNotExposeSecretValues() {
        PolicyRepository policyRepository = mock(PolicyRepository.class);
        PolicyEmbeddingSyncRepository syncRepository = mock(PolicyEmbeddingSyncRepository.class);
        RegionCodeRepository regionCodeRepository = mock(RegionCodeRepository.class);
        PolicySourceSnapshotRepository snapshotRepository = mock(PolicySourceSnapshotRepository.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<VectorStore> vectorStoreProvider = mock(ObjectProvider.class);
        RagProperties ragProperties = new RagProperties();
        ragProperties.setEnabled(true);
        LocalSecretConfigurationStatus configurationStatus = new LocalSecretConfigurationStatus(
                "real-youth-key", "real-openai-key", "openai", "openai", true);
        when(policyRepository.count()).thenReturn(1L);
        when(policyRepository.countByActiveTrue()).thenReturn(1L);
        when(syncRepository.countBySyncStatus("PENDING")).thenReturn(0L);
        when(syncRepository.countBySyncStatus("PROCESSING")).thenReturn(0L);
        when(syncRepository.countBySyncStatus("SYNCED")).thenReturn(1L);
        when(syncRepository.countBySyncStatus("FAILED")).thenReturn(0L);

        AdminStatusResponse response = new AdminStatusService(policyRepository, syncRepository, vectorStoreProvider,
                ragProperties, configurationStatus, regionCodeRepository, snapshotRepository, regionSyncProperties(), new RegionSynchronizationState()).status();

        assertThat(response.youthCenterApiKeyConfigured()).isTrue();
        assertThat(response.openAiApiKeyConfigured()).isTrue();
        assertThat(response.chatModelAvailable()).isTrue();
        assertThat(response.embeddingModelAvailable()).isTrue();
        assertThat(response.toString()).doesNotContain("real-youth-key", "real-openai-key");
    }

    @Test
    void statusRespondsWhenDatabaseIsUnavailable() {
        PolicyRepository policyRepository = mock(PolicyRepository.class);
        PolicyEmbeddingSyncRepository syncRepository = mock(PolicyEmbeddingSyncRepository.class);
        RegionCodeRepository regionCodeRepository = mock(RegionCodeRepository.class);
        PolicySourceSnapshotRepository snapshotRepository = mock(PolicySourceSnapshotRepository.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<VectorStore> vectorStoreProvider = mock(ObjectProvider.class);
        RagProperties ragProperties = new RagProperties();
        LocalSecretConfigurationStatus configurationStatus = new LocalSecretConfigurationStatus("", "", "none", "none", false);
        when(policyRepository.count()).thenThrow(new DataAccessResourceFailureException("db down"));

        AdminStatusResponse response = new AdminStatusService(policyRepository, syncRepository, vectorStoreProvider,
                ragProperties, configurationStatus, regionCodeRepository, snapshotRepository, regionSyncProperties(), new RegionSynchronizationState()).status();

        assertThat(response.mysqlAvailable()).isFalse();
        assertThat(response.youthCenterApiKeyConfigured()).isFalse();
        assertThat(response.chatModelAvailable()).isFalse();
        assertThat(response.embeddingModelAvailable()).isFalse();
        assertThat(response.ragEnabled()).isFalse();
    }

    private RegionSyncProperties regionSyncProperties() {
        return new RegionSyncProperties(false, false, "0 0 4 1 * *",
                java.time.Duration.ofMillis(100), java.time.Duration.ofSeconds(5), java.time.Duration.ofSeconds(20), 3,
                new RegionSyncProperties.Sgis("https://sgisapi.mods.go.kr", "", ""));
    }
}
