package com.weaone.themoa.domain.recommend.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.weaone.themoa.domain.recommend.service.GoalFeasibility;
import com.weaone.themoa.domain.recommend.service.LlmSelector;
import com.weaone.themoa.domain.recommend.service.PreferredPeriod;
import com.weaone.themoa.domain.recommend.dto.Recommendation;
import com.weaone.themoa.domain.recommend.service.RecommendationService;
import com.weaone.themoa.domain.recommend.service.RiskType;
import com.weaone.themoa.domain.recommend.dto.UserProfile;

/**
 * 브라우저에서 직접 추천을 받아보는 간단한 화면(프로토타입).
 * GET "/"          → 입력 폼
 * GET "/recommend" → 폼 값으로 추천 결과 표시
 */
@RestController
public class RecommendWebController {

    private static final int MIN_GOAL_AMOUNT_WON = 100_000;
    private static final int MIN_DEPOSIT_WON = 10_000;
    // 우리 상품 데이터(예·적금)는 최장 36개월까지만 있다(연금상품은 취급 안 함) — 그보다 긴 목표기간은
    // 애초에 매칭될 상품이 없어서 추천 자체가 불가능하므로 여기서 미리 막는다.
    private static final int MAX_GOAL_MONTHS = 36;
    private static final int MIN_AGE = 1;
    private static final int MAX_AGE = 120;

    private final RecommendationService recommendationService;
    private final LlmSelector llmSelector;

    public RecommendWebController(RecommendationService recommendationService, LlmSelector llmSelector) {
        this.recommendationService = recommendationService;
        this.llmSelector = llmSelector;
    }

    @GetMapping(value = "/", produces = "text/html;charset=UTF-8")
    public String form() {
        return page("""
                <h1>💰 금융상품 맞춤 추천</h1>
                <p class="sub">내 정보를 넣으면 가입 가능한 예·적금 중 딱 맞는 걸 골라드려요.</p>
                <form method="get" action="/recommend">
                  <label>나이 <input type="number" name="age" value="26" min="1" max="120"></label>
                  <label>월소득(만원) <input type="number" name="income" value="250"></label>
                  <label>취업유형
                    <select name="employment">
                      <option value="직장인">직장인</option>
                      <option value="프리랜서">프리랜서</option>
                      <option value="무관">무관</option>
                    </select>
                  </label>
                  <label>위험성향
                    <select name="risk">
                      <option value="STABLE">안정형</option>
                      <option value="NEUTRAL">중립형</option>
                      <option value="AGGRESSIVE">공격형</option>
                    </select>
                  </label>
                  <label>월 납입가능금액(원, 잉여금 연동 예정) <input type="number" name="deposit" value="300000" min="10000" step="10000"></label>

                  <div class="goal-toggle">
                    <label class="check"><input type="radio" name="goalMode" value="none" checked onchange="toggleGoalMode()"> 저축목표 없음(선호기간만 사용)</label>
                    <label class="check"><input type="radio" name="goalMode" value="set" onchange="toggleGoalMode()"> 저축목표 직접입력</label>
                  </div>

                  <div id="noGoalFields">
                    <label>선호 가입기간
                      <select name="period">
                        <option value="SHORT">단기(6개월)</option>
                        <option value="MID">중기(1~2년, 최대 24개월)</option>
                        <option value="LONG">장기(3년, 36개월)</option>
                      </select>
                    </label>
                  </div>

                  <div id="goalFields" style="display:none">
                    <label>저축목표 금액(원) <input type="number" id="goalAmountInput" name="goalAmount" placeholder="예: 5000000" min="100000" step="10000"></label>
                    <label>저축목표 기간(개월, 최대 36개월) <input type="number" id="goalMonthsInput" name="goalMonths" placeholder="예: 12" min="1" max="36"></label>
                  </div>

                  <label class="check"><input type="checkbox" name="lowIncome" value="true"> 차상위계층</label>
                  <label class="check"><input type="checkbox" name="accept" value="true"> 우대조건 감수 가능</label>
                  <label class="check"><input type="checkbox" name="liquidity" value="true"> 중간에 뺄 수도 있어요(유동성 중요)</label>
                  <button type="submit">추천받기</button>
                </form>
                <script>
                  function toggleGoalMode() {
                    var goalSet = document.querySelector('input[name=goalMode]:checked').value === 'set';
                    document.getElementById('goalFields').style.display = goalSet ? '' : 'none';
                    document.getElementById('noGoalFields').style.display = goalSet ? 'none' : '';
                    if (!goalSet) {
                      document.getElementById('goalAmountInput').value = '';
                      document.getElementById('goalMonthsInput').value = '';
                    }
                  }
                </script>
                """);
    }

