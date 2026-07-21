package com.weaone.themoa.domain.customerservice.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.config.CustomerServiceRagProperties;
import com.weaone.themoa.domain.customerservice.entity.CustomerServiceRagSetting;
import com.weaone.themoa.domain.customerservice.rag.CustomerServiceRagSettingValues;
import com.weaone.themoa.domain.customerservice.repository.CustomerServiceRagSettingRepository;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CustomerServiceRagSettingService {

    private static final int MIN_TOP_K = 1;
    private static final int MAX_TOP_K = 20;
    private static final int MAX_PROMPT_LENGTH = 6_000;

    private final CustomerServiceRagProperties properties;
    private final CustomerServiceRagSettingRepository settingRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public CustomerServiceRagSettingValues current() {
        return settingRepository.findTopByOrderByIdAsc()
                .map(setting -> new CustomerServiceRagSettingValues(
                        setting.getTopK(),
                        setting.getMinimumSimilarity(),
                        setting.getSystemPrompt()))
                .orElseGet(() -> defaults());
    }

    @Transactional
    public CustomerServiceRagSettingValues update(Long adminId, Integer topK, Double minimumSimilarity,
                                                  String systemPrompt) {
        CustomerServiceRagSettingValues normalized = normalize(topK, minimumSimilarity, systemPrompt);
        Member admin = adminId == null ? null : memberRepository.getReferenceById(adminId);
        LocalDateTime now = LocalDateTime.now();
        CustomerServiceRagSetting setting = settingRepository.findTopByOrderByIdAsc()
                .orElseGet(() -> settingRepository.save(CustomerServiceRagSetting.create(
                        normalized.topK(),
                        normalized.minimumSimilarity(),
                        normalized.systemPrompt(),
                        admin,
                        now)));
        setting.update(normalized.topK(), normalized.minimumSimilarity(), normalized.systemPrompt(), admin, now);
        return normalized;
    }

    public CustomerServiceRagSettingValues normalize(Integer topK, Double minimumSimilarity, String systemPrompt) {
        int normalizedTopK = topK == null ? properties.topK() : topK;
        double normalizedMinimumSimilarity = minimumSimilarity == null
                ? properties.minimumSimilarity()
                : minimumSimilarity;
        String normalizedPrompt = StringUtils.hasText(systemPrompt)
                ? systemPrompt.trim()
                : CustomerServiceChatService.DEFAULT_SYSTEM_PROMPT;
        if (normalizedTopK < MIN_TOP_K || normalizedTopK > MAX_TOP_K
                || normalizedMinimumSimilarity < 0 || normalizedMinimumSimilarity > 1
                || normalizedPrompt.length() > MAX_PROMPT_LENGTH) {
            throw new BusinessException(ErrorCode.CUSTOMER_KNOWLEDGE_INVALID_REQUEST);
        }
        return new CustomerServiceRagSettingValues(normalizedTopK, normalizedMinimumSimilarity, normalizedPrompt);
    }

    public CustomerServiceRagSettingValues defaults() {
        return new CustomerServiceRagSettingValues(
                properties.topK(),
                properties.minimumSimilarity(),
                CustomerServiceChatService.DEFAULT_SYSTEM_PROMPT);
    }
}
