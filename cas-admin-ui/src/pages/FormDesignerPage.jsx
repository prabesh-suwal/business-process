import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { formDefinitions } from '../api';

// Standard field types
const FIELD_TYPES = [
    { value: 'TEXT', label: 'Text', icon: '📝' },
    { value: 'TEXTAREA', label: 'Text Area', icon: '📄' },
    { value: 'NUMBER', label: 'Number', icon: '🔢' },
    { value: 'DATE', label: 'Date', icon: '📅' },
    { value: 'DATETIME', label: 'Date & Time', icon: '🕐' },
    { value: 'DROPDOWN', label: 'Dropdown', icon: '📋' },
    { value: 'MULTI_SELECT', label: 'Multi-Select', icon: '☑️' },
    { value: 'CHECKBOX', label: 'Checkbox', icon: '✅' },
    { value: 'RADIO', label: 'Radio', icon: '🔘' },
    { value: 'FILE', label: 'File Upload', icon: '📎' },
    { value: 'SIGNATURE', label: 'Signature', icon: '✍️' },
];

// Layout elements for form structure
const LAYOUT_ELEMENTS = [
    { value: 'SECTION', label: 'Section', icon: '📁', description: 'Group of fields' },
    { value: 'DIVIDER', label: 'Divider', icon: '➖', description: 'Visual separator' },
    { value: 'HEADING', label: 'Heading', icon: '🏷️', description: 'Section header' },
];

// Width options for fields
const WIDTH_OPTIONS = [
    { value: 'full', label: 'Full Width', cols: 4 },
    { value: 'half', label: 'Half (1/2)', cols: 2 },
    { value: 'third', label: 'Third (1/3)', cols: 1.33 },
    { value: 'quarter', label: 'Quarter (1/4)', cols: 1 },
];