    @GetMapping(value = "/recommend", produces = "text/html;charset=UTF-8")
    public String recommend(
            @RequestParam(defaultValue = "26") int age,
            @RequestParam(name = "income", required = false) Integer incomeManwon,
            @RequestParam(defaultValue = "무관") String employment,
            @RequestParam(defaultValue = "STABLE") RiskType risk,
            @RequestParam(defaultValue = "SHORT") PreferredPeriod period,
            @RequestParam(defaultValue = "300000") int deposit,
            @RequestParam(defaultValue = "false") boolean lowIncome,
            @RequestParam(defaultValue = "false") boolean accept,
            @RequestParam(defaultValue = "false") boolean liquidity,
            @RequestParam(name = "goalAmount", required = false) Integer goalAmount,
            @RequestParam(name = "goalMonths", required = false) Integer goalMonths) {

        // 목표금액 없이 목표기간(개월)만 들어오면, 그 개월수가 아무 경고·안내 없이 선호기간을 조용히
        // 덮어써버려서 사용자가 모르는 사이 랭킹이 바뀌는 문제가 있었다 — 폼에서는 두 필드를 묶어서만
        // 받게 바꿨지만(직접입력/없음 토글), URL로 직접 goalMonths만 넣는 경우까지 막으려면 서버에서도
        // "목표금액이 없으면 목표기간도 무시" 규칙을 강제해야 한다.
        if (goalAmount == null) {
            goalMonths = null;
        }

        // 터무니없는 입력값(음수·0·극소값)은 뒤 계산이 다 무의미해지므로 여기서 바로 막는다.
        // (HTML min 속성은 우회 가능하므로 서버에서도 반드시 검증)
        String validationError = validateInput(age, deposit, goalAmount, goalMonths);
        if (validationError != null) {
            return page("<h1>🎯 추천 결과</h1><div class=\"warn\"><b>⚠️ 입력값을 확인해주세요.</b><br>"
                    + validationError + "</div><p><a href=\"/\">← 다시 입력하기</a></p>");
        }

        UserProfile profile = new UserProfile(age, incomeManwon, employment, lowIncome,
                risk, period, deposit, accept, liquidity, goalAmount, goalMonths, null);
        List<Recommendation> recs = recommendationService.recommend(profile, 5);

        StringBuilder body = new StringBuilder();
        body.append("<h1>🎯 추천 결과</h1>");
        body.append("<p class=\"sub\">").append(age).append("세 · ").append(esc(employment))
                .append(" · ").append(riskLabel(risk)).append(" · ").append(periodLabel(period))
                .append(accept ? " · 우대조건 감수 가능" : " · 우대조건 감수 싫음").append("</p>");
        body.append("<p class=\"sub\">").append(llmSelector.isEnabled()
                        ? "🤖 AI가 추천 상품마다 이유를 작성했어요. (순위는 규칙 기반 점수로 결정돼요)"
                        : "※ AI 최종선택 비활성(키 미설정) — 규칙 점수 순으로 표시 중")
                .append("</p>");
        body.append("<div class=\"legend\">"
                + "<b>보는 법</b> · 점수: 이 프로필에 얼마나 잘 맞는지(높을수록 좋음, 순위 결정 기준) · "
                + "💰 예상만기금액: 지금 월 납입가능액을 전액 넣었을 때 만기에 받는 금액 · "
                + "🎯 목표달성 최소금액: 입력하신 저축목표만 채우려면 매월 얼마씩 넣으면 되는지"
                + "</div>");

        // 목표 실현가능성은 원금만 나누지 않고, 실제 판매중 적금의 진짜 금리로 이자까지 포함해 계산한다.
        GoalFeasibility feasibility = recommendationService.assessGoal(profile);

        if (feasibility.hopeless()) {
            // 실제 이자를 다 반영해도, 우리가 가진 상품 데이터 범위(최장 36개월) 안에서 도달 불가 —
            // 상품 추천 자체가 무의미하므로 대안 목록 없이 직설적으로 안내만 한다.
            body.append("<div class=\"warn\"><b>⚠️ 이 목표는 예·적금만으로는 현실적이지 않아요.</b><br>")
                    .append("지금 월 납입가능액(").append(String.format("%,d", deposit))
                    .append("원)으로는 이자를 다 포함해도 저희 상품 중 가장 긴 상품(36개월)으로도 도달이 어려워요. ")
                    .append("월 납입액을 크게 늘리거나 목표를 현실적으로 낮춰서 다시 시도해주세요.</div>");
        } else if (!feasibility.reachableAtGoalMonths()) {
            body.append("<div class=\"warn\">⚠️ 지금 월 납입가능액(")
                    .append(String.format("%,d", deposit)).append("원)으로는 목표(")
                    .append(String.format("%,d", goalAmount)).append("원, ").append(goalMonths).append("개월)를 이자 포함해도 못 채워요. ")
                    .append("실제 판매중인 상품 기준으로 약 ").append(feasibility.actualMonthsNeeded()).append("개월 필요해요. ")
                    .append("월 납입액을 늘리거나 목표를 조정해보세요 (추천은 억지로 늘리지 않고 지금 여력 기준으로 드려요).</div>");
        }

        boolean goalUnreachable = feasibility.hasGoal() && !feasibility.reachableAtGoalMonths();

        if (!feasibility.hopeless()) {
            if (goalUnreachable) {
                body.append("<h2 class=\"section\">입력하신 목표기간(")
                        .append(periodLabel(period)).append(") 기준</h2>");
            }
            body.append(renderCards(recs));

            // 목표기간 안엔 이자 포함해도 못 채우지만 "그래도 해볼 만한" 수준이면, 실제 걸리는 기간
            // (진짜 상품 금리로 계산된 값) 기준 추천도 추가로 보여준다.
            int primaryTargetMonths = goalMonths != null ? goalMonths : profile.effectiveTargetMonths();
            if (goalUnreachable) {
                primaryTargetMonths = feasibility.actualMonthsNeeded();
                UserProfile paceProfile = profile.withExactTargetMonths(primaryTargetMonths);
                List<Recommendation> paceRecs = recommendationService.recommend(paceProfile, 5);

                body.append("<h2 class=\"section\">실제 이 페이스(약 ").append(primaryTargetMonths)
                        .append("개월)로 달성 가능한 시점 기준</h2>");
                body.append(renderCards(paceRecs));
            }

            // ⚠️ 위 목록들은 전부 "여력(월 납입가능액) 전액을 넣는다"는 가정으로 하드필터/점수를 계산한 것이다.
            // 목표금액만 채우려면 훨씬 적은 금액으로도 충분한 경우, 그 낮은 금액 기준으로 다시 하드필터를
            // 돌려야 한다 — 월한도가 낮아 여력 기준으론 제외됐던 상품(예: 월한도 30만원 적금)이
            // 목표 기준에선 정당하게 포함될 수 있고, 그 결과 상위 상품 자체가 달라질 수 있다.
            // 단, 목표를 기간 안에 못 채우는 경우(goalUnreachable)엔 이미 여력 전액도 부족하다는 뜻이라
            // "최소금액" 계산 자체가 의미 없다(사실상 여력 전액에 근접한 값이 나와 위 "실제 페이스" 섹션과
            // 거의 같은 내용이 중복 표시됨) — 그 경우엔 이 섹션을 아예 건너뛴다.
            if (!goalUnreachable && goalAmount != null && primaryTargetMonths > 0) {
                long principalMonthly = (long) Math.ceil(goalAmount / (double) primaryTargetMonths);
                if (principalMonthly < deposit) {
                    UserProfile goalOnlyProfile = profile.withExactTargetMonths(primaryTargetMonths)
                            .withMonthlyDeposit((int) principalMonthly);
                    List<Recommendation> goalOnlyRecs = recommendationService.recommend(goalOnlyProfile, 5);

                    // 여력 기준 목록과 상품 구성이 사실상 같으면(순서·상품 동일) 중복 노출을 피하려고 건너뛴다.
                    if (!sameProducts(recs, goalOnlyRecs)) {
                        body.append("<h2 class=\"section\">목표 금액만 채우려면(월 최소 약 ")
                                .append(String.format("%,d", principalMonthly)).append("원) 기준 — 여력 기준과 다를 수 있어요</h2>");
                        body.append(renderCards(goalOnlyRecs));
                    }
                }
            }
        }

        body.append("<a class=\"back\" href=\"/\">← 다시 입력하기</a>");
        body.append("<p class=\"disc\">※ 금리는 선호 가입기간에 맞는 옵션 기준입니다. "
                + "‘회전식’은 회전주기마다 금리가 갱신되어 표시금리가 계속 유지되지 않을 수 있어요. "
                + "실제 금리·조건은 각 금융사 공식 페이지에서 확인하세요.</p>");
        return page(body.toString());
    }

