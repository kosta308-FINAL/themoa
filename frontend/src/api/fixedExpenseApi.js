import axiosInstance from "./axiosInstance";

const responseData = (response) => response.data?.data;

export const getFixedExpenses = () =>
  axiosInstance.get("/api/fixed-expenses").then(responseData);

export const registerFixedExpenseDirect = (payload) =>
  axiosInstance.post("/api/fixed-expenses", payload).then(responseData);

export const registerFixedExpenseFromCandidate = (candidateId, payload) =>
  axiosInstance
    .post(`/api/fixed-expenses/from-candidate/${candidateId}`, payload)
    .then(responseData);

export const updateFixedExpense = (fixedExpenseId, payload) =>
  axiosInstance.patch(`/api/fixed-expenses/${fixedExpenseId}`, payload);

export const cancelFixedExpense = (fixedExpenseId) =>
  axiosInstance.delete(`/api/fixed-expenses/${fixedExpenseId}`);

export const getMissedPaymentCandidates = (fixedExpenseId) =>
  axiosInstance
    .get(`/api/fixed-expenses/${fixedExpenseId}/missed-payment-candidates`)
    .then(responseData);

export const confirmMissedPayment = (fixedExpenseId, transactionId) =>
  axiosInstance.post(
    `/api/fixed-expenses/${fixedExpenseId}/missed-payment-candidates/${transactionId}/confirm`,
  );

export const rejectFixedExpenseCandidate = (candidateId) =>
  axiosInstance.post(`/api/fixed-expense-candidates/${candidateId}/reject`);

export const snoozeFixedExpenseCandidate = (candidateId) =>
  axiosInstance.post(`/api/fixed-expense-candidates/${candidateId}/snooze`);
