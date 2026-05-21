import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { PlusCircle, Trash2, ArrowLeft, Wallet } from "lucide-react";

import api from "../api/axiosConfig";
import { formatCurrency } from "../utils/formatCurrency";

const Incomes = () => {
  const [incomes, setIncomes] = useState([]);
  const [formData, setFormData] = useState({
    amount: "",
    source: "SALARY",
    description: "",
    incomeDate: "",
  });

  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");

  const fetchIncomes = async () => {
    try {
      setLoading(true);
      setMessage("");

      const response = await api.get("/api/incomes");

      setIncomes(response.data || []);
    } catch (error) {
      console.error("Fetch incomes error:", error);

      if (error.response?.status === 401) {
        setMessage("Session expired. Please login again.");
      } else if (error.response?.status === 403) {
        setMessage("Access denied. Please check backend security/CORS config.");
      } else {
        setMessage("Failed to load incomes");
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchIncomes();
  }, []);

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const handleAddIncome = async (e) => {
    e.preventDefault();
    setMessage("");

    try {
      await api.post("/api/incomes", {
        amount: Number(formData.amount),
        source: formData.source,
        description: formData.description,
        incomeDate: formData.incomeDate,
      });

      setMessage("Income added successfully ✅");

      setFormData({
        amount: "",
        source: "SALARY",
        description: "",
        incomeDate: "",
      });

      fetchIncomes();
    } catch (error) {
      console.error("Add income error:", error);

      if (error.response?.status === 401) {
        setMessage("Session expired. Please login again.");
      } else if (error.response?.status === 403) {
        setMessage("Access denied. Please check backend security/CORS config.");
      } else {
        setMessage("Failed to add income");
      }
    }
  };

  const handleDeleteIncome = async (id) => {
    try {
      setMessage("");

      await api.delete(`/api/incomes/${id}`);

      setMessage("Income deleted successfully");

      fetchIncomes();
    } catch (error) {
      console.error("Delete income error:", error);

      if (error.response?.status === 401) {
        setMessage("Session expired. Please login again.");
      } else if (error.response?.status === 403) {
        setMessage("Access denied. Please check backend security/CORS config.");
      } else {
        setMessage("Failed to delete income");
      }
    }
  };

  const totalIncome = incomes.reduce(
    (sum, income) => sum + Number(income.amount || 0),
    0
  );

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <Link to="/dashboard" className="back-link">
            <ArrowLeft size={18} /> Back to Dashboard
          </Link>

          <h1>Income Management</h1>
          <p>Add and manage your income records</p>
        </div>

        <div className="total-box">
          <Wallet size={22} />
          <div>
            <span>Total Income</span>
            <strong>{formatCurrency(totalIncome)}</strong>
          </div>
        </div>
      </div>

      {message && <div className="message-box">{message}</div>}

      <div className="form-list-grid">
        <div className="form-card">
          <h2>
            <PlusCircle size={22} /> Add Income
          </h2>

          <form onSubmit={handleAddIncome}>
            <label>Amount</label>
            <input
              type="number"
              name="amount"
              placeholder="Enter amount"
              value={formData.amount}
              onChange={handleChange}
              required
            />

            <label>Source</label>
            <select
              name="source"
              value={formData.source}
              onChange={handleChange}
              required
            >
              <option value="SALARY">Salary</option>
              <option value="BUSINESS">Business</option>
              <option value="FREELANCE">Freelance</option>
              <option value="INVESTMENT">Investment</option>
              <option value="OTHER">Other</option>
            </select>

            <label>Description</label>
            <input
              type="text"
              name="description"
              placeholder="Monthly salary, freelancing, etc."
              value={formData.description}
              onChange={handleChange}
            />

            <label>Income Date</label>
            <input
              type="date"
              name="incomeDate"
              value={formData.incomeDate}
              onChange={handleChange}
              required
            />

            <button type="submit">Add Income</button>
          </form>
        </div>

        <div className="list-card">
          <h2>Income Records</h2>

          {loading ? (
            <p>Loading incomes...</p>
          ) : incomes.length === 0 ? (
            <p className="empty-text">No income records found.</p>
          ) : (
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>Source</th>
                    <th>Amount</th>
                    <th>Date</th>
                    <th>Description</th>
                    <th>Action</th>
                  </tr>
                </thead>

                <tbody>
                  {incomes.map((income) => (
                    <tr key={income.id}>
                      <td>{income.source}</td>
                      <td>{formatCurrency(income.amount)}</td>
                      <td>{income.incomeDate}</td>
                      <td>{income.description || "-"}</td>
                      <td>
                        <button
                          className="delete-btn"
                          onClick={() => handleDeleteIncome(income.id)}
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
      </div>
    </div>
  );
};

export default Incomes;