/* ===================== Slide 2 ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s2{ background:transparent; }   /* 배경은 #deck에 고정 */
#s2 .blob{ left:1286px; top:-444px; width:984px; height:984px; z-index:0; }
#s2 .ptitle{ left:52px; top:40px; width:711px; color:#6B7C8A; font-size:45px; font-weight:800; }
#s2 .subq{ left:262px; top:250px; width:1000px; color:#111; font-size:42px; font-weight:600; }
/* 왼쪽 스크린샷 콜라주 */
#s2 .post{ position:absolute; z-index:2; filter:drop-shadow(0 6px 14px rgba(0,0,0,.10)); }
#s2 .p1{ left:142px; top:350px; width:752px; }
#s2 .p2{ left:290px; top:530px; width:548px; }
#s2 .p3{ left:99px;  top:694px; width:433px; }
#s2 .p4{ left:277px; top:813px; width:772px; }
/* 검은 리본 배너 */
#s2 .ribbon{
  left:218px; top:454px; width:1426px; height:130px;
  background:#111; color:#fff; transform:rotate(5.196deg);
  display:flex; align-items:center; justify-content:center;
  font-size:26px; font-weight:500; text-align:center; padding:0 40px;
  z-index:6; box-shadow:0 10px 24px rgba(0,0,0,.25);
}
/* 오른쪽 문제 3가지 */
#s2 .prob{ left:1153px; width:740px; color:#111; font-size:35px; font-weight:400; }
#s2 .prob1{ top:386px; } #s2 .prob2{ top:564px; } #s2 .prob3{ top:741px; }
#s2 .marker{ position:absolute; left:1012px; width:91px; height:45px;
  background:#8E8E90; z-index:2;
  clip-path:polygon(0 0, 75% 0, 100% 50%, 75% 100%, 0 100%, 25% 50%); }
#s2 .m1{ top:396px; } #s2 .m2{ top:574px; } #s2 .m3{ top:751px; }

/* ---- 클릭 애니메이션(프래그먼트) ---- */
#s2 .prob.frag{ transform:translateY(16px); }
#s2 .prob.frag.show{ transform:translateY(0); }
#s2 .marker.frag{ transform:translateY(16px); }
#s2 .marker.frag.show{ transform:translateY(0); }
/* 검은 리본 "쿵!" 슬램 */
#s2 .ribbon.frag{ opacity:0; }
#s2 .ribbon.frag.show{ animation:slam .55s cubic-bezier(.18,.7,.28,1.25) forwards; }
@keyframes slam{
  0%  { opacity:0; transform:rotate(5.196deg) scale(1.7); }
  55% { opacity:1; transform:rotate(5.196deg) scale(.93); }
  75% { transform:rotate(5.196deg) scale(1.04); }
  100%{ opacity:1; transform:rotate(5.196deg) scale(1); }
}
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s2">
  <img class="abs blob" src="assets/s2_blob.png" alt="">
  <div class="abs ptitle">문제정의</div>
  <div class="abs subq">[20대들의 흔한 ‘소비’ 고민]</div>

  <img class="post p1" src="assets/s2_post1.png" alt="블라인드 - 사회초년생 소비 패턴">
  <img class="post p2" src="assets/s2_post2.png" alt="사회초년생 소비습관 조언">
  <img class="post p3" src="assets/s2_post3.png" alt="신용 체크 카드 추천">
  <img class="post p4" src="assets/s2_post4.png" alt="20대 저축 얼마나">

  <div class="abs marker m1 frag" data-step="1"></div>
  <div class="abs prob prob1 frag" data-step="1">고정지출 및 소비 내역 관리의 어려움</div>
  <div class="abs marker m2 frag" data-step="2"></div>
  <div class="abs prob prob2 frag" data-step="2">개인 맞춤형 청년 정책 정보 접근성 부족</div>
  <div class="abs marker m3 frag" data-step="3"></div>
  <div class="abs prob prob3 frag" data-step="3">여유 자금의 비효율적인 활용</div>

  <div class="abs ribbon frag" data-step="4">소비내역을 관리해주고, 나에게 필요한 정보를 한 곳에서 쉽게 찾을 수 있는 서비스 필요</div>
</section>
`);
