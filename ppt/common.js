/* 덱 엔진. 이 파일은 마지막에 로드되며, 그 전에 slides/slideN.js 가
   각자의 <section class="slide"> 를 #deck에 넣어둔 상태여야 합니다. */
(function(){
  const slides = Array.from(document.querySelectorAll('.slide'));
  let idx = 0;

  const revealAll = new URLSearchParams(location.search).has('all');

  function maxStep(slide){
    let m=0;
    slide.querySelectorAll('.frag').forEach(e=> m=Math.max(m, +e.dataset.step||0));
    if(slide.hasAttribute('data-morph')) m=Math.max(m,1);   // 모프 슬라이드는 단계 1개 추가
    return m;
  }
  // 슬라이드를 특정 단계로 세팅(누적): 프래그먼트 표시 + 모프 상태
  function setStep(slide, s){
    slide.dataset.step = s;
    slide.querySelectorAll('.frag').forEach(e=>{
      const st = +e.dataset.step||0;
      e.classList.toggle('show', st>0 && st<=s);
    });
    if(slide.hasAttribute('data-morph')) slide.classList.toggle('morphed', s>=1);
  }
  function show(i){
    idx = Math.max(0, Math.min(slides.length-1, i));
    slides.forEach((s,k)=>{
      s.classList.toggle('active', k===idx);
      s.classList.toggle('past', k<idx);   // 지난 슬라이드 = 왼쪽, 다음 = 오른쪽
    });
    setStep(slides[idx], revealAll ? maxStep(slides[idx]) : 0);
    // 진입 애니메이션 재생(활성 슬라이드에서만)
    const cur = slides[idx];
    cur.classList.remove('entering');
    void cur.offsetWidth;               // 리플로우로 애니메이션 재시작
    cur.classList.add('entering');
    document.getElementById('pageInd').textContent = (idx+1)+' / '+slides.length;
    if(history.replaceState) history.replaceState(null,'','#'+(idx+1));
  }
  // 클릭/→ : 남은 단계가 있으면 한 단계씩, 아니면 다음 슬라이드
  function next(){
    const s=slides[idx], cur=+s.dataset.step||0, mx=maxStep(s);
    if(cur<mx) setStep(s, cur+1);
    else show(idx+1);
  }
  function prev(){
    const s=slides[idx], cur=+s.dataset.step||0;
    if(cur>0) setStep(s, cur-1);
    else show(idx-1);
  }
  function fromHash(){ return (parseInt((location.hash||'').replace('#',''))||1) - 1; }
  window.addEventListener('hashchange', ()=> show(fromHash()));
  document.getElementById('next').onclick = next;
  document.getElementById('prev').onclick = prev;
  document.getElementById('stage').addEventListener('click', next);
  window.addEventListener('keydown', e=>{
    if(e.key==='ArrowRight'||e.key==='PageDown'||e.key===' '){ e.preventDefault(); next(); }
    if(e.key==='ArrowLeft' ||e.key==='PageUp'){ e.preventDefault(); prev(); }
  });

  // 화면 크기에 맞춰 덱 스케일 + 정중앙 배치
  function fit(){
    const deck = document.getElementById('deck');
    const s  = Math.min(window.innerWidth/1920, window.innerHeight/1080);
    const tx = (window.innerWidth  - 1920*s)/2;
    const ty = (window.innerHeight - 1080*s)/2;
    deck.style.transform = 'translate('+tx+'px,'+ty+'px) scale('+s+')';
  }

  window.addEventListener('resize', fit);
  const deckEl = document.getElementById('deck');
  fit(); show(fromHash());
  requestAnimationFrame(()=> requestAnimationFrame(()=> deckEl.classList.remove('notrans')));
})();
