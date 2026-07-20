package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.repository.PolicySearchProjectionRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PolicyLexicalIndexBuilderTest {

    @Test
    void refreshFailureKeepsPreviousCachedIndex() {
        PolicySearchProjectionRepository projectionRepository = mock(PolicySearchProjectionRepository.class);
        PolicyKeywordNormalizer normalizer = mock(PolicyKeywordNormalizer.class);
        PolicyLexicalIndexBuilder builder = new PolicyLexicalIndexBuilder(projectionRepository, normalizer);
        when(projectionRepository.findAllActive())
                .thenReturn(List.of())
                .thenThrow(new IllegalStateException("index build failed"));

        PolicyLexicalIndex previousIndex = builder.current();

        assertThatThrownBy(builder::refresh)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("index build failed");
        assertThat(builder.current()).isSameAs(previousIndex);
    }
}
