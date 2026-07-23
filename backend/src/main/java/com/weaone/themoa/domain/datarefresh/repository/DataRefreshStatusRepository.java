package com.weaone.themoa.domain.datarefresh.repository;

import com.weaone.themoa.domain.datarefresh.entity.DataRefreshSource;
import com.weaone.themoa.domain.datarefresh.entity.DataRefreshStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DataRefreshStatusRepository extends JpaRepository<DataRefreshStatus, Long> {

    Optional<DataRefreshStatus> findBySource(DataRefreshSource source);
}
