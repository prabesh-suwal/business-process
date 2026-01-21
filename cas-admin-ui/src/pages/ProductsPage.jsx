import { useState, useEffect } from 'react'
import { products } from '../api'

export default function ProductsPage() {
    const [productList, setProductList] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    // Product modal
    const [showProductModal, setShowProductModal] = useState(false);
    const [editProduct, setEditProduct] = useState(null);
    const [productForm, setProductForm] = useState({ code: '', name: '', description: '' });

    // Permission modal
    const [selectedProduct, setSelectedProduct] = useState(null);
    const [permissions, setPermissions] = useState([]);
    const [showPermModal, setShowPermModal] = useState(false);
    const [editPerm, setEditPerm] = useState(null);
    const [permForm, setPermForm] = useState({ code: '', name: '', description: '' });

    useEffect(() => {
        loadProducts();
    }, []);

    const loadProducts = async () => {
        setLoading(true);
        setError('');
        try {
            const data = await products.list();
            setProductList(data);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    const loadPermissions = async (productCode) => {
        try {
            const data = await products.permissions(productCode);
            setPermissions(data);
        } catch (err) {
            setError(err.message);
        }
    };

    // Product CRUD
    const handleProductSubmit = async (e) => {
        e.preventDefault();
        setError('');
        try {
            if (editProduct) {
                await products.update(editProduct.code, productForm);
            } else {
                await products.create(productForm);
            }
            setShowProductModal(false);
            setEditProduct(null);
            setProductForm({ code: '', name: '', description: '' });
            loadProducts();
        } catch (err) {
            setError(err.message);
        }
    };

    const handleEditProduct = (product) => {
        setEditProduct(product);
        setProductForm({ code: product.code, name: product.name, description: product.description || '' });
        setShowProductModal(true);
    };

    const handleDeleteProduct = async (code) => {
        if (!confirm(`Deactivate product "${code}"?`)) return;
        try {
            await products.delete(code);
            loadProducts();
        } catch (err) {
            setError(err.message);
        }
    };

    const openCreateProduct = () => {
        setEditProduct(null);
        setProductForm({ code: '', name: '', description: '' });
        setShowProductModal(true);
    };

    // Permission CRUD
    const openPermissions = async (product) => {
        setSelectedProduct(product);
        await loadPermissions(product.code);
    };

    const handlePermSubmit = async (e) => {
        e.preventDefault();
        setError('');
        try {
            if (editPerm) {
                await products.updatePermission(selectedProduct.code, editPerm.id, permForm);
            } else {
                await products.createPermission(selectedProduct.code, permForm);
            }
            setShowPermModal(false);
            setEditPerm(null);
            setPermForm({ code: '', name: '', description: '' });
            loadPermissions(selectedProduct.code);
        } catch (err) {
            setError(err.message);
        }
    };

    const handleEditPerm = (perm) => {
        setEditPerm(perm);
        setPermForm({ code: perm.code, name: perm.name, description: perm.description || '' });
        setShowPermModal(true);
    };

    const handleDeletePerm = async (permId) => {
        if (!confirm('Delete this permission?')) return;
        try {
            await products.deletePermission(selectedProduct.code, permId);
            loadPermissions(selectedProduct.code);
        } catch (err) {
            setError(err.message);
        }
    };

    const openCreatePerm = () => {
        setEditPerm(null);
        setPermForm({ code: '', name: '', description: '' });
        setShowPermModal(true);
    };

    return (
        <div>
            <div className="page-header">
                <h1>Products & Permissions</h1>
                <button className="btn btn-primary" onClick={openCreateProduct}>+ Add Product</button>
            </div>

            {error && <div className="form-error mb-4">{error}</div>}

            <div style={{ display: 'grid', gridTemplateColumns: selectedProduct ? '1fr 1fr' : '1fr', gap: '24px' }}>
                {/* Products List */}
                <div className="card">
                    <h3 style={{ marginBottom: '16px' }}>Products</h3>
                    {loading ? (
                        <div className="loading">Loading...</div>
                    ) : productList.length === 0 ? (
                        <div className="empty-state">No products found</div>
                    ) : (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                            {productList.map(product => (
                                <div
                                    key={product.code}
                                    onClick={() => openPermissions(product)}
                                    style={{
                                        padding: '12px',
                                        border: selectedProduct?.code === product.code ? '2px solid var(--primary)' : '1px solid var(--border)',
                                        borderRadius: '8px',
                                        cursor: 'pointer',
                                        background: selectedProduct?.code === product.code ? 'rgba(var(--primary-rgb), 0.05)' : 'transparent'
                                    }}
                                >
                                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                        <div>
                                            <strong>{product.name}</strong>
                                            <code style={{ marginLeft: '8px', fontSize: '12px', color: 'var(--text-muted)' }}>{product.code}</code>
                                            <span className={`badge ml-2 ${product.status === 'ACTIVE' ? 'badge-success' : 'badge-neutral'}`}>
                                                {product.status}
                                            </span>
                                        </div>
                                        <div className="flex gap-1" onClick={e => e.stopPropagation()}>
                                            <button className="btn btn-sm btn-secondary" onClick={() => handleEditProduct(product)}>Edit</button>
                                            <button className="btn btn-sm btn-danger" onClick={() => handleDeleteProduct(product.code)}>Delete</button>
                                        </div>
                                    </div>
                                    {product.description && (
                                        <div className="text-muted" style={{ fontSize: '12px', marginTop: '4px' }}>{product.description}</div>
                                    )}
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                {/* Permissions Panel */}
                {selectedProduct && (
                    <div className="card">
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                            <h3>Permissions - {selectedProduct.name}</h3>
                            <button className="btn btn-sm btn-primary" onClick={openCreatePerm}>+ Add</button>
                        </div>
                        {permissions.length === 0 ? (
                            <div className="empty-state">No permissions defined</div>
                        ) : (
                            <div className="table-container">
                                <table>
                                    <thead>
                                        <tr>
                                            <th>Code</th>
                                            <th>Name</th>
                                            <th>Actions</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {permissions.map(perm => (
                                            <tr key={perm.id}>
                                                <td><code>{perm.code}</code></td>
                                                <td>{perm.name}</td>
                                                <td>
                                                    <div className="flex gap-1">
                                                        <button className="btn btn-sm btn-secondary" onClick={() => handleEditPerm(perm)}>Edit</button>
                                                        <button className="btn btn-sm btn-danger" onClick={() => handleDeletePerm(perm.id)}>Delete</button>
                                                    </div>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </div>
                )}
            </div>

            {/* Product Modal */}
            {showProductModal && (
                <div className="modal-overlay" onClick={() => setShowProductModal(false)}>
                    <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: '450px' }}>
                        <div className="modal-header">
                            <h3>{editProduct ? 'Edit Product' : 'Create Product'}</h3>
                            <button className="btn btn-sm btn-secondary" onClick={() => setShowProductModal(false)}>✕</button>
                        </div>
                        <form onSubmit={handleProductSubmit}>
                            <div className="modal-body">
                                <div className="form-group">
                                    <label className="form-label">Code</label>
                                    <input
                                        className="form-input"
                                        value={productForm.code}
                                        onChange={e => setProductForm({ ...productForm, code: e.target.value.toUpperCase().replace(/[^A-Z0-9_]/g, '') })}
                                        placeholder="LMS, WFM, CRM"
                                        required
                                        disabled={!!editProduct}
                                    />
                                </div>
                                <div className="form-group">
                                    <label className="form-label">Name</label>
                                    <input
                                        className="form-input"
                                        value={productForm.name}
                                        onChange={e => setProductForm({ ...productForm, name: e.target.value })}
                                        placeholder="Loan Management System"
                                        required
                                    />
                                </div>
                                <div className="form-group">
                                    <label className="form-label">Description</label>
                                    <textarea
                                        className="form-input"
                                        value={productForm.description}
                                        onChange={e => setProductForm({ ...productForm, description: e.target.value })}
                                        rows={2}
                                    />
                                </div>
                            </div>
                            <div className="modal-footer">
                                <button type="button" className="btn btn-secondary" onClick={() => setShowProductModal(false)}>Cancel</button>
                                <button type="submit" className="btn btn-primary">{editProduct ? 'Update' : 'Create'}</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* Permission Modal */}
            {showPermModal && (
                <div className="modal-overlay" onClick={() => setShowPermModal(false)}>
                    <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: '450px' }}>
                        <div className="modal-header">
                            <h3>{editPerm ? 'Edit Permission' : 'Create Permission'}</h3>
                            <button className="btn btn-sm btn-secondary" onClick={() => setShowPermModal(false)}>✕</button>
                        </div>
                        <form onSubmit={handlePermSubmit}>
                            <div className="modal-body">
                                <div className="form-group">
                                    <label className="form-label">Code</label>
                                    <input
                                        className="form-input"
                                        value={permForm.code}
                                        onChange={e => setPermForm({ ...permForm, code: e.target.value.toLowerCase().replace(/[^a-z0-9:_]/g, '') })}
                                        placeholder="loan:read, loan:approve"
                                        required
                                        disabled={!!editPerm}
                                    />
                                </div>
                                <div className="form-group">
                                    <label className="form-label">Name</label>
                                    <input
                                        className="form-input"
                                        value={permForm.name}
                                        onChange={e => setPermForm({ ...permForm, name: e.target.value })}
                                        placeholder="Read Loans"
                                        required
                                    />
                                </div>
                                <div className="form-group">
                                    <label className="form-label">Description</label>
                                    <textarea
                                        className="form-input"
                                        value={permForm.description}
                                        onChange={e => setPermForm({ ...permForm, description: e.target.value })}
                                        rows={2}
                                    />
                                </div>
                            </div>
                            <div className="modal-footer">
                                <button type="button" className="btn btn-secondary" onClick={() => setShowPermModal(false)}>Cancel</button>
                                <button type="submit" className="btn btn-primary">{editPerm ? 'Update' : 'Create'}</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
