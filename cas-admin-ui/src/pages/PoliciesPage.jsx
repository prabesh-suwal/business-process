import { useState, useEffect } from 'react'
import { policies, products } from '../api'
import PolicyRuleBuilder from '../components/PolicyRuleBuilder'

export default function PoliciesPage() {
    const [policyList, setPolicyList] = useState([]);
    const [availableProducts, setAvailableProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [showModal, setShowModal] = useState(false);
    const [editPolicy, setEditPolicy] = useState(null);
    const [form, setForm] = useState({
        name: '',
        description: '',
        resourceType: '',
        action: '',
        effect: 'ALLOW',
        priority: 0,
        products: [],
        rules: []
    });

    const loadData = async () => {
        setLoading(true);
        setError('');
        try {
            const [pList, prodList] = await Promise.all([
                policies.list(),
                products.list()
            ]);
            setPolicyList(pList || []);
            setAvailableProducts(prodList || []);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { loadData(); }, []);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        try {
            if (editPolicy) {
                await policies.update(editPolicy.id, form);
            } else {
                await policies.create(form);
            }
            setShowModal(false);
            setEditPolicy(null);
            setForm({ name: '', description: '', resourceType: '', action: '', effect: 'ALLOW', priority: 0, products: [], rules: [] });
            loadData();
        } catch (err) {
            setError(err.message);
        }
    };

    const handleEdit = (policy) => {
        setEditPolicy(policy);
        setForm({
            name: policy.name,
            description: policy.description || '',
            resourceType: policy.resourceType,
            action: policy.action,
            effect: policy.effect,
            priority: policy.priority,
            products: policy.products || [],
            rules: policy.rules || []
        });
        setShowModal(true);
    };

    const handleDelete = async (id) => {
        if (confirm('Are you sure you want to delete this policy?')) {
            try {
                await policies.delete(id);
                loadData();
            } catch (err) {
                setError(err.message);
            }
        }
    };

    const handleToggleActive = async (policy) => {
        try {
            if (policy.active) {
                await policies.deactivate(policy.id);
            } else {
                await policies.activate(policy.id);
            }
            loadData();
        } catch (err) {
            setError(err.message);
        }
    };

    const openCreate = () => {
        setEditPolicy(null);
        setForm({ name: '', description: '', resourceType: '', action: '', effect: 'ALLOW', priority: 0, products: [], rules: [] });
        setShowModal(true);
    };

    return (
        <div>
            <div className="page-header">
                <h1>Policies</h1>
                <button className="btn btn-primary" onClick={openCreate}>+ Create Policy</button>
            </div>

            {error && <div className="form-error mb-4">{error}</div>}

            <div className="card">
                {loading ? (
                    <div className="loading">Loading policies...</div>
                ) : policyList.length === 0 ? (
                    <div className="empty-state">
                        <p>No policies found</p>
                        <p className="text-muted">Create your first policy to start managing access control</p>
                    </div>
                ) : (
                    <div className="table-container">
                        <table>
                            <thead>
                                <tr>
                                    <th>Name</th>
                                    <th>Resource</th>
                                    <th>Action</th>
                                    <th>Effect</th>
                                    <th>Priority</th>
                                    <th>Status</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {policyList.map((policy) => (
                                    <tr key={policy.id}>
                                        <td>
                                            <strong>{policy.name}</strong>
                                            {policy.description && (
                                                <div className="text-muted" style={{ fontSize: '12px' }}>{policy.description}</div>
                                            )}
                                        </td>
                                        <td><code>{policy.resourceType}</code></td>
                                        <td><code>{policy.action}</code></td>
                                        <td>
                                            <span className={`badge ${policy.effect === 'ALLOW' ? 'badge-success' : 'badge-error'}`}>
                                                {policy.effect}
                                            </span>
                                        </td>
                                        <td>{policy.priority}</td>
                                        <td>
                                            <button
                                                className={`btn btn-sm ${policy.active ? 'badge-success' : 'badge-neutral'}`}
                                                onClick={() => handleToggleActive(policy)}
                                                style={{ cursor: 'pointer' }}
                                            >
                                                {policy.active ? 'Active' : 'Inactive'}
                                            </button>
                                        </td>
                                        <td>
                                            <div className="flex gap-2">
                                                <button className="btn btn-sm btn-secondary" onClick={() => handleEdit(policy)}>Edit</button>
                                                <button className="btn btn-sm btn-danger" onClick={() => handleDelete(policy.id)}>Delete</button>
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>

            {showModal && (
                <div className="modal-overlay" onClick={() => setShowModal(false)}>
                    <div className="modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '900px' }}>
                        <div className="modal-header">
                            <h3>{editPolicy ? 'Edit Policy' : 'Create Policy'}</h3>
                            <button className="btn btn-sm btn-secondary" onClick={() => setShowModal(false)}>✕</button>
                        </div>
                        <form onSubmit={handleSubmit}>
                            <div className="modal-body">
                                {error && <div className="form-error mb-4">{error}</div>}

                                <div className="form-group">
                                    <label className="form-label">Name</label>
                                    <input
                                        className="form-input"
                                        value={form.name}
                                        onChange={(e) => setForm({ ...form, name: e.target.value })}
                                        placeholder="e.g., loan_branch_approval"
                                        required
                                    />
                                </div>

                                <div className="form-group">
                                    <label className="form-label">Description</label>
                                    <textarea
                                        className="form-input"
                                        value={form.description}
                                        onChange={(e) => setForm({ ...form, description: e.target.value })}
                                        placeholder="Human-readable description of what this policy does"
                                        rows={2}
                                    />
                                </div>

                                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                                    <div className="form-group">
                                        <label className="form-label">Resource Type</label>
                                        <input
                                            className="form-input"
                                            value={form.resourceType}
                                            onChange={(e) => setForm({ ...form, resourceType: e.target.value })}
                                            placeholder="e.g., loan, customer, user"
                                            required
                                        />
                                    </div>

                                    <div className="form-group">
                                        <label className="form-label">Action</label>
                                        <input
                                            className="form-input"
                                            value={form.action}
                                            onChange={(e) => setForm({ ...form, action: e.target.value })}
                                            placeholder="e.g., view, approve, delete"
                                            required
                                        />
                                    </div>
                                </div>

                                <div className="form-group">
                                    <label className="form-label">Products Scope</label>
                                    <div style={{ padding: '8px', border: '1px solid var(--border)', borderRadius: '4px' }}>
                                        <label style={{ display: 'flex', alignItems: 'center', marginBottom: '8px', fontWeight: 'bold' }}>
                                            <input
                                                type="checkbox"
                                                checked={form.products.includes('*') || form.products.length === 0}
                                                onChange={(e) => {
                                                    if (e.target.checked) setForm({ ...form, products: ['*'] });
                                                    else setForm({ ...form, products: [] });
                                                }}
                                                style={{ marginRight: '8px' }}
                                            />
                                            Apply to All Products (*)
                                        </label>

                                        {/* Product List */}
                                        {!form.products.includes('*') && (
                                            <div style={{
                                                display: 'grid',
                                                gridTemplateColumns: '1fr 1fr',
                                                gap: '8px',
                                                maxHeight: '150px',
                                                overflowY: 'auto',
                                                padding: '8px',
                                                backgroundColor: 'rgba(0,0,0,0.02)'
                                            }}>
                                                {availableProducts.map(p => (
                                                    <label key={p.code} style={{ display: 'flex', alignItems: 'center', fontSize: '13px' }}>
                                                        <input
                                                            type="checkbox"
                                                            checked={form.products.includes(p.code)}
                                                            onChange={(e) => {
                                                                const newProducts = e.target.checked
                                                                    ? [...form.products, p.code]
                                                                    : form.products.filter(c => c !== p.code);
                                                                setForm({ ...form, products: newProducts });
                                                            }}
                                                            style={{ marginRight: '6px' }}
                                                        />
                                                        {p.name} ({p.code})
                                                    </label>
                                                ))}
                                                {availableProducts.length === 0 && <div className="text-muted">No products defined</div>}
                                            </div>
                                        )}
                                    </div>
                                    <small className="text-muted">Select which products this policy applies to.</small>
                                </div>

                                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                                    <div className="form-group">
                                        <label className="form-label">Effect</label>
                                        <select
                                            className="form-select"
                                            value={form.effect}
                                            onChange={(e) => setForm({ ...form, effect: e.target.value })}
                                        >
                                            <option value="ALLOW">ALLOW</option>
                                            <option value="DENY">DENY</option>
                                        </select>
                                    </div>

                                    <div className="form-group">
                                        <label className="form-label">Priority</label>
                                        <input
                                            type="number"
                                            className="form-input"
                                            value={form.priority}
                                            onChange={(e) => setForm({ ...form, priority: parseInt(e.target.value) || 0 })}
                                        />
                                    </div>
                                </div>

                                {/* Visual Rule Builder */}
                                <PolicyRuleBuilder
                                    rules={form.rules}
                                    onChange={(rules) => setForm({ ...form, rules })}
                                />
                            </div>
                            <div className="modal-footer">
                                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Cancel</button>
                                <button type="submit" className="btn btn-primary">{editPolicy ? 'Update' : 'Create'}</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
