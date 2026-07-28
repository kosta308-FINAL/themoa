/* ===================== 시현 (섹션 구분 슬라이드) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s-demo{ background:transparent; }
#s-demo .demo-text{ position:absolute; left:70px; top:50px; color:#0E1A2A; font-size:52px; font-weight:900; letter-spacing:0.02em; }
#s-demo .video-wrap{ position:absolute; left:260px; top:172px; width:1400px; height:788px;
  border-radius:20px; overflow:hidden; background:#000;
  box-shadow:0 30px 60px rgba(0,0,0,.35); }
#s-demo video{ width:100%; height:100%; object-fit:contain; background:#000; }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s-demo">
  <div class="demo-text">시현</div>
  <div class="video-wrap">
    <video src="assets/demo_video.mp4" controls playsinline></video>
  </div>
</section>
`);
