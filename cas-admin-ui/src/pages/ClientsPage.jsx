import { useState, useEffect } from 'react'
import { clients } from '../api'

export default function ClientsPage() {
    const [data, setData] = useState({ content: [], totalElements: 0 });
    const [page, setPage] = useState(0);
    const [loading, setLoading] = useState(true);
    const [showModal, setShowModal] = useState(false);
    const [newClient, setNewClient] = useState(null);
    const [form, setForm] = useState({ name: '', description: '' });

    const loadClients = async () => {
        setLoading(true);
        try {
            const result = await clients.list(page);
            setData(result);
        } catch (err) {
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { loadClients(); }, [page]);

    const handleCreate = async (e) => {
        e.preventDefault();
        try {
            const result = await clients.create(form);
            setNewClient(result); // Show secret once
            setForm({ name: '', description: '' });
            loadClients();
        } catch (err) {
            alert(err.message);
        }
    };

    const handleRevoke = async (id) => {
        if (confirm('Revoke this API client?')) {
            await clients.revoke(id);
            loadClients();
        }
    };

    const handleRotate = async (id) => {
        if (confirm('Rotate secret? The old secret will stop working immediately.')) {
            const result = await clients.rotateSecret(id);
            setNewClient(result);
        }
    };

    return (
        <div>
            <div className="page-header">
                <h1>API Clients</h1>
                <button className="btn btn-primary" onClick={() => setShowModal(true)}>+ Create Client</button>
            </div>

            {newClient?.clientSecret && (
                <div className="card mb-4" style={{ background: '#fef3c7', borderColor: '#fcd34d' }}>
                    <h4 style={{ color: '#92400e', marginBottom: '8px' }}>⚠️ Save Your Client Secret</h4>
                    <p className="text-muted" style={{ marginBottom: '12px' }}>This secret will only be shown once. Copy it now!</p>
                    <div style={{ fontFamily: 'monospace', background: '#fff', padding: '12px', borderRadius: '4px', wordBreak: 'break-all' }}>
                        <div><strong>Client ID:</strong> {newClient.clientId}</div>
                        <div><strong>Client Secret:</strong> {newClient.clientSecret}</div>
                    </div>
                    <button className="btn btn-secondary mt-4" onClick={() => setNewClient(null)}>I've saved it</button>
                </div>
            )}

            <div className="card">
                {loading ? (
                    <div className="loading">Loading...</div>
                ) : (
                    <>
                        <div className="table-container">
                            <table>
                                <thead>
                                    <tr>
                                        <th>Client ID</th>
                                        <th>Name</th>
                                        <th>Status</th>
                                        <th>Created</th>
                                        <th>Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {data.content.map((client) => (
                                        <tr key={client.id}>
                                            <td><code>{client.clientId}</code></td>
                                            <td>{client.name}</td>
                                            <td>
                                                <span className={`badge ${client.status === 'ACTIVE' ? 'badge-success' : 'badge-error'}`}>
                                                    {client.status}
                                                </span>
                                            </td>
                                            <td>{new Date(client.createdAt).toLocaleDateString()}</td>
                                            <td>
                                                <div className="flex gap-2">
                                                    <button className="btn btn-sm btn-secondary" onClick={() => handleRotate(client.id)}>Rotate Secret</button>
                                                    <button className="btn btn-sm btn-danger" onClick={() => handleRevoke(client.id)}>Revoke</button>
                                                </div>
                                            </td>
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

            {showModal && (
                <div className="modal-overlay" onClick={() => setShowModal(false)}>
                    <div className="modal" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <h3>Create API Client</h3>
                            <button className="btn btn-sm btn-secondary" onClick={() => setShowModal(false)}>✕</button>
                        </div>
                        <form onSubmit={handleCreate}>
                            <div className="modal-body">
                                <div className="form-group">
                                    <label className="form-label">Name</label>
                                    <input
                                        className="form-input"
                                        value={form.name}
                                        onChange={(e) => setForm({ ...form, name: e.target.value })}
                                        required
                                    />
                                </div>
                                <div className="form-group">
                                    <label className="form-label">Description</label>
                                    <input
                                        className="form-input"
                                        value={form.description}
                                        onChange={(e) => setForm({ ...form, description: e.target.value })}
                                    />
                                </div>
                            </div>
                            <div className="modal-footer">
                                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Cancel</button>
                                <button type="submit" className="btn btn-primary">Create</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
