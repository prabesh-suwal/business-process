import { useState } from 'react'

/**
 * PolicyRuleBuilder - Enterprise visual rule builder for ABAC policies
 * 
 * Converts JSON rules to/from a user-friendly visual interface
 */

// Operator definitions with labels and categories
const OPERATORS = [
    { value: 'EQUALS', label: '= Equals', category: 'equality' },
    { value: 'NOT_EQUALS', label: '≠ Not Equals', category: 'equality' },
    { value: 'IN', label: '∈ In List', category: 'collection' },
    { value: 'NOT_IN', label: '∉ Not In List', category: 'collection' },
    { value: 'CONTAINS', label: '⊃ Contains', category: 'collection' },
    { value: 'CONTAINS_ANY', label: '∩ Contains Any', category: 'collection' },
    { value: 'GREATER_THAN', label: '> Greater Than', category: 'comparison' },
    { value: 'GREATER_THAN_OR_EQUAL', label: '≥ Greater or Equal', category: 'comparison' },
    { value: 'LESS_THAN', label: '< Less Than', category: 'comparison' },
    { value: 'LESS_THAN_OR_EQUAL', label: '≤ Less or Equal', category: 'comparison' },
    { value: 'STARTS_WITH', label: 'Starts With', category: 'string' },
    { value: 'ENDS_WITH', label: 'Ends With', category: 'string' },
    { value: 'IS_NULL', label: 'Is Null', category: 'null' },
    { value: 'IS_NOT_NULL', label: 'Is Not Null', category: 'null' },
    { value: 'IS_TRUE', label: 'Is True', category: 'boolean' },
    { value: 'IS_FALSE', label: 'Is False', category: 'boolean' },
];

const TEMPORAL_CONDITIONS = [
    { value: 'NONE', label: 'No time restriction', icon: '🔓' },
    { value: 'BUSINESS_HOURS', label: 'Business hours only', icon: '🏢' },
    { value: 'WEEKDAYS_ONLY', label: 'Weekdays only (Mon-Fri)', icon: '📅' },
    { value: 'WEEKENDS_ONLY', label: 'Weekends only', icon: '🌴' },
    { value: 'WITHIN_PERIOD', label: 'Within date range', icon: '📆' },
    { value: 'OUTSIDE_PERIOD', label: 'Outside date range', icon: '🚫' },
    { value: 'TIME_WINDOW', label: 'Specific time window', icon: '⏰' },
];

const VALUE_TYPES = [
    { value: 'STRING', label: 'Text', description: 'Exact text match (e.g. "approved")' },
    { value: 'NUMBER', label: 'Number', description: 'Numeric value (e.g. 1000, 50.5)' },
    { value: 'BOOLEAN', label: 'Boolean', description: 'True or False' },
    { value: 'ARRAY', label: 'List', description: 'Comma separated list (e.g. A, B, C)' },
    { value: 'EXPRESSION', label: 'Expression', description: 'Reference like subject.branchIds' },
];

// Common attribute suggestions for banking/enterprise
// Enterprise attribute dictionary
const ATTRIBUTES = {
    subject: [
        { value: 'userId', label: 'User ID' },
        { value: 'username', label: 'Username' },
        { value: 'email', label: 'Email' },
        { value: 'roles', label: 'Assigned Roles' },
        { value: 'permissions', label: 'Assigned Permissions' },
        { value: 'branchIds', label: 'Assigned Branches' },
        { value: 'departmentIds', label: 'Assigned Departments' },
        { value: 'regionIds', label: 'Assigned Regions' },
        { value: 'approvalLimit', label: 'Approval Limit (Amount)' },
        { value: 'hierarchyLevel', label: 'Hierarchy Level' },
    ],
    resource: [
        { value: 'type', label: 'Resource Type' },
        { value: 'id', label: 'Resource ID' },
        { value: 'branchId', label: 'Branch ID' },
        { value: 'regionId', label: 'Region ID' },
        { value: 'amount', label: 'Amount / Value' },
        { value: 'ownerId', label: 'Owner User ID' },
        { value: 'status', label: 'Status' },
        { value: 'classification', label: 'Security Classification' },
    ],
    environment: [
        { value: 'clientIp', label: 'Client IP Address' },
        { value: 'timestamp', label: 'Request Time' },
        { value: 'userAgent', label: 'User Agent' },
        { value: 'service', label: 'Service Name' },
    ],
    context: [ // Legacy support
        { value: 'ipAddress', label: 'IP Address' },
        { value: 'requestTime', label: 'Request Time' },
        { value: 'channel', label: 'Channel' },
    ]
};

