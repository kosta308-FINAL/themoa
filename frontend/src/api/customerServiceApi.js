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

// 고객센터 AI 품질관리 - 관리자
export const getAdminCustomerAiSettings = () =>
  axiosInstance
    .get("/api/admin/customer-service/ai-quality/settings")
    .then(responseData);

export const updateAdminCustomerAiSettings = (request) =>
  axiosInstance
    .put("/api/admin/customer-service/ai-quality/settings", request)
    .then(responseData);

export const searchAdminCustomerAiKnowledge = (request) =>
  axiosInstance
    .post("/api/admin/customer-service/ai-quality/search", request)
    .then(responseData);

export const previewAdminCustomerAiAnswer = (request) =>
  axiosInstance
    .post("/api/admin/customer-service/ai-quality/preview", request)
    .then(responseData);

export const getAdminCustomerKnowledgeDocuments = () =>
  axiosInstance
    .get("/api/admin/customer-service/ai-quality/documents")
    .then(responseData);

export const getAdminCustomerKnowledgeMetadataOptions = () =>
  axiosInstance
    .get("/api/admin/customer-service/ai-quality/metadata-options")
    .then(responseData);

export const uploadAdminCustomerKnowledgeDocument = ({
  title,
  category,
  file,
  chunkMaxLength,
  chunkOverlapLength,
  splitByMarkdownHeading,
}) => {
  const formData = new FormData();
  formData.append("title", title);
  formData.append("category", category);
  formData.append("chunkMaxLength", chunkMaxLength);
  formData.append("chunkOverlapLength", chunkOverlapLength);
  formData.append("splitByMarkdownHeading", splitByMarkdownHeading);
  formData.append("file", file);
  return axiosInstance
    .post("/api/admin/customer-service/ai-quality/documents", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    })
    .then(responseData);
};

export const previewAdminCustomerKnowledgeChunks = (request) =>
  axiosInstance
    .post(
      "/api/admin/customer-service/ai-quality/documents/chunk-preview",
      request,
    )
    .then(responseData);

export const createAdminCustomerKnowledgeText = (request) =>
  axiosInstance
    .post("/api/admin/customer-service/ai-quality/documents/text", request)
    .then(responseData);

export const reembedAdminCustomerKnowledgeDocument = (documentId) =>
  axiosInstance
    .post(
      `/api/admin/customer-service/ai-quality/documents/${documentId}/reembed`,
    )
    .then(responseData);

export const disableAdminCustomerKnowledgeDocument = (documentId) =>
  axiosInstance
    .delete(`/api/admin/customer-service/ai-quality/documents/${documentId}`)
    .then(responseData);
