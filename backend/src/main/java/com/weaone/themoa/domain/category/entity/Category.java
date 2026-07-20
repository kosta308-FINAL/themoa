package com.weaone.themoa.domain.category.entity;

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
 * 전역 마스터 카테고리(category.md §1·§3). 사용자별 소유가 없고 관리자 큐레이션으로만 추가·수정된다.
 * {@code code}는 조인·매핑 시드가 참조하는 불변 키, {@code name}은 화면 표시용이라 자유롭게 바꿔도
 * 참조가 깨지지 않는다.
 */
@Entity
@Table(name = "category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    private Category(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static Category seed(CategoryCode code, String name) {
        return new Category(code.name(), name);
    }
}
