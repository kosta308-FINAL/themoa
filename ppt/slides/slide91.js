/* ===================== Slide 91 (LLM 활용 구조 · 도메인별 전문 Agent + 독립 Qdrant Collection) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s91{ background:transparent; }   /* 배경은 #deck에 고정된 공통 그라데이션을 그대로 사용 */
#s91 .ptitle{ left:52px; top:40px; width:1100px; color:#6B7C8A; font-size:45px; font-weight:800; }
#s91 .psub{ left:52px; top:112px; width:1500px; color:#111; font-size:24px; font-weight:500; }

/* 사용자 질문 pill */
#s91 .query{
  left:830px; top:172px; width:260px; height:58px; border-radius:999px;
  background:#111; color:#fff; display:flex; align-items:center; justify-content:center;
  font-size:20px; font-weight:700; z-index:3; box-shadow:0 10px 22px rgba(0,0,0,.25);
}

/* 연결선 */
#s91 .wires{ z-index:1; pointer-events:none; }

/* 도메인 Agent 카드 */
#s91 .agent{
  position:absolute; top:300px; width:520px; height:280px; border-radius:20px;
  background:#fff; box-shadow:0 20px 46px rgba(0,0,0,.14); z-index:2;
  padding:26px 30px; overflow:hidden;
}
#s91 .agent.a1{ left:90px; }
#s91 .agent.a2{ left:700px; }
#s91 .agent.a3{ left:1310px; }
#s91 .agent .bar{ position:absolute; left:0; top:0; width:100%; height:8px; }
#s91 .agent.a1 .bar{ background:#2F6FED; }
#s91 .agent.a2 .bar{ background:#007613; }
#s91 .agent.a3 .bar{ background:#7C3AED; }

#s91 .agent .icon{
  width:56px; height:56px; border-radius:14px; display:flex; align-items:center;
  justify-content:center; font-size:28px; margin-bottom:14px;
}
#s91 .agent.a1 .icon{ background:#EAF1FF; }
#s91 .agent.a2 .icon{ background:#E8F5EE; }
#s91 .agent.a3 .icon{ background:#F1EBFC; }

#s91 .agent .aname{ color:#111; font-size:28px; font-weight:800; margin-bottom:8px; }
#s91 .agent .adesc{ color:#555; font-size:17px; font-weight:500; line-height:1.5; margin-bottom:18px; }

#s91 .agent .dbtag{
  display:inline-flex; align-items:center; gap:9px; height:38px; padding:0 15px;
  border-radius:8px; background:#0F172A; color:#8FF7C0;
  font-size:16px; font-weight:700; letter-spacing:.2px;
}
#s91 .agent .dbtag .dot{ width:8px; height:8px; border-radius:50%; background:#8FF7C0; flex:none; }

/* 통합 응답 박스 */
#s91 .answer{
  left:800px; top:642px; width:320px; height:64px; border-radius:14px;
  background:#111; color:#fff; display:flex; align-items:center; justify-content:center;
  font-size:19px; font-weight:700; z-index:3; box-shadow:0 10px 22px rgba(0,0,0,.25);
}

/* 장점 칩 */
#s91 .benefits{ left:70px; top:760px; width:1780px; display:flex; gap:22px; z-index:2; }
#s91 .benefit{
  flex:1; background:#fff; border-radius:16px; padding:20px 22px;
  box-shadow:0 12px 26px rgba(0,0,0,.10);
}
#s91 .benefit .btitle{ color:#007613; font-size:19px; font-weight:800; margin-bottom:8px;
  display:flex; align-items:center; gap:8px; }
#s91 .benefit .bdesc{ color:#333; font-size:15px; font-weight:500; line-height:1.5; }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s91">
  <div class="abs ptitle">LLM 활용 구조</div>
  <div class="abs psub">하나의 Agent가 정책·금융상품·문의를 다 처리하지 않고, 도메인별 전문 Agent 3개 + 독립된 Qdrant Collection 3개로 분리</div>

  <div class="abs query">사용자 질문</div>

  <svg class="abs wires" viewBox="0 0 1920 1080" width="1920" height="1080" style="left:0;top:0">
    <path d="M960,230 C960,260 350,260 350,300" fill="none" stroke="#BEC8DA" stroke-width="5"/>
    <path d="M960,230 C960,260 960,260 960,300" fill="none" stroke="#BEC8DA" stroke-width="5"/>
    <path d="M960,230 C960,260 1570,260 1570,300" fill="none" stroke="#BEC8DA" stroke-width="5"/>
    <path d="M350,580 C350,620 960,610 960,642" fill="none" stroke="#BEC8DA" stroke-width="5"/>
    <path d="M960,580 C960,610 960,610 960,642" fill="none" stroke="#BEC8DA" stroke-width="5"/>
    <path d="M1570,580 C1570,620 960,610 960,642" fill="none" stroke="#BEC8DA" stroke-width="5"/>
  </svg>

  <div class="agent a1">
    <div class="bar"></div>
    <div class="icon">🏛️</div>
    <div class="aname">정책 Agent</div>
    <div class="adesc">청년정책 검색 · 조건별 추천</div>
    <div class="dbtag"><span class="dot"></span>youthcenter_policies</div>
  </div>

  <div class="agent a2">
    <div class="bar"></div>
    <div class="icon">💳</div>
    <div class="aname">금융상품 Agent</div>
    <div class="adesc">예금 · 적금 · 대출 상품 매칭</div>
    <div class="dbtag"><span class="dot"></span>financial_products</div>
  </div>

  <div class="agent a3">
    <div class="bar"></div>
    <div class="icon">💬</div>
    <div class="aname">고객문의 Agent</div>
    <div class="adesc">FAQ · 이용문의 응답</div>
    <div class="dbtag"><span class="dot"></span>customer_service_knowledge</div>
  </div>

  <div class="abs answer">통합 응답 생성</div>

  <div class="abs benefits">
    <div class="benefit">
      <div class="btitle">도메인 간 간섭 없음</div>
      <div class="bdesc">컬렉션이 분리돼 있어 정책 임베딩이 금융상품 검색에<br>노이즈로 섞이지 않습니다.</div>
    </div>
    <div class="benefit">
      <div class="btitle">독립적인 확장</div>
      <div class="bdesc">한 도메인의 임베딩 모델·스키마를 바꿔도 다른 도메인 코드에는<br>영향이 없습니다.</div>
    </div>
    <div class="benefit">
      <div class="btitle">명확한 유지보수</div>
      <div class="bdesc">Agent 하나가 모든 걸 처리하지 않아, 도메인별로 로직과 책임이<br>분리돼 있습니다.</div>
    </div>
    <div class="benefit">
      <div class="btitle">손쉬운 도메인 추가</div>
      <div class="bdesc">새 컬렉션과 서비스만 추가하면 기존 도메인 로직은<br>그대로 유지됩니다.</div>
    </div>
  </div>
</section>
`);
