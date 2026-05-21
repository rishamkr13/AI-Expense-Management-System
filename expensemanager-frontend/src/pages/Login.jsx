import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import axios from "axios";
import "./Login.css";

function Login() {
  const navigate = useNavigate();

  const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

  const [formData, setFormData] = useState({
    email: "",
    password: "",
  });

  const [error, setError] = useState("");

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const handleLogin = async (e) => {
    e.preventDefault();

    try {
      setError("");

      const response = await axios.post(
        `${API_BASE_URL}/api/auth/login`,
        formData
      );

      localStorage.setItem("token", response.data.token);

      navigate("/dashboard");
    } catch (err) {
      console.error(err);
      setError("Invalid email or password");
    }
  };

  return (
    <div className="auth-slide-page">
      <div className="auth-slide-container">
        <div className="auth-form-section">
          <div className="auth-form-box">
            <h1>Sign in to Expense Manager</h1>

            {error && <div className="error-message">{error}</div>}

            <form className="slide-form" onSubmit={handleLogin}>
              <div className="slide-input-box">
                <span>✉</span>
                <input
                  type="email"
                  name="email"
                  placeholder="Email"
                  value={formData.email}
                  onChange={handleChange}
                  required
                />
              </div>

              <div className="slide-input-box">
                <span>🔒</span>
                <input
                  type="password"
                  name="password"
                  placeholder="Password"
                  value={formData.password}
                  onChange={handleChange}
                  required
                />
              </div>

              <p className="forgot-text">Forgot your password?</p>

              <button type="submit" className="slide-main-btn">
                SIGN IN
              </button>
            </form>
          </div>
        </div>

        <div className="auth-panel-section">
          <div className="shape shape-one"></div>
          <div className="shape shape-two"></div>
          <div className="shape shape-three"></div>

          <h2>Hello, Friend!</h2>
          <p>
            Enter your personal details <br />
            and start journey with us
          </p>

          <Link to="/register" className="slide-outline-btn">
            SIGN UP
          </Link>
        </div>
      </div>
    </div>
  );
}

export default Login;