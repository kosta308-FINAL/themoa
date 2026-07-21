package com.weaone.themoa.domain.customerservice.repository;

import com.weaone.themoa.domain.customerservice.entity.CustomerInquiryAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.List;

public interface CustomerInquiryAnswerRepository extends JpaRepository<CustomerInquiryAnswer, Long> {

    Optional<CustomerInquiryAnswer> findByInquiry_Id(Long inquiryId);

    @Query("""
            select a from CustomerInquiryAnswer a
            join fetch a.inquiry i
            join fetch i.inquiryCategory c
            where i.status = com.weaone.themoa.domain.customerservice.entity.InquiryStatus.ANSWERED
            order by a.updatedAt desc, a.createdAt desc, a.id desc
            """)
    List<CustomerInquiryAnswer> findAnsweredKnowledgeSources();
}