const ENTITY_TYPES = [
    { value: 'subject', label: '👤 Subject (Who)' },
    { value: 'resource', label: '📦 Resource (What)' },
    { value: 'environment', label: '🌐 Environment (Ctx)' },
];


export default function PolicyRuleBuilder({ rules, onChange }) {
    const [expandedRule, setExpandedRule] = useState(null);

    const addRule = () => {
        const newRule = {
            id: Date.now(),
            attribute: '',
            operator: 'EQUALS',
            valueType: 'STRING',
            value: '',
            description: '',
            ruleGroup: 'default',
            sortOrder: rules.length,
            temporalCondition: 'NONE',
            timeFrom: null,
            timeTo: null,
            validFrom: null,
            validUntil: null,
            id: Date.now(),
            attribute: '',
            operator: 'EQUALS',
            valueType: 'STRING',
            value: '',
            description: '',
            ruleGroup: 'default',
            sortOrder: rules.length,
            temporalCondition: 'NONE',
            timeFrom: null,
            timeTo: null,
            validFrom: null,
            validUntil: null,
            timezone: 'Asia/Kathmandu',
        };
        onChange([...rules, newRule]);
        setExpandedRule(newRule.id);
    };

    const updateRule = (index, updates) => {
        const newRules = [...rules];
        newRules[index] = { ...newRules[index], ...updates };
        onChange(newRules);
    };

    const removeRule = (index) => {
        onChange(rules.filter((_, i) => i !== index));
    };

    const moveRule = (index, direction) => {
        if (index + direction < 0 || index + direction >= rules.length) return;
        const newRules = [...rules];
        [newRules[index], newRules[index + direction]] = [newRules[index + direction], newRules[index]];
        onChange(newRules);
    };

    const needsTimeInputs = (condition) => ['BUSINESS_HOURS', 'TIME_WINDOW'].includes(condition);
    const needsDateInputs = (condition) => ['WITHIN_PERIOD', 'OUTSIDE_PERIOD'].includes(condition);
    const noValueNeeded = (op) => ['IS_NULL', 'IS_NOT_NULL', 'IS_TRUE', 'IS_FALSE'].includes(op);

    return (
        <div className="policy-rule-builder">
            {/* Rules Header */}
            <div style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                marginBottom: '16px',
                padding: '12px 16px',
                background: 'var(--bg-elevated)',
                borderRadius: '8px'
            }}>
                <div>
                    <strong>Policy Rules</strong>
                    <span className="text-muted ml-2">({rules.length} condition{rules.length !== 1 ? 's' : ''})</span>
                </div>
                <button type="button" className="btn btn-sm btn-primary" onClick={addRule}>
                    + Add Condition
                </button>
            </div>

            {/* Rules List */}
            {rules.length === 0 ? (
                <div style={{
                    padding: '40px 20px',
                    textAlign: 'center',
                    border: '2px dashed var(--border)',
                    borderRadius: '8px',
                    color: 'var(--text-muted)'
                }}>
                    <div style={{ fontSize: '32px', marginBottom: '8px' }}>📋</div>
                    <div>No conditions defined</div>
                    <div style={{ fontSize: '12px' }}>Click "Add Condition" to create policy rules</div>
                </div>
            ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                    {rules.map((rule, index) => {
                        const isExpanded = expandedRule === (rule.id || index);
                        const operator = OPERATORS.find(o => o.value === rule.operator);
                        const temporal = TEMPORAL_CONDITIONS.find(t => t.value === rule.temporalCondition);

                        return (
                            <div
                                key={rule.id || index}
                                style={{
                                    border: '1px solid var(--border)',
                                    borderRadius: '8px',
                                    background: 'var(--bg-card)',
                                    overflow: 'hidden'
                                }}
                            >
                                {/* Rule Header (collapsed view) */}
                                <div
                                    onClick={() => setExpandedRule(isExpanded ? null : (rule.id || index))}
                                    style={{
                                        padding: '12px 16px',
                                        display: 'flex',
                                        alignItems: 'center',
                                        gap: '12px',
                                        cursor: 'pointer',
                                        background: isExpanded ? 'rgba(var(--primary-rgb), 0.05)' : 'transparent'
                                    }}
                                >
                                    <span style={{ color: 'var(--text-muted)', fontSize: '12px', fontWeight: 600 }}>
                                        #{index + 1}
                                    </span>

                                    <div style={{ flex: 1 }}>
                                        {rule.attribute ? (
                                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                                                <code style={{ fontSize: '13px', color: 'var(--primary)' }}>{rule.attribute}</code>
                                                <span className="badge badge-neutral">{operator?.label || rule.operator}</span>
                                                {!noValueNeeded(rule.operator) && (
                                                    <>
                                                        <code style={{
                                                            fontSize: '13px',
                                                            color: rule.valueType === 'EXPRESSION' ? 'var(--warning)' : 'var(--text)'
                                                        }}>
                                                            {rule.valueType === 'EXPRESSION' ? `{${rule.value}}` : `"${rule.value}"`}
                                                        </code>
                                                    </>
                                                )}
                                                {rule.temporalCondition && rule.temporalCondition !== 'NONE' && (
                                                    <span style={{ marginLeft: '8px' }}>
                                                        {temporal?.icon} <span className="text-muted">{temporal?.label}</span>
                                                    </span>
                                                )}
                                            </div>
                                        ) : (
                                            <span className="text-muted">Click to configure condition</span>
                                        )}
                                    </div>

                                    <div style={{ display: 'flex', gap: '4px' }} onClick={e => e.stopPropagation()}>
                                        <button
                                            type="button"
                                            className="btn btn-sm btn-secondary"
                                            onClick={() => moveRule(index, -1)}
                                            disabled={index === 0}
                                            title="Move up"
                                        >↑</button>
                                        <button
                                            type="button"
                                            className="btn btn-sm btn-secondary"
                                            onClick={() => moveRule(index, 1)}
                                            disabled={index === rules.length - 1}
                                            title="Move down"
                                        >↓</button>
                                        <button
                                            type="button"
                                            className="btn btn-sm btn-danger"
                                            onClick={() => removeRule(index)}
                                            title="Remove"
                                        >✕</button>
                                    </div>

                                    <span style={{ marginLeft: '8px' }}>{isExpanded ? '▼' : '▶'}</span>
                                </div>

                                {/* Expanded Rule Editor */}
                                {isExpanded && (
                                    <div style={{
                                        padding: '16px',
                                        borderTop: '1px solid var(--border)',
                                        background: 'var(--bg-elevated)'
                                    }}>
                                        {/* Attribute Selection */}
                                        {/* Attribute Selection (Split) */}
                                        <div className="form-group">
                                            <label className="form-label">Attribute</label>
                                            <div style={{ display: 'flex', gap: '8px' }}>
                                                {/* Entity Dropdown */}
                                                <div style={{ flex: 1 }}>
                                                    <select
                                                        className="form-select"
                                                        value={rule.attribute ? rule.attribute.split('.')[0] : ''}
                                                        onChange={(e) => {
                                                            const entity = e.target.value;
                                                            // When entity changes, try to keep field if valid, else clear
                                                            const newAttr = entity ? `${entity}.` : '';
                                                            updateRule(index, { attribute: newAttr });
                                                        }}
                                                    >
                                                        <option value="">-- Entity --</option>
                                                        {ENTITY_TYPES.map(e => (
                                                            <option key={e.value} value={e.value}>{e.label}</option>
                                                        ))}
                                                        <option value="_custom">Custom...</option>
                                                    </select>
                                                </div>

                                                {/* Field Dropdown */}
                                                <div style={{ flex: 2 }}>
                                                    {rule.attribute && !rule.attribute.startsWith('_custom') ? (() => {
                                                        const entity = rule.attribute.split('.')[0] || 'subject';
                                                        const field = rule.attribute.split('.')[1];
                                                        const isKnown = (ATTRIBUTES[entity] || []).some(a => a.value === field);
                                                        // If field exists but isn't known, it's manual. If it's literally '_manual', it's manual.
                                                        const selectValue = isKnown ? field : (field ? '_manual' : '');

                                                        return (
                                                            <>
                                                                <select
                                                                    className="form-select"
                                                                    value={selectValue}
                                                                    onChange={(e) => {
                                                                        const selected = e.target.value;
                                                                        if (selected === '_manual') {
                                                                            updateRule(index, { attribute: `${entity}._manual` });
                                                                        } else {
                                                                            updateRule(index, { attribute: `${entity}.${selected}` });
                                                                        }
                                                                    }}
                                                                >
                                                                    <option value="">-- Select Attribute --</option>
                                                                    {(ATTRIBUTES[entity] || []).map(a => (
                                                                        <option key={a.value} value={a.value}>{a.label}</option>
                                                                    ))}
                                                                    <option disabled>──────────</option>
                                                                    <option value="_manual">Enter manually...</option>
                                                                </select>

                                                                {/* Manual Input - overrides dropdown if selected */}
                                                                {selectValue === '_manual' && (
                                                                    <input
                                                                        style={{ marginTop: '8px' }}
                                                                        className="form-input"
                                                                        placeholder="Type attribute name..."
                                                                        value={field === '_manual' ? '' : field}
                                                                        onChange={(e) => {
                                                                            updateRule(index, { attribute: `${entity}.${e.target.value}` });
                                                                        }}
                                                                        autoFocus={field === '_manual'}
                                                                    />
                                                                )}
                                                            </>
                                                        );
                                                    })() : (
                                                        <input
                                                            className="form-input"
                                                            placeholder="Full attribute path (e.g. subject.role)"
                                                            value={rule.attribute === '_custom' ? '' : rule.attribute}
                                                            onChange={(e) => updateRule(index, { attribute: e.target.value })}
                                                        />
                                                    )}
                                                </div>
                                            </div>
                                            {/* (Removed old manual input block as it's now integrated above) */}
                                        </div>

                                        {/* Operator */}
                                        <div className="form-group">
                                            <label className="form-label">Operator</label>
                                            <select
                                                className="form-select"
                                                value={rule.operator}
                                                onChange={(e) => updateRule(index, { operator: e.target.value })}
                                            >
                                                <optgroup label="Equality">
                                                    {OPERATORS.filter(o => o.category === 'equality').map(o => (
                                                        <option key={o.value} value={o.value}>{o.label}</option>
                                                    ))}
                                                </optgroup>
                                                <optgroup label="Collection">
                                                    {OPERATORS.filter(o => o.category === 'collection').map(o => (
                                                        <option key={o.value} value={o.value}>{o.label}</option>
                                                    ))}
                                                </optgroup>
                                                <optgroup label="Comparison">
                                                    {OPERATORS.filter(o => o.category === 'comparison').map(o => (
                                                        <option key={o.value} value={o.value}>{o.label}</option>
                                                    ))}
                                                </optgroup>
                                                <optgroup label="String">
                                                    {OPERATORS.filter(o => o.category === 'string').map(o => (
                                                        <option key={o.value} value={o.value}>{o.label}</option>
                                                    ))}
                                                </optgroup>
                                                <optgroup label="Null/Boolean">
                                                    {OPERATORS.filter(o => ['null', 'boolean'].includes(o.category)).map(o => (
                                                        <option key={o.value} value={o.value}>{o.label}</option>
                                                    ))}
                                                </optgroup>
                                            </select>
                                        </div>

                                        {/* Value Section */}
                                        {/* Value Section */}
                                        {!noValueNeeded(rule.operator) && (
                                            <div className="form-group">
                                                <label className="form-label">Value</label>
                                                <div style={{ display: 'flex', gap: '8px', alignItems: 'flex-start' }}>
                                                    <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                                                        <select
                                                            className="form-select"
                                                            value={rule.valueType}
                                                            onChange={(e) => updateRule(index, { valueType: e.target.value })}
                                                            style={{ width: '120px' }}
                                                        >
                                                            {VALUE_TYPES.map(vt => (
                                                                <option key={vt.value} value={vt.value}>{vt.label}</option>
                                                            ))}
                                                        </select>
                                                    </div>

                                                    {/* Dynamic Input based on Type */}
                                                    {rule.valueType === 'BOOLEAN' ? (
                                                        <select
                                                            className="form-select"
                                                            value={rule.value}
                                                            onChange={(e) => updateRule(index, { value: e.target.value })}
                                                            style={{ flex: 1 }}
                                                        >
                                                            <option value="">-- Select --</option>
                                                            <option value="true">True</option>
                                                            <option value="false">False</option>
                                                        </select>
                                                    ) : (
                                                        <input
                                                            type={rule.valueType === 'NUMBER' ? 'number' : 'text'}
                                                            className="form-input"
                                                            value={rule.value}
                                                            onChange={(e) => updateRule(index, { value: e.target.value })}
                                                            placeholder={
                                                                rule.valueType === 'EXPRESSION' ? 'e.g. subject.branchIds' :
                                                                    rule.valueType === 'ARRAY' ? 'e.g. ADMIN, MANAGER' :
                                                                        'Value to compare against'
                                                            }
                                                            style={{ flex: 1 }}
                                                        />
                                                    )}
                                                </div>
                                                <small className="text-muted">
                                                    {VALUE_TYPES.find(v => v.value === rule.valueType)?.description}
                                                </small>
                                            </div>
                                        )}

                                        {/* Temporal Conditions */}
                                        <div style={{
                                            marginTop: '16px',
                                            padding: '12px',
                                            background: 'var(--bg-card)',
                                            borderRadius: '6px',
                                            border: '1px solid var(--border)'
                                        }}>
                                            <label className="form-label" style={{ marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                                                ⏰ Time Restriction
                                            </label>
                                            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))', gap: '8px' }}>
                                                {TEMPORAL_CONDITIONS.map(tc => (
                                                    <label
                                                        key={tc.value}
                                                        style={{
                                                            display: 'flex',
                                                            alignItems: 'center',
                                                            gap: '8px',
                                                            padding: '8px 12px',
                                                            border: rule.temporalCondition === tc.value ? '2px solid var(--primary)' : '1px solid var(--border)',
                                                            borderRadius: '6px',
                                                            cursor: 'pointer',
                                                            background: rule.temporalCondition === tc.value ? 'rgba(var(--primary-rgb), 0.1)' : 'transparent',
                                                            fontSize: '13px'
                                                        }}
                                                    >
                                                        <input
                                                            type="radio"
                                                            name={`temporal-${index}`}
                                                            checked={rule.temporalCondition === tc.value}
                                                            onChange={() => updateRule(index, { temporalCondition: tc.value })}
                                                        />
                                                        <span>{tc.icon}</span>
                                                        <span>{tc.label}</span>
                                                    </label>
                                                ))}
                                            </div>

                                            {/* Time Inputs */}
                                            {needsTimeInputs(rule.temporalCondition) && (
                                                <div style={{ display: 'flex', gap: '16px', marginTop: '12px' }}>
                                                    <div className="form-group" style={{ flex: 1, marginBottom: 0 }}>
                                                        <label className="form-label">From Time</label>
                                                        <input
                                                            type="time"
                                                            className="form-input"
                                                            value={rule.timeFrom || '09:00'}
                                                            onChange={(e) => updateRule(index, { timeFrom: e.target.value })}
                                                        />
                                                    </div>
                                                    <div className="form-group" style={{ flex: 1, marginBottom: 0 }}>
                                                        <label className="form-label">To Time</label>
                                                        <input
                                                            type="time"
                                                            className="form-input"
                                                            value={rule.timeTo || '18:00'}
                                                            onChange={(e) => updateRule(index, { timeTo: e.target.value })}
                                                        />
                                                    </div>
                                                </div>
                                            )}

                                            {/* Date Inputs */}
                                            {needsDateInputs(rule.temporalCondition) && (
                                                <div style={{ display: 'flex', gap: '16px', marginTop: '12px' }}>
                                                    <div className="form-group" style={{ flex: 1, marginBottom: 0 }}>
                                                        <label className="form-label">Valid From</label>
                                                        <input
                                                            type="date"
                                                            className="form-input"
                                                            value={rule.validFrom || ''}
                                                            onChange={(e) => updateRule(index, { validFrom: e.target.value })}
                                                        />
                                                    </div>
                                                    <div className="form-group" style={{ flex: 1, marginBottom: 0 }}>
                                                        <label className="form-label">Valid Until</label>
                                                        <input
                                                            type="date"
                                                            className="form-input"
                                                            value={rule.validUntil || ''}
                                                            onChange={(e) => updateRule(index, { validUntil: e.target.value })}
                                                        />
                                                    </div>
                                                </div>
                                            )}
                                        </div>

                                        {/* Description */}
                                        <div className="form-group" style={{ marginTop: '16px', marginBottom: 0 }}>
                                            <label className="form-label">Description (optional)</label>
                                            <input
                                                className="form-input"
                                                value={rule.description || ''}
                                                onChange={(e) => updateRule(index, { description: e.target.value })}
                                                placeholder="Human-readable description of this condition"
                                            />
                                        </div>
                                    </div>
                                )}
                            </div>
                        );
                    })}
                </div>
            )}

            {/* Help Text */}
            <div style={{
                marginTop: '16px',
                padding: '12px 16px',
                background: 'rgba(var(--info-rgb, 100, 150, 255), 0.1)',
                borderRadius: '8px',
                fontSize: '13px',
                color: 'var(--text-muted)'
            }}>
                <strong>💡 How it works:</strong> All conditions must be satisfied (AND logic).
                Use expressions to compare attributes dynamically (e.g., "resource.branchId IN subject.branchIds").
            </div>
        </div>
    );
}
