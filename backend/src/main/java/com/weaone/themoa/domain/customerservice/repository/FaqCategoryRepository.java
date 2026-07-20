package com.weaone.themoa.domain.customerservice.repository;

import com.weaone.themoa.domain.customerservice.entity.FaqCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FaqCategoryRepository extends JpaRepository<FaqCategory, Long> {

    Optional<FaqCategory> findByCodeAndActiveTrue(String code);
}
