import axiosInstance from "./axiosInstance";

const responseData = (response) => response.data?.data;

/** 상품 id로 등록 폼 초안(기본금리·기간·우대조건 파싱)을 불러온다. */
export const getSubscriptionDraft = (productId) =>
  axiosInstance
    .get("/api/savings-subscriptions/draft", { params: { productId } })
    .then(responseData);

/** 가입한 예·적금 등록. productId 없이 상품명·회사명 직접 입력도 가능. */
export const createSubscription = (payload) =>
  axiosInstance.post("/api/savings-subscriptions", payload).then(responseData);

/** 내가 등록한 가입 예·적금 목록(만기예상·미충족조건 수 포함). */
export const getSubscriptions = () =>
  axiosInstance.get("/api/savings-subscriptions").then(responseData);

/** 우대조건 충족 여부 토글. */
export const updateSubscriptionCondition = (conditionId, met) =>
  axiosInstance.patch(`/api/savings-subscriptions/conditions/${conditionId}`, {
    met,
  });

/** 가입 예·적금 삭제. */
export const deleteSubscription = (id) =>
  axiosInstance.delete(`/api/savings-subscriptions/${id}`);
