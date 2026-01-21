import { useState, useEffect } from 'react'
import { branches, geo } from '../api'

export default function BranchesPage() {
    const [branchList, setBranchList] = useState([]);
    const [geoLocations, setGeoLocations] = useState([]);
    const [loading, setLoading] = useState(true);
    const [showModal, setShowModal] = useState(false);
    const [editBranch, setEditBranch] = useState(null);
    const [form, setForm] = useState({
        code: '', name: '', localName: '', branchType: 'BRANCH',
        geoLocationId: '', address: '', phone: '', email: ''
    });

    useEffect(() => {
        loadData();
    }, []);

    const loadData = async () => {
        try {
            const [branchData, geoData] = await Promise.all([
                branches.list(),
                geo.locations('NP', 'MUNICIPALITY')
            ]);
            setBranchList(branchData);
            setGeoLocations(geoData);
        } catch (err) {
            console.error('Failed to load:', err);
        } finally {
            setLoading(false);
        }
    };

    const openCreate = () => {
        setEditBranch(null);
        setForm({
            code: '', name: '', localName: '', branchType: 'BRANCH',
            geoLocationId: '', address: '', phone: '', email: ''
        });
        setShowModal(true);
    };

    const openEdit = (branch) => {
        setEditBranch(branch);
        setForm({
            code: branch.code,
            name: branch.name,
            localName: branch.localName || '',
            branchType: branch.branchType,
            geoLocationId: branch.geoLocationId || '',
            address: branch.address || '',
            phone: branch.phone || '',
            email: branch.email || ''
        });
        setShowModal(true);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            if (editBranch) {
                await branches.update(editBranch.id, form);
            } else {
                await branches.create(form);
            }
            setShowModal(false);
            loadData();
        } catch (err) {
            alert('Error: ' + err.message);
        }
    };

    const handleDelete = async (id) => {
        if (!confirm('Close this branch?')) return;
        await branches.delete(id);
        loadData();
    };

    const branchTypeLabel = {
        HEAD_OFFICE: '🏛️ Head Office',
        REGIONAL_OFFICE: '🏢 Regional Office',
        BRANCH: '🏪 Branch',
        SUB_BRANCH: '📍 Sub-Branch',
        EXTENSION_COUNTER: '💳 Extension Counter',
        ATM: '🏧 ATM'
    };

    if (loading) return <div className="loading">Loading branches...</div>;

    return (
        <div className="page">
            <div className="page-header">
                <h1>Branches</h1>
                <button className="btn btn-primary" onClick={openCreate}>+ Add Branch</button>
            </div>

            <div className="card">
                <table className="table">
                    <thead>
                        <tr>
                            <th>Code</th>
                            <th>Name</th>
                            <th>Type</th>
                            <th>Location</th>
                            <th>Contact</th>
                            <th>Status</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {branchList.map(branch => (
                            <tr key={branch.id}>
                                <td><code>{branch.code}</code></td>
                                <td>
                                    <strong>{branch.name}</strong>
                                    {branch.localName && <div className="text-muted" style={{ fontSize: '12px' }}>{branch.localName}</div>}
                                </td>
                                <td>{branchTypeLabel[branch.branchType] || branch.branchType}</td>
                                <td>{branch.geoLocationName || '-'}</td>
                                <td>
                                    {branch.phone && <div>📞 {branch.phone}</div>}
                                    {branch.email && <div>✉️ {branch.email}</div>}
                                </td>
                                <td>
                                    <span className={`badge ${branch.status === 'ACTIVE' ? 'badge-success' : 'badge-secondary'}`}>
                                        {branch.status}
                                    </span>
                                </td>
                                <td>
                                    <button className="btn btn-sm btn-secondary" onClick={() => openEdit(branch)}>Edit</button>
                                    <button className="btn btn-sm btn-danger ml-1" onClick={() => handleDelete(branch.id)}>Close</button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            {showModal && (
                <div className="modal-overlay" onClick={() => setShowModal(false)}>
                    <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: '600px' }}>
                        <div className="modal-header">
                            <h3>{editBranch ? 'Edit Branch' : 'Create Branch'}</h3>
                            <button className="btn btn-sm btn-secondary" onClick={() => setShowModal(false)}>✕</button>
                        </div>
                        <form onSubmit={handleSubmit}>
                            <div className="modal-body">
                                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                                    <div className="form-group">
                                        <label className="form-label">Code *</label>
                                        <input className="form-input" value={form.code} disabled={!!editBranch}
                                            onChange={e => setForm({ ...form, code: e.target.value })} required />
                                    </div>
                                    <div className="form-group">
                                        <label className="form-label">Branch Type</label>
                                        <select className="form-select" value={form.branchType}
                                            onChange={e => setForm({ ...form, branchType: e.target.value })}>
                                            <option value="HEAD_OFFICE">Head Office</option>
                                            <option value="REGIONAL_OFFICE">Regional Office</option>
                                            <option value="BRANCH">Branch</option>
                                            <option value="SUB_BRANCH">Sub-Branch</option>
                                            <option value="EXTENSION_COUNTER">Extension Counter</option>
                                            <option value="ATM">ATM</option>
                                        </select>
                                    </div>
                                </div>

                                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                                    <div className="form-group">
                                        <label className="form-label">Name (English) *</label>
                                        <input className="form-input" value={form.name}
                                            onChange={e => setForm({ ...form, name: e.target.value })} required />
                                    </div>
                                    <div className="form-group">
                                        <label className="form-label">Name (Nepali)</label>
                                        <input className="form-input" value={form.localName}
                                            onChange={e => setForm({ ...form, localName: e.target.value })} />
                                    </div>
                                </div>

                                <div className="form-group">
                                    <label className="form-label">Location</label>
                                    <select className="form-select" value={form.geoLocationId}
                                        onChange={e => setForm({ ...form, geoLocationId: e.target.value })}>
                                        <option value="">-- Select Location --</option>
                                        {geoLocations.map(loc => (
                                            <option key={loc.id} value={loc.id}>{loc.name} ({loc.localName})</option>
                                        ))}
                                    </select>
                                </div>

                                <div className="form-group">
                                    <label className="form-label">Address</label>
                                    <textarea className="form-input" value={form.address} rows={2}
                                        onChange={e => setForm({ ...form, address: e.target.value })} />
                                </div>

                                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                                    <div className="form-group">
                                        <label className="form-label">Phone</label>
                                        <input className="form-input" value={form.phone}
                                            onChange={e => setForm({ ...form, phone: e.target.value })} />
                                    </div>
                                    <div className="form-group">
                                        <label className="form-label">Email</label>
                                        <input className="form-input" type="email" value={form.email}
                                            onChange={e => setForm({ ...form, email: e.target.value })} />
                                    </div>
                                </div>
                            </div>
                            <div className="modal-footer">
                                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Cancel</button>
                                <button type="submit" className="btn btn-primary">{editBranch ? 'Update' : 'Create'}</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
