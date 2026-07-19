import axiosInstance from './axiosInstance'

const responseData = (response) => response.data?.data
const policyLocalToolsEnabled =
  import.meta.env.DEV && import.meta.env.VITE_POLICY_LOCAL_TOOLS_ENABLED === 'true'
const localPolicyRequestConfig = policyLocalToolsEnabled ? { skipAuth: true } : undefined

export const searchPolicies = ({ query, page = 0, size = 10 }) =>
  axiosInstance.post('/api/policies/search', { query, page, size }, localPolicyRequestConfig).then(responseData)

export const getPolicyDetail = (policyId) =>
  axiosInstance.get(`/api/policies/${policyId}`, localPolicyRequestConfig).then(responseData)

export const getPolicyAdminDashboard = () =>
  axiosInstance.get('/api/policies/admin/dashboard', localPolicyRequestConfig).then(responseData)

export const getPolicyAdminStatus = () =>
  axiosInstance.get('/api/policies/admin/status', localPolicyRequestConfig).then(responseData)

export const getPolicyEmbeddingHistory = ({ status, keyword, page = 0, size = 20 } = {}) =>
  axiosInstance.get('/api/policies/admin/embeddings', {
    ...localPolicyRequestConfig,
    params: { status: status || undefined, keyword: keyword || undefined, page, size },
  }).then(responseData)

export const getPolicyLatestJob = () =>
  axiosInstance.get('/api/policies/admin/jobs/latest', localPolicyRequestConfig).then(responseData)

export const getPolicyJob = (jobId) =>
  axiosInstance.get(`/api/policies/admin/jobs/${jobId}`, localPolicyRequestConfig).then(responseData)

export const startPolicyJob = (jobKey) =>
  axiosInstance.post(`/api/policies/admin/jobs/${jobKey}`, undefined, localPolicyRequestConfig).then(responseData)
