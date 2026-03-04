import { useState, useEffect } from 'react'
import { makerChecker } from '../api'

const STATUS_COLORS = {
    PENDING: { bg: '#fef3c7', color: '#92400e', border: '#fbbf24' },
    APPROVED: { bg: '#d1fae5', color: '#065f46', border: '#34d399' },
    EXECUTED: { bg: '#dbeafe', color: '#1e40af', border: '#60a5fa' },
    REJECTED: { bg: '#fee2e2', color: '#991b1b', border: '#f87171' },
    FAILED: { bg: '#fee2e2', color: '#991b1b', border: '#f87171' },
    EXPIRED: { bg: '#f3f4f6', color: '#6b7280', border: '#9ca3af' },
};

const STATUS_TABS = ['ALL', 'PENDING', 'APPROVED', 'REJECTED', 'EXECUTED'];

export default function ApprovalsPage() {
    const [approvals, setApprovals] = useState({ content: [], totalElements: 0 });
    const [loading, setLoading] = useState(true);
    const [statusFilter, setStatusFilter] = useState('PENDING');
    const [page, setPage] = useState(0);
    const [selectedApproval, setSelectedApproval] = useState(null);
    const [comment, setComment] = useState('');
    const [actionLoading, setActionLoading] = useState(false);

    const loadApprovals = async () => {
        setLoading(true);
        try {
            const status = statusFilter === 'ALL' ? null : statusFilter;
            const result = await makerChecker.approvals(status, page, 20);
            setApprovals(result);
        } catch (err) {
            console.error('Failed to load approvals:', err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { loadApprovals(); }, [statusFilter, page]);

    const handleApprove = async (id) => {
        if (!confirm('Are you sure you want to approve this request?')) return;
        setActionLoading(true);
        try {
            await makerChecker.approve(id, { comment: comment || null });
            setSelectedApproval(null);
            setComment('');
            loadApprovals();
        } catch (err) {
            alert('Failed to approve: ' + err.message);
        } finally {
            setActionLoading(false);
        }
    };

    const handleReject = async (id) => {
        if (!comment.trim()) {
            alert('Please provide a reason for rejection');
            return;
        }
        setActionLoading(true);
        try {
            await makerChecker.reject(id, { comment });
            setSelectedApproval(null);
            setComment('');
            loadApprovals();
        } catch (err) {
            alert('Failed to reject: ' + err.message);
        } finally {
            setActionLoading(false);
        }
    };

    const formatDate = (dateStr) => {
        if (!dateStr) return '-';
        return new Date(dateStr).toLocaleString('en-US', {
            month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
        });
    };

    const parseRequestBody = (bodyStr) => {
        if (!bodyStr) return null;
        try { return JSON.parse(bodyStr); } catch { return null; }
    };

    const getMethodBadgeColor = (method) => {
        const colors = {
            POST: '#10b981', PUT: '#3b82f6', PATCH: '#f59e0b', DELETE: '#ef4444'
        };
        return colors[method] || '#6b7280';
    };

    return (
        <div>
            <div className="page-header">
                <h1>Approval Requests</h1>
                <span className="text-muted" style={{ fontSize: '14px' }}>
                    Review and manage pending approval requests
                </span>
            </div>

            {/* Status Filter Tabs */}
            <div style={{
                display: 'flex', gap: '4px', marginBottom: '20px',
                background: 'var(--bg-secondary)', padding: '4px', borderRadius: '10px',
                width: 'fit-content'
            }}>
                {STATUS_TABS.map(tab => (
                    <button
                        key={tab}
                        onClick={() => { setStatusFilter(tab); setPage(0); }}
                        style={{
                            padding: '8px 16px', borderRadius: '8px', border: 'none',
                            background: statusFilter === tab ? 'var(--accent)' : 'transparent',
                            color: statusFilter === tab ? '#fff' : 'var(--text-secondary)',
                            cursor: 'pointer', fontSize: '13px', fontWeight: '500',
                            transition: 'all 150ms ease',
                        }}
                    >
                        {tab}
                    </button>
                ))}
            </div>

            {/* Approvals Table */}
            <div className="card">
                {loading ? (
                    <div className="loading">Loading...</div>
                ) : approvals.content?.length === 0 ? (
                    <div style={{ padding: '40px', textAlign: 'center', color: 'var(--text-secondary)' }}>
                        <div style={{ fontSize: '48px', marginBottom: '12px' }}>📋</div>
                        <p>No {statusFilter !== 'ALL' ? statusFilter.toLowerCase() : ''} approval requests found</p>
                    </div>
                ) : (
                    <>
                        <div className="table-container">
                            <table>
                                <thead>
                                    <tr>
                                        <th>Description</th>
                                        <th>Endpoint</th>
                                        <th>Maker</th>
                                        <th>Status</th>
                                        <th>Created</th>
                                        <th>Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {approvals.content.map((approval) => {
                                        const sc = STATUS_COLORS[approval.status] || STATUS_COLORS.PENDING;
                                        return (
                                            <tr key={approval.id}>
                                                <td>
                                                    <div style={{ fontWeight: '500' }}>
                                                        {approval.configDescription || 'N/A'}
                                                    </div>
                                                    <div className="text-muted" style={{ fontSize: '12px' }}>
                                                        {approval.serviceName}
                                                    </div>
                                                </td>
                                                <td>
                                                    <span style={{
                                                        display: 'inline-flex', alignItems: 'center', gap: '6px'
                                                    }}>
                                                        <span style={{
                                                            fontSize: '11px', fontWeight: '600',
                                                            color: getMethodBadgeColor(approval.httpMethod),
                                                            padding: '2px 6px', borderRadius: '4px',
                                                            background: getMethodBadgeColor(approval.httpMethod) + '18',
                                                        }}>
                                                            {approval.httpMethod}
                                                        </span>
                                                        <span className="text-muted" style={{ fontSize: '12px' }}>
                                                            {approval.requestPath}
                                                        </span>
                                                    </span>
                                                </td>
                                                <td>{approval.makerUserName || approval.makerUserId}</td>
                                                <td>
                                                    <span style={{
                                                        padding: '4px 10px', borderRadius: '12px',
                                                        fontSize: '12px', fontWeight: '600',
                                                        background: sc.bg, color: sc.color,
                                                        border: `1px solid ${sc.border}`,
                                                    }}>
                                                        {approval.status}
                                                    </span>
                                                </td>
                                                <td className="text-muted" style={{ fontSize: '13px' }}>
                                                    {formatDate(approval.createdAt)}
                                                </td>
                                                <td>
                                                    <button
                                                        className="btn btn-sm btn-primary"
                                                        onClick={() => { setSelectedApproval(approval); setComment(''); }}
                                                    >
                                                        View
                                                    </button>
                                                </td>
                                            </tr>
                                        );
                                    })}
                                </tbody>
                            </table>
                        </div>

                        <div className="pagination">
                            <span>
                                Showing {approvals.content.length} of {approvals.totalElements} requests
                            </span>
                            <div className="pagination-buttons">
                                <button className="btn btn-sm btn-secondary" disabled={page === 0}
                                    onClick={() => setPage(p => p - 1)}>Previous</button>
                                <button className="btn btn-sm btn-secondary" disabled={approvals.last}
                                    onClick={() => setPage(p => p + 1)}>Next</button>
                            </div>
                        </div>
                    </>
                )}
            </div>

            {/* Approval Detail Modal */}
            {selectedApproval && (
                <div className="modal-overlay" onClick={() => setSelectedApproval(null)}>
                    <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: '800px', maxHeight: '85vh', display: 'flex', flexDirection: 'column' }}>

                        {/* Header */}
                        <div className="modal-header" style={{ borderBottom: '1px solid var(--border)' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                                <h3 style={{ margin: 0 }}>Approval Request</h3>
                                <span style={{
                                    padding: '4px 10px', borderRadius: '12px', fontSize: '12px', fontWeight: '600',
                                    background: (STATUS_COLORS[selectedApproval.status] || STATUS_COLORS.PENDING).bg,
                                    color: (STATUS_COLORS[selectedApproval.status] || STATUS_COLORS.PENDING).color,
                                    border: `1px solid ${(STATUS_COLORS[selectedApproval.status] || STATUS_COLORS.PENDING).border}`,
                                }}>
                                    {selectedApproval.status}
                                </span>
                            </div>
                            <button className="btn btn-sm btn-secondary" onClick={() => setSelectedApproval(null)}>✕</button>
                        </div>

                        {/* Body */}
                        <div className="modal-body" style={{ overflowY: 'auto', flex: 1 }}>

                            {/* Request Info */}
                            <div style={{
                                display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px',
                                padding: '16px', borderRadius: '10px',
                                background: 'var(--bg-secondary)', marginBottom: '20px',
                            }}>
                                <div>
                                    <div className="text-muted" style={{ fontSize: '12px', marginBottom: '4px' }}>Description</div>
                                    <div style={{ fontWeight: '500' }}>{selectedApproval.configDescription || 'N/A'}</div>
                                </div>
                                <div>
                                    <div className="text-muted" style={{ fontSize: '12px', marginBottom: '4px' }}>Endpoint</div>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                                        <span style={{
                                            fontSize: '11px', fontWeight: '700',
                                            color: getMethodBadgeColor(selectedApproval.httpMethod),
                                            padding: '2px 6px', borderRadius: '4px',
                                            background: getMethodBadgeColor(selectedApproval.httpMethod) + '18',
                                        }}>{selectedApproval.httpMethod}</span>
                                        <code style={{ fontSize: '13px' }}>{selectedApproval.requestPath}</code>
                                    </div>
                                </div>
                                <div>
                                    <div className="text-muted" style={{ fontSize: '12px', marginBottom: '4px' }}>Submitted By</div>
                                    <div style={{ fontWeight: '500' }}>{selectedApproval.makerUserName || selectedApproval.makerUserId}</div>
                                </div>
                                <div>
                                    <div className="text-muted" style={{ fontSize: '12px', marginBottom: '4px' }}>Submitted At</div>
                                    <div>{formatDate(selectedApproval.createdAt)}</div>
                                </div>
                                {selectedApproval.checkerUserName && (
                                    <>
                                        <div>
                                            <div className="text-muted" style={{ fontSize: '12px', marginBottom: '4px' }}>Reviewed By</div>
                                            <div style={{ fontWeight: '500' }}>{selectedApproval.checkerUserName}</div>
                                        </div>
                                        <div>
                                            <div className="text-muted" style={{ fontSize: '12px', marginBottom: '4px' }}>Reviewed At</div>
                                            <div>{formatDate(selectedApproval.resolvedAt)}</div>
                                        </div>
                                    </>
                                )}
                                {selectedApproval.checkerComment && (
                                    <div style={{ gridColumn: '1 / -1' }}>
                                        <div className="text-muted" style={{ fontSize: '12px', marginBottom: '4px' }}>Comment</div>
                                        <div style={{
                                            padding: '8px 12px', borderRadius: '6px',
                                            background: 'var(--bg-primary)', fontStyle: 'italic',
                                        }}>"{selectedApproval.checkerComment}"</div>
                                    </div>
                                )}
                            </div>

                            {/* Request Body — Data Comparison */}
                            <RequestDataView
                                requestBody={selectedApproval.requestBody}
                                httpMethod={selectedApproval.httpMethod}
                            />

                            {/* Execution Result */}
                            {selectedApproval.responseStatus && (
                                <div style={{ marginTop: '20px' }}>
                                    <h4 style={{ fontSize: '14px', marginBottom: '10px', color: 'var(--text-secondary)' }}>
                                        📡 Execution Result
                                    </h4>
                                    <div style={{
                                        padding: '12px 16px', borderRadius: '10px',
                                        background: selectedApproval.responseStatus < 400 ? '#d1fae520' : '#fee2e220',
                                        border: `1px solid ${selectedApproval.responseStatus < 400 ? '#34d399' : '#f87171'}`,
                                    }}>
                                        <div style={{ fontWeight: '600', marginBottom: '6px' }}>
                                            Status: {selectedApproval.responseStatus}
                                        </div>
                                        {selectedApproval.responseBody && (
                                            <pre style={{
                                                fontSize: '12px', margin: 0, whiteSpace: 'pre-wrap',
                                                maxHeight: '120px', overflowY: 'auto',
                                            }}>
                                                {tryFormatJson(selectedApproval.responseBody)}
                                            </pre>
                                        )}
                                    </div>
                                </div>
                            )}

                            {/* Checker Action Area */}
                            {selectedApproval.status === 'PENDING' && (
                                <div style={{
                                    marginTop: '24px', padding: '16px', borderRadius: '10px',
                                    border: '1px solid var(--border)', background: 'var(--bg-secondary)',
                                }}>
                                    <h4 style={{ fontSize: '14px', marginBottom: '10px' }}>✍️ Review Action</h4>
                                    <div className="form-group" style={{ marginBottom: '12px' }}>
                                        <label className="form-label">Comment (required for rejection)</label>
                                        <textarea
                                            className="form-input"
                                            value={comment}
                                            onChange={e => setComment(e.target.value)}
                                            rows={3}
                                            placeholder="Add your review comments here..."
                                            style={{ resize: 'vertical' }}
                                        />
                                    </div>
                                    <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
                                        <button
                                            className="btn"
                                            disabled={actionLoading}
                                            onClick={() => handleReject(selectedApproval.id)}
                                            style={{
                                                background: '#fee2e2', color: '#991b1b', border: '1px solid #f87171',
                                                fontWeight: '600',
                                            }}
                                        >
                                            {actionLoading ? '...' : '✕ Reject'}
                                        </button>
                                        <button
                                            className="btn"
                                            disabled={actionLoading}
                                            onClick={() => handleApprove(selectedApproval.id)}
                                            style={{
                                                background: '#10b981', color: '#fff', border: 'none',
                                                fontWeight: '600',
                                            }}
                                        >
                                            {actionLoading ? '...' : '✓ Approve'}
                                        </button>
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}


/**
 * Renders the request body as a clean key-value display.
 * For PUT/PATCH (updates), shows data as "Requested Changes".
 * For POST (create), shows as "New Data".
 */
function RequestDataView({ requestBody, httpMethod }) {
    const data = requestBody ? (() => { try { return JSON.parse(requestBody); } catch { return null; } })() : null;

    if (!data) {
        return requestBody ? (
            <div>
                <h4 style={{ fontSize: '14px', marginBottom: '10px', color: 'var(--text-secondary)' }}>
                    📦 Raw Request Body
                </h4>
                <pre style={{
                    padding: '12px', borderRadius: '8px', background: 'var(--bg-secondary)',
                    fontSize: '12px', whiteSpace: 'pre-wrap', maxHeight: '200px', overflowY: 'auto',
                }}>{requestBody}</pre>
            </div>
        ) : null;
    }

    const isUpdate = httpMethod === 'PUT' || httpMethod === 'PATCH';
    const title = isUpdate ? '📝 Requested Changes' : '🆕 New Data';

    // Filter out internal/empty fields
    const displayData = Object.entries(data).filter(([key, value]) =>
        value !== null && value !== '' && !['password'].includes(key)
    );

    return (
        <div>
            <h4 style={{ fontSize: '14px', marginBottom: '10px', color: 'var(--text-secondary)' }}>{title}</h4>
            <div style={{
                borderRadius: '10px', overflow: 'hidden',
                border: '1px solid var(--border)',
            }}>
                <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                    <thead>
                        <tr style={{ background: 'var(--bg-secondary)' }}>
                            <th style={{ padding: '10px 16px', textAlign: 'left', fontSize: '12px', fontWeight: '600', width: '35%' }}>Field</th>
                            <th style={{ padding: '10px 16px', textAlign: 'left', fontSize: '12px', fontWeight: '600' }}>Value</th>
                        </tr>
                    </thead>
                    <tbody>
                        {displayData.map(([key, value], idx) => (
                            <tr key={key} style={{ borderTop: idx > 0 ? '1px solid var(--border)' : 'none' }}>
                                <td style={{
                                    padding: '10px 16px', fontSize: '13px',
                                    fontWeight: '500', color: 'var(--text-secondary)',
                                }}>
                                    {formatFieldName(key)}
                                </td>
                                <td style={{ padding: '10px 16px', fontSize: '13px' }}>
                                    {renderValue(value)}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
}


function formatFieldName(key) {
    return key
        .replace(/([A-Z])/g, ' $1')
        .replace(/[_-]/g, ' ')
        .replace(/^./, s => s.toUpperCase())
        .trim();
}

function renderValue(value) {
    if (typeof value === 'boolean') {
        return <span style={{
            padding: '2px 8px', borderRadius: '4px', fontSize: '12px', fontWeight: '600',
            background: value ? '#d1fae5' : '#fee2e2',
            color: value ? '#065f46' : '#991b1b',
        }}>{value ? 'Yes' : 'No'}</span>;
    }
    if (typeof value === 'object') {
        return <code style={{ fontSize: '12px' }}>{JSON.stringify(value, null, 2)}</code>;
    }
    return String(value);
}

function tryFormatJson(str) {
    try { return JSON.stringify(JSON.parse(str), null, 2); } catch { return str; }
}
