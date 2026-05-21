import axios from "axios";

const API_BASE_URL = `${import.meta.env.VITE_API_BASE_URL}/api/dashboard`;

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

export const getCategoryWiseExpense = () => {
  return axios.get(`${API_BASE_URL}/categories`, getAuthHeaders());
};

export const getMonthlyTrend = () => {
  return axios.get(`${API_BASE_URL}/monthly`, getAuthHeaders());
};

export const getRecentTransactions = () => {
  return axios.get(`${API_BASE_URL}/recent`, getAuthHeaders());
};

export const getBudgetStatus = () => {
  return axios.get(`${API_BASE_URL}/budget`, getAuthHeaders());
};