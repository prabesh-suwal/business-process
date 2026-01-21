import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { loanApplications, persons } from '../api';

export default function ApplicationDetail() {
    const { id } = useParams();
    const [application, setApplication] = useState(null);
    const [personView, setPersonView] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        loadApplication();
    }, [id]);

    const loadApplication = async () => {
        try {
            const app = await loanApplications.get(id);
            setApplication(app);

            // Load person 360° view if personId exists
            if (app.personId) {
                try {
                    const view = await persons.get360View(app.personId);
                    setPersonView(view);
                } catch (e) {
                    console.warn('Could not load person view:', e);
                }
            }
        } catch (err) {
            setError('Failed to load application');
        } finally {
            setLoading(false);
        }
    };

    const getStatusBadge = (status) => {
        const statusMap = {
            DRAFT: { class: 'badge-draft', color: '#6b7280' },
            SUBMITTED: { class: 'badge-submitted', color: '#3b82f6' },
            UNDER_REVIEW: { class: 'badge-pending', color: '#f59e0b' },
            PENDING_DOCS: { class: 'badge-pending', color: '#f59e0b' },
            PENDING_APPROVAL: { class: 'badge-pending', color: '#f59e0b' },
            APPROVED: { class: 'badge-approved', color: '#10b981' },
            CONDITIONALLY_APPROVED: { class: 'badge-approved', color: '#10b981' },
            REJECTED: { class: 'badge-rejected', color: '#ef4444' },
            CANCELLED: { class: 'badge-rejected', color: '#ef4444' },
            DISBURSED: { class: 'badge-disbursed', color: '#8b5cf6' },
        };
        return statusMap[status] || { class: 'badge-draft', color: '#6b7280' };
    };

    if (loading) {
        return <div className="main-content"><p>Loading application...</p></div>;
    }

    if (error || !application) {
        return (
            <div className="main-content">
                <div className="card" style={{ background: '#fee2e2', color: '#b91c1c' }}>
                    {error || 'Application not found'}
                </div>
            </div>
        );
    }

    const statusInfo = getStatusBadge(application.status);

    return (
        <div className="main-content">
            <div className="page-header">
                <div>
                    <Link to="/applications" style={{ color: 'var(--color-gray-500)', marginBottom: 'var(--spacing-2)', display: 'block' }}>
                        ← Back to Applications
                    </Link>
                    <h1 className="page-title">{application.applicationNumber}</h1>
                </div>
                <span className={`badge ${statusInfo.class}`} style={{ fontSize: '1rem', padding: 'var(--spacing-2) var(--spacing-4)' }}>
                    {application.status?.replace(/_/g, ' ')}
                </span>
            </div>

            <div className="grid-2">
                {/* Loan Details */}
                <div className="card">
                    <h3 style={{ marginBottom: 'var(--spacing-4)' }}>📋 Loan Details</h3>
                    <div className="detail-grid">
                        <div className="detail-item">
                            <span className="detail-label">Loan Product</span>
                            <span className="detail-value">{application.loanProductName}</span>
                        </div>
                        <div className="detail-item">
                            <span className="detail-label">Application Type</span>
                            <span className="detail-value">{application.applicationType || 'NEW'}</span>
                        </div>
                        <div className="detail-item">
                            <span className="detail-label">Requested Amount</span>
                            <span className="detail-value">₹{application.requestedAmount?.toLocaleString()}</span>
                        </div>
                        <div className="detail-item">
                            <span className="detail-label">Approved Amount</span>
                            <span className="detail-value">{application.approvedAmount ? `₹${application.approvedAmount.toLocaleString()}` : '-'}</span>
                        </div>
                        <div className="detail-item">
                            <span className="detail-label">Interest Rate</span>
                            <span className="detail-value">{application.interestRate}% p.a.</span>
                        </div>
                        <div className="detail-item">
                            <span className="detail-label">Tenure</span>
                            <span className="detail-value">{application.requestedTenure} months</span>
                        </div>
                        {application.loanPurpose && (
                            <div className="detail-item" style={{ gridColumn: '1 / -1' }}>
                                <span className="detail-label">Purpose</span>
                                <span className="detail-value">{application.loanPurpose}</span>
                            </div>
                        )}
                    </div>
                </div>

                {/* Applicant Details */}
                <div className="card">
                    <h3 style={{ marginBottom: 'var(--spacing-4)' }}>👤 Applicant Details</h3>
                    <div className="detail-grid">
                        <div className="detail-item">
                            <span className="detail-label">Name</span>
                            <span className="detail-value">{application.applicantName}</span>
                        </div>
                        <div className="detail-item">
                            <span className="detail-label">Email</span>
                            <span className="detail-value">{application.applicantEmail}</span>
                        </div>
                        <div className="detail-item">
                            <span className="detail-label">Phone</span>
                            <span className="detail-value">{application.applicantPhone || '-'}</span>
                        </div>
                        {application.personId && (
                            <div className="detail-item">
                                <span className="detail-label">Person ID</span>
                                <span className="detail-value" style={{ fontSize: '0.75rem' }}>{application.personId}</span>
                            </div>
                        )}
                    </div>

                    {/* 360° View Link */}
                    {personView && (
                        <div style={{ marginTop: 'var(--spacing-4)', padding: 'var(--spacing-3)', background: 'var(--color-primary-50)', borderRadius: 'var(--radius-md)' }}>
                            <strong>360° Customer View</strong>
                            <div style={{ fontSize: '0.875rem', color: 'var(--color-gray-600)', marginTop: 'var(--spacing-2)' }}>
                                <span>Roles: {personView.summary?.totalRoles || 0}</span>
                                <span style={{ marginLeft: 'var(--spacing-4)' }}>Relationships: {personView.summary?.relationships || 0}</span>
                            </div>
                        </div>
                    )}
                </div>

                {/* Workflow Status */}
                <div className="card">
                    <h3 style={{ marginBottom: 'var(--spacing-4)' }}>⚙️ Workflow Status</h3>
                    <div className="detail-grid">
                        <div className="detail-item">
                            <span className="detail-label">Current Task</span>
                            <span className="detail-value">{application.currentTaskName || 'No active task'}</span>
                        </div>
                        <div className="detail-item">
                            <span className="detail-label">Assigned To</span>
                            <span className="detail-value">{application.currentTaskAssignee || '-'}</span>
                        </div>
                        {application.taskSlaDeadline && (
                            <div className="detail-item">
                                <span className="detail-label">SLA Deadline</span>
                                <span className="detail-value" style={{ color: new Date(application.taskSlaDeadline) < new Date() ? '#ef4444' : 'inherit' }}>
                                    {new Date(application.taskSlaDeadline).toLocaleString()}
                                </span>
                            </div>
                        )}
                        {application.subStatus && (
                            <div className="detail-item">
                                <span className="detail-label">Sub-Status</span>
                                <span className="detail-value">{application.subStatus}</span>
                            </div>
                        )}
                    </div>
                </div>

                {/* Timeline */}
                <div className="card">
                    <h3 style={{ marginBottom: 'var(--spacing-4)' }}>📅 Timeline</h3>
                    <div className="detail-grid">
                        <div className="detail-item">
                            <span className="detail-label">Created</span>
                            <span className="detail-value">{application.createdAt ? new Date(application.createdAt).toLocaleString() : '-'}</span>
                        </div>
                        <div className="detail-item">
                            <span className="detail-label">Submitted</span>
                            <span className="detail-value">{application.submittedAt ? new Date(application.submittedAt).toLocaleString() : '-'}</span>
                        </div>
                        {application.decidedAt && (
                            <div className="detail-item">
                                <span className="detail-label">Decision Date</span>
                                <span className="detail-value">{new Date(application.decidedAt).toLocaleString()}</span>
                            </div>
                        )}
                        {application.decidedByName && (
                            <div className="detail-item">
                                <span className="detail-label">Decided By</span>
                                <span className="detail-value">{application.decidedByName}</span>
                            </div>
                        )}
                    </div>
                </div>
            </div>

            {/* Co-Applicants */}
            {application.coApplicants && application.coApplicants.length > 0 && (
                <div className="card" style={{ marginTop: 'var(--spacing-4)' }}>
                    <h3 style={{ marginBottom: 'var(--spacing-4)' }}>👥 Co-Applicants / Guarantors</h3>
                    <div className="table-container">
                        <table>
                            <thead>
                                <tr>
                                    <th>Name</th>
                                    <th>Role</th>
                                    <th>Email</th>
                                    <th>Phone</th>
                                </tr>
                            </thead>
                            <tbody>
                                {application.coApplicants.map((co, idx) => (
                                    <tr key={idx}>
                                        <td>{co.name}</td>
                                        <td>
                                            <span className="badge badge-draft">{co.role}</span>
                                        </td>
                                        <td>{co.email || '-'}</td>
                                        <td>{co.phone || '-'}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}

            {/* Decision/Rejection */}
            {(application.decisionComments || application.rejectionReason) && (
                <div className="card" style={{ marginTop: 'var(--spacing-4)', background: application.status === 'REJECTED' ? '#fef2f2' : '#f0fdf4' }}>
                    <h3 style={{ marginBottom: 'var(--spacing-4)' }}>
                        {application.status === 'REJECTED' ? '❌ Rejection' : '✅ Decision'}
                    </h3>
                    <p>{application.rejectionReason || application.decisionComments}</p>
                </div>
            )}
        </div>
    );
}
