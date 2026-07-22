package com.weaone.themoa.domain.logging.repository;

import com.weaone.themoa.domain.logging.entity.AiLogDiagnosis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiLogDiagnosisRepository extends JpaRepository<AiLogDiagnosis, Long> {

    Optional<AiLogDiagnosis> findByErrorLog_Id(Long errorLogId);
}
