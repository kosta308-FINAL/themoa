import axiosInstance from "./axiosInstance";

const responseData = (response) => response.data?.data;

// FAQ
export const getFaqs = (params) =>
  axiosInstance.get("/api/faqs", { params }).then(responseData);

export const putFaqFeedback = (faqId, helpful) =>
  axiosInstance.put(`/api/faqs/${faqId}/feedback`, { helpful });

export const askCustomerServiceChat = (request) =>
  axiosInstance.post("/api/customer-service/chat", request).then(responseData);

// 1:1 문의 - 사용자
export const getInquiryCategories = () =>
  axiosInstance.get("/api/inquiry-categories").then(responseData);

export const createInquiry = (request, files) => {
  const formData = new FormData();
  formData.append(
    "request",
    new Blob([JSON.stringify(request)], { type: "application/json" }),
  );
  (files || []).forEach((file) => formData.append("files", file));
  return axiosInstance
    .post("/api/inquiries", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    })
    .then(responseData);
};

export const getMyInquiries = (params) =>
  axiosInstance.get("/api/inquiries", { params }).then(responseData);

export const getMyInquiryDetail = (inquiryId) =>
  axiosInstance.get(`/api/inquiries/${inquiryId}`).then(responseData);

export const downloadMyInquiryAttachment = (inquiryId, attachmentId) =>
  axiosInstance
    .get(`/api/inquiries/${inquiryId}/attachments/${attachmentId}`, {
      responseType: "blob",
    })
    .then((response) => response.data);

// 1:1 문의 - 관리자
export const getAdminInquiries = (params) =>
  axiosInstance.get("/api/admin/inquiries", { params }).then(responseData);

export const getAdminInquiryDetail = (inquiryId) =>
  axiosInstance.get(`/api/admin/inquiries/${inquiryId}`).then(responseData);

export const downloadAdminInquiryAttachment = (inquiryId, attachmentId) =>
  axiosInstance
    .get(`/api/admin/inquiries/${inquiryId}/attachments/${attachmentId}`, {
      responseType: "blob",
    })
    .then((response) => response.data);

export const upsertAdminInquiryAnswer = (inquiryId, contentMarkdown, version) =>
  axiosInstance
    .put(`/api/admin/inquiries/${inquiryId}/answer`, {
      contentMarkdown,
      version,
    })
    .then(responseData);
