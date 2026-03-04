import React, { createContext, useContext, useState, useEffect } from 'react';
import { MemoApi } from '../lib/api';
import { useAccess } from './AccessContext';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const { fetchAccess, clearAccess } = useAccess();

    useEffect(() => {
        // Check for existing session on mount
        const initAuth = async () => {
            const token = localStorage.getItem('access_token');
            if (token) {
                try {
                    const session = await MemoApi.getSession().catch(() => null);
                    if (session && session.active) {
                        setUser({ username: session.username, ...session });
                        // Fetch effective access context
                        await fetchAccess();
                    } else {
                        setUser({ isAuthenticated: true });
                        await fetchAccess();
                    }
                } catch (error) {
                    console.error("Auth initialization failed", error);
                }
            }
            setLoading(false);
        };
        initAuth();
    }, []); // eslint-disable-line react-hooks/exhaustive-deps

    const login = async (username, password) => {
        try {
            const data = await MemoApi.login(username, password);
            if (data.tokens && data.tokens.access_token) {
                setUser({ username, ...data.user });
                // Fetch effective access context after successful login
                await fetchAccess();
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
            clearAccess();
        }
    };

    return (
        <AuthContext.Provider value={{ user, login, logout, loading, isAuthenticated: !!user }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => useContext(AuthContext);
