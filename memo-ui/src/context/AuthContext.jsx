import React, { createContext, useContext, useState, useEffect } from 'react';
import { MemoApi } from '../lib/api';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        // Check for existing session on mount
        const initAuth = async () => {
            const token = localStorage.getItem('access_token');
            if (token) {
                try {
                    // Ideally we verify token or get user info here
                    // For now, we assume if token exists, we are "logged in" sufficiently to start
                    // We could call an endpoint like /auth/me or /auth/session to get user details
                    const session = await MemoApi.getSession().catch(() => null);
                    if (session && session.active) {
                        setUser({ username: session.username, ...session });
                    } else {
                        // Fallback: decode token or just set a placeholder if we trust the token existence
                        // For robustness, let's just say we are authenticated if token exists
                        // But getting session is better.
                        // If session check fails but we have token, we might be in a weird state.
                        // Let's rely on api.js interceptor to clear token if it's invalid.
                        setUser({ isAuthenticated: true });
                    }
                } catch (error) {
                    console.error("Auth initialization failed", error);
                    // Token might be invalid, api.js interceptor handles 401
                }
            }
            setLoading(false);
        };
        initAuth();
    }, []);

    const login = async (username, password) => {
        try {
            const data = await MemoApi.login(username, password);
            if (data.tokens && data.tokens.access_token) {
                setUser({ username, ...data.user });
                return true;
            }
            return false;
        } catch (error) {
            console.error("Login failed", error);
            throw error;
        }
    };

    const logout = async () => {
        try {
            await MemoApi.logout();
        } catch (error) {
            console.error("Logout failed", error);
        } finally {
            localStorage.removeItem('access_token');
            setUser(null);
            // Optional: window.location.href = '/login'; // Let the router handle this via ProtectedRoute
        }
    };

    return (
        <AuthContext.Provider value={{ user, login, logout, loading, isAuthenticated: !!user }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => useContext(AuthContext);
