package com.weaone.themoa.domain.customerservice.repository;

import com.weaone.themoa.domain.customerservice.entity.CustomerKnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerKnowledgeChunkRepository extends JpaRepository<CustomerKnowledgeChunk, Long> {

    List<CustomerKnowledgeChunk> findByKnowledgeFile_ActiveTrueOrderByKnowledgeFile_IdAscChunkIndexAsc();

    List<CustomerKnowledgeChunk> findByKnowledgeFile_IdOrderByChunkIndexAsc(Long knowledgeFileId);

    void deleteByKnowledgeFile_Id(Long knowledgeFileId);
}
