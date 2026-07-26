/* ===================== 인트로 영상 (2번째 슬라이드) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#sIntro{ background:#000; }
#sIntro video{
  position:absolute; inset:0;
  width:100%; height:100%;
  object-fit:cover;          /* 화면 꽉 채우기 */
}
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="sIntro">
  <video src="assets/intro.mp4" playsinline preload="auto"></video>
</section>
`);

/* 이 슬라이드가 보일 때 처음부터 재생, 벗어나면 정지 */
(function(){
  const sec = document.getElementById('sIntro');
  const vid = sec.querySelector('video');
  new MutationObserver(function(){
    if(sec.classList.contains('active')){
      vid.currentTime = 0;
      // 소리 포함 재생 시도 → 브라우저가 막으면 음소거로 재생
      vid.play().catch(function(){ vid.muted = true; vid.play().catch(function(){}); });
    } else {
      vid.pause();
    }
  }).observe(sec, { attributes:true, attributeFilter:['class'] });
})();
