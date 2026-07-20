package com.weaone.themoa.domain.customerservice.repository;

import com.weaone.themoa.domain.customerservice.entity.CustomerInquiryAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerInquiryAttachmentRepository extends JpaRepository<CustomerInquiryAttachment, Long> {

    List<CustomerInquiryAttachment> findByInquiry_IdOrderById(Long inquiryId);

    Optional<CustomerInquiryAttachment> findByIdAndInquiry_Id(Long id, Long inquiryId);
}
