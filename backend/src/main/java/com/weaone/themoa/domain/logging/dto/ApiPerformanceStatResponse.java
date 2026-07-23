package com.weaone.themoa.domain.logging.dto;

public record ApiPerformanceStatResponse(
        String method,
        String uri,
        String status,
        long count,
        double avgMs,
        double maxMs) {}
