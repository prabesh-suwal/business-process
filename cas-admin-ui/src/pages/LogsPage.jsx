import { useState, useEffect } from 'react'
import { auditLogs, apiLogs, activityLogs } from '../api'

// Tab definitions
const TABS = [
    { id: 'audit', label: 'Audit Logs', icon: '🔒', description: 'Compliance & security events' },
    { id: 'api', label: 'API Logs', icon: '🌐', description: 'Technical/system logs' },
    { id: 'activity', label: 'Activity Logs', icon: '📋', description: 'User timeline' }
];

// Audit log categories
const AUDIT_CATEGORIES = [
    'AUTHENTICATION', 'AUTHORIZATION', 'ACCESS', 'DATA_ACCESS',
    'DOCUMENT', 'ORGANIZATION', 'WORKFLOW', 'SECURITY', 'ADMIN', 'SYSTEM'
];

const AUDIT_ACTIONS = [
    'LOGIN', 'LOGOUT', 'TOKEN_REFRESH', 'TOKEN_REVOKED',
    'ACCESS_GRANTED', 'ACCESS_DENIED', 'PERMISSION_CHECK',
    'CREATE', 'READ', 'UPDATE', 'DELETE', 'ARCHIVE',
    'APPROVE', 'REJECT', 'SUBMIT', 'CANCEL',
    'ASSIGN', 'UNASSIGN', 'CLAIM', 'UNCLAIM', 'DELEGATE', 'ESCALATE',
    'ACTIVATE', 'DEACTIVATE', 'SUSPEND', 'RESTORE',
    'EXPORT', 'IMPORT', 'DOWNLOAD', 'UPLOAD',
    'CONFIG_CHANGE', 'SYSTEM_START', 'SYSTEM_STOP', 'ERROR'
];

// HTTP methods for API logs
const HTTP_METHODS = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'];

// Activity types
const ACTIVITY_TYPES = [
    'FORM_SUBMITTED', 'FORM_SAVED', 'FORM_VIEWED',
    'TASK_CLAIMED', 'TASK_COMPLETED', 'TASK_DELEGATED',
    'DOCUMENT_VIEWED', 'DOCUMENT_SIGNED', 'DOCUMENT_SHARED',
    'WORKFLOW_STARTED', 'WORKFLOW_COMPLETED',
    'COMMENT_ADDED', 'APPROVAL_GIVEN', 'REJECTION_GIVEN',
    'LOGIN', 'LOGOUT', 'PAGE_VISITED'
];

