/* ===================== Slide 31 (기술스택&인프라 · AWS 아키텍처) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s31{ background:transparent; }
#s31 .sectitle{ position:absolute; left:1035px; top:296px; width:775px; color:#171717; font-size:26px; font-weight:800; }
#s31 .sitem{ position:absolute; left:1035px; width:775px; display:flex; align-items:flex-start; gap:16px; }
#s31 .sitem .snum{ width:38px; height:38px; border-radius:50%; flex:none; background:#2D8A4E; color:#fff;
  display:flex; align-items:center; justify-content:center; font-size:15px; font-weight:800;
  box-shadow:0 8px 18px rgba(45,138,78,.32); }
#s31 .sitem .stitle{ color:#171717; font-size:20px; font-weight:800; margin-bottom:4px; }
#s31 .sitem .sdesc{ color:#555; font-size:15px; font-weight:500; line-height:1.45; }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s31">
  <div class="abs" style="left:52px;top:35px;width:711px;text-align:left;color:#6B7C8A;font-size:45px;font-weight:700">기술스택&amp;인프라</div>
  <div class="abs" style="left:317px;top:236px;width:400px;text-align:center;color:#2D8A4E;font-size:22px;font-weight:800">aws architecture</div>
  <img class="abs" style="left:120px;top:280px;width:794px;height:692px;object-fit:contain" src="assets/s31_0.png" alt="AWS 아키텍처">

  <div class="sectitle">보안 설계 포인트</div>

  <div class="sitem" style="top:352px">
    <div class="snum">01</div>
    <div>
      <div class="stitle">외부 공개 지점을 ALB 하나로 제한</div>
      <div class="sdesc">web/app EC2, RDS, Qdrant는 전부 내부에서만 접근합니다.</div>
    </div>
  </div>
  <div class="sitem" style="top:452px">
    <div class="snum">02</div>
    <div>
      <div class="stitle">EC2는 private subnet, public IP 미할당</div>
      <div class="sdesc">web/app 서버 모두 외부에서 직접 접근할 수 없습니다.</div>
    </div>
  </div>
  <div class="sitem" style="top:552px">
    <div class="snum">03</div>
    <div>
      <div class="stitle">SSH 대신 SSM Session Manager 접속</div>
      <div class="sdesc">22번 포트를 열지 않고 AWS Systems Manager로만 접속합니다.</div>
    </div>
  </div>
  <div class="sitem" style="top:652px">
    <div class="snum">04</div>
    <div>
      <div class="stitle">RDS는 public access 차단</div>
      <div class="sdesc">app 서버 보안 그룹에서만 3306 포트 접근을 허용합니다.</div>
    </div>
  </div>
  <div class="sitem" style="top:752px">
    <div class="snum">05</div>
    <div>
      <div class="stitle">Qdrant는 app EC2 내부 localhost 전용</div>
      <div class="sdesc">외부는 물론 다른 인스턴스에서도 직접 접근할 수 없습니다.</div>
    </div>
  </div>
  <div class="sitem" style="top:852px">
    <div class="snum">06</div>
    <div>
      <div class="stitle">HTTPS는 ALB에서 ACM 인증서로 처리</div>
      <div class="sdesc">HTTP 요청은 전부 HTTPS로 리다이렉트합니다.</div>
    </div>
  </div>
</section>
`);
