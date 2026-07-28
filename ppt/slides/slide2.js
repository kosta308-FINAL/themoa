/* ===================== Slide 2 (문제제기) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s2{ background:transparent; }
#s2 .ptitle{ left:52px; top:40px; width:711px; color:#6B7C8A; font-size:45px; font-weight:800; }

#s2 .cards{ left:88px; top:302px; width:1744px; display:flex; gap:32px; }
#s2 .card{
  flex:1; background:#fff; border-radius:22px; padding:34px 34px 40px;
  box-shadow:0 18px 40px rgba(20,40,25,.09);
  display:flex; flex-direction:column;
}
#s2 .num{ color:#2D8A5E; font-size:32px; font-weight:800; font-variant-numeric:tabular-nums; }
#s2 .head{ color:#171717; font-size:26px; font-weight:800; margin-top:10px; line-height:1.35; }
#s2 .illust{ width:150px; height:143px; object-fit:contain; margin:38px auto 30px; display:block; }
#s2 .divider{ height:1px; background:#E7ECE8; margin-bottom:22px; }
#s2 .cap{ color:#6B7C8A; font-size:17px; font-weight:600; text-align:center; line-height:1.55; }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s2">
  <div class="abs ptitle">문제제기</div>

  <div class="abs cards">
    <div class="card">
      <div class="num">01</div>
      <div class="head">고정지출 및 소비 내역 관리의 어려움</div>
      <img class="illust" src="assets/problem1_consume.png" alt="OTT 구독을 보고 당황한 26세 김다미양">
      <div class="divider"></div>
      <div class="cap">자신이 사용하지 않는 OTT가 구독되어있어서<br>당황스러운 26세 김다미양</div>
    </div>

    <div class="card">
      <div class="num">02</div>
      <div class="head">여유 자금의 비효율적인 활용</div>
      <img class="illust" src="assets/problem2_saving.png" alt="어떤 저축상품을 들어야할지 혼란스러운 신입사원 박호민군">
      <div class="divider"></div>
      <div class="cap">어떤 저축상품을 들어야할지 혼란스러운<br>신입사원 박호민군</div>
    </div>

    <div class="card">
      <div class="num">03</div>
      <div class="head">개인 맞춤형 청년 정책 정보 접근성 부족</div>
      <img class="illust" src="assets/problem3_policy.png" alt="청년 면접정장 대여료 지원사업 정책을 면접이 끝난 뒤 본 취준생 임연호군">
      <div class="divider"></div>
      <div class="cap">청년 면접정장 대여료 지원사업 정책을<br>면접이 끝난 뒤 본 취준생 임연호군</div>
    </div>
  </div>
</section>
`);
