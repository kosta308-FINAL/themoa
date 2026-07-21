package com.weaone.themoa.domain.customerservice.repository;

import com.weaone.themoa.domain.customerservice.entity.CustomerInquiry;
import com.weaone.themoa.domain.customerservice.entity.InquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerInquiryRepository extends JpaRepository<CustomerInquiry, Long> {

    /** 본인 소유가 아니거나 없으면 빈 값 — 다른 회원 문의와 같은 404로 응답하기 위해서다(customerservice.md §3-1). */
    Optional<CustomerInquiry> findByIdAndMember_Id(Long id, Long memberId);

    Page<CustomerInquiry> findByMember_IdOrderByCreatedAtDescIdDesc(Long memberId, Pageable pageable);

    /** 관리자 목록(customerservice.md §4-3): PENDING 우선, 그 안에서 created_at ASC, id ASC. */
    @Query("""
            select i from CustomerInquiry i
            where (:status is null or i.status = :status)
              and (:inquiryCategoryId is null or i.inquiryCategory.id = :inquiryCategoryId)
              and (:keyword is null
                   or lower(i.title) like concat('%', :keyword, '%')
                   or lower(i.content) like concat('%', :keyword, '%'))
            order by case when i.status = com.weaone.themoa.domain.customerservice.entity.InquiryStatus.PENDING then 0 else 1 end asc,
                     i.createdAt asc, i.id asc
            """)
    Page<CustomerInquiry> searchForAdmin(@Param("status") InquiryStatus status,
                                          @Param("inquiryCategoryId") Long inquiryCategoryId,
                                          @Param("keyword") String keyword,
                                          Pageable pageable);
}
