package com.weaone.themoa.domain.merchant.repository;

import com.weaone.themoa.domain.merchant.entity.Biller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillerRepository extends JpaRepository<Biller, Long> {

    /** 가맹점 원본명이 결제대행자인지 판정(trim+uppercase 완전일치, merchant.md §5-D-1). */
    @Query("select case when count(b) > 0 then true else false end from Biller b "
            + "where upper(trim(b.name)) = upper(trim(:name))")
    boolean existsByNameNormalized(@Param("name") String name);
}
