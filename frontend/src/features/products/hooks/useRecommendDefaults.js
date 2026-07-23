import { useEffect, useState } from "react";
import { getRecommendDefaults } from "../../../api/productsApi";

/**
 * 추천 입력 폼의 서버 기본값(월소득·월 납입가능금액)을 한 번 받아온다.
 * 폼은 이 값이 준비된 뒤에 마운트되므로, 여기서는 로딩 완료 여부만 알려주면 된다.
 * 실패해도 폼 자체는 떠야 하므로 defaults를 null로 두고 화면에서 자체 기본값을 쓴다.
 */
export const useRecommendDefaults = () => {
  const [defaults, setDefaults] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    getRecommendDefaults()
      .then((data) => {
        if (active) {
          setDefaults(data || null);
        }
      })
      .catch(() => {
        // 기본값을 못 받아와도 폼은 직접 입력으로 쓸 수 있어야 하므로 조용히 넘어간다.
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, []);

  return { defaults, loading };
};
