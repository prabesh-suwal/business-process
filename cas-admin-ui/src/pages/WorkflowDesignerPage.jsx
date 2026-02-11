import { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { processTemplates, workflowConfigs } from '../api';
import BpmnDesigner, { getElementMeta } from '../components/BpmnDesigner';
import PropertyPanel from '../components/workflow/PropertyPanel';
import GlobalSettingsPanel from '../components/workflow/GlobalSettingsPanel';

export default function WorkflowDesignerPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const designerRef = useRef(null);

    // Template state
    const [template, setTemplate] = useState(null);
    const [bpmnXml, setBpmnXml] = useState('');
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [hasChanges, setHasChanges] = useState(false);

    // Panel state
    const [selectedElement, setSelectedElement] = useState(null);
    const [panelOpen, setPanelOpen] = useState(false);
    const [globalSettingsOpen, setGlobalSettingsOpen] = useState(false);

    // Config state (loaded from backend)
    const [configId, setConfigId] = useState(null);
    const [stepConfigs, setStepConfigs] = useState([]);
    const [gatewayRules, setGatewayRules] = useState([]);

    const isReadOnly = template?.status !== 'DRAFT';

    // Load template
    useEffect(() => {
        loadTemplate();
    }, [id]);

    const loadTemplate = async () => {
        try {
            setLoading(true);
            const data = await processTemplates.get(id);
            setTemplate(data);
            setBpmnXml(data.bpmnXml || '');

            // Try to load workflow config
            if (data.code || data.processKey) {
                try {
                    const cfg = await workflowConfigs.getByCode(data.code || data.processKey);
                    if (cfg) {
                        setConfigId(cfg.id);
                        setStepConfigs(cfg.stepConfigs || []);
                        setGatewayRules(cfg.gatewayRules || []);
                    }
                } catch { /* Config may not exist yet */ }
            }
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    // Element selection handler
    const handleElementSelect = useCallback((element) => {
        setSelectedElement(element);
        if (element) {
            setPanelOpen(true);
            setGlobalSettingsOpen(false);
        } else {
            setPanelOpen(false);
        }
    }, []);

    // Close property panel
    const handleClosePanel = useCallback(() => {
        setPanelOpen(false);
        setSelectedElement(null);
    }, []);

    // Toggle global settings
    const handleToggleGlobalSettings = useCallback(() => {
        if (globalSettingsOpen) {
            setGlobalSettingsOpen(false);
        } else {
            setGlobalSettingsOpen(true);
            setPanelOpen(false);
            setSelectedElement(null);
        }
    }, [globalSettingsOpen]);

    // XML change
    const handleXmlChange = useCallback((xml) => {
        setBpmnXml(xml);
        setHasChanges(true);
    }, []);

    // Save BPMN
    const handleSave = useCallback(async (xml) => {
        try {
            setSaving(true);
            setError('');
            await processTemplates.update(id, { bpmnXml: xml || bpmnXml });
            setSuccess('Workflow saved!');
            setHasChanges(false);
            setTimeout(() => setSuccess(''), 3000);
        } catch (err) {
            setError(err.message);
        } finally {
            setSaving(false);
        }
    }, [id, bpmnXml]);

    // Deploy
    const handleDeploy = useCallback(async () => {
        if (hasChanges) { setError('Save changes before deploying'); return; }
        if (!confirm('Deploy this workflow? It becomes active and cannot be edited.')) return;
        try {
            setSaving(true);
            await processTemplates.deploy(id);
            setSuccess('Workflow deployed!');
            loadTemplate();
        } catch (err) { setError(err.message); }
        finally { setSaving(false); }
    }, [id, hasChanges]);

    // New version
    const handleCreateNewVersion = useCallback(async () => {
        if (!confirm('Create a new draft version based on this workflow?')) return;
        try {
            setSaving(true);
            const newVersion = await processTemplates.createNewVersion(id);
            setSuccess(`Version ${newVersion.version} created!`);
            navigate(`/workflows/${newVersion.id}/design`);
        } catch (err) { setError(err.message); }
        finally { setSaving(false); }
    }, [id, navigate]);

    // Config change handler (from property panel tabs)
    const handleConfigChange = useCallback(async (type, key, data) => {
        if (type === 'step') {
            setStepConfigs(prev => {
                const idx = prev.findIndex(s => s.taskKey === key);
                if (idx >= 0) {
                    const updated = [...prev];
                    updated[idx] = { ...updated[idx], ...data };
                    return updated;
                }
                return [...prev, { taskKey: key, ...data }];
            });
        } else if (type === 'gateway') {
            setGatewayRules(prev => {
                const idx = prev.findIndex(g => g.gatewayKey === key);
                if (idx >= 0) {
                    const updated = [...prev];
                    updated[idx] = { ...updated[idx], ...data };
                    return updated;
                }
                return [...prev, { gatewayKey: key, ...data }];
            });
        }
    }, []);

    const showPanel = panelOpen && selectedElement;
    const showAnyPanel = showPanel || globalSettingsOpen;

    // Keyboard shortcut: Ctrl+S to save
    useEffect(() => {
        const handleKeyDown = (e) => {
            if ((e.ctrlKey || e.metaKey) && e.key === 's') {
                e.preventDefault();
                if (!isReadOnly && hasChanges) handleSave();
            }
        };
        document.addEventListener('keydown', handleKeyDown);
        return () => document.removeEventListener('keydown', handleKeyDown);
    }, [isReadOnly, hasChanges, handleSave]);

    if (loading) {
        return (
            <div className="page-container">
                <div style={{
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    height: '60vh', color: 'var(--color-gray-400)', fontSize: 'var(--font-size-lg)'
                }}>
                    Loading designer...
                </div>
            </div>
        );
    }

    return (
        <div className="page-container" style={{ maxWidth: 'none', padding: 0 }}>
            {/* Compact Toolbar */}
            <div className="designer-toolbar">
                <div className="designer-toolbar__left">
                    <button className="btn btn-secondary btn-sm" onClick={() => navigate('/workflows')}
                        title="Back to workflows">
                        ← Back
                    </button>
                    <div className="designer-toolbar__title">
                        <span className="designer-toolbar__name">{template?.name}</span>
                        <span className="designer-toolbar__meta">
                            v{template?.version}
                            <span className={`status-badge status-badge--${(template?.status || 'draft').toLowerCase()}`}>
                                {template?.status}
                            </span>
                            {hasChanges && !isReadOnly && (
                                <span className="dirty-indicator" title="Unsaved changes" />
                            )}
                        </span>
                    </div>
                </div>

                <div className="designer-toolbar__right">
                    <button className="btn btn-ghost btn-sm" onClick={handleToggleGlobalSettings}
                        title="Global Settings" style={{
                            background: globalSettingsOpen ? 'var(--color-primary-50)' : undefined,
                            color: globalSettingsOpen ? 'var(--color-primary-600)' : undefined
                        }}>
                        ⚙️ Settings
                    </button>

                    {!isReadOnly && (
                        <>
                            <button className="btn btn-secondary btn-sm" onClick={() => handleSave()}
                                disabled={saving || !hasChanges}>
                                {saving ? '...' : '💾 Save'}
                            </button>
                            <button className="btn btn-success btn-sm" onClick={handleDeploy}
                                disabled={saving || hasChanges}
                                title={hasChanges ? 'Save changes first' : 'Deploy workflow'}>
                                🚀 Deploy
                            </button>
                        </>
                    )}
                    {isReadOnly && (
                        <button className="btn btn-primary btn-sm" onClick={handleCreateNewVersion}
                            disabled={saving}>
                            {saving ? '...' : '📝 New Version'}
                        </button>
                    )}
                </div>
            </div>

            {/* Alerts */}
            {error && (
                <div className="alert alert-danger" style={{ margin: '0 var(--space-4)', borderRadius: 0 }}>
                    {error}
                    <button onClick={() => setError('')} style={{
                        float: 'right', background: 'none',
                        border: 'none', cursor: 'pointer', fontSize: '16px'
                    }}>×</button>
                </div>
            )}
            {success && (
                <div className="alert alert-success" style={{ margin: '0 var(--space-4)', borderRadius: 0 }}>
                    {success}
                </div>
            )}

            {/* Main Designer Layout */}
            <div className="designer-layout">
                {/* Canvas Area */}
                <div className="designer-canvas" style={{
                    marginRight: showAnyPanel ? '0' : undefined,
                    transition: 'margin 300ms cubic-bezier(0.4, 0, 0.2, 1)'
                }}>
                    <BpmnDesigner
                        ref={designerRef}
                        initialXml={bpmnXml}
                        onXmlChange={isReadOnly ? undefined : handleXmlChange}
                        onSave={isReadOnly ? undefined : handleSave}
                        onElementSelect={handleElementSelect}
                        readOnly={isReadOnly}
                        height="100%"
                        style={{ width: '100%', height: '100%' }}
                    />

                    {/* Selection hint when no panel is open */}
                    {!showAnyPanel && !selectedElement && (
                        <div className="designer-hint">
                            Click an element to view its properties
                        </div>
                    )}
                </div>

                {/* Property Panel - slides in from right */}
                {showPanel && (
                    <PropertyPanel
                        element={selectedElement}
                        modelerRef={designerRef}
                        topicId={template?.topicId}
                        stepConfigs={stepConfigs}
                        gatewayRules={gatewayRules}
                        onConfigChange={handleConfigChange}
                        onClose={handleClosePanel}
                    />
                )}

                {/* Global Settings Panel */}
                {globalSettingsOpen && (
                    <GlobalSettingsPanel
                        topicId={template?.topicId}
                        templateId={id}
                        configId={configId}
                        onClose={() => setGlobalSettingsOpen(false)}
                        onSaved={() => {
                            setSuccess('Settings saved!');
                            setTimeout(() => setSuccess(''), 3000);
                            loadTemplate();
                        }}
                    />
                )}
            </div>
        </div>
    );
}
