package com.weaone.themoa.domain.member.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.auth.entity.MemberTermsAgreement;
import com.weaone.themoa.domain.auth.repository.MemberTermsAgreementRepository;
import com.weaone.themoa.domain.member.dto.response.MyPageResponse;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MyPageQueryService {

    private final MemberRepository memberRepository;
    private final MemberTermsAgreementRepository memberTermsAgreementRepository;

    @Transactional(readOnly = true)
    public MyPageResponse getMyPage(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        List<MemberTermsAgreement> termsAgreements =
                memberTermsAgreementRepository.findByMember_IdOrderByAgreedAtDesc(memberId);
        return MyPageResponse.of(member, termsAgreements);
    }
}
