package com.weaone.themoa.domain.customerservice.repository;

import com.weaone.themoa.domain.customerservice.entity.CustomerInquiryCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerInquiryCategoryRepository extends JpaRepository<CustomerInquiryCategory, Long> {

    List<CustomerInquiryCategory> findByActiveTrueOrderByDisplayOrderAscIdAsc();

    Optional<CustomerInquiryCategory> findByIdAndActiveTrue(Long id);
}
