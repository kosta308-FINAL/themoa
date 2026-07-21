package com.weaone.themoa.domain.customerservice.repository;

import com.weaone.themoa.domain.customerservice.entity.CustomerServiceRagSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerServiceRagSettingRepository extends JpaRepository<CustomerServiceRagSetting, Long> {

    Optional<CustomerServiceRagSetting> findTopByOrderByIdAsc();
}
