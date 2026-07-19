import axiosInstance from './axiosInstance'

const responseData = (response) => response.data?.data

export const searchPolicies = ({ query, page = 0, size = 10 }) =>
  axiosInstance.post('/api/policies/search', { query, page, size }).then(responseData)

export const getPolicyDetail = (policyId) =>
  axiosInstance.get(`/api/policies/${policyId}`).then(responseData)

export const getPolicyAdminDashboard = () =>
  axiosInstance.get('/api/policies/admin/dashboard').then(responseData)

export const getPolicyAdminStatus = () =>
  axiosInstance.get('/api/policies/admin/status').then(responseData)

export const getPolicyEmbeddingHistory = ({ status, keyword, page = 0, size = 20 } = {}) =>
  axiosInstance.get('/api/policies/admin/embeddings', {
    params: { status: status || undefined, keyword: keyword || undefined, page, size },
  }).then(responseData)

export const getPolicyLatestJob = () =>
  axiosInstance.get('/api/policies/admin/jobs/latest').then(responseData)

export const getPolicyJob = (jobId) =>
  axiosInstance.get(`/api/policies/admin/jobs/${jobId}`).then(responseData)

export const startPolicyJob = (jobKey) =>
  axiosInstance.post(`/api/policies/admin/jobs/${jobKey}`).then(responseData)
