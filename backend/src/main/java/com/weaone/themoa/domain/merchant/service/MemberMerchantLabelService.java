package com.weaone.themoa.domain.merchant.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.merchant.entity.MemberMerchantAliasLabel;
import com.weaone.themoa.domain.merchant.entity.MerchantAlias;
import com.weaone.themoa.domain.merchant.repository.MemberMerchantAliasLabelRepository;
import com.weaone.themoa.domain.merchant.repository.MerchantAliasRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 회원 개인 표시명 오버라이드(merchant.md 확장). 전역 {@code canonical_service_name}보다 항상 우선한다 —
 * 승격/재분류로 전역 이름이 바뀌어도 이 회원 화면에는 영향이 없다.
 */
@Service
@RequiredArgsConstructor
public class MemberMerchantLabelService {

    private final MemberMerchantAliasLabelRepository memberMerchantAliasLabelRepository;
    private final MerchantAliasRepository merchantAliasRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void setLabel(Long memberId, Long merchantAliasId, String label) {
        if (!StringUtils.hasText(label)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        MerchantAlias alias = merchantAliasRepository.findById(merchantAliasId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_ALIAS_NOT_FOUND));
        memberMerchantAliasLabelRepository.findByMember_IdAndMerchantAlias_Id(memberId, merchantAliasId)
                .ifPresentOrElse(
                        existing -> existing.updateLabel(label.trim()),
                        () -> {
                            Member member = memberRepository.getReferenceById(memberId);
                            memberMerchantAliasLabelRepository.save(
                                    MemberMerchantAliasLabel.create(member, alias, label.trim()));
                        });
    }

    /** 표시명을 지우고 전역 이름으로 되돌린다. */
    @Transactional
    public void clearLabel(Long memberId, Long merchantAliasId) {
        memberMerchantAliasLabelRepository.deleteByMember_IdAndMerchantAlias_Id(memberId, merchantAliasId);
    }

    @Transactional(readOnly = true)
    public Optional<String> resolveLabel(Long memberId, Long merchantAliasId) {
        return memberMerchantAliasLabelRepository.findByMember_IdAndMerchantAlias_Id(memberId, merchantAliasId)
                .map(MemberMerchantAliasLabel::getLabel);
    }

    /** 거래 목록 매핑용 일괄 조회. 라벨이 없는 alias는 맵에 아예 안 들어간다(호출부에서 getOrDefault/null 처리). */
    @Transactional(readOnly = true)
    public Map<Long, String> resolveLabels(Long memberId, Collection<Long> merchantAliasIds) {
        List<Long> distinctIds = merchantAliasIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> labels = new HashMap<>();
        for (MemberMerchantAliasLabel entry : memberMerchantAliasLabelRepository
                .findByMember_IdAndMerchantAlias_IdIn(memberId, distinctIds)) {
            labels.put(entry.getMerchantAlias().getId(), entry.getLabel());
        }
        return labels;
    }
}
