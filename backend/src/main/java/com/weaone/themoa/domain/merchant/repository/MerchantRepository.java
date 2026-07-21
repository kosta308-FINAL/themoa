package com.weaone.themoa.domain.merchant.repository;

import com.weaone.themoa.domain.merchant.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {

    Optional<Merchant> findByMerchantNameRaw(String merchantNameRaw);

    /** 관리자 서비스 병합 대상 조회. */
    List<Merchant> findByMerchantAlias_Id(Long merchantAliasId);
}
