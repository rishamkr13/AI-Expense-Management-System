import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  ArrowLeft,
  Upload,
  FileText,
  Trash2,
  RefreshCcw,
  CheckCircle,
  AlertCircle,
  Wand2,
  ClipboardCheck,
  X,
} from "lucide-react";

import api from "../api/axiosConfig";
import { formatCurrency } from "../utils/formatCurrency";

const CATEGORIES = [
  "FOOD", "TRAVEL", "SHOPPING", "BILLS",
  "HEALTH", "EDUCATION", "ENTERTAINMENT", "OTHER",
];

const PAYMENT_MODES = [
  "CASH", "UPI", "DEBIT_CARD", "CREDIT_CARD",
  "NET_BANKING", "WALLET", "OTHER",
];

const Receipts = () => {
  const [receipts, setReceipts] = useState([]);
  const [singleFile, setSingleFile] = useState(null);
  const [bulkFiles, setBulkFiles] = useState([]);
  const [message, setMessage] = useState("");
  const [messageType, setMessageType] = useState("success"); // "success" | "error"
  const [loading, setLoading] = useState(false);

  // Confirm modal state
  const [confirmModal, setConfirmModal] = useState(false);
  const [selectedReceipt, setSelectedReceipt] = useState(null);
  const [confirmForm, setConfirmForm] = useState({
    amount: "",
    merchantName: "",
    expenseDate: "",
    category: "OTHER",
  });
  const [confirmLoading, setConfirmLoading] = useState(false);

  const showMessage = (text, type = "success") => {
    setMessage(text);
    setMessageType(type);
    setTimeout(() => setMessage(""), 4000);
  };

  const fetchReceipts = async () => {
    try {
      setLoading(true);
      const response = await api.get("/receipts");
      setReceipts(response.data || []);
    } catch (error) {
      console.error("Fetch receipts error:", error);
      showMessage("Failed to load receipts", "error");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReceipts();
  }, []);

  // ── SINGLE UPLOAD ──
  const handleSingleUpload = async (e) => {
    e.preventDefault();
    setMessage("");

    if (!singleFile) {
      showMessage("Please select a receipt file", "error");
      return;
    }

    const formData = new FormData();
    formData.append("file", singleFile);

    try {
      setLoading(true);
      const response = await api.post("/receipts/upload", formData, {
        headers: { "Content-Type": "multipart/form-data" },
      });

      const uploadedReceipt = response.data;

      if (uploadedReceipt?.id) {
        await api.post(`/receipts/${uploadedReceipt.id}/process`);
      }

      showMessage("Receipt uploaded and OCR processed! Now click Confirm to save expense ✅");
      setSingleFile(null);
      await fetchReceipts();
    } catch (error) {
      console.error("Single receipt upload error:", error);
      showMessage("Failed to upload/process receipt", "error");
    } finally {
      setLoading(false);
    }
  };

  // ── BULK UPLOAD ──
  const handleBulkUpload = async (e) => {
    e.preventDefault();
    setMessage("");

    if (bulkFiles.length === 0) {
      showMessage("Please select receipt files", "error");
      return;
    }

    const formData = new FormData();
    bulkFiles.forEach((file) => formData.append("files", file));

    try {
      setLoading(true);
      const response = await api.post("/receipts/bulk-upload", formData, {
        headers: { "Content-Type": "multipart/form-data" },
      });

      const uploadedReceipts = response.data || [];

      for (const receipt of uploadedReceipts) {
        if (receipt?.id) {
          await api.post(`/receipts/${receipt.id}/process`);
        }
      }

      showMessage(`${uploadedReceipts.length} receipts uploaded and OCR processed ✅`);
      setBulkFiles([]);
      await fetchReceipts();
    } catch (error) {
      console.error("Bulk receipt upload error:", error);
      showMessage("Failed to upload/process bulk receipts", "error");
    } finally {
      setLoading(false);
    }
  };

  // ── RE-RUN OCR ──
  const handleProcessReceipt = async (id) => {
    try {
      setLoading(true);
      setMessage("");
      const res = await api.post(`/receipts/${id}/process`);
      showMessage("OCR processed! Click Confirm to review and save ✅");
      await fetchReceipts();

      // Auto-open confirm modal after re-processing
      const updated = await api.get(`/receipts/${id}`);
      openConfirmModal(updated.data);
    } catch (error) {
      console.error("Process receipt error:", error);
      showMessage("Failed to process receipt", "error");
    } finally {
      setLoading(false);
    }
  };

  // ── DELETE ──
  const handleDeleteReceipt = async (id) => {
    if (!window.confirm("Delete this receipt?")) return;
    try {
      setLoading(true);
      await api.delete(`/receipts/${id}`);
      showMessage("Receipt deleted");
      await fetchReceipts();
    } catch (error) {
      console.error("Delete receipt error:", error);
      showMessage("Failed to delete receipt", "error");
    } finally {
      setLoading(false);
    }
  };

  // ── OPEN CONFIRM MODAL ──
  const openConfirmModal = (receipt) => {
    setSelectedReceipt(receipt);
    setConfirmForm({
      amount: receipt.amount || "",
      merchantName: receipt.merchantName || "",
      expenseDate: receipt.expenseDate || new Date().toISOString().split("T")[0],
      category: receipt.category || "OTHER",
    });
    setConfirmModal(true);
  };

  // ── SUBMIT CONFIRM ──
  const handleConfirmSubmit = async (e) => {
    e.preventDefault();

    if (!confirmForm.amount || Number(confirmForm.amount) <= 0) {
      showMessage("Please enter a valid amount", "error");
      return;
    }

    try {
      setConfirmLoading(true);

      await api.post(`/receipts/${selectedReceipt.id}/confirm`, {
        amount: parseFloat(confirmForm.amount),
        merchantName: confirmForm.merchantName,
        expenseDate: confirmForm.expenseDate,
        category: confirmForm.category,
      });

      showMessage("Expense saved successfully! Dashboard will update ✅");
      setConfirmModal(false);
      setSelectedReceipt(null);
      await fetchReceipts();
    } catch (error) {
      console.error("Confirm receipt error:", error);
      showMessage("Failed to confirm receipt", "error");
    } finally {
      setConfirmLoading(false);
    }
  };

  // ── STATUS BADGE ──
  const getStatusBadge = (status) => {
    const s = status || "UPLOADED";
    if (s === "PROCESSED" || s === "SUCCESS") {
      return (
        <span className="status-badge success">
          <CheckCircle size={14} /> {s}
        </span>
      );
    }
    if (s === "PROCESSING") {
      return <span className="status-badge" style={{ background: "#fef9c3", color: "#854d0e" }}>⏳ PROCESSING</span>;
    }
    return (
      <span className="status-badge failed">
        <AlertCircle size={14} /> {s}
      </span>
    );
  };

  const totalExtractedAmount = receipts.reduce(
    (sum, r) => sum + Number(r.amount || 0),
    0
  );

  return (
    <div className="page-container">

      {/* ── PAGE HEADER ── */}
      <div className="page-header">
        <div>
          <Link to="/dashboard" className="back-link">
            <ArrowLeft size={18} /> Back to Dashboard
          </Link>
          <h1>Receipt OCR Management</h1>
          <p>Upload receipts → OCR extracts data → You confirm → Expense saved</p>
        </div>

        <div className="total-box receipt-total">
          <FileText size={22} />
          <div>
            <span>Total Extracted</span>
            <strong>{formatCurrency(totalExtractedAmount)}</strong>
          </div>
        </div>
      </div>

      {/* ── MESSAGE ── */}
      {message && (
        <div
          className="message-box"
          style={{
            background: messageType === "error" ? "#fee2e2" : "#ecfdf5",
            color: messageType === "error" ? "#991b1b" : "#065f46",
          }}
        >
          {message}
        </div>
      )}

      {/* ── UPLOAD FORMS ── */}
      <div className="receipt-upload-grid">
        <div className="form-card">
          <h2><Upload size={22} /> Single Receipt Upload</h2>
          <form onSubmit={handleSingleUpload}>
            <label>Choose Receipt (JPG / PNG)</label>
            <input
              type="file"
              accept="image/*"
              onChange={(e) => setSingleFile(e.target.files[0])}
            />
            {singleFile && <p className="file-name">📎 {singleFile.name}</p>}
            <button type="submit" disabled={loading}>
              {loading ? "Processing..." : "Upload & OCR"}
            </button>
          </form>
        </div>

        <div className="form-card">
          <h2><Upload size={22} /> Bulk Receipt Upload</h2>
          <form onSubmit={handleBulkUpload}>
            <label>Choose Multiple Receipts</label>
            <input
              type="file"
              accept="image/*"
              multiple
              onChange={(e) => setBulkFiles(Array.from(e.target.files))}
            />
            {bulkFiles.length > 0 && (
              <p className="file-name">📎 {bulkFiles.length} files selected</p>
            )}
            <button type="submit" disabled={loading}>
              {loading ? "Processing..." : "Upload Bulk"}
            </button>
          </form>
        </div>
      </div>

      {/* ── RECEIPTS TABLE ── */}
      <div className="list-card receipt-list-card">
        <div className="list-header">
          <h2>Uploaded Receipts</h2>
          <button className="refresh-btn" onClick={fetchReceipts}>
            <RefreshCcw size={16} /> Refresh
          </button>
        </div>

        {loading ? (
          <p>Loading receipts...</p>
        ) : receipts.length === 0 ? (
          <p className="empty-text">No receipts uploaded yet.</p>
        ) : (
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>File</th>
                  <th>Merchant</th>
                  <th>Amount</th>
                  <th>Date</th>
                  <th>Category</th>
                  <th>Status</th>
                  <th>Re-OCR</th>
                  <th>Confirm</th>
                  <th>Delete</th>
                </tr>
              </thead>
              <tbody>
                {receipts.map((receipt) => (
                  <tr key={receipt.id}>
                    <td>{receipt.originalFileName || "-"}</td>
                    <td>{receipt.merchantName || "-"}</td>
                    <td>{formatCurrency(receipt.amount || 0)}</td>
                    <td>{receipt.expenseDate || "-"}</td>
                    <td>{receipt.category || "-"}</td>
                    <td>{getStatusBadge(receipt.status)}</td>

                    {/* Re-run OCR */}
                    <td>
                      <button
                        className="process-btn"
                        onClick={() => handleProcessReceipt(receipt.id)}
                        disabled={loading}
                        title="Re-run OCR"
                      >
                        <Wand2 size={15} /> OCR
                      </button>
                    </td>

                    {/* Confirm button — only show if OCR already ran */}
                    <td>
                      {receipt.status === "PROCESSED" ? (
                        <button
                          className="process-btn"
                          style={{ background: "#059669" }}
                          onClick={() => openConfirmModal(receipt)}
                          title="Review and save expense"
                        >
                          <ClipboardCheck size={15} /> Confirm
                        </button>
                      ) : (
                        <span style={{ color: "#9ca3af", fontSize: "13px" }}>
                          Run OCR first
                        </span>
                      )}
                    </td>

                    {/* Delete */}
                    <td>
                      <button
                        className="delete-btn"
                        onClick={() => handleDeleteReceipt(receipt.id)}
                        disabled={loading}
                      >
                        <Trash2 size={16} />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* ── CONFIRM MODAL ── */}
      {confirmModal && selectedReceipt && (
        <div style={styles.overlay}>
          <div style={styles.modal}>

            {/* Modal Header */}
            <div style={styles.modalHeader}>
              <h2 style={{ margin: 0 }}>
                <ClipboardCheck size={22} style={{ verticalAlign: "middle", marginRight: 8 }} />
                Confirm Receipt Data
              </h2>
              <button
                style={styles.closeBtn}
                onClick={() => setConfirmModal(false)}
              >
                <X size={20} />
              </button>
            </div>

            <p style={{ color: "#6b7280", marginTop: 4, marginBottom: 20 }}>
              OCR has extracted this data. Please review, correct if needed, then click Save.
            </p>

            {/* Confirm Form */}
            <form onSubmit={handleConfirmSubmit} style={styles.form}>

              <div style={styles.formGroup}>
                <label style={styles.label}>Amount (₹) *</label>
                <input
                  style={styles.input}
                  type="number"
                  step="0.01"
                  min="1"
                  required
                  value={confirmForm.amount}
                  onChange={(e) =>
                    setConfirmForm({ ...confirmForm, amount: e.target.value })
                  }
                />
              </div>

              <div style={styles.formGroup}>
                <label style={styles.label}>Merchant Name</label>
                <input
                  style={styles.input}
                  type="text"
                  value={confirmForm.merchantName}
                  onChange={(e) =>
                    setConfirmForm({ ...confirmForm, merchantName: e.target.value })
                  }
                />
              </div>

              <div style={styles.formGroup}>
                <label style={styles.label}>Expense Date</label>
                <input
                  style={styles.input}
                  type="date"
                  value={confirmForm.expenseDate}
                  onChange={(e) =>
                    setConfirmForm({ ...confirmForm, expenseDate: e.target.value })
                  }
                />
              </div>

              <div style={styles.formGroup}>
                <label style={styles.label}>Category</label>
                <select
                  style={styles.input}
                  value={confirmForm.category}
                  onChange={(e) =>
                    setConfirmForm({ ...confirmForm, category: e.target.value })
                  }
                >
                  {CATEGORIES.map((cat) => (
                    <option key={cat} value={cat}>{cat}</option>
                  ))}
                </select>
              </div>

              <div style={styles.modalFooter}>
                <button
                  type="button"
                  style={styles.cancelBtn}
                  onClick={() => setConfirmModal(false)}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  style={styles.saveBtn}
                  disabled={confirmLoading}
                >
                  {confirmLoading ? "Saving..." : "✅ Save Expense"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

// ── MODAL STYLES ──
const styles = {
  overlay: {
    position: "fixed",
    inset: 0,
    background: "rgba(0,0,0,0.45)",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    zIndex: 1000,
  },
  modal: {
    background: "white",
    borderRadius: "20px",
    padding: "32px",
    width: "100%",
    maxWidth: "480px",
    boxShadow: "0 20px 60px rgba(0,0,0,0.2)",
  },
  modalHeader: {
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
  },
  closeBtn: {
    border: "none",
    background: "#f3f4f6",
    borderRadius: "8px",
    padding: "6px",
    cursor: "pointer",
    display: "flex",
  },
  form: {
    display: "flex",
    flexDirection: "column",
    gap: "16px",
  },
  formGroup: {
    display: "flex",
    flexDirection: "column",
    gap: "6px",
  },
  label: {
    fontWeight: "700",
    fontSize: "14px",
    color: "#374151",
  },
  input: {
    padding: "12px",
    border: "1px solid #d1d5db",
    borderRadius: "10px",
    fontSize: "15px",
    outline: "none",
    width: "100%",
    boxSizing: "border-box",
  },
  modalFooter: {
    display: "flex",
    gap: "12px",
    marginTop: "8px",
  },
  cancelBtn: {
    flex: 1,
    padding: "12px",
    border: "1px solid #d1d5db",
    borderRadius: "10px",
    background: "white",
    fontWeight: "700",
    cursor: "pointer",
    fontSize: "15px",
  },
  saveBtn: {
    flex: 2,
    padding: "12px",
    border: "none",
    borderRadius: "10px",
    background: "#059669",
    color: "white",
    fontWeight: "700",
    cursor: "pointer",
    fontSize: "15px",
  },
};

export default Receipts;