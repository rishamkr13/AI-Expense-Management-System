import axios from "axios";

const API_BASE_URL = "http://localhost:8080/api/dashboard";

const getAuthHeaders = () => {
  const token = localStorage.getItem("token");

  if (!token) {
    throw new Error("No token found. Please login again.");
  }

  return {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  };
};

export const getDashboardSummary = () => {
  return axios.get(`${API_BASE_URL}/summary`, getAuthHeaders());
};

// FIXED: was /category-wise → now /categories (matches backend)
export const getCategoryWiseExpense = () => {
  return axios.get(`${API_BASE_URL}/categories`, getAuthHeaders());
};

// FIXED: was /monthly-trend → now /monthly (matches backend)
export const getMonthlyTrend = () => {
  return axios.get(`${API_BASE_URL}/monthly`, getAuthHeaders());
};

// FIXED: was /recent-transactions → now /recent (matches backend)
export const getRecentTransactions = () => {
  return axios.get(`${API_BASE_URL}/recent`, getAuthHeaders());
};

// FIXED: was /budget-status → now /budget (matches backend)
export const getBudgetStatus = () => {
  return axios.get(`${API_BASE_URL}/budget`, getAuthHeaders());
};