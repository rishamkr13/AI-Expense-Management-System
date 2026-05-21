import API from "./axiosconfig";

export const getDashboardSummary = () => {
  return API.get("/api/dashboard/summary");
};

export const getCategoryWiseExpense = () => {
  return API.get("/api/dashboard/categories");
};

export const getMonthlyTrend = () => {
  return API.get("/api/dashboard/monthly");
};

export const getRecentTransactions = () => {
  return API.get("/api/dashboard/recent");
};

export const getBudgetStatus = () => {
  return API.get("/api/dashboard/budget");
};