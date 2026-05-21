import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import "./Login.css";

const Register = () => {
  const navigate = useNavigate();
  const { register } = useAuth();

  const [formData, setFormData] = useState({
    name: "",
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

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    try {
      await register(formData.name, formData.email, formData.password);
      navigate("/dashboard");
    } catch (err) {
      setError("Registration failed. Email may already exist.");
    }
  };

  return (
    <div className="auth-slide-page">
      <div className="auth-slide-container register-mode">
        <div className="auth-panel-section register-left-panel">
          <div className="shape shape-one"></div>
          <div className="shape shape-two"></div>
          <div className="shape shape-three"></div>

          <h2>Welcome Back!</h2>
          <p>
            To keep connected with us <br />
            please login with your info
          </p>

          <Link to="/login" className="slide-outline-btn">
            SIGN IN
          </Link>
        </div>

        <div className="auth-form-section">
          <div className="auth-form-box">
            <h1>Create Account</h1>

            {error && <div className="error-message">{error}</div>}

            <form className="slide-form" onSubmit={handleSubmit}>
              <div className="slide-input-box">
                <span>👤</span>
                <input
                  type="text"
                  name="name"
                  placeholder="Name"
                  value={formData.name}
                  onChange={handleChange}
                  required
                />
              </div>

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

              <button type="submit" className="slide-main-btn">
                SIGN UP
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Register;