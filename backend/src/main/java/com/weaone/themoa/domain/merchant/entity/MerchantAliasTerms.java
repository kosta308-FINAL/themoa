package com.weaone.themoa.domain.merchant.entity;

import com.weaone.themoa.domain.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * 별칭 표기 사전. {@code member} = {@code null}이면 관리자 전역 시드, 값이 있으면 그 회원의 학습 결과다
 * (merchant.md §1 "학습 결과 저장 범위 = per-user"). 원본 문자열을 그대로 저장하고, 매칭 비교 시에만
 * trim + uppercase를 적용한다(부분일치 아님, 완전일치).
 */
@Entity
@Table(name = "merchant_alias_terms",
        uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "alias_text"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MerchantAliasTerms {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_alias_id", nullable = false)
    private MerchantAlias merchantAlias;

    /** NULL = 관리자 전역 시드. 값 있음 = 그 회원의 학습 결과(본인 매칭에만 쓰인다). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "alias_text", nullable = false, length = 255)
    private String aliasText;

    private MerchantAliasTerms(MerchantAlias merchantAlias, Member member, String aliasText) {
        this.merchantAlias = merchantAlias;
        this.member = member;
        this.aliasText = aliasText;
    }

    /** 관리자 전역 시드(import 대체 seeder) 전용. member는 항상 NULL이다. */
    public static MerchantAliasTerms seed(MerchantAlias merchantAlias, String aliasText) {
        return new MerchantAliasTerms(merchantAlias, null, aliasText);
    }

    /** 사용자 학습 루프 전용. 반드시 그 사용자 소유로 저장된다(merchant.md §3 4단계). */
    public static MerchantAliasTerms learn(MerchantAlias merchantAlias, Member member, String aliasText) {
        return new MerchantAliasTerms(merchantAlias, member, aliasText);
    }
}
