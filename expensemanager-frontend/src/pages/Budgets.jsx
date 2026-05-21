import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowLeft, PlusCircle, Trash2, Target } from "lucide-react";

import api from "../api/axiosConfig";
import { formatCurrency } from "../utils/formatCurrency";

const Budgets = () => {
  const [budgets, setBudgets] = useState([]);
  const [expenses, setExpenses] = useState([]);

  const currentDate = new Date();

  const [formData, setFormData] = useState({
    category: "FOOD",
    amount: "",
    month: currentDate.getMonth() + 1,
    year: currentDate.getFullYear(),
  });

  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");

  const fetchBudgetsData = async () => {
    try {
      setLoading(true);

      const [budgetRes, expenseRes] = await Promise.all([
        api.get("/budgets"),
        api.get("/expenses"),
      ]);

      setBudgets(budgetRes.data || []);
      setExpenses(expenseRes.data || []);
    } catch (error) {
      console.error("Fetch budgets error:", error);
      setMessage("Failed to load budgets");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBudgetsData();
  }, []);

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]:
        e.target.name === "amount" ||
        e.target.name === "month" ||
        e.target.name === "year"
          ? Number(e.target.value)
          : e.target.value,
    });
  };

  const handleAddBudget = async (e) => {
    e.preventDefault();
    setMessage("");

    try {
      await api.post("/budgets", {
        category: formData.category,
        amount: Number(formData.amount),
        month: Number(formData.month),
        year: Number(formData.year),
      });

      setMessage("Budget added successfully ✅");

      setFormData({
        category: "FOOD",
        amount: "",
        month: currentDate.getMonth() + 1,
        year: currentDate.getFullYear(),
      });

      fetchBudgetsData();
    } catch (error) {
      console.error("Add budget error:", error);
      setMessage("Failed to add budget");
    }
  };

  const handleDeleteBudget = async (id) => {
    try {
      await api.delete(`/budgets/${id}`);

      setMessage("Budget deleted successfully");

      fetchBudgetsData();
    } catch (error) {
      console.error("Delete budget error:", error);
      setMessage("Failed to delete budget");
    }
  };

  const getSpentForBudget = (budget) => {
    return expenses
      .filter((expense) => {
        const expenseDate = new Date(expense.expenseDate);

        return (
          expense.category === budget.category &&
          expenseDate.getMonth() + 1 === Number(budget.month) &&
          expenseDate.getFullYear() === Number(budget.year)
        );
      })
      .reduce((sum, expense) => sum + Number(expense.amount || 0), 0);
  };

  const budgetSummary = useMemo(() => {
    const totalBudget = budgets.reduce(
      (sum, budget) => sum + Number(budget.amount || 0),
      0
    );

    const totalSpent = budgets.reduce(
      (sum, budget) => sum + getSpentForBudget(budget),
      0
    );

    return {
      totalBudget,
      totalSpent,
      remaining: totalBudget - totalSpent,
    };
  }, [budgets, expenses]);

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <Link to="/dashboard" className="back-link">
            <ArrowLeft size={18} /> Back to Dashboard
          </Link>

          <h1>Budget Management</h1>
          <p>Set monthly category-wise spending limits</p>
        </div>

        <div className="total-box budget-total">
          <Target size={22} />
          <div>
            <span>Total Budget</span>
            <strong>{formatCurrency(budgetSummary.totalBudget)}</strong>
          </div>
        </div>
      </div>

      {message && <div className="message-box">{message}</div>}

      <div className="budget-summary-grid">
        <div className="mini-summary-card">
          <span>Total Budget</span>
          <strong>{formatCurrency(budgetSummary.totalBudget)}</strong>
        </div>

        <div className="mini-summary-card">
          <span>Total Spent</span>
          <strong>{formatCurrency(budgetSummary.totalSpent)}</strong>
        </div>

        <div className="mini-summary-card">
          <span>Remaining</span>
          <strong>{formatCurrency(budgetSummary.remaining)}</strong>
        </div>
      </div>

      <div className="form-list-grid">
        <div className="form-card">
          <h2>
            <PlusCircle size={22} /> Add Budget
          </h2>

          <form onSubmit={handleAddBudget}>
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

            <label>Amount</label>
            <input
              type="number"
              name="amount"
              placeholder="Enter budget amount"
              value={formData.amount}
              onChange={handleChange}
              required
            />

            <label>Month</label>
            <select
              name="month"
              value={formData.month}
              onChange={handleChange}
              required
            >
              <option value={1}>January</option>
              <option value={2}>February</option>
              <option value={3}>March</option>
              <option value={4}>April</option>
              <option value={5}>May</option>
              <option value={6}>June</option>
              <option value={7}>July</option>
              <option value={8}>August</option>
              <option value={9}>September</option>
              <option value={10}>October</option>
              <option value={11}>November</option>
              <option value={12}>December</option>
            </select>

            <label>Year</label>
            <input
              type="number"
              name="year"
              value={formData.year}
              onChange={handleChange}
              required
            />

            <button type="submit">Add Budget</button>
          </form>
        </div>

        <div className="list-card">
          <h2>Budget Records</h2>

          {loading ? (
            <p>Loading budgets...</p>
          ) : budgets.length === 0 ? (
            <p className="empty-text">No budgets added yet.</p>
          ) : (
            <div className="budget-list">
              {budgets.map((budget) => {
                const spent = getSpentForBudget(budget);
                const amount = Number(budget.amount || 0);
                const remaining = amount - spent;
                const percentage =
                  amount === 0 ? 0 : Math.min((spent / amount) * 100, 100);

                const isOverBudget = spent > amount;

                return (
                  <div className="budget-item" key={budget.id}>
                    <div className="budget-item-header">
                      <div>
                        <h3>{budget.category}</h3>
                        <p>
                          {budget.month}/{budget.year}
                        </p>
                      </div>

                      <button
                        className="delete-btn"
                        onClick={() => handleDeleteBudget(budget.id)}
                      >
                        <Trash2 size={16} />
                      </button>
                    </div>

                    <div className="budget-numbers">
                      <span>Budget: {formatCurrency(amount)}</span>
                      <span>Spent: {formatCurrency(spent)}</span>
                      <span>
                        {isOverBudget ? "Over: " : "Left: "}
                        {formatCurrency(Math.abs(remaining))}
                      </span>
                    </div>

                    <div className="progress-track">
                      <div
                        className={
                          isOverBudget
                            ? "progress-fill over-budget"
                            : "progress-fill"
                        }
                        style={{ width: `${percentage}%` }}
                      ></div>
                    </div>

                    {isOverBudget ? (
                      <p className="over-text">
                        You crossed this budget limit ⚠️
                      </p>
                    ) : (
                      <p className="safe-text">Budget is under control ✅</p>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Budgets;