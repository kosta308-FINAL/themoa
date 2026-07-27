package com.weaone.themoa.domain.policy.policy.support;

import com.weaone.themoa.domain.policy.policy.entity.PolicyCategory;
import com.weaone.themoa.domain.policy.policy.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 정책 동기화(청년센터 API 수집·임베딩)가 끝나기 전에도 정책 상세·북마크 화면을 시연할 수 있도록,
 * 운영 DB에서 확인한 정책 5건을 같은 id로 미리 채워 넣는 로컬 데모 시드.
 *
 * <p>id가 IDENTITY 컬럼이라 JPA save로는 특정 id를 지정할 수 없어 JdbcTemplate로 직접 insert한다.
 *
 * <p><b>주의(동기화 충돌):</b> 여기서 쓰는 source_policy_id("DEMO-SEED-*")는 실제 청년센터 plcyNo가
 * 아닌 자리표시자다. {@link com.weaone.themoa.domain.policy.policy.service.PolicyPersistenceService}는
 * (source_type, source_policy_id)로 upsert하므로, 나중에 관리자 화면에서 "정책 API 수집 실행"/"지금
 * 동기화"를 돌리면 이 시드 row와 매칭되지 않고 같은 제목의 정책이 실제 plcyNo로 새로 insert된다 —
 * 즉 같은 제목의 정책이 두 건(이 시드 row + 동기화로 들어온 실제 row) 존재하게 된다. member_id=1
 * 북마크는 이 시드 row(id=50 등)를 계속 가리키므로 자동으로 실제 row로 옮겨가지 않는다. 동기화 이후에는
 * 이 시드 row를 지우고 북마크를 실제 row로 옮기는 정리가 필요하다.
 */
@Slf4j
@Component
@Order(25)
@ConditionalOnProperty(prefix = "app.demo-seed", name = "policies-enabled", havingValue = "true")
@RequiredArgsConstructor
public class PolicyDemoSeeder implements ApplicationRunner {

    private record DemoPolicy(int id, String title, String agencyName, PolicyCategory category) {
    }

    private static final List<DemoPolicy> DEMO_POLICIES = List.of(
            new DemoPolicy(50, "K-패스(K패스)", "국토교통부", PolicyCategory.생활지원),
            new DemoPolicy(2025, "청년 자산형성 지원(청년도약계좌)", "금융위원회", PolicyCategory.금융),
            new DemoPolicy(355, "경기청년 일자리 매치업 플러스", "경기도", PolicyCategory.일자리),
            new DemoPolicy(353, "경기도 청년 면접수당", "경기도", PolicyCategory.일자리),
            new DemoPolicy(356, "경기청년 맞춤형 채용지원 서비스", "경기도", PolicyCategory.일자리)
    );

    private final PolicyRepository policyRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int createdCount = 0;
        int skippedCount = 0;

        for (DemoPolicy demo : DEMO_POLICIES) {
            if (policyRepository.existsById(demo.id())) {
                skippedCount++;
                continue;
            }
            insert(demo);
            createdCount++;
        }

        log.info("Policy demo seed completed. created={}, skipped={}", createdCount, skippedCount);
    }

    private void insert(DemoPolicy demo) {
        LocalDate today = LocalDate.now();
        jdbcTemplate.update("""
                        INSERT INTO policy
                            (id, title, source_policy_id, source_type, agency_name, category,
                             summary, official_url, start_date, due_date, is_always_open, is_active, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                demo.id(),
                demo.title(),
                "DEMO-SEED-" + demo.id(),
                "DEMO_SEED",
                demo.agencyName(),
                demo.category().name(),
                "실 데이터 동기화 전 임시로 채운 데모 정책입니다.",
                null,
                today,
                today.plusMonths(3),
                false,
                true,
                "OPEN"
        );
        log.info("Policy demo seed row inserted. id={}, title={}", demo.id(), demo.title());
    }
}
