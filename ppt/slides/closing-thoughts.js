/* ===================== 마무리 소감 (팀원별 3분할) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s-closing{ background:transparent; }
#s-closing .ptitle{ position:absolute; left:52px; top:40px; width:1500px; color:#6B7C8A; font-size:45px; font-weight:800; }
#s-closing .psub{ position:absolute; left:52px; top:112px; width:1780px; color:#111; font-size:22px; font-weight:500; line-height:1.5; }

#s-closing .cols{ position:absolute; left:70px; top:220px; width:1780px; display:flex; gap:32px; }
#s-closing .ccol{ flex:1; background:#fff; border-radius:24px; box-shadow:0 20px 46px rgba(0,0,0,.14);
  padding:36px 34px; display:flex; flex-direction:column; min-height:700px; }

#s-closing .chead{ display:flex; align-items:center; gap:16px; margin-bottom:8px; }
#s-closing .cavatar{ flex:none; width:60px; height:60px; border-radius:50%; color:#fff;
  display:flex; align-items:center; justify-content:center; font-size:20px; font-weight:800; }
#s-closing .p1 .cavatar{ background:#2F6FED; }
#s-closing .p2 .cavatar{ background:#007613; }
#s-closing .p3 .cavatar{ background:#7C3AED; }
#s-closing .cname{ font-size:25px; font-weight:800; color:#171717; }

#s-closing .cquote{ font-size:52px; font-weight:800; margin:18px 0 4px; line-height:1; }
#s-closing .p1 .cquote{ color:#2F6FED; }
#s-closing .p2 .cquote{ color:#007613; }
#s-closing .p3 .cquote{ color:#7C3AED; }

#s-closing .ctext{ flex:1; font-size:17px; color:#333; font-weight:500; line-height:1.75; }

#s-closing .placeholder{ flex:1; border:2px dashed #D8DEE5; border-radius:14px;
  display:flex; align-items:center; justify-content:center; text-align:center;
  color:#9AA5B1; font-size:16px; font-weight:700; line-height:1.7; padding:20px; }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s-closing">
  <div class="ptitle">마무리 소감</div>
  <div class="psub">짧은 시간 동안 함께 만든 프로젝트를 마치며, 각자의 소감을 남깁니다.</div>

  <div class="cols">
    <div class="ccol p1">
      <div class="chead"><div class="cavatar">솔민</div><div class="cname">김솔민</div></div>
      <div class="cquote">&ldquo;</div>
      <div class="placeholder">여기에 김솔민님의 소감을<br>채워주세요.</div>
    </div>

    <div class="ccol p2">
      <div class="chead"><div class="cavatar">호연</div><div class="cname">문호연</div></div>
      <div class="cquote">&ldquo;</div>
      <div class="placeholder">여기에 문호연님의 소감을<br>채워주세요.</div>
    </div>

    <div class="ccol p3">
      <div class="chead"><div class="cavatar">수지</div><div class="cname">임수지</div></div>
      <div class="cquote">&ldquo;</div>
      <div class="placeholder">여기에 임수지님의 소감을<br>채워주세요.</div>
    </div>
  </div>
</section>
`);
