import { useState, useEffect } from 'react'
import { roles, products } from '../api'

export default function RolesPage() {
    const [productList, setProductList] = useState([]);
    const [roleList, setRoleList] = useState([]);
    const [permissionList, setPermissionList] = useState([]);
    const [selectedProduct, setSelectedProduct] = useState('');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    // Modal states
    const [showModal, setShowModal] = useState(false);
    const [editRole, setEditRole] = useState(null);
    const [showPermissionsModal, setShowPermissionsModal] = useState(false);
    const [editingPermissionsRole, setEditingPermissionsRole] = useState(null);
    const [selectedPermissions, setSelectedPermissions] = useState([]);

    const [form, setForm] = useState({
        productCode: '',
        code: '',
        name: '',
        description: ''
    });

    useEffect(() => {
        const loadProducts = async () => {
            try {
                const data = await products.list();
                setProductList(data);
                if (data.length > 0) {
                    setSelectedProduct(data[0].code);
                    setForm(f => ({ ...f, productCode: data[0].code }));
                }
            } catch (err) {
                setError(err.message);
            }
        };
        loadProducts();
    }, []);

    useEffect(() => {
        if (selectedProduct) loadRoles();
    }, [selectedProduct]);

    const loadRoles = async () => {
        setLoading(true);
        setError('');
        try {
            const data = await roles.list(selectedProduct);
            setRoleList(data);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    const loadPermissions = async (productCode) => {
        try {
            const data = await products.permissions(productCode);
            setPermissionList(data);
        } catch (err) {
            setError(err.message);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        try {
            if (editRole) {
                await roles.update(editRole.id, {
                    name: form.name,
                    description: form.description
                });
            } else {
                await roles.create(form);
            }
            setShowModal(false);
            setEditRole(null);
            setForm({ productCode: selectedProduct, code: '', name: '', description: '' });
            loadRoles();
        } catch (err) {
            setError(err.message);
        }
    };

    const handleEdit = (role) => {
        setEditRole(role);
        setForm({
            productCode: role.productCode || selectedProduct,
            code: role.code,
            name: role.name,
            description: role.description || ''
        });
        setShowModal(true);
    };

    const handleDelete = async (role) => {
        if (role.systemRole) {
            alert('Cannot delete system roles');
            return;
        }
        if (!confirm(`Delete role "${role.name}"? This cannot be undone.`)) return;

        setError('');
        try {
            await roles.delete(role.id);
            loadRoles();
        } catch (err) {
            setError(err.message);
        }
    };

    const openCreate = () => {
        setEditRole(null);
        setForm({ productCode: selectedProduct, code: '', name: '', description: '' });
        setShowModal(true);
    };

    const openPermissionsEditor = async (role) => {
        setEditingPermissionsRole(role);
        await loadPermissions(selectedProduct);
        setSelectedPermissions(role.permissions?.map(p => p.id) || []);
        setShowPermissionsModal(true);
    };

    const handleSavePermissions = async () => {
        setError('');
        try {
            await roles.setPermissions(editingPermissionsRole.id, selectedPermissions);
            setShowPermissionsModal(false);
            loadRoles();
        } catch (err) {
            setError(err.message);
        }
    };

    const togglePermission = (permId) => {
        setSelectedPermissions(prev =>
            prev.includes(permId)
                ? prev.filter(id => id !== permId)
                : [...prev, permId]
        );
    };

    return (
        <div>
            <div className="page-header">
                <h1>Roles</h1>
                <div className="flex gap-2">
                    <select
                        className="form-select"
                        style={{ width: '200px' }}
                        value={selectedProduct}
                        onChange={(e) => setSelectedProduct(e.target.value)}
                    >
                        {productList.map((p) => (
                            <option key={p.code} value={p.code}>{p.name}</option>
                        ))}
                    </select>
                    <button className="btn btn-primary" onClick={openCreate}>+ Add Role</button>
                </div>
            </div>

            {error && <div className="form-error mb-4">{error}</div>}

            <div className="card">
                {loading ? (
                    <div className="loading">Loading...</div>
                ) : roleList.length === 0 ? (
                    <div className="empty-state">No roles found for this product</div>
                ) : (
                    <div className="table-container">
                        <table>
                            <thead>
                                <tr>
                                    <th>Code</th>
                                    <th>Name</th>
                                    <th>Description</th>
                                    <th>Type</th>
                                    <th>Permissions</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {roleList.map((role) => (
                                    <tr key={role.id}>
                                        <td><code>{role.code}</code></td>
                                        <td><strong>{role.name}</strong></td>
                                        <td className="text-muted">{role.description || '-'}</td>
                                        <td>
                                            <span className={`badge ${role.systemRole ? 'badge-warning' : 'badge-neutral'}`}>
                                                {role.systemRole ? 'System' : 'Custom'}
                                            </span>
                                        </td>
                                        <td>
                                            <button
                                                className="btn btn-sm btn-secondary"
                                                onClick={() => openPermissionsEditor(role)}
                                            >
                                                {role.permissions?.length || 0} permissions
                                            </button>
                                        </td>
                                        <td>
                                            <div className="flex gap-2">
                                                <button
                                                    className="btn btn-sm btn-secondary"
                                                    onClick={() => handleEdit(role)}
                                                    disabled={role.systemRole}
                                                >
                                                    Edit
                                                </button>
                                                <button
                                                    className="btn btn-sm btn-danger"
                                                    onClick={() => handleDelete(role)}
                                                    disabled={role.systemRole}
                                                >
                                                    Delete
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>

            {/* Create/Edit Role Modal */}
            {showModal && (
                <div className="modal-overlay" onClick={() => setShowModal(false)}>
                    <div className="modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '500px' }}>
                        <div className="modal-header">
                            <h3>{editRole ? 'Edit Role' : 'Create Role'}</h3>
                            <button className="btn btn-sm btn-secondary" onClick={() => setShowModal(false)}>✕</button>
                        </div>
                        <form onSubmit={handleSubmit}>
                            <div className="modal-body">
                                {!editRole && (
                                    <div className="form-group">
                                        <label className="form-label">Product</label>
                                        <select
                                            className="form-select"
                                            value={form.productCode}
                                            onChange={(e) => setForm({ ...form, productCode: e.target.value })}
                                            required
                                        >
                                            {productList.map((p) => (
                                                <option key={p.code} value={p.code}>{p.name}</option>
                                            ))}
                                        </select>
                                    </div>
                                )}

                                <div className="form-group">
                                    <label className="form-label">Code</label>
                                    <input
                                        className="form-input"
                                        value={form.code}
                                        onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase().replace(/[^A-Z0-9_]/g, '') })}
                                        placeholder="LOAN_OFFICER"
                                        required
                                        disabled={!!editRole}
                                    />
                                    <small className="text-muted">Uppercase letters, numbers, underscores only</small>
                                </div>

                                <div className="form-group">
                                    <label className="form-label">Name</label>
                                    <input
                                        className="form-input"
                                        value={form.name}
                                        onChange={(e) => setForm({ ...form, name: e.target.value })}
                                        placeholder="Loan Officer"
                                        required
                                    />
                                </div>

                                <div className="form-group">
                                    <label className="form-label">Description</label>
                                    <textarea
                                        className="form-input"
                                        value={form.description}
                                        onChange={(e) => setForm({ ...form, description: e.target.value })}
                                        placeholder="What this role can do..."
                                        rows={2}
                                    />
                                </div>
                            </div>
                            <div className="modal-footer">
                                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Cancel</button>
                                <button type="submit" className="btn btn-primary">{editRole ? 'Update' : 'Create'}</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* Permissions Editor Modal */}
            {showPermissionsModal && editingPermissionsRole && (
                <div className="modal-overlay" onClick={() => setShowPermissionsModal(false)}>
                    <div className="modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '550px' }}>
                        <div className="modal-header">
                            <h3>Permissions - {editingPermissionsRole.name}</h3>
                            <button className="btn btn-sm btn-secondary" onClick={() => setShowPermissionsModal(false)}>✕</button>
                        </div>
                        <div className="modal-body" style={{ maxHeight: '50vh', overflowY: 'auto' }}>
                            {permissionList.length === 0 ? (
                                <div className="text-muted">No permissions defined for this product</div>
                            ) : (
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                                    {permissionList.map(perm => (
                                        <label
                                            key={perm.id}
                                            style={{
                                                display: 'flex',
                                                alignItems: 'center',
                                                gap: '12px',
                                                padding: '10px 12px',
                                                border: '1px solid var(--border)',
                                                borderRadius: '6px',
                                                cursor: 'pointer',
                                                background: selectedPermissions.includes(perm.id) ? 'rgba(var(--primary-rgb), 0.1)' : 'transparent'
                                            }}
                                        >
                                            <input
                                                type="checkbox"
                                                checked={selectedPermissions.includes(perm.id)}
                                                onChange={() => togglePermission(perm.id)}
                                                style={{ width: '18px', height: '18px' }}
                                            />
                                            <div>
                                                <code style={{ fontSize: '13px' }}>{perm.code}</code>
                                                {perm.description && (
                                                    <div className="text-muted" style={{ fontSize: '12px' }}>{perm.description}</div>
                                                )}
                                            </div>
                                        </label>
                                    ))}
                                </div>
                            )}
                        </div>
                        <div className="modal-footer">
                            <span className="text-muted" style={{ marginRight: 'auto' }}>
                                {selectedPermissions.length} selected
                            </span>
                            <button className="btn btn-secondary" onClick={() => setShowPermissionsModal(false)}>Cancel</button>
                            <button className="btn btn-primary" onClick={handleSavePermissions}>Save Permissions</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
