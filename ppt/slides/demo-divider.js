/* ===================== 시현 (섹션 구분 슬라이드) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s-demo{ background:transparent; display:flex; align-items:center; justify-content:center; }
#s-demo .demo-text{ color:#0E1A2A; font-size:160px; font-weight:900; letter-spacing:0.02em; }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s-demo">
  <div class="demo-text">시현</div>
</section>
`);
