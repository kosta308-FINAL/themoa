import axiosInstance from "./axiosInstance";

const responseData = (response) => response.data?.data;

/**
 * 수집·임베딩은 외부 금융 API 전체를 훑기 때문에 수십 초~수 분이 걸린다.
 * 기본 설정으로 두면 중간에 끊길 수 있어 이 두 요청에만 넉넉한 타임아웃을 준다.
 */
const LONG_RUNNING_TIMEOUT_MS = 5 * 60 * 1000;

/** 금융감독원 API에서 예·적금/대출 상품을 수집해 DB에 반영한다(ADMIN 전용). */
export const collectFinancialProducts = () =>
  axiosInstance
    .post("/api/admin/financial-products/collect", undefined, {
      timeout: LONG_RUNNING_TIMEOUT_MS,
    })
    .then(responseData);

/** 수집된 상품을 검색용 벡터 인덱스에 다시 임베딩한다. 응답은 {embeddedCount, completedAt}. */
export const rebuildFinancialEmbeddings = () =>
  axiosInstance
    .post("/api/financial-products/embeddings/rebuild", undefined, {
      timeout: LONG_RUNNING_TIMEOUT_MS,
    })
    .then(responseData);

const SEARCH_SETTINGS_URL = "/api/admin/financial-products/search/settings";

/** 검색 튜닝값(결과 개수·후보 개수·유사도 임계값) 현재 상태. */
export const getSearchSettings = () =>
  axiosInstance.get(SEARCH_SETTINGS_URL).then(responseData);

/**
 * 검색 튜닝값 변경. 항목을 null로 보내면 그 항목만 서버 기본값으로 되돌아간다.
 * 응답은 실제로 적용된 값이라 그대로 화면에 반영하면 된다.
 */
export const updateSearchSettings = ({ topK, retryTopK, minimumSimilarity }) =>
  axiosInstance
    .put(SEARCH_SETTINGS_URL, { topK, retryTopK, minimumSimilarity })
    .then(responseData);

/** 검색 튜닝값을 서버 기본값으로 되돌린다. 응답은 되돌아간 기본값. */
export const resetSearchSettings = () =>
  axiosInstance.delete(SEARCH_SETTINGS_URL).then(responseData);

/**
 * 검색어가 어떻게 해석되고 어떤 후보가 어떤 점수로 걸렸는지 진단한다.
 * 검색 결과가 이상할 때 원인을 숫자로 확인하는 용도.
 */
export const explainFinancialSearch = (query) =>
  axiosInstance
    .post("/api/admin/financial-products/search/explain", { query })
    .then(responseData);

/** 상품 수·마지막 수집 시각·벡터 인덱스·은행 링크 현황. */
export const getFinancialProductStatus = () =>
  axiosInstance.get("/api/admin/financial-products/status").then(responseData);

const BANK_LINKS_URL = "/api/admin/financial-products/bank-links";

/**
 * 은행 공식 링크 목록. links는 등록된 것, companiesWithoutLink는 아직 링크가 없어
 * 검색 링크로 대체되고 있는 회사명이다.
 */
export const getBankLinks = () =>
  axiosInstance.get(BANK_LINKS_URL).then(responseData);

/** 은행 공식 링크 등록·수정. 회사명이 식별자라 같은 이름이면 URL만 갱신된다. */
export const saveBankLink = ({ companyName, officialUrl }) =>
  axiosInstance.put(BANK_LINKS_URL, { companyName, officialUrl });

/** 은행 공식 링크 삭제. 회사명이 경로에 들어가므로 인코딩해서 보낸다. */
export const deleteBankLink = (companyName) =>
  axiosInstance.delete(`${BANK_LINKS_URL}/${encodeURIComponent(companyName)}`);
