import { useState, useEffect, useRef } from 'react';
import { useParams } from 'react-router-dom';
import { formDefinitions } from '../api';

// Helper function to get preview data from localStorage
function getLocalPreviewData(id) {
    const previewKey = `form_preview_${id}`;
    console.log('FormPreviewPage: Checking localStorage for key:', previewKey);
    const localData = localStorage.getItem(previewKey);

    if (localData) {
        try {
            const parsed = JSON.parse(localData);
            console.log('FormPreviewPage: Found localStorage data:', {
                hasForm: !!parsed.form,
                hasFields: !!parsed.fields,
                fieldsCount: parsed.fields?.length
            });

            if (parsed.form && parsed.fields && parsed.fields.length > 0) {
                console.log('FormPreviewPage: Returning localStorage data with', parsed.fields.length, 'fields');
                return parsed;
            }
        } catch (parseErr) {
            console.warn('FormPreviewPage: Parse error:', parseErr);
        }
    } else {
        console.log('FormPreviewPage: No localStorage data found');
    }
    return null;
}

export default function FormPreviewPage() {
    const { id } = useParams();

    // Use ref to cache localStorage data across StrictMode re-renders
    const localPreviewRef = useRef(null);
    if (localPreviewRef.current === null) {
        localPreviewRef.current = getLocalPreviewData(id) || false; // false means "checked but not found"
    }
    const localPreview = localPreviewRef.current === false ? null : localPreviewRef.current;

    const [form, setForm] = useState(localPreview?.form || null);
    const [fields, setFields] = useState(localPreview?.fields || []);
    const [loading, setLoading] = useState(!localPreview);
    const [error, setError] = useState('');
    const [formData, setFormData] = useState({});
    const [submitted, setSubmitted] = useState(false);
    const [errors, setErrors] = useState({});
    const [isLocalPreview] = useState(!!localPreview);

    // Clean up localStorage after successful mount with local data
    useEffect(() => {
        if (localPreview) {
            const previewKey = `form_preview_${id}`;
            localStorage.removeItem(previewKey);
            console.log('FormPreviewPage: Cleaned up localStorage after mount');
        }
    }, []); // Empty deps - run once on mount

    useEffect(() => {
        // Only load from API if we don't have local preview data
        if (!localPreview) {
            loadFormFromAPI();
        }
    }, [id, localPreview]);

    const loadFormFromAPI = async () => {
        try {
            setLoading(true);
            console.log('FormPreviewPage: Loading from API...');
            const data = await formDefinitions.get(id);
            setForm(data);
            setFields(data.fields || []);
            console.log('FormPreviewPage: Loaded from API with', data.fields?.length || 0, 'fields');
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    const handleChange = (fieldKey, value) => {
        setFormData({ ...formData, [fieldKey]: value });
        if (errors[fieldKey]) {
            setErrors({ ...errors, [fieldKey]: null });
        }
    };

    const handleSubmit = (e) => {
        e.preventDefault();

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

    if (loading) {
        return (
            <div className="loading">
                Loading form preview...
            </div>
        );
    }

    if (error) {
        return (
            <div style={{
                minHeight: '100vh',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexDirection: 'column',
                gap: 'var(--space-4)',
                background: 'var(--color-gray-50)'
            }}>
                <div style={{ color: 'var(--color-error)', fontSize: 'var(--font-size-lg)' }}>
                    ❌ Error: {error}
                </div>
                <button className="btn btn-secondary" onClick={() => window.close()}>
                    Close
                </button>
            </div>
        );
    }

    return (
        <div style={{
            minHeight: '100vh',
            background: 'var(--color-gray-100)'
        }}>
            {/* Preview Header */}
            <header style={{
                background: 'var(--color-gray-900)',
                color: 'var(--color-white)',
                padding: 'var(--space-3) var(--space-6)',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                position: 'sticky',
                top: 0,
                zIndex: 100
            }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-4)' }}>
                    <span style={{ fontSize: 'var(--font-size-lg)' }}>👁️</span>
                    <div>
                        <div style={{ fontWeight: 'var(--font-weight-semibold)' }}>Form Preview</div>
                        <div style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-gray-400)' }}>
                            {form?.name} • Version {form?.version}
                        </div>
                    </div>
                    <span className={`badge badge-${form?.status === 'ACTIVE' ? 'success' : 'warning'}`}>
                        {form?.status}
                    </span>
                    {isLocalPreview && (
                        <span className="badge badge-info">
                            ⚡ Unsaved Changes
                        </span>
                    )}
                </div>
                <div style={{ display: 'flex', gap: 'var(--space-3)' }}>
                    <button className="btn btn-secondary btn-sm" onClick={handleReset}>
                        🔄 Reset
                    </button>
                    <button className="btn btn-secondary btn-sm" onClick={() => window.close()}>
                        ✕ Close Preview
                    </button>
                </div>
            </header>

            {/* Main Content - Full Screen Width */}
            <main style={{
                maxWidth: '1200px',
                width: '100%',
                margin: '0 auto',
                padding: 'var(--space-6) var(--space-8)'
            }}>
                <div className="card">
                    {/* Form Title */}
                    <div style={{
                        borderBottom: '1px solid var(--color-gray-200)',
                        paddingBottom: 'var(--space-4)',
                        marginBottom: 'var(--space-6)'
                    }}>
                        <h1 style={{ marginBottom: 'var(--space-2)' }}>{form?.name}</h1>
                        {form?.description && (
                            <p style={{ color: 'var(--color-gray-500)', margin: 0 }}>
                                {form.description}
                            </p>
                        )}
                    </div>

                    {/* Form Content */}
                    {submitted ? (
                        <div style={{ textAlign: 'center', padding: 'var(--space-8)' }}>
                            <div style={{
                                width: '64px',
                                height: '64px',
                                borderRadius: '50%',
                                background: '#d1fae5',
                                color: '#059669',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                margin: '0 auto var(--space-4)',
                                fontSize: '32px'
                            }}>
                                ✓
                            </div>
                            <h2 style={{ color: 'var(--color-success)', marginBottom: 'var(--space-2)' }}>
                                Form Submitted Successfully!
                            </h2>
                            <p className="text-muted" style={{ marginBottom: 'var(--space-6)' }}>
                                This is a preview. In production, your data would be saved.
                            </p>

                            <details style={{ textAlign: 'left', marginBottom: 'var(--space-6)' }}>
                                <summary style={{
                                    cursor: 'pointer',
                                    fontWeight: 'var(--font-weight-medium)',
                                    padding: 'var(--space-3)',
                                    background: 'var(--color-gray-50)',
                                    borderRadius: 'var(--radius-md)'
                                }}>
                                    📋 View Submitted Data
                                </summary>
                                <pre style={{
                                    background: 'var(--color-gray-900)',
                                    color: '#4ade80',
                                    padding: 'var(--space-4)',
                                    borderRadius: '0 0 var(--radius-md) var(--radius-md)',
                                    overflow: 'auto',
                                    fontSize: 'var(--font-size-sm)',
                                    margin: 0
                                }}>
                                    {JSON.stringify(formData, null, 2)}
                                </pre>
                            </details>

                            <button className="btn btn-primary" onClick={handleReset}>
                                🔄 Submit Another Response
                            </button>
                        </div>
                    ) : (
                        <form onSubmit={handleSubmit}>
                            {fields.length === 0 ? (
                                <div className="empty-state">
                                    <div style={{ fontSize: '48px', marginBottom: 'var(--space-4)' }}>📝</div>
                                    <p>No fields have been added to this form yet.</p>
                                    <p className="text-muted" style={{ fontSize: 'var(--font-size-sm)' }}>
                                        Make sure to <strong>save</strong> the form after adding fields.
                                    </p>
                                </div>
                            ) : (
                                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 'var(--space-4)' }}>
                                    {fields.map(field => {
                                        // Get width percentage for grid
                                        const widthMap = {
                                            full: '100%',
                                            half: 'calc(50% - 8px)',
                                            third: 'calc(33.33% - 10px)',
                                            quarter: 'calc(25% - 12px)'
                                        };
                                        const fieldWidth = field.elementType === 'layout' ? '100%' : (widthMap[field.width] || '100%');

                                        // Render Section
                                        if (field.fieldType === 'SECTION') {
                                            return (
                                                <div key={field.id} style={{
                                                    width: '100%',
                                                    marginTop: 'var(--space-4)',
                                                    marginBottom: 'var(--space-2)'
                                                }}>
                                                    <div style={{
                                                        display: 'flex',
                                                        alignItems: 'center',
                                                        gap: 'var(--space-2)',
                                                        padding: 'var(--space-3) 0',
                                                        borderBottom: '2px solid var(--color-primary-200)'
                                                    }}>
                                                        <span style={{ color: 'var(--color-primary-600)' }}>📁</span>
                                                        <h3 style={{
                                                            margin: 0,
                                                            color: 'var(--color-primary-700)',
                                                            fontSize: 'var(--font-size-md)'
                                                        }}>{field.label}</h3>
                                                    </div>
                                                </div>
                                            );
                                        }

                                        // Render Divider
                                        if (field.fieldType === 'DIVIDER') {
                                            return (
                                                <div key={field.id} style={{
                                                    width: '100%',
                                                    padding: 'var(--space-2) 0'
                                                }}>
                                                    <div style={{
                                                        height: '1px',
                                                        background: 'var(--color-gray-200)'
                                                    }} />
                                                </div>
                                            );
                                        }

                                        // Render Heading
                                        if (field.fieldType === 'HEADING') {
                                            return (
                                                <div key={field.id} style={{
                                                    width: '100%',
                                                    marginTop: 'var(--space-2)'
                                                }}>
                                                    <h4 style={{
                                                        margin: 0,
                                                        color: 'var(--color-gray-700)',
                                                        fontSize: 'var(--font-size-base)',
                                                        fontWeight: 'var(--font-weight-semibold)'
                                                    }}>{field.label}</h4>
                                                </div>
                                            );
                                        }

                                        // Get field width - use customWidth if set
                                        const previewWidth = field.customWidth
                                            ? `${field.customWidth}px`
                                            : fieldWidth;

                                        // Regular field with label position support
                                        const isHorizontal = field.labelPosition === 'left';

                                        return (
                                            <div key={field.id} style={{ width: previewWidth }}>
                                                <div className="form-group" style={{
                                                    marginBottom: 0,
                                                    display: isHorizontal ? 'flex' : 'block',
                                                    alignItems: isHorizontal ? 'center' : 'stretch',
                                                    gap: isHorizontal ? 'var(--space-3)' : 0
                                                }}>
                                                    <label className="form-label" style={{
                                                        minWidth: isHorizontal ? '140px' : 'auto',
                                                        marginBottom: isHorizontal ? 0 : 'var(--space-2)',
                                                        flexShrink: 0
                                                    }}>
                                                        {field.label}
                                                        {field.required && (
                                                            <span style={{ color: 'var(--color-error)', marginLeft: '4px' }}>*</span>
                                                        )}
                                                    </label>

                                                    <div style={{ flex: isHorizontal ? 1 : 'none' }}>
                                                        {renderField(field, formData[field.fieldKey], (value) => handleChange(field.fieldKey, value), errors[field.fieldKey])}

                                                        {field.helpText && (
                                                            <div style={{
                                                                fontSize: 'var(--font-size-sm)',
                                                                color: 'var(--color-gray-500)',
                                                                marginTop: 'var(--space-1)'
                                                            }}>
                                                                {field.helpText}
                                                            </div>
                                                        )}
                                                        {errors[field.fieldKey] && (
                                                            <div className="form-error">
                                                                {errors[field.fieldKey]}
                                                            </div>
                                                        )}
                                                    </div>
                                                </div>
                                            </div>
                                        );
                                    })}

                                    <div style={{
                                        width: '100%',
                                        display: 'flex',
                                        gap: 'var(--space-3)',
                                        marginTop: 'var(--space-6)',
                                        paddingTop: 'var(--space-4)',
                                        borderTop: '1px solid var(--color-gray-200)'
                                    }}>
                                        <button type="submit" className="btn btn-primary">
                                            Submit Form
                                        </button>
                                        <button type="button" className="btn btn-secondary" onClick={handleReset}>
                                            Reset
                                        </button>
                                    </div>
                                </div>
                            )}
                        </form>
                    )}
                </div>

                {/* Footer */}
                <div style={{
                    textAlign: 'center',
                    marginTop: 'var(--space-6)',
                    fontSize: 'var(--font-size-sm)',
                    color: 'var(--color-gray-400)'
                }}>
                    Enterprise CAS • Form Preview
                </div>
            </main>
        </div>
    );
}

