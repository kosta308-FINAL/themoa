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
  <div class="cdesc2" style="left:70px;top:780px;width:470px">공통 규칙 · env · application.yaml ·<br>기능별 md 문서를 Notion으로 관리하고,<br>다른 기능에서 참조가 필요하면 해당<br>기능별 md를 그대로 참조하도록 했습니다.</div>

  <!-- ② 기능별 브랜치 전략 -->
  <div class="clabel" style="left:618px;top:212px;">기능별 브랜치 전략</div>
  <img class="shot" style="left:618px;top:288px;width:400px" src="assets/s32_0.png" alt="기능별 브랜치 전략">
  <div class="cdesc2" style="left:618px;top:780px;width:400px">기능별 브랜치 전략으로 충돌을 최소화하고<br>유연하게 작업할 수 있도록 했습니다.<br>변경 이력이 기능 단위로 분리돼 리뷰가<br>쉽고, 문제가 생기면 원인 브랜치를 바로<br>찾아 되돌릴 수 있습니다.</div>

  <!-- ③ discord merge message -->
  <div class="clabel" style="left:1100px;top:212px;">discord merge message</div>
  <video class="shot" style="left:1100px;top:288px;width:750px" src="assets/s32_collab.mp4" poster="assets/s32_3.jpeg" autoplay muted loop playsinline></video>
  <div class="cdesc2" style="left:1100px;top:780px;width:750px">Discord Webhook으로 PR이 dev 브랜치에 merge되면 변경 사항이 자동으로 올라와,<br>팀원에게 직접 말할 필요 없이 실시간으로 공유됩니다.</div>
</section>
`);
