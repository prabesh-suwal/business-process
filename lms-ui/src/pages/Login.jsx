import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { auth, getToken } from '../api';

// Session storage key to prevent multiple SSO attempts
const SSO_CHECK_KEY = 'sso_check_in_progress';

export default function Login() {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const [checkingSSO, setCheckingSSO] = useState(true);
    const navigate = useNavigate();

    // Check for existing SSO session on mount (only once per session)
    useEffect(() => {
        const checkExistingSession = async () => {
            // If already logged in, redirect home
            if (getToken()) {
                setCheckingSSO(false);
                navigate('/', { replace: true });
                return;
            }

            // Skip SSO auto-login if there was a recent auth failure (prevents loop)
            if (sessionStorage.getItem('auth_failed') === 'true') {
                console.log('Skipping SSO auto-login due to recent auth failure');
                sessionStorage.removeItem('auth_failed');
                setCheckingSSO(false);
                return;
            }

            // Prevent multiple SSO check attempts using sessionStorage
            if (sessionStorage.getItem(SSO_CHECK_KEY) === 'true') {
                console.log('SSO check already attempted this session');
                setCheckingSSO(false);
                return;
            }

            // Mark SSO check as in progress
            sessionStorage.setItem(SSO_CHECK_KEY, 'true');

            // Check if user has an SSO session from another product
            try {
                const ssoSession = await auth.checkSSO();
                if (ssoSession) {
                    // SSO session exists! Get tokens for LMS product
                    console.log('SSO session found, getting LMS tokens...');
                    const result = await auth.getTokenForProduct('LMS');
                    if (result && result.tokens) {
                        console.log('SSO auto-login successful!');
                        // Clear the flag on successful SSO login
                        sessionStorage.removeItem(SSO_CHECK_KEY);
                        navigate('/', { replace: true });
                        return;
                    }
                }
            } catch (err) {
                console.warn('SSO auto-login failed:', err);
            }

            setCheckingSSO(false);
        };

        checkExistingSession();
    }, []); // Empty dependency - run only once

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            await auth.login(username, password);
            // Clear SSO check flag on successful login
            sessionStorage.removeItem(SSO_CHECK_KEY);
            navigate('/', { replace: true });
        } catch (err) {
            setError(err.message || 'Login failed. Please check your credentials.');
        } finally {
            setLoading(false);
        }
    };

    // Show loading while checking SSO
    if (checkingSSO) {
        return (
            <div className="login-container">
                <div className="login-card">
                    <div className="login-header">
                        <span className="login-logo">🏦</span>
                        <h1>LMS Portal</h1>
                    </div>
                    <div style={{ textAlign: 'center', padding: '2rem' }}>
                        <p>Checking for existing session...</p>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="login-container">
            <div className="login-card">
                <div className="login-header">
                    <span className="login-logo">🏦</span>
                    <h1>LMS Portal</h1>
                    <p>Loan Management System</p>
                </div>

                <form onSubmit={handleSubmit} className="login-form">
                    {error && <div className="form-error">{error}</div>}

                    <div className="form-group">
                        <label className="form-label">Username</label>
                        <input
                            type="text"
                            className="form-input"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            placeholder="Enter your username"
                            required
                            autoFocus
                        />
                    </div>

                    <div className="form-group">
                        <label className="form-label">Password</label>
                        <input
                            type="password"
                            className="form-input"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            placeholder="Enter your password"
                            required
                        />
                    </div>

                    <button
                        type="submit"
                        className="btn btn-primary btn-block"
                        disabled={loading}
                    >
                        {loading ? 'Signing in...' : 'Sign In'}
                    </button>
                </form>

                <div className="login-footer">
                    <p className="text-muted">Secure access to banking services</p>
                </div>
            </div>
        </div>
    );
}
