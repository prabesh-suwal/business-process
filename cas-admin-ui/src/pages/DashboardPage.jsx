import { useState, useEffect } from 'react'
import { users, roles as rolesApi, products } from '../api'

export default function DashboardPage() {
    const [stats, setStats] = useState({ users: 0, roles: 0, clients: 0 });
    const [loading, setLoading] = useState(true);
    const user = JSON.parse(localStorage.getItem('user') || '{}');

    useEffect(() => {
        const loadStats = async () => {
            try {
                const [usersData, rolesData, productsData] = await Promise.all([
                    users.list(0, 1),
                    rolesApi.list(),
                    products.list()
                ]);
                setStats({
                    users: usersData.totalElements || 0,
                    roles: rolesData?.length || 0,
                    products: productsData?.length || 0
                });
            } catch (err) {
                console.error('Failed to load stats:', err);
            } finally {
                setLoading(false);
            }
        };
        loadStats();
    }, []);

    if (loading) {
        return <div className="loading">Loading...</div>;
    }

    return (
        <div>
            <div className="page-header">
                <h1>Dashboard</h1>
                <span className="text-muted">Welcome, {user.firstName || user.username}</span>
            </div>

            <div className="stats-grid">
                <div className="stat-card">
                    <div className="stat-label">Total Users</div>
                    <div className="stat-value">{stats.users}</div>
                </div>
                <div className="stat-card">
                    <div className="stat-label">Total Roles</div>
                    <div className="stat-value">{stats.roles}</div>
                </div>
                <div className="stat-card">
                    <div className="stat-label">Products</div>
                    <div className="stat-value">{stats.products}</div>
                </div>
            </div>

            <div className="card">
                <h3 style={{ marginBottom: '16px' }}>Quick Actions</h3>
                <div className="flex gap-4">
                    <a href="/users" className="btn btn-primary">Manage Users</a>
                    <a href="/roles" className="btn btn-secondary">Manage Roles</a>
                    <a href="/clients" className="btn btn-secondary">API Clients</a>
                </div>
            </div>
        </div>
    );
}
