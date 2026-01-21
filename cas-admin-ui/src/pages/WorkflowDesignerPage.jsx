import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { processTemplates, formDefinitions } from '../api';
import BpmnDesigner from '../components/BpmnDesigner';

export default function WorkflowDesignerPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [template, setTemplate] = useState(null);
    const [bpmnXml, setBpmnXml] = useState('');
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [hasChanges, setHasChanges] = useState(false);
    const [forms, setForms] = useState([]);

    useEffect(() => {
        loadTemplate();
    }, [id]);

    const loadTemplate = async () => {
        try {
            setLoading(true);
            const data = await processTemplates.get(id);
            setTemplate(data);
            setBpmnXml(data.bpmnXml || '');

            // Load available forms for linking
            if (data.productId) {
                try {
                    const formData = await formDefinitions.list(data.productId, true);
                    setForms(formData || []);
                } catch {
                    // Form service might not be running
                    setForms([]);
                }
            }
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    const handleXmlChange = (xml) => {
        setBpmnXml(xml);
        setHasChanges(true);
    };

    const handleSave = async (xml) => {
        try {
            setSaving(true);
            setError('');
            await processTemplates.update(id, { bpmnXml: xml || bpmnXml });
            setSuccess('Workflow saved successfully!');
            setHasChanges(false);
            setTimeout(() => setSuccess(''), 3000);
        } catch (err) {
            setError(err.message);
        } finally {
            setSaving(false);
        }
    };

    const handleDeploy = async () => {
        if (hasChanges) {
            setError('Please save changes before deploying');
            return;
        }
        if (!confirm('Deploy this workflow? Once deployed, it becomes active and cannot be edited.')) {
            return;
        }
        try {
            setSaving(true);
            await processTemplates.deploy(id);
            setSuccess('Workflow deployed successfully!');
            loadTemplate();
        } catch (err) {
            setError(err.message);
        } finally {
            setSaving(false);
        }
    };

    const handleCreateNewVersion = async () => {
        if (!confirm('Create a new draft version based on this workflow?')) {
            return;
        }
        try {
            setSaving(true);
            setError('');
            const newVersion = await processTemplates.createNewVersion(id);
            setSuccess(`New version ${newVersion.version} created!`);
            // Navigate to the new version for editing
            navigate(`/workflows/${newVersion.id}/design`);
        } catch (err) {
            setError(err.message);
        } finally {
            setSaving(false);
        }
    };

    const isReadOnly = template?.status !== 'DRAFT';

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
                    <button
                        className="btn btn-secondary"
                        onClick={() => navigate('/workflows')}
                    >
                        ← Back
                    </button>
                    <div>
                        <h1 style={{ margin: 0 }}>{template?.name}</h1>
                        <div style={{ color: 'var(--text-secondary)', fontSize: '14px' }}>
                            Version {template?.version} •
                            <span style={{
                                marginLeft: '8px',
                                padding: '2px 8px',
                                borderRadius: '4px',
                                background: template?.status === 'ACTIVE' ? 'var(--success)' :
                                    template?.status === 'DRAFT' ? 'var(--warning)' : 'var(--text-secondary)',
                                color: template?.status === 'DRAFT' ? '#000' : '#fff',
                                fontSize: '12px'
                            }}>
                                {template?.status}
                            </span>
                        </div>
                    </div>
                </div>
                <div style={{ display: 'flex', gap: '12px' }}>
                    {!isReadOnly && (
                        <>
                            <button
                                className="btn btn-secondary"
                                onClick={() => handleSave()}
                                disabled={saving || !hasChanges}
                            >
                                {saving ? 'Saving...' : '💾 Save'}
                            </button>
                            <button
                                className="btn btn-success"
                                onClick={handleDeploy}
                                disabled={saving || hasChanges}
                                title={hasChanges ? 'Save changes before deploying' : 'Deploy workflow'}
                            >
                                🚀 Deploy
                            </button>
                        </>
                    )}
                    {isReadOnly && (
                        <button
                            className="btn btn-primary"
                            onClick={handleCreateNewVersion}
                            disabled={saving}
                            title="Create a new draft version based on this workflow"
                        >
                            {saving ? 'Creating...' : '📝 Create New Version'}
                        </button>
                    )}
                </div>
            </div>

            {error && <div className="alert alert-danger">{error}</div>}
            {success && <div className="alert alert-success">{success}</div>}
            {hasChanges && !isReadOnly && (
                <div className="alert" style={{ background: 'var(--warning)', color: '#000', marginBottom: '16px' }}>
                    ⚠️ You have unsaved changes
                </div>
            )}

            {/* Designer */}
            <div style={{ display: 'flex', gap: '16px' }}>
                {/* Main Canvas */}
                <div style={{ flex: 1 }}>
                    <BpmnDesigner
                        initialXml={bpmnXml}
                        onXmlChange={isReadOnly ? undefined : handleXmlChange}
                        onSave={isReadOnly ? undefined : handleSave}
                        readOnly={isReadOnly}
                        height="calc(100vh - 200px)"
                    />
                </div>

                {/* Side Panel */}
                <div style={{ width: '300px', flexShrink: 0 }}>
                    <div className="card" style={{ marginBottom: '16px' }}>
                        <h3 style={{ margin: '0 0 16px 0', fontSize: '16px' }}>📋 Properties</h3>
                        <div className="form-group">
                            <label>Name</label>
                            <input
                                type="text"
                                className="form-control"
                                value={template?.name || ''}
                                disabled
                            />
                        </div>
                        <div className="form-group">
                            <label>Description</label>
                            <textarea
                                className="form-control"
                                rows={3}
                                value={template?.description || ''}
                                disabled
                            />
                        </div>
                    </div>

                    <div className="card" style={{ marginBottom: '16px' }}>
                        <h3 style={{ margin: '0 0 16px 0', fontSize: '16px' }}>📝 Available Forms</h3>
                        {forms.length === 0 ? (
                            <div style={{ color: 'var(--text-secondary)', fontSize: '14px' }}>
                                No forms available. Create forms in the Forms section.
                            </div>
                        ) : (
                            <ul style={{ margin: 0, padding: 0, listStyle: 'none' }}>
                                {forms.map(form => (
                                    <li key={form.id} style={{
                                        padding: '8px',
                                        background: 'var(--bg-tertiary)',
                                        borderRadius: '4px',
                                        marginBottom: '8px',
                                        fontSize: '14px'
                                    }}>
                                        <strong>{form.name}</strong>
                                        <div style={{ color: 'var(--text-secondary)', fontSize: '12px' }}>
                                            {Object.keys(form.schema?.properties || {}).length} fields
                                        </div>
                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>

                    <div className="card">
                        <h3 style={{ margin: '0 0 16px 0', fontSize: '16px' }}>ℹ️ Instructions</h3>
                        <ol style={{ margin: 0, paddingLeft: '20px', fontSize: '14px', color: 'var(--text-secondary)' }}>
                            <li>Drag elements from the palette on the left</li>
                            <li>Connect tasks with sequence flows</li>
                            <li>Use User Tasks for human activities</li>
                            <li>Use Service Tasks for automated actions</li>
                            <li>Add Gateways for branching logic</li>
                            <li>Save and Deploy when ready</li>
                        </ol>
                    </div>
                </div>
            </div>
        </div>
    );
}
