package com.weaone.themoa.domain.financialsearch.service;

import com.weaone.themoa.domain.financialsearch.config.FinancialRagProperties;
import com.weaone.themoa.domain.financialsearch.entity.FinancialRagSetting;
import com.weaone.themoa.domain.financialsearch.repository.FinancialRagSettingRepository;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 검색 튜닝값 조회·변경. 저장된 설정이 없으면 application.yaml 기본값을 쓴다.
 *
 * <p>입력값은 항상 허용 범위로 조정한다. 관리자 실수로 임계값을 1.0으로 올려 검색이 통째로 0건이
 * 되거나, topK를 수천으로 올려 응답이 폭주하는 일을 막기 위함이다.
 */
@Service
public class FinancialRagSettingService {

    private static final int MIN_TOP_K = 1;
    private static final int MAX_TOP_K = 50;
    private static final int MIN_RETRY_TOP_K = 1;
    private static final int MAX_RETRY_TOP_K = 100;
    private static final double MIN_SIMILARITY = 0.0;
    private static final double MAX_SIMILARITY = 1.0;

    private final FinancialRagProperties properties;
    private final FinancialRagSettingRepository settingRepository;
    private final MemberRepository memberRepository;

    public FinancialRagSettingService(FinancialRagProperties properties,
                                      FinancialRagSettingRepository settingRepository,
                                      MemberRepository memberRepository) {
        this.properties = properties;
        this.settingRepository = settingRepository;
        this.memberRepository = memberRepository;
    }

    /** 검색에 지금 적용되는 값. */
    @Transactional(readOnly = true)
    public FinancialRagSettingValues current() {
        return settingRepository.findTopByOrderByIdAsc()
                .map(setting -> new FinancialRagSettingValues(
                        setting.getTopK(), setting.getRetryTopK(), setting.getMinimumSimilarity()))
                .orElseGet(this::defaults);
    }

    /** 마지막으로 누가 언제 바꿨는지. 아직 기본값을 쓰고 있으면 비어 있다. */
    @Transactional(readOnly = true)
    public Optional<LocalDateTime> findLastUpdatedAt() {
        return settingRepository.findTopByOrderByIdAsc().map(FinancialRagSetting::getUpdatedAt);
    }

    /**
     * 설정 변경. null로 넘긴 항목은 application.yaml 기본값으로 되돌아간다.
     *
     * @return 조정 후 실제로 적용된 값
     */
    @Transactional
    public FinancialRagSettingValues update(Long adminId, Integer topK, Integer retryTopK, Double minimumSimilarity) {
        FinancialRagSettingValues normalized = normalize(topK, retryTopK, minimumSimilarity);
        Member admin = adminId == null ? null : memberRepository.getReferenceById(adminId);
        LocalDateTime now = LocalDateTime.now();

        settingRepository.findTopByOrderByIdAsc()
                .ifPresentOrElse(
                        setting -> setting.update(normalized.topK(), normalized.retryTopK(),
                                normalized.minimumSimilarity(), admin, now),
                        () -> settingRepository.save(FinancialRagSetting.create(normalized.topK(),
                                normalized.retryTopK(), normalized.minimumSimilarity(), admin, now)));
        return normalized;
    }

    /**
     * 저장된 설정을 지워 application.yaml 기본값으로 완전히 되돌린다.
     * 기본값을 그대로 저장하는 게 아니라 행 자체를 지우므로, 이후 yaml 기본값이 바뀌면 그 값을 따라간다.
     *
     * @return 되돌린 뒤 적용되는 기본값
     */
    @Transactional
    public FinancialRagSettingValues resetToDefaults() {
        settingRepository.findTopByOrderByIdAsc().ifPresent(settingRepository::delete);
        return defaults();
    }

    private FinancialRagSettingValues defaults() {
        return new FinancialRagSettingValues(
                properties.getTopK(), properties.getRetryTopK(), properties.getMinimumSimilarity());
    }

    private FinancialRagSettingValues normalize(Integer topK, Integer retryTopK, Double minimumSimilarity) {
        FinancialRagSettingValues defaults = defaults();
        return new FinancialRagSettingValues(
                clamp(topK == null ? defaults.topK() : topK, MIN_TOP_K, MAX_TOP_K),
                clamp(retryTopK == null ? defaults.retryTopK() : retryTopK, MIN_RETRY_TOP_K, MAX_RETRY_TOP_K),
                clamp(minimumSimilarity == null ? defaults.minimumSimilarity() : minimumSimilarity,
                        MIN_SIMILARITY, MAX_SIMILARITY));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
