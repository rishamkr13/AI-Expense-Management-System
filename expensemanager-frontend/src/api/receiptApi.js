import API from "./axios";

export const uploadReceipt = (file) => {
  const formData = new FormData();
  formData.append("file", file);

  return API.post("/api/receipts/upload", formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
};

export const processReceipt = (receiptId) => {
  return API.post(`/api/receipts/${receiptId}/process`, {});
};

export const getReceipts = () => {
  return API.get("/api/receipts");
};

export const deleteReceipt = (receiptId) => {
  return API.delete(`/api/receipts/${receiptId}`);
};

export const confirmReceipt = (receiptId, data) => {
  return API.post(`/api/receipts/${receiptId}/confirm`, data);
};