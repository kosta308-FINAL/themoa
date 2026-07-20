package com.weaone.themoa.domain.customerservice.repository;

import com.weaone.themoa.domain.customerservice.entity.CustomerInquiryAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerInquiryAnswerRepository extends JpaRepository<CustomerInquiryAnswer, Long> {

    Optional<CustomerInquiryAnswer> findByInquiry_Id(Long inquiryId);
}