    /** 추천 목록 하나를 카드 HTML로 렌더링(순위는 이 목록 안에서 1위부터 다시 매김). */
    private static String renderCards(List<Recommendation> recs) {
        if (recs.isEmpty()) {
            return "<div class=\"empty\">조건에 맞는 상품이 없어요. 입력을 바꿔서 다시 시도해보세요.</div>";
        }
        StringBuilder body = new StringBuilder();
        int rank = 1;
        for (Recommendation r : recs) {
            body.append("<div class=\"card\">");
            body.append("<div class=\"rank\">").append(rank++).append("위</div>");
            body.append("<div class=\"score\">점수 ").append(r.score()).append("</div>");
            boolean rotating = r.productName() != null && r.productName().contains("회전");
            body.append("<div class=\"name\">").append(esc(r.company())).append(" · ")
                    .append(esc(r.productName()));
            if (rotating) {
                body.append(" <span class=\"badge\">⚠️ 회전식·금리변동</span>");
            }
            body.append("</div>");
            body.append("<div class=\"meta\">유형: ").append("DEPOSIT".equals(r.type()) ? "정기예금" : "적금")
                    .append(" · 금리: ").append(r.bestRate() == null ? "-" : r.bestRate() + "%")
                    .append(r.bestRateTerm() == null ? "" : " (" + r.bestRateTerm() + "개월 기준)")
                    .append("</div>");
            if (r.maturityAmountWon() != null) {
                body.append("<div class=\"maturity\"><b>💰 예상만기금액(여력 전액 기준):</b> ")
                        .append(String.format("%,d", r.maturityAmountWon())).append("원 (세전 추정)</div>");
            }
            if (r.goalMonthlyWon() != null) {
                body.append("<div class=\"maturity goal\"><b>🎯 목표달성 최소금액:</b> 매월 ")
                        .append(String.format("%,d", r.goalMonthlyWon())).append("원씩만 넣어도 충분 (만기 약 ")
                        .append(String.format("%,d", r.goalMaturityAmountWon())).append("원, 세전 추정)</div>");
                // 은행별 실제 최소가입금액 데이터가 없어 위 계산이 그 상품의 최소한도보다 작을 수 있다 —
                // 계산상 필요금액이 작을 때만(실제로 최소한도 미달 가능성이 있는 구간) 주의 문구를 붙인다.
                if (r.goalMonthlyWon() < 50_000) {
                    body.append("<div class=\"caveat\">※ 위 금액은 계산상 필요한 최소값이에요. "
                            + "실제 이 상품의 최소가입금액과는 다를 수 있으니 가입 전 확인해주세요.</div>");
                }
            }
            body.append("<div class=\"protect\">🛡 예금자보호: 1억원 (예금보험공사)</div>");
            if (r.llmReason() != null && !r.llmReason().isBlank()) {
                body.append("<div class=\"ai\"><b>🤖 AI 총평:</b> ").append(esc(r.llmReason())).append("</div>");
            }
            body.append("<div class=\"reasons-label\">추천 이유:</div>");
            body.append("<ul class=\"reasons\">");
            for (String reason : r.reasons()) {
                body.append("<li>").append(esc(stripScoreNotation(reason))).append("</li>");
            }
            body.append("</ul>");
            String homepage = BankHomepage.urlFor(r.company());
            if (homepage != null) {
                body.append("<a class=\"link\" target=\"_blank\" href=\"").append(homepage)
                        .append("\">🔗 ").append(esc(r.company())).append(" 공식 홈페이지</a>");
            } else {
                body.append("<a class=\"link\" target=\"_blank\" href=\"").append(searchLink(r))
                        .append("\">🔗 ").append(esc(r.company())).append(" 홈페이지 찾기</a>");
            }
            body.append("</div>");
        }
        return body.toString();
    }

