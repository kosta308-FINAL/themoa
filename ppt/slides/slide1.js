/* ===================== Slide 1 ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s1{
  background:radial-gradient(circle at 34% 40%,
    #ffffff 0%, #f5fff3 33%, #f6fff2 66%, #d2ffbd 100%);
}
/* 금색 뱃지 */
#s1 .badge{
  left:104px; top:288px; width:338px; height:58px;
  border-radius:999px;
  background:radial-gradient(circle at 30% 25%,
    #FFC555 0%, #998314 40%, #79610B 72%, #724100 100%);
  display:flex; align-items:center; justify-content:center;
  box-shadow:0 6px 16px rgba(114,65,0,.35);
}
#s1 .badge span{
  color:#ffffff; font-weight:700; font-size:20px; letter-spacing:1px;
}
/* 대형 타이틀 */
#s1 .title{
  left:104px; top:363px; width:1704px; height:216px;
  color:#222222; font-weight:700; font-size:160px; line-height:1;
  letter-spacing:-2px;
}
/* 반사(뒤집힌 aoMehT) — 초록 그라데이션 31% */
#s1 .reflect{
  left:104px; top:631px; width:1704px; height:216px;
  font-weight:700; font-size:160px; line-height:1; letter-spacing:-2px;
  transform:scaleY(-1); transform-origin:top;
  background:radial-gradient(circle at 50% 50%,
    rgba(91,255,147,.31) 0%, rgba(90,208,166,.31) 50%, rgba(55,145,0,.31) 100%);
  -webkit-background-clip:text; background-clip:text; color:transparent;
  pointer-events:none;
}
/* 금화 (제목 'o' 완전히 덮기) */
#s1 .coin{ left:528px; top:408px; width:94px; height:94px; }
/* 3D 카드 */
#s1 .cards{ left:967px; top:195px; width:766px; height:720px; }
/* 초록 세로선 */
#s1 .vbar{
  left:113px; top:938px; width:5px; height:48px; border-radius:3px;
  background:linear-gradient(to bottom, #B3FFC6, #6EFAAB 50%, #00A651);
}
/* 크레딧 */
#s1 .credit{
  left:137px; top:929px; width:1046px; height:57px;
  display:flex; align-items:center;
  color:#222222; font-size:30px; font-weight:500;
}
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s1">
  <div class="abs badge"><span>KOSTA FINAL PROJECT</span></div>
  <div class="abs title">TheMoa</div>
  <div class="abs reflect">TheMoa</div>
  <img class="abs coin"  src="assets/s1_coin.png"  alt="gold coin">
  <img class="abs cards" src="assets/s1_cards.png" alt="credit cards">
  <div class="abs vbar"></div>
  <div class="abs credit">소작농 &nbsp;|&nbsp; 김솔민, 문호연, 임수지</div>
</section>
`);
