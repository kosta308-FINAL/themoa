/* ===================== LLM 코칭 카드 (습관지출 · 고정지출 연 환산, 규칙 계층과 LLM 역할 분리) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s-coacheng{ background:transparent; }
#s-coacheng .ptitle{ position:absolute; left:52px; top:40px; width:1500px; color:#6B7C8A; font-size:45px; font-weight:800; }
#s-coacheng .psub{ position:absolute; left:52px; top:112px; width:1780px; color:#111; font-size:22px; font-weight:500; line-height:1.5; }

#s-coacheng .pipeline{ position:absolute; left:70px; top:200px; width:1780px; background:#fff; border-radius:20px;
  box-shadow:0 20px 46px rgba(0,0,0,.14); padding:28px 36px; }
#s-coacheng .pipeline .plabel{ color:#171717; font-size:20px; font-weight:800; margin-bottom:20px; }

#s-coacheng .prow{ display:flex; align-items:center; gap:20px; margin-bottom:16px; }
#s-coacheng .prow:last-of-type{ margin-bottom:0; }
#s-coacheng .ptime{ flex:none; width:78px; height:78px; border-radius:50%; background:#111; color:#fff;
  display:flex; align-items:center; justify-content:center; font-size:17px; font-weight:800; }
#s-coacheng .prow.r2 .ptime{ background:#0B7A3D; }
#s-coacheng .pbody{ flex:1; }
#s-coacheng .pname{ color:#171717; font-size:18px; font-weight:800; margin-bottom:6px; }
#s-coacheng .pdesc{ color:#555; font-size:16px; font-weight:500; line-height:1.5; }

#s-coacheng .pnote{ margin-top:18px; padding-top:16px; border-top:1px dashed #E3E7EC;
  color:#B5541F; font-size:15px; font-weight:700; line-height:1.5; }

#s-coacheng .twocol{ position:absolute; left:70px; top:582px; width:1780px; display:flex; gap:40px; }
#s-coacheng .col{ flex:1; background:#fff; border-radius:20px; box-shadow:0 20px 46px rgba(0,0,0,.14);
  padding:26px 30px; }
#s-coacheng .col .chead{ display:flex; align-items:center; gap:10px; margin-bottom:20px; }
#s-coacheng .col .chead .cdot{ flex:none; width:14px; height:14px; border-radius:50%; }
#s-coacheng .col.habit .chead .cdot{ background:#2F6FED; }
#s-coacheng .col.fixed .chead .cdot{ background:#0B7A3D; }
#s-coacheng .col .chead .ctitle{ color:#171717; font-size:20px; font-weight:800; }

#s-coacheng .sitem2{ display:flex; align-items:flex-start; gap:16px; margin-bottom:18px; }
#s-coacheng .sitem2:last-child{ margin-bottom:0; }
#s-coacheng .sitem2 .snum2{ flex:none; width:34px; height:34px; border-radius:50%; color:#fff;
  display:flex; align-items:center; justify-content:center; font-size:13px; font-weight:800; }
#s-coacheng .col.habit .snum2{ background:#2F6FED; }
#s-coacheng .col.fixed .snum2{ background:#0B7A3D; }
#s-coacheng .sitem2 .stitle2{ color:#171717; font-size:17px; font-weight:800; margin-bottom:5px; }
#s-coacheng .sitem2 .sdesc2{ color:#555; font-size:14px; font-weight:500; line-height:1.5; }

#s-coacheng .note{ position:absolute; left:70px; top:930px; width:1780px; background:#fff; border-radius:14px;
  box-shadow:0 10px 24px rgba(0,0,0,.08); padding:18px 28px; color:#444; font-size:15px; font-weight:600;
  display:flex; align-items:center; gap:12px; }
#s-coacheng .note .dot{ flex:none; width:10px; height:10px; border-radius:50%; background:#2D8A4E; }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s-coacheng">
  <div class="ptitle">LLM 코칭 카드 — 규칙과 LLM의 역할 분리</div>
  <div class="psub">습관지출 코칭과 고정지출 연 환산 코칭, 두 도메인 모두 숫자는 규칙 계층이 계산하고<br>LLM(Gemini)은 그 숫자를 그대로 담은 문장만 씁니다.</div>

  <div class="pipeline">
    <div class="plabel">회원별 새벽 배치 파이프라인</div>

    <div class="prow r1">
      <div class="ptime">04:30</div>
      <div class="pbody">
        <div class="pname">습관 코칭 카드 배치 (HabitCoachingCardBatchService)</div>
        <div class="pdesc">회원의 급여일 새벽에만 실행되어, 직전에 끝난 급여 주기 소비로 카드를 만듭니다.</div>
      </div>
    </div>

    <div class="prow r2">
      <div class="ptime">03:00</div>
      <div class="pbody">
        <div class="pname">고정지출 연 환산 코칭 배치 (FixedExpenseCoachingCardService)</div>
        <div class="pdesc">전 회원을 매일 순회하며 이번 주기 카드가 아직 없는 회원만 생성합니다.</div>
      </div>
    </div>

    <div class="pnote">두 배치 모두 year_month 단위로 이미 카드가 있는지 먼저 확인해 멱등하게 동작합니다.<br>습관 코칭은 여기에 더해, 최초 3개월 백필이 끝나는 순간에도 한 번 더 트리거됩니다.</div>
  </div>

  <div class="twocol">
    <div class="col habit">
      <div class="chead"><div class="cdot"></div><div class="ctitle">습관지출 코칭</div></div>

      <div class="sitem2">
        <div class="snum2">01</div>
        <div>
          <div class="stitle2">규칙 계층이 후보를 정한다</div>
          <div class="sdesc2">카테고리·가맹점 alias별로 지출을 집계해 월평균<br>3만원 이상인 것만, 많이 쓴 순으로 최대 3개 추립니다.</div>
        </div>
      </div>
      <div class="sitem2">
        <div class="snum2">02</div>
        <div>
          <div class="stitle2">절감액도 규칙이 계산한다</div>
          <div class="sdesc2">배달·카페·여가처럼 재량 소비는 50%, 편의점·식비·<br>교통처럼 필수가 섞인 소비는 30%로 비율을 고정합니다.</div>
        </div>
      </div>
      <div class="sitem2">
        <div class="snum2">03</div>
        <div>
          <div class="stitle2">LLM은 문장만 쓴다</div>
          <div class="sdesc2">낭비로 단정하지 않는 어조로 문구만 생성하고, 이미<br>"필요한 소비"로 표시한 항목은 담담한 톤으로만 안내합니다.</div>
        </div>
      </div>
    </div>

    <div class="col fixed">
      <div class="chead"><div class="cdot"></div><div class="ctitle">고정지출 연 환산 코칭</div></div>

      <div class="sitem2">
        <div class="snum2">01</div>
        <div>
          <div class="stitle2">명백한 것만 규칙이 먼저 거른다</div>
          <div class="sdesc2">기부·회비 카테고리와 사용자가 dismiss한 항목만<br>규칙 계층이 미리 후보에서 제외합니다.</div>
        </div>
      </div>
      <div class="sitem2">
        <div class="snum2">02</div>
        <div>
          <div class="stitle2">필수 지출 판별은 LLM이 이름으로</div>
          <div class="sdesc2">월세·관리비·보험료처럼 카테고리가 애매해도 이름에서<br>필수로 읽히면 LLM이 후보에서 골라내 제외합니다.</div>
        </div>
      </div>
      <div class="sitem2">
        <div class="snum2">03</div>
        <div>
          <div class="stitle2">연 환산 숫자도 규칙이 계산</div>
          <div class="sdesc2">월 납입액×12를 규칙이 미리 계산해 넘기고, LLM은<br>그 숫자를 그대로 담아 안내 문장만 만듭니다.</div>
        </div>
      </div>
    </div>
  </div>

  <div class="note"><div class="dot"></div>LLM 응답에 예상 숫자(월평균·절감액·연환산액)가 실제로 들어있는지 검사해 실패한 카드만<br>그 자리에서 고정 템플릿으로 교체합니다 — 호출이 아예 실패해도 "카드 없음"으로 가지 않는 최소 보장선입니다.</div>
</section>
`);
