import { useState, useEffect } from 'react';
import { workflowConfigs, products, processTemplates, formDefinitions, roles, branches, departments, users, groups } from '../api';

export default function WorkflowConfigsPage() {
    const [configs, setConfigs] = useState([]);
    const [productsList, setProductsList] = useState([]);
    const [templatesList, setTemplatesList] = useState([]);
    const [formsList, setFormsList] = useState([]);
    const [rolesList, setRolesList] = useState([]);
    const [branchesList, setBranchesList] = useState([]);
    const [departmentsList, setDepartmentsList] = useState([]);
    const [usersList, setUsersList] = useState([]);
    const [groupsList, setGroupsList] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [selectedProduct, setSelectedProduct] = useState('');

    // Modal states
    const [showModal, setShowModal] = useState(false);
    const [showStepsModal, setShowStepsModal] = useState(false);
    const [showRulesModal, setShowRulesModal] = useState(false);
    const [editingConfig, setEditingConfig] = useState(null);

    // Form state
    const [form, setForm] = useState({
        productCode: '',
        code: '',
        name: '',
        description: '',
        processTemplateId: '',
        startFormId: ''
    });

    // Steps & Rules (managed separately for clarity)
    const [steps, setSteps] = useState([]);
    const [rules, setRules] = useState({
        defaultAssignment: { type: 'ROLE', roleCode: '', branchCode: '', departmentCode: '', userId: '' },
        taskRules: {}
    });
    const [newStepName, setNewStepName] = useState('');
    const [showAddStep, setShowAddStep] = useState(false);

    useEffect(() => {
        loadInitialData();
    }, []);

    useEffect(() => {
        if (selectedProduct) loadConfigs();
    }, [selectedProduct]);

    async function loadInitialData() {
        try {
            const [productsRes, rolesRes, branchesRes, departmentsRes, usersRes, groupsRes] = await Promise.all([
                products.list(),
                roles.list(),
                branches.list().catch(() => []),
                departments.list().catch(() => []),
                users.list().catch(() => []),
                groups.list().catch(() => [])
            ]);
            setProductsList(productsRes || []);
            setRolesList(rolesRes || []);
            setBranchesList(branchesRes || []);
            setDepartmentsList(departmentsRes || []);
            setUsersList(usersRes || []);
            setGroupsList(groupsRes || []);
            if (productsRes?.length > 0) {
                setSelectedProduct(productsRes[0].code);
            }
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }

    async function loadConfigs() {
        try {
            setLoading(true);
            const configsRes = await workflowConfigs.list(selectedProduct);
            setConfigs(configsRes || []);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }

    async function loadTemplatesAndForms(productCode) {
        if (!productCode) return;
        try {
            const product = productsList.find(p => p.code === productCode);
            if (product) {
                const [templates, forms] = await Promise.all([
                    processTemplates.list(product.id, false),
                    formDefinitions.list(product.id, false)
                ]);
                setTemplatesList(templates || []);
                setFormsList(forms || []);
            }
        } catch (err) {
            console.error('Failed to load templates/forms:', err);
        }
    }

    function openCreate() {
        setEditingConfig(null);
        setForm({
            productCode: selectedProduct,
            code: '',
            name: '',
            description: '',
            processTemplateId: '',
            startFormId: ''
        });
        loadTemplatesAndForms(selectedProduct);
        setShowModal(true);
    }

    function openEdit(config) {
        setEditingConfig(config);
        setForm({
            productCode: config.productCode,
            code: config.code,
            name: config.name,
            description: config.description || '',
            processTemplateId: config.processTemplateId || '',
            startFormId: config.startFormId || ''
        });
        loadTemplatesAndForms(config.productCode);
        setShowModal(true);
    }

    async function handleSubmit(e) {
        e.preventDefault();
        setError('');
        try {
            const data = {
                ...form,
                taskFormMappings: editingConfig?.taskFormMappings || {},
                assignmentRules: editingConfig?.assignmentRules || { defaultAssignment: {}, taskOverrides: {} },
                config: editingConfig?.config || {}
            };

            if (editingConfig) {
                await workflowConfigs.update(editingConfig.id, data);
            } else {
                await workflowConfigs.create(data);
            }
            setShowModal(false);
            loadConfigs();
        } catch (err) {
            setError(err.message);
        }
    }

    async function handleDelete(config) {
        if (!confirm(`Delete "${config.name}"? This cannot be undone.`)) return;
        try {
            await workflowConfigs.delete(config.id);
            loadConfigs();
        } catch (err) {
            setError(err.message);
        }
    }

    // Steps Modal
    function openStepsModal(config) {
        setEditingConfig(config);
        loadTemplatesAndForms(config.productCode);
        const existingSteps = Object.entries(config.taskFormMappings || {}).map(([key, formId]) => ({
            key,
            name: key.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase()),
            formId: formId || ''
        }));
        setSteps(existingSteps.length > 0 ? existingSteps : []);
        setShowStepsModal(true);
    }

    function addStep() {
        if (!newStepName.trim()) return;
        const key = newStepName.trim().toLowerCase().replace(/\s+/g, '_');
        setSteps(prev => [...prev, { key, name: newStepName.trim(), formId: '' }]);
        setNewStepName('');
        setShowAddStep(false);
    }

    function updateStepForm(index, formId) {
        setSteps(prev => prev.map((s, i) => i === index ? { ...s, formId } : s));
    }

    function removeStep(index) {
        setSteps(prev => prev.filter((_, i) => i !== index));
    }

    async function saveSteps() {
        try {
            const taskFormMappings = {};
            steps.forEach(s => { taskFormMappings[s.key] = s.formId; });

            await workflowConfigs.update(editingConfig.id, {
                ...form,
                productCode: editingConfig.productCode,
                code: editingConfig.code,
                name: editingConfig.name,
                taskFormMappings,
                assignmentRules: editingConfig.assignmentRules || {},
                config: editingConfig.config || {}
            });
            setShowStepsModal(false);
            loadConfigs();
        } catch (err) {
            setError(err.message);
        }
    }

    // Rules Modal
    function openRulesModal(config) {
        setEditingConfig(config);
        const assignmentRules = config.assignmentRules || {};
        const defaultAssign = assignmentRules.defaultAssignment || {};

        // Build task rules with full assignment objects
        const taskRulesInit = {};
        Object.entries(assignmentRules.taskOverrides || {}).forEach(([key, rule]) => {
            const defaultRule = rule.default || {};
            taskRulesInit[key] = {
                assignment: {
                    type: defaultRule.type || 'ROLE',
                    roleCode: defaultRule.roleCode || '',
                    branchCode: defaultRule.branchCode || '',
                    departmentCode: defaultRule.departmentCode || '',
                    userId: defaultRule.userId || ''
                },
                conditions: (rule.conditions || []).map(c => ({
                    field: c.field || '',
                    operator: c.operator || 'GREATER_THAN',
                    value: c.value || '',
                    assignment: {
                        type: c.assignment?.type || 'ROLE',
                        roleCode: c.assignment?.roleCode || '',
                        branchCode: c.assignment?.branchCode || '',
                        departmentCode: c.assignment?.departmentCode || '',
                        userId: c.assignment?.userId || ''
                    }
                }))
            };
        });

        // Ensure we have entries for all tasks
        Object.keys(config.taskFormMappings || {}).forEach(key => {
            if (!taskRulesInit[key]) {
                taskRulesInit[key] = {
                    assignment: { type: 'ROLE', roleCode: '', branchCode: '', departmentCode: '', userId: '' },
                    conditions: []
                };
            }
        });

        setRules({
            defaultAssignment: {
                type: defaultAssign.type || 'ROLE',
                roleCode: defaultAssign.roleCode || '',
                branchCode: defaultAssign.branchCode || '',
                departmentCode: defaultAssign.departmentCode || '',
                userId: defaultAssign.userId || ''
            },
            taskRules: taskRulesInit
        });
        setShowRulesModal(true);
    }

    function updateDefaultAssignment(updates) {
        setRules(prev => ({
            ...prev,
            defaultAssignment: { ...prev.defaultAssignment, ...updates }
        }));
    }

    function updateTaskAssignment(taskKey, updates) {
        setRules(prev => ({
            ...prev,
            taskRules: {
                ...prev.taskRules,
                [taskKey]: {
                    ...prev.taskRules[taskKey],
                    assignment: { ...prev.taskRules[taskKey]?.assignment, ...updates }
                }
            }
        }));
    }

    function addCondition(taskKey) {
        setRules(prev => ({
            ...prev,
            taskRules: {
                ...prev.taskRules,
                [taskKey]: {
                    ...prev.taskRules[taskKey],
                    conditions: [
                        ...(prev.taskRules[taskKey]?.conditions || []),
                        {
                            field: '',
                            operator: 'GREATER_THAN',
                            value: '',
                            assignment: { type: 'ROLE', roleCode: '', branchCode: '', departmentCode: '', userId: '' }
                        }
                    ]
                }
            }
        }));
    }

    function updateCondition(taskKey, index, updates) {
        setRules(prev => ({
            ...prev,
            taskRules: {
                ...prev.taskRules,
                [taskKey]: {
                    ...prev.taskRules[taskKey],
                    conditions: prev.taskRules[taskKey].conditions.map((c, i) =>
                        i === index ? { ...c, ...updates } : c
                    )
                }
            }
        }));
    }

    function updateConditionAssignment(taskKey, index, assignmentUpdates) {
        setRules(prev => ({
            ...prev,
            taskRules: {
                ...prev.taskRules,
                [taskKey]: {
                    ...prev.taskRules[taskKey],
                    conditions: prev.taskRules[taskKey].conditions.map((c, i) =>
                        i === index ? { ...c, assignment: { ...c.assignment, ...assignmentUpdates } } : c
                    )
                }
            }
        }));
    }

    function removeCondition(taskKey, index) {
        setRules(prev => ({
            ...prev,
            taskRules: {
                ...prev.taskRules,
                [taskKey]: {
                    ...prev.taskRules[taskKey],
                    conditions: prev.taskRules[taskKey].conditions.filter((_, i) => i !== index)
                }
            }
        }));
    }

    async function saveRules() {
        try {
            const assignmentRules = {
                defaultAssignment: rules.defaultAssignment,
                taskOverrides: {}
            };

            Object.entries(rules.taskRules).forEach(([key, rule]) => {
                assignmentRules.taskOverrides[key] = {
                    default: rule.assignment,
                    conditions: rule.conditions.map(c => ({
                        field: c.field,
                        operator: c.operator,
                        value: c.value,
                        assignment: c.assignment
                    }))
                };
            });

            await workflowConfigs.update(editingConfig.id, {
                productCode: editingConfig.productCode,
                code: editingConfig.code,
                name: editingConfig.name,
                taskFormMappings: editingConfig.taskFormMappings || {},
                assignmentRules,
                config: editingConfig.config || {}
            });
            setShowRulesModal(false);
            loadConfigs();
        } catch (err) {
            setError(err.message);
        }
    }

    // Assignment Selector Component - Supports all real-world banking/corporate scenarios
    function AssignmentSelector({ assignment, onChange, label = "Assign To" }) {
        const type = assignment?.type || 'ROLE';

        // Group assignment types for better UX
        const assignmentTypes = [
            {
                group: 'Role-Based', options: [
                    { value: 'ROLE', label: 'By Role - Any user with this role' },
                    { value: 'ROLE_SAME_BRANCH', label: 'By Role - Same branch as applicant' },
                    { value: 'ROLE_SAME_DEPARTMENT', label: 'By Role - Same department as applicant' },
                ]
            },
            {
                group: 'Group/Committee', options: [
                    { value: 'GROUP', label: 'By Group - Committee, Board, Shareholders' },
                    { value: 'POOL', label: 'By Pool - Work queue (first available)' },
                ]
            },
            {
                group: 'Organizational', options: [
                    { value: 'BRANCH', label: 'By Branch - All users in a branch' },
                    { value: 'DEPARTMENT', label: 'By Department - All users in dept' },
                ]
            },
            {
                group: 'Specific Person', options: [
                    { value: 'SPECIFIC_USER', label: 'Specific User - Named individual' },
                    { value: 'ORIGINATOR', label: 'Originator - Person who started process' },
                    { value: 'PREVIOUS_HANDLER', label: 'Previous Handler - Same as last step' },
                ]
            },
            {
                group: 'Hierarchy', options: [
                    { value: 'SUPERVISOR', label: 'Supervisor - Direct manager of current handler' },
                    { value: 'ESCALATION', label: 'Escalation Chain - Up the hierarchy' },
                ]
            },
            {
                group: 'Distribution', options: [
                    { value: 'ROUND_ROBIN', label: 'Round Robin - Distribute evenly across role' },
                    { value: 'LEAST_BUSY', label: 'Least Busy - User with fewest tasks' },
                ]
            },
        ];

        const needsRoleSelect = ['ROLE', 'ROLE_SAME_BRANCH', 'ROLE_SAME_DEPARTMENT', 'ROUND_ROBIN', 'LEAST_BUSY'].includes(type);
        const needsGroupSelect = ['GROUP', 'POOL'].includes(type);
        const needsBranchSelect = type === 'BRANCH';
        const needsDepartmentSelect = type === 'DEPARTMENT';
        const needsUserSelect = type === 'SPECIFIC_USER';
        const needsEscalationLevel = type === 'ESCALATION';
        const isAutomatic = ['ORIGINATOR', 'PREVIOUS_HANDLER', 'SUPERVISOR'].includes(type);

        return (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                {label && (
                    <div className="form-group" style={{ marginBottom: 0 }}>
                        <label className="form-label">{label}</label>
                    </div>
                )}
                <select
                    className="form-select"
                    value={type}
                    onChange={(e) => onChange({
                        type: e.target.value,
                        roleCode: '',
                        branchCode: '',
                        departmentCode: '',
                        userId: '',
                        groupCode: '',
                        escalationLevel: 1
                    })}
                >
                    {assignmentTypes.map(group => (
                        <optgroup key={group.group} label={group.group}>
                            {group.options.map(opt => (
                                <option key={opt.value} value={opt.value}>{opt.label}</option>
                            ))}
                        </optgroup>
                    ))}
                </select>

                {needsRoleSelect && (
                    <select
                        className="form-select"
                        value={assignment?.roleCode || ''}
                        onChange={(e) => onChange({ ...assignment, roleCode: e.target.value })}
                    >
                        <option value="">-- Select Role --</option>
                        {rolesList.map(r => (
                            <option key={r.id} value={r.code}>{r.name}</option>
                        ))}
                    </select>
                )}

                {needsGroupSelect && (
                    <select
                        className="form-select"
                        value={assignment?.groupCode || ''}
                        onChange={(e) => onChange({ ...assignment, groupCode: e.target.value })}
                    >
                        <option value="">-- Select Group/Committee --</option>
                        {groupsList.map(g => (
                            <option key={g.id} value={g.code}>{g.name}</option>
                        ))}
                    </select>
                )}

                {needsBranchSelect && (
                    <select
                        className="form-select"
                        value={assignment?.branchCode || ''}
                        onChange={(e) => onChange({ ...assignment, branchCode: e.target.value })}
                    >
                        <option value="">-- Select Branch --</option>
                        {branchesList.map(b => (
                            <option key={b.id} value={b.code}>{b.name}</option>
                        ))}
                    </select>
                )}

                {needsDepartmentSelect && (
                    <select
                        className="form-select"
                        value={assignment?.departmentCode || ''}
                        onChange={(e) => onChange({ ...assignment, departmentCode: e.target.value })}
                    >
                        <option value="">-- Select Department --</option>
                        {departmentsList.map(d => (
                            <option key={d.id} value={d.code}>{d.name}</option>
                        ))}
                    </select>
                )}

                {needsUserSelect && (
                    <select
                        className="form-select"
                        value={assignment?.userId || ''}
                        onChange={(e) => onChange({ ...assignment, userId: e.target.value })}
                    >
                        <option value="">-- Select User --</option>
                        {usersList.map(u => (
                            <option key={u.id} value={u.id}>{u.fullName || u.username}</option>
                        ))}
                    </select>
                )}

                {needsEscalationLevel && (
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <span className="text-muted">Escalate up</span>
                        <input
                            type="number"
                            className="form-input"
                            style={{ width: '60px' }}
                            min="1"
                            max="5"
                            value={assignment?.escalationLevel || 1}
                            onChange={(e) => onChange({ ...assignment, escalationLevel: parseInt(e.target.value) || 1 })}
                        />
                        <span className="text-muted">level(s) in hierarchy</span>
                    </div>
                )}

                {isAutomatic && (
                    <div style={{
                        padding: '8px 12px',
                        background: 'rgba(var(--success-rgb), 0.1)',
                        borderRadius: '6px',
                        fontSize: '13px',
                        color: 'var(--success)'
                    }}>
                        ✓ Automatically determined at runtime based on process context
                    </div>
                )}
            </div>
        );
    }

    return (
        <div>
            <div className="page-header">
                <h1>Process Configurations</h1>
                <div className="flex gap-2">
                    <select
                        className="form-select"
                        style={{ width: '200px' }}
                        value={selectedProduct}
                        onChange={(e) => setSelectedProduct(e.target.value)}
                    >
                        {productsList.map(p => (
                            <option key={p.code} value={p.code}>{p.name}</option>
                        ))}
                    </select>
                    <button className="btn btn-primary" onClick={openCreate}>+ Add Configuration</button>
                </div>
            </div>

            {error && <div className="form-error mb-4">{error}</div>}

            <div className="card">
                {loading ? (
                    <div className="loading">Loading...</div>
                ) : configs.length === 0 ? (
                    <div className="empty-state">
                        <p>No process configurations found for this product.</p>
                        <p className="text-muted">Create a configuration to link workflows, forms, and assignment rules.</p>
                    </div>
                ) : (
                    <div className="table-container">
                        <table>
                            <thead>
                                <tr>
                                    <th>Name</th>
                                    <th>Code</th>
                                    <th>Workflow</th>
                                    <th>Steps</th>
                                    <th>Assignment</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {configs.map(config => (
                                    <tr key={config.id}>
                                        <td><strong>{config.name}</strong></td>
                                        <td><code>{config.code}</code></td>
                                        <td className="text-muted">{config.processTemplateName || 'Not linked'}</td>
                                        <td>
                                            <button
                                                className="btn btn-sm btn-secondary"
                                                onClick={() => openStepsModal(config)}
                                            >
                                                {Object.keys(config.taskFormMappings || {}).length} steps
                                            </button>
                                        </td>
                                        <td>
                                            <button
                                                className="btn btn-sm btn-secondary"
                                                onClick={() => openRulesModal(config)}
                                            >
                                                Edit Rules
                                            </button>
                                        </td>
                                        <td>
                                            <div className="flex gap-2">
                                                <button className="btn btn-sm btn-secondary" onClick={() => openEdit(config)}>
                                                    Edit
                                                </button>
                                                <button className="btn btn-sm btn-danger" onClick={() => handleDelete(config)}>
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

            {/* Create/Edit Configuration Modal */}
            {showModal && (
                <div className="modal-overlay" onClick={() => setShowModal(false)}>
                    <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: '500px' }}>
                        <div className="modal-header">
                            <h3>{editingConfig ? 'Edit Configuration' : 'Create Configuration'}</h3>
                            <button className="btn btn-sm btn-secondary" onClick={() => setShowModal(false)}>✕</button>
                        </div>
                        <form onSubmit={handleSubmit}>
                            <div className="modal-body">
                                <div className="form-group">
                                    <label className="form-label">Product</label>
                                    <select
                                        className="form-select"
                                        value={form.productCode}
                                        onChange={(e) => {
                                            setForm({ ...form, productCode: e.target.value });
                                            loadTemplatesAndForms(e.target.value);
                                        }}
                                        required
                                        disabled={!!editingConfig}
                                    >
                                        {productsList.map(p => (
                                            <option key={p.code} value={p.code}>{p.name}</option>
                                        ))}
                                    </select>
                                </div>

                                <div className="form-group">
                                    <label className="form-label">Code</label>
                                    <input
                                        className="form-input"
                                        value={form.code}
                                        onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase().replace(/[^A-Z0-9_]/g, '') })}
                                        placeholder="HOME_LOAN"
                                        required
                                        disabled={!!editingConfig}
                                    />
                                    <small className="text-muted">Unique identifier (e.g., HOME_LOAN, PERSONAL_LOAN)</small>
                                </div>

                                <div className="form-group">
                                    <label className="form-label">Name</label>
                                    <input
                                        className="form-input"
                                        value={form.name}
                                        onChange={(e) => setForm({ ...form, name: e.target.value })}
                                        placeholder="Home Loan Application"
                                        required
                                    />
                                </div>

                                <div className="form-group">
                                    <label className="form-label">Description</label>
                                    <textarea
                                        className="form-input"
                                        value={form.description}
                                        onChange={(e) => setForm({ ...form, description: e.target.value })}
                                        placeholder="Process for handling home loan applications..."
                                        rows={2}
                                    />
                                </div>

                                <div className="form-group">
                                    <label className="form-label">Workflow Template</label>
                                    <select
                                        className="form-select"
                                        value={form.processTemplateId}
                                        onChange={(e) => setForm({ ...form, processTemplateId: e.target.value })}
                                    >
                                        <option value="">-- Select Workflow --</option>
                                        {templatesList.map(t => (
                                            <option key={t.id} value={t.id}>{t.name} (v{t.version})</option>
                                        ))}
                                    </select>
                                    <small className="text-muted">Link to a BPMN workflow that controls this process</small>
                                </div>

                                <div className="form-group">
                                    <label className="form-label">Start Form</label>
                                    <select
                                        className="form-select"
                                        value={form.startFormId}
                                        onChange={(e) => setForm({ ...form, startFormId: e.target.value })}
                                    >
                                        <option value="">-- Select Start Form --</option>
                                        {formsList.map(f => (
                                            <option key={f.id} value={f.id}>{f.name}</option>
                                        ))}
                                    </select>
                                    <small className="text-muted">Form shown when starting a new application</small>
                                </div>
                            </div>
                            <div className="modal-footer">
                                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Cancel</button>
                                <button type="submit" className="btn btn-primary">{editingConfig ? 'Update' : 'Create'}</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* Steps Modal */}
            {showStepsModal && editingConfig && (
                <div className="modal-overlay" onClick={() => setShowStepsModal(false)}>
                    <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: '600px' }}>
                        <div className="modal-header">
                            <h3>Process Steps - {editingConfig.name}</h3>
                            <button className="btn btn-sm btn-secondary" onClick={() => setShowStepsModal(false)}>✕</button>
                        </div>
                        <div className="modal-body" style={{ maxHeight: '60vh', overflowY: 'auto' }}>
                            <p className="text-muted mb-4">
                                Define the steps in your process and which form to show at each step.
                            </p>

                            {steps.length === 0 && !showAddStep ? (
                                <div className="empty-state">
                                    <p>No steps defined yet.</p>
                                    <button className="btn btn-primary btn-sm" onClick={() => setShowAddStep(true)}>+ Add First Step</button>
                                </div>
                            ) : (
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                                    {steps.map((step, index) => (
                                        <div
                                            key={step.key}
                                            style={{
                                                display: 'flex',
                                                alignItems: 'center',
                                                gap: '12px',
                                                padding: '12px',
                                                border: '1px solid var(--border)',
                                                borderRadius: '8px',
                                                background: 'var(--bg-secondary)'
                                            }}
                                        >
                                            <div style={{
                                                width: '28px',
                                                height: '28px',
                                                background: 'var(--primary)',
                                                color: 'white',
                                                borderRadius: '50%',
                                                display: 'flex',
                                                alignItems: 'center',
                                                justifyContent: 'center',
                                                fontWeight: 'bold',
                                                fontSize: '14px'
                                            }}>
                                                {index + 1}
                                            </div>
                                            <div style={{ flex: 1 }}>
                                                <div style={{ fontWeight: 500, marginBottom: '4px' }}>{step.name}</div>
                                                <select
                                                    className="form-select"
                                                    value={step.formId}
                                                    onChange={(e) => updateStepForm(index, e.target.value)}
                                                    style={{ width: '100%' }}
                                                >
                                                    <option value="">-- Select Form --</option>
                                                    {formsList.map(f => (
                                                        <option key={f.id} value={f.id}>{f.name}</option>
                                                    ))}
                                                </select>
                                            </div>
                                            <button
                                                className="btn btn-sm btn-danger"
                                                onClick={() => removeStep(index)}
                                                title="Remove step"
                                            >
                                                ✕
                                            </button>
                                        </div>
                                    ))}

                                    {/* Inline Add Step Form */}
                                    {showAddStep ? (
                                        <div style={{
                                            display: 'flex',
                                            alignItems: 'center',
                                            gap: '12px',
                                            padding: '12px',
                                            border: '2px dashed var(--primary)',
                                            borderRadius: '8px',
                                            background: 'rgba(var(--primary-rgb), 0.05)'
                                        }}>
                                            <div style={{
                                                width: '28px',
                                                height: '28px',
                                                background: 'var(--border)',
                                                color: 'var(--text)',
                                                borderRadius: '50%',
                                                display: 'flex',
                                                alignItems: 'center',
                                                justifyContent: 'center',
                                                fontWeight: 'bold',
                                                fontSize: '14px'
                                            }}>
                                                {steps.length + 1}
                                            </div>
                                            <div style={{ flex: 1 }}>
                                                <input
                                                    className="form-input"
                                                    type="text"
                                                    placeholder="Enter step name (e.g., Document Verification)"
                                                    value={newStepName}
                                                    onChange={(e) => setNewStepName(e.target.value)}
                                                    onKeyDown={(e) => {
                                                        if (e.key === 'Enter') addStep();
                                                        if (e.key === 'Escape') { setShowAddStep(false); setNewStepName(''); }
                                                    }}
                                                    autoFocus
                                                />
                                            </div>
                                            <button
                                                className="btn btn-sm btn-primary"
                                                onClick={addStep}
                                                disabled={!newStepName.trim()}
                                            >
                                                Add
                                            </button>
                                            <button
                                                className="btn btn-sm btn-secondary"
                                                onClick={() => { setShowAddStep(false); setNewStepName(''); }}
                                            >
                                                Cancel
                                            </button>
                                        </div>
                                    ) : (
                                        <button
                                            className="btn btn-secondary btn-sm"
                                            onClick={() => setShowAddStep(true)}
                                            style={{ alignSelf: 'flex-start' }}
                                        >
                                            + Add Step
                                        </button>
                                    )}
                                </div>
                            )}
                        </div>
                        <div className="modal-footer">
                            <span className="text-muted" style={{ marginRight: 'auto' }}>
                                {steps.length} step{steps.length !== 1 ? 's' : ''}
                            </span>
                            <button className="btn btn-secondary" onClick={() => setShowStepsModal(false)}>Cancel</button>
                            <button className="btn btn-primary" onClick={saveSteps}>Save Steps</button>
                        </div>
                    </div>
                </div>
            )}

            {/* Assignment Rules Modal */}
            {showRulesModal && editingConfig && (
                <div className="modal-overlay" onClick={() => setShowRulesModal(false)}>
                    <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: '750px' }}>
                        <div className="modal-header">
                            <h3>Assignment Rules - {editingConfig.name}</h3>
                            <button className="btn btn-sm btn-secondary" onClick={() => setShowRulesModal(false)}>✕</button>
                        </div>
                        <div className="modal-body" style={{ maxHeight: '65vh', overflowY: 'auto' }}>
                            {/* Default Assignment */}
                            <div style={{ marginBottom: '20px', padding: '16px', background: 'var(--bg-secondary)', borderRadius: '8px' }}>
                                <h4 style={{ marginBottom: '12px' }}>Default Handler (for all steps)</h4>
                                <AssignmentSelector
                                    assignment={rules.defaultAssignment}
                                    onChange={(a) => updateDefaultAssignment(a)}
                                    label="Who handles tasks by default?"
                                />
                                <small className="text-muted" style={{ display: 'block', marginTop: '8px' }}>
                                    This applies unless a step has its own specific rule
                                </small>
                            </div>

                            <hr style={{ margin: '20px 0', border: 'none', borderTop: '1px solid var(--border)' }} />

                            <h4 style={{ marginBottom: '12px' }}>Step-Specific Rules</h4>

                            {Object.keys(editingConfig.taskFormMappings || {}).length === 0 ? (
                                <p className="text-muted">No steps defined. Add steps first to configure assignment rules.</p>
                            ) : (
                                Object.keys(editingConfig.taskFormMappings || {}).map(taskKey => (
                                    <div
                                        key={taskKey}
                                        style={{
                                            marginBottom: '16px',
                                            padding: '16px',
                                            border: '1px solid var(--border)',
                                            borderRadius: '8px'
                                        }}
                                    >
                                        <div style={{ fontWeight: 600, marginBottom: '12px', fontSize: '15px' }}>
                                            📌 {taskKey.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase())}
                                        </div>

                                        <AssignmentSelector
                                            assignment={rules.taskRules[taskKey]?.assignment}
                                            onChange={(a) => updateTaskAssignment(taskKey, a)}
                                            label="Who handles this step?"
                                        />

                                        {/* Conditions */}
                                        <div style={{ marginTop: '16px' }}>
                                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                                                <small className="text-muted" style={{ fontWeight: 500 }}>Conditional Routing (optional)</small>
                                                <button className="btn btn-sm btn-secondary" onClick={() => addCondition(taskKey)}>
                                                    + Add Condition
                                                </button>
                                            </div>

                                            {(rules.taskRules[taskKey]?.conditions || []).map((cond, idx) => (
                                                <div
                                                    key={idx}
                                                    style={{
                                                        marginBottom: '12px',
                                                        padding: '12px',
                                                        background: 'var(--bg-secondary)',
                                                        borderRadius: '8px',
                                                        border: '1px solid var(--border)'
                                                    }}
                                                >
                                                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '10px', flexWrap: 'wrap' }}>
                                                        <span style={{ fontWeight: 600, color: 'var(--primary)' }}>IF</span>
                                                        <input
                                                            className="form-input"
                                                            style={{ width: '140px' }}
                                                            placeholder="loanAmount"
                                                            value={cond.field || ''}
                                                            onChange={(e) => updateCondition(taskKey, idx, { field: e.target.value })}
                                                        />
                                                        <select
                                                            className="form-select"
                                                            style={{ width: '100px' }}
                                                            value={cond.operator || 'GREATER_THAN'}
                                                            onChange={(e) => updateCondition(taskKey, idx, { operator: e.target.value })}
                                                        >
                                                            <option value="EQUALS">=</option>
                                                            <option value="NOT_EQUALS">≠</option>
                                                            <option value="GREATER_THAN">&gt;</option>
                                                            <option value="LESS_THAN">&lt;</option>
                                                            <option value="GTE">≥</option>
                                                            <option value="LTE">≤</option>
                                                            <option value="CONTAINS">contains</option>
                                                            <option value="IN">in list</option>
                                                        </select>
                                                        <input
                                                            className="form-input"
                                                            style={{ width: '120px' }}
                                                            placeholder="5000000"
                                                            value={cond.value || ''}
                                                            onChange={(e) => updateCondition(taskKey, idx, { value: e.target.value })}
                                                        />
                                                        <button
                                                            className="btn btn-sm btn-danger"
                                                            onClick={() => removeCondition(taskKey, idx)}
                                                            style={{ marginLeft: 'auto' }}
                                                        >
                                                            ✕
                                                        </button>
                                                    </div>
                                                    <div style={{ paddingLeft: '24px' }}>
                                                        <span style={{ fontWeight: 600, color: 'var(--primary)', display: 'block', marginBottom: '8px' }}>THEN →</span>
                                                        <AssignmentSelector
                                                            assignment={cond.assignment}
                                                            onChange={(a) => updateConditionAssignment(taskKey, idx, a)}
                                                            label=""
                                                        />
                                                    </div>
                                                </div>
                                            ))}

                                            {(rules.taskRules[taskKey]?.conditions || []).length === 0 && (
                                                <p className="text-muted" style={{ fontSize: '13px', margin: 0 }}>
                                                    No conditions. Add conditions to route tasks based on application data.
                                                </p>
                                            )}
                                        </div>
                                    </div>
                                ))
                            )}
                        </div>
                        <div className="modal-footer">
                            <button className="btn btn-secondary" onClick={() => setShowRulesModal(false)}>Cancel</button>
                            <button className="btn btn-primary" onClick={saveRules}>Save Rules</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
