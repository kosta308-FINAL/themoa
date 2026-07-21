package com.weaone.themoa.domain.policy.bookmark.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.policy.bookmark.dto.response.PolicyBookmarkListResponse;
import com.weaone.themoa.domain.policy.bookmark.dto.response.PolicyBookmarkResponse;
import com.weaone.themoa.domain.policy.bookmark.entity.PolicyBookmark;
import com.weaone.themoa.domain.policy.bookmark.repository.PolicyBookmarkRepository;
import com.weaone.themoa.domain.policy.policy.entity.Policy;
import com.weaone.themoa.domain.policy.policy.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PolicyBookmarkService {
    private final PolicyBookmarkRepository bookmarkRepository;
    private final MemberRepository memberRepository;
    private final PolicyRepository policyRepository;

    @Transactional
    public PolicyBookmarkResponse add(Long memberId, Integer policyId) {
        return bookmarkRepository.findByMember_IdAndPolicy_Id(memberId, policyId)
                .map(PolicyBookmarkResponse::from)
                .orElseGet(() -> create(memberId, policyId));
    }

    @Transactional(readOnly = true)
    public PolicyBookmarkListResponse list(Long memberId) {
        return new PolicyBookmarkListResponse(bookmarkRepository.findByMember_IdOrderByIdDesc(memberId)
                .stream()
                .map(PolicyBookmarkResponse::from)
                .toList());
    }

    @Transactional
    public void remove(Long memberId, Integer policyId) {
        bookmarkRepository.deleteByMember_IdAndPolicy_Id(memberId, policyId);
    }

    private PolicyBookmarkResponse create(Long memberId, Integer policyId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_NOT_FOUND));
        return PolicyBookmarkResponse.from(bookmarkRepository.save(PolicyBookmark.interest(member, policy)));
    }
}
