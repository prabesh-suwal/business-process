import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { auth } from '../api'

export default function Layout() {
    const navigate = useNavigate();

    const handleLogout = () => {
        auth.globalLogout();
    };

    return (
        <div className="app-layout">
            <aside className="sidebar">
                <div className="sidebar-logo">CAS Admin</div>
                <nav>
                    <ul className="sidebar-nav">
                        <li>
                            <NavLink to="/" end>
                                <span>📊</span> Dashboard
                            </NavLink>
                        </li>
                        <li>
                            <NavLink to="/users">
                                <span>👥</span> Users
                            </NavLink>
                        </li>
                        <li>
                            <NavLink to="/roles">
                                <span>🔑</span> Roles
                            </NavLink>
                        </li>
                        <li>
                            <NavLink to="/products">
                                <span>📦</span> Products
                            </NavLink>
                        </li>
                        <li>
                            <NavLink to="/permissions">
                                <span>🔐</span> Permissions
                            </NavLink>
                        </li>
                        <li>
                            <NavLink to="/clients">
                                <span>🔌</span> API Clients
                            </NavLink>
                        </li>
                        <li>
                            <NavLink to="/audit-logs">
                                <span>📋</span> Audit Logs
                            </NavLink>
                        </li>
                        <li>
                            <NavLink to="/policies">
                                <span>📜</span> Policies
                            </NavLink>
                        </li>
                        <li className="nav-section">Workflow Management</li>
                        <li>
                            <NavLink to="/workflows">
                                <span>🔄</span> Workflows
                            </NavLink>
                        </li>
                        <li>
                            <NavLink to="/workflow-configs">
                                <span>⚙️</span> Configurations
                            </NavLink>
                        </li>
                        <li>
                            <NavLink to="/forms">
                                <span>📝</span> Forms
                            </NavLink>
                        </li>
                        <li className="nav-section">Organization</li>
                        <li>
                            <NavLink to="/branches">
                                <span>🏪</span> Branches
                            </NavLink>
                        </li>
                        <li>
                            <NavLink to="/departments">
                                <span>🏢</span> Departments
                            </NavLink>
                        </li>
                        <li>
                            <NavLink to="/groups">
                                <span>👥</span> Groups
                            </NavLink>
                        </li>
                    </ul>
                </nav>
                <div style={{ marginTop: 'auto', paddingTop: '24px' }}>
                    <button onClick={handleLogout} className="btn btn-secondary" style={{ width: '100%' }}>
                        Logout
                    </button>
                </div>
            </aside>
            <main className="main-content">
                <Outlet />
            </main>
        </div>
    );
}
