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
 * 회원 개인이 특정 서비스(MerchantAlias)에 붙이는 표시명 오버라이드. 전역 {@code canonical_service_name}보다
 * 항상 우선한다 — 승격으로 전역 이름이 바뀌거나 다른 회원이 뭘로 부르든, 이 회원 화면에서는 이 표시명이
 * 계속 유지된다. 회원별로 alias 하나당 하나만 존재한다.
 */
@Entity
@Table(name = "member_merchant_alias_label",
        uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "merchant_alias_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberMerchantAliasLabel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_alias_id", nullable = false)
    private MerchantAlias merchantAlias;

    @Column(nullable = false, length = 255)
    private String label;

    private MemberMerchantAliasLabel(Member member, MerchantAlias merchantAlias, String label) {
        this.member = member;
        this.merchantAlias = merchantAlias;
        this.label = label;
    }

    public static MemberMerchantAliasLabel create(Member member, MerchantAlias merchantAlias, String label) {
        return new MemberMerchantAliasLabel(member, merchantAlias, label);
    }

    public void updateLabel(String label) {
        this.label = label;
    }
}
