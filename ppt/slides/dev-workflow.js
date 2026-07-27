/* ===================== 개발 프로세스 (작업지시서 기반 반복 사이클) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s-dev-workflow{ background:transparent; }   /* 배경은 #deck에 고정된 공통 그라데이션을 그대로 사용 */
#s-dev-workflow .ptitle{ left:52px; top:40px; width:1200px; color:#6B7C8A; font-size:45px; font-weight:800; }
#s-dev-workflow .psub{ left:52px; top:112px; width:1780px; color:#111; font-size:24px; font-weight:500; }

/* 파이프라인 카드 */
#s-dev-workflow .pipeline{ left:70px; top:230px; width:1780px; height:300px; }
#s-dev-workflow .step{
  position:absolute; top:0; width:336px; height:300px; border-radius:20px;
  background:#fff; box-shadow:0 20px 46px rgba(0,0,0,.12); padding:28px 24px; overflow:hidden;
}
#s-dev-workflow .step .bar{ position:absolute; left:0; top:0; width:100%; height:8px; background:#2F6FED; }
#s-dev-workflow .step.st2 .bar{ background:#007613; }
#s-dev-workflow .step.st3 .bar{ background:#B8892E; }
#s-dev-workflow .step.st4 .bar{ background:#7C3AED; }
#s-dev-workflow .step.st5 .bar{ background:#C0392B; }

#s-dev-workflow .step .num{
  position:absolute; right:20px; top:28px; width:36px; height:36px; border-radius:50%;
  background:#F3F5F7; color:#8A93A0; font-size:17px; font-weight:800;
  display:flex; align-items:center; justify-content:center;
}
#s-dev-workflow .step .stitle{ color:#171717; font-size:23px; font-weight:800; line-height:1.3; margin-bottom:14px; min-height:60px; max-width:250px; }
#s-dev-workflow .step .sdesc{ color:#555; font-size:17px; font-weight:500; line-height:1.55; }

#s-dev-workflow .step.st1{ left:0px; }
#s-dev-workflow .step.st2{ left:361px; }
#s-dev-workflow .step.st3{ left:722px; }
#s-dev-workflow .step.st4{ left:1083px; }
#s-dev-workflow .step.st5{ left:1444px; }

/* 단계 사이 화살표 */
#s-dev-workflow .arrow{ position:absolute; top:135px; width:25px; height:30px; color:#B7C0CC; font-size:26px; font-weight:800; text-align:center; }
#s-dev-workflow .arrow.a1{ left:336px; }
#s-dev-workflow .arrow.a2{ left:697px; }
#s-dev-workflow .arrow.a3{ left:1058px; }
#s-dev-workflow .arrow.a4{ left:1419px; }

/* 반복 루프 */
#s-dev-workflow .loop{ left:599px; top:548px; width:1083px; }
#s-dev-workflow .loop svg{ display:block; }
#s-dev-workflow .looplabel{
  position:absolute; left:50%; top:8px; transform:translateX(-50%);
  background:#171717; color:#fff; font-size:17px; font-weight:800;
  padding:9px 22px; border-radius:999px; white-space:nowrap;
}

/* 실제 예시 카드 */
#s-dev-workflow .example{
  left:70px; top:648px; width:1780px; height:400px; border-radius:20px;
  background:#fff; box-shadow:0 18px 40px rgba(0,0,0,.10); overflow:hidden;
}
#s-dev-workflow .ex-head{
  position:absolute; left:0; top:0; width:100%; height:58px; background:#F7F8FA;
  display:flex; align-items:center; padding:0 30px; gap:12px;
  border-bottom:1px solid #EAEDF0;
}
#s-dev-workflow .ex-head .extag{ background:#EAF1FF; color:#2F6FED; font-size:15px; font-weight:800; padding:6px 14px; border-radius:8px; }
#s-dev-workflow .ex-head .exname{ color:#171717; font-size:19px; font-weight:800; }
#s-dev-workflow .ex-body{ position:absolute; left:0; top:58px; width:100%; height:342px; display:flex; }
#s-dev-workflow .ex-col{ flex:1; padding:24px 34px; }
#s-dev-workflow .ex-col.left{ border-right:1px solid #EAEDF0; }
#s-dev-workflow .ex-collabel{ color:#8A93A0; font-size:15px; font-weight:800; margin-bottom:16px; }
#s-dev-workflow .toc{ list-style:none; }
#s-dev-workflow .toc li{ display:flex; gap:12px; font-size:18px; font-weight:600; color:#333; margin-bottom:12px; line-height:1.4; }
#s-dev-workflow .toc li .n{ flex:none; color:#2F6FED; font-weight:800; }
#s-dev-workflow .toc li.hi .n, #s-dev-workflow .toc li.hi span:last-child{ color:#C0392B; }

