import { useState, useEffect } from 'react'
import { groups, departments, branches, users } from '../api'

export default function GroupsPage() {
    const [groupList, setGroupList] = useState([]);
    const [deptList, setDeptList] = useState([]);
    const [branchList, setBranchList] = useState([]);
    const [loading, setLoading] = useState(true);
    const [showModal, setShowModal] = useState(false);
    const [showMembersModal, setShowMembersModal] = useState(false);
    const [editGroup, setEditGroup] = useState(null);
    const [selectedGroup, setSelectedGroup] = useState(null);
    const [form, setForm] = useState({
        code: '', name: '', description: '', groupType: 'TEAM', branchId: '', departmentId: ''
    });

    useEffect(() => { loadData(); }, []);

    const loadData = async () => {
        try {
            const [groupData, deptData, branchData] = await Promise.all([
                groups.list(),
                departments.list(),
                branches.list()
            ]);
            setGroupList(groupData);
            setDeptList(deptData);
            setBranchList(branchData);
        } catch (err) {
            console.error('Failed to load:', err);
        } finally {
            setLoading(false);
        }
    };

    const openCreate = () => {
        setEditGroup(null);
        setForm({ code: '', name: '', description: '', groupType: 'TEAM', branchId: '', departmentId: '' });
        setShowModal(true);
    };

    const openEdit = (group) => {
        setEditGroup(group);
        setForm({
            code: group.code,
            name: group.name,
            description: group.description || '',
            groupType: group.groupType,
            branchId: group.branchId || '',
            departmentId: group.departmentId || ''
        });
        setShowModal(true);
    };

    const openMembers = (group) => {
        setSelectedGroup(group);
        setShowMembersModal(true);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            if (editGroup) {
                await groups.update(editGroup.id, form);
            } else {
                await groups.create(form);
            }
            setShowModal(false);
            loadData();
        } catch (err) {
            alert('Error: ' + err.message);
        }
    };

    const handleRemoveMember = async (userId) => {
        if (!confirm('Remove this member?')) return;
        await groups.removeMember(selectedGroup.id, userId);
        const updated = await groups.get(selectedGroup.id);
        setSelectedGroup(updated);
        loadData();
    };

    const groupTypeLabel = {
        TEAM: '👥 Team',
        COMMITTEE: '📋 Committee',
        WORKING_GROUP: '🔧 Working Group',
        TASK_FORCE: '⚡ Task Force'
    };

    if (loading) return <div className="loading">Loading groups...</div>;

    return (
        <div className="page">
            <div className="page-header">
                <h1>Groups & Committees</h1>
                <button className="btn btn-primary" onClick={openCreate}>+ Create Group</button>
            </div>

            <div className="card">
                <table className="table">
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th>Type</th>
                            <th>Scope</th>
                            <th>Members</th>
                            <th>Status</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {groupList.map(group => (
                            <tr key={group.id}>
                                <td>
                                    <strong>{group.name}</strong>
                                    <div className="text-muted" style={{ fontSize: '12px' }}>{group.code}</div>
                                </td>
                                <td>{groupTypeLabel[group.groupType] || group.groupType}</td>
                                <td>
                                    {group.departmentName && <div>🏢 {group.departmentName}</div>}
                                    {group.branchName && <div>📍 {group.branchName}</div>}
                                    {!group.departmentName && !group.branchName && <span className="text-muted">Organization-wide</span>}
                                </td>
                                <td>
                                    <button className="btn btn-sm btn-secondary" onClick={() => openMembers(group)}>
                                        {group.members?.length || 0} members
                                    </button>
                                </td>
                                <td>
                                    <span className={`badge ${group.status === 'ACTIVE' ? 'badge-success' : 'badge-secondary'}`}>
                                        {group.status}
                                    </span>
                                </td>
                                <td>
                                    <button className="btn btn-sm btn-secondary" onClick={() => openEdit(group)}>Edit</button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            {/* Create/Edit Modal */}
            {showModal && (
                <div className="modal-overlay" onClick={() => setShowModal(false)}>
                    <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: '500px' }}>
                        <div className="modal-header">
                            <h3>{editGroup ? 'Edit Group' : 'Create Group'}</h3>
                            <button className="btn btn-sm btn-secondary" onClick={() => setShowModal(false)}>✕</button>
                        </div>
                        <form onSubmit={handleSubmit}>
                            <div className="modal-body">
                                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                                    <div className="form-group">
                                        <label className="form-label">Code *</label>
                                        <input className="form-input" value={form.code} disabled={!!editGroup}
                                            onChange={e => setForm({ ...form, code: e.target.value })} required />
                                    </div>
                                    <div className="form-group">
                                        <label className="form-label">Type</label>
                                        <select className="form-select" value={form.groupType}
                                            onChange={e => setForm({ ...form, groupType: e.target.value })}>
                                            <option value="TEAM">Team</option>
                                            <option value="COMMITTEE">Committee</option>
                                            <option value="WORKING_GROUP">Working Group</option>
                                            <option value="TASK_FORCE">Task Force</option>
                                        </select>
                                    </div>
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
                                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                                    <div className="form-group">
                                        <label className="form-label">Branch</label>
                                        <select className="form-select" value={form.branchId}
                                            onChange={e => setForm({ ...form, branchId: e.target.value })}>
                                            <option value="">-- Any --</option>
                                            {branchList.map(b => <option key={b.id} value={b.id}>{b.name}</option>)}
                                        </select>
                                    </div>
                                    <div className="form-group">
                                        <label className="form-label">Department</label>
                                        <select className="form-select" value={form.departmentId}
                                            onChange={e => setForm({ ...form, departmentId: e.target.value })}>
                                            <option value="">-- Any --</option>
                                            {deptList.map(d => <option key={d.id} value={d.id}>{d.name}</option>)}
                                        </select>
                                    </div>
                                </div>
                            </div>
                            <div className="modal-footer">
                                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Cancel</button>
                                <button type="submit" className="btn btn-primary">{editGroup ? 'Update' : 'Create'}</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* Members Modal */}
            {showMembersModal && selectedGroup && (
                <div className="modal-overlay" onClick={() => setShowMembersModal(false)}>
                    <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: '500px' }}>
                        <div className="modal-header">
                            <h3>{selectedGroup.name} - Members</h3>
                            <button className="btn btn-sm btn-secondary" onClick={() => setShowMembersModal(false)}>✕</button>
                        </div>
                        <div className="modal-body">
                            {selectedGroup.members?.length === 0 ? (
                                <div className="text-muted text-center" style={{ padding: '32px' }}>
                                    No members yet. Add members from user management.
                                </div>
                            ) : (
                                <table className="table">
                                    <thead>
                                        <tr>
                                            <th>User ID</th>
                                            <th>Role</th>
                                            <th></th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {selectedGroup.members?.map(m => (
                                            <tr key={m.id}>
                                                <td><code>{m.userId}</code></td>
                                                <td>
                                                    <span className={`badge ${m.memberRole === 'LEADER' ? 'badge-primary' : 'badge-secondary'}`}>
                                                        {m.memberRole}
                                                    </span>
                                                </td>
                                                <td>
                                                    <button className="btn btn-sm btn-danger" onClick={() => handleRemoveMember(m.userId)}>
                                                        Remove
                                                    </button>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            )}
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
