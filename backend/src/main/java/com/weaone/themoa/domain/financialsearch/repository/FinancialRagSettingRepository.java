package com.weaone.themoa.domain.financialsearch.repository;

import com.weaone.themoa.domain.financialsearch.entity.FinancialRagSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FinancialRagSettingRepository extends JpaRepository<FinancialRagSetting, Long> {

    /** 설정은 한 행만 유지한다. 행이 없으면 application.yaml 기본값을 쓴다. */
    Optional<FinancialRagSetting> findTopByOrderByIdAsc();
}
