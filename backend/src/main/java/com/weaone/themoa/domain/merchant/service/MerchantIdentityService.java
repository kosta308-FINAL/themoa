package com.weaone.themoa.domain.merchant.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.merchant.entity.Merchant;
import com.weaone.themoa.domain.merchant.entity.MerchantAlias;
import com.weaone.themoa.domain.merchant.entity.MerchantAliasTerms;
import com.weaone.themoa.domain.merchant.repository.BillerRepository;
import com.weaone.themoa.domain.merchant.repository.MerchantAliasRepository;
import com.weaone.themoa.domain.merchant.repository.MerchantAliasTermsRepository;
import com.weaone.themoa.domain.merchant.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 가맹점 신원 판별·학습의 단일 진입점(merchant.md). category.md·fixedExpense.md가 이 서비스를 공유한다.
 * card_transaction 도메인은 아직 구현 전이라, 해석 결과를 거래 행에 저장하는 것은 호출자(향후
 * cardtransaction.md 구현) 책임이다 — 이 서비스는 판별 결과만 반환한다.
 */
@Service
@RequiredArgsConstructor
public class MerchantIdentityService {

    private final MerchantRepository merchantRepository;
    private final MerchantAliasRepository merchantAliasRepository;
    private final MerchantAliasTermsRepository merchantAliasTermsRepository;
    private final BillerRepository billerRepository;
    private final MemberRepository memberRepository;

    /**
     * 수집 시 자동 매칭(merchant.md §3 2단계, §5-D-2). 원본 가맹점명 기준 merchant를 찾거나 만들고,
     * biller면 이름 판별을 건너뛴 채 결과만 반환한다. 그 외에는 내 term → 전역 시드 term → merchant의
     * 전역 시드 연결 순으로 alias를 찾고, 못 찾으면 NULL로 끝낸다(추측 금지).
     */
    @Transactional
    public MerchantIdentityResult resolve(Long memberId, String merchantNameRaw) {
        Merchant merchant = findOrCreateMerchant(merchantNameRaw);
        if (billerRepository.existsByNameNormalized(merchantNameRaw)) {
            return MerchantIdentityResult.biller(merchant.getId());
        }

        Long aliasId = merchantAliasTermsRepository.findMineByRawName(memberId, merchantNameRaw)
                .or(() -> merchantAliasTermsRepository.findGlobalByRawName(merchantNameRaw))
                .map(term -> term.getMerchantAlias().getId())
                .orElseGet(() -> merchant.getMerchantAlias() == null ? null : merchant.getMerchantAlias().getId());
        return MerchantIdentityResult.identified(merchant.getId(), aliasId);
    }

    /**
     * 직접 등록(merchant.md §3 1단계). 거래 0건인 상태에서도 alias를 만들 수 있다. 같은 서비스명이
     * 이미 있으면 재사용하고, 초기 표기가 있으면 그 사용자 학습분으로 함께 등록한다.
     */
    @Transactional
    public MerchantAlias registerAlias(Long memberId, String canonicalServiceName, Long defaultCategoryId,
                                        String initialTermText) {
        MerchantAlias alias = merchantAliasRepository.findByCanonicalServiceNameNormalized(canonicalServiceName)
                .orElseGet(() -> saveNewAlias(canonicalServiceName, defaultCategoryId));
        if (StringUtils.hasText(initialTermText)) {
            learnTerm(memberId, alias.getId(), initialTermText);
        }
        return alias;
    }

    /**
     * 학습 루프(merchant.md §3 4단계). 반드시 그 사용자 소유로만 저장된다. biller 이름은 절대
     * 등록하지 않는다(§5-D-4 가드) — 그 이름은 여러 무관 서비스를 가려서 전역은 물론 그 사용자
     * 안에서도 표기 하나로 서비스 하나를 특정할 수 없기 때문이다.
     */
    @Transactional
    public void learnTerm(Long memberId, Long aliasId, String termText) {
        if (billerRepository.existsByNameNormalized(termText)) {
            throw new BusinessException(ErrorCode.MERCHANT_ALIAS_TERM_BILLER_FORBIDDEN);
        }
        MerchantAlias alias = merchantAliasRepository.findById(aliasId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_ALIAS_NOT_FOUND));

        Optional<MerchantAliasTerms> existing = merchantAliasTermsRepository.findByMember_IdAndAliasText(memberId, termText);
        if (existing.isPresent()) {
            rejectIfPointsToOtherAlias(existing.get(), aliasId);
            return;
        }

        Member member = memberRepository.getReferenceById(memberId);
        try {
            merchantAliasTermsRepository.save(MerchantAliasTerms.learn(alias, member, termText));
        } catch (DataIntegrityViolationException e) {
            merchantAliasTermsRepository.findByMember_IdAndAliasText(memberId, termText)
                    .ifPresentOrElse(raced -> rejectIfPointsToOtherAlias(raced, aliasId), () -> {
                        throw e;
                    });
        }
    }

    private void rejectIfPointsToOtherAlias(MerchantAliasTerms term, Long expectedAliasId) {
        if (!term.getMerchantAlias().getId().equals(expectedAliasId)) {
            throw new BusinessException(ErrorCode.MERCHANT_ALIAS_TERM_CONFLICT);
        }
    }

    private Merchant findOrCreateMerchant(String merchantNameRaw) {
        return merchantRepository.findByMerchantNameRaw(merchantNameRaw)
                .orElseGet(() -> createMerchant(merchantNameRaw));
    }

    private Merchant createMerchant(String merchantNameRaw) {
        MerchantAlias globalAlias = merchantAliasTermsRepository.findGlobalByRawName(merchantNameRaw)
                .map(MerchantAliasTerms::getMerchantAlias)
                .orElse(null);
        try {
            return merchantRepository.save(Merchant.observe(merchantNameRaw, globalAlias));
        } catch (DataIntegrityViolationException e) {
            return merchantRepository.findByMerchantNameRaw(merchantNameRaw).orElseThrow(() -> e);
        }
    }

    private MerchantAlias saveNewAlias(String canonicalServiceName, Long defaultCategoryId) {
        try {
            return merchantAliasRepository.save(MerchantAlias.create(canonicalServiceName, defaultCategoryId));
        } catch (DataIntegrityViolationException e) {
            return merchantAliasRepository.findByCanonicalServiceNameNormalized(canonicalServiceName)
                    .orElseThrow(() -> e);
        }
    }
}
