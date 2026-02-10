import { useState } from 'react'
import './LogDetailDrawer.css'

/**
 * Enterprise-level log detail drawer with proper formatting
 */
export default function LogDetailDrawer({ log, logType, onClose }) {
    const [activeTab, setActiveTab] = useState('overview');

    if (!log) return null;

    const formatValue = (value) => {
        if (value === null || value === undefined) return <span className="log-value-null">—</span>;
        if (typeof value === 'boolean') return <span className={`log-value-bool ${value ? 'true' : 'false'}`}>{value ? 'Yes' : 'No'}</span>;
        if (typeof value === 'object') return <pre className="log-value-json">{JSON.stringify(value, null, 2)}</pre>;
        return value;
    };

    const formatTimestamp = (ts) => {
        if (!ts) return '—';
        const date = new Date(ts);
        return (
            <div className="log-timestamp">
                <span className="log-timestamp-date">{date.toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' })}</span>
                <span className="log-timestamp-time">{date.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit', second: '2-digit', fractionalSecondDigits: 3 })}</span>
            </div>
        );
    };

    const getStatusColor = (status) => {
        if (!status) return 'neutral';
        if (status >= 200 && status < 300) return 'success';
        if (status >= 400 && status < 500) return 'warning';
        if (status >= 500) return 'error';
        return 'neutral';
    };

    const getResultColor = (result) => {
        if (!result) return 'neutral';
        if (result === 'SUCCESS' || result.includes('GRANTED')) return 'success';
        if (result === 'FAILURE' || result.includes('DENIED') || result.includes('ERROR')) return 'error';
        return 'warning';
    };

    const renderAuditOverview = () => (
        <div className="log-detail-grid">
            <div className="log-detail-section">
                <h4>Event Information</h4>
                <dl>
                    <dt>Event Type</dt>
                    <dd><span className="log-badge primary">{log.eventType || log.action}</span></dd>
                    <dt>Category</dt>
                    <dd><span className="log-badge info">{log.category}</span></dd>
                    <dt>Result</dt>
                    <dd><span className={`log-badge ${getResultColor(log.result)}`}>{log.result}</span></dd>
                    <dt>Timestamp</dt>
                    <dd>{formatTimestamp(log.timestamp)}</dd>
                    <dt>Description</dt>
                    <dd>{log.description || log.remarks || '—'}</dd>
                </dl>
            </div>
            <div className="log-detail-section">
                <h4>Actor</h4>
                <dl>
                    <dt>User ID</dt>
                    <dd><code>{log.actorId || log.performedByUserId || '—'}</code></dd>
                    <dt>Username</dt>
                    <dd>{log.actorName || log.performedByUsername || '—'}</dd>
                    <dt>Role</dt>
                    <dd>{log.performedByRole || (log.actorRoles?.join(', ')) || '—'}</dd>
                    <dt>Department</dt>
                    <dd>{log.performedByDepartment || '—'}</dd>
                    <dt>Branch</dt>
                    <dd>{log.performedByBranch || '—'}</dd>
                </dl>
            </div>
            <div className="log-detail-section">
                <h4>Resource</h4>
                <dl>
                    <dt>Entity Type</dt>
                    <dd><span className="log-badge neutral">{log.resourceType || log.entityName || '—'}</span></dd>
                    <dt>Entity ID</dt>
                    <dd><code>{log.resourceId || log.entityId || '—'}</code></dd>
                    <dt>Business Key</dt>
                    <dd>{log.businessKey || '—'}</dd>
                    <dt>Module</dt>
                    <dd>{log.moduleName || log.serviceName || '—'}</dd>
                </dl>
            </div>
            <div className="log-detail-section">
                <h4>Context</h4>
                <dl>
                    <dt>Correlation ID</dt>
                    <dd><code className="log-code-small">{log.correlationId || '—'}</code></dd>
                    <dt>IP Address</dt>
                    <dd><code>{log.ipAddress || '—'}</code></dd>
                    <dt>Session ID</dt>
                    <dd><code className="log-code-small">{log.sessionId || '—'}</code></dd>
                    <dt>Device ID</dt>
                    <dd>{log.deviceId || '—'}</dd>
                </dl>
            </div>
        </div>
    );

    const renderApiOverview = () => (
        <div className="log-detail-grid">
            <div className="log-detail-section">
                <h4>Request</h4>
                <dl>
                    <dt>Method</dt>
                    <dd><span className={`log-badge http-${log.httpMethod?.toLowerCase()}`}>{log.httpMethod}</span></dd>
                    <dt>Endpoint</dt>
                    <dd><code className="log-endpoint">{log.endpoint || log.fullPath}</code></dd>
                    <dt>Query Params</dt>
                    <dd><code className="log-code-small">{log.queryParams || '—'}</code></dd>
                    <dt>Content Type</dt>
                    <dd>{log.requestContentType || '—'}</dd>
                    <dt>Request Size</dt>
                    <dd>{log.requestSize ? `${log.requestSize} bytes` : '—'}</dd>
                </dl>
            </div>
            <div className="log-detail-section">
                <h4>Response</h4>
                <dl>
                    <dt>Status</dt>
                    <dd><span className={`log-badge ${getStatusColor(log.responseStatus)}`}>{log.responseStatus}</span></dd>
                    <dt>Duration</dt>
                    <dd><span className={`log-duration ${log.responseTimeMs > 1000 ? 'slow' : log.responseTimeMs > 500 ? 'medium' : 'fast'}`}>{log.responseTimeMs}ms</span></dd>
                    <dt>Content Type</dt>
                    <dd>{log.responseContentType || '—'}</dd>
                    <dt>Response Size</dt>
                    <dd>{log.responseSize ? `${log.responseSize} bytes` : '—'}</dd>
                </dl>
            </div>
            <div className="log-detail-section">
                <h4>Client</h4>
                <dl>
                    <dt>IP Address</dt>
                    <dd><code>{log.clientIp || '—'}</code></dd>
                    <dt>User Agent</dt>
                    <dd className="log-user-agent">{log.userAgent || '—'}</dd>
                    <dt>User ID</dt>
                    <dd><code>{log.authenticatedUserId || '—'}</code></dd>
                </dl>
            </div>
            <div className="log-detail-section">
                <h4>System</h4>
                <dl>
                    <dt>Service</dt>
                    <dd><span className="log-badge neutral">{log.serviceName || '—'}</span></dd>
                    <dt>Gateway</dt>
                    <dd>{log.gatewayName || '—'}</dd>
                    <dt>Correlation ID</dt>
                    <dd><code className="log-code-small">{log.correlationId || '—'}</code></dd>
                    <dt>Timestamp</dt>
                    <dd>{formatTimestamp(log.timestamp)}</dd>
                </dl>
            </div>
        </div>
    );

    const renderActivityOverview = () => (
        <div className="log-detail-grid">
            <div className="log-detail-section">
                <h4>Activity</h4>
                <dl>
                    <dt>Type</dt>
                    <dd><span className="log-badge primary">{log.activityType}</span></dd>
                    <dt>Description</dt>
                    <dd>{log.description || '—'}</dd>
                    <dt>Timestamp</dt>
                    <dd>{formatTimestamp(log.timestamp)}</dd>
                </dl>
            </div>
            <div className="log-detail-section">
                <h4>User</h4>
                <dl>
                    <dt>User ID</dt>
                    <dd><code>{log.performedByUserId || '—'}</code></dd>
                    <dt>Username</dt>
                    <dd>{log.performedByUsername || '—'}</dd>
                    <dt>Role</dt>
                    <dd>{log.performedByRole || '—'}</dd>
                    <dt>Department</dt>
                    <dd>{log.performedByDepartment || '—'}</dd>
                </dl>
            </div>
            <div className="log-detail-section">
                <h4>Entity</h4>
                <dl>
                    <dt>Type</dt>
                    <dd><span className="log-badge neutral">{log.entityType || '—'}</span></dd>
                    <dt>ID</dt>
                    <dd><code>{log.entityId || '—'}</code></dd>
                    <dt>Name</dt>
                    <dd>{log.entityName || '—'}</dd>
                </dl>
            </div>
            <div className="log-detail-section">
                <h4>Context</h4>
                <dl>
                    <dt>Correlation ID</dt>
                    <dd><code className="log-code-small">{log.correlationId || '—'}</code></dd>
                    <dt>IP Address</dt>
                    <dd><code>{log.ipAddress || '—'}</code></dd>
                    <dt>Session ID</dt>
                    <dd><code className="log-code-small">{log.sessionId || '—'}</code></dd>
                </dl>
            </div>
        </div>
    );

    const renderChanges = () => {
        if (!log.oldValue && !log.newValue && !log.changedFields) {
            return <div className="log-empty-state">No change data available</div>;
        }
        return (
            <div className="log-changes">
                {log.changedFields && (
                    <div className="log-changed-fields">
                        <h4>Changed Fields</h4>
                        <div className="log-field-tags">
                            {(Array.isArray(log.changedFields) ? log.changedFields : [log.changedFields]).map((f, i) => (
                                <span key={i} className="log-field-tag">{f}</span>
                            ))}
                        </div>
                    </div>
                )}
                <div className="log-diff">
                    <div className="log-diff-before">
                        <h4>Before</h4>
                        <pre>{log.oldValue ? (typeof log.oldValue === 'object' ? JSON.stringify(log.oldValue, null, 2) : log.oldValue) : '—'}</pre>
                    </div>
                    <div className="log-diff-after">
                        <h4>After</h4>
                        <pre>{log.newValue ? (typeof log.newValue === 'object' ? JSON.stringify(log.newValue, null, 2) : log.newValue) : '—'}</pre>
                    </div>
                </div>
            </div>
        );
    };

    const renderRawJson = () => (
        <div className="log-raw-json">
            <pre>{JSON.stringify(log, null, 2)}</pre>
        </div>
    );

    return (
        <div className="log-drawer-overlay" onClick={onClose}>
            <div className="log-drawer" onClick={e => e.stopPropagation()}>
                <div className="log-drawer-header">
                    <div className="log-drawer-title">
                        <span className="log-drawer-icon">
                            {logType === 'audit' && '🔒'}
                            {logType === 'api' && '🌐'}
                            {logType === 'activity' && '📋'}
                        </span>
                        <div>
                            <h2>{logType === 'audit' ? 'Audit Log Details' : logType === 'api' ? 'API Log Details' : 'Activity Log Details'}</h2>
                            <span className="log-drawer-id">{log.id || log.logId}</span>
                        </div>
                    </div>
                    <button className="log-drawer-close" onClick={onClose} aria-label="Close">✕</button>
                </div>

                <div className="log-drawer-tabs">
                    <button className={`log-tab ${activeTab === 'overview' ? 'active' : ''}`} onClick={() => setActiveTab('overview')}>Overview</button>
                    {logType === 'audit' && <button className={`log-tab ${activeTab === 'changes' ? 'active' : ''}`} onClick={() => setActiveTab('changes')}>Changes</button>}
                    <button className={`log-tab ${activeTab === 'raw' ? 'active' : ''}`} onClick={() => setActiveTab('raw')}>Raw JSON</button>
                </div>

                <div className="log-drawer-content">
                    {activeTab === 'overview' && logType === 'audit' && renderAuditOverview()}
                    {activeTab === 'overview' && logType === 'api' && renderApiOverview()}
                    {activeTab === 'overview' && logType === 'activity' && renderActivityOverview()}
                    {activeTab === 'changes' && renderChanges()}
                    {activeTab === 'raw' && renderRawJson()}
                </div>
            </div>
        </div>
    );
}
