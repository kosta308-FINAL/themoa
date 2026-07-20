package com.weaone.themoa.domain.merchant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Apple·Google Play처럼 실제 서비스를 가리는 결제대행자. 이름으로는 신원 판별이 불가능해
 * 판정에만 쓰이고, {@code name}(trim+uppercase)이 가맹점 원본명과 일치하면 biller 분기를 탄다
 * (merchant.md §5-D-1). 신원 자체는 사용자별 금액·주기 매칭(fixed_expense)이 담당한다.
 */
@Entity
@Table(name = "biller")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Biller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /** 현재 판정 로직은 name만 비교한다. 패턴 기반 판정으로 확장할 때를 대비한 자리다(erd.md). */
    @Column(name = "match_pattern", length = 255)
    private String matchPattern;

    private Biller(String name, String matchPattern) {
        this.name = name;
        this.matchPattern = matchPattern;
    }

    public static Biller seed(String name) {
        return new Biller(name, null);
    }
}
