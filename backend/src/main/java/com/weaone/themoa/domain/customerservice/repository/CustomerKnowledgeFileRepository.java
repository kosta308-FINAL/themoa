package com.weaone.themoa.domain.customerservice.repository;

import com.weaone.themoa.domain.customerservice.entity.CustomerKnowledgeFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerKnowledgeFileRepository extends JpaRepository<CustomerKnowledgeFile, Long> {

    List<CustomerKnowledgeFile> findAllByOrderByCreatedAtDescIdDesc();

    Optional<CustomerKnowledgeFile> findByIdAndActiveTrue(Long id);
}
