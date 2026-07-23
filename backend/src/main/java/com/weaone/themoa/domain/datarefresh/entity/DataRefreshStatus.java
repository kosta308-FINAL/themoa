package com.weaone.themoa.domain.datarefresh.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "data_refresh_status")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DataRefreshStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 30)
    private DataRefreshSource source;

    @Column(name = "last_success_at", nullable = false)
    private LocalDateTime lastSuccessAt;

    private DataRefreshStatus(DataRefreshSource source, LocalDateTime lastSuccessAt) {
        this.source = source;
        this.lastSuccessAt = lastSuccessAt;
    }

    public static DataRefreshStatus create(DataRefreshSource source, LocalDateTime lastSuccessAt) {
        return new DataRefreshStatus(source, lastSuccessAt);
    }

    public void recordSuccess(LocalDateTime completedAt) {
        this.lastSuccessAt = completedAt;
    }
}
