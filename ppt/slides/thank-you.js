/* ===================== 감사합니다 ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s-thank-you{ background:transparent; color:#16241C; }
#s-thank-you .wrap{ position:absolute; left:220px; top:168px; width:1480px; height:690px; display:flex; flex-direction:column; align-items:center; justify-content:center; text-align:center; }
#s-thank-you .kicker{ color:#0B7A3D; font-size:24px; font-weight:850; letter-spacing:0; margin-bottom:28px; }
#s-thank-you .title{ color:#16241C; font-size:94px; font-weight:900; line-height:1.08; letter-spacing:0; }
#s-thank-you .line{ width:120px; height:7px; border-radius:999px; background:#0B7A3D; margin:42px 0 34px; }
#s-thank-you .sub{ color:#5E6B62; font-size:24px; font-weight:650; line-height:1.5; }
#s-thank-you .panel{ position:absolute; left:360px; bottom:116px; width:1200px; height:82px; border-radius:22px; background:rgba(255,255,255,.82); border:1px solid rgba(11,122,61,.14); box-shadow:0 14px 30px rgba(20,40,25,.08); display:flex; align-items:center; justify-content:center; color:#16241C; font-size:20px; font-weight:750; }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s-thank-you">
  <div class="circle c1"></div>
  <div class="circle c2"></div>
  <div class="circle c3"></div>
  <div class="dot d1"></div>
  <div class="dot d2"></div>
  <div class="dot d3"></div>

  <div class="wrap">
    <div class="kicker">TheMoa Project</div>
    <div class="title">감사합니다</div>
    <div class="line"></div>
  </div>

  <div class="panel">KOSTA Final Project · TheMoa</div>
</section>
`);
