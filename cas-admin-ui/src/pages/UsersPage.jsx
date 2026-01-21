import { useState, useEffect } from 'react'
import { users, branches, departments } from '../api'
import UserRoleEditor from '../components/UserRoleEditor'

export default function UsersPage() {
    const [data, setData] = useState({ content: [], totalElements: 0 });
    const [page, setPage] = useState(0);
    const [search, setSearch] = useState('');
    const [loading, setLoading] = useState(true);
    const [showModal, setShowModal] = useState(false);
    const [editUser, setEditUser] = useState(null);
    const [manageRolesUser, setManageRolesUser] = useState(null);
    const [form, setForm] = useState({ username: '', email: '', password: '', firstName: '', lastName: '', branchId: '', departmentId: '' });
    const [error, setError] = useState('');

    // Organization data
    const [branchList, setBranchList] = useState([]);
    const [deptList, setDeptList] = useState([]);

    const loadUsers = async () => {
        setLoading(true);
        try {
            const result = await users.list(page, 20, search);
            setData(result);
        } catch (err) {
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const loadOrgData = async () => {
        try {
            const [branchData, deptData] = await Promise.all([
                branches.list(),
                departments.list()
            ]);
            setBranchList(branchData || []);
            setDeptList(deptData || []);
        } catch (err) {
            console.error('Could not load org data:', err);
        }
    };

    useEffect(() => { loadUsers(); }, [page, search]);
    useEffect(() => { loadOrgData(); }, []);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        try {
            const submitData = {
                ...form,
                branchId: form.branchId || null,
                departmentId: form.departmentId || null
            };
            if (editUser) {
                await users.update(editUser.id, submitData);
            } else {
                await users.create(submitData);
            }
            setShowModal(false);
            setEditUser(null);
            setForm({ username: '', email: '', password: '', firstName: '', lastName: '', branchId: '', departmentId: '' });
            loadUsers();
        } catch (err) {
            setError(err.message);
        }
    };

    const handleEdit = (user) => {
        setEditUser(user);
        setForm({
            username: user.username,
            email: user.email,
            firstName: user.firstName || '',
            lastName: user.lastName || '',
            branchId: user.branchId || '',
            departmentId: user.departmentId || ''
        });
        setShowModal(true);
    };

    const handleDelete = async (id) => {
        if (confirm('Are you sure you want to deactivate this user?')) {
            await users.delete(id);
            loadUsers();
        }
    };

    const openCreate = () => {
        setEditUser(null);
        setForm({ username: '', email: '', password: '', firstName: '', lastName: '', branchId: '', departmentId: '' });
        setShowModal(true);
    };

    // Helper to get branch/dept name
    const getBranchName = (id) => branchList.find(b => b.id === id)?.name || '-';
    const getDeptName = (id) => deptList.find(d => d.id === id)?.name || '-';

    return (
        <div>
            <div className="page-header">
                <h1>Users</h1>
                <button className="btn btn-primary" onClick={openCreate}>+ Add User</button>
            </div>

            <div className="search-box">
                <input
                    type="text"
                    className="form-input search-input"
                    placeholder="Search users..."
                    value={search}
                    onChange={(e) => { setSearch(e.target.value); setPage(0); }}
                />
            </div>

            <div className="card">
                {loading ? (
                    <div className="loading">Loading...</div>
                ) : (
                    <>
                        <div className="table-container">
                            <table>
                                <thead>
                                    <tr>
                                        <th>Username</th>
                                        <th>Email</th>
                                        <th>Name</th>
                                        <th>Branch</th>
                                        <th>Department</th>
                                        <th>Status</th>
                                        <th>Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {data.content.map((user) => (
                                        <tr key={user.id}>
                                            <td>{user.username}</td>
                                            <td>{user.email}</td>
                                            <td>{user.firstName} {user.lastName}</td>
                                            <td><span className="text-muted">{getBranchName(user.branchId)}</span></td>
                                            <td><span className="text-muted">{getDeptName(user.departmentId)}</span></td>
                                            <td>
                                                <span className={`badge ${user.status === 'ACTIVE' ? 'badge-success' : 'badge-warning'}`}>
                                                    {user.status}
                                                </span>
                                            </td>
                                            <td>
                                                <div className="flex gap-2">
                                                    <button className="btn btn-sm btn-primary" onClick={() => setManageRolesUser(user)}>Roles</button>
                                                    <button className="btn btn-sm btn-secondary" onClick={() => handleEdit(user)}>Edit</button>
                                                    <button className="btn btn-sm btn-danger" onClick={() => handleDelete(user.id)}>Delete</button>
                                                </div>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>

                        <div className="pagination">
                            <span>Showing {data.content.length} of {data.totalElements} users</span>
                            <div className="pagination-buttons">
                                <button className="btn btn-sm btn-secondary" disabled={page === 0} onClick={() => setPage(p => p - 1)}>Previous</button>
                                <button className="btn btn-sm btn-secondary" disabled={data.last} onClick={() => setPage(p => p + 1)}>Next</button>
                            </div>
                        </div>
                    </>
                )}
            </div>

            {showModal && (
                <div className="modal-overlay" onClick={() => setShowModal(false)}>
                    <div className="modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '600px' }}>
                        <div className="modal-header">
                            <h3>{editUser ? 'Edit User' : 'Create User'}</h3>
                            <button className="btn btn-sm btn-secondary" onClick={() => setShowModal(false)}>✕</button>
                        </div>
                        <form onSubmit={handleSubmit}>
                            <div className="modal-body">
                                {error && <div className="form-error mb-4">{error}</div>}

                                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                                    <div className="form-group">
                                        <label className="form-label">Username *</label>
                                        <input
                                            className="form-input"
                                            value={form.username}
                                            onChange={(e) => setForm({ ...form, username: e.target.value })}
                                            required
                                            disabled={!!editUser}
                                        />
                                    </div>

                                    <div className="form-group">
                                        <label className="form-label">Email *</label>
                                        <input
                                            type="email"
                                            className="form-input"
                                            value={form.email}
                                            onChange={(e) => setForm({ ...form, email: e.target.value })}
                                            required
                                        />
                                    </div>
                                </div>

                                {!editUser && (
                                    <div className="form-group">
                                        <label className="form-label">Password *</label>
                                        <input
                                            type="password"
                                            className="form-input"
                                            value={form.password}
                                            onChange={(e) => setForm({ ...form, password: e.target.value })}
                                            required
                                        />
                                    </div>
                                )}

                                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                                    <div className="form-group">
                                        <label className="form-label">First Name</label>
                                        <input
                                            className="form-input"
                                            value={form.firstName}
                                            onChange={(e) => setForm({ ...form, firstName: e.target.value })}
                                        />
                                    </div>

                                    <div className="form-group">
                                        <label className="form-label">Last Name</label>
                                        <input
                                            className="form-input"
                                            value={form.lastName}
                                            onChange={(e) => setForm({ ...form, lastName: e.target.value })}
                                        />
                                    </div>
                                </div>

                                <div style={{ borderTop: '1px solid var(--border)', paddingTop: '16px', marginTop: '8px' }}>
                                    <h4 style={{ fontSize: '14px', marginBottom: '12px', color: 'var(--text-secondary)' }}>🏢 Organization Assignment</h4>
                                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                                        <div className="form-group">
                                            <label className="form-label">Branch</label>
                                            <select
                                                className="form-select"
                                                value={form.branchId}
                                                onChange={(e) => setForm({ ...form, branchId: e.target.value })}
                                            >
                                                <option value="">-- Not Assigned --</option>
                                                {branchList.map(b => (
                                                    <option key={b.id} value={b.id}>{b.name}</option>
                                                ))}
                                            </select>
                                        </div>

                                        <div className="form-group">
                                            <label className="form-label">Department</label>
                                            <select
                                                className="form-select"
                                                value={form.departmentId}
                                                onChange={(e) => setForm({ ...form, departmentId: e.target.value })}
                                            >
                                                <option value="">-- Not Assigned --</option>
                                                {deptList.map(d => (
                                                    <option key={d.id} value={d.id}>{d.name}</option>
                                                ))}
                                            </select>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div className="modal-footer">
                                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Cancel</button>
                                <button type="submit" className="btn btn-primary">{editUser ? 'Update' : 'Create'}</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* Role Management Modal */}
            {manageRolesUser && (
                <div className="modal-overlay" onClick={() => setManageRolesUser(null)}>
                    <div className="modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '700px' }}>
                        <div className="modal-header">
                            <h3>Manage Roles - {manageRolesUser.firstName || manageRolesUser.username}</h3>
                            <button className="btn btn-sm btn-secondary" onClick={() => setManageRolesUser(null)}>✕</button>
                        </div>
                        <div className="modal-body" style={{ maxHeight: '60vh', overflowY: 'auto' }}>
                            <UserRoleEditor
                                userId={manageRolesUser.id}
                                onClose={() => setManageRolesUser(null)}
                            />
                        </div>
                        <div className="modal-footer">
                            <button className="btn btn-secondary" onClick={() => setManageRolesUser(null)}>Close</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

