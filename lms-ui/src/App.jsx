import { BrowserRouter, Routes, Route, NavLink, Navigate, useLocation } from 'react-router-dom';
import { useState, useEffect } from 'react';
import Dashboard from './pages/Dashboard';
import LoanProducts from './pages/LoanProducts';
import Applications from './pages/Applications';
import ApplicationDetail from './pages/ApplicationDetail';
import NewApplication from './pages/NewApplication';
import TaskInbox from './pages/TaskInbox';
import ProductManagement from './pages/ProductManagement';
import Login from './pages/Login';
import { getToken, clearToken, auth } from './api';
import './index.css';

// Protected Route Component
function ProtectedRoute({ children }) {
  const token = getToken();
  const location = useLocation();

  if (!token) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return children;
}

// Get user from localStorage
function getUser() {
  try {
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
  } catch {
    return null;
  }
}

// Main App Layout with Sidebar
function AppLayout({ children }) {
  const [user, setUser] = useState(getUser());

  useEffect(() => {
    setUser(getUser());
  }, []);

  const handleLogout = () => {
    auth.globalLogout();
  };

  return (
    <div className="app-container">
      {/* Sidebar */}
      <aside className="sidebar">
        <div className="sidebar-logo">
          <span>🏦</span>
          <span>LMS Portal</span>
        </div>

        {user && (
          <div className="user-info">
            <div className="user-avatar">👤</div>
            <div className="user-details">
              <div className="user-name">{user.name || user.username}</div>
              <div className="user-role">{user.roles?.[0] || 'User'}</div>
            </div>
          </div>
        )}

        <nav className="sidebar-nav">
          <NavLink to="/" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`} end>
            📊 Dashboard
          </NavLink>
          <NavLink to="/products" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
            💳 Loan Products
          </NavLink>
          <NavLink to="/applications" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
            📝 My Applications
          </NavLink>
          <NavLink to="/tasks" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
            📥 Task Inbox
          </NavLink>
          <NavLink to="/applications/new" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
            ➕ New Application
          </NavLink>

          <div style={{ marginTop: 'var(--spacing-4)', paddingTop: 'var(--spacing-4)', borderTop: '1px solid rgba(255,255,255,0.1)', fontSize: '0.75rem', color: 'rgba(255,255,255,0.5)' }}>ADMIN</div>
          <NavLink to="/admin/products" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
            ⚙️ Manage Products
          </NavLink>
        </nav>

        <div style={{ marginTop: 'auto', paddingTop: 'var(--spacing-6)', borderTop: '1px solid rgba(255,255,255,0.1)' }}>
          <div className="nav-item" style={{ cursor: 'pointer' }} onClick={handleLogout}>
            🚪 Logout
          </div>
        </div>
      </aside>

      {/* Main Content */}
      {children}
    </div>
  );
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public Route - Login */}
        <Route path="/login" element={<Login />} />

        {/* Protected Routes */}
        <Route path="/" element={
          <ProtectedRoute>
            <AppLayout><Dashboard /></AppLayout>
          </ProtectedRoute>
        } />
        <Route path="/products" element={
          <ProtectedRoute>
            <AppLayout><LoanProducts /></AppLayout>
          </ProtectedRoute>
        } />
        <Route path="/applications" element={
          <ProtectedRoute>
            <AppLayout><Applications /></AppLayout>
          </ProtectedRoute>
        } />
        <Route path="/applications/new" element={
          <ProtectedRoute>
            <AppLayout><NewApplication /></AppLayout>
          </ProtectedRoute>
        } />
        <Route path="/applications/:id" element={
          <ProtectedRoute>
            <AppLayout><ApplicationDetail /></AppLayout>
          </ProtectedRoute>
        } />
        <Route path="/tasks" element={
          <ProtectedRoute>
            <AppLayout><TaskInbox /></AppLayout>
          </ProtectedRoute>
        } />
        <Route path="/admin/products" element={
          <ProtectedRoute>
            <AppLayout><ProductManagement /></AppLayout>
          </ProtectedRoute>
        } />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
