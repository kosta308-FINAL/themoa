package com.weaone.themoa.domain.auth.entity;

import com.weaone.themoa.domain.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 약관 동의 이력(erd.md §1). 행이 존재한다는 사실 자체가 동의의 증빙이라 {@code agreed BOOLEAN} 플래그는
 * 두지 않는다({@code customer_inquiry.privacy_agreed_at}과 같은 원칙). 약관 개정으로 재동의를 받으면
 * 기존 행을 UPDATE하지 않고 새 행을 추가한다.
 */
@Entity
@Table(name = "member_terms_agreement",
        indexes = @Index(name = "idx_member_terms_agreement_lookup", columnList = "member_id, terms_type, agreed_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberTermsAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "terms_type", nullable = false, length = 20)
    private TermsType termsType;

    @Column(name = "terms_version", nullable = false, length = 20)
    private String termsVersion;

    @Column(name = "agreed_at", nullable = false)
    private LocalDateTime agreedAt;

    private MemberTermsAgreement(Member member, TermsType termsType, String termsVersion, LocalDateTime agreedAt) {
        this.member = member;
        this.termsType = termsType;
        this.termsVersion = termsVersion;
        this.agreedAt = agreedAt;
    }

    public static MemberTermsAgreement agree(Member member, TermsType termsType, String termsVersion,
                                              LocalDateTime now) {
        return new MemberTermsAgreement(member, termsType, termsVersion, now);
    }
}