export default function LogsPage() {
    const [activeTab, setActiveTab] = useState('audit');
    const [data, setData] = useState({ content: [], totalElements: 0, totalPages: 0 });
    const [page, setPage] = useState(0);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [showFilters, setShowFilters] = useState(false);
    const [selectedLog, setSelectedLog] = useState(null);

    // Audit filters
    const [auditFilters, setAuditFilters] = useState({
        category: '', action: '', productCode: '', actorId: '',
        resourceType: '', correlationId: '', startTime: '', endTime: ''
    });

    // API filters
    const [apiFilters, setApiFilters] = useState({
        httpMethod: '', endpoint: '', serviceName: '', responseStatus: '',
        correlationId: '', userId: '', startTime: '', endTime: ''
    });

    // Activity filters
    const [activityFilters, setActivityFilters] = useState({
        activityType: '', userId: '', entityType: '', entityId: '',
        correlationId: '', startTime: '', endTime: ''
    });

    const loadData = async () => {
        setLoading(true);
        setError(null);
        try {
            let result;
            if (activeTab === 'audit') {
                const filters = buildAuditFilters();
                result = await auditLogs.search(filters, page, 20);
            } else if (activeTab === 'api') {
                const filters = buildApiFilters();
                result = await apiLogs.search(filters, page, 20);
            } else {
                const filters = buildActivityFilters();
                result = await activityLogs.search(filters, page, 20);
            }
            setData(result || { content: [], totalElements: 0, totalPages: 0 });
        } catch (err) {
            console.error(err);
            setError(err.message || 'Failed to load logs');
            setData({ content: [], totalElements: 0, totalPages: 0 });
        } finally {
            setLoading(false);
        }
    };

    const buildAuditFilters = () => {
        const f = {};
        if (auditFilters.category) f.category = auditFilters.category;
        if (auditFilters.action) f.action = auditFilters.action;
        if (auditFilters.productCode) f.productCode = auditFilters.productCode;
        if (auditFilters.actorId) f.actorId = auditFilters.actorId;
        if (auditFilters.resourceType) f.resourceType = auditFilters.resourceType;
        if (auditFilters.correlationId) f.correlationId = auditFilters.correlationId;
        if (auditFilters.startTime) f.startTime = new Date(auditFilters.startTime).toISOString();
        if (auditFilters.endTime) f.endTime = new Date(auditFilters.endTime).toISOString();
        return f;
    };

    const buildApiFilters = () => {
        const f = {};
        if (apiFilters.httpMethod) f.httpMethod = apiFilters.httpMethod;
        if (apiFilters.endpoint) f.endpoint = apiFilters.endpoint;
        if (apiFilters.serviceName) f.serviceName = apiFilters.serviceName;
        if (apiFilters.responseStatus) f.responseStatus = parseInt(apiFilters.responseStatus);
        if (apiFilters.correlationId) f.correlationId = apiFilters.correlationId;
        if (apiFilters.userId) f.authenticatedUserId = apiFilters.userId;
        if (apiFilters.startTime) f.startTime = new Date(apiFilters.startTime).toISOString();
        if (apiFilters.endTime) f.endTime = new Date(apiFilters.endTime).toISOString();
        return f;
    };

    const buildActivityFilters = () => {
        const f = {};
        if (activityFilters.activityType) f.activityType = activityFilters.activityType;
        if (activityFilters.userId) f.performedByUserId = activityFilters.userId;
        if (activityFilters.entityType) f.entityType = activityFilters.entityType;
        if (activityFilters.entityId) f.entityId = activityFilters.entityId;
        if (activityFilters.correlationId) f.correlationId = activityFilters.correlationId;
        if (activityFilters.startTime) f.startTime = new Date(activityFilters.startTime).toISOString();
        if (activityFilters.endTime) f.endTime = new Date(activityFilters.endTime).toISOString();
        return f;
    };

    useEffect(() => { loadData(); }, [page, activeTab]);

    const handleTabChange = (tabId) => {
        setActiveTab(tabId);
        setPage(0);
        setSelectedLog(null);
    };

    const clearFilters = () => {
        if (activeTab === 'audit') {
            setAuditFilters({ category: '', action: '', productCode: '', actorId: '', resourceType: '', correlationId: '', startTime: '', endTime: '' });
        } else if (activeTab === 'api') {
            setApiFilters({ httpMethod: '', endpoint: '', serviceName: '', responseStatus: '', correlationId: '', userId: '', startTime: '', endTime: '' });
        } else {
            setActivityFilters({ activityType: '', userId: '', entityType: '', entityId: '', correlationId: '', startTime: '', endTime: '' });
        }
        setPage(0);
        setTimeout(loadData, 0);
    };

    const formatTimestamp = (ts) => {
        if (!ts) return '-';
        return new Date(ts).toLocaleString(undefined, {
            month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit', second: '2-digit'
        });
    };

    const getStatusBadge = (status) => {
        if (!status) return 'badge-neutral';
        if (status >= 200 && status < 300) return 'badge-success';
        if (status >= 400 && status < 500) return 'badge-warning';
        if (status >= 500) return 'badge-error';
        return 'badge-neutral';
    };

    const getActionBadge = (action) => {
        if (!action) return 'badge-neutral';
        if (action.includes('DENIED') || action.includes('ERROR') || action.includes('REJECT')) return 'badge-error';
        if (action.includes('GRANTED') || action.includes('CREATE') || action.includes('APPROVE') || action.includes('LOGIN')) return 'badge-success';
        if (action.includes('DELETE') || action.includes('REVOKE') || action.includes('SUSPEND')) return 'badge-warning';
        return 'badge-neutral';
    };

    const exportToJSON = () => {
        if (!data.content?.length) return;
        const json = JSON.stringify(data.content, null, 2);
        const blob = new Blob([json], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `${activeTab}-logs-${new Date().toISOString().split('T')[0]}.json`;
        link.click();
        URL.revokeObjectURL(url);
    };

    // Render functions for each tab
    const renderAuditTable = () => (
        <table className="table">
            <thead>
                <tr>
                    <th>Timestamp</th>
                    <th>Category</th>
                    <th>Action</th>
                    <th>Actor</th>
                    <th>Resource</th>
                    <th>Description</th>
                    <th></th>
                </tr>
            </thead>
            <tbody>
                {data.content?.map(log => (
                    <tr key={log.id} onClick={() => setSelectedLog(log)} style={{ cursor: 'pointer' }}>
                        <td style={{ whiteSpace: 'nowrap' }}>{formatTimestamp(log.timestamp)}</td>
                        <td><span className="badge badge-info" style={{ fontSize: '0.7rem' }}>{log.category}</span></td>
                        <td><span className={`badge ${getActionBadge(log.action)}`}>{log.action}</span></td>
                        <td>{log.actorName || log.actorId || '-'}</td>
                        <td>{log.resourceType ? `${log.resourceType}/${log.resourceId || ''}` : '-'}</td>
                        <td style={{ maxWidth: '300px', overflow: 'hidden', textOverflow: 'ellipsis' }}>{log.description || '-'}</td>
                        <td><button className="btn btn-xs btn-ghost" onClick={(e) => { e.stopPropagation(); setSelectedLog(log); }}>👁️</button></td>
                    </tr>
                ))}
            </tbody>
        </table>
    );

    const renderApiTable = () => (
        <table className="table">
            <thead>
                <tr>
                    <th>Timestamp</th>
                    <th>Method</th>
                    <th>Endpoint</th>
                    <th>Status</th>
                    <th>Duration</th>
                    <th>Service</th>
                    <th>User</th>
                    <th></th>
                </tr>
            </thead>
            <tbody>
                {data.content?.map(log => (
                    <tr key={log.logId || log.id} onClick={() => setSelectedLog(log)} style={{ cursor: 'pointer' }}>
                        <td style={{ whiteSpace: 'nowrap' }}>{formatTimestamp(log.timestamp)}</td>
                        <td><span className={`badge ${log.httpMethod === 'GET' ? 'badge-info' : log.httpMethod === 'POST' ? 'badge-success' : log.httpMethod === 'DELETE' ? 'badge-error' : 'badge-warning'}`}>{log.httpMethod}</span></td>
                        <td style={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>{log.endpoint || log.fullPath}</td>
                        <td><span className={`badge ${getStatusBadge(log.responseStatus)}`}>{log.responseStatus}</span></td>
                        <td>{log.responseTimeMs ? `${log.responseTimeMs}ms` : '-'}</td>
                        <td>{log.serviceName || '-'}</td>
                        <td>{log.authenticatedUserId || '-'}</td>
                        <td><button className="btn btn-xs btn-ghost" onClick={(e) => { e.stopPropagation(); setSelectedLog(log); }}>👁️</button></td>
                    </tr>
                ))}
            </tbody>
        </table>
    );

    const renderActivityTable = () => (
        <table className="table">
            <thead>
                <tr>
                    <th>Timestamp</th>
                    <th>Type</th>
                    <th>User</th>
                    <th>Entity</th>
                    <th>Description</th>
                    <th></th>
                </tr>
            </thead>
            <tbody>
                {data.content?.map(log => (
                    <tr key={log.logId || log.id} onClick={() => setSelectedLog(log)} style={{ cursor: 'pointer' }}>
                        <td style={{ whiteSpace: 'nowrap' }}>{formatTimestamp(log.timestamp)}</td>
                        <td><span className="badge badge-primary">{log.activityType}</span></td>
                        <td>{log.performedByUsername || log.performedByUserId || '-'}</td>
                        <td>{log.entityType ? `${log.entityType}/${log.entityId || ''}` : '-'}</td>
                        <td style={{ maxWidth: '300px', overflow: 'hidden', textOverflow: 'ellipsis' }}>{log.description || '-'}</td>
                        <td><button className="btn btn-xs btn-ghost" onClick={(e) => { e.stopPropagation(); setSelectedLog(log); }}>👁️</button></td>
                    </tr>
                ))}
            </tbody>
        </table>
    );

    const renderFilters = () => {
        if (activeTab === 'audit') {
            return (
                <div className="filter-grid">
                    <div className="form-group">
                        <label>Category</label>
                        <select value={auditFilters.category} onChange={e => setAuditFilters(p => ({ ...p, category: e.target.value }))}>
                            <option value="">All Categories</option>
                            {AUDIT_CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
                        </select>
                    </div>
                    <div className="form-group">
                        <label>Action</label>
                        <select value={auditFilters.action} onChange={e => setAuditFilters(p => ({ ...p, action: e.target.value }))}>
                            <option value="">All Actions</option>
                            {AUDIT_ACTIONS.map(a => <option key={a} value={a}>{a}</option>)}
                        </select>
                    </div>
                    <div className="form-group">
                        <label>Actor ID</label>
                        <input type="text" value={auditFilters.actorId} onChange={e => setAuditFilters(p => ({ ...p, actorId: e.target.value }))} placeholder="User ID..." />
                    </div>
                    <div className="form-group">
                        <label>Correlation ID</label>
                        <input type="text" value={auditFilters.correlationId} onChange={e => setAuditFilters(p => ({ ...p, correlationId: e.target.value }))} placeholder="Correlation ID..." />
                    </div>
                    <div className="form-group">
                        <label>Start Time</label>
                        <input type="datetime-local" value={auditFilters.startTime} onChange={e => setAuditFilters(p => ({ ...p, startTime: e.target.value }))} />
                    </div>
                    <div className="form-group">
                        <label>End Time</label>
                        <input type="datetime-local" value={auditFilters.endTime} onChange={e => setAuditFilters(p => ({ ...p, endTime: e.target.value }))} />
                    </div>
                </div>
            );
        } else if (activeTab === 'api') {
            return (
                <div className="filter-grid">
                    <div className="form-group">
                        <label>HTTP Method</label>
                        <select value={apiFilters.httpMethod} onChange={e => setApiFilters(p => ({ ...p, httpMethod: e.target.value }))}>
                            <option value="">All Methods</option>
                            {HTTP_METHODS.map(m => <option key={m} value={m}>{m}</option>)}
                        </select>
                    </div>
                    <div className="form-group">
                        <label>Endpoint</label>
                        <input type="text" value={apiFilters.endpoint} onChange={e => setApiFilters(p => ({ ...p, endpoint: e.target.value }))} placeholder="/api/..." />
                    </div>
                    <div className="form-group">
                        <label>Status Code</label>
                        <input type="number" value={apiFilters.responseStatus} onChange={e => setApiFilters(p => ({ ...p, responseStatus: e.target.value }))} placeholder="200, 404..." />
                    </div>
                    <div className="form-group">
                        <label>Service</label>
                        <input type="text" value={apiFilters.serviceName} onChange={e => setApiFilters(p => ({ ...p, serviceName: e.target.value }))} placeholder="Service name..." />
                    </div>
                    <div className="form-group">
                        <label>Start Time</label>
                        <input type="datetime-local" value={apiFilters.startTime} onChange={e => setApiFilters(p => ({ ...p, startTime: e.target.value }))} />
                    </div>
                    <div className="form-group">
                        <label>End Time</label>
                        <input type="datetime-local" value={apiFilters.endTime} onChange={e => setApiFilters(p => ({ ...p, endTime: e.target.value }))} />
                    </div>
                </div>
            );
        } else {
            return (
                <div className="filter-grid">
                    <div className="form-group">
                        <label>Activity Type</label>
                        <select value={activityFilters.activityType} onChange={e => setActivityFilters(p => ({ ...p, activityType: e.target.value }))}>
                            <option value="">All Types</option>
                            {ACTIVITY_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
                        </select>
                    </div>
                    <div className="form-group">
                        <label>User ID</label>
                        <input type="text" value={activityFilters.userId} onChange={e => setActivityFilters(p => ({ ...p, userId: e.target.value }))} placeholder="User ID..." />
                    </div>
                    <div className="form-group">
                        <label>Entity Type</label>
                        <input type="text" value={activityFilters.entityType} onChange={e => setActivityFilters(p => ({ ...p, entityType: e.target.value }))} placeholder="MEMO, TASK..." />
                    </div>
                    <div className="form-group">
                        <label>Entity ID</label>
                        <input type="text" value={activityFilters.entityId} onChange={e => setActivityFilters(p => ({ ...p, entityId: e.target.value }))} placeholder="Entity ID..." />
                    </div>
                    <div className="form-group">
                        <label>Start Time</label>
                        <input type="datetime-local" value={activityFilters.startTime} onChange={e => setActivityFilters(p => ({ ...p, startTime: e.target.value }))} />
                    </div>
                    <div className="form-group">
                        <label>End Time</label>
                        <input type="datetime-local" value={activityFilters.endTime} onChange={e => setActivityFilters(p => ({ ...p, endTime: e.target.value }))} />
                    </div>
                </div>
            );
        }
    };

    return (
        <div>
            <div className="page-header">
                <h1>System Logs</h1>
                <div className="page-actions">
                    <button className={`btn btn-secondary ${showFilters ? 'btn-active' : ''}`} onClick={() => setShowFilters(!showFilters)}>
                        🔍 Filters
                    </button>
                    <button className="btn btn-secondary" onClick={exportToJSON} disabled={!data.content?.length}>
                        📥 Export
                    </button>
                    <button className="btn btn-secondary" onClick={loadData}>↻ Refresh</button>
                </div>
            </div>

            {/* Tabs */}
            <div className="tabs-container" style={{ marginBottom: '1rem' }}>
                <div className="tabs" style={{ display: 'flex', gap: '0.5rem', borderBottom: '2px solid var(--border-color)', paddingBottom: '0' }}>
                    {TABS.map(tab => (
                        <button
                            key={tab.id}
                            className={`tab-btn ${activeTab === tab.id ? 'active' : ''}`}
                            onClick={() => handleTabChange(tab.id)}
                            style={{
                                padding: '0.75rem 1.25rem',
                                border: 'none',
                                background: activeTab === tab.id ? 'var(--primary-color)' : 'transparent',
                                color: activeTab === tab.id ? 'white' : 'var(--text-color)',
                                borderRadius: '0.5rem 0.5rem 0 0',
                                cursor: 'pointer',
                                fontWeight: activeTab === tab.id ? '600' : '400',
                                transition: 'all 0.2s ease'
                            }}
                        >
                            <span style={{ marginRight: '0.5rem' }}>{tab.icon}</span>
                            {tab.label}
                        </button>
                    ))}
                </div>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginTop: '0.5rem' }}>
                    {TABS.find(t => t.id === activeTab)?.description}
                </p>
            </div>

            {/* Filters */}
            {showFilters && (
                <div className="filters-panel" style={{ background: 'var(--bg-secondary)', padding: '1rem', borderRadius: '0.5rem', marginBottom: '1rem' }}>
                    {renderFilters()}
                    <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem' }}>
                        <button className="btn btn-primary btn-sm" onClick={() => { setPage(0); loadData(); }}>Apply Filters</button>
                        <button className="btn btn-secondary btn-sm" onClick={clearFilters}>Clear</button>
                    </div>
                </div>
            )}

            {/* Error */}
            {error && <div className="alert alert-error" style={{ marginBottom: '1rem' }}>{error}</div>}

            {/* Table */}
            <div className="card" style={{ overflow: 'auto' }}>
                {loading ? (
                    <div style={{ padding: '2rem', textAlign: 'center' }}>Loading...</div>
                ) : data.content?.length === 0 ? (
                    <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-secondary)' }}>No logs found</div>
                ) : (
                    activeTab === 'audit' ? renderAuditTable() :
                        activeTab === 'api' ? renderApiTable() :
                            renderActivityTable()
                )}
            </div>

            {/* Pagination */}
            {data.totalPages > 1 && (
                <div className="pagination" style={{ marginTop: '1rem', display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '0.5rem' }}>
                    <button className="btn btn-sm" onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}>← Prev</button>
                    <span style={{ padding: '0 1rem' }}>Page {page + 1} of {data.totalPages} ({data.totalElements} total)</span>
                    <button className="btn btn-sm" onClick={() => setPage(p => Math.min(data.totalPages - 1, p + 1))} disabled={page >= data.totalPages - 1}>Next →</button>
                </div>
            )}

            {/* Detail Modal */}
            {selectedLog && (
                <div className="modal-overlay" onClick={() => setSelectedLog(null)} style={{
                    position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
                    background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000
                }}>
                    <div className="modal-content" onClick={e => e.stopPropagation()} style={{
                        background: 'var(--bg-primary)', borderRadius: '0.5rem', padding: '1.5rem',
                        maxWidth: '800px', maxHeight: '80vh', overflow: 'auto', width: '90%'
                    }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                            <h3>Log Details</h3>
                            <button className="btn btn-ghost" onClick={() => setSelectedLog(null)}>✕</button>
                        </div>
                        <pre style={{
                            background: 'var(--bg-secondary)', padding: '1rem', borderRadius: '0.25rem',
                            overflow: 'auto', fontSize: '0.85rem', lineHeight: '1.5'
                        }}>
                            {JSON.stringify(selectedLog, null, 2)}
                        </pre>
                    </div>
                </div>
            )}

            <style>{`
                .filter-grid {
                    display: grid;
                    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
                    gap: 1rem;
                }
                .form-group label {
                    display: block;
                    margin-bottom: 0.25rem;
                    font-size: 0.875rem;
                    color: var(--text-secondary);
                }
                .form-group input, .form-group select {
                    width: 100%;
                    padding: 0.5rem;
                    border: 1px solid var(--border-color);
                    border-radius: 0.25rem;
                    background: var(--bg-primary);
                    color: var(--text-color);
                }
            `}</style>
        </div>
    );
}
