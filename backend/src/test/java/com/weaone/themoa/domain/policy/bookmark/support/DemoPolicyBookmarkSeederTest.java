package com.weaone.themoa.domain.policy.bookmark.support;

import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.policy.bookmark.repository.PolicyBookmarkRepository;
import com.weaone.themoa.domain.policy.bookmark.service.PolicyBookmarkService;
import com.weaone.themoa.domain.policy.policy.entity.Policy;
import com.weaone.themoa.domain.policy.policy.entity.PolicyCategory;
import com.weaone.themoa.domain.policy.policy.repository.PolicyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DemoPolicyBookmarkSeederTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final PolicyRepository policyRepository = mock(PolicyRepository.class);
    private final PolicyBookmarkRepository policyBookmarkRepository = mock(PolicyBookmarkRepository.class);
    private final PolicyBookmarkService policyBookmarkService = mock(PolicyBookmarkService.class);
    private final DemoPolicyBookmarkSeeder seeder = new DemoPolicyBookmarkSeeder(
            memberRepository,
            policyRepository,
            policyBookmarkRepository,
            policyBookmarkService
    );

    @Test
    void runCreatesFiveBookmarksWhenMemberAndPoliciesExist() {
        given(memberRepository.existsById(1L)).willReturn(true);
        stubPolicies();

        seeder.run(new DefaultApplicationArguments());

        verify(policyBookmarkService).add(1L, 101);
        verify(policyBookmarkService).add(1L, 102);
        verify(policyBookmarkService).add(1L, 103);
        verify(policyBookmarkService).add(1L, 104);
        verify(policyBookmarkService).add(1L, 105);
    }

    @Test
    void runSkipsAlreadyBookmarkedPolicies() {
        given(memberRepository.existsById(1L)).willReturn(true);
        stubPolicies();
        given(policyBookmarkRepository.existsByMember_IdAndPolicy_Id(1L, 101)).willReturn(true);
        given(policyBookmarkRepository.existsByMember_IdAndPolicy_Id(1L, 102)).willReturn(true);
        given(policyBookmarkRepository.existsByMember_IdAndPolicy_Id(1L, 103)).willReturn(true);
        given(policyBookmarkRepository.existsByMember_IdAndPolicy_Id(1L, 104)).willReturn(true);
        given(policyBookmarkRepository.existsByMember_IdAndPolicy_Id(1L, 105)).willReturn(true);

        seeder.run(new DefaultApplicationArguments());

        verify(policyBookmarkService, never()).add(1L, 101);
        verify(policyBookmarkService, never()).add(1L, 102);
        verify(policyBookmarkService, never()).add(1L, 103);
        verify(policyBookmarkService, never()).add(1L, 104);
        verify(policyBookmarkService, never()).add(1L, 105);
    }

    @Test
    void runContinuesWhenOnePolicyIsMissing() {
        given(memberRepository.existsById(1L)).willReturn(true);
        given(policyRepository.findByTitleAndActiveTrue("K-패스(K패스)")).willReturn(List.of(policy(101, "K-패스(K패스)")));
        given(policyRepository.findByTitleAndActiveTrue("청년 자산형성 지원(청년도약계좌)")).willReturn(List.of());
        given(policyRepository.findByTitleAndActiveTrue("경기청년 일자리 매치업 플러스"))
                .willReturn(List.of(policy(103, "경기청년 일자리 매치업 플러스")));
        given(policyRepository.findByTitleAndActiveTrue("경기도 청년 면접수당"))
                .willReturn(List.of(policy(104, "경기도 청년 면접수당")));
        given(policyRepository.findByTitleAndActiveTrue("경기청년 맞춤형 채용지원 서비스"))
                .willReturn(List.of(policy(105, "경기청년 맞춤형 채용지원 서비스")));

        seeder.run(new DefaultApplicationArguments());

        verify(policyBookmarkService).add(1L, 101);
        verify(policyBookmarkService, never()).add(1L, 102);
        verify(policyBookmarkService).add(1L, 103);
        verify(policyBookmarkService).add(1L, 104);
        verify(policyBookmarkService).add(1L, 105);
    }

    @Test
    void runSkipsAllWhenMemberDoesNotExist() {
        given(memberRepository.existsById(1L)).willReturn(false);

        seeder.run(new DefaultApplicationArguments());

        verify(policyRepository, never()).findByTitleAndActiveTrue("K-패스(K패스)");
        verify(policyBookmarkService, never()).add(1L, 101);
    }

    @Test
    void runSkipsDuplicatePolicyTitleWithoutChoosingArbitrarily() {
        given(memberRepository.existsById(1L)).willReturn(true);
        given(policyRepository.findByTitleAndActiveTrue("K-패스(K패스)"))
                .willReturn(List.of(policy(101, "K-패스(K패스)"), policy(201, "K-패스(K패스)")));
        given(policyRepository.findByTitleAndActiveTrue("청년 자산형성 지원(청년도약계좌)"))
                .willReturn(List.of(policy(102, "청년 자산형성 지원(청년도약계좌)")));
        given(policyRepository.findByTitleAndActiveTrue("경기청년 일자리 매치업 플러스"))
                .willReturn(List.of(policy(103, "경기청년 일자리 매치업 플러스")));
        given(policyRepository.findByTitleAndActiveTrue("경기도 청년 면접수당"))
                .willReturn(List.of(policy(104, "경기도 청년 면접수당")));
        given(policyRepository.findByTitleAndActiveTrue("경기청년 맞춤형 채용지원 서비스"))
                .willReturn(List.of(policy(105, "경기청년 맞춤형 채용지원 서비스")));

        seeder.run(new DefaultApplicationArguments());

        verify(policyBookmarkService, never()).add(1L, 101);
        verify(policyBookmarkService, never()).add(1L, 201);
        verify(policyBookmarkService, times(4)).add(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.anyInt());
    }

    private void stubPolicies() {
        given(policyRepository.findByTitleAndActiveTrue("K-패스(K패스)")).willReturn(List.of(policy(101, "K-패스(K패스)")));
        given(policyRepository.findByTitleAndActiveTrue("청년 자산형성 지원(청년도약계좌)"))
                .willReturn(List.of(policy(102, "청년 자산형성 지원(청년도약계좌)")));
        given(policyRepository.findByTitleAndActiveTrue("경기청년 일자리 매치업 플러스"))
                .willReturn(List.of(policy(103, "경기청년 일자리 매치업 플러스")));
        given(policyRepository.findByTitleAndActiveTrue("경기도 청년 면접수당"))
                .willReturn(List.of(policy(104, "경기도 청년 면접수당")));
        given(policyRepository.findByTitleAndActiveTrue("경기청년 맞춤형 채용지원 서비스"))
                .willReturn(List.of(policy(105, "경기청년 맞춤형 채용지원 서비스")));
    }

    private Policy policy(Integer id, String title) {
        Policy policy = new Policy("DEMO-" + id);
        ReflectionTestUtils.setField(policy, "id", id);
        policy.updateBasic(
                title,
                "경기도",
                PolicyCategory.일자리,
                "시연용 정책",
                "https://example.com",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 12, 31),
                false,
                true,
                "신청중"
        );
        return policy;
    }
}
