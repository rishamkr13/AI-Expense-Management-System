import { useState } from "react";
import { Link } from "react-router-dom";
import { ArrowLeft, Bot, Send, UserRound } from "lucide-react";

import api from "../api/axiosConfig";

const Chatbot = () => {
  const [question, setQuestion] = useState("");
  const [messages, setMessages] = useState([
    {
      type: "bot",
      text: "Hi! I am your expense advisor. Ask me about savings, overspending, budgets, or expense summary.",
      source: "SYSTEM",
    },
  ]);
  const [loading, setLoading] = useState(false);

  const quickQuestions = [
    "How can I save more money?",
    "Am I overspending?",
    "What is my highest spending category?",
    "Give me my monthly summary",
    "How much did I spend on food?",
    "How is my budget status?",
  ];

  const askQuestion = async (customQuestion) => {
    const finalQuestion = customQuestion || question;

    if (!finalQuestion.trim()) {
      return;
    }

    const userMessage = {
      type: "user",
      text: finalQuestion,
    };

    setMessages((prev) => [...prev, userMessage]);
    setQuestion("");
    setLoading(true);

    try {
      const response = await api.post("/api/chatbot/ask-ai", {
        question: finalQuestion,
      });

      const botMessage = {
        type: "bot",
        text:
          response.data?.answer ||
          response.data?.message ||
          "I received your question, but no answer was returned.",
        source: response.data?.source || "AI",
      };

      setMessages((prev) => [...prev, botMessage]);
    } catch (error) {
      console.error("Chatbot error:", error);

      let errorText =
        "Sorry, I could not process your question right now. Please try again.";

      if (error.response?.status === 401) {
        errorText = "Session expired. Please login again.";
      } else if (error.response?.status === 403) {
        errorText =
          "Access denied. Please check backend CORS/security config and login token.";
      } else if (error.response?.data?.message) {
        errorText = error.response.data.message;
      }

      const errorMessage = {
        type: "bot",
        text: errorText,
        source: "ERROR",
      };

      setMessages((prev) => [...prev, errorMessage]);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    askQuestion();
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <Link to="/dashboard" className="back-link">
            <ArrowLeft size={18} /> Back to Dashboard
          </Link>

          <h1>AI Chatbot Advisor</h1>
          <p>Ask financial questions based on your income, expenses, and budgets</p>
        </div>

        <div className="total-box chatbot-total">
          <Bot size={22} />
          <div>
            <span>Mode</span>
            <strong>AI + Fallback</strong>
          </div>
        </div>
      </div>

      <div className="chatbot-layout">
        <div className="chat-card">
          <div className="chat-messages">
            {messages.map((message, index) => (
              <div
                key={index}
                className={
                  message.type === "user"
                    ? "chat-row user-row"
                    : "chat-row bot-row"
                }
              >
                <div className="chat-avatar">
                  {message.type === "user" ? (
                    <UserRound size={18} />
                  ) : (
                    <Bot size={18} />
                  )}
                </div>

                <div
                  className={
                    message.type === "user"
                      ? "chat-bubble user-bubble"
                      : "chat-bubble bot-bubble"
                  }
                >
                  <p>{message.text}</p>

                  {message.source && (
                    <span className="source-badge">{message.source}</span>
                  )}
                </div>
              </div>
            ))}

            {loading && (
              <div className="chat-row bot-row">
                <div className="chat-avatar">
                  <Bot size={18} />
                </div>

                <div className="chat-bubble bot-bubble">
                  <p>Thinking...</p>
                </div>
              </div>
            )}
          </div>

          <form className="chat-input-box" onSubmit={handleSubmit}>
            <input
              type="text"
              placeholder="Ask something like: How can I save more money?"
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
            />

            <button type="submit" disabled={loading}>
              <Send size={18} />
            </button>
          </form>
        </div>

        <div className="quick-card">
          <h2>Quick Questions</h2>
          <p>Click any question to test your chatbot.</p>

          <div className="quick-list">
            {quickQuestions.map((item, index) => (
              <button
                key={index}
                onClick={() => askQuestion(item)}
                disabled={loading}
              >
                {item}
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Chatbot;