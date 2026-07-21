package com.weaone.themoa.domain.customerservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * FAQ 콘텐츠 탐색 전용 분류(erd.md §8). 소비 분류용 {@code category}, 문의 처리용
 * {@code customer_inquiry_category}와는 목적·수명주기가 달라 합치지 않는다.
 */
@Entity
@Table(name = "faq_category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FaqCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private FaqCategory(String code, String name, int displayOrder, LocalDateTime now) {
        this.code = code;
        this.name = name;
        this.displayOrder = displayOrder;
        this.active = true;
        this.createdAt = now;
    }

    public static FaqCategory seed(String code, String name, int displayOrder, LocalDateTime now) {
        return new FaqCategory(code, name, displayOrder, now);
    }
}
