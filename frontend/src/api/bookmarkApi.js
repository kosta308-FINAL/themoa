import axiosInstance from "./axiosInstance";

const responseData = (response) => response.data?.data;

/** 북마크 등록. 이미 저장돼 있으면 백엔드가 새로 만들지 않고 200으로 응답한다. */
export const addBookmark = ({ targetType, targetId }) =>
  axiosInstance.post("/api/bookmarks", { targetType, targetId });

/** 북마크 해제. 등록과 달리 쿼리 파라미터로 대상을 지정한다(응답 본문 없음). */
export const removeBookmark = ({ targetType, targetId }) =>
  axiosInstance.delete("/api/bookmarks", { params: { targetType, targetId } });

/** 마이페이지용 저장 목록(상세 포함, 최근순). */
export const getBookmarks = () =>
  axiosInstance.get("/api/bookmarks").then(responseData);

/** 목록 화면에서 어떤 상품의 북마크를 채울지 판단하는 가벼운 목록. */
export const getBookmarkTargets = () =>
  axiosInstance.get("/api/bookmarks/targets").then(responseData);
