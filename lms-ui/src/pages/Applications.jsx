import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { loanApplications } from '../api';

export default function Applications() {
    const [applications, setApplications] = useState([]);
    const [loading, setLoading] = useState(true);
    const [filter, setFilter] = useState('ALL');

    useEffect(() => {
        loadApplications();
    }, [filter]);

    const loadApplications = async () => {
        try {
            let data;
            if (filter === 'ALL') {
                data = await loanApplications.getMyApplications();
            } else {
                data = await loanApplications.getByStatus(filter);
            }
            setApplications(data || []);
        } catch (error) {
            console.error('Failed to load applications:', error);
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

    return (
        <div className="main-content">
            <div className="page-header">
                <h1 className="page-title">My Applications</h1>
                <Link to="/applications/new" className="btn btn-primary">
                    + New Application
                </Link>
            </div>

            {/* Filters */}
            <div className="card" style={{ padding: 'var(--spacing-4)' }}>
                <div style={{ display: 'flex', gap: 'var(--spacing-2)' }}>
                    {['ALL', 'DRAFT', 'SUBMITTED', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'DISBURSED'].map(status => (
                        <button
                            key={status}
                            className={`btn ${filter === status ? 'btn-primary' : 'btn-secondary'}`}
                            onClick={() => setFilter(status)}
                        >
                            {status.replace('_', ' ')}
                        </button>
                    ))}
                </div>
            </div>

            {/* Applications Table */}
            <div className="card">
                {loading ? (
                    <p>Loading...</p>
                ) : applications.length === 0 ? (
                    <p style={{ textAlign: 'center', color: 'var(--color-gray-500)' }}>
                        No applications found
                    </p>
                ) : (
                    <div className="table-container">
                        <table>
                            <thead>
                                <tr>
                                    <th>Application #</th>
                                    <th>Loan Type</th>
                                    <th>Applicant</th>
                                    <th>Amount</th>
                                    <th>Tenure</th>
                                    <th>Status</th>
                                    <th>Applied Date</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {applications.map((app) => (
                                    <tr key={app.id}>
                                        <td>
                                            <Link to={`/applications/${app.id}`}>
                                                <strong>{app.applicationNumber}</strong>
                                            </Link>
                                        </td>
                                        <td>{app.loanProductName}</td>
                                        <td>{app.applicantName}</td>
                                        <td>₹{app.requestedAmount?.toLocaleString()}</td>
                                        <td>{app.requestedTenure} months</td>
                                        <td>
                                            <span className={`badge ${getStatusBadge(app.status)}`}>
                                                {app.status}
                                            </span>
                                        </td>
                                        <td>{new Date(app.createdAt).toLocaleDateString()}</td>
                                        <td>
                                            <Link to={`/applications/${app.id}`} className="btn btn-secondary">
                                                View
                                            </Link>
                                        </td>
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
