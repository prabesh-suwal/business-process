import { useState, useEffect } from 'react'
import { auditLogs } from '../api'

export default function AuditLogsPage() {
    const [data, setData] = useState({ content: [], totalElements: 0 });
    const [page, setPage] = useState(0);
    const [loading, setLoading] = useState(true);

    const loadLogs = async () => {
        setLoading(true);
        try {
            const result = await auditLogs.list(page);
            setData(result);
        } catch (err) {
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { loadLogs(); }, [page]);

    const getEventBadge = (type) => {
        if (type.includes('FAILURE') || type.includes('DENIED')) return 'badge-error';
        if (type.includes('SUCCESS') || type.includes('CREATED')) return 'badge-success';
        return 'badge-neutral';
    };

    return (
        <div>
            <div className="page-header">
                <h1>Audit Logs</h1>
            </div>

            <div className="card">
                {loading ? (
                    <div className="loading">Loading...</div>
                ) : (
                    <>
                        <div className="table-container">
                            <table>
                                <thead>
                                    <tr>
                                        <th>Time</th>
                                        <th>Event</th>
                                        <th>Actor</th>
                                        <th>Target</th>
                                        <th>Product</th>
                                        <th>IP Address</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {data.content.map((log) => (
                                        <tr key={log.id}>
                                            <td className="text-muted" style={{ whiteSpace: 'nowrap' }}>
                                                {new Date(log.createdAt).toLocaleString()}
                                            </td>
                                            <td>
                                                <span className={`badge ${getEventBadge(log.eventType)}`}>
                                                    {log.eventType}
                                                </span>
                                            </td>
                                            <td>
                                                <span className="text-muted">{log.actorType}</span>
                                                {log.actorId && <span style={{ marginLeft: '4px' }}>#{log.actorId.slice(0, 8)}</span>}
                                            </td>
                                            <td>
                                                {log.targetType && (
                                                    <>
                                                        <span className="text-muted">{log.targetType}</span>
                                                        {log.targetId && <span style={{ marginLeft: '4px' }}>#{log.targetId.slice(0, 8)}</span>}
                                                    </>
                                                )}
                                            </td>
                                            <td>{log.productCode || '-'}</td>
                                            <td className="text-muted">{log.ipAddress || '-'}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>

                        <div className="pagination">
                            <span>Showing {data.content.length} of {data.totalElements}</span>
                            <div className="pagination-buttons">
                                <button className="btn btn-sm btn-secondary" disabled={page === 0} onClick={() => setPage(p => p - 1)}>Previous</button>
                                <button className="btn btn-sm btn-secondary" disabled={data.last} onClick={() => setPage(p => p + 1)}>Next</button>
                            </div>
                        </div>
                    </>
                )}
            </div>
        </div>
    );
}
