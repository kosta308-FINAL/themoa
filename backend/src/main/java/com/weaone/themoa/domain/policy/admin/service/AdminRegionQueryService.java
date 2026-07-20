package com.weaone.themoa.domain.policy.admin.service;

import com.weaone.themoa.domain.policy.admin.dto.response.AdminExternalCodeResponse;
import com.weaone.themoa.domain.policy.admin.dto.response.AdminRegionCoverageResponse;
import com.weaone.themoa.domain.policy.admin.dto.response.AdminRegionResolveResponse;
import com.weaone.themoa.domain.policy.admin.dto.response.AdminRegionSearchResponse;
import com.weaone.themoa.domain.policy.admin.dto.response.AdminRegionSyncRunResponse;
import com.weaone.themoa.domain.policy.policy.entity.RegionCode;
import com.weaone.themoa.domain.policy.policy.entity.RegionSyncRun;
import com.weaone.themoa.domain.policy.policy.region.RegionCatalog;
import com.weaone.themoa.domain.policy.policy.region.UserRegionResolution;
import com.weaone.themoa.domain.policy.policy.region.UserRegionTextResolver;
import com.weaone.themoa.domain.policy.policy.repository.RegionCodeRepository;
import com.weaone.themoa.domain.policy.policy.repository.RegionExternalCodeRepository;
import com.weaone.themoa.domain.policy.policy.repository.RegionSyncRunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminRegionQueryService {
    private final UserRegionTextResolver userRegionTextResolver;
    private final RegionCatalog regionCatalog;
    private final RegionCodeRepository regionCodeRepository;
    private final RegionExternalCodeRepository externalCodeRepository;
    private final RegionSyncRunRepository syncRunRepository;

    public AdminRegionQueryService(UserRegionTextResolver userRegionTextResolver,
                                   RegionCatalog regionCatalog,
                                   RegionCodeRepository regionCodeRepository,
                                   RegionExternalCodeRepository externalCodeRepository,
                                   RegionSyncRunRepository syncRunRepository) {
        this.userRegionTextResolver = userRegionTextResolver;
        this.regionCatalog = regionCatalog;
        this.regionCodeRepository = regionCodeRepository;
        this.externalCodeRepository = externalCodeRepository;
        this.syncRunRepository = syncRunRepository;
    }

    @Transactional(readOnly = true)
    public AdminRegionResolveResponse resolveRegion(String query) {
        UserRegionResolution result = userRegionTextResolver.resolve(query);
        List<AdminExternalCodeResponse> externalCodes = List.of();
        Integer regionId = null;
        if (result.regionCode() != null) {
            RegionCode region = regionCodeRepository.findByRegionCode(result.regionCode()).orElse(null);
            if (region != null) {
                regionId = region.getId();
                externalCodes = externalCodes(region.getId());
            }
        }
        return new AdminRegionResolveResponse(
                query,
                result.status(),
                result.regionLevel(),
                result.province(),
                result.city(),
                result.regionName(),
                result.matchedText(),
                result.matchType(),
                regionId,
                result.regionCode(),
                result.regionName(),
                externalCodes,
                result.candidateDetails()
        );
    }

    @Transactional(readOnly = true)
    public List<AdminRegionSearchResponse> searchRegions(String name) {
        return regionCatalog.findInText(name).stream()
                .map(region -> new AdminRegionSearchResponse(
                        region.getId(),
                        region.getRegionCode(),
                        region.getProvince(),
                        region.getCity(),
                        region.getRegionLevel(),
                        externalCodes(region.getId())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminRegionCoverageResponse regionCoverage() {
        return new AdminRegionCoverageResponse(
                regionCodeRepository.countByRegionLevel("PROVINCE"),
                regionCodeRepository.countByRegionLevel("CITY"),
                regionCodeRepository.countByRegionLevel("CITY"),
                externalCodeRepository.countByCodeSystemAndRegionLevel("SGIS", "PROVINCE"),
                externalCodeRepository.countByCodeSystemAndRegionLevel("SGIS", "CITY"),
                externalCodeRepository.countByCodeSystem("SGIS"),
                regionCodeRepository.countLegacyRegions(),
                regionCodeRepository.countByRegionCodeStartingWith("P:")
                        + regionCodeRepository.countByRegionCodeStartingWith("M:")
        );
    }

    @Transactional(readOnly = true)
    public AdminRegionSyncRunResponse latestRegionSyncRun() {
        return syncRunRepository.findTopByOrderByStartedAtDesc()
                .map(this::toResponse)
                .orElse(null);
    }

    public void refreshRegionCache() {
        regionCatalog.refreshCache();
    }

    private List<AdminExternalCodeResponse> externalCodes(Integer regionId) {
        return externalCodeRepository.findByRegionId(regionId).stream()
                .map(code -> new AdminExternalCodeResponse(code.getCodeSystem(), code.getExternalCode()))
                .toList();
    }

    private AdminRegionSyncRunResponse toResponse(RegionSyncRun run) {
        return new AdminRegionSyncRunResponse(
                run.getId(),
                run.getStatus(),
                run.getStartedAt(),
                run.getCompletedAt(),
                run.getApiProvinceCount(),
                run.getApiMunicipalityCount(),
                run.getInsertedCount(),
                run.getUpdatedCount(),
                run.getUnchangedCount(),
                run.getFailedCount(),
                run.getProgressPercent(),
                run.getCurrentProvinceCode(),
                run.getCurrentProvinceName(),
                run.getErrorSummary()
        );
    }
}
