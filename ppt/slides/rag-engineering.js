/* ===================== RAG 품질을 위한 엔지니어링 (청킹 전략 · 새벽 배치 · 변경 감지) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s-rageng{ background:transparent; }
#s-rageng .ptitle{ position:absolute; left:52px; top:40px; width:1500px; color:#6B7C8A; font-size:45px; font-weight:800; }
#s-rageng .psub{ position:absolute; left:52px; top:112px; width:1780px; color:#111; font-size:24px; font-weight:500; line-height:1.5; }

#s-rageng .pipeline{ position:absolute; left:70px; top:196px; width:1780px; background:#fff; border-radius:20px;
  box-shadow:0 20px 46px rgba(0,0,0,.14); padding:26px 36px; }
#s-rageng .pipeline .plabel{ color:#171717; font-size:22px; font-weight:800; margin-bottom:16px; }

#s-rageng .prow{ display:flex; align-items:center; gap:20px; margin-bottom:12px; }
#s-rageng .prow:last-of-type{ margin-bottom:0; }
#s-rageng .ptime{ flex:none; width:76px; height:76px; border-radius:50%; background:#111; color:#fff;
  display:flex; align-items:center; justify-content:center; font-size:17px; font-weight:800; }
#s-rageng .prow.r2 .ptime{ background:#2D8A4E; }
#s-rageng .pbody{ flex:1; }
#s-rageng .pname{ color:#171717; font-size:19px; font-weight:800; margin-bottom:5px; }
#s-rageng .pdesc{ color:#555; font-size:17px; font-weight:500; line-height:1.45; }

#s-rageng .pnote{ margin-top:14px; padding-top:12px; border-top:1px dashed #E3E7EC;
  color:#B5541F; font-size:16px; font-weight:700; line-height:1.45; }

#s-rageng .twocol{ position:absolute; left:70px; top:552px; width:1780px; display:flex; gap:40px; }
#s-rageng .col{ flex:1; min-height:300px; display:flex; flex-direction:column; background:#fff; border-radius:20px; box-shadow:0 20px 46px rgba(0,0,0,.14);
  padding:30px 34px; }
#s-rageng .col .chead{ flex:none; display:flex; align-items:center; gap:10px; margin-bottom:22px; }
#s-rageng .col .chead .cdot{ flex:none; width:15px; height:15px; border-radius:50%; }
#s-rageng .col.policy .chead .cdot{ background:#2F6FED; }
#s-rageng .col.fin .chead .cdot{ background:#007613; }
#s-rageng .col .chead .ctitle{ color:#171717; font-size:22px; font-weight:800; }

#s-rageng .citems{ flex:1; display:flex; flex-direction:column; justify-content:space-between; }
#s-rageng .sitem2{ display:flex; align-items:center; gap:18px; }
#s-rageng .sitem2 .snum2{ flex:none; width:42px; height:42px; border-radius:50%; color:#fff;
  display:flex; align-items:center; justify-content:center; font-size:16px; font-weight:800; }
#s-rageng .col.policy .snum2{ background:#2F6FED; }
#s-rageng .col.fin .snum2{ background:#007613; }
#s-rageng .sitem2 .stitle2{ color:#171717; font-size:26px; font-weight:800; line-height:1.3; }

#s-rageng .note{ position:absolute; left:70px; top:918px; width:1780px; background:#fff; border-radius:14px;
  box-shadow:0 10px 24px rgba(0,0,0,.08); padding:14px 28px; color:#444; font-size:15px; font-weight:600;
  display:flex; align-items:center; gap:12px; }
#s-rageng .note .dot{ flex:none; width:10px; height:10px; border-radius:50%; background:#2D8A4E; }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s-rageng">
  <div class="ptitle">RAG 품질을 위한 엔지니어링</div>
  <div class="psub">정책과 금융상품, 두 RAG 도메인에서 데이터 특성에 맞춘 설계를 적용했습니다.</div>

  <div class="pipeline">
    <div class="plabel">새벽 배치 파이프라인</div>

    <div class="prow r1">
      <div class="ptime">04:00</div>
      <div class="pbody">
        <div class="pname">정책·금융상품 수집 배치 (PolicyDailySyncScheduler · CollectionScheduler)</div>
        <div class="pdesc">정책 API 재수집과 Finlife 예금·적금·대출 수집이 같은 시각에 나란히 실행됩니다.</div>
      </div>
    </div>

    <div class="prow r2">
      <div class="ptime">05:00</div>
      <div class="pbody">
        <div class="pname">금융상품 변경 감지 배치 (FinancialChangeDetectionScheduler)</div>
        <div class="pdesc">전일 수집 결과와 비교해 변경된 상품만 재처리합니다.</div>
      </div>
    </div>

    <div class="pnote">정책·상품 수집이 끝나기 전에 변경 감지가 돌면 어제 데이터와 비교하게 되어 변경분을 놓칩니다.<br>그래서 두 배치의 실행 시각을 1시간 차이로 설계해 순서를 보장했습니다.</div>
  </div>

  <div class="twocol">
    <div class="col policy">
      <div class="chead"><div class="cdot"></div><div class="ctitle">정책 도메인</div></div>

      <div class="citems">
      <div class="sitem2"><div class="snum2">01</div><div class="stitle2">필드 기반 구조화 청킹</div></div>
      <div class="sitem2"><div class="snum2">02</div><div class="stitle2">SHA-256 해시 변경 감지</div></div>
      <div class="sitem2"><div class="snum2">03</div><div class="stitle2">지역 텍스트 정규화 매칭</div></div>
      </div>
    </div>

    <div class="col fin">
      <div class="chead"><div class="cdot"></div><div class="ctitle">금융상품 도메인</div></div>

      <div class="citems">
      <div class="sitem2"><div class="snum2">01</div><div class="stitle2">상품 유형별 청킹 템플릿</div></div>
      <div class="sitem2"><div class="snum2">02</div><div class="stitle2">우대조건 LLM 구조화 파싱</div></div>
      <div class="sitem2"><div class="snum2">03</div><div class="stitle2">점수 고정 + LLM은 설명만</div></div>
      </div>
    </div>
  </div>

  <div class="note"><div class="dot"></div>정책은 SHA-256 해시로, 금융상품은 결정적 문서 ID(UUID)로 — 방식은 다르지만 재실행해도 중복 없이 안전하게 재처리되도록 설계했습니다.</div>
</section>
`);
