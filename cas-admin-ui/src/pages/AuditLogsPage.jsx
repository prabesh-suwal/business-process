import { useState, useEffect } from 'react'
import { auditLogs } from '../api'

// Action and Category enums (matching backend)
const CATEGORIES = [
    'AUTHENTICATION', 'AUTHORIZATION', 'ACCESS', 'DATA_ACCESS',
    'DOCUMENT', 'ORGANIZATION', 'WORKFLOW', 'SECURITY', 'ADMIN', 'SYSTEM'
];

const ACTIONS = [
    'LOGIN', 'LOGOUT', 'TOKEN_REFRESH', 'TOKEN_REVOKED',
    'ACCESS_GRANTED', 'ACCESS_DENIED', 'PERMISSION_CHECK',
    'CREATE', 'READ', 'UPDATE', 'DELETE', 'ARCHIVE',
    'APPROVE', 'REJECT', 'SUBMIT', 'CANCEL',
    'ASSIGN', 'UNASSIGN', 'CLAIM', 'UNCLAIM', 'DELEGATE', 'ESCALATE',
    'ACTIVATE', 'DEACTIVATE', 'SUSPEND', 'RESTORE',
    'EXPORT', 'IMPORT', 'DOWNLOAD', 'UPLOAD',
    'CONFIG_CHANGE', 'SYSTEM_START', 'SYSTEM_STOP', 'ERROR'
];

