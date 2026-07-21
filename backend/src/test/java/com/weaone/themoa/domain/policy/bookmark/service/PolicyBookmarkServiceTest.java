package com.weaone.themoa.domain.policy.bookmark.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.member.entity.Gender;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.policy.bookmark.dto.response.PolicyBookmarkListResponse;
import com.weaone.themoa.domain.policy.bookmark.dto.response.PolicyBookmarkResponse;
import com.weaone.themoa.domain.policy.bookmark.entity.PolicyApplyStatus;
import com.weaone.themoa.domain.policy.bookmark.entity.PolicyBookmark;
import com.weaone.themoa.domain.policy.bookmark.repository.PolicyBookmarkRepository;
import com.weaone.themoa.domain.policy.policy.entity.Policy;
import com.weaone.themoa.domain.policy.policy.entity.PolicyCategory;
import com.weaone.themoa.domain.policy.policy.repository.PolicyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PolicyBookmarkServiceTest {
    private final PolicyBookmarkRepository bookmarkRepository = mock(PolicyBookmarkRepository.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final PolicyRepository policyRepository = mock(PolicyRepository.class);
    private final PolicyBookmarkService service =
            new PolicyBookmarkService(bookmarkRepository, memberRepository, policyRepository);

    @Test
    void addCreatesInterestedBookmark() {
        Member member = member(7L);
        Policy policy = policy(12, "경기도 청년 취업지원금");
        given(bookmarkRepository.findByMember_IdAndPolicy_Id(7L, 12)).willReturn(Optional.empty());
        given(memberRepository.findById(7L)).willReturn(Optional.of(member));
        given(policyRepository.findById(12)).willReturn(Optional.of(policy));
        given(bookmarkRepository.save(any(PolicyBookmark.class))).willAnswer(invocation -> {
            PolicyBookmark bookmark = invocation.getArgument(0);
            ReflectionTestUtils.setField(bookmark, "id", 3);
            return bookmark;
        });

        PolicyBookmarkResponse response = service.add(7L, 12);

        assertThat(response.bookmarkId()).isEqualTo(3);
        assertThat(response.policyId()).isEqualTo(12);
        assertThat(response.applyStatus()).isEqualTo("INTERESTED");
        assertThat(response.notificationEnabled()).isFalse();
        assertThat(response.note()).isNull();
        verify(bookmarkRepository).save(any(PolicyBookmark.class));
    }

    @Test
    void addReturnsExistingBookmarkWithoutLoadingMemberOrPolicy() {
        PolicyBookmark bookmark = bookmark(3, member(7L), policy(12, "기존 정책"));
        given(bookmarkRepository.findByMember_IdAndPolicy_Id(7L, 12)).willReturn(Optional.of(bookmark));

        PolicyBookmarkResponse response = service.add(7L, 12);

        assertThat(response.bookmarkId()).isEqualTo(3);
        assertThat(response.policyId()).isEqualTo(12);
        verify(memberRepository, never()).findById(7L);
        verify(policyRepository, never()).findById(12);
        verify(bookmarkRepository, never()).save(any());
    }

    @Test
    void addThrowsMemberNotFound() {
        given(bookmarkRepository.findByMember_IdAndPolicy_Id(7L, 12)).willReturn(Optional.empty());
        given(memberRepository.findById(7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.add(7L, 12))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Test
    void addThrowsPolicyNotFound() {
        given(bookmarkRepository.findByMember_IdAndPolicy_Id(7L, 12)).willReturn(Optional.empty());
        given(memberRepository.findById(7L)).willReturn(Optional.of(member(7L)));
        given(policyRepository.findById(12)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.add(7L, 12))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.POLICY_NOT_FOUND));
    }

    @Test
    void listKeepsRepositoryOrderAndMapsFields() {
        Member member = member(7L);
        PolicyBookmark first = bookmark(5, member, policy(20, "첫 번째"));
        PolicyBookmark second = bookmark(4, member, policy(12, "두 번째"));
        given(bookmarkRepository.findByMember_IdOrderByIdDesc(7L)).willReturn(List.of(first, second));

        PolicyBookmarkListResponse response = service.list(7L);

        assertThat(response.items()).extracting(PolicyBookmarkResponse::bookmarkId)
                .containsExactly(5, 4);
        assertThat(response.items().get(0).title()).isEqualTo("첫 번째");
        assertThat(response.items().get(0).category()).isEqualTo("일자리");
        assertThat(response.items().get(0).agencyName()).isEqualTo("경기도");
        assertThat(response.items().get(0).summary()).isEqualTo("미취업 청년을 위한 지원 정책");
        assertThat(response.items().get(0).officialUrl()).isEqualTo("https://example.com");
        assertThat(response.items().get(0).startDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(response.items().get(0).dueDate()).isEqualTo(LocalDate.of(2026, 7, 31));
        assertThat(response.items().get(0).alwaysOpen()).isFalse();
        assertThat(response.items().get(0).active()).isTrue();
        assertThat(response.items().get(0).policyStatus()).isEqualTo("신청중");
    }

    @Test
    void removeDeletesByMemberAndPolicyAndIgnoresMissingBookmark() {
        when(bookmarkRepository.deleteByMember_IdAndPolicy_Id(7L, 12)).thenReturn(0L);

        service.remove(7L, 12);

        verify(bookmarkRepository).deleteByMember_IdAndPolicy_Id(7L, 12);
    }

    private Member member(Long id) {
        Member member = Member.signUp(
                "user" + id + "@example.com",
                "password",
                "회원",
                Gender.MALE,
                LocalDate.of(1998, 1, 1),
                LocalDateTime.of(2026, 7, 21, 0, 0)
        );
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private Policy policy(Integer id, String title) {
        Policy policy = new Policy("YC-" + id);
        ReflectionTestUtils.setField(policy, "id", id);
        policy.updateBasic(
                title,
                "경기도",
                PolicyCategory.일자리,
                "미취업 청년을 위한 지원 정책",
                "https://example.com",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                false,
                true,
                "신청중"
        );
        return policy;
    }

    private PolicyBookmark bookmark(Integer id, Member member, Policy policy) {
        PolicyBookmark bookmark = PolicyBookmark.interest(member, policy);
        ReflectionTestUtils.setField(bookmark, "id", id);
        ReflectionTestUtils.setField(bookmark, "applyStatus", PolicyApplyStatus.INTERESTED);
        return bookmark;
    }
}