    /** 내부 채점용 "(+N)" 표기는 개발자용이지 사용자용이 아니라서, 화면에 보여줄 땐 떼어낸다. */
    private static String stripScoreNotation(String reason) {
        return reason.replaceAll("\\s*\\(\\+\\d+\\)\\s*$", "");
    }

    /** 두 추천 목록이 같은 상품을 같은 순서로 담고 있는지(회사+상품명 기준). 같으면 별도 섹션을 또 보여줄 필요가 없다. */
    private static boolean sameProducts(List<Recommendation> a, List<Recommendation> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).company().equals(b.get(i).company())
                    || !a.get(i).productName().equals(b.get(i).productName())) {
                return false;
            }
        }
        return true;
    }

    /** 뒤 계산이 다 무의미해지는 극단적 입력을 걸러낸다. 문제없으면 null, 있으면 안내 문구를 반환. */
    private static String validateInput(int age, int deposit, Integer goalAmount, Integer goalMonths) {
        if (age < MIN_AGE || age > MAX_AGE) {
            return "나이는 " + MIN_AGE + "세 ~ " + MAX_AGE + "세 사이로 입력해주세요.";
        }
        if (deposit < MIN_DEPOSIT_WON) {
            return "월 납입가능금액은 " + String.format("%,d", MIN_DEPOSIT_WON) + "원 이상으로 입력해주세요.";
        }
        if (goalAmount != null && goalAmount < MIN_GOAL_AMOUNT_WON) {
            return "저축목표 금액은 " + String.format("%,d", MIN_GOAL_AMOUNT_WON) + "원 이상으로 입력해주세요.";
        }
        if (goalMonths != null && goalMonths < 1) {
            return "저축목표 기간은 1개월 이상으로 입력해주세요.";
        }
        if (goalMonths != null && goalMonths > MAX_GOAL_MONTHS) {
            return "저축목표 기간은 " + MAX_GOAL_MONTHS + "개월 이하로 입력해주세요. "
                    + "저희가 다루는 예·적금 상품은 최장 " + MAX_GOAL_MONTHS + "개월까지라, 그보다 긴 목표는 추천이 불가능해요.";
        }
        return null;
    }

    // ---- HTML 뼈대 & 유틸 ----

    private static String page(String inner) {
        return """
                <!doctype html><html lang="ko"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>금융상품 맞춤 추천</title>
                <style>
                  body{font-family:-apple-system,'Apple SD Gothic Neo',sans-serif;max-width:640px;margin:0 auto;padding:24px;background:#f6f7f9;color:#1a1a1a}
                  h1{font-size:24px;margin:0 0 4px}
                  .sub{color:#666;margin:0 0 20px}
                  form{background:#fff;border-radius:14px;padding:20px;box-shadow:0 1px 4px rgba(0,0,0,.06);display:flex;flex-direction:column;gap:14px}
                  label{display:flex;justify-content:space-between;align-items:center;gap:10px;font-size:15px}
                  label.check{justify-content:flex-start}
                  input[type=number],select{padding:8px 10px;border:1px solid #d0d3d8;border-radius:8px;font-size:15px;width:190px}
                  button{margin-top:6px;padding:13px;border:0;border-radius:10px;background:#2b6ef2;color:#fff;font-size:16px;font-weight:600;cursor:pointer}
                  .card{background:#fff;border-radius:14px;padding:16px 18px;margin-bottom:12px;box-shadow:0 1px 4px rgba(0,0,0,.06);position:relative}
                  .rank{position:absolute;top:16px;right:16px;color:#2b6ef2;font-weight:700}
                  .score{font-size:13px;color:#2b6ef2;font-weight:600}
                  .name{font-size:17px;font-weight:700;margin:2px 0}
                  .badge{display:inline-block;font-size:11px;font-weight:600;color:#a15c00;background:#fff3d6;border-radius:6px;padding:2px 7px;vertical-align:middle;margin-left:4px}
                  .meta{color:#555;font-size:14px;margin-bottom:6px}
                  .protect{display:inline-block;font-size:12px;font-weight:600;color:#0a7d3c;background:#e5f5ea;border-radius:6px;padding:2px 8px;margin-bottom:8px}
                  .maturity{font-size:13px;color:#1a5c99;font-weight:600;margin-bottom:6px}
                  .maturity.goal{color:#0a7d3c}
                  .caveat{font-size:12px;color:#8a6d00;margin:-2px 0 8px}
                  .ai{background:#eef2ff;border-left:3px solid #2b6ef2;border-radius:8px;padding:9px 11px;margin:2px 0 8px;font-size:13.5px;line-height:1.6;color:#22314f}
                  .reasons-label{font-size:12px;color:#888;margin:6px 0 2px}
                  .reasons{margin:0;padding-left:18px;color:#444;font-size:13px;line-height:1.7}
                  .legend{font-size:12px;color:#777;background:#fff;border-radius:8px;padding:8px 12px;margin-bottom:16px;line-height:1.6}
                  .link{display:inline-block;margin-top:4px;color:#2b6ef2;text-decoration:none;font-size:13px;font-weight:600}
                  .back{display:inline-block;margin-top:8px;color:#2b6ef2;text-decoration:none;font-weight:600}
                  .empty{background:#fff;border-radius:14px;padding:24px;text-align:center;color:#666}
                  .warn{background:#fdeeee;border:1px solid #f0b8b8;border-radius:10px;padding:12px 14px;margin-bottom:14px;color:#8a2c2c;font-size:13px;line-height:1.6}
                  .section{font-size:15px;margin:18px 0 10px;color:#333}
                  .disc{color:#999;font-size:12px;margin-top:16px}
                  .notice{margin-top:22px;padding:14px 16px;background:#fff8e6;border:1px solid #f0d98a;border-radius:10px;color:#7a5b00;font-size:12.5px;line-height:1.7}
                  .notice b{color:#5c4400}
                </style></head><body>
                """ + inner + """
                <div class="notice">
                  <b>⚠️ 꼭 읽어주세요</b><br>
                  본 서비스는 금융감독원 공시 데이터를 바탕으로 한 <b>추천 참고용</b>일 뿐입니다.
                  실제 금리·가입조건은 상품마다 다르고 수시로 바뀌므로, <b>가입 전 반드시 해당 금융사 공식 정보를 직접 확인</b>하세요.
                  최종 가입 결정과 그 결과에 대한 책임은 <b>본인에게</b> 있습니다.
                </div>
                </body></html>""";
    }

    /** 매핑에 없는 은행은 "은행명 공식 홈페이지" 검색으로 보낸다(공식 사이트가 보통 검색 1위). */
    private static String searchLink(Recommendation r) {
        String query = URLEncoder.encode(r.company() + " 공식 홈페이지", StandardCharsets.UTF_8);
        return "https://search.naver.com/search.naver?query=" + query;
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String riskLabel(RiskType r) {
        return switch (r) {
            case STABLE -> "안정형";
            case NEUTRAL -> "중립형";
            case AGGRESSIVE -> "공격형";
        };
    }

    private static String periodLabel(PreferredPeriod p) {
        return switch (p) {
            case SINGLE -> "초단기";
            case SHORT -> "단기";
            case MID -> "중기";
            case LONG -> "장기";
        };
    }
}
