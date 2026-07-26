/* ===================== Slide 31 (기술스택&인프라 · AWS 아키텍처) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s31{ background:transparent; }
#s31 .awsitem{ position:absolute; left:1035px; width:775px; display:flex; align-items:center; gap:22px; }
#s31 .awsitem .dot{ width:48px; height:48px; border-radius:50%; background:#2D8A4E; color:#fff; flex:none;
  display:flex; align-items:center; justify-content:center; font-size:24px; font-weight:800;
  box-shadow:0 8px 18px rgba(45,138,78,.32); }
#s31 .awsitem .t{ color:#222; font-size:27px; font-weight:600; line-height:1.4; }
#s31 .awsitem b{ color:#2D8A4E; font-weight:800; }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s31">
  <div class="abs" style="left:52px;top:35px;width:711px;text-align:left;color:#6B7C8A;font-size:45px;font-weight:700">기술스택&amp;인프라</div>
  <div class="abs" style="left:317px;top:236px;width:400px;text-align:center;color:#2D8A4E;font-size:22px;font-weight:800">aws architecture</div>
  <img class="abs" style="left:120px;top:280px;width:794px;height:692px;object-fit:contain" src="assets/s31_0.png" alt="AWS 아키텍처">

  <div class="awsitem" style="top:452px"><div class="dot">&#10003;</div><div class="t"><b>Nginx + Load Balancer</b>로 안정적인 서비스 제공</div></div>
  <div class="awsitem" style="top:610px"><div class="dot">&#10003;</div><div class="t"><b>AWS RDS</b>와 연동하여 데이터 저장</div></div>
  <div class="awsitem" style="top:768px"><div class="dot">&#10003;</div><div class="t"><b>HTTPS</b> 적용으로 보안 강화</div></div>
</section>
`);
