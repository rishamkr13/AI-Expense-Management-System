import { createContext, useContext, useEffect, useState } from "react";
import axios from "axios";

const AuthContext = createContext();

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(() => localStorage.getItem("token"));
  const [loading, setLoading] = useState(true);

  // Axios instance
  const API = axios.create({
    baseURL: API_BASE_URL,
  });

  // Add token automatically in every request
  API.interceptors.request.use(
    (config) => {
      const savedToken = localStorage.getItem("token");

      if (savedToken) {
        config.headers.Authorization = `Bearer ${savedToken}`;
      }

      return config;
    },
    (error) => Promise.reject(error)
  );

  // Load user from localStorage on refresh
  useEffect(() => {
    const savedToken = localStorage.getItem("token");
    const savedUser = localStorage.getItem("user");

    if (savedToken) {
      setToken(savedToken);
    }

    if (savedUser) {
      try {
        setUser(JSON.parse(savedUser));
      } catch (error) {
        localStorage.removeItem("user");
      }
    }

    setLoading(false);
  }, []);

  // Register user
  const register = async (name, email, password) => {
    try {
      const response = await API.post("/api/auth/register", {
        name,
        email,
        password,
      });

      const data = response.data;

      if (data.token) {
        localStorage.setItem("token", data.token);
        setToken(data.token);
      }

      if (data.user) {
        localStorage.setItem("user", JSON.stringify(data.user));
        setUser(data.user);
      } else {
        setUser({
          name,
          email,
        });
        localStorage.setItem(
          "user",
          JSON.stringify({
            name,
            email,
          })
        );
      }

      return data;
    } catch (error) {
      console.error("Register error:", error);
      throw error;
    }
  };

  // Login user
  const login = async (email, password) => {
    try {
      const response = await API.post("/api/auth/login", {
        email,
        password,
      });

      const data = response.data;

      if (data.token) {
        localStorage.setItem("token", data.token);
        setToken(data.token);
      }

      if (data.user) {
        localStorage.setItem("user", JSON.stringify(data.user));
        setUser(data.user);
      } else {
        setUser({
          email,
        });
        localStorage.setItem(
          "user",
          JSON.stringify({
            email,
          })
        );
      }

      return data;
    } catch (error) {
      console.error("Login error:", error);
      throw error;
    }
  };

  // Logout user
  const logout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("user");

    setToken(null);
    setUser(null);
  };

  const isAuthenticated = !!token;

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        loading,
        login,
        register,
        logout,
        isAuthenticated,
        API,
      }}
    >
      {!loading && children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  return useContext(AuthContext);
};