import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { auth, setToken, getToken } from '../api'

// Session storage key to prevent multiple SSO attempts
const SSO_CHECK_KEY = 'sso_check_cas_admin_in_progress';

export default function LoginPage() {
    const navigate = useNavigate();
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const [checkingSSO, setCheckingSSO] = useState(true);

    // Check for existing SSO session on mount (only once per session)
    useEffect(() => {
        const checkExistingSession = async () => {
            // If already logged in, redirect home
            if (getToken()) {
                setCheckingSSO(false);
                navigate('/', { replace: true });
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
                    // SSO session exists! Get tokens for CAS_ADMIN product
                    console.log('SSO session found, getting CAS_ADMIN tokens...');
                    const result = await auth.getTokenForProduct('CAS_ADMIN');
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
            const response = await auth.login(username, password);
            setToken(response.tokens.access_token);
            localStorage.setItem('refreshToken', response.tokens.refresh_token);
            localStorage.setItem('user', JSON.stringify(response.user));
            // Clear SSO check flag on successful login
            sessionStorage.removeItem(SSO_CHECK_KEY);
            navigate('/', { replace: true });
        } catch (err) {
            setError(err.message || 'Login failed');
        } finally {
            setLoading(false);
        }
    };

    // Show loading while checking SSO
    if (checkingSSO) {
        return (
            <div className="login-container">
                <div className="login-card">
                    <h1 className="login-title">CAS Admin</h1>
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
                <h1 className="login-title">CAS Admin</h1>

                {error && (
                    <div className="form-error" style={{ marginBottom: '16px', textAlign: 'center' }}>
                        {error}
                    </div>
                )}

                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label className="form-label" htmlFor="username">Username</label>
                        <input
                            id="username"
                            type="text"
                            className="form-input"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            required
                            autoFocus
                        />
                    </div>

                    <div className="form-group">
                        <label className="form-label" htmlFor="password">Password</label>
                        <input
                            id="password"
                            type="password"
                            className="form-input"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                        />
                    </div>

                    <button type="submit" className="btn btn-primary" style={{ width: '100%' }} disabled={loading}>
                        {loading ? 'Signing in...' : 'Sign In'}
                    </button>
                </form>
            </div>
        </div>
    );
}
