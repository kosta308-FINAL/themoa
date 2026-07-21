package com.weaone.themoa.domain.merchant.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.category.repository.CategoryRepository;
import com.weaone.themoa.domain.cardtransaction.repository.CardTransactionRepository;
import com.weaone.themoa.domain.coaching.repository.CoachingDismissRepository;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpenseRepository;
import com.weaone.themoa.domain.fixedexpense.repository.RecurringPaymentGroupRepository;
import com.weaone.themoa.domain.fixedexpense.repository.UserMerchantPreferenceRepository;
import com.weaone.themoa.domain.merchant.dto.response.AdminMerchantAliasSummaryResponse;
import com.weaone.themoa.domain.merchant.dto.response.AdminMerchantPromotionCandidateResponse;
import com.weaone.themoa.domain.merchant.dto.response.AdminUnclassifiedMerchantResponse;
import com.weaone.themoa.domain.merchant.entity.Merchant;
import com.weaone.themoa.domain.merchant.entity.MerchantAlias;
import com.weaone.themoa.domain.merchant.entity.MerchantAliasTerms;
import com.weaone.themoa.domain.merchant.repository.MerchantAliasRepository;
import com.weaone.themoa.domain.merchant.repository.MerchantAliasTermsRepository;
import com.weaone.themoa.domain.merchant.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 가맹점 & 서비스 마스터 관리자 화면(merchant.md §1·§2-1 확장): per-user 학습 표기의 전역 승격 +
 * 최근 미식별 가맹점 빠른 등록. 둘 다 "전역 시드"를 채우는 관리자 판단 영역이라 merchant 쪽 §2-1 원칙
 * (전역 alias 연결은 관리자 시드만) 그대로 재사용한다.
 */
@Service
@RequiredArgsConstructor
public class AdminMerchantService {

    private static final int UNCLASSIFIED_WINDOW_DAYS = 15;
    private static final int UNCLASSIFIED_LIMIT = 30;
    private static final int PROMOTION_CANDIDATE_LIMIT = 50;

    private final MerchantRepository merchantRepository;
    private final MerchantAliasRepository merchantAliasRepository;
    private final MerchantAliasTermsRepository merchantAliasTermsRepository;
    private final CardTransactionRepository cardTransactionRepository;
    private final CategoryRepository categoryRepository;
    private final FixedExpenseRepository fixedExpenseRepository;
    private final CoachingDismissRepository coachingDismissRepository;
    private final UserMerchantPreferenceRepository userMerchantPreferenceRepository;
    private final RecurringPaymentGroupRepository recurringPaymentGroupRepository;

    @Transactional(readOnly = true)
    public List<AdminMerchantPromotionCandidateResponse> listPromotionCandidates() {
        return merchantAliasTermsRepository.findPromotionCandidates(PageRequest.of(0, PROMOTION_CANDIDATE_LIMIT))
                .stream()
                .map(row -> new AdminMerchantPromotionCandidateResponse(
                        row.getAliasId(), row.getAliasText(), row.getCanonicalServiceName(),
                        row.getCategoryName(), row.getLearnerCount()))
                .toList();
    }

