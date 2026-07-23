import { useEffect, useState } from "react";
import { getPopularProducts } from "../../../api/financialSearchApi";

const REFRESH_MS = 30000;

/**
 * 실시간 인기 금융상품(북마크 많은 순). 30초마다 자동 재조회한다(언마운트 시 clearInterval).
 * 부가 위젯이라 실패하면 조용히 빈 목록으로 둔다(엔드포인트 오류여도 추천 화면은 정상 동작).
 * version은 갱신될 때마다 증가해 위젯의 갱신 애니메이션을 트리거한다.
 */
export const usePopularProducts = (limit = 5) => {
  const [items, setItems] = useState([]);
  const [version, setVersion] = useState(0);

  useEffect(() => {
    let active = true;

    const fetchOnce = () => {
      getPopularProducts(limit)
        .then((data) => {
          if (active) {
            setItems(data || []);
            setVersion((prev) => prev + 1);
          }
        })
        .catch(() => {
          // 인기 상품을 못 받아와도 화면은 그대로 두고 위젯만 숨긴다.
        });
    };

    fetchOnce();
    const timer = setInterval(fetchOnce, REFRESH_MS);

    return () => {
      active = false;
      clearInterval(timer);
    };
  }, [limit]);

  return { items, version };
};