export default function FormDesignerPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [form, setForm] = useState(null);
    const [fields, setFields] = useState([]);
    const [selectedField, setSelectedField] = useState(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [hasChanges, setHasChanges] = useState(false);
    const [showPreview, setShowPreview] = useState(false);

    // Drag and drop state
    const [draggedIndex, setDraggedIndex] = useState(null);
    const [dragOverIndex, setDragOverIndex] = useState(null);

    // Drag and drop handlers
    const handleDragStart = (e, index) => {
        setDraggedIndex(index);
        e.dataTransfer.effectAllowed = 'move';
        e.dataTransfer.setData('text/plain', index);
    };

    const handleDragOver = (e, index) => {
        e.preventDefault();
        e.dataTransfer.dropEffect = 'move';
        if (draggedIndex !== null && index !== draggedIndex) {
            setDragOverIndex(index);
        }
    };

    const handleDragEnd = () => {
        setDraggedIndex(null);
        setDragOverIndex(null);
    };

    const handleDrop = (e, dropIndex) => {
        e.preventDefault();
        if (draggedIndex === null || draggedIndex === dropIndex) return;

        const newFields = [...fields];
        const [draggedField] = newFields.splice(draggedIndex, 1);
        newFields.splice(dropIndex, 0, draggedField);

        // Update display order
        newFields.forEach((field, idx) => {
            field.displayOrder = idx;
        });

        setFields(newFields);
        setHasChanges(true);
        setDraggedIndex(null);
        setDragOverIndex(null);
    };

    useEffect(() => {
        loadForm();
    }, [id]);

    const loadForm = async () => {
        try {
            setLoading(true);
            const data = await formDefinitions.get(id);
            setForm(data);
            setFields(data.fields || []);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    const addField = (type, isLayout = false) => {
        const fieldType = isLayout
            ? LAYOUT_ELEMENTS.find(f => f.value === type)
            : FIELD_TYPES.find(f => f.value === type);

        const newField = {
            id: `field_${Date.now()}`,
            fieldKey: `field_${fields.length + 1}`,
            fieldType: type,
            elementType: isLayout ? 'layout' : 'field',
            label: type === 'SECTION' ? 'New Section'
                : type === 'DIVIDER' ? ''
                    : type === 'HEADING' ? 'Section Heading'
                        : `New ${fieldType?.label || type} Field`,
            placeholder: '',
            helpText: '',
            required: false,
            width: 'full',
            customWidth: null,  // Custom width in pixels (null = use preset width)
            customHeight: null, // Custom height in pixels (null = auto)
            labelPosition: 'top', // 'top' (vertical) or 'left' (horizontal/inline)
            sectionId: null,
            displayOrder: fields.length,
            validationRules: {},
            options: type === 'DROPDOWN' || type === 'RADIO' || type === 'MULTI_SELECT'
                ? { items: [{ value: 'option1', label: 'Option 1' }] }
                : type === 'SECTION'
                    ? { collapsible: true, collapsed: false }
                    : null
        };
        setFields([...fields, newField]);
        setSelectedField(newField);
        setHasChanges(true);
    };

    // Render field action buttons (move up/down, delete)
    const renderFieldActions = (field, index) => {
        if (isReadOnly) return null;
        return (
            <div style={{ display: 'flex', gap: '4px' }} onClick={(e) => e.stopPropagation()}>
                <button
                    className="btn btn-sm btn-secondary"
                    onClick={() => moveField(index, 'up')}
                    disabled={index === 0}
                    title="Move up"
                >↑</button>
                <button
                    className="btn btn-sm btn-secondary"
                    onClick={() => moveField(index, 'down')}
                    disabled={index === fields.length - 1}
                    title="Move down"
                >↓</button>
                <button
                    className="btn btn-sm btn-danger"
                    onClick={() => removeField(field.id)}
                    title="Delete"
                >×</button>
            </div>
        );
    };

    const updateField = (fieldId, updates) => {
        setFields(fields.map(f => f.id === fieldId ? { ...f, ...updates } : f));
        if (selectedField?.id === fieldId) {
            setSelectedField({ ...selectedField, ...updates });
        }
        setHasChanges(true);
    };

    const removeField = (fieldId) => {
        setFields(fields.filter(f => f.id !== fieldId));
        if (selectedField?.id === fieldId) {
            setSelectedField(null);
        }
        setHasChanges(true);
    };

    const moveField = (index, direction) => {
        const newIndex = direction === 'up' ? index - 1 : index + 1;
        if (newIndex < 0 || newIndex >= fields.length) return;

        const newFields = [...fields];
        [newFields[index], newFields[newIndex]] = [newFields[newIndex], newFields[index]];
        newFields.forEach((f, i) => f.displayOrder = i);
        setFields(newFields);
        setHasChanges(true);
    };

    const handleSave = async () => {
        try {
            setSaving(true);
            setError('');

            // Build schema from fields
            const properties = {};
            const required = [];

            fields.forEach(field => {
                properties[field.fieldKey] = {
                    type: getSchemaType(field.fieldType),
                    title: field.label,
                    description: field.helpText
                };
                if (field.required) {
                    required.push(field.fieldKey);
                }
            });

            await formDefinitions.update(id, {
                name: form.name,
                description: form.description,
                schema: { type: 'object', properties, required },
                fields: fields.map(f => ({
                    fieldKey: f.fieldKey,
                    fieldType: f.fieldType,
                    elementType: f.elementType || 'field',
                    label: f.label,
                    placeholder: f.placeholder,
                    helpText: f.helpText,
                    required: f.required,
                    displayOrder: f.displayOrder,
                    width: f.width || 'full',
                    customWidth: f.customWidth || null,
                    customHeight: f.customHeight || null,
                    labelPosition: f.labelPosition || 'top',
                    sectionId: f.sectionId || null,
                    validationRules: f.validationRules,
                    options: f.options
                }))
            });

            setSuccess('Form saved successfully!');
            setHasChanges(false);
            setTimeout(() => setSuccess(''), 3000);
        } catch (err) {
            setError(err.message);
        } finally {
            setSaving(false);
        }
    };

    const handleActivate = async () => {
        if (hasChanges) {
            setError('Please save changes before activating');
            return;
        }
        try {
            await formDefinitions.activate(id);
            setSuccess('Form activated!');
            loadForm();
        } catch (err) {
            setError(err.message);
        }
    };

    const getSchemaType = (fieldType) => {
        const typeMap = {
            TEXT: 'string',
            TEXTAREA: 'string',
            NUMBER: 'number',
            DATE: 'string',
            DATETIME: 'string',
            DROPDOWN: 'string',
            MULTI_SELECT: 'array',
            CHECKBOX: 'boolean',
            RADIO: 'string',
            FILE: 'string',
            SIGNATURE: 'string'
        };
        return typeMap[fieldType] || 'string';
    };

    const isReadOnly = form?.status !== 'DRAFT';

    if (loading) {
        return (
            <div className="page-container">
                <div style={{ textAlign: 'center', padding: '48px' }}>Loading...</div>
            </div>
        );
    }

    return (
        <div className="page-container" style={{ maxWidth: 'none' }}>
            {/* Header */}
            <div className="page-header" style={{ marginBottom: '16px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                    <button className="btn btn-secondary" onClick={() => navigate('/forms')}>
                        ← Back
                    </button>
                    <div>
                        <h1 style={{ margin: 0 }}>{form?.name}</h1>
                        <div style={{ color: 'var(--text-secondary)', fontSize: '14px' }}>
                            Version {form?.version} •
                            <span style={{
                                marginLeft: '8px',
                                padding: '2px 8px',
                                borderRadius: '4px',
                                background: form?.status === 'ACTIVE' ? 'var(--success)' :
                                    form?.status === 'DRAFT' ? 'var(--warning)' : 'var(--text-secondary)',
                                color: form?.status === 'DRAFT' ? '#000' : '#fff',
                                fontSize: '12px'
                            }}>
                                {form?.status}
                            </span>
                        </div>
                    </div>
                </div>
                <div style={{ display: 'flex', gap: '12px' }}>
                    <button
                        className="btn btn-secondary"
                        onClick={() => {
                            // Save current fields to localStorage for preview before save
                            // localStorage is shared between tabs, sessionStorage is NOT
                            const previewKey = `form_preview_${id}`;
                            const previewData = JSON.stringify({
                                form: form,
                                fields: fields,
                                timestamp: Date.now()
                            });
                            localStorage.setItem(previewKey, previewData);
                            console.log('Preview data stored:', previewKey, 'Fields:', fields.length);
                            window.open(`/forms/${id}/preview?t=${Date.now()}`, '_blank');
                        }}
                        disabled={fields.length === 0}
                        title="Preview form in new tab (works before save)"
                    >
                        👁️ Preview
                    </button>
                    {!isReadOnly && (
                        <>
                            <button
                                className="btn btn-secondary"
                                onClick={handleSave}
                                disabled={saving || !hasChanges}
                            >
                                {saving ? 'Saving...' : '💾 Save'}
                            </button>
                            <button
                                className="btn btn-success"
                                onClick={handleActivate}
                                disabled={saving || hasChanges}
                            >
                                ✓ Activate
                            </button>
                        </>
                    )}
                </div>
            </div>

            {error && <div className="alert alert-danger">{error}</div>}
            {success && <div className="alert alert-success">{success}</div>}

            <div style={{ display: 'flex', gap: '16px', height: 'calc(100vh - 200px)' }}>
                {/* Field Palette */}
                {!isReadOnly && (
                    <div className="card" style={{ width: '200px', flexShrink: 0, overflowY: 'auto' }}>
                        {/* Layout Elements */}
                        <div style={{ marginBottom: '16px' }}>
                            <h4 style={{ fontSize: '12px', color: 'var(--color-gray-500)', marginBottom: '8px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                                Layout
                            </h4>
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                                {LAYOUT_ELEMENTS.map(type => (
                                    <button
                                        key={type.value}
                                        className="btn btn-secondary"
                                        style={{
                                            justifyContent: 'flex-start',
                                            padding: '6px 10px',
                                            fontSize: '12px',
                                            background: 'var(--color-primary-50)',
                                            borderColor: 'var(--color-primary-200)'
                                        }}
                                        onClick={() => addField(type.value, true)}
                                        title={type.description}
                                    >
                                        {type.icon} {type.label}
                                    </button>
                                ))}
                            </div>
                        </div>

                        {/* Field Types */}
                        <div>
                            <h4 style={{ fontSize: '12px', color: 'var(--color-gray-500)', marginBottom: '8px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                                Fields
                            </h4>
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                                {FIELD_TYPES.map(type => (
                                    <button
                                        key={type.value}
                                        className="btn btn-secondary"
                                        style={{
                                            justifyContent: 'flex-start',
                                            padding: '6px 10px',
                                            fontSize: '12px'
                                        }}
                                        onClick={() => addField(type.value, false)}
                                    >
                                        {type.icon} {type.label}
                                    </button>
                                ))}
                            </div>
                        </div>
                    </div>
                )}

                {/* Form Canvas */}
                <div className="card" style={{ flex: 1, overflowY: 'auto' }}>
                    <h3 style={{ fontSize: '16px', marginBottom: '16px' }}>Form Canvas</h3>

                    {fields.length === 0 ? (
                        <div style={{
                            textAlign: 'center',
                            padding: '48px',
                            color: 'var(--color-gray-400)',
                            border: '2px dashed var(--color-gray-300)',
                            borderRadius: '8px'
                        }}>
                            <div style={{ fontSize: '32px', marginBottom: '12px' }}>📝</div>
                            <div>Click elements from the palette to build your form</div>
                            <div style={{ fontSize: '13px', marginTop: '8px', color: 'var(--color-gray-400)' }}>
                                Start with a Section to group related fields
                            </div>
                        </div>
                    ) : (
                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '12px' }}>
                            {fields.map((field, index) => {
                                // Get width - use customWidth if set, else preset
                                const widthMap = { full: '100%', half: 'calc(50% - 6px)', third: 'calc(33.33% - 8px)', quarter: 'calc(25% - 9px)' };
                                const fieldWidth = field.customWidth
                                    ? `${field.customWidth}px`
                                    : (field.elementType === 'layout' ? '100%' : (widthMap[field.width] || '100%'));

                                // Render layout elements differently
                                if (field.fieldType === 'SECTION') {
                                    return (
                                        <div
                                            key={field.id}
                                            draggable={!isReadOnly}
                                            onDragStart={(e) => handleDragStart(e, index)}
                                            onDragOver={(e) => handleDragOver(e, index)}
                                            onDragEnd={handleDragEnd}
                                            onDrop={(e) => handleDrop(e, index)}
                                            onClick={() => setSelectedField(field)}
                                            style={{
                                                width: '100%',
                                                padding: '12px 16px',
                                                background: 'var(--color-primary-50)',
                                                border: selectedField?.id === field.id ? '2px solid var(--color-primary-500)' : '1px solid var(--color-primary-200)',
                                                borderRadius: '8px',
                                                cursor: !isReadOnly ? 'grab' : 'pointer',
                                                display: 'flex',
                                                justifyContent: 'space-between',
                                                alignItems: 'center',
                                                opacity: draggedIndex === index ? 0.5 : 1,
                                                borderTop: dragOverIndex === index ? '3px solid var(--color-primary-500)' : undefined
                                            }}
                                        >
                                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                                <span>📁</span>
                                                <strong style={{ color: 'var(--color-primary-700)' }}>{field.label}</strong>
                                                {field.options?.collapsible && <span style={{ fontSize: '11px', color: 'var(--color-gray-500)' }}>(collapsible)</span>}
                                            </div>
                                            {renderFieldActions(field, index)}
                                        </div>
                                    );
                                }

                                if (field.fieldType === 'DIVIDER') {
                                    return (
                                        <div
                                            key={field.id}
                                            draggable={!isReadOnly}
                                            onDragStart={(e) => handleDragStart(e, index)}
                                            onDragOver={(e) => handleDragOver(e, index)}
                                            onDragEnd={handleDragEnd}
                                            onDrop={(e) => handleDrop(e, index)}
                                            onClick={() => setSelectedField(field)}
                                            style={{
                                                width: '100%',
                                                padding: '8px 0',
                                                cursor: !isReadOnly ? 'grab' : 'pointer',
                                                display: 'flex',
                                                alignItems: 'center',
                                                gap: '8px',
                                                border: selectedField?.id === field.id ? '1px dashed var(--color-primary-500)' : '1px dashed transparent',
                                                borderRadius: '4px',
                                                opacity: draggedIndex === index ? 0.5 : 1,
                                                borderTop: dragOverIndex === index ? '3px solid var(--color-primary-500)' : undefined
                                            }}
                                        >
                                            <div style={{ flex: 1, height: '1px', background: 'var(--color-gray-300)' }} />
                                            {!isReadOnly && renderFieldActions(field, index)}
                                        </div>
                                    );
                                }

                                if (field.fieldType === 'HEADING') {
                                    return (
                                        <div
                                            key={field.id}
                                            draggable={!isReadOnly}
                                            onDragStart={(e) => handleDragStart(e, index)}
                                            onDragOver={(e) => handleDragOver(e, index)}
                                            onDragEnd={handleDragEnd}
                                            onDrop={(e) => handleDrop(e, index)}
                                            onClick={() => setSelectedField(field)}
                                            style={{
                                                width: '100%',
                                                padding: '8px 12px',
                                                cursor: !isReadOnly ? 'grab' : 'pointer',
                                                display: 'flex',
                                                justifyContent: 'space-between',
                                                alignItems: 'center',
                                                border: selectedField?.id === field.id ? '1px solid var(--color-primary-500)' : '1px solid transparent',
                                                borderRadius: '4px',
                                                opacity: draggedIndex === index ? 0.5 : 1,
                                                borderTop: dragOverIndex === index ? '3px solid var(--color-primary-500)' : undefined
                                            }}
                                        >
                                            <h4 style={{ margin: 0, color: 'var(--color-gray-700)' }}>🏷️ {field.label}</h4>
                                            {!isReadOnly && renderFieldActions(field, index)}
                                        </div>
                                    );
                                }

                                // Regular field with resize handle
                                return (
                                    <div
                                        key={field.id}
                                        draggable={!isReadOnly}
                                        onDragStart={(e) => handleDragStart(e, index)}
                                        onDragOver={(e) => handleDragOver(e, index)}
                                        onDragEnd={handleDragEnd}
                                        onDrop={(e) => handleDrop(e, index)}
                                        onClick={() => setSelectedField(field)}
                                        style={{
                                            width: fieldWidth,
                                            padding: '12px',
                                            border: selectedField?.id === field.id
                                                ? '2px solid var(--color-primary-500)'
                                                : '1px solid var(--color-gray-200)',
                                            borderRadius: '8px',
                                            cursor: !isReadOnly ? 'grab' : 'pointer',
                                            background: selectedField?.id === field.id
                                                ? 'var(--color-primary-50)'
                                                : 'var(--color-white)',
                                            position: 'relative',
                                            resize: !isReadOnly ? 'both' : 'none',
                                            overflow: 'auto',
                                            minWidth: '100px',
                                            minHeight: field.customHeight ? `${field.customHeight}px` : '60px',
                                            maxWidth: '100%',
                                            opacity: draggedIndex === index ? 0.5 : 1,
                                            borderTop: dragOverIndex === index ? '3px solid var(--color-primary-500)' : undefined
                                        }}
                                        onMouseUp={(e) => {
                                            // Capture resize from CSS resize handle
                                            if (!isReadOnly && e.currentTarget) {
                                                const newWidth = e.currentTarget.offsetWidth;
                                                const newHeight = e.currentTarget.offsetHeight;
                                                const updates = {};
                                                if (newWidth !== field.customWidth && newWidth > 100) {
                                                    updates.customWidth = newWidth;
                                                }
                                                if (newHeight !== field.customHeight && newHeight > 40) {
                                                    updates.customHeight = newHeight;
                                                }
                                                if (Object.keys(updates).length > 0) {
                                                    updateField(field.id, updates);
                                                }
                                            }
                                        }}
                                    >
                                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', marginBottom: '8px' }}>
                                            <div style={{ flex: 1 }}>
                                                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '4px', flexWrap: 'wrap' }}>
                                                    <label style={{ fontWeight: 500, fontSize: '13px' }}>
                                                        {field.label}
                                                        {field.required && <span style={{ color: 'var(--color-error)', marginLeft: '2px' }}>*</span>}
                                                    </label>
                                                    <span style={{
                                                        fontSize: '10px',
                                                        padding: '1px 5px',
                                                        background: 'var(--color-gray-100)',
                                                        borderRadius: '3px',
                                                        color: 'var(--color-gray-500)'
                                                    }}>
                                                        {field.customWidth ? `${field.customWidth}px` : (field.width || 'full')}
                                                    </span>
                                                    <span style={{
                                                        fontSize: '10px',
                                                        padding: '1px 5px',
                                                        background: field.labelPosition === 'left' ? 'var(--color-warning-100)' : 'var(--color-gray-100)',
                                                        borderRadius: '3px',
                                                        color: field.labelPosition === 'left' ? 'var(--color-warning-700)' : 'var(--color-gray-500)'
                                                    }}>
                                                        {field.labelPosition === 'left' ? '↔ horizontal' : '↕ vertical'}
                                                    </span>
                                                </div>
                                                <div style={{ fontSize: '11px', color: 'var(--color-gray-400)' }}>
                                                    {FIELD_TYPES.find(t => t.value === field.fieldType)?.icon} {field.fieldType}
                                                </div>
                                            </div>
                                            {!isReadOnly && renderFieldActions(field, index)}
                                        </div>
                                        <div style={{ marginTop: '8px' }}>
                                            {renderFieldPreview(field)}
                                        </div>
                                        {field.helpText && (
                                            <div style={{ fontSize: '11px', color: 'var(--color-gray-400)', marginTop: '4px' }}>
                                                {field.helpText}
                                            </div>
                                        )}
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </div>

                {/* Properties Panel */}
                <div className="card" style={{ width: '300px', flexShrink: 0, overflowY: 'auto' }}>
                    <h3 style={{ fontSize: '16px', marginBottom: '16px' }}>Field Properties</h3>

                    {selectedField ? (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                            <div className="form-group">
                                <label>Field Key</label>
                                <input
                                    type="text"
                                    className="form-control"
                                    value={selectedField.fieldKey}
                                    onChange={(e) => updateField(selectedField.id, { fieldKey: e.target.value })}
                                    disabled={isReadOnly}
                                />
                            </div>
                            <div className="form-group">
                                <label>Label</label>
                                <input
                                    type="text"
                                    className="form-control"
                                    value={selectedField.label}
                                    onChange={(e) => updateField(selectedField.id, { label: e.target.value })}
                                    disabled={isReadOnly}
                                />
                            </div>
                            <div className="form-group">
                                <label>Placeholder</label>
                                <input
                                    type="text"
                                    className="form-control"
                                    value={selectedField.placeholder || ''}
                                    onChange={(e) => updateField(selectedField.id, { placeholder: e.target.value })}
                                    disabled={isReadOnly}
                                />
                            </div>
                            <div className="form-group">
                                <label>Help Text</label>
                                <input
                                    type="text"
                                    className="form-control"
                                    value={selectedField.helpText || ''}
                                    onChange={(e) => updateField(selectedField.id, { helpText: e.target.value })}
                                    disabled={isReadOnly}
                                />
                            </div>
                            <div className="form-group">
                                <label style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                    <input
                                        type="checkbox"
                                        checked={selectedField.required || false}
                                        onChange={(e) => updateField(selectedField.id, { required: e.target.checked })}
                                        disabled={isReadOnly}
                                    />
                                    Required
                                </label>
                            </div>

                            {/* Width selector - only for non-layout elements */}
                            {selectedField.elementType !== 'layout' && (
                                <div className="form-group">
                                    <label>Width</label>
                                    <select
                                        className="form-select"
                                        value={selectedField.width || 'full'}
                                        onChange={(e) => updateField(selectedField.id, { width: e.target.value })}
                                        disabled={isReadOnly}
                                    >
                                        {WIDTH_OPTIONS.map(opt => (
                                            <option key={opt.value} value={opt.value}>{opt.label}</option>
                                        ))}
                                    </select>
                                    <div style={{ fontSize: '11px', color: 'var(--color-gray-500)', marginTop: '4px' }}>
                                        Controls how much horizontal space the field takes
                                    </div>
                                </div>
                            )}

                            {/* Section options */}
                            {selectedField.fieldType === 'SECTION' && (
                                <div className="form-group">
                                    <label style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                        <input
                                            type="checkbox"
                                            checked={selectedField.options?.collapsible || false}
                                            onChange={(e) => updateField(selectedField.id, {
                                                options: { ...selectedField.options, collapsible: e.target.checked }
                                            })}
                                            disabled={isReadOnly}
                                        />
                                        Collapsible
                                    </label>
                                </div>
                            )}

                            {/* Label Position - only for regular fields */}
                            {selectedField.elementType !== 'layout' && (
                                <div className="form-group">
                                    <label>Label Position</label>
                                    <select
                                        className="form-select"
                                        value={selectedField.labelPosition || 'top'}
                                        onChange={(e) => updateField(selectedField.id, { labelPosition: e.target.value })}
                                        disabled={isReadOnly}
                                    >
                                        <option value="top">Top (Vertical)</option>
                                        <option value="left">Left (Horizontal)</option>
                                    </select>
                                    <div style={{ fontSize: '11px', color: 'var(--color-gray-500)', marginTop: '4px' }}>
                                        Label above input vs. label beside input
                                    </div>
                                </div>
                            )}

                            {/* Custom Width - only for regular fields */}
                            {selectedField.elementType !== 'layout' && (
                                <div className="form-group">
                                    <label>Custom Width (px)</label>
                                    <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                                        <input
                                            type="number"
                                            className="form-control"
                                            value={selectedField.customWidth || ''}
                                            onChange={(e) => updateField(selectedField.id, {
                                                customWidth: e.target.value ? parseInt(e.target.value) : null
                                            })}
                                            placeholder="Auto"
                                            min={100}
                                            max={800}
                                            disabled={isReadOnly}
                                        />
                                        {selectedField.customWidth && (
                                            <button
                                                className="btn btn-sm btn-secondary"
                                                onClick={() => updateField(selectedField.id, { customWidth: null })}
                                                title="Reset to auto"
                                            >
                                                Reset
                                            </button>
                                        )}
                                    </div>
                                    <div style={{ fontSize: '11px', color: 'var(--color-gray-500)', marginTop: '4px' }}>
                                        Leave empty for auto-width. Drag corners to resize.
                                    </div>
                                </div>
                            )}

                            {/* Custom Height - only for regular fields */}
                            {selectedField.elementType !== 'layout' && (
                                <div className="form-group">
                                    <label>Custom Height (px)</label>
                                    <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                                        <input
                                            type="number"
                                            className="form-control"
                                            value={selectedField.customHeight || ''}
                                            onChange={(e) => updateField(selectedField.id, {
                                                customHeight: e.target.value ? parseInt(e.target.value) : null
                                            })}
                                            placeholder="Auto"
                                            min={40}
                                            max={400}
                                            disabled={isReadOnly}
                                        />
                                        {selectedField.customHeight && (
                                            <button
                                                className="btn btn-sm btn-secondary"
                                                onClick={() => updateField(selectedField.id, { customHeight: null })}
                                                title="Reset to auto"
                                            >
                                                Reset
                                            </button>
                                        )}
                                    </div>
                                    <div style={{ fontSize: '11px', color: 'var(--color-gray-500)', marginTop: '4px' }}>
                                        Leave empty for auto-height. Useful for textareas.
                                    </div>
                                </div>
                            )}

                            {/* Options for dropdown/radio/multi-select */}
                            {['DROPDOWN', 'RADIO', 'MULTI_SELECT'].includes(selectedField.fieldType) && (
                                <div className="form-group">
                                    <label>Options (one per line)</label>
                                    <textarea
                                        className="form-control"
                                        rows={4}
                                        value={selectedField.options?.items?.map(i => i.label).join('\n') || ''}
                                        onChange={(e) => {
                                            const items = e.target.value.split('\n').filter(Boolean).map((label, i) => ({
                                                value: `option${i + 1}`,
                                                label: label.trim()
                                            }));
                                            updateField(selectedField.id, { options: { items } });
                                        }}
                                        disabled={isReadOnly}
                                    />
                                </div>
                            )}

                            {/* Validation Rules */}
                            <div className="form-group">
                                <label>Validation</label>
                                {selectedField.fieldType === 'TEXT' && (
                                    <>
                                        <div style={{ display: 'flex', gap: '8px', marginTop: '8px' }}>
                                            <input
                                                type="number"
                                                className="form-control"
                                                placeholder="Min length"
                                                value={selectedField.validationRules?.minLength || ''}
                                                onChange={(e) => updateField(selectedField.id, {
                                                    validationRules: {
                                                        ...selectedField.validationRules,
                                                        minLength: e.target.value ? parseInt(e.target.value) : undefined
                                                    }
                                                })}
                                                disabled={isReadOnly}
                                            />
                                            <input
                                                type="number"
                                                className="form-control"
                                                placeholder="Max length"
                                                value={selectedField.validationRules?.maxLength || ''}
                                                onChange={(e) => updateField(selectedField.id, {
                                                    validationRules: {
                                                        ...selectedField.validationRules,
                                                        maxLength: e.target.value ? parseInt(e.target.value) : undefined
                                                    }
                                                })}
                                                disabled={isReadOnly}
                                            />
                                        </div>
                                    </>
                                )}
                                {selectedField.fieldType === 'NUMBER' && (
                                    <div style={{ display: 'flex', gap: '8px', marginTop: '8px' }}>
                                        <input
                                            type="number"
                                            className="form-control"
                                            placeholder="Min value"
                                            value={selectedField.validationRules?.min || ''}
                                            onChange={(e) => updateField(selectedField.id, {
                                                validationRules: {
                                                    ...selectedField.validationRules,
                                                    min: e.target.value ? parseFloat(e.target.value) : undefined
                                                }
                                            })}
                                            disabled={isReadOnly}
                                        />
                                        <input
                                            type="number"
                                            className="form-control"
                                            placeholder="Max value"
                                            value={selectedField.validationRules?.max || ''}
                                            onChange={(e) => updateField(selectedField.id, {
                                                validationRules: {
                                                    ...selectedField.validationRules,
                                                    max: e.target.value ? parseFloat(e.target.value) : undefined
                                                }
                                            })}
                                            disabled={isReadOnly}
                                        />
                                    </div>
                                )}
                            </div>
                        </div>
                    ) : (
                        <div style={{ color: 'var(--text-secondary)', textAlign: 'center', padding: '24px' }}>
                            Select a field to edit its properties
                        </div>
                    )}
                </div>
            </div>

            {/* Preview Modal */}
            {showPreview && (
                <div
                    style={{
                        position: 'fixed',
                        top: 0,
                        left: 0,
                        right: 0,
                        bottom: 0,
                        background: 'rgba(0, 0, 0, 0.5)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        zIndex: 1000
                    }}
                    onClick={() => setShowPreview(false)}
                >
                    <div
                        className="card"
                        style={{
                            width: '600px',
                            maxWidth: '90vw',
                            maxHeight: '90vh',
                            overflow: 'auto'
                        }}
                        onClick={(e) => e.stopPropagation()}
                    >
                        <div style={{
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center',
                            marginBottom: '24px'
                        }}>
                            <h2 style={{ margin: 0 }}>📝 {form?.name}</h2>
                            <button
                                className="btn btn-secondary"
                                onClick={() => setShowPreview(false)}
                            >
                                ✕ Close
                            </button>
                        </div>

                        {form?.description && (
                            <p style={{ color: 'var(--text-secondary)', marginBottom: '24px' }}>
                                {form.description}
                            </p>
                        )}

                        <FormPreview fields={fields} />
                    </div>
                </div>
            )}
        </div>
    );
}

function renderFieldPreview(field) {
    const style = {
        width: '100%',
        padding: '8px 12px',
        border: '1px solid var(--border-color)',
        borderRadius: '4px',
        background: 'var(--bg-secondary)'
    };

    switch (field.fieldType) {
        case 'TEXT':
            return <input type="text" placeholder={field.placeholder} style={style} disabled />;
        case 'TEXTAREA':
            return <textarea placeholder={field.placeholder} rows={3} style={style} disabled />;
        case 'NUMBER':
            return <input type="number" placeholder={field.placeholder} style={style} disabled />;
        case 'DATE':
            return <input type="date" style={style} disabled />;
        case 'DATETIME':
            return <input type="datetime-local" style={style} disabled />;
        case 'DROPDOWN':
            return (
                <select style={style} disabled>
                    <option>{field.placeholder || 'Select...'}</option>
                    {field.options?.items?.map((opt, i) => (
                        <option key={i}>{opt.label}</option>
                    ))}
                </select>
            );
        case 'CHECKBOX':
            return (
                <label style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <input type="checkbox" disabled /> {field.label}
                </label>
            );
        case 'RADIO':
            return (
                <div>
                    {field.options?.items?.map((opt, i) => (
                        <label key={i} style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
                            <input type="radio" name={field.fieldKey} disabled /> {opt.label}
                        </label>
                    ))}
                </div>
            );
        case 'FILE':
            return <input type="file" style={style} disabled />;
        case 'SIGNATURE':
            return (
                <div style={{ ...style, height: '80px', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-secondary)' }}>
                    ✍️ Signature pad
                </div>
            );
        default:
            return <input type="text" style={style} disabled />;
    }
}

// Interactive Form Preview Component
function FormPreview({ fields }) {
    const [formData, setFormData] = useState({});
    const [submitted, setSubmitted] = useState(false);
    const [errors, setErrors] = useState({});

    const handleChange = (fieldKey, value) => {
        setFormData({ ...formData, [fieldKey]: value });
        // Clear error when user starts typing
        if (errors[fieldKey]) {
            setErrors({ ...errors, [fieldKey]: null });
        }
    };

    const handleSubmit = (e) => {
        e.preventDefault();

        // Validate required fields
        const newErrors = {};
        fields.forEach(field => {
            if (field.required) {
                const value = formData[field.fieldKey];
                if (value === undefined || value === '' || value === null) {
                    newErrors[field.fieldKey] = `${field.label} is required`;
                }
            }
        });

        if (Object.keys(newErrors).length > 0) {
            setErrors(newErrors);
            return;
        }

        setSubmitted(true);
    };

    const handleReset = () => {
        setFormData({});
        setSubmitted(false);
        setErrors({});
    };

    if (submitted) {
        return (
            <div style={{ textAlign: 'center', padding: '24px' }}>
                <div style={{ fontSize: '48px', marginBottom: '16px' }}>✅</div>
                <h3>Form Submitted Successfully!</h3>
                <p style={{ color: 'var(--text-secondary)' }}>
                    This is a preview. In production, this data would be saved.
                </p>
                <details style={{ textAlign: 'left', marginTop: '16px' }}>
                    <summary style={{ cursor: 'pointer', fontWeight: 500 }}>View Submitted Data</summary>
                    <pre style={{
                        background: 'var(--bg-tertiary)',
                        padding: '12px',
                        borderRadius: '4px',
                        overflow: 'auto',
                        fontSize: '12px',
                        marginTop: '8px'
                    }}>
                        {JSON.stringify(formData, null, 2)}
                    </pre>
                </details>
                <button
                    className="btn btn-secondary"
                    onClick={handleReset}
                    style={{ marginTop: '16px' }}
                >
                    🔄 Try Again
                </button>
            </div>
        );
    }

    return (
        <form onSubmit={handleSubmit}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
                {fields.map(field => (
                    <div key={field.id} className="form-group">
                        <label style={{ fontWeight: 500, marginBottom: '6px', display: 'block' }}>
                            {field.label}
                            {field.required && <span style={{ color: 'var(--danger)' }}> *</span>}
                        </label>

                        {renderInteractiveField(field, formData[field.fieldKey], (value) => handleChange(field.fieldKey, value))}

                        {field.helpText && (
                            <div style={{ fontSize: '12px', color: 'var(--text-secondary)', marginTop: '4px' }}>
                                {field.helpText}
                            </div>
                        )}
                        {errors[field.fieldKey] && (
                            <div style={{ fontSize: '12px', color: 'var(--danger)', marginTop: '4px' }}>
                                {errors[field.fieldKey]}
                            </div>
                        )}
                    </div>
                ))}
            </div>

            <div style={{ marginTop: '24px', display: 'flex', gap: '12px' }}>
                <button type="submit" className="btn btn-primary">
                    Submit
                </button>
                <button type="button" className="btn btn-secondary" onClick={handleReset}>
                    Reset
                </button>
            </div>
        </form>
    );
}

function renderInteractiveField(field, value, onChange) {
    const style = {
        width: '100%',
        padding: '10px 12px',
        border: '1px solid var(--border-color)',
        borderRadius: '6px',
        background: 'var(--bg-secondary)',
        fontSize: '14px'
    };

    switch (field.fieldType) {
        case 'TEXT':
            return (
                <input
                    type="text"
                    placeholder={field.placeholder}
                    style={style}
                    value={value || ''}
                    onChange={(e) => onChange(e.target.value)}
                />
            );
        case 'TEXTAREA':
            return (
                <textarea
                    placeholder={field.placeholder}
                    rows={4}
                    style={style}
                    value={value || ''}
                    onChange={(e) => onChange(e.target.value)}
                />
            );
        case 'NUMBER':
            return (
                <input
                    type="number"
                    placeholder={field.placeholder}
                    style={style}
                    value={value || ''}
                    onChange={(e) => onChange(e.target.value)}
                    min={field.validationRules?.min}
                    max={field.validationRules?.max}
                />
            );
        case 'DATE':
            return (
                <input
                    type="date"
                    style={style}
                    value={value || ''}
                    onChange={(e) => onChange(e.target.value)}
                />
            );
        case 'DATETIME':
            return (
                <input
                    type="datetime-local"
                    style={style}
                    value={value || ''}
                    onChange={(e) => onChange(e.target.value)}
                />
            );
        case 'DROPDOWN':
            return (
                <select
                    style={style}
                    value={value || ''}
                    onChange={(e) => onChange(e.target.value)}
                >
                    <option value="">{field.placeholder || 'Select...'}</option>
                    {field.options?.items?.map((opt, i) => (
                        <option key={i} value={opt.value}>{opt.label}</option>
                    ))}
                </select>
            );
        case 'MULTI_SELECT':
            return (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    {field.options?.items?.map((opt, i) => (
                        <label key={i} style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <input
                                type="checkbox"
                                checked={(value || []).includes(opt.value)}
                                onChange={(e) => {
                                    const current = value || [];
                                    if (e.target.checked) {
                                        onChange([...current, opt.value]);
                                    } else {
                                        onChange(current.filter(v => v !== opt.value));
                                    }
                                }}
                            />
                            {opt.label}
                        </label>
                    ))}
                </div>
            );
        case 'CHECKBOX':
            return (
                <label style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <input
                        type="checkbox"
                        checked={value || false}
                        onChange={(e) => onChange(e.target.checked)}
                    />
                    {field.label}
                </label>
            );
        case 'RADIO':
            return (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    {field.options?.items?.map((opt, i) => (
                        <label key={i} style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <input
                                type="radio"
                                name={field.fieldKey}
                                value={opt.value}
                                checked={value === opt.value}
                                onChange={(e) => onChange(e.target.value)}
                            />
                            {opt.label}
                        </label>
                    ))}
                </div>
            );
        case 'FILE':
            return (
                <input
                    type="file"
                    style={style}
                    onChange={(e) => onChange(e.target.files?.[0]?.name || '')}
                />
            );
        case 'SIGNATURE':
            return (
                <div
                    style={{
                        ...style,
                        height: '100px',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: 'var(--text-secondary)',
                        cursor: 'pointer'
                    }}
                    onClick={() => onChange('signature_captured')}
                >
                    {value ? '✅ Signature Captured' : '✍️ Click to sign'}
                </div>
            );
        default:
            return (
                <input
                    type="text"
                    style={style}
                    value={value || ''}
                    onChange={(e) => onChange(e.target.value)}
                />
            );
    }
}
