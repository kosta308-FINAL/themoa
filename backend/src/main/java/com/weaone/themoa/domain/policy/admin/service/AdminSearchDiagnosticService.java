package com.weaone.themoa.domain.policy.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.policy.admin.dto.request.AdminSearchExplainRequest;
import com.weaone.themoa.domain.policy.admin.dto.response.AdminRegionSearchQualityCaseResponse;
import com.weaone.themoa.domain.policy.admin.dto.response.AdminRegionSearchQualitySuiteResponse;
import com.weaone.themoa.domain.policy.admin.dto.response.AdminResolvedRegionResponse;
import com.weaone.themoa.domain.policy.admin.dto.response.AdminSearchExplainResponse;
import com.weaone.themoa.domain.policy.admin.dto.response.AdminSearchQualityCaseResponse;
import com.weaone.themoa.domain.policy.admin.dto.response.AdminSearchQualitySuiteResponse;
import com.weaone.themoa.domain.policy.policy.entity.RegionCode;
import com.weaone.themoa.domain.policy.policy.region.RegionEligiblePolicyCandidate;
import com.weaone.themoa.domain.policy.policy.region.ResolvedUserRegion;
import com.weaone.themoa.domain.policy.policy.region.SearchRegionLevel;
import com.weaone.themoa.domain.policy.policy.repository.RegionCodeRepository;
import com.weaone.themoa.domain.policy.policy.service.RegionEligiblePolicyCandidateService;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchRequest;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchResponse;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchResultItem;
import com.weaone.themoa.domain.policy.rag.service.PolicyRagSearchService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminSearchDiagnosticService {
    private final PolicyRagSearchService policyRagSearchService;
    private final RegionCodeRepository regionCodeRepository;
    private final RegionEligiblePolicyCandidateService regionEligiblePolicyCandidateService;
    private final ObjectMapper objectMapper;

    public AdminSearchDiagnosticService(PolicyRagSearchService policyRagSearchService,
                                        RegionCodeRepository regionCodeRepository,
                                        RegionEligiblePolicyCandidateService regionEligiblePolicyCandidateService,
                                        ObjectMapper objectMapper) {
        this.policyRagSearchService = policyRagSearchService;
        this.regionCodeRepository = regionCodeRepository;
        this.regionEligiblePolicyCandidateService = regionEligiblePolicyCandidateService;
        this.objectMapper = objectMapper;
    }

    public AdminSearchExplainResponse explainSearch(AdminSearchExplainRequest request) {
        if (request == null || !StringUtils.hasText(request.query())) {
            throw new BusinessException(ErrorCode.POLICY_DIAGNOSTIC_DATA_INVALID);
        }
        if (request.policyId() == null && !StringUtils.hasText(request.sourcePolicyId())) {
            throw new BusinessException(ErrorCode.POLICY_DIAGNOSTIC_DATA_INVALID);
        }
        return new AdminSearchExplainResponse(
                policyRagSearchService.explain(request.query(), request.policyId(), request.sourcePolicyId())
        );
    }

    public AdminSearchQualitySuiteResponse searchQualitySuite() {
        List<String> queries = List.of(
                "K패스", "K-패스", "k 패스", "청년", "수원 사는 27살 청년 정책",
                "수원 사는 27살 취준생 정책", "수원 청년 계좌", "청년 면접수당");
        List<AdminSearchQualityCaseResponse> cases = new ArrayList<>();
        int passed = 0;
        for (String query : queries) {
            PolicySearchResponse response = policyRagSearchService.search(new PolicySearchRequest(query, null, 0, 20));
            List<String> titles = response.results().stream()
                    .map(PolicySearchResultItem::title)
                    .toList();
            boolean pass = searchQualityPassed(query, response, titles);
            if (response.interpretedCondition().regionExplicit()) {
                pass = pass && primaryRegionsAllowed(response);
            }
            if (pass) {
                passed++;
            }
            cases.add(new AdminSearchQualityCaseResponse(
                    query,
                    pass,
                    response.queryType(),
                    response.totalMatched(),
                    response.hasNext(),
                    regionViolations(response),
                    titles.stream().limit(10).toList()
            ));
        }
        AdminSearchQualitySuiteResponse report = new AdminSearchQualitySuiteResponse(
                Instant.now().toString(),
                passed,
                queries.size(),
                passed == queries.size(),
                cases
        );
        writeQualityReports(report);
        return report;
    }

    public AdminRegionSearchQualitySuiteResponse regionSearchQualitySuite() {
        List<AdminRegionSearchQualityCaseResponse> cases = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        int passed = 0;
        for (RegionCode region : regionCodeRepository.findActiveDirectSigunguRegions()) {
            String alias = shortRegionAlias(region.getCity());
            String query = alias + " 사는 30살 청년 정책";
            ResolvedUserRegion resolved = new ResolvedUserRegion(
                    region.getProvince(), region.getCity(), null, SearchRegionLevel.SIGUNGU, region);
            List<RegionEligiblePolicyCandidate> candidates = regionEligiblePolicyCandidateService.findSearchEligibleCandidates(resolved);
            Map<String, Long> counts = candidates.stream()
                    .collect(Collectors.groupingBy(
                            candidate -> candidate.compatibility().name(),
                            LinkedHashMap::new,
                            Collectors.counting()));
            List<String> violations = candidates.stream()
                    .map(candidate -> candidate.compatibility().name())
                    .filter(status -> "NOT_MATCHED".equals(status) || "UNKNOWN".equals(status))
                    .toList();
            long distinctCount = candidates.stream()
                    .map(RegionEligiblePolicyCandidate::policyId)
                    .distinct()
                    .count();
            boolean pass = !candidates.isEmpty()
                    && violations.isEmpty()
                    && counts.getOrDefault("EXACT_SIGUNGU", 0L)
                    + counts.getOrDefault("MULTIPLE_SIGUNGU_MATCH", 0L)
                    + counts.getOrDefault("MULTIPLE_REGION_MATCH", 0L) > 0
                    && counts.getOrDefault("NATIONWIDE", 0L) > 0
                    && distinctCount == candidates.size();
            if (pass) {
                passed++;
            } else {
                failures.add(region.displayName());
            }
            cases.add(new AdminRegionSearchQualityCaseResponse(
                    query,
                    new AdminResolvedRegionResponse(resolved.province(), resolved.city(), resolved.level().name(), true),
                    candidates.size(),
                    counts.getOrDefault("EXACT_SIGUNGU", 0L),
                    counts.getOrDefault("PARENT_SIDO", 0L),
                    counts.getOrDefault("NATIONWIDE", 0L),
                    counts.getOrDefault("MULTIPLE_SIGUNGU_MATCH", 0L)
                            + counts.getOrDefault("MULTIPLE_SIDO_MATCH", 0L)
                            + counts.getOrDefault("MULTIPLE_REGION_MATCH", 0L),
                    0,
                    0,
                    candidates.stream().limit(20).map(candidate -> candidate.compatibility().name()).toList(),
                    violations,
                    pass
            ));
        }
        AdminRegionSearchQualitySuiteResponse report = new AdminRegionSearchQualitySuiteResponse(
                Instant.now().toString(),
                passed,
                cases.size(),
                passed == cases.size(),
                failures,
                cases
        );
        writeRegionQualityReports(report);
        return report;
    }

    private boolean searchQualityPassed(String query, PolicySearchResponse response, List<String> titles) {
        return switch (query) {
            case "K패스", "K-패스", "k 패스" -> !response.results().isEmpty()
                    && response.results().get(0).title().replaceAll("[\\s\\-]", "").contains("K패스");
            case "청년" -> response.totalMatched() > response.size() && response.hasNext()
                    && primaryRegionsAllowed(response);
            case "수원 사는 27살 청년 정책" -> response.totalMatched() > 1
                    && primaryRegionsAllowed(response);
            case "수원 사는 27살 취준생 정책" -> titles.stream().limit(10).anyMatch(title -> title.contains("면접"));
            case "수원 청년 계좌" -> titles.stream().limit(10).anyMatch(title -> title.contains("계좌") || title.contains("통장") || title.contains("저축"))
                    && primaryRegionsAllowed(response);
            case "청년 면접수당" -> titles.stream().limit(10).anyMatch(title -> title.contains("면접"));
            default -> false;
        };
    }

    private boolean primaryRegionsAllowed(PolicySearchResponse response) {
        return regionViolations(response).isEmpty();
    }

    private List<String> regionViolations(PolicySearchResponse response) {
        if (response == null || response.interpretedCondition() == null || !response.interpretedCondition().regionExplicit()) {
            return List.of();
        }
        return response.results().stream()
                .map(PolicySearchResultItem::regionCompatibility)
                .filter(status -> "NOT_MATCHED".equals(status) || "UNKNOWN".equals(status))
                .toList();
    }

    private String shortRegionAlias(String city) {
        if (!StringUtils.hasText(city)) {
            return "";
        }
        String value = city.contains(" ") ? city.substring(0, city.indexOf(' ')) : city;
        for (String suffix : List.of("특별자치시", "특별자치도", "특별시", "광역시", "시", "군", "구", "도")) {
            if (value.endsWith(suffix) && value.length() > suffix.length()) {
                return value.substring(0, value.length() - suffix.length());
            }
        }
        return value;
    }

    private void writeRegionQualityReports(AdminRegionSearchQualitySuiteResponse report) {
        try {
            Path dir = Path.of("build", "reports");
            Files.createDirectories(dir);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(dir.resolve("region-search-quality.json").toFile(), report);
            StringBuilder md = new StringBuilder();
            md.append("# Region Search Quality Report\n\n");
            md.append("- generatedAt: ").append(report.generatedAt()).append('\n');
            md.append("- passed: ").append(report.passed()).append('/').append(report.total()).append('\n');
            md.append("- success: ").append(report.success()).append('\n');
            md.append("- failedRegions: ").append(report.failedRegions()).append("\n\n");
            for (AdminRegionSearchQualityCaseResponse item : report.cases()) {
                md.append("## ").append(item.query()).append('\n');
                md.append("- passed: ").append(item.passed()).append('\n');
                md.append("- resolvedRegion: ").append(item.resolvedRegion()).append('\n');
                md.append("- eligiblePoolCount: ").append(item.eligiblePoolCount()).append('\n');
                md.append("- exactCount: ").append(item.exactCount()).append('\n');
                md.append("- parentCount: ").append(item.parentCount()).append('\n');
                md.append("- nationwideCount: ").append(item.nationwideCount()).append('\n');
                md.append("- unknownExcludedCount: ").append(item.unknownExcludedCount()).append('\n');
                md.append("- wrongRegionExcludedCount: ").append(item.wrongRegionExcludedCount()).append('\n');
                md.append("- top20 region compatibility: ").append(item.top20RegionCompatibility()).append('\n');
                md.append("- violations: ").append(item.violations()).append("\n\n");
            }
            Files.writeString(dir.resolve("region-search-quality.md"), md.toString());
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.POLICY_DIAGNOSTIC_REPORT_WRITE_FAILED);
        }
    }

    private void writeQualityReports(AdminSearchQualitySuiteResponse report) {
        try {
            Path dir = Path.of("build", "reports");
            Files.createDirectories(dir);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(dir.resolve("search-quality-report.json").toFile(), report);
            StringBuilder md = new StringBuilder();
            md.append("# Search Quality Report\n\n");
            md.append("- generatedAt: ").append(report.generatedAt()).append('\n');
            md.append("- passed: ").append(report.passed()).append('/').append(report.total()).append('\n');
            md.append("- success: ").append(report.success()).append("\n\n");
            for (AdminSearchQualityCaseResponse item : report.cases()) {
                md.append("## ").append(item.query()).append('\n');
                md.append("- passed: ").append(item.passed()).append('\n');
                md.append("- queryType: ").append(item.queryType()).append('\n');
                md.append("- totalMatched: ").append(item.totalMatched()).append('\n');
                md.append("- hasNext: ").append(item.hasNext()).append('\n');
                md.append("- topTitles: ").append(item.topTitles()).append("\n\n");
            }
            Files.writeString(dir.resolve("search-quality-report.md"), md.toString());
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.POLICY_DIAGNOSTIC_REPORT_WRITE_FAILED);
        }
    }
}