    /**
     * 이미 전역 표기로 있으면 아무 것도 하지 않는다(멱등) — 여러 회원이 같은 표기를 각자 학습했어도 승격은 한 번만 실질 반영된다.
     * 전역 표기 추가에 그치지 않고, 이 원본 가맹점명으로 이미 쌓여 있던 모든 회원의 미분류 거래도 그 자리에서
     * 소급 재분류한다(다음 동기화까지 기다리지 않아도 됨) — 승격의 효과가 관리자 화면에서 바로 확인돼야 하기 때문이다.
     */
    @Transactional
    public void promote(Long aliasId, String aliasText) {
        if (merchantAliasTermsRepository.findGlobalByRawName(aliasText).isPresent()) {
            return;
        }
        MerchantAlias alias = merchantAliasRepository.findById(aliasId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_ALIAS_NOT_FOUND));
        merchantAliasTermsRepository.save(MerchantAliasTerms.seed(alias, aliasText));
        reclassifyExistingTransactions(alias, aliasText);
    }

    private void reclassifyExistingTransactions(MerchantAlias alias, String aliasText) {
        merchantRepository.findByMerchantNameRaw(aliasText).ifPresent(merchant -> {
            merchant.linkGlobalAlias(alias);
            cardTransactionRepository.findByMerchant_IdAndMerchantAliasIsNull(merchant.getId())
                    .forEach(tx -> tx.assignMerchant(merchant, alias));
        });
    }

    @Transactional(readOnly = true)
    public List<AdminUnclassifiedMerchantResponse> listUnclassifiedMerchants() {
        LocalDate since = LocalDate.now().minusDays(UNCLASSIFIED_WINDOW_DAYS);
        return cardTransactionRepository.findUnclassifiedMerchants(since, UNCLASSIFIED_LIMIT).stream()
                .map(row -> new AdminUnclassifiedMerchantResponse(
                        row.getMerchantId(), row.getMerchantNameRaw(), row.getMerchantTypeRaw(),
                        row.getTransactionCount(), row.getAverageAmount()))
                .toList();
    }

    /**
     * 관리자 직접 등록(merchant.md §2-1 전역 시드 경로). 같은 서비스명이 이미 있으면 재사용하고,
     * 이 merchant의 원본명을 전역 표기로 추가한 뒤 merchant를 그 alias에 직접 연결한다.
     */
    @Transactional
    public void quickRegisterGlobalAlias(Long merchantId, String canonicalServiceName, Long categoryId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_NOT_FOUND));

        MerchantAlias alias = merchantAliasRepository.findByCanonicalServiceNameNormalized(canonicalServiceName)
                .orElseGet(() -> {
                    Category category = categoryId == null ? null : categoryRepository.getReferenceById(categoryId);
                    return merchantAliasRepository.save(MerchantAlias.create(canonicalServiceName, category));
                });

        if (merchantAliasTermsRepository.findGlobalByRawName(merchant.getMerchantNameRaw()).isEmpty()) {
            merchantAliasTermsRepository.save(MerchantAliasTerms.seed(alias, merchant.getMerchantNameRaw()));
        }
        merchant.linkGlobalAlias(alias);
    }

    /**
     * 서비스(MerchantAlias) 전체 목록 — 등록 경로가 여러 곳이라(F-03 검색 vs 직접 등록 시 자유 입력) 같은
     * 실서비스가 이름만 다른 여러 행으로 쌓일 수 있다. 관리자가 이름순으로 훑어 중복을 찾아 병합하는 용도다.
     */
    @Transactional(readOnly = true)
    public List<AdminMerchantAliasSummaryResponse> listAllAliases() {
        return merchantAliasRepository.findAllWithUsage().stream()
                .map(row -> new AdminMerchantAliasSummaryResponse(
                        row.getAliasId(), row.getCanonicalServiceName(), row.getCategoryName(),
                        row.getFixedExpenseCount(), row.getMerchantCount()))
                .toList();
    }

    /**
     * 중복 생성된 서비스를 하나로 합친다. {@code sourceAliasIds} 아래 있던 원본 가맹점(Merchant)·고정지출·
     * 회원 학습 표기·카드거래 스냅샷을 전부 {@code targetAliasId}로 옮기고 원본 서비스는 지운다.
     * 코칭 넘김·선호도·반복결제 그룹은 회원별 UNIQUE라 옮기지 않고 그냥 지운다(다음 주기에 다시 쌓인다).
     */
    @Transactional
    public void mergeAliases(Long targetAliasId, List<Long> sourceAliasIds) {
        MerchantAlias target = merchantAliasRepository.findById(targetAliasId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_ALIAS_NOT_FOUND));

        for (Long sourceAliasId : sourceAliasIds) {
            if (sourceAliasId.equals(targetAliasId)) {
                continue;
            }
            MerchantAlias source = merchantAliasRepository.findById(sourceAliasId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_ALIAS_NOT_FOUND));

            merchantRepository.findByMerchantAlias_Id(sourceAliasId).forEach(m -> m.linkGlobalAlias(target));
            fixedExpenseRepository.findByMerchantAlias_Id(sourceAliasId).forEach(fe -> fe.reassignMerchantAlias(target));
            merchantAliasTermsRepository.findByMerchantAlias_Id(sourceAliasId).forEach(t -> t.reassignAlias(target));
            cardTransactionRepository.findByMerchantAlias_Id(sourceAliasId)
                    .forEach(tx -> tx.assignMerchant(tx.getMerchant(), target));

            coachingDismissRepository.deleteAll(coachingDismissRepository.findByMerchantAlias_Id(sourceAliasId));
            userMerchantPreferenceRepository.deleteAll(
                    userMerchantPreferenceRepository.findByMerchantAlias_Id(sourceAliasId));
            recurringPaymentGroupRepository.deleteAll(
                    recurringPaymentGroupRepository.findByMerchantAlias_Id(sourceAliasId));

            merchantAliasRepository.delete(source);
        }
    }
}
