import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  PlusCircle,
  Trash2,
  ArrowLeft,
  CreditCard,
  Search,
} from "lucide-react";

import api from "../api/axiosConfig";
import { formatCurrency } from "../utils/formatCurrency";

const Expenses = () => {
  const [expenses, setExpenses] = useState([]);

  const [formData, setFormData] = useState({
    amount: "",
    category: "FOOD",
    merchantName: "",
    description: "",
    expenseDate: "",
    paymentMode: "CASH",
  });

  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [searchText, setSearchText] = useState("");

  const fetchExpenses = async () => {
    try {
      setLoading(true);

      const response = await api.get("/expenses");

      setExpenses(response.data || []);
    } catch (error) {
      console.error("Fetch expenses error:", error);
      setMessage("Failed to load expenses");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchExpenses();
  }, []);

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const handleAddExpense = async (e) => {
    e.preventDefault();
    setMessage("");

    try {
      await api.post("/expenses", {
        amount: Number(formData.amount),
        category: formData.category,
        merchantName: formData.merchantName,
        description: formData.description,
        expenseDate: formData.expenseDate,
        paymentMode: formData.paymentMode,
      });

      setMessage("Expense added successfully ✅");

      setFormData({
        amount: "",
        category: "FOOD",
        merchantName: "",
        description: "",
        expenseDate: "",
        paymentMode: "CASH",
      });

      fetchExpenses();
    } catch (error) {
      console.error("Add expense error:", error);
      setMessage("Failed to add expense");
    }
  };

  const handleDeleteExpense = async (id) => {
    try {
      await api.delete(`/expenses/${id}`);

      setMessage("Expense deleted successfully");

      fetchExpenses();
    } catch (error) {
      console.error("Delete expense error:", error);
      setMessage("Failed to delete expense");
    }
  };

  const filteredExpenses = expenses.filter((expense) => {
    const text = searchText.toLowerCase();

    return (
      expense.category?.toLowerCase().includes(text) ||
      expense.merchantName?.toLowerCase().includes(text) ||
      expense.description?.toLowerCase().includes(text) ||
      expense.paymentMode?.toLowerCase().includes(text)
    );
  });

  const totalExpense = filteredExpenses.reduce(
    (sum, expense) => sum + Number(expense.amount || 0),
    0
  );

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <Link to="/dashboard" className="back-link">
            <ArrowLeft size={18} /> Back to Dashboard
          </Link>

          <h1>Expense Management</h1>
          <p>Add and manage your spending records</p>
        </div>

        <div className="total-box expense-total">
          <CreditCard size={22} />
          <div>
            <span>Total Expense</span>
            <strong>{formatCurrency(totalExpense)}</strong>
          </div>
        </div>
      </div>

      {message && <div className="message-box">{message}</div>}

      <div className="form-list-grid">
        <div className="form-card">
          <h2>
            <PlusCircle size={22} /> Add Expense
          </h2>

          <form onSubmit={handleAddExpense}>
            <label>Amount</label>
            <input
              type="number"
              name="amount"
              placeholder="Enter amount"
              value={formData.amount}
              onChange={handleChange}
              required
            />

            <label>Category</label>
            <select
              name="category"
              value={formData.category}
              onChange={handleChange}
              required
            >
              <option value="FOOD">Food</option>
              <option value="TRAVEL">Travel</option>
              <option value="SHOPPING">Shopping</option>
              <option value="RENT">Rent</option>
              <option value="BILLS">Bills</option>
              <option value="EDUCATION">Education</option>
              <option value="HEALTH">Health</option>
              <option value="ENTERTAINMENT">Entertainment</option>
              <option value="OTHER">Other</option>
            </select>

            <label>Merchant Name</label>
            <input
              type="text"
              name="merchantName"
              placeholder="Zomato, Amazon, Metro, etc."
              value={formData.merchantName}
              onChange={handleChange}
            />

            <label>Description</label>
            <input
              type="text"
              name="description"
              placeholder="Lunch, books, recharge, etc."
              value={formData.description}
              onChange={handleChange}
            />

            <label>Expense Date</label>
            <input
              type="date"
              name="expenseDate"
              value={formData.expenseDate}
              onChange={handleChange}
              required
            />

            <label>Payment Mode</label>
            <select
              name="paymentMode"
              value={formData.paymentMode}
              onChange={handleChange}
              required
            >
              <option value="CASH">Cash</option>
              <option value="UPI">UPI</option>
              <option value="CARD">Card</option>
              <option value="NET_BANKING">Net Banking</option>
              <option value="OTHER">Other</option>
            </select>

            <button type="submit">Add Expense</button>
          </form>
        </div>

        <div className="list-card">
          <div className="list-header">
            <h2>Expense Records</h2>

            <div className="search-box">
              <Search size={16} />
              <input
                type="text"
                placeholder="Search expenses..."
                value={searchText}
                onChange={(e) => setSearchText(e.target.value)}
              />
            </div>
          </div>

          {loading ? (
            <p>Loading expenses...</p>
          ) : filteredExpenses.length === 0 ? (
            <p className="empty-text">No expense records found.</p>
          ) : (
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>Category</th>
                    <th>Merchant</th>
                    <th>Amount</th>
                    <th>Date</th>
                    <th>Payment</th>
                    <th>Description</th>
                    <th>Action</th>
                  </tr>
                </thead>

                <tbody>
                  {filteredExpenses.map((expense) => (
                    <tr key={expense.id}>
                      <td>
                        <span className="category-badge">
                          {expense.category}
                        </span>
                      </td>
                      <td>{expense.merchantName || "-"}</td>
                      <td>{formatCurrency(expense.amount)}</td>
                      <td>{expense.expenseDate}</td>
                      <td>{expense.paymentMode}</td>
                      <td>{expense.description || "-"}</td>
                      <td>
                        <button
                          className="delete-btn"
                          onClick={() => handleDeleteExpense(expense.id)}
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

export default Expenses;