export default function AuditLogsPage() {
    const [data, setData] = useState({ content: [], totalElements: 0, totalPages: 0 });
    const [page, setPage] = useState(0);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    // Filters
    const [filters, setFilters] = useState({
        category: '',
        action: '',
        productCode: '',
        actorId: '',
        resourceType: '',
        correlationId: '',
        startTime: '',
        endTime: ''
    });

    const [showFilters, setShowFilters] = useState(false);
    const [selectedLog, setSelectedLog] = useState(null);

    const loadLogs = async () => {
        setLoading(true);
        setError(null);
        try {
            // Build search request
            const searchRequest = {};
            if (filters.category) searchRequest.category = filters.category;
            if (filters.action) searchRequest.action = filters.action;
            if (filters.productCode) searchRequest.productCode = filters.productCode;
            if (filters.actorId) searchRequest.actorId = filters.actorId;
            if (filters.resourceType) searchRequest.resourceType = filters.resourceType;
            if (filters.correlationId) searchRequest.correlationId = filters.correlationId;
            if (filters.startTime) searchRequest.startTime = new Date(filters.startTime).toISOString();
            if (filters.endTime) searchRequest.endTime = new Date(filters.endTime).toISOString();

            const result = await auditLogs.search(searchRequest, page, 20);
            setData(result);
        } catch (err) {
            console.error(err);
            setError(err.message || 'Failed to load audit logs');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { loadLogs(); }, [page]);

    const handleFilterChange = (field, value) => {
        setFilters(prev => ({ ...prev, [field]: value }));
    };

    const applyFilters = () => {
        setPage(0);
        loadLogs();
    };

    const clearFilters = () => {
        setFilters({
            category: '',
            action: '',
            productCode: '',
            actorId: '',
            resourceType: '',
            correlationId: '',
            startTime: '',
            endTime: ''
        });
        setPage(0);
        setTimeout(loadLogs, 0);
    };

    const getActionBadge = (action) => {
        if (!action) return 'badge-neutral';
        if (action.includes('DENIED') || action.includes('ERROR') || action.includes('REJECT'))
            return 'badge-error';
        if (action.includes('GRANTED') || action.includes('CREATE') || action.includes('APPROVE') || action.includes('LOGIN'))
            return 'badge-success';
        if (action.includes('DELETE') || action.includes('REVOKE') || action.includes('SUSPEND'))
            return 'badge-warning';
        return 'badge-neutral';
    };

    const getCategoryColor = (category) => {
        const colors = {
            'AUTHENTICATION': '#6366f1',
            'AUTHORIZATION': '#8b5cf6',
            'ACCESS': '#3b82f6',
            'DATA_ACCESS': '#10b981',
            'DOCUMENT': '#f59e0b',
            'ORGANIZATION': '#ec4899',
            'WORKFLOW': '#14b8a6',
            'SECURITY': '#ef4444',
            'ADMIN': '#6b7280',
            'SYSTEM': '#71717a'
        };
        return colors[category] || '#6b7280';
    };

    const formatTimestamp = (timestamp) => {
        if (!timestamp) return '-';
        const date = new Date(timestamp);
        return date.toLocaleString(undefined, {
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });
    };

    const hasActiveFilters = Object.values(filters).some(v => v !== '');

    const exportToCSV = () => {
        if (!data.content || data.content.length === 0) return;

        const headers = [
            'ID', 'Timestamp', 'Category', 'Action', 'Result',
            'Actor ID', 'Actor Name', 'Actor Email', 'Actor Type', 'IP Address',
            'Resource Type', 'Resource ID', 'Description',
            'Service', 'Product Code', 'Correlation ID'
        ];

        const rows = data.content.map(log => [
            log.id,
            log.timestamp,
            log.category,
            log.action,
            log.result || '',
            log.actorId || '',
            log.actorName || '',
            log.actorEmail || '',
            log.actorType || '',
            log.ipAddress || '',
            log.resourceType || '',
            log.resourceId || '',
            (log.description || '').replace(/"/g, '""'),
            log.serviceName || '',
            log.productCode || '',
            log.correlationId || ''
        ]);

        const csvContent = [
            headers.join(','),
            ...rows.map(row => row.map(cell => `"${cell}"`).join(','))
        ].join('\n');

        const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `audit-logs-${new Date().toISOString().split('T')[0]}.csv`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
    };

    const exportToJSON = () => {
        if (!data.content || data.content.length === 0) return;

        const jsonContent = JSON.stringify(data.content, null, 2);
        const blob = new Blob([jsonContent], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `audit-logs-${new Date().toISOString().split('T')[0]}.json`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
    };

    return (
        <div>
            <div className="page-header">
                <h1>Audit Logs</h1>
                <div className="page-actions">
                    <button
                        className={`btn btn-secondary ${showFilters ? 'btn-active' : ''}`}
                        onClick={() => setShowFilters(!showFilters)}
                    >
                        🔍 Filters {hasActiveFilters && <span className="badge badge-info" style={{ marginLeft: '4px' }}>Active</span>}
                    </button>
                    <div className="dropdown" style={{ position: 'relative', display: 'inline-block' }}>
                        <button className="btn btn-secondary dropdown-trigger" onClick={(e) => {
                            const menu = e.currentTarget.nextElementSibling;
                            menu.style.display = menu.style.display === 'block' ? 'none' : 'block';
                        }}>
                            📥 Export ▼
                        </button>
                        <div className="dropdown-menu" style={{
                            display: 'none',
                            position: 'absolute',
                            right: 0,
                            top: '100%',
                            background: '#1f2937',
                            border: '1px solid #374151',
                            borderRadius: '4px',
                            zIndex: 1000,
                            minWidth: '120px'
                        }}>
                            <button
                                className="dropdown-item"
                                style={{ display: 'block', width: '100%', padding: '8px 12px', textAlign: 'left', background: 'none', border: 'none', color: 'white', cursor: 'pointer' }}
                                onClick={exportToCSV}
                            >
                                Export CSV
                            </button>
                            <button
                                className="dropdown-item"
                                style={{ display: 'block', width: '100%', padding: '8px 12px', textAlign: 'left', background: 'none', border: 'none', color: 'white', cursor: 'pointer' }}
                                onClick={exportToJSON}
                            >
                                Export JSON
                            </button>
                        </div>
                    </div>
                    <button className="btn btn-secondary" onClick={loadLogs}>
                        ↻ Refresh
                    </button>
                </div>
            </div>

            {/* Filters Panel */}
            {showFilters && (
                <div className="card" style={{ marginBottom: '1rem' }}>
                    <h3 style={{ marginBottom: '1rem' }}>Filters</h3>
                    <div className="grid" style={{
                        display: 'grid',
                        gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))',
                        gap: '1rem',
                        marginBottom: '1rem'
                    }}>
                        <div className="form-group">
                            <label>Category</label>
                            <select
                                className="form-control"
                                value={filters.category}
                                onChange={(e) => handleFilterChange('category', e.target.value)}
                            >
                                <option value="">All Categories</option>
                                {CATEGORIES.map(cat => (
                                    <option key={cat} value={cat}>{cat}</option>
                                ))}
                            </select>
                        </div>
                        <div className="form-group">
                            <label>Action</label>
                            <select
                                className="form-control"
                                value={filters.action}
                                onChange={(e) => handleFilterChange('action', e.target.value)}
                            >
                                <option value="">All Actions</option>
                                {ACTIONS.map(action => (
                                    <option key={action} value={action}>{action}</option>
                                ))}
                            </select>
                        </div>
                        <div className="form-group">
                            <label>Product Code</label>
                            <input
                                type="text"
                                className="form-control"
                                placeholder="e.g. WFM, LMS"
                                value={filters.productCode}
                                onChange={(e) => handleFilterChange('productCode', e.target.value)}
                            />
                        </div>
                        <div className="form-group">
                            <label>Actor ID</label>
                            <input
                                type="text"
                                className="form-control"
                                placeholder="User ID"
                                value={filters.actorId}
                                onChange={(e) => handleFilterChange('actorId', e.target.value)}
                            />
                        </div>
                        <div className="form-group">
                            <label>Resource Type</label>
                            <input
                                type="text"
                                className="form-control"
                                placeholder="e.g. User, Memo"
                                value={filters.resourceType}
                                onChange={(e) => handleFilterChange('resourceType', e.target.value)}
                            />
                        </div>
                        <div className="form-group">
                            <label>Correlation ID</label>
                            <input
                                type="text"
                                className="form-control"
                                placeholder="Trace ID"
                                value={filters.correlationId}
                                onChange={(e) => handleFilterChange('correlationId', e.target.value)}
                            />
                        </div>
                        <div className="form-group">
                            <label>Start Time</label>
                            <input
                                type="datetime-local"
                                className="form-control"
                                value={filters.startTime}
                                onChange={(e) => handleFilterChange('startTime', e.target.value)}
                            />
                        </div>
                        <div className="form-group">
                            <label>End Time</label>
                            <input
                                type="datetime-local"
                                className="form-control"
                                value={filters.endTime}
                                onChange={(e) => handleFilterChange('endTime', e.target.value)}
                            />
                        </div>
                    </div>
                    <div style={{ display: 'flex', gap: '0.5rem' }}>
                        <button className="btn btn-primary" onClick={applyFilters}>Apply Filters</button>
                        <button className="btn btn-secondary" onClick={clearFilters}>Clear All</button>
                    </div>
                </div>
            )}

            {error && (
                <div className="alert alert-error" style={{ marginBottom: '1rem' }}>
                    {error}
                </div>
            )}

            <div className="card">
                {loading ? (
                    <div className="loading">Loading audit logs...</div>
                ) : (
                    <>
                        <div className="table-container">
                            <table>
                                <thead>
                                    <tr>
                                        <th style={{ width: '130px' }}>Time</th>
                                        <th style={{ width: '100px' }}>Category</th>
                                        <th style={{ width: '120px' }}>Action</th>
                                        <th>Actor</th>
                                        <th>Resource</th>
                                        <th style={{ width: '80px' }}>Service</th>
                                        <th style={{ width: '50px' }}></th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {data.content && data.content.length > 0 ? (
                                        data.content.map((log) => (
                                            <tr key={log.id}>
                                                <td className="text-muted" style={{ whiteSpace: 'nowrap', fontSize: '0.85em' }}>
                                                    {formatTimestamp(log.timestamp)}
                                                </td>
                                                <td>
                                                    <span
                                                        style={{
                                                            background: getCategoryColor(log.category),
                                                            color: 'white',
                                                            padding: '2px 8px',
                                                            borderRadius: '4px',
                                                            fontSize: '0.75em',
                                                            fontWeight: '500'
                                                        }}
                                                    >
                                                        {log.category}
                                                    </span>
                                                </td>
                                                <td>
                                                    <span className={`badge ${getActionBadge(log.action)}`}>
                                                        {log.action}
                                                    </span>
                                                </td>
                                                <td>
                                                    <div style={{ fontSize: '0.9em' }}>
                                                        {log.actorName || log.actorEmail || (
                                                            <span className="text-muted">
                                                                {log.actorType}: {log.actorId?.slice(0, 8)}
                                                            </span>
                                                        )}
                                                    </div>
                                                    {log.ipAddress && (
                                                        <div className="text-muted" style={{ fontSize: '0.75em' }}>
                                                            {log.ipAddress}
                                                        </div>
                                                    )}
                                                </td>
                                                <td>
                                                    <div style={{ fontSize: '0.9em' }}>
                                                        <span className="text-muted">{log.resourceType}: </span>
                                                        {log.resourceId?.length > 20
                                                            ? log.resourceId.slice(0, 20) + '...'
                                                            : log.resourceId}
                                                    </div>
                                                    {log.description && (
                                                        <div className="text-muted" style={{ fontSize: '0.75em', maxWidth: '300px' }}>
                                                            {log.description.length > 60
                                                                ? log.description.slice(0, 60) + '...'
                                                                : log.description}
                                                        </div>
                                                    )}
                                                </td>
                                                <td>
                                                    <span className="text-muted" style={{ fontSize: '0.85em' }}>
                                                        {log.serviceName || log.productCode || '-'}
                                                    </span>
                                                </td>
                                                <td>
                                                    <button
                                                        className="btn btn-sm btn-ghost"
                                                        onClick={() => setSelectedLog(log)}
                                                        title="View Details"
                                                    >
                                                        👁
                                                    </button>
                                                </td>
                                            </tr>
                                        ))
                                    ) : (
                                        <tr>
                                            <td colSpan="7" style={{ textAlign: 'center', padding: '2rem' }}>
                                                No audit logs found
                                            </td>
                                        </tr>
                                    )}
                                </tbody>
                            </table>
                        </div>

                        <div className="pagination">
                            <span>
                                Showing {data.content?.length || 0} of {data.totalElements || 0} entries
                                (Page {page + 1} of {data.totalPages || 1})
                            </span>
                            <div className="pagination-buttons">
                                <button
                                    className="btn btn-sm btn-secondary"
                                    disabled={page === 0}
                                    onClick={() => setPage(p => p - 1)}
                                >
                                    Previous
                                </button>
                                <button
                                    className="btn btn-sm btn-secondary"
                                    disabled={data.last || page >= (data.totalPages - 1)}
                                    onClick={() => setPage(p => p + 1)}
                                >
                                    Next
                                </button>
                            </div>
                        </div>
                    </>
                )}
            </div>

            {/* Detail Modal */}
            {selectedLog && (
                <div className="modal-overlay" onClick={() => setSelectedLog(null)}>
                    <div className="modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '700px' }}>
                        <div className="modal-header">
                            <h2>Audit Log Details</h2>
                            <button className="btn btn-ghost" onClick={() => setSelectedLog(null)}>✕</button>
                        </div>
                        <div className="modal-body">
                            <table className="detail-table">
                                <tbody>
                                    <tr>
                                        <th>ID</th>
                                        <td><code>{selectedLog.id}</code></td>
                                    </tr>
                                    <tr>
                                        <th>Sequence #</th>
                                        <td>{selectedLog.sequenceNumber}</td>
                                    </tr>
                                    <tr>
                                        <th>Timestamp</th>
                                        <td>{new Date(selectedLog.timestamp).toLocaleString()}</td>
                                    </tr>
                                    <tr>
                                        <th>Category</th>
                                        <td>
                                            <span style={{
                                                background: getCategoryColor(selectedLog.category),
                                                color: 'white',
                                                padding: '2px 8px',
                                                borderRadius: '4px'
                                            }}>
                                                {selectedLog.category}
                                            </span>
                                        </td>
                                    </tr>
                                    <tr>
                                        <th>Action</th>
                                        <td>
                                            <span className={`badge ${getActionBadge(selectedLog.action)}`}>
                                                {selectedLog.action}
                                            </span>
                                        </td>
                                    </tr>
                                    <tr>
                                        <th>Result</th>
                                        <td>{selectedLog.result || '-'}</td>
                                    </tr>
                                    <tr><td colSpan="2"><hr style={{ opacity: 0.3 }} /></td></tr>
                                    <tr>
                                        <th>Actor ID</th>
                                        <td><code>{selectedLog.actorId || '-'}</code></td>
                                    </tr>
                                    <tr>
                                        <th>Actor Name</th>
                                        <td>{selectedLog.actorName || '-'}</td>
                                    </tr>
                                    <tr>
                                        <th>Actor Email</th>
                                        <td>{selectedLog.actorEmail || '-'}</td>
                                    </tr>
                                    <tr>
                                        <th>Actor Type</th>
                                        <td>{selectedLog.actorType || '-'}</td>
                                    </tr>
                                    <tr>
                                        <th>Actor Roles</th>
                                        <td>{selectedLog.actorRoles?.join(', ') || '-'}</td>
                                    </tr>
                                    <tr>
                                        <th>IP Address</th>
                                        <td>{selectedLog.ipAddress || '-'}</td>
                                    </tr>
                                    <tr><td colSpan="2"><hr style={{ opacity: 0.3 }} /></td></tr>
                                    <tr>
                                        <th>Resource Type</th>
                                        <td>{selectedLog.resourceType || '-'}</td>
                                    </tr>
                                    <tr>
                                        <th>Resource ID</th>
                                        <td><code>{selectedLog.resourceId || '-'}</code></td>
                                    </tr>
                                    <tr>
                                        <th>Description</th>
                                        <td>{selectedLog.description || '-'}</td>
                                    </tr>
                                    {selectedLog.errorMessage && (
                                        <tr>
                                            <th>Error</th>
                                            <td style={{ color: '#ef4444' }}>{selectedLog.errorMessage}</td>
                                        </tr>
                                    )}
                                    <tr><td colSpan="2"><hr style={{ opacity: 0.3 }} /></td></tr>
                                    <tr>
                                        <th>Service</th>
                                        <td>{selectedLog.serviceName || '-'}</td>
                                    </tr>
                                    <tr>
                                        <th>Product Code</th>
                                        <td>{selectedLog.productCode || '-'}</td>
                                    </tr>
                                    <tr>
                                        <th>Correlation ID</th>
                                        <td><code style={{ fontSize: '0.85em' }}>{selectedLog.correlationId || '-'}</code></td>
                                    </tr>
                                    <tr><td colSpan="2"><hr style={{ opacity: 0.3 }} /></td></tr>
                                    <tr>
                                        <th>Record Hash</th>
                                        <td>
                                            <code style={{ fontSize: '0.75em', wordBreak: 'break-all' }}>
                                                {selectedLog.recordHash || '-'}
                                            </code>
                                        </td>
                                    </tr>
                                    <tr>
                                        <th>Integrity</th>
                                        <td>
                                            {selectedLog.integrityVerified
                                                ? <span style={{ color: '#10b981' }}>✓ Verified</span>
                                                : <span className="text-muted">Not verified</span>
                                            }
                                        </td>
                                    </tr>
                                    {selectedLog.metadata && (
                                        <tr>
                                            <th>Metadata</th>
                                            <td>
                                                <pre style={{
                                                    background: '#1a1a2e',
                                                    padding: '0.5rem',
                                                    borderRadius: '4px',
                                                    fontSize: '0.8em',
                                                    overflow: 'auto',
                                                    maxHeight: '150px'
                                                }}>
                                                    {typeof selectedLog.metadata === 'string'
                                                        ? selectedLog.metadata
                                                        : JSON.stringify(selectedLog.metadata, null, 2)}
                                                </pre>
                                            </td>
                                        </tr>
                                    )}
                                </tbody>
                            </table>
                        </div>
                        <div className="modal-footer">
                            <button className="btn btn-secondary" onClick={() => setSelectedLog(null)}>
                                Close
                            </button>
                        </div>
                    </div>
                </div>
            )}

            <style>{`
                .detail-table {
                    width: 100%;
                    border-collapse: collapse;
                }
                .detail-table th {
                    width: 130px;
                    text-align: left;
                    padding: 0.5rem;
                    vertical-align: top;
                    font-weight: 500;
                    color: #9ca3af;
                }
                .detail-table td {
                    padding: 0.5rem;
                }
                .detail-table code {
                    background: #374151;
                    padding: 2px 6px;
                    border-radius: 4px;
                    font-size: 0.9em;
                }
            `}</style>
        </div>
    );
}
