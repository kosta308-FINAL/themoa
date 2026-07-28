/* ===================== 목차 ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s-table-of-contents{ background:transparent; color:#16241C; }
#s-table-of-contents .title{ position:absolute; left:92px; top:72px; color:#16241C; font-size:56px; font-weight:850; line-height:1.1; }
#s-table-of-contents .subtitle{ position:absolute; left:96px; top:150px; color:#5E6B62; font-size:22px; font-weight:600; line-height:1.45; }
#s-table-of-contents .accent-line{ position:absolute; left:96px; top:205px; width:88px; height:6px; border-radius:999px; background:#0B7A3D; }
#s-table-of-contents .list{ position:absolute; left:172px; top:274px; width:1576px; display:grid; grid-template-columns:1fr 1fr; column-gap:120px; }
#s-table-of-contents .col{ display:flex; flex-direction:column; }
#s-table-of-contents .item{ height:92px; display:grid; grid-template-columns:76px 1fr; align-items:center; border-bottom:1px solid rgba(11,122,61,.18); }
#s-table-of-contents .item:first-child{ border-top:1px solid rgba(11,122,61,.18); }
#s-table-of-contents .num{ color:#0B7A3D; font-size:27px; font-weight:850; letter-spacing:0; font-variant-numeric:tabular-nums; }
#s-table-of-contents .text{ color:#142033; font-size:29px; font-weight:760; line-height:1.24; letter-spacing:0; white-space:nowrap; }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s-table-of-contents">
  <div class="title">목차</div>
  <div class="subtitle">TheMoa 프로젝트 발표 구성</div>
  <div class="accent-line"></div>

  <div class="list">
    <div class="col">
      <div class="item"><div class="num">01</div><div class="text">문제 제기</div></div>
      <div class="item"><div class="num">02</div><div class="text">서비스 소개</div></div>
      <div class="item"><div class="num">03</div><div class="text">기능 소개</div></div>
      <div class="item"><div class="num">04</div><div class="text">시현 영상</div></div>
      <div class="item"><div class="num">05</div><div class="text">설계 흐름도</div></div>
      <div class="item"><div class="num">06</div><div class="text">기술 스택</div></div>
    </div>
    <div class="col">
      <div class="item"><div class="num">07</div><div class="text">협업 관리</div></div>
      <div class="item"><div class="num">08</div><div class="text">비기능적 요구사항 구현</div></div>
      <div class="item"><div class="num">09</div><div class="text">인프라 및 구조</div></div>
      <div class="item"><div class="num">10</div><div class="text">개발 프로세스</div></div>
      <div class="item"><div class="num">11</div><div class="text">트러블슈팅</div></div>
      <div class="item"><div class="num">12</div><div class="text">소감</div></div>
    </div>
  </div>
</section>
`);
