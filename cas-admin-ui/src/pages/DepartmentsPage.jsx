import { useState, useEffect } from 'react'
import { departments, branches } from '../api'

export default function DepartmentsPage() {
    const [deptList, setDeptList] = useState([]);
    const [branchList, setBranchList] = useState([]);
    const [loading, setLoading] = useState(true);
    const [showModal, setShowModal] = useState(false);
    const [editDept, setEditDept] = useState(null);
    const [form, setForm] = useState({
        code: '', name: '', description: '', parentId: '', branchId: ''
    });

    useEffect(() => { loadData(); }, []);

    const loadData = async () => {
        try {
            const [deptData, branchData] = await Promise.all([
                departments.list(),
                branches.list()
            ]);
            setDeptList(deptData);
            setBranchList(branchData);
        } catch (err) {
            console.error('Failed to load:', err);
        } finally {
            setLoading(false);
        }
    };

    const openCreate = () => {
        setEditDept(null);
        setForm({ code: '', name: '', description: '', parentId: '', branchId: '' });
        setShowModal(true);
    };

    const openEdit = (dept) => {
        setEditDept(dept);
        setForm({
            code: dept.code,
            name: dept.name,
            description: dept.description || '',
            parentId: dept.parentId || '',
            branchId: dept.branchId || ''
        });
        setShowModal(true);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            if (editDept) {
                await departments.update(editDept.id, form);
            } else {
                await departments.create(form);
            }
            setShowModal(false);
            loadData();
        } catch (err) {
            alert('Error: ' + err.message);
        }
    };

    // Group departments by parent for tree view
    const rootDepts = deptList.filter(d => !d.parentId);
    const childDepts = (parentId) => deptList.filter(d => d.parentId === parentId);

    const DeptRow = ({ dept, level = 0 }) => (
        <>
            <tr key={dept.id}>
                <td style={{ paddingLeft: `${level * 24 + 12}px` }}>
                    {level > 0 && <span className="text-muted">└─ </span>}
                    <strong>{dept.name}</strong>
                </td>
                <td><code>{dept.code}</code></td>
                <td>{dept.branchName || <span className="text-muted">Organization-wide</span>}</td>
                <td>
                    <span className={`badge ${dept.status === 'ACTIVE' ? 'badge-success' : 'badge-secondary'}`}>
                        {dept.status}
                    </span>
                </td>
                <td>
                    <button className="btn btn-sm btn-secondary" onClick={() => openEdit(dept)}>Edit</button>
                </td>
            </tr>
            {childDepts(dept.id).map(child => (
                <DeptRow key={child.id} dept={child} level={level + 1} />
            ))}
        </>
    );

    if (loading) return <div className="loading">Loading departments...</div>;

    return (
        <div className="page">
            <div className="page-header">
                <h1>Departments</h1>
                <button className="btn btn-primary" onClick={openCreate}>+ Add Department</button>
            </div>

            <div className="card">
                <table className="table">
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th>Code</th>
                            <th>Branch</th>
                            <th>Status</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {rootDepts.map(dept => <DeptRow key={dept.id} dept={dept} />)}
                    </tbody>
                </table>
            </div>

            {showModal && (
                <div className="modal-overlay" onClick={() => setShowModal(false)}>
                    <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: '500px' }}>
                        <div className="modal-header">
                            <h3>{editDept ? 'Edit Department' : 'Create Department'}</h3>
                            <button className="btn btn-sm btn-secondary" onClick={() => setShowModal(false)}>✕</button>
                        </div>
                        <form onSubmit={handleSubmit}>
                            <div className="modal-body">
                                <div className="form-group">
                                    <label className="form-label">Code *</label>
                                    <input className="form-input" value={form.code} disabled={!!editDept}
                                        onChange={e => setForm({ ...form, code: e.target.value })} required />
                                </div>
                                <div className="form-group">
                                    <label className="form-label">Name *</label>
                                    <input className="form-input" value={form.name}
                                        onChange={e => setForm({ ...form, name: e.target.value })} required />
                                </div>
                                <div className="form-group">
                                    <label className="form-label">Description</label>
                                    <textarea className="form-input" value={form.description} rows={2}
                                        onChange={e => setForm({ ...form, description: e.target.value })} />
                                </div>
                                <div className="form-group">
                                    <label className="form-label">Parent Department</label>
                                    <select className="form-select" value={form.parentId}
                                        onChange={e => setForm({ ...form, parentId: e.target.value })}>
                                        <option value="">-- None (Root) --</option>
                                        {deptList.filter(d => d.id !== editDept?.id).map(d => (
                                            <option key={d.id} value={d.id}>{d.name}</option>
                                        ))}
                                    </select>
                                </div>
                                <div className="form-group">
                                    <label className="form-label">Branch Scope</label>
                                    <select className="form-select" value={form.branchId}
                                        onChange={e => setForm({ ...form, branchId: e.target.value })}>
                                        <option value="">Organization-wide</option>
                                        {branchList.map(b => (
                                            <option key={b.id} value={b.id}>{b.name}</option>
                                        ))}
                                    </select>
                                </div>
                            </div>
                            <div className="modal-footer">
                                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Cancel</button>
                                <button type="submit" className="btn btn-primary">{editDept ? 'Update' : 'Create'}</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
