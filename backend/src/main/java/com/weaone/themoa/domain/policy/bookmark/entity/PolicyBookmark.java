package com.weaone.themoa.domain.policy.bookmark.entity;

import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.policy.policy.entity.Policy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "policy_bookmark",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_policy_bookmark_member_policy",
                        columnNames = {"member_id", "policy_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PolicyBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Enumerated(EnumType.STRING)
    @Column(name = "apply_status", nullable = false, length = 15)
    private PolicyApplyStatus applyStatus;

    @Column(name = "notification_enabled", nullable = false)
    private boolean notificationEnabled;

    @Column(length = 300)
    private String note;

    public static PolicyBookmark interest(Member member, Policy policy) {
        PolicyBookmark bookmark = new PolicyBookmark();
        bookmark.member = member;
        bookmark.policy = policy;
        bookmark.applyStatus = PolicyApplyStatus.INTERESTED;
        bookmark.notificationEnabled = false;
        bookmark.note = null;
        return bookmark;
    }
}
