package com.weaone.themoa.domain.merchant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제내역에 실제 등장한 원본 가맹점명 1건당 1행. 전역 테이블(member_id 없음)이라
 * {@link #merchantAlias}는 관리자 시드(전역 표기사전) 매칭 결과만 담는다.
 * 사용자 학습으로는 절대 갱신하지 않는다 — 그 결과는 카드거래 쪽에 저장한다(merchant.md §2-1).
 */
@Entity
@Table(name = "merchant")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 전역 시드(관리자) 표기사전 매칭 결과만 여기 담긴다. 사용자 학습 결과는 담지 않는다(merchant.md §2-1). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_alias_id")
    private MerchantAlias merchantAlias;

    /** 카드사가 준 원본 가맹점명 그대로. merchant는 이 원본명 단위로 생성된다(정규화 컬럼 없음). */
    @Column(name = "merchant_name_raw", nullable = false, unique = true, length = 255)
    private String merchantNameRaw;

    @Column(name = "display_name", length = 255)
    private String displayName;

    private Merchant(String merchantNameRaw, MerchantAlias merchantAlias) {
        this.merchantNameRaw = merchantNameRaw;
        this.merchantAlias = merchantAlias;
    }

    /** 새 원본 가맹점명 관찰. 전역 시드 표기사전에 이미 걸리면 {@code globalAlias}로 즉시 연결한다. */
    public static Merchant observe(String merchantNameRaw, MerchantAlias globalAlias) {
        return new Merchant(merchantNameRaw, globalAlias);
    }
}
