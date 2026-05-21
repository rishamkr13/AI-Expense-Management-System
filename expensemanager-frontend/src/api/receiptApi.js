import axios from "axios";

const API_BASE_URL = `${import.meta.env.VITE_API_BASE_URL}/api/receipts`;

const getAuthHeaders = () => {
  const token = localStorage.getItem("token");

  return {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  };
};

export const uploadReceipt = (file) => {
  const formData = new FormData();
  formData.append("file", file);

  const token = localStorage.getItem("token");

  return axios.post(`${API_BASE_URL}/upload`, formData, {
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "multipart/form-data",
    },
  });
};

export const processReceipt = (receiptId) => {
  return axios.post(`${API_BASE_URL}/${receiptId}/process`, {}, getAuthHeaders());
};

export const getReceipts = () => {
  return axios.get(API_BASE_URL, getAuthHeaders());
};

export const deleteReceipt = (receiptId) => {
  return axios.delete(`${API_BASE_URL}/${receiptId}`, getAuthHeaders());
};

export const confirmReceipt = (receiptId, data) => {
  return axios.post(`${API_BASE_URL}/${receiptId}/confirm`, data, getAuthHeaders());
};