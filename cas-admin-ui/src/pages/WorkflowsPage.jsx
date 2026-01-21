import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { products, processTemplates } from '../api';
import Modal from '../components/common/Modal';

export default function WorkflowsPage() {
    const navigate = useNavigate();
    const [productList, setProductList] = useState([]);
    const [selectedProduct, setSelectedProduct] = useState(null);
    const [templates, setTemplates] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [showCreateModal, setShowCreateModal] = useState(false);
    const [newTemplate, setNewTemplate] = useState({ name: '', description: '' });

    // Load products
    useEffect(() => {
        loadProducts();
    }, []);

    // Load templates when product changes
    useEffect(() => {
        if (selectedProduct) {
            loadTemplates();
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

    const loadTemplates = async () => {
        if (!selectedProduct) return;
        try {
            setLoading(true);
            const data = await processTemplates.list(selectedProduct.id, false);
            setTemplates(data || []);
        } catch (err) {
            // Service might not be running yet
            console.warn('Workflow service not available:', err);
            setTemplates([]);
        } finally {
            setLoading(false);
        }
    };

    const handleCreateTemplate = async () => {
        if (!newTemplate.name.trim()) {
            setError('Name is required');
            return;
        }
        try {
            const template = await processTemplates.create({
                productId: selectedProduct.id,
                name: newTemplate.name,
                description: newTemplate.description
            });
            setShowCreateModal(false);
            setNewTemplate({ name: '', description: '' });
            navigate(`/workflows/${template.id}/design`);
        } catch (err) {
            setError(err.message);
        }
    };

    const handleDeploy = async (templateId) => {
        if (!confirm('Deploy this workflow? Once deployed, it cannot be edited.')) return;
        try {
            await processTemplates.deploy(templateId);
            loadTemplates();
        } catch (err) {
            setError(err.message);
        }
    };

    const handleDelete = async (templateId) => {
        if (!confirm('Delete this workflow template?')) return;
        try {
            await processTemplates.delete(templateId);
            loadTemplates();
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
                    <h1>Workflow Templates</h1>
                    <p className="page-subtitle">Design and manage BPMN workflows for your products</p>
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
                        + New Workflow
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
                            <th>Version</th>
                            <th>Status</th>
                            <th>Created</th>
                            <th style={{ width: '200px' }}>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {loading ? (
                            <tr><td colSpan="6" style={{ textAlign: 'center' }}>Loading...</td></tr>
                        ) : templates.length === 0 ? (
                            <tr>
                                <td colSpan="6" style={{ textAlign: 'center', padding: '48px' }}>
                                    <div style={{ color: 'var(--text-secondary)' }}>
                                        <div style={{ fontSize: '48px', marginBottom: '12px' }}>📋</div>
                                        <div>No workflow templates yet</div>
                                        <button
                                            className="btn btn-primary"
                                            style={{ marginTop: '16px' }}
                                            onClick={() => setShowCreateModal(true)}
                                        >
                                            Create your first workflow
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        ) : templates.map(template => (
                            <tr key={template.id}>
                                <td><strong>{template.name}</strong></td>
                                <td>{template.description || '-'}</td>
                                <td>v{template.version}</td>
                                <td>{getStatusBadge(template.status)}</td>
                                <td>{new Date(template.createdAt).toLocaleDateString()}</td>
                                <td>
                                    <div style={{ display: 'flex', gap: '8px' }}>
                                        <button
                                            className="btn btn-sm btn-secondary"
                                            onClick={() => navigate(`/workflows/${template.id}/design`)}
                                        >
                                            {template.status === 'DRAFT' ? '✏️ Edit' : '👁️ View'}
                                        </button>
                                        {template.status === 'DRAFT' && (
                                            <>
                                                <button
                                                    className="btn btn-sm btn-success"
                                                    onClick={() => handleDeploy(template.id)}
                                                >
                                                    🚀 Deploy
                                                </button>
                                                <button
                                                    className="btn btn-sm btn-danger"
                                                    onClick={() => handleDelete(template.id)}
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
                title="Create Workflow Template"
            >
                <div className="form-group">
                    <label>Workflow Name *</label>
                    <input
                        type="text"
                        className="form-control"
                        value={newTemplate.name}
                        onChange={(e) => setNewTemplate({ ...newTemplate, name: e.target.value })}
                        placeholder="e.g., Loan Approval Process"
                    />
                </div>
                <div className="form-group">
                    <label>Description</label>
                    <textarea
                        className="form-control"
                        rows={3}
                        value={newTemplate.description}
                        onChange={(e) => setNewTemplate({ ...newTemplate, description: e.target.value })}
                        placeholder="Describe the workflow purpose..."
                    />
                </div>
                <div className="modal-footer">
                    <button className="btn btn-secondary" onClick={() => setShowCreateModal(false)}>
                        Cancel
                    </button>
                    <button className="btn btn-primary" onClick={handleCreateTemplate}>
                        Create & Design
                    </button>
                </div>
            </Modal>
        </div>
    );
}
