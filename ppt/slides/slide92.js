/* ===================== Slide 92 (LLM 활용 구조 · 도메인별 전문 Agent + 독립 Qdrant Collection) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s92{ background:transparent; color:#0F1B2A; }
#s92 .ptitle{ position:absolute; left:58px; top:50px; width:980px; font-size:54px; font-weight:850; line-height:1.08; color:#0E1A2A; letter-spacing:0; }
#s92 .ptitle .accent{ color:#007A3D; }
#s92 .psub{ position:absolute; left:60px; top:124px; width:1580px; color:#152235; font-size:25px; font-weight:560; line-height:1.46; letter-spacing:0; }
#s92 .psub .accent{ color:#007A3D; font-weight:850; }
#s92 .query{ position:absolute; left:767px; top:224px; width:386px; height:82px; border-radius:999px; background:linear-gradient(135deg,#22C98B 0%,#007A51 58%,#0B566F 100%); color:#fff; display:flex; align-items:center; justify-content:center; gap:22px; font-size:30px; font-weight:850; z-index:4; box-shadow:0 18px 34px rgba(0,122,81,.24); border:1px solid rgba(255,255,255,.36); }
#s92 .query .user-ico{ width:56px; height:56px; border-radius:50%; background:rgba(255,255,255,.92); display:flex; align-items:center; justify-content:center; color:#0FA66B; }
#s92 .query .user-ico svg{ width:36px; height:36px; display:block; }
#s92 .wires{ position:absolute; left:0; top:0; z-index:2; pointer-events:none; overflow:visible; }
#s92 .wire{ opacity:0; transition:opacity .45s ease, transform .45s ease; }
#s92 .wire.show{ opacity:1; transform:translateY(0); }
#s92 .agent{ position:absolute; top:392px; width:500px; height:276px; border-radius:24px; background:rgba(255,255,255,.94); border:1px solid rgba(196,211,203,.72); box-shadow:0 18px 40px rgba(25,48,38,.12); z-index:3; padding:39px 34px 30px; overflow:hidden; transform:translateY(18px); }
#s92 .agent.show{ transform:translateY(0); }
#s92 .agent.a1{ left:118px; }
#s92 .agent.a2{ left:710px; }
#s92 .agent.a3{ left:1302px; }
#s92 .agent .bar{ position:absolute; left:0; top:0; width:100%; height:9px; }
#s92 .agent.a1 .bar{ background:#2F73EA; }
#s92 .agent.a2 .bar{ background:#059A50; }
#s92 .agent.a3 .bar{ background:#8051DE; }
#s92 .agent .row{ display:flex; align-items:center; gap:28px; }
#s92 .agent .icon{ width:92px; height:92px; border-radius:50%; display:flex; align-items:center; justify-content:center; flex:none; box-shadow:0 12px 22px rgba(17,44,28,.10); }
#s92 .agent.a1 .icon{ background:#EAF2FF; color:#2F73EA; }
#s92 .agent.a2 .icon{ background:#E8F7EF; color:#059A50; }
#s92 .agent.a3 .icon{ background:#F0EAFF; color:#8051DE; }
#s92 .agent .icon svg{ width:56px; height:56px; display:block; }
#s92 .agent .aname{ color:#102033; font-size:31px; font-weight:850; line-height:1.2; margin-bottom:12px; letter-spacing:0; }
#s92 .agent.a1 .aname{ color:#123F8F; }
#s92 .agent.a2 .aname{ color:#006B39; }
#s92 .agent.a3 .aname{ color:#4B2B98; }
#s92 .agent .adesc{ color:#223047; font-size:20px; font-weight:620; line-height:1.42; letter-spacing:0; }
#s92 .agent .divider{ width:100%; height:1px; background:#E3E9E6; margin:28px 0 18px; }
#s92 .agent .respond{ font-size:19px; font-weight:800; }
#s92 .agent.a1 .respond{ color:#2F73EA; }
#s92 .agent.a2 .respond{ color:#007A3D; }
#s92 .agent.a3 .respond{ color:#8051DE; }
#s92 .agent .wm{ position:absolute; right:31px; bottom:25px; font-size:42px; font-weight:900; line-height:1; opacity:.12; }
#s92 .agent.a1 .wm{ color:#2F73EA; }
#s92 .agent.a2 .wm{ color:#059A50; }
#s92 .agent.a3 .wm{ color:#8051DE; }
#s92 .benefits{ position:absolute; left:90px; top:752px; width:1740px; height:190px; background:rgba(255,255,255,.94); border:1px solid rgba(211,224,216,.9); border-radius:24px; box-shadow:0 16px 34px rgba(24,54,39,.11); display:grid; grid-template-columns:repeat(4,1fr); z-index:3; overflow:hidden; transform:translateY(18px); }
#s92 .benefits.show{ transform:translateY(0); }
#s92 .benefit{ display:flex; align-items:center; gap:18px; padding:30px 26px; position:relative; }
#s92 .benefit + .benefit{ border-left:1px solid #DDE8E1; }
#s92 .benefit .bicon{ width:70px; height:70px; border-radius:50%; flex:none; display:flex; align-items:center; justify-content:center; background:#EAF7F0; color:#087540; box-shadow:inset 0 0 0 1px rgba(8,117,64,.08); }
#s92 .benefit .bicon svg{ width:42px; height:42px; display:block; }
#s92 .benefit .btitle{ color:#006B39; font-size:23px; font-weight:850; margin-bottom:10px; line-height:1.2; letter-spacing:0; }
#s92 .benefit .bdesc{ color:#263241; font-size:18px; font-weight:560; line-height:1.48; letter-spacing:0; }
#s92 .frag{ transform:translateY(18px); }
#s92 .frag.show{ transform:translateY(0); }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s92">
  <div class="ptitle"><span class="accent">LLM</span> 활용 구조</div>
  <div class="psub">하나의 Agent가 정책·금융상품·문의사항을 모두 처리하지 않고,<br>도메인별 <span class="accent">전문 Agent 3개</span> + 독립된 <span class="accent">Qdrant Collection 3개</span>로 분리하여 정확도와 확장성을 높였습니다.</div>

  <div class="query frag" data-step="1">
    <div class="user-ico">
      <svg viewBox="0 0 48 48" aria-hidden="true">
        <circle cx="24" cy="17" r="8" fill="currentColor"></circle>
        <path d="M10 40c1.7-8.3 7.2-13 14-13s12.3 4.7 14 13H10z" fill="currentColor"></path>
      </svg>
    </div>
    <div>사용자 질문</div>
  </div>

  <svg class="wires" viewBox="0 0 1920 1080" width="1920" height="1080">
    <g class="wire frag" data-step="2">
      <path d="M960 306 C960 346 368 330 368 392" fill="none" stroke="#6FA1F5" stroke-width="4" stroke-linecap="round"></path>
      <circle cx="368" cy="392" r="8" fill="#2F73EA"></circle>
    </g>
    <g class="wire frag" data-step="3">
      <path d="M960 306 C960 342 960 348 960 392" fill="none" stroke="#11A967" stroke-width="4" stroke-linecap="round"></path>
      <circle cx="960" cy="392" r="8" fill="#059A50"></circle>
    </g>
    <g class="wire frag" data-step="4">
      <path d="M960 306 C960 346 1552 330 1552 392" fill="none" stroke="#9B75F1" stroke-width="4" stroke-linecap="round"></path>
      <circle cx="1552" cy="392" r="8" fill="#8051DE"></circle>
    </g>
  </svg>

  <div class="agent a1 frag" data-step="2">
    <div class="bar"></div>
    <div class="row">
      <div class="icon">
        <svg viewBox="0 0 64 64" aria-hidden="true">
          <path d="M12 26h40v6H12v-6z" fill="currentColor"></path>
          <path d="M16 36h7v14h-7V36zm13 0h7v14h-7V36zm13 0h7v14h-7V36z" fill="currentColor"></path>
          <path d="M10 52h44v6H10v-6zM32 8l22 12v4H10v-4L32 8z" fill="currentColor"></path>
        </svg>
      </div>
      <div>
        <div class="aname">정책 Agent</div>
        <div class="adesc">청년정책 검색 · 조건별 추천</div>
      </div>
    </div>
    <div class="divider"></div>
    <div class="respond">→ 사용자에게 바로 응답</div>
    <div class="wm">01</div>
  </div>

  <div class="agent a2 frag" data-step="3">
    <div class="bar"></div>
    <div class="row">
      <div class="icon">
        <svg viewBox="0 0 64 64" aria-hidden="true">
          <rect x="10" y="18" width="44" height="30" rx="5" fill="currentColor"></rect>
          <rect x="15" y="26" width="34" height="5" rx="2" fill="#FFFFFF" opacity=".72"></rect>
          <rect x="16" y="38" width="14" height="4" rx="2" fill="#FFFFFF" opacity=".72"></rect>
        </svg>
      </div>
      <div>
        <div class="aname">금융상품 Agent</div>
        <div class="adesc">예금 · 적금 · 대출 상품 매칭</div>
      </div>
    </div>
    <div class="divider"></div>
    <div class="respond">→ 사용자에게 바로 응답</div>
    <div class="wm">02</div>
  </div>

  <div class="agent a3 frag" data-step="4">
    <div class="bar"></div>
    <div class="row">
      <div class="icon">
        <svg viewBox="0 0 64 64" aria-hidden="true">
          <path d="M14 18h25c7 0 12 5 12 12s-5 12-12 12H29l-12 8v-8h-3c-7 0-12-5-12-12s5-12 12-12z" fill="currentColor" opacity=".48"></path>
          <path d="M27 26h24c6 0 11 5 11 11s-5 11-11 11h-8l-11 7v-7h-5c-6 0-11-5-11-11s5-11 11-11z" fill="currentColor"></path>
          <circle cx="33" cy="37" r="2.3" fill="#FFFFFF"></circle>
          <circle cx="42" cy="37" r="2.3" fill="#FFFFFF"></circle>
          <circle cx="51" cy="37" r="2.3" fill="#FFFFFF"></circle>
        </svg>
      </div>
      <div>
        <div class="aname">고객문의 Agent</div>
        <div class="adesc">FAQ · 이용문의 응답</div>
      </div>
    </div>
    <div class="divider"></div>
    <div class="respond">→ 사용자에게 바로 응답</div>
    <div class="wm">03</div>
  </div>

  <div class="benefits frag" data-step="5">
    <div class="benefit">
      <div class="bicon">
        <svg viewBox="0 0 64 64" aria-hidden="true">
          <path d="M32 7l21 8v15c0 14-8.7 23.3-21 28C19.7 53.3 11 44 11 30V15l21-8z" fill="none" stroke="currentColor" stroke-width="5" stroke-linejoin="round"></path>
          <path d="M22 32l7 7 14-16" fill="none" stroke="currentColor" stroke-width="5" stroke-linecap="round" stroke-linejoin="round"></path>
        </svg>
      </div>
      <div>
        <div class="btitle">도메인 간 간섭 없음</div>
        <div class="bdesc">컬렉션이 분리되어 정책 임베딩이<br>금융상품 검색에 노이즈로<br>섞이지 않습니다.</div>
      </div>
    </div>
    <div class="benefit">
      <div class="bicon">
        <svg viewBox="0 0 64 64" aria-hidden="true">
          <ellipse cx="32" cy="15" rx="18" ry="7" fill="none" stroke="currentColor" stroke-width="5"></ellipse>
          <path d="M14 15v25c0 4 8 8 18 8s18-4 18-8V15M14 28c0 4 8 8 18 8s18-4 18-8" fill="none" stroke="currentColor" stroke-width="5"></path>
        </svg>
      </div>
      <div>
        <div class="btitle">독립적인 확장</div>
        <div class="bdesc">한 도메인의 임베딩 모델·스키마를<br>바꿔도 다른 도메인 코드에는<br>영향이 없습니다.</div>
      </div>
    </div>
    <div class="benefit">
      <div class="bicon">
        <svg viewBox="0 0 64 64" aria-hidden="true">
          <rect x="16" y="28" width="32" height="24" rx="4" fill="none" stroke="currentColor" stroke-width="5"></rect>
          <path d="M23 28v-7a9 9 0 0 1 18 0v7" fill="none" stroke="currentColor" stroke-width="5" stroke-linecap="round"></path>
          <circle cx="32" cy="40" r="3" fill="currentColor"></circle>
        </svg>
      </div>
      <div>
        <div class="btitle">명확한 유지보수</div>
        <div class="bdesc">Agent별로 로직과 책임이 분리되어<br>도메인 단위로 관리할 수 있습니다.</div>
      </div>
    </div>
    <div class="benefit">
      <div class="bicon">
        <svg viewBox="0 0 64 64" aria-hidden="true">
          <path d="M35 31l-9 9-7-7 9-9c7-7 15-10 25-10-1 10-4 18-11 25l-9 9-7-7 9-10z" fill="none" stroke="currentColor" stroke-width="5" stroke-linejoin="round"></path>
          <path d="M20 44l-7 7M43 21l1 0" stroke="currentColor" stroke-width="5" stroke-linecap="round"></path>
        </svg>
      </div>
      <div>
        <div class="btitle">손쉬운 도메인 추가</div>
        <div class="bdesc">새 Collection과 Agent만 추가하면<br>기존 도메인 로직은 그대로<br>유지됩니다.</div>
      </div>
    </div>
  </div>
</section>
`);
