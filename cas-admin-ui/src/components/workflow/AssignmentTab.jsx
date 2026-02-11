import { useState, useEffect, useCallback } from 'react';
import { roles as rolesApi, groups as groupsApi, branches as branchesApi, departments as departmentsApi, users as usersApi } from '../../api';

const ASSIGNMENT_TYPES = [
    { value: 'ROLE_BASED', label: 'Role-Based', group: 'Standard', desc: 'Assign to users with a specific role' },
    { value: 'GROUP', label: 'Group / Committee', group: 'Standard', desc: 'Assign to a user group' },
    { value: 'BRANCH', label: 'Branch-Based', group: 'Organization', desc: 'Assign based on branch' },
    { value: 'DEPARTMENT', label: 'Department-Based', group: 'Organization', desc: 'Assign based on department' },
    { value: 'SPECIFIC_PERSON', label: 'Specific Person', group: 'Direct', desc: 'Assign to a specific user' },
    { value: 'HIERARCHY', label: 'Hierarchy', group: 'Advanced', desc: 'Route based on organizational hierarchy' },
    { value: 'ROUND_ROBIN', label: 'Round Robin', group: 'Advanced', desc: 'Distribute evenly across eligible users' }
];

/**
 * Assignment tab — shown only for User Tasks.
 * Configures who gets assigned to this workflow step.
 */
