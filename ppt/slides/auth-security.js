/* ===================== 인증 보안 설계 (탈취 전제하의 방어) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s-authsec{ background:transparent; }
#s-authsec .ptitle{ position:absolute; left:52px; top:40px; width:1500px; color:#6B7C8A; font-size:45px; font-weight:800; }
#s-authsec .psub{ position:absolute; left:52px; top:112px; width:1780px; color:#111; font-size:22px; font-weight:500; line-height:1.5; }

#s-authsec .twocol{ position:absolute; left:70px; top:200px; width:1780px; display:flex; gap:40px; }
#s-authsec .col{ flex:1; background:#fff; border-radius:20px; box-shadow:0 20px 46px rgba(0,0,0,.14);
  padding:26px 30px; }
#s-authsec .col .chead{ display:flex; align-items:center; gap:10px; margin-bottom:20px; }
#s-authsec .col .chead .cdot{ flex:none; width:14px; height:14px; border-radius:50%; }
#s-authsec .col.store .chead .cdot{ background:#2F6FED; }
#s-authsec .col.guard .chead .cdot{ background:#C0392B; }
#s-authsec .col .chead .ctitle{ color:#171717; font-size:20px; font-weight:800; }

#s-authsec .sitem2{ display:flex; align-items:flex-start; gap:16px; margin-bottom:20px; }
#s-authsec .sitem2:last-child{ margin-bottom:0; }
#s-authsec .sitem2 .snum2{ flex:none; width:34px; height:34px; border-radius:50%; color:#fff;
  display:flex; align-items:center; justify-content:center; font-size:13px; font-weight:800; }
#s-authsec .col.store .snum2{ background:#2F6FED; }
#s-authsec .col.guard .snum2{ background:#C0392B; }
#s-authsec .sitem2 .stitle2{ color:#171717; font-size:17px; font-weight:800; margin-bottom:6px; }
#s-authsec .sitem2 .sdesc2{ color:#555; font-size:15px; font-weight:500; line-height:1.6; }

#s-authsec .note{ position:absolute; left:70px; top:930px; width:1780px; background:#fff; border-radius:14px;
  box-shadow:0 10px 24px rgba(0,0,0,.08); padding:18px 28px; color:#444; font-size:16px; font-weight:600;
  display:flex; align-items:center; gap:12px; }
#s-authsec .note .dot{ flex:none; width:10px; height:10px; border-radius:50%; background:#2D8A4E; }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s-authsec">
  <div class="ptitle">인증 보안 설계 — 탈취를 막을 수 없다는 전제에서 시작</div>
  <div class="psub">Access/Refresh Token은 소지 자체가 인증이 되는 bearer 자격증명이라 탈취를 100% 막을 수 없습니다.<br>목표는 탈취 확률을 낮추고, 탈취돼도 노출 창을 좁히는 것입니다.</div>

  <div class="twocol">
    <div class="col store">
      <div class="chead"><div class="cdot"></div><div class="ctitle">토큰 저장 · 전송</div></div>

      <div class="sitem2">
        <div class="snum2">01</div>
        <div>
          <div class="stitle2">Refresh는 HttpOnly 쿠키, Access는 헤더</div>
          <div class="sdesc2">localStorage는 XSS로 두 토큰이 모두 탈취될 수 있지만 CSRF엔 안전하고,<br>HttpOnly 쿠키는 XSS로부터 Refresh를 지키는 대신 CSRF 대응이 따라옵니다.<br>장수명·고가치인 Refresh만 HttpOnly+Secure 쿠키로 절충했습니다.</div>
        </div>
      </div>
      <div class="sitem2">
        <div class="snum2">02</div>
        <div>
          <div class="stitle2">짧은 수명 + rotation으로 노출 창 축소</div>
          <div class="sdesc2">Access Token은 30분 stateless JWT로 짧게 만료시키고, Refresh Token은<br>재발급마다 폐기 후 재발급(rotation)해 탈취된 토큰의 유효 시간을 줄입니다.</div>
        </div>
      </div>
      <div class="sitem2">
        <div class="snum2">03</div>
        <div>
          <div class="stitle2">전송 구간 HTTPS(TLS) 강제</div>
          <div class="sdesc2">배포 환경 전 구간에 TLS를 강제합니다. 토큰·비밀번호가 평문으로<br>네트워크를 지나면 도청으로 탈취되고, Refresh 쿠키의 Secure 속성도<br>HTTPS를 전제로 하기 때문입니다.</div>
        </div>
      </div>
    </div>

    <div class="col guard">
      <div class="chead"><div class="cdot"></div><div class="ctitle">요청 방어 · 즉시 무효화</div></div>

      <div class="sitem2">
        <div class="snum2">01</div>
        <div>
          <div class="stitle2">CSRF 토큰 없이 SameSite로 방어</div>
          <div class="sdesc2">Refresh 쿠키에 SameSite=Lax(재발급·로그아웃은 Strict)를 적용해<br>cross-site 요청엔 쿠키가 실리지 않게 하고, 나머지 API는 Authorization<br>헤더 방식이라 애초에 자동 전송되지 않아 CSRF에 면역입니다.</div>
        </div>
      </div>
      <div class="sitem2">
        <div class="snum2">02</div>
        <div>
          <div class="stitle2">token_version으로 강제 무효화</div>
          <div class="sdesc2">Stateless JWT는 만료 전 강제 로그아웃이 원래 어렵지만, 회원 단위<br>token_version을 클레임에 실어 매 요청 대조해 비밀번호 변경·계정<br>탈취 의심 시 모든 기기의 Access Token을 즉시 무효화합니다.</div>
        </div>
      </div>
      <div class="sitem2">
        <div class="snum2">03</div>
        <div>
          <div class="stitle2">sub 기반 소유권 검사</div>
          <div class="sdesc2">요청 파라미터의 id를 신뢰하지 않고 Access Token의 sub(memberId)를<br>소유자로 삼아 조회·수정 조건에 강제합니다. ROLE 인가로는<br>대체되지 않는 객체 수준(object-level) 검사입니다.</div>
        </div>
      </div>
    </div>
  </div>

  <div class="note"><div class="dot"></div>여섯 가지 결정 모두 "왜 이렇게 했는지" 트레이드오프 근거를 문서로 남겼습니다 — HTTPS 등 baseline 항목 포함 여부는 팀 논의 후 확정합니다.</div>
</section>
`);
