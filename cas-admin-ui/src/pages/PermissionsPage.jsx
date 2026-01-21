import { useState, useEffect, useMemo } from 'react';
import { permissions, products } from '../api';
import DataTable from '../components/common/DataTable';
import Pagination from '../components/common/Pagination';
import SearchInput from '../components/common/SearchInput';
import FilterSelect from '../components/common/FilterSelect';
import Badge from '../components/common/Badge';

export default function PermissionsPage() {
    // Data State
    const [permissionsList, setPermissionsList] = useState([]);
    const [productsList, setProductsList] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    // Filter State
    const [searchTerm, setSearchTerm] = useState('');
    const [categoryFilter, setCategoryFilter] = useState('');
    const [productFilter, setProductFilter] = useState('');
    const [currentPage, setCurrentPage] = useState(0);
    const itemsPerPage = 10;

    // Modal State
    const [showModal, setShowModal] = useState(false);
    const [editingPermission, setEditingPermission] = useState(null);
    const [form, setForm] = useState({ code: '', name: '', description: '', category: '', productCodes: [] });

    // Constants
    const CATEGORY_OPTIONS = [
        { value: 'READ', label: 'Read' },
        { value: 'WRITE', label: 'Write' },
        { value: 'DELETE', label: 'Delete' },
        { value: 'ADMIN', label: 'Admin' }
    ];

    useEffect(() => {
        loadData();
    }, []);

    const loadData = async () => {
        try {
            setLoading(true);
            const [permsData, prodsData] = await Promise.all([
                permissions.list(),
                products.list()
            ]);
            setPermissionsList(permsData);
            setProductsList(prodsData);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    // Derived Data: Filtering & Pagination
    const filteredData = useMemo(() => {
        return permissionsList.filter(perm => {
            const matchesSearch =
                perm.code.toLowerCase().includes(searchTerm.toLowerCase()) ||
                perm.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
                (perm.description && perm.description.toLowerCase().includes(searchTerm.toLowerCase()));

            const matchesCategory = categoryFilter ? perm.category === categoryFilter : true;

            const matchesProduct = productFilter
                ? (perm.productCodes && perm.productCodes.includes(productFilter))
                : true;

            return matchesSearch && matchesCategory && matchesProduct;
        });
    }, [permissionsList, searchTerm, categoryFilter, productFilter]);

    const paginatedData = useMemo(() => {
        const start = currentPage * itemsPerPage;
        return filteredData.slice(start, start + itemsPerPage);
    }, [filteredData, currentPage]);

    const totalPages = Math.ceil(filteredData.length / itemsPerPage);

    // Handlers
    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            if (editingPermission) {
                await permissions.update(editingPermission.id, {
                    name: form.name,
                    description: form.description,
                    category: form.category
                });
                // Note: Updating products for existing permission needs separate API call if we want to support it here
                // For now, let's assume update endpoint only handles name/desc/category as per backend

                // If we want to support product assignment on edit, we'd need to compare and call assign/remove
                // For MVP, just update basic info. Use the standalone assign buttons or improve this later.
                // Actually user might expect it. Let's start with basic update.
            } else {
                await permissions.create(form);
            }
            setShowModal(false);
            setEditingPermission(null);
            resetForm();
            loadData();
        } catch (err) {
            setError(err.message);
        }
    };

    const resetForm = () => {
        setForm({ code: '', name: '', description: '', category: '', productCodes: [] });
    };

    const handleEdit = (perm) => {
        setEditingPermission(perm);
        setForm({
            code: perm.code,
            name: perm.name,
            description: perm.description || '',
            category: perm.category || '',
            productCodes: perm.productCodes || []
        });
        setShowModal(true);
    };

    const handleDelete = async (id, e) => {
        e.stopPropagation(); // Prevent row click
        if (confirm('Delete this permission?')) {
            try {
                await permissions.delete(id);
                loadData();
            } catch (err) {
                setError(err.message);
            }
        }
    };

    const toggleProduct = (productCode) => {
        setForm(prev => ({
            ...prev,
            productCodes: prev.productCodes.includes(productCode)
                ? prev.productCodes.filter(c => c !== productCode)
                : [...prev.productCodes, productCode]
        }));
    };

    // Columns Configuration
    const columns = [
        {
            header: 'Code',
            accessor: 'code',
            render: (row) => <code>{row.code}</code>,
            width: '20%'
        },
        {
            header: 'Name',
            accessor: 'name',
            width: '20%'
        },
        {
            header: 'Category',
            accessor: 'category',
            render: (row) => <Badge type={row.category}>{row.category || 'N/A'}</Badge>,
            width: '15%'
        },
        {
            header: 'Products',
            accessor: 'productCodes',
            render: (row) => (
                <div className="flex gap-2" style={{ flexWrap: 'wrap' }}>
                    {(row.productCodes || []).map(code => (
                        <Badge key={code} type="info">{code}</Badge>
                    ))}
                    {(!row.productCodes || row.productCodes.length === 0) &&
                        <span className="text-muted" style={{ fontSize: '12px' }}>Global / None</span>}
                </div>
            ),
            width: '30%'
        },
        {
            header: 'Actions',
            render: (row) => (
                <div className="flex gap-2">
                    <button className="btn-sm" onClick={(e) => { e.stopPropagation(); handleEdit(row); }}>Edit</button>
                    <button className="btn-sm btn-danger" onClick={(e) => handleDelete(row.id, e)}>Delete</button>
                </div>
            ),
            width: '15%'
        }
    ];

    if (loading && permissionsList.length === 0) return <div className="loading">Loading...</div>;

    return (
        <div className="page-container">
            <div className="page-header">
                <h1>Permissions</h1>
                <button className="btn-primary" onClick={() => {
                    setEditingPermission(null);
                    resetForm();
                    setShowModal(true);
                }}>
                    + New Permission
                </button>
            </div>

            {error && <div className="error-banner">{error}</div>}

            <div className="filter-bar">
                <SearchInput
                    value={searchTerm}
                    onChange={(val) => { setSearchTerm(val); setCurrentPage(0); }}
                    placeholder="Search permissions..."
                />
                <FilterSelect
                    label="Category:"
                    value={categoryFilter}
                    onChange={(val) => { setCategoryFilter(val); setCurrentPage(0); }}
                    options={CATEGORY_OPTIONS}
                />
                <FilterSelect
                    label="Product:"
                    value={productFilter}
                    onChange={(val) => { setProductFilter(val); setCurrentPage(0); }}
                    options={productsList.map(p => ({ value: p.code, label: p.name }))}
                    placeholder="All Products"
                />
            </div>

            <DataTable
                columns={columns}
                data={paginatedData}
                keyField="id"
                emptyMessage="No permissions found matching your filters."
                onRowClick={(row) => handleEdit(row)}
            />

            <Pagination
                currentPage={currentPage}
                totalPages={totalPages}
                totalItems={filteredData.length}
                itemsPerPage={itemsPerPage}
                onPageChange={setCurrentPage}
            />

            {showModal && (
                <div className="modal-overlay" onClick={() => setShowModal(false)}>
                    <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: '600px' }}>
                        <div className="modal-header">
                            <h2>{editingPermission ? 'Edit Permission' : 'New Permission'}</h2>
                            <button className="btn-icon" onClick={() => setShowModal(false)}>✕</button>
                        </div>
                        <form onSubmit={handleSubmit}>
                            <div className="modal-body">
                                <div className="form-group">
                                    <label className="form-label">Permission Code</label>
                                    <input
                                        type="text"
                                        className="form-input"
                                        value={form.code}
                                        onChange={e => setForm({ ...form, code: e.target.value })}
                                        disabled={!!editingPermission}
                                        required
                                        placeholder="e.g., user:read"
                                    />
                                    <p className="form-help text-muted" style={{ fontSize: '12px', marginTop: '4px' }}>
                                        Unique identifier for the permission.
                                    </p>
                                </div>

                                <div className="grid-row" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                                    <div className="form-group">
                                        <label className="form-label">Name</label>
                                        <input
                                            type="text"
                                            className="form-input"
                                            value={form.name}
                                            onChange={e => setForm({ ...form, name: e.target.value })}
                                            required
                                            placeholder="e.g., Read Users"
                                        />
                                    </div>
                                    <div className="form-group">
                                        <label className="form-label">Category</label>
                                        <select
                                            className="form-select"
                                            value={form.category}
                                            onChange={e => setForm({ ...form, category: e.target.value })}
                                        >
                                            <option value="">Select category</option>
                                            {CATEGORY_OPTIONS.map(opt => (
                                                <option key={opt.value} value={opt.value}>{opt.label}</option>
                                            ))}
                                        </select>
                                    </div>
                                </div>

                                <div className="form-group">
                                    <label className="form-label">Description</label>
                                    <textarea
                                        className="form-input"
                                        value={form.description}
                                        onChange={e => setForm({ ...form, description: e.target.value })}
                                        rows={2}
                                        placeholder="Optional description"
                                    />
                                </div>

                                {/* Product Assignment - Only enable for create for now, or if editing make it clear it might need backend logic adjustment */}
                                {!editingPermission && (
                                    <div className="form-group">
                                        <label className="form-label">Assign to Products</label>
                                        <div className="chip-container">
                                            {productsList.map(prod => {
                                                const isSelected = form.productCodes.includes(prod.code);
                                                return (
                                                    <div
                                                        key={prod.code}
                                                        className={`chip chip-selectable ${isSelected ? 'selected' : ''}`}
                                                        onClick={() => toggleProduct(prod.code)}
                                                    >
                                                        {prod.name}
                                                        {isSelected && <span style={{ marginLeft: '4px' }}>✓</span>}
                                                    </div>
                                                );
                                            })}
                                        </div>
                                    </div>
                                )}
                                {editingPermission && (
                                    <div className="alert alert-info" style={{ background: '#eff6ff', padding: '12px', borderRadius: '4px', fontSize: '13px', color: '#1e40af' }}>
                                        To manage product assignments, use the "Assign Products" action in the list view (Coming Soon).
                                    </div>
                                )}
                            </div>
                            <div className="modal-footer">
                                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>
                                    Cancel
                                </button>
                                <button type="submit" className="btn btn-primary">
                                    {editingPermission ? 'Save Changes' : 'Create'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
