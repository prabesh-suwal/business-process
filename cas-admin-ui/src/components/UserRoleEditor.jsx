import { useState, useEffect } from 'react'
import { users, roles, products } from '../api'

/**
 * UserRoleEditor - Manage user role assignments with ABAC constraints
 */
export default function UserRoleEditor({ userId, onClose }) {
    const [userRoles, setUserRoles] = useState([]);
    const [allProducts, setAllProducts] = useState([]);
    const [allRoles, setAllRoles] = useState({});
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [expandedProduct, setExpandedProduct] = useState(null);
    const [editingConstraints, setEditingConstraints] = useState(null);
    const [constraintForm, setConstraintForm] = useState({ branchIds: '', maxApprovalAmount: '' });

    useEffect(() => {
        loadData();
    }, [userId]);

    const loadData = async () => {
        setLoading(true);
        setError('');
        try {
            const [userRolesData, productsData] = await Promise.all([
                users.getRoles(userId),
                products.list()
            ]);
            setUserRoles(userRolesData);
            setAllProducts(productsData);

            // Load roles for each product
            const rolesByProduct = {};
            for (const product of productsData) {
                rolesByProduct[product.code] = await roles.list(product.code);
            }
            setAllRoles(rolesByProduct);

            if (productsData.length > 0) {
                setExpandedProduct(productsData[0].code);
            }
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    const handleAssignRole = async (roleId) => {
        setError('');
        try {
            await users.assignRole(userId, roleId, {});
            loadData();
        } catch (err) {
            setError(err.message);
        }
    };

    const handleRemoveRole = async (userRoleId) => {
        if (!confirm('Remove this role?')) return;
        setError('');
        try {
            await users.removeRole(userId, userRoleId);
            loadData();
        } catch (err) {
            setError(err.message);
        }
    };

    const openConstraintsEditor = (userRole) => {
        setEditingConstraints(userRole);
        const branchIds = userRole.constraints?.branchIds?.join(', ') || '';
        const maxApprovalAmount = userRole.constraints?.maxApprovalAmount || '';
        setConstraintForm({ branchIds, maxApprovalAmount });
    };

    const handleSaveConstraints = async () => {
        setError('');
        try {
            const constraints = {};
            if (constraintForm.branchIds.trim()) {
                constraints.branchIds = constraintForm.branchIds.split(',').map(b => b.trim()).filter(Boolean);
            }
            if (constraintForm.maxApprovalAmount) {
                constraints.maxApprovalAmount = Number(constraintForm.maxApprovalAmount);
            }
            await users.updateConstraints(userId, editingConstraints.id, constraints);
            setEditingConstraints(null);
            loadData();
        } catch (err) {
            setError(err.message);
        }
    };

    const getUserRoleForRole = (roleId) => userRoles.find(ur => ur.roleId === roleId);

    if (loading) return <div className="loading">Loading roles...</div>;

    return (
        <div className="user-role-editor">
            {error && <div className="form-error mb-4">{error}</div>}

            <div className="products-accordion">
                {allProducts.map(product => {
                    const productRoles = allRoles[product.code] || [];
                    const userProductRoles = userRoles.filter(ur => ur.productCode === product.code);
                    const isExpanded = expandedProduct === product.code;

                    return (
                        <div key={product.code} className="product-section">
                            <div
                                className="product-header"
                                onClick={() => setExpandedProduct(isExpanded ? null : product.code)}
                                style={{
                                    padding: '12px 16px',
                                    background: 'var(--bg-elevated)',
                                    borderRadius: '8px',
                                    cursor: 'pointer',
                                    display: 'flex',
                                    justifyContent: 'space-between',
                                    alignItems: 'center',
                                    marginBottom: '8px'
                                }}
                            >
                                <div>
                                    <strong>{product.name}</strong>
                                    <span className="text-muted" style={{ marginLeft: '8px' }}>
                                        ({userProductRoles.length} role{userProductRoles.length !== 1 ? 's' : ''})
                                    </span>
                                </div>
                                <span style={{ fontSize: '18px' }}>{isExpanded ? '▼' : '►'}</span>
                            </div>

                            {isExpanded && (
                                <div className="product-roles" style={{ padding: '0 16px 16px' }}>
                                    {productRoles.length === 0 ? (
                                        <div className="text-muted">No roles defined for this product</div>
                                    ) : (
                                        <div className="roles-grid" style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                                            {productRoles.map(role => {
                                                const userRole = getUserRoleForRole(role.id);
                                                const isAssigned = !!userRole;

                                                return (
                                                    <div
                                                        key={role.id}
                                                        className="role-card"
                                                        style={{
                                                            padding: '12px',
                                                            border: isAssigned ? '2px solid var(--primary)' : '1px solid var(--border)',
                                                            borderRadius: '8px',
                                                            background: isAssigned ? 'rgba(var(--primary-rgb), 0.05)' : 'var(--bg-card)'
                                                        }}
                                                    >
                                                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                                                            <div>
                                                                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                                                    <input
                                                                        type="checkbox"
                                                                        checked={isAssigned}
                                                                        onChange={() => isAssigned ? handleRemoveRole(userRole.id) : handleAssignRole(role.id)}
                                                                        style={{ width: '18px', height: '18px' }}
                                                                    />
                                                                    <strong>{role.name}</strong>
                                                                    <code style={{ fontSize: '12px', color: 'var(--text-muted)' }}>{role.code}</code>
                                                                </div>
                                                                {role.description && (
                                                                    <div className="text-muted" style={{ fontSize: '12px', marginTop: '4px', marginLeft: '26px' }}>
                                                                        {role.description}
                                                                    </div>
                                                                )}
                                                            </div>
                                                            {isAssigned && (
                                                                <button
                                                                    className="btn btn-sm btn-secondary"
                                                                    onClick={() => openConstraintsEditor(userRole)}
                                                                >
                                                                    Constraints
                                                                </button>
                                                            )}
                                                        </div>

                                                        {isAssigned && userRole.constraints && Object.keys(userRole.constraints).length > 0 && (
                                                            <div style={{ marginTop: '8px', marginLeft: '26px', display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                                                                {userRole.constraints.branchIds && (
                                                                    <span className="badge badge-info">
                                                                        Branches: {userRole.constraints.branchIds.join(', ')}
                                                                    </span>
                                                                )}
                                                                {userRole.constraints.maxApprovalAmount && (
                                                                    <span className="badge badge-warning">
                                                                        Max: {userRole.constraints.maxApprovalAmount.toLocaleString()}
                                                                    </span>
                                                                )}
                                                            </div>
                                                        )}
                                                    </div>
                                                );
                                            })}
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>
                    );
                })}
            </div>

            {/* Constraints Editor Modal */}
            {editingConstraints && (
                <div className="modal-overlay" onClick={() => setEditingConstraints(null)}>
                    <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: '450px' }}>
                        <div className="modal-header">
                            <h3>Edit Constraints - {editingConstraints.roleName}</h3>
                            <button className="btn btn-sm btn-secondary" onClick={() => setEditingConstraints(null)}>✕</button>
                        </div>
                        <div className="modal-body">
                            <div className="form-group">
                                <label className="form-label">Branch IDs</label>
                                <input
                                    className="form-input"
                                    placeholder="KTM-001, KTM-002, PKR-001"
                                    value={constraintForm.branchIds}
                                    onChange={e => setConstraintForm({ ...constraintForm, branchIds: e.target.value })}
                                />
                                <small className="text-muted">Comma-separated branch codes</small>
                            </div>
                            <div className="form-group">
                                <label className="form-label">Max Approval Amount</label>
                                <input
                                    type="number"
                                    className="form-input"
                                    placeholder="500000"
                                    value={constraintForm.maxApprovalAmount}
                                    onChange={e => setConstraintForm({ ...constraintForm, maxApprovalAmount: e.target.value })}
                                />
                                <small className="text-muted">Maximum amount this user can approve</small>
                            </div>
                        </div>
                        <div className="modal-footer">
                            <button className="btn btn-secondary" onClick={() => setEditingConstraints(null)}>Cancel</button>
                            <button className="btn btn-primary" onClick={handleSaveConstraints}>Save Constraints</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