export default function AssignmentTab({
    element,
    topicId,
    stepConfig,
    onConfigChange,
    onDirty,
    onSaved
}) {
    const taskKey = element?.id || '';

    // Assignment config state
    const [assignmentType, setAssignmentType] = useState('');
    const [roleId, setRoleId] = useState('');
    const [groupId, setGroupId] = useState('');
    const [branchId, setBranchId] = useState('');
    const [departmentId, setDepartmentId] = useState('');
    const [userId, setUserId] = useState('');
    const [saving, setSaving] = useState(false);

    // Reference data
    const [rolesList, setRolesList] = useState([]);
    const [groupsList, setGroupsList] = useState([]);
    const [branchesList, setBranchesList] = useState([]);
    const [departmentsList, setDepartmentsList] = useState([]);
    const [usersList, setUsersList] = useState([]);
    const [loadingRef, setLoadingRef] = useState(false);

    // Load existing config
    useEffect(() => {
        if (stepConfig?.assignmentConfig) {
            const config = stepConfig.assignmentConfig;
            setAssignmentType(config.type || '');
            setRoleId(config.roleId || '');
            setGroupId(config.groupId || '');
            setBranchId(config.branchId || '');
            setDepartmentId(config.departmentId || '');
            setUserId(config.userId || '');
        } else {
            setAssignmentType('');
            setRoleId('');
            setGroupId('');
            setBranchId('');
            setDepartmentId('');
            setUserId('');
        }
    }, [stepConfig]);

    // Load reference data
    useEffect(() => {
        let cancelled = false;
        setLoadingRef(true);

        const loadData = async () => {
            try {
                const [rolesRes, groupsRes, branchesRes, deptsRes, usersRes] = await Promise.allSettled([
                    rolesApi.list(),
                    groupsApi.list(),
                    branchesApi.list(),
                    departmentsApi.list(),
                    usersApi.list(0, 100)
                ]);

                if (cancelled) return;

                if (rolesRes.status === 'fulfilled') setRolesList(rolesRes.value || []);
                if (groupsRes.status === 'fulfilled') setGroupsList(groupsRes.value || []);
                if (branchesRes.status === 'fulfilled') setBranchesList(branchesRes.value || []);
                if (deptsRes.status === 'fulfilled') setDepartmentsList(deptsRes.value || []);
                if (usersRes.status === 'fulfilled') setUsersList(usersRes.value?.content || usersRes.value || []);
            } catch (err) {
                console.error('Error loading reference data:', err);
            } finally {
                if (!cancelled) setLoadingRef(false);
            }
        };

        loadData();
        return () => { cancelled = true; };
    }, []);

    const handleSave = useCallback(async () => {
        if (!topicId || !taskKey) return;

        const config = { type: assignmentType };
        if (assignmentType === 'ROLE_BASED') config.roleId = roleId;
        if (assignmentType === 'GROUP') config.groupId = groupId;
        if (assignmentType === 'BRANCH') config.branchId = branchId;
        if (assignmentType === 'DEPARTMENT') config.departmentId = departmentId;
        if (assignmentType === 'SPECIFIC_PERSON') config.userId = userId;

        setSaving(true);
        try {
            await onConfigChange?.('step', taskKey, { assignmentConfig: config });
            onSaved?.();
        } catch (err) {
            console.error('Error saving assignment config:', err);
        } finally {
            setSaving(false);
        }
    }, [topicId, taskKey, assignmentType, roleId, groupId, branchId, departmentId, userId, onConfigChange, onSaved]);

    // Group the types
    const groups = {};
    ASSIGNMENT_TYPES.forEach(t => {
        if (!groups[t.group]) groups[t.group] = [];
        groups[t.group].push(t);
    });

    return (
        <div>
            {/* Assignment Type */}
            <div className="panel-section">
                <label className="panel-section__label">Assignment Type</label>
                {Object.entries(groups).map(([groupName, types]) => (
                    <div key={groupName} className="assignment-group">
                        <div className="assignment-group__label">{groupName}</div>
                        {types.map(t => (
                            <label
                                key={t.value}
                                style={{
                                    display: 'flex',
                                    alignItems: 'flex-start',
                                    gap: '8px',
                                    padding: '8px 12px',
                                    borderRadius: 'var(--radius-md)',
                                    cursor: 'pointer',
                                    background: assignmentType === t.value ? 'var(--color-primary-50)' : 'transparent',
                                    border: assignmentType === t.value ? '1px solid var(--color-primary-200)' : '1px solid transparent',
                                    marginBottom: '4px',
                                    transition: 'all 150ms ease'
                                }}
                            >
                                <input
                                    type="radio"
                                    name="assignmentType"
                                    value={t.value}
                                    checked={assignmentType === t.value}
                                    onChange={(e) => {
                                        setAssignmentType(e.target.value);
                                        onDirty?.();
                                    }}
                                    style={{ marginTop: '2px' }}
                                />
                                <div>
                                    <div style={{ fontSize: 'var(--font-size-sm)', fontWeight: 'var(--font-weight-medium)', color: 'var(--color-gray-800)' }}>
                                        {t.label}
                                    </div>
                                    <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-gray-500)' }}>
                                        {t.desc}
                                    </div>
                                </div>
                            </label>
                        ))}
                    </div>
                ))}
            </div>

            {/* Conditional selectors */}
            {assignmentType === 'ROLE_BASED' && (
                <div className="panel-section">
                    <label className="panel-section__label">Select Role</label>
                    <select
                        className="panel-select"
                        value={roleId}
                        onChange={(e) => { setRoleId(e.target.value); onDirty?.(); }}
                        disabled={loadingRef}
                    >
                        <option value="">Choose a role...</option>
                        {rolesList.map(r => (
                            <option key={r.id} value={r.id}>{r.name}</option>
                        ))}
                    </select>
                </div>
            )}

            {assignmentType === 'GROUP' && (
                <div className="panel-section">
                    <label className="panel-section__label">Select Group</label>
                    <select
                        className="panel-select"
                        value={groupId}
                        onChange={(e) => { setGroupId(e.target.value); onDirty?.(); }}
                        disabled={loadingRef}
                    >
                        <option value="">Choose a group...</option>
                        {groupsList.map(g => (
                            <option key={g.id} value={g.id}>{g.name}</option>
                        ))}
                    </select>
                </div>
            )}

            {assignmentType === 'BRANCH' && (
                <div className="panel-section">
                    <label className="panel-section__label">Select Branch</label>
                    <select
                        className="panel-select"
                        value={branchId}
                        onChange={(e) => { setBranchId(e.target.value); onDirty?.(); }}
                        disabled={loadingRef}
                    >
                        <option value="">Choose a branch...</option>
                        {branchesList.map(b => (
                            <option key={b.id} value={b.id}>{b.name} ({b.code})</option>
                        ))}
                    </select>
                </div>
            )}

            {assignmentType === 'DEPARTMENT' && (
                <div className="panel-section">
                    <label className="panel-section__label">Select Department</label>
                    <select
                        className="panel-select"
                        value={departmentId}
                        onChange={(e) => { setDepartmentId(e.target.value); onDirty?.(); }}
                        disabled={loadingRef}
                    >
                        <option value="">Choose a department...</option>
                        {departmentsList.map(d => (
                            <option key={d.id} value={d.id}>{d.name}</option>
                        ))}
                    </select>
                </div>
            )}

            {assignmentType === 'SPECIFIC_PERSON' && (
                <div className="panel-section">
                    <label className="panel-section__label">Select User</label>
                    <select
                        className="panel-select"
                        value={userId}
                        onChange={(e) => { setUserId(e.target.value); onDirty?.(); }}
                        disabled={loadingRef}
                    >
                        <option value="">Choose a user...</option>
                        {usersList.map(u => (
                            <option key={u.id} value={u.id}>
                                {u.fullName || u.username} ({u.email || u.username})
                            </option>
                        ))}
                    </select>
                </div>
            )}

            {/* Save Button */}
            {assignmentType && (
                <div style={{ paddingTop: 'var(--space-3)' }}>
                    <button
                        className="btn btn-primary"
                        onClick={handleSave}
                        disabled={saving}
                        style={{ width: '100%' }}
                    >
                        {saving ? 'Saving...' : 'Save Assignment'}
                    </button>
                </div>
            )}
        </div>
    );
}
