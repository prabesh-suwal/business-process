import { useState, useEffect } from 'react';
import { loanProducts, workflowTemplates } from '../api';

export default function ProductManagement() {
    const [products, setProducts] = useState([]);
    const [templates, setTemplates] = useState([]);
    const [loading, setLoading] = useState(true);
    const [showForm, setShowForm] = useState(false);
    const [editingProduct, setEditingProduct] = useState(null);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');

    const [formData, setFormData] = useState({
        code: '',
        name: '',
        description: '',
        interestRate: '',
        minAmount: '',
        maxAmount: '',
        minTenure: '',
        maxTenure: '',
        processingFeePercent: '',
        workflowTemplateId: '',
        active: true
    });

    useEffect(() => {
        loadProducts();
        loadTemplates();
    }, []);

    const loadTemplates = async () => {
        try {
            const data = await workflowTemplates.list();
            setTemplates(data || []);
        } catch (err) {
            console.error('Failed to load templates:', err);
        }
    };

    const loadProducts = async () => {
        try {
            const data = await loanProducts.list();
            setProducts(data || []);
        } catch (err) {
            setError('Failed to load products');
        } finally {
            setLoading(false);
        }
    };

    const resetForm = () => {
        setFormData({
            code: '',
            name: '',
            description: '',
            interestRate: '',
            minAmount: '',
            maxAmount: '',
            minTenure: '',
            maxTenure: '',
            processingFeePercent: '',
            workflowTemplateId: '',
            active: true
        });
        setEditingProduct(null);
        setShowForm(false);
        setError('');
    };

    const handleEdit = (product) => {
        setFormData({
            code: product.code || '',
            name: product.name || '',
            description: product.description || '',
            interestRate: product.interestRate || '',
            minAmount: product.minAmount || '',
            maxAmount: product.maxAmount || '',
            minTenure: product.minTenure || '',
            maxTenure: product.maxTenure || '',
            processingFeePercent: product.processingFeePercent || '',
            workflowTemplateId: product.workflowTemplateId || '',
            active: product.active !== false
        });
        setEditingProduct(product);
        setShowForm(true);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSaving(true);
        setError('');

        try {
            const payload = {
                ...formData,
                interestRate: parseFloat(formData.interestRate),
                minAmount: parseFloat(formData.minAmount),
                maxAmount: parseFloat(formData.maxAmount),
                minTenure: parseInt(formData.minTenure),
                maxTenure: parseInt(formData.maxTenure),
                processingFeePercent: parseFloat(formData.processingFeePercent) || 0
            };

            if (editingProduct) {
                await loanProducts.update(editingProduct.id, payload);
            } else {
                await loanProducts.create(payload);
            }

            resetForm();
            loadProducts();
        } catch (err) {
            setError(err.message || 'Failed to save product');
        } finally {
            setSaving(false);
        }
    };

    const handleDelete = async (product) => {
        if (!confirm(`Delete "${product.name}"? This cannot be undone.`)) return;

        try {
            await loanProducts.delete(product.id);
            loadProducts();
        } catch (err) {
            setError('Failed to delete product');
        }
    };

    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : value
        }));
    };

    if (loading) {
        return <div className="main-content"><p>Loading...</p></div>;
    }

    return (
        <div className="main-content">
            <div className="page-header">
                <h1 className="page-title">Manage Loan Products</h1>
                {!showForm && (
                    <button className="btn btn-primary" onClick={() => setShowForm(true)}>
                        + New Product
                    </button>
                )}
            </div>

            {error && (
                <div className="card" style={{ background: '#fee2e2', color: '#b91c1c', marginBottom: 'var(--spacing-4)' }}>
                    {error}
                </div>
            )}

            {/* Create/Edit Form */}
            {showForm && (
                <div className="card" style={{ marginBottom: 'var(--spacing-6)' }}>
                    <h3 style={{ marginBottom: 'var(--spacing-4)' }}>
                        {editingProduct ? 'Edit Product' : 'Create New Product'}
                    </h3>

                    <form onSubmit={handleSubmit}>
                        <div className="grid-2">
                            <div className="form-group">
                                <label className="form-label">Product Code *</label>
                                <input
                                    type="text"
                                    name="code"
                                    className="form-input"
                                    value={formData.code}
                                    onChange={handleChange}
                                    placeholder="e.g., HOME_LOAN"
                                    required
                                    disabled={!!editingProduct}
                                />
                            </div>
                            <div className="form-group">
                                <label className="form-label">Product Name *</label>
                                <input
                                    type="text"
                                    name="name"
                                    className="form-input"
                                    value={formData.name}
                                    onChange={handleChange}
                                    placeholder="e.g., Home Loan"
                                    required
                                />
                            </div>
                        </div>

                        <div className="form-group">
                            <label className="form-label">Description</label>
                            <textarea
                                name="description"
                                className="form-input"
                                value={formData.description}
                                onChange={handleChange}
                                placeholder="Brief description of the loan product"
                                rows={2}
                            />
                        </div>

                        <div className="grid-3">
                            <div className="form-group">
                                <label className="form-label">Interest Rate (% p.a.) *</label>
                                <input
                                    type="number"
                                    name="interestRate"
                                    className="form-input"
                                    value={formData.interestRate}
                                    onChange={handleChange}
                                    step="0.01"
                                    min="0"
                                    max="50"
                                    required
                                />
                            </div>
                            <div className="form-group">
                                <label className="form-label">Min Amount (₹) *</label>
                                <input
                                    type="number"
                                    name="minAmount"
                                    className="form-input"
                                    value={formData.minAmount}
                                    onChange={handleChange}
                                    min="0"
                                    required
                                />
                            </div>
                            <div className="form-group">
                                <label className="form-label">Max Amount (₹) *</label>
                                <input
                                    type="number"
                                    name="maxAmount"
                                    className="form-input"
                                    value={formData.maxAmount}
                                    onChange={handleChange}
                                    min="0"
                                    required
                                />
                            </div>
                        </div>

                        <div className="grid-3">
                            <div className="form-group">
                                <label className="form-label">Min Tenure (months) *</label>
                                <input
                                    type="number"
                                    name="minTenure"
                                    className="form-input"
                                    value={formData.minTenure}
                                    onChange={handleChange}
                                    min="1"
                                    required
                                />
                            </div>
                            <div className="form-group">
                                <label className="form-label">Max Tenure (months) *</label>
                                <input
                                    type="number"
                                    name="maxTenure"
                                    className="form-input"
                                    value={formData.maxTenure}
                                    onChange={handleChange}
                                    min="1"
                                    required
                                />
                            </div>
                            <div className="form-group">
                                <label className="form-label">Processing Fee (%)</label>
                                <input
                                    type="number"
                                    name="processingFeePercent"
                                    className="form-input"
                                    value={formData.processingFeePercent}
                                    onChange={handleChange}
                                    step="0.01"
                                    min="0"
                                    max="10"
                                />
                            </div>
                        </div>

                        <div className="form-group">
                            <label style={{ display: 'flex', alignItems: 'center', gap: 'var(--spacing-2)', cursor: 'pointer' }}>
                                <input
                                    type="checkbox"
                                    name="active"
                                    checked={formData.active}
                                    onChange={handleChange}
                                />
                                <span>Active (available for new applications)</span>
                            </label>
                        </div>

                        {/* Workflow Template Selection */}
                        <div className="form-group">
                            <label className="form-label">Workflow Template</label>
                            <select
                                name="workflowTemplateId"
                                className="form-input"
                                value={formData.workflowTemplateId}
                                onChange={handleChange}
                            >
                                <option value="">No workflow (manual processing)</option>
                                {templates.map(t => (
                                    <option key={t.id} value={t.id}>
                                        {t.name} (v{t.version}) - {t.status}
                                    </option>
                                ))}
                            </select>
                            <small style={{ color: 'var(--color-gray-500)', marginTop: 'var(--spacing-1)', display: 'block' }}>
                                Links this product to an automated workflow for application processing
                            </small>
                        </div>

                        <div style={{ display: 'flex', gap: 'var(--spacing-3)', marginTop: 'var(--spacing-4)' }}>
                            <button type="submit" className="btn btn-primary" disabled={saving}>
                                {saving ? 'Saving...' : (editingProduct ? 'Update Product' : 'Create Product')}
                            </button>
                            <button type="button" className="btn btn-secondary" onClick={resetForm}>
                                Cancel
                            </button>
                        </div>
                    </form>
                </div>
            )}

            {/* Products Table */}
            <div className="card">
                <h3 style={{ marginBottom: 'var(--spacing-4)' }}>Existing Products</h3>

                {products.length === 0 ? (
                    <p style={{ textAlign: 'center', color: 'var(--color-gray-500)' }}>
                        No loan products configured yet
                    </p>
                ) : (
                    <div className="table-container">
                        <table>
                            <thead>
                                <tr>
                                    <th>Code</th>
                                    <th>Name</th>
                                    <th>Interest Rate</th>
                                    <th>Amount Range</th>
                                    <th>Tenure</th>
                                    <th>Status</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {products.map((product) => (
                                    <tr key={product.id}>
                                        <td><strong>{product.code}</strong></td>
                                        <td>{product.name}</td>
                                        <td>{product.interestRate}%</td>
                                        <td>₹{(product.minAmount / 100000).toFixed(1)}L - ₹{(product.maxAmount / 100000).toFixed(1)}L</td>
                                        <td>{product.minTenure}-{product.maxTenure} mo</td>
                                        <td>
                                            <span className={`badge ${product.active ? 'badge-approved' : 'badge-draft'}`}>
                                                {product.active ? 'Active' : 'Inactive'}
                                            </span>
                                        </td>
                                        <td>
                                            <div style={{ display: 'flex', gap: 'var(--spacing-2)' }}>
                                                <button className="btn btn-secondary" onClick={() => handleEdit(product)}>
                                                    Edit
                                                </button>
                                                <button className="btn btn-danger" onClick={() => handleDelete(product)}>
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
        </div>
    );
}
