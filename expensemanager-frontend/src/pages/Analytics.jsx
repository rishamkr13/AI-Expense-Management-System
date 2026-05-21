import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  ArrowLeft,
  BarChart3,
  PieChart as PieChartIcon,
  TrendingUp,
  Target,
  Brain,
} from "lucide-react";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  LineChart,
  Line,
  CartesianGrid,
} from "recharts";

import api from "../api/axiosConfig";
import { formatCurrency } from "../utils/formatCurrency";

const Analytics = () => {
  const [expenses, setExpenses] = useState([]);
  const [incomes, setIncomes] = useState([]);
  const [budgets, setBudgets] = useState([]);
  const [backendInsights, setBackendInsights] = useState(null);
  const [loading, setLoading] = useState(false);

  const fetchAnalyticsData = async () => {
    try {
      setLoading(true);

      const [expenseRes, incomeRes, budgetRes, insightRes] = await Promise.all([
        api.get("/expenses"),
        api.get("/incomes"),
        api.get("/budgets"),
        api.get("/analytics/insights"),
      ]);

      setExpenses(expenseRes.data || []);
      setIncomes(incomeRes.data || []);
      setBudgets(budgetRes.data || []);
      setBackendInsights(insightRes.data || null);
    } catch (error) {
      console.error("Analytics fetch error:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAnalyticsData();
  }, []);

  const totalIncome = useMemo(() => {
    return incomes.reduce((sum, item) => sum + Number(item.amount || 0), 0);
  }, [incomes]);

  const totalExpense = useMemo(() => {
    return expenses.reduce((sum, item) => sum + Number(item.amount || 0), 0);
  }, [expenses]);

  const savings = totalIncome - totalExpense;

  const savingsRate = totalIncome === 0 ? 0 : (savings / totalIncome) * 100;

  const categoryData = useMemo(() => {
    const map = {};

    expenses.forEach((expense) => {
      const category = expense.category || "OTHER";
      map[category] = (map[category] || 0) + Number(expense.amount || 0);
    });

    return Object.entries(map).map(([category, amount]) => ({
      category,
      amount,
    }));
  }, [expenses]);

  const paymentModeData = useMemo(() => {
    const map = {};

    expenses.forEach((expense) => {
      const mode = expense.paymentMode || "OTHER";
      map[mode] = (map[mode] || 0) + Number(expense.amount || 0);
    });

    return Object.entries(map).map(([mode, amount]) => ({
      mode,
      amount,
    }));
  }, [expenses]);

  const monthlyTrendData = useMemo(() => {
    const map = {};

    expenses.forEach((expense) => {
      if (!expense.expenseDate) return;

      const date = new Date(expense.expenseDate);
      const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(
        2,
        "0"
      )}`;

      map[key] = (map[key] || 0) + Number(expense.amount || 0);
    });

    return Object.entries(map)
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([month, amount]) => ({
        month,
        amount,
      }));
  }, [expenses]);

  const budgetUtilizationData = useMemo(() => {
    return budgets.map((budget) => {
      const spent = expenses
        .filter((expense) => {
          if (!expense.expenseDate) return false;

          const date = new Date(expense.expenseDate);

          return (
            expense.category === budget.category &&
            date.getMonth() + 1 === Number(budget.month) &&
            date.getFullYear() === Number(budget.year)
          );
        })
        .reduce((sum, expense) => sum + Number(expense.amount || 0), 0);

      const budgetAmount = Number(budget.amount || 0);

      return {
        category: budget.category,
        budget: budgetAmount,
        spent,
        utilization:
          budgetAmount === 0 ? 0 : Math.round((spent / budgetAmount) * 100),
      };
    });
  }, [budgets, expenses]);

  const generatedInsights = useMemo(() => {
    const insights = [];

    if (totalIncome === 0) {
      insights.push("You have not added income yet. Add income to calculate savings rate.");
    }

    if (totalExpense === 0) {
      insights.push("You have not added expenses yet. Add expenses to analyze spending.");
    }

    if (totalIncome > 0 && savingsRate >= 30) {
      insights.push(`Great! Your savings rate is ${savingsRate.toFixed(1)}%.`);
    }

    if (totalIncome > 0 && savingsRate < 10) {
      insights.push(`Your savings rate is only ${savingsRate.toFixed(1)}%. Try reducing non-essential expenses.`);
    }

    const highestCategory = [...categoryData].sort((a, b) => b.amount - a.amount)[0];

    if (highestCategory) {
      insights.push(
        `Your highest spending category is ${highestCategory.category} with ${formatCurrency(
          highestCategory.amount
        )}.`
      );
    }

    const overBudget = budgetUtilizationData.filter((item) => item.spent > item.budget);

    if (overBudget.length > 0) {
      insights.push(
        `You crossed budget limits in: ${overBudget
          .map((item) => item.category)
          .join(", ")}.`
      );
    }

    return insights;
  }, [
    totalIncome,
    totalExpense,
    savingsRate,
    categoryData,
    budgetUtilizationData,
  ]);

  if (loading) {
    return (
      <div className="page-container">
        <h2>Loading analytics...</h2>
      </div>
    );
  }

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <Link to="/dashboard" className="back-link">
            <ArrowLeft size={18} /> Back to Dashboard
          </Link>

          <h1>Analytics</h1>
          <p>Visual insights from your income, expenses, and budgets</p>
        </div>

        <div className="total-box analytics-total">
          <BarChart3 size={22} />
          <div>
            <span>Savings Rate</span>
            <strong>{savingsRate.toFixed(1)}%</strong>
          </div>
        </div>
      </div>

      <div className="analytics-summary-grid">
        <div className="mini-summary-card">
          <span>Total Income</span>
          <strong>{formatCurrency(totalIncome)}</strong>
        </div>

        <div className="mini-summary-card">
          <span>Total Expense</span>
          <strong>{formatCurrency(totalExpense)}</strong>
        </div>

        <div className="mini-summary-card">
          <span>Savings</span>
          <strong>{formatCurrency(savings)}</strong>
        </div>

        <div className="mini-summary-card">
          <span>Total Transactions</span>
          <strong>{expenses.length}</strong>
        </div>
      </div>

      <div className="analytics-grid">
        <div className="dashboard-panel">
          <div className="panel-title">
            <PieChartIcon size={22} />
            <h2>Category-wise Expenses</h2>
          </div>

          {categoryData.length === 0 ? (
            <p className="empty-text">No category data available.</p>
          ) : (
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie
                  data={categoryData}
                  dataKey="amount"
                  nameKey="category"
                  outerRadius={105}
                  label
                >
                  {categoryData.map((entry, index) => (
                    <Cell key={index} />
                  ))}
                </Pie>
                <Tooltip formatter={(value) => formatCurrency(value)} />
              </PieChart>
            </ResponsiveContainer>
          )}
        </div>

        <div className="dashboard-panel">
          <div className="panel-title">
            <BarChart3 size={22} />
            <h2>Payment Mode Analysis</h2>
          </div>

          {paymentModeData.length === 0 ? (
            <p className="empty-text">No payment mode data available.</p>
          ) : (
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={paymentModeData}>
                <XAxis dataKey="mode" />
                <YAxis />
                <Tooltip formatter={(value) => formatCurrency(value)} />
                <Bar dataKey="amount" />
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>

        <div className="dashboard-panel">
          <div className="panel-title">
            <TrendingUp size={22} />
            <h2>Monthly Expense Trend</h2>
          </div>

          {monthlyTrendData.length === 0 ? (
            <p className="empty-text">No monthly trend available.</p>
          ) : (
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={monthlyTrendData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="month" />
                <YAxis />
                <Tooltip formatter={(value) => formatCurrency(value)} />
                <Line type="monotone" dataKey="amount" strokeWidth={3} />
              </LineChart>
            </ResponsiveContainer>
          )}
        </div>

        <div className="dashboard-panel">
          <div className="panel-title">
            <Target size={22} />
            <h2>Budget Utilization</h2>
          </div>

          {budgetUtilizationData.length === 0 ? (
            <p className="empty-text">No budget utilization data available.</p>
          ) : (
            <div className="utilization-list">
              {budgetUtilizationData.map((item, index) => {
                const percentage = Math.min(item.utilization, 100);
                const isOver = item.spent > item.budget;

                return (
                  <div className="utilization-item" key={index}>
                    <div className="utilization-header">
                      <strong>{item.category}</strong>
                      <span>{item.utilization}% used</span>
                    </div>

                    <div className="progress-track">
                      <div
                        className={
                          isOver
                            ? "progress-fill over-budget"
                            : "progress-fill"
                        }
                        style={{ width: `${percentage}%` }}
                      ></div>
                    </div>

                    <p>
                      Spent {formatCurrency(item.spent)} of{" "}
                      {formatCurrency(item.budget)}
                    </p>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>

      <div className="dashboard-panel insight-panel">
        <div className="panel-title">
          <Brain size={22} />
          <h2>Smart Analytics Insights</h2>
        </div>

        <div className="insight-content">
          {generatedInsights.map((item, index) => (
            <p key={index}>• {item}</p>
          ))}

          {backendInsights && (
            <>
              <hr />
              <p>
                <strong>Backend Insights:</strong>
              </p>
              {Array.isArray(backendInsights) ? (
                backendInsights.map((item, index) => (
                  <p key={`backend-${index}`}>• {item.message || item}</p>
                ))
              ) : (
                <pre>{JSON.stringify(backendInsights, null, 2)}</pre>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
};

export default Analytics;