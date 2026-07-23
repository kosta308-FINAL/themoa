package com.weaone.themoa.domain.datarefresh.service;

import com.weaone.themoa.domain.datarefresh.entity.DataRefreshSource;
import com.weaone.themoa.domain.datarefresh.entity.DataRefreshStatus;
import com.weaone.themoa.domain.datarefresh.repository.DataRefreshStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class DataRefreshStatusService {
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final DataRefreshStatusRepository dataRefreshStatusRepository;

    @Transactional
    public void recordSuccess(DataRefreshSource source, LocalDateTime completedAt) {
        DataRefreshStatus status = dataRefreshStatusRepository.findBySource(source)
                .orElseGet(() -> dataRefreshStatusRepository.save(DataRefreshStatus.create(source, completedAt)));
        status.recordSuccess(completedAt);
    }

    @Transactional(readOnly = true)
    public boolean wasSuccessfulOn(DataRefreshSource source, LocalDate date) {
        return dataRefreshStatusRepository.findBySource(source)
                .map(DataRefreshStatus::getLastSuccessAt)
                .map(lastSuccessAt -> lastSuccessAt.atZone(SEOUL_ZONE).toLocalDate())
                .filter(date::equals)
                .isPresent();
    }
}
