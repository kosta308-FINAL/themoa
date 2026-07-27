/* ===================== LLM 코칭 카드 (습관지출 · 고정지출 연 환산, 규칙 계층과 LLM 역할 분리) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s-coacheng{ background:transparent; }
#s-coacheng .ptitle{ position:absolute; left:52px; top:40px; width:1500px; color:#6B7C8A; font-size:45px; font-weight:800; }
#s-coacheng .psub{ position:absolute; left:52px; top:112px; width:1780px; color:#111; font-size:23px; font-weight:500; line-height:1.5; }

#s-coacheng .pipeline{ position:absolute; left:70px; top:212px; width:1780px; background:#fff; border-radius:20px;
  box-shadow:0 20px 46px rgba(0,0,0,.14); padding:26px 36px; }
#s-coacheng .pipeline .plabel{ color:#171717; font-size:22px; font-weight:800; margin-bottom:16px; }

#s-coacheng .prow{ display:flex; align-items:center; gap:20px; margin-bottom:12px; }
#s-coacheng .prow:last-of-type{ margin-bottom:0; }
#s-coacheng .ptime{ flex:none; width:76px; height:76px; border-radius:50%; background:#111; color:#fff;
  display:flex; align-items:center; justify-content:center; font-size:17px; font-weight:800; }
#s-coacheng .prow.r2 .ptime{ background:#0B7A3D; }
#s-coacheng .pbody{ flex:1; }
#s-coacheng .pname{ color:#171717; font-size:19px; font-weight:800; margin-bottom:5px; }
#s-coacheng .pdesc{ color:#555; font-size:17px; font-weight:500; line-height:1.45; }

#s-coacheng .pnote{ margin-top:14px; padding-top:12px; border-top:1px dashed #E3E7EC;
  color:#B5541F; font-size:16px; font-weight:700; line-height:1.45; }

#s-coacheng .twocol{ position:absolute; left:70px; top:566px; width:1780px; display:flex; gap:40px; }
#s-coacheng .col{ flex:1; min-height:300px; display:flex; flex-direction:column; background:#fff; border-radius:20px; box-shadow:0 20px 46px rgba(0,0,0,.14);
  padding:30px 34px; }
#s-coacheng .col .chead{ flex:none; display:flex; align-items:center; gap:10px; margin-bottom:22px; }
#s-coacheng .col .chead .cdot{ flex:none; width:15px; height:15px; border-radius:50%; }
#s-coacheng .col.habit .chead .cdot{ background:#2F6FED; }
#s-coacheng .col.fixed .chead .cdot{ background:#0B7A3D; }
#s-coacheng .col .chead .ctitle{ color:#171717; font-size:22px; font-weight:800; }

#s-coacheng .citems{ flex:1; display:flex; flex-direction:column; justify-content:space-between; }
#s-coacheng .sitem2{ display:flex; align-items:center; gap:18px; }
#s-coacheng .sitem2 .snum2{ flex:none; width:42px; height:42px; border-radius:50%; color:#fff;
  display:flex; align-items:center; justify-content:center; font-size:16px; font-weight:800; }
#s-coacheng .col.habit .snum2{ background:#2F6FED; }
#s-coacheng .col.fixed .snum2{ background:#0B7A3D; }
#s-coacheng .sitem2 .stitle2{ color:#171717; font-size:26px; font-weight:800; line-height:1.3; }

#s-coacheng .note{ position:absolute; left:70px; top:908px; width:1780px; background:#fff; border-radius:14px;
  box-shadow:0 10px 24px rgba(0,0,0,.08); padding:14px 28px; color:#444; font-size:15px; font-weight:600;
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

      <div class="citems">
      <div class="sitem2"><div class="snum2">01</div><div class="stitle2">규칙 계층이 후보를 정한다</div></div>
      <div class="sitem2"><div class="snum2">02</div><div class="stitle2">절감액도 규칙이 계산한다</div></div>
      <div class="sitem2"><div class="snum2">03</div><div class="stitle2">LLM은 문장만 쓴다</div></div>
      </div>
    </div>

    <div class="col fixed">
      <div class="chead"><div class="cdot"></div><div class="ctitle">고정지출 연 환산 코칭</div></div>

      <div class="citems">
      <div class="sitem2"><div class="snum2">01</div><div class="stitle2">명백한 것만 규칙이 먼저 거른다</div></div>
      <div class="sitem2"><div class="snum2">02</div><div class="stitle2">필수 지출 판별은 LLM이 이름으로</div></div>
      <div class="sitem2"><div class="snum2">03</div><div class="stitle2">연 환산 숫자도 규칙이 계산</div></div>
      </div>
    </div>
  </div>

  <div class="note"><div class="dot"></div>LLM 응답에 예상 숫자(월평균·절감액·연환산액)가 실제로 들어있는지 검사해 실패한 카드만<br>그 자리에서 고정 템플릿으로 교체합니다 — 호출이 아예 실패해도 "카드 없음"으로 가지 않는 최소 보장선입니다.</div>
</section>
`);
