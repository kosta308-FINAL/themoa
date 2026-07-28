/* ===================== Slide 32 (협업관리) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s32{ background:transparent; }
#s32 .clabel{ position:absolute; color:#171717; font-weight:800; font-size:32px; }
#s32 .cdesc2{ position:absolute; color:#444; font-size:20px; font-weight:500; line-height:1.65; }
#s32 .shot{ position:absolute; border-radius:12px; box-shadow:0 16px 38px rgba(0,0,0,.14);
  border:1px solid #E7ECE8; background:#fff; }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s32">
  <div class="abs" style="left:52px;top:35px;width:711px;text-align:left;color:#6B7C8A;font-size:45px;font-weight:700">협업관리</div>

  <!-- ① Notion -->
  <img class="abs" style="left:70px;top:206px;width:48px;height:52px;object-fit:contain" src="assets/s32_2.png" alt="">
  <div class="clabel" style="left:136px;top:212px;">Notion</div>
  <img class="shot" style="left:70px;top:288px;width:470px" src="assets/s32_1.png" alt="Notion 워크스페이스">

  <!-- ② 기능별 브랜치 전략 -->
  <div class="clabel" style="left:618px;top:212px;">기능별 브랜치 전략</div>
  <img class="shot" style="left:618px;top:288px;width:400px" src="assets/s32_0.png" alt="기능별 브랜치 전략">

  <!-- ③ discord merge message -->
  <div class="clabel" style="left:1100px;top:212px;">discord merge message</div>
  <video class="shot" style="left:1100px;top:288px;width:750px" src="assets/s32_collab.mp4" poster="assets/s32_3.jpeg" autoplay muted loop playsinline></video>
</section>
`);
