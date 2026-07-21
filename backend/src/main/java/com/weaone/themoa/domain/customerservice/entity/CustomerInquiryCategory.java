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
 * 1:1 문의 카테고리 마스터(erd.md §8). 별도 코드 없이 PK {@code id}로만 구분하며, 클라이언트는 활성 목록을
 * 조회한 뒤 선택한 id를 문의 등록에 그대로 보낸다. 비활성화해도 기존 문의 조회·관리에는 계속 쓰인다.
 */
@Entity
@Table(name = "customer_inquiry_category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerInquiryCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    private CustomerInquiryCategory(String name, int displayOrder, LocalDateTime now) {
        this.name = name;
        this.displayOrder = displayOrder;
        this.active = true;
        this.createdAt = now;
    }

    public static CustomerInquiryCategory seed(String name, int displayOrder, LocalDateTime now) {
        return new CustomerInquiryCategory(name, displayOrder, now);
    }
}
