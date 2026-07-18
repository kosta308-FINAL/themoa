import axiosInstance from './axiosInstance'

const responseData = (response) => response.data?.data

export const getSpendingGuideSummary = () =>
  axiosInstance.get('/api/spending-guide/summary').then(responseData)

export const setupSpendingGuide = (payload) =>
  axiosInstance.put('/api/spending-guide/setup', payload).then(responseData)

export const updateSalary = (payload) =>
  axiosInstance.patch('/api/spending-guide/salary', payload)

export const updateSavingsGoal = (payload) =>
  axiosInstance.patch('/api/spending-guide/savings-goal', payload)

export const getTodayTransactions = (limit = 5) =>
  axiosInstance.get('/api/spending-guide/transactions/today', { params: { limit } }).then(responseData)

export const getSpendingTransactions = (params) =>
  axiosInstance.get('/api/spending-guide/transactions', { params }).then(responseData)

export const getTransactionDetail = (transactionId) =>
  axiosInstance.get(`/api/card-transactions/${transactionId}`).then(responseData)

export const syncCardTransactions = () =>
  axiosInstance.post('/api/card-transactions/sync', null, { params: { manual: true } }).then(responseData)

export const updateTransactionCategory = (transactionId, categoryId) =>
  axiosInstance.patch(`/api/card-transactions/${transactionId}/category`, { categoryId })

export const updateTransactionMemo = (transactionId, memo) =>
  axiosInstance.patch(`/api/card-transactions/${transactionId}/memo`, { memo })

export const correctTransactionCanceledAmount = (transactionId, canceledAmount) =>
  axiosInstance.patch(`/api/card-transactions/${transactionId}/canceled-amount`, { canceledAmount })

export const correctTransactionAmount = (transactionId, amount) =>
  axiosInstance.patch(`/api/card-transactions/${transactionId}/amount`, { amount })

export const getRecentDays = () =>
  axiosInstance.get('/api/spending-guide/recent-days', { params: { days: 7 } }).then(responseData)

export const getCategorySummary = () =>
  axiosInstance.get('/api/card-transactions/category-summary').then(responseData)

export const getFixedExpenseCandidates = () =>
  axiosInstance.get('/api/fixed-expense-candidates').then(responseData)

export const getCoachingCards = () =>
  axiosInstance.get('/api/spending-guide/coaching-cards').then(responseData)

export const dismissCoachingCard = (cardId, dismissType) =>
  axiosInstance.post(`/api/spending-guide/coaching-cards/${cardId}/dismiss`, { dismissType })

export const getCategories = () =>
  axiosInstance.get('/api/categories').then(responseData)

export const createManualTransaction = (payload) =>
  axiosInstance.post('/api/manual-transactions', payload).then(responseData)

export const updateManualTransaction = (transactionId, payload) =>
  axiosInstance.patch(`/api/manual-transactions/${transactionId}`, payload).then(responseData)

export const deleteManualTransaction = (transactionId) =>
  axiosInstance.delete(`/api/manual-transactions/${transactionId}`)

export const getCardConnections = () =>
  axiosInstance.get('/api/card-connections').then(responseData)

export const getCardIssuers = () =>
  axiosInstance.get('/api/card-issuers').then(responseData)

export const getInitialSyncStatus = () =>
  axiosInstance.get('/api/card-connections/initial-sync-status').then(responseData)

export const retryInitialSync = (connectionId) =>
  axiosInstance.post(`/api/card-connections/${connectionId}/initial-sync/retry`).then(responseData)

export const createCardConnection = (payload) =>
  axiosInstance.post('/api/card-connections', payload).then(responseData)

export const updateCardSyncEnabled = (enabled) =>
  axiosInstance.patch('/api/card-connections/sync-enabled', { enabled })
