package com.weaone.themoa.domain.cardconnection.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지원 카드사 화이트리스트(마스터 시드). PK가 CODEF 기관코드(자연키)라 대리키·AUTO_INCREMENT를 두지 않는다.
 * 광주·수협·제주(인증서 전용)·씨티(SMS 2-way 추가인증)는 시드에 없으므로 연결 시도가 FK 조회에서 자연히 막힌다(connection.md §2-1).
 */
@Entity
@Table(name = "card_issuer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardIssuer {

    @Id
    @Column(name = "organization", length = 10)
    private String organization;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "fx_type", nullable = false, length = 10)
    private CodefValueType fxType;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancel_type", nullable = false, length = 10)
    private CodefValueType cancelType;

    @Column(name = "cancel_amount_uncertain", nullable = false)
    private boolean cancelAmountUncertain;

    private CardIssuer(String organization, String name, CodefValueType fxType,
                        CodefValueType cancelType, boolean cancelAmountUncertain) {
        this.organization = organization;
        this.name = name;
        this.fxType = fxType;
        this.cancelType = cancelType;
        this.cancelAmountUncertain = cancelAmountUncertain;
    }

    public static CardIssuer seed(String organization, String name, CodefValueType fxType,
                                   CodefValueType cancelType, boolean cancelAmountUncertain) {
        return new CardIssuer(organization, name, fxType, cancelType, cancelAmountUncertain);
    }
}
