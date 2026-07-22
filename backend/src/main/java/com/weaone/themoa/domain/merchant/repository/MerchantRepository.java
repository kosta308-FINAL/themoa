package com.weaone.themoa.domain.merchant.repository;

import com.weaone.themoa.domain.merchant.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {

    Optional<Merchant> findByMerchantNameRaw(String merchantNameRaw);
}
