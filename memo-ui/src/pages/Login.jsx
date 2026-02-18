import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { MemoApi } from '../lib/api';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '../components/ui/card';
import { FileText, Loader2, Eye, EyeOff } from 'lucide-react';
import { toast } from 'sonner';

// Session storage key to prevent multiple SSO attempts in same browser session
const SSO_CHECK_KEY = 'sso_check_mms_in_progress';

export default function Login() {
    const navigate = useNavigate();
    const { login, isAuthenticated } = useAuth();
    const [loading, setLoading] = useState(false);
    const [showPassword, setShowPassword] = useState(false);
    const [checkingSSO, setCheckingSSO] = useState(true);
    const [credentials, setCredentials] = useState({ username: '', password: '' });

    // Check for existing SSO session on mount (e.g., user already logged into admin-ui)
    useEffect(() => {
        const checkExistingSession = async () => {
            // If already logged in, redirect home
            if (isAuthenticated || localStorage.getItem('access_token')) {
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
                const ssoSession = await MemoApi.checkSSO();
                if (ssoSession) {
                    // SSO session exists! Get tokens for MMS product
                    console.log('SSO session found, getting MMS tokens...');
                    const result = await MemoApi.getTokenForProduct('MMS');
                    if (result && result.tokens) {
                        console.log('SSO auto-login successful!');
                        // Clear the flag on successful SSO login
                        sessionStorage.removeItem(SSO_CHECK_KEY);
                        // Force a page reload to re-initialize AuthContext with new tokens
                        window.location.href = '/';
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

    const handleLogin = async (e) => {
        e.preventDefault();
        setLoading(true);
        try {
            await login(credentials.username, credentials.password);
            toast.success("Login successful");
            // Clear SSO check flag on successful login
            sessionStorage.removeItem(SSO_CHECK_KEY);
            navigate('/');
        } catch (error) {
            console.error(error);
            toast.error("Login failed. Please check your credentials.");
        } finally {
            setLoading(false);
        }
    };

    // Show loading while checking SSO
    if (checkingSSO) {
        return (
            <div className="flex h-screen w-full items-center justify-center bg-muted/40 font-sans">
                <Card className="w-full max-w-sm shadow-xl">
                    <CardHeader className="space-y-1 text-center">
                        <div className="flex justify-center mb-4">
                            <div className="bg-primary p-3 rounded-xl text-primary-foreground">
                                <FileText className="h-8 w-8" />
                            </div>
                        </div>
                        <CardTitle className="text-2xl font-bold">Memo System</CardTitle>
                    </CardHeader>
                    <CardContent className="text-center py-8">
                        <Loader2 className="h-6 w-6 animate-spin mx-auto mb-2" />
                        <p className="text-sm text-muted-foreground">Checking for existing session...</p>
                    </CardContent>
                </Card>
            </div>
        );
    }

    return (
        <div className="flex h-screen w-full items-center justify-center bg-muted/40 font-sans">
            <Card className="w-full max-w-sm shadow-xl">
                <CardHeader className="space-y-1 text-center">
                    <div className="flex justify-center mb-4">
                        <div className="bg-primary p-3 rounded-xl text-primary-foreground">
                            <FileText className="h-8 w-8" />
                        </div>
                    </div>
                    <CardTitle className="text-2xl font-bold">Memo System</CardTitle>
                    <CardDescription>Enters your credentials to access the dashboard</CardDescription>
                </CardHeader>
                <form onSubmit={handleLogin}>
                    <CardContent className="space-y-4">
                        <div className="space-y-2">
                            <Label htmlFor="username">Username</Label>
                            <Input
                                id="username"
                                placeholder="admin"
                                required
                                value={credentials.username}
                                onChange={(e) => setCredentials({ ...credentials, username: e.target.value })}
                            />
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="password">Password</Label>
                            <div className="relative">
                                <Input
                                    id="password"
                                    type={showPassword ? "text" : "password"}
                                    required
                                    value={credentials.password}
                                    onChange={(e) => setCredentials({ ...credentials, password: e.target.value })}
                                    className="pr-10"
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowPassword(!showPassword)}
                                    className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-600"
                                >
                                    {showPassword ? (
                                        <EyeOff className="h-5 w-5" />
                                    ) : (
                                        <Eye className="h-5 w-5" />
                                    )}
                                </button>
                            </div>
                        </div>
                    </CardContent>
                    <CardFooter>
                        <Button className="w-full" type="submit" disabled={loading}>
                            {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                            Sign In
                        </Button>
                    </CardFooter>
                </form>
            </Card>
        </div>
    );
}
