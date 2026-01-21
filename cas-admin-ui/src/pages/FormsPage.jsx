import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { products, formDefinitions } from '../api';
import Modal from '../components/common/Modal';

const FIELD_TYPES = [
    { value: 'TEXT', label: '📝 Text', icon: '📝' },
    { value: 'TEXTAREA', label: '📄 Text Area', icon: '📄' },
    { value: 'NUMBER', label: '🔢 Number', icon: '🔢' },
    { value: 'DATE', label: '📅 Date', icon: '📅' },
    { value: 'DATETIME', label: '🕐 Date & Time', icon: '🕐' },
    { value: 'DROPDOWN', label: '📋 Dropdown', icon: '📋' },
    { value: 'MULTI_SELECT', label: '☑️ Multi-Select', icon: '☑️' },
    { value: 'CHECKBOX', label: '✅ Checkbox', icon: '✅' },
    { value: 'RADIO', label: '🔘 Radio', icon: '🔘' },
    { value: 'FILE', label: '📎 File Upload', icon: '📎' },
    { value: 'SIGNATURE', label: '✍️ Signature', icon: '✍️' },
];

export default function FormsPage() {
    const navigate = useNavigate();
    const [productList, setProductList] = useState([]);
    const [selectedProduct, setSelectedProduct] = useState(null);
    const [forms, setForms] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [showCreateModal, setShowCreateModal] = useState(false);
    const [newForm, setNewForm] = useState({ name: '', description: '' });

    useEffect(() => {
        loadProducts();
    }, []);

    useEffect(() => {
        if (selectedProduct) {
            loadForms();
        }
    }, [selectedProduct]);

    const loadProducts = async () => {
        try {
            const data = await products.list();
            setProductList(data || []);
            if (data?.length > 0) {
                setSelectedProduct(data[0]);
            }
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    const loadForms = async () => {
        if (!selectedProduct) return;
        try {
            setLoading(true);
            const data = await formDefinitions.list(selectedProduct.id, false);
            setForms(data || []);
        } catch (err) {
            console.warn('Form service not available:', err);
            setForms([]);
        } finally {
            setLoading(false);
        }
    };

    const handleCreateForm = async () => {
        if (!newForm.name.trim()) {
            setError('Name is required');
            return;
        }
        try {
            const form = await formDefinitions.create({
                productId: selectedProduct.id,
                name: newForm.name,
                description: newForm.description,
                schema: { type: 'object', properties: {} },
                fields: []
            });
            setShowCreateModal(false);
            setNewForm({ name: '', description: '' });
            navigate(`/forms/${form.id}/design`);
        } catch (err) {
            setError(err.message);
        }
    };

    const handleActivate = async (formId) => {
        try {
            await formDefinitions.activate(formId);
            loadForms();
        } catch (err) {
            setError(err.message);
        }
    };

    const handleDelete = async (formId) => {
        if (!confirm('Delete this form?')) return;
        try {
            await formDefinitions.delete(formId);
            loadForms();
        } catch (err) {
            setError(err.message);
        }
    };

    const getStatusBadge = (status) => {
        const styles = {
            DRAFT: { background: 'var(--warning)', color: '#000' },
            ACTIVE: { background: 'var(--success)', color: '#fff' },
            DEPRECATED: { background: 'var(--text-secondary)', color: '#fff' }
        };
        return (
            <span className="badge" style={styles[status] || styles.DRAFT}>
                {status}
            </span>
        );
    };

    return (
        <div className="page-container">
            <div className="page-header">
                <div>
                    <h1>Form Definitions</h1>
                    <p className="page-subtitle">Build dynamic forms for workflow tasks</p>
                </div>
                <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
                    <select
                        className="form-control"
                        value={selectedProduct?.id || ''}
                        onChange={(e) => {
                            const prod = productList.find(p => p.id === e.target.value);
                            setSelectedProduct(prod);
                        }}
                        style={{ minWidth: '200px' }}
                    >
                        {productList.map(prod => (
                            <option key={prod.id} value={prod.id}>{prod.name}</option>
                        ))}
                    </select>
                    <button
                        className="btn btn-primary"
                        onClick={() => setShowCreateModal(true)}
                        disabled={!selectedProduct}
                    >
                        + New Form
                    </button>
                </div>
            </div>

            {error && <div className="alert alert-danger">{error}</div>}

            <div className="card">
                <table className="data-table">
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th>Description</th>
                            <th>Fields</th>
                            <th>Version</th>
                            <th>Status</th>
                            <th style={{ width: '200px' }}>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {loading ? (
                            <tr><td colSpan="6" style={{ textAlign: 'center' }}>Loading...</td></tr>
                        ) : forms.length === 0 ? (
                            <tr>
                                <td colSpan="6" style={{ textAlign: 'center', padding: '48px' }}>
                                    <div style={{ color: 'var(--text-secondary)' }}>
                                        <div style={{ fontSize: '48px', marginBottom: '12px' }}>📝</div>
                                        <div>No forms yet</div>
                                        <button
                                            className="btn btn-primary"
                                            style={{ marginTop: '16px' }}
                                            onClick={() => setShowCreateModal(true)}
                                        >
                                            Create your first form
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        ) : forms.map(form => (
                            <tr key={form.id}>
                                <td><strong>{form.name}</strong></td>
                                <td>{form.description || '-'}</td>
                                <td>{form.fields?.length || 0} fields</td>
                                <td>v{form.version}</td>
                                <td>{getStatusBadge(form.status)}</td>
                                <td>
                                    <div style={{ display: 'flex', gap: '8px' }}>
                                        <button
                                            className="btn btn-sm btn-secondary"
                                            onClick={() => navigate(`/forms/${form.id}/design`)}
                                        >
                                            {form.status === 'DRAFT' ? '✏️ Edit' : '👁️ View'}
                                        </button>
                                        {form.status === 'DRAFT' && (
                                            <>
                                                <button
                                                    className="btn btn-sm btn-success"
                                                    onClick={() => handleActivate(form.id)}
                                                >
                                                    ✓ Activate
                                                </button>
                                                <button
                                                    className="btn btn-sm btn-danger"
                                                    onClick={() => handleDelete(form.id)}
                                                >
                                                    🗑️
                                                </button>
                                            </>
                                        )}
                                    </div>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            {/* Create Modal */}
            <Modal
                isOpen={showCreateModal}
                onClose={() => setShowCreateModal(false)}
                title="Create Form"
            >
                <div className="form-group">
                    <label>Form Name *</label>
                    <input
                        type="text"
                        className="form-control"
                        value={newForm.name}
                        onChange={(e) => setNewForm({ ...newForm, name: e.target.value })}
                        placeholder="e.g., Loan Application Form"
                    />
                </div>
                <div className="form-group">
                    <label>Description</label>
                    <textarea
                        className="form-control"
                        rows={3}
                        value={newForm.description}
                        onChange={(e) => setNewForm({ ...newForm, description: e.target.value })}
                        placeholder="Describe the form purpose..."
                    />
                </div>
                <div className="modal-footer">
                    <button className="btn btn-secondary" onClick={() => setShowCreateModal(false)}>
                        Cancel
                    </button>
                    <button className="btn btn-primary" onClick={handleCreateForm}>
                        Create & Design
                    </button>
                </div>
            </Modal>
        </div>
    );
}