#s-dev-workflow .check{ display:flex; flex-direction:column; gap:8px; margin-bottom:20px; }
#s-dev-workflow .check .crow{ display:flex; align-items:flex-start; gap:12px; font-size:18px; font-weight:600; color:#333; line-height:1.5; }
#s-dev-workflow .check .cb{ flex:none; width:24px; height:24px; border-radius:6px; background:#E8F5EE; color:#1E7A3D; font-size:16px; font-weight:900; display:flex; align-items:center; justify-content:center; margin-top:1px; }
#s-dev-workflow .check .utag{ margin-left:36px; display:inline-block; background:#F1EBFC; color:#7C3AED; font-size:13px; font-weight:800; padding:3px 10px; border-radius:999px; }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s-dev-workflow">
  <div class="abs ptitle">개발 프로세스</div>
  <div class="abs psub">작업지시서(md) 기반으로 작업단위를 쪼개 구현 → 검증하고, 다음 작업단위로 넘어가는 사이클을 반복했습니다.</div>

  <div class="abs pipeline">
    <div class="step st1">
      <div class="bar"></div><div class="num">1</div>
      <div class="stitle">작업지시서 확인</div>
      <div class="sdesc">기능별 작업지시서에<br>구현 방법, 참고 문서,<br>필요한 API와 완료 기준을<br>미리 정리했습니다</div>
    </div>
    <div class="step st2">
      <div class="bar"></div><div class="num">2</div>
      <div class="stitle">작업단위 분리</div>
      <div class="sdesc">단위테스트가 가능한 크기로<br>기능을 쪼갬<br>(예: 탐지 배치 / 등록 API /<br>수집 매칭을 각각 별도 단위로)</div>
    </div>
    <div class="step st3">
      <div class="bar"></div><div class="num">3</div>
      <div class="stitle">작업단위 구현</div>
      <div class="sdesc">Controller - Service -<br>Repository 계층으로 구현<br>(mustrule.md 공통 규약 준수)</div>
    </div>
    <div class="step st4">
      <div class="bar"></div><div class="num">4</div>
      <div class="stitle">체크리스트 확인</div>
      <div class="sdesc">작업단위마다 대응하는<br>체크리스트 항목을 하나씩<br>체크하며 빠짐없이 구현됐는지 검증</div>
    </div>
    <div class="step st5">
      <div class="bar"></div><div class="num">5</div>
      <div class="stitle">Swagger / Postman<br>단위테스트</div>
      <div class="sdesc">실제 endpoint를 직접 호출해<br>요청·응답을 눈으로 확인</div>
    </div>

    <div class="abs arrow a1">→</div>
    <div class="abs arrow a2">→</div>
    <div class="abs arrow a3">→</div>
    <div class="abs arrow a4">→</div>
  </div>

  <div class="abs loop">
    <svg viewBox="0 0 1083 90" width="1083" height="90">
      <path d="M1083,10 C1083,70 0,70 0,10" fill="none" stroke="#B7C0CC" stroke-width="4" marker-end="url(#dw-arrow)"/>
      <defs>
        <marker id="dw-arrow" markerWidth="10" markerHeight="10" refX="6" refY="3" orient="auto">
          <path d="M0,0 L6,3 L0,6 Z" fill="#B7C0CC"/>
        </marker>
      </defs>
    </svg>
    <div class="looplabel">다음 작업단위로 반복</div>
  </div>

  <div class="abs example">
    <div class="ex-head">
      <div class="extag">작업지시서 예시</div>
      <div class="exname">plan/md/fixedExpense.md</div>
    </div>
    <div class="ex-body">
      <div class="ex-col left">
        <div class="ex-collabel">문서 구조</div>
        <ul class="toc">
          <li><span class="n">1.</span><span>목적</span></li>
          <li><span class="n">2.</span><span>반복결제 탐지 규칙</span></li>
          <li><span class="n">4.</span><span>등록 경로 (+ erd.md 참고)</span></li>
          <li><span class="n">5.</span><span>규칙 구조 + 수집 매칭 로직</span></li>
          <li class="hi"><span class="n">8.</span><span>구현 체크리스트</span></li>
        </ul>
      </div>
      <div class="ex-col right">
        <div class="ex-collabel">작업단위별 진행 방식 (§8 발췌)</div>
        <div class="check">
          <div class="crow"><div class="cb">✓</div><span>구현 완료 → 체크리스트 확인 → Postman 테스트 통과</span></div>
          <div class="utag">작업단위: 등록 API</div>
        </div>
        <div class="check">
          <div class="crow"><div class="cb">✓</div><span>구현 완료 → 체크리스트 확인 → 반영 확인</span></div>
          <div class="utag">작업단위: ERD 수정</div>
        </div>
        <div class="check">
          <div class="crow"><div class="cb">✓</div><span>구현 완료 → 체크리스트 확인 → 단위테스트 통과</span></div>
          <div class="utag">작업단위: 탐지 배치</div>
        </div>
      </div>
    </div>
  </div>
</section>
`);
