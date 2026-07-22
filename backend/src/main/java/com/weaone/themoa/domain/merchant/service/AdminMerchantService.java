package com.weaone.themoa.domain.merchant.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.category.repository.CategoryRepository;
import com.weaone.themoa.domain.cardtransaction.repository.CardTransactionRepository;
import com.weaone.themoa.domain.merchant.dto.response.AdminMerchantPromotionCandidateResponse;
import com.weaone.themoa.domain.merchant.dto.response.AdminUnclassifiedMerchantResponse;
import com.weaone.themoa.domain.merchant.entity.Merchant;
import com.weaone.themoa.domain.merchant.entity.MerchantAlias;
import com.weaone.themoa.domain.merchant.entity.MerchantAliasTerms;
import com.weaone.themoa.domain.merchant.entity.PromotionCandidateRejection;
import com.weaone.themoa.domain.merchant.repository.MerchantAliasRepository;
import com.weaone.themoa.domain.merchant.repository.MerchantAliasTermsRepository;
import com.weaone.themoa.domain.merchant.repository.MerchantRepository;
import com.weaone.themoa.domain.merchant.repository.PromotionCandidateRejectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 가맹점 & 서비스 마스터 관리자 화면(merchant.md §1·§2-1 확장): per-user 학습 표기의 전역 승격 +
 * 최근 미식별 가맹점 빠른 등록. 둘 다 "전역 시드"를 채우는 관리자 판단 영역이라 merchant 쪽 §2-1 원칙
 * (전역 alias 연결은 관리자 시드만) 그대로 재사용한다.
 *
 * <p>승격은 항상 원본 표기(raw text) 기준이다 — raw text가 같다는 사실 자체가 "같은 실제 서비스"라는
 * 객관적 증거라서, 그 표기에 붙은 여러 제안 중 admin이 하나를 골라(기존 alias 재사용 또는 새 이름 직접
 * 입력) 전역 기본값으로 확정한다. 원본 표기 없이 이름만 다른 alias 중복(예: 고정지출 직접 등록 시 자유
 * 입력)은 raw text로 묶을 근거가 없어 이 화면에서 다루지 않는다 — 그런 alias는 실제로 아무런 매칭에도
 * 관여하지 않아 방치해도 해가 없고, 나중에 누군가 F-05로 raw text를 확인하는 순간 이 화면에 정상적으로
 * 편입된다.
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
    private final PromotionCandidateRejectionRepository promotionCandidateRejectionRepository;

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
        promoteToAlias(alias, aliasText);
    }

    /**
     * 제안된 이름들이 다 마음에 안 들 때, 관리자가 새 서비스명을 직접 지어서 이 표기를 그걸로 승격한다.
     * 기존 제안 중 아무거나 골라 승격하는 것과 마찬가지로, 다른 학습자의 개인 표기는 건드리지 않는다.
     */
    @Transactional
    public void promoteAsNewService(String aliasText, String canonicalServiceName, Long categoryId) {
        if (merchantAliasTermsRepository.findGlobalByRawName(aliasText).isPresent()) {
            return;
        }
        MerchantAlias alias = merchantAliasRepository.findByCanonicalServiceNameNormalized(canonicalServiceName)
                .orElseGet(() -> {
                    Category category = categoryId == null ? null : categoryRepository.getReferenceById(categoryId);
                    return merchantAliasRepository.save(MerchantAlias.create(canonicalServiceName, category));
                });
        promoteToAlias(alias, aliasText);
    }

    private void promoteToAlias(MerchantAlias alias, String aliasText) {
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

    /**
     * 이 (표기, 제안 서비스명) 조합을 승격 대기목록에서 다시 안 뜨게 반려한다 — "티이이빙"처럼 그 조합
     * 자체가 틀렸다고 판단될 때 쓴다. 학습한 회원의 개인 표기(merchant_alias_terms)는 손대지 않는다 —
     * 반려는 관리자의 전역 후보 큐 관점의 판단일 뿐, 그 회원이 자기 화면에서 뭘 보는지와는 무관하다
     * (개인 학습은 늘 전역보다 우선하므로 그 회원 본인에게는 아무 영향이 없다).
     */
    @Transactional
    public void rejectCandidate(Long aliasId, String aliasText) {
        if (promotionCandidateRejectionRepository.existsByMerchantAlias_IdAndAliasText(aliasId, aliasText)) {
            return;
        }
        MerchantAlias alias = merchantAliasRepository.findById(aliasId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_ALIAS_NOT_FOUND));
        promotionCandidateRejectionRepository.save(
                PromotionCandidateRejection.reject(alias, aliasText, LocalDateTime.now()));
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
}
