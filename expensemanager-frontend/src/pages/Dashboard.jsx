import { useEffect, useState } from "react";
import {
  getDashboardSummary,
  getCategoryWiseExpense,
  getRecentTransactions,
  getMonthlyTrend,
  getBudgetStatus,
} from "../api/dashboardApi";
import "./Dashboard.css";

import {
  PieChart,
  Pie,
  Cell,
  Tooltip,
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
} from "recharts";

const COLORS = [
  "#6366f1", "#22c55e", "#f97316", "#ef4444",
  "#06b6d4", "#a855f7", "#eab308", "#14b8a6",
];

const Dashboard = () => {
  const [summary, setSummary] = useState(null);
  const [categoryData, setCategoryData] = useState([]);
  const [recentTransactions, setRecentTransactions] = useState([]);
  const [monthlyTrend, setMonthlyTrend] = useState([]);
  const [budgetStatus, setBudgetStatus] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const formatMoney = (value) => {
    if (value === null || value === undefined) return "₹0";
    return `₹${Number(value).toLocaleString("en-IN")}`;
  };

  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        setLoading(true);
        const [summaryRes, categoryRes, recentRes, monthlyRes, budgetRes] =
          await Promise.all([
            getDashboardSummary(),
            getCategoryWiseExpense(),
            getRecentTransactions(),
            getMonthlyTrend(),
            getBudgetStatus(),
          ]);

        setSummary(summaryRes.data);
        setCategoryData(categoryRes.data || []);
        setRecentTransactions(recentRes.data || []);
        setMonthlyTrend(monthlyRes.data || []);
        setBudgetStatus(budgetRes.data || []);
      } catch (err) {
        console.error(err);
        setError("Failed to load dashboard data");
      } finally {
        setLoading(false);
      }
    };

    fetchDashboardData();
  }, []);

  if (loading) return <div className="dash-loading">Loading dashboard...</div>;
  if (error)   return <div className="dash-error">{error}</div>;

  return (
    <div className="dashboard-page">
      <h1>Dashboard</h1>

      {/* ── SUMMARY CARDS ── */}
      <div className="dash-stats-grid">
        <div className="dash-stat-card">
          <span className="stat-label">Total Income</span>
          <p className="stat-value green">{formatMoney(summary?.totalIncome)}</p>
        </div>
        <div className="dash-stat-card">
          <span className="stat-label">Total Expense</span>
          <p className="stat-value red">{formatMoney(summary?.totalExpense)}</p>
        </div>
        <div className="dash-stat-card">
          <span className="stat-label">Balance</span>
          <p className="stat-value blue">{formatMoney(summary?.balance)}</p>
        </div>
        <div className="dash-stat-card">
          <span className="stat-label">Savings</span>
          <p className="stat-value purple">{summary?.savingsPercentage || 0}%</p>
        </div>
      </div>

      {/* ── THIS MONTH ── */}
      <div className="dash-month-grid">
        <div className="dash-month-card">
          <span className="stat-label">This Month Income</span>
          <p className="stat-value green">{formatMoney(summary?.thisMonthIncome)}</p>
        </div>
        <div className="dash-month-card">
          <span className="stat-label">This Month Expense</span>
          <p className="stat-value red">{formatMoney(summary?.thisMonthExpense)}</p>
        </div>
        <div className="dash-month-card">
          <span className="stat-label">Highest Category</span>
          <p className="stat-value" style={{ color: "#111827" }}>
            {summary?.highestExpenseCategory || "N/A"}
          </p>
        </div>
      </div>

      {/* ── CHARTS ── */}
      <div className="dash-charts-grid">
        {/* Pie Chart */}
        <div className="dash-panel">
          <h2>Category-wise Expense</h2>
          {categoryData.length === 0 ? (
            <p className="dash-empty">No category data available</p>
          ) : (
            <ResponsiveContainer width="100%" height={280}>
              <PieChart>
                <Pie
                  data={categoryData}
                  dataKey="amount"
                  nameKey="category"
                  cx="50%"
                  cy="50%"
                  outerRadius={100}
                  label={({ category }) => category}
                >
                  {categoryData.map((_, index) => (
                    <Cell key={index} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip formatter={(val) => formatMoney(val)} />
              </PieChart>
            </ResponsiveContainer>
          )}
        </div>

        {/* Bar Chart */}
        <div className="dash-panel">
          <h2>Monthly Expense Trend</h2>
          <ResponsiveContainer width="100%" height={280}>
            <BarChart data={monthlyTrend}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f3f4f6" />
              <XAxis dataKey="month" tick={{ fontSize: 12 }} />
              <YAxis tick={{ fontSize: 12 }} />
              <Tooltip formatter={(val) => formatMoney(val)} />
              <Bar dataKey="amount" fill="#6366f1" radius={[6, 6, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* ── BUDGET STATUS ── */}
      <div className="dash-panel" style={{ marginBottom: "26px" }}>
        <h2>Budget Status</h2>
        {budgetStatus.length === 0 ? (
          <p className="dash-empty">No budget set for this month</p>
        ) : (
          <div className="dash-budget-grid">
            {budgetStatus.map((budget, index) => (
              <div key={index} className="dash-budget-item">
                <div className="dash-budget-item-header">
                  <h3>{budget.category}</h3>
                  <span className={`dash-badge ${budget.overspent ? "over" : "safe"}`}>
                    {budget.overspent ? "Overspent" : "Safe"}
                  </span>
                </div>
                <div className="dash-budget-info">
                  <span>Budget: <strong>{formatMoney(budget.budgetAmount)}</strong></span>
                  <span>Spent: <strong>{formatMoney(budget.spentAmount)}</strong></span>
                  <span>Remaining: <strong>{formatMoney(budget.remainingAmount)}</strong></span>
                </div>
                <div className="progress-track">
                  <div
                    className={`progress-fill ${budget.overspent ? "over-budget" : ""}`}
                    style={{ width: `${Math.min(Number(budget.usedPercentage), 100)}%` }}
                  />
                </div>
                <p className="progress-pct">{budget.usedPercentage}% used</p>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* ── RECENT TRANSACTIONS ── */}
      <div className="dash-panel">
        <h2>Recent Transactions</h2>
        {recentTransactions.length === 0 ? (
          <p className="dash-empty">No recent transactions</p>
        ) : (
          <div className="dash-table-wrapper">
            <table className="dash-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Merchant</th>
                  <th>Category</th>
                  <th>Payment</th>
                  <th>Amount</th>
                </tr>
              </thead>
              <tbody>
                {recentTransactions.map((txn) => (
                  <tr key={txn.id}>
                    <td>{txn.expenseDate}</td>
                    <td>{txn.merchantName || "Unknown"}</td>
                    <td>
                      <span className="category-badge">{txn.category}</span>
                    </td>
                    <td>{txn.paymentMode}</td>
                    <td className="amount-cell">{formatMoney(txn.amount)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default Dashboard;