function renderField(field, value, onChange, hasError) {
    const inputClass = `form-input${hasError ? ' error' : ''}`;

    switch (field.fieldType) {
        case 'TEXT':
            return (
                <input
                    type="text"
                    className={inputClass}
                    placeholder={field.placeholder || `Enter ${field.label.toLowerCase()}...`}
                    value={value || ''}
                    onChange={(e) => onChange(e.target.value)}
                />
            );
        case 'TEXTAREA':
            return (
                <textarea
                    className={inputClass}
                    placeholder={field.placeholder || `Enter ${field.label.toLowerCase()}...`}
                    rows={4}
                    value={value || ''}
                    onChange={(e) => onChange(e.target.value)}
                />
            );
        case 'NUMBER':
            return (
                <input
                    type="number"
                    className={inputClass}
                    placeholder={field.placeholder || '0'}
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
                    className={inputClass}
                    value={value || ''}
                    onChange={(e) => onChange(e.target.value)}
                />
            );
        case 'DATETIME':
            return (
                <input
                    type="datetime-local"
                    className={inputClass}
                    value={value || ''}
                    onChange={(e) => onChange(e.target.value)}
                />
            );
        case 'DROPDOWN':
            return (
                <select
                    className="form-select"
                    value={value || ''}
                    onChange={(e) => onChange(e.target.value)}
                >
                    <option value="">{field.placeholder || 'Select an option...'}</option>
                    {field.options?.items?.map((opt, i) => (
                        <option key={i} value={opt.value}>{opt.label}</option>
                    ))}
                </select>
            );
        case 'MULTI_SELECT':
            return (
                <div style={{
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 'var(--space-2)',
                    padding: 'var(--space-3)',
                    background: 'var(--color-gray-50)',
                    borderRadius: 'var(--radius-md)',
                    border: '1px solid var(--color-gray-200)'
                }}>
                    {field.options?.items?.map((opt, i) => (
                        <label key={i} style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: 'var(--space-2)',
                            cursor: 'pointer'
                        }}>
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
                            <span>{opt.label}</span>
                        </label>
                    ))}
                </div>
            );
        case 'CHECKBOX':
            return (
                <label style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 'var(--space-2)',
                    cursor: 'pointer'
                }}>
                    <input
                        type="checkbox"
                        checked={value || false}
                        onChange={(e) => onChange(e.target.checked)}
                    />
                    <span>{field.label}</span>
                </label>
            );
        case 'RADIO':
            return (
                <div style={{
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 'var(--space-2)',
                    padding: 'var(--space-3)',
                    background: 'var(--color-gray-50)',
                    borderRadius: 'var(--radius-md)',
                    border: '1px solid var(--color-gray-200)'
                }}>
                    {field.options?.items?.map((opt, i) => (
                        <label key={i} style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: 'var(--space-2)',
                            cursor: 'pointer'
                        }}>
                            <input
                                type="radio"
                                name={field.fieldKey}
                                value={opt.value}
                                checked={value === opt.value}
                                onChange={(e) => onChange(e.target.value)}
                            />
                            <span>{opt.label}</span>
                        </label>
                    ))}
                </div>
            );
        case 'FILE':
            return (
                <div style={{
                    border: '2px dashed var(--color-gray-300)',
                    borderRadius: 'var(--radius-md)',
                    padding: 'var(--space-6)',
                    textAlign: 'center',
                    background: 'var(--color-gray-50)',
                    cursor: 'pointer'
                }}>
                    <input
                        type="file"
                        onChange={(e) => onChange(e.target.files?.[0]?.name || '')}
                        style={{ display: 'none' }}
                        id={`file-${field.fieldKey}`}
                    />
                    <label htmlFor={`file-${field.fieldKey}`} style={{ cursor: 'pointer' }}>
                        {value ? (
                            <>
                                <div style={{ fontSize: '24px', marginBottom: 'var(--space-2)' }}>📎</div>
                                <div style={{ color: 'var(--color-primary-600)', fontWeight: 'var(--font-weight-medium)' }}>
                                    {value}
                                </div>
                            </>
                        ) : (
                            <>
                                <div style={{ fontSize: '24px', marginBottom: 'var(--space-2)' }}>📤</div>
                                <div style={{ color: 'var(--color-gray-600)' }}>Click to upload file</div>
                            </>
                        )}
                    </label>
                </div>
            );
        case 'SIGNATURE':
            return (
                <div
                    onClick={() => onChange(value ? '' : 'signature_captured')}
                    style={{
                        border: `2px dashed ${value ? 'var(--color-success)' : 'var(--color-gray-300)'}`,
                        borderRadius: 'var(--radius-md)',
                        padding: 'var(--space-6)',
                        textAlign: 'center',
                        background: value ? '#d1fae5' : 'var(--color-gray-50)',
                        cursor: 'pointer',
                        transition: 'all 150ms ease'
                    }}
                >
                    {value ? (
                        <>
                            <div style={{ fontSize: '32px', marginBottom: 'var(--space-2)' }}>✍️</div>
                            <div style={{ color: 'var(--color-success)', fontWeight: 'var(--font-weight-medium)' }}>
                                Signature Captured
                            </div>
                            <div style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-gray-500)' }}>
                                Click to clear
                            </div>
                        </>
                    ) : (
                        <>
                            <div style={{ fontSize: '32px', marginBottom: 'var(--space-2)', opacity: 0.5 }}>✍️</div>
                            <div style={{ color: 'var(--color-gray-600)' }}>Click to sign</div>
                        </>
                    )}
                </div>
            );
        default:
            return (
                <input
                    type="text"
                    className={inputClass}
                    value={value || ''}
                    onChange={(e) => onChange(e.target.value)}
                />
            );
    }
}
