import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { loanApplications, loanProducts } from '../api';

export default function Dashboard() {
    const [stats, setStats] = useState({
        totalApplications: 0,
        pendingReview: 0,
        approved: 0,
        disbursed: 0,
    });
    const [recentApplications, setRecentApplications] = useState([]);
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        loadDashboardData();
    }, []);

    const loadDashboardData = async () => {
        try {
            const [prods, submitted, pending, approved, disbursed] = await Promise.all([
                loanProducts.list(),
                loanApplications.getByStatus('SUBMITTED').catch(() => []),
                loanApplications.getByStatus('PENDING_APPROVAL').catch(() => []),
                loanApplications.getByStatus('APPROVED').catch(() => []),
                loanApplications.getByStatus('DISBURSED').catch(() => []),
            ]);

            setProducts(prods || []);
            setStats({
                totalApplications: submitted.length + pending.length + approved.length + disbursed.length,
                pendingReview: submitted.length + pending.length,
                approved: approved.length,
                disbursed: disbursed.length,
            });
            setRecentApplications([...submitted, ...pending].slice(0, 5));
        } catch (error) {
            console.error('Failed to load dashboard data:', error);
        } finally {
            setLoading(false);
        }
    };

    const getStatusBadge = (status) => {
        const statusMap = {
            DRAFT: 'badge-draft',
            SUBMITTED: 'badge-submitted',
            UNDER_REVIEW: 'badge-pending',
            PENDING_APPROVAL: 'badge-pending',
            APPROVED: 'badge-approved',
            REJECTED: 'badge-rejected',
            DISBURSED: 'badge-disbursed',
        };
        return statusMap[status] || 'badge-draft';
    };

    if (loading) {
        return <div className="main-content"><p>Loading dashboard...</p></div>;
    }

    return (
        <div className="main-content">
            <div className="page-header">
                <h1 className="page-title">Dashboard</h1>
                <Link to="/applications/new" className="btn btn-primary">
                    + New Application
                </Link>
            </div>

            {/* Stats Grid */}
            <div className="stats-grid">
                <div className="stat-card">
                    <span className="stat-value">{stats.totalApplications}</span>
                    <span className="stat-label">Total Applications</span>
                </div>
                <div className="stat-card">
                    <span className="stat-value">{stats.pendingReview}</span>
                    <span className="stat-label">Pending Review</span>
                </div>
                <div className="stat-card">
                    <span className="stat-value">{stats.approved}</span>
                    <span className="stat-label">Approved</span>
                </div>
                <div className="stat-card">
                    <span className="stat-value">{stats.disbursed}</span>
                    <span className="stat-label">Disbursed</span>
                </div>
            </div>

            {/* Quick Actions - Loan Products */}
            <div className="card">
                <div className="card-header">
                    <h2 className="card-title">Apply for a Loan</h2>
                </div>
                <div className="grid-4">
                    {products.map((product) => (
                        <Link
                            key={product.id}
                            to={`/applications/new?product=${product.code}`}
                            className="stat-card"
                            style={{ textDecoration: 'none', cursor: 'pointer' }}
                        >
                            <span className="stat-label">{product.name}</span>
                            <span style={{ fontSize: '0.875rem', color: 'var(--color-gray-600)' }}>
                                {product.interestRate}% p.a.
                            </span>
                            <span style={{ fontSize: '0.75rem', color: 'var(--color-gray-500)' }}>
                                ₹{(product.minAmount / 100000).toFixed(0)}L - ₹{(product.maxAmount / 100000).toFixed(0)}L
                            </span>
                        </Link>
                    ))}
                </div>
            </div>

            {/* Recent Applications */}
            <div className="card">
                <div className="card-header">
                    <h2 className="card-title">Recent Applications</h2>
                    <Link to="/applications" className="btn btn-secondary">View All</Link>
                </div>
                {recentApplications.length === 0 ? (
                    <p style={{ color: 'var(--color-gray-500)' }}>No applications yet</p>
                ) : (
                    <div className="table-container">
                        <table>
                            <thead>
                                <tr>
                                    <th>Application #</th>
                                    <th>Product</th>
                                    <th>Amount</th>
                                    <th>Status</th>
                                    <th>Date</th>
                                </tr>
                            </thead>
                            <tbody>
                                {recentApplications.map((app) => (
                                    <tr key={app.id}>
                                        <td>
                                            <Link to={`/applications/${app.id}`}>
                                                {app.applicationNumber}
                                            </Link>
                                        </td>
                                        <td>{app.loanProductName}</td>
                                        <td>₹{app.requestedAmount?.toLocaleString()}</td>
                                        <td>
                                            <span className={`badge ${getStatusBadge(app.status)}`}>
                                                {app.status}
                                            </span>
                                        </td>
                                        <td>{new Date(app.createdAt).toLocaleDateString()}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        </div>
    );
}
