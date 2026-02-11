import { useState, useEffect, useCallback } from 'react';
import { workflowConfigs, processTemplates, formDefinitions, products } from '../../api';

/**
 * Global settings panel — accessed via ⚙️ icon.
 * Shows topic-level / workflow configuration settings.
 */
export default function GlobalSettingsPanel({ topicId, templateId, configId, onClose, onSaved }) {
    const [processName, setProcessName] = useState('');
    const [processDesc, setProcessDesc] = useState('');
    const [productCode, setProductCode] = useState('');
    const [startFormId, setStartFormId] = useState('');
    const [configActive, setConfigActive] = useState(false);
    const [saving, setSaving] = useState(false);
    const [loading, setLoading] = useState(true);

    // Reference data
    const [productsList, setProductsList] = useState([]);
    const [formsList, setFormsList] = useState([]);
    const [templateInfo, setTemplateInfo] = useState(null);
    const [configInfo, setConfigInfo] = useState(null);

    // Load data
    useEffect(() => {
        let cancelled = false;
        setLoading(true);

        const load = async () => {
            try {
                const [prodsRes, formsRes] = await Promise.allSettled([
                    products.list(),
                    formDefinitions.list(null, true)
                ]);
                if (cancelled) return;
                if (prodsRes.status === 'fulfilled') setProductsList(prodsRes.value || []);
                if (formsRes.status === 'fulfilled') setFormsList(formsRes.value || []);

                // Load template info
                if (templateId) {
                    try {
                        const tmpl = await processTemplates.get(templateId);
                        if (!cancelled) {
                            setTemplateInfo(tmpl);
                            setProcessName(tmpl.name || '');
                            setProcessDesc(tmpl.description || '');
                            setProductCode(tmpl.productCode || tmpl.productId || '');
                        }
                    } catch (e) { console.error('Error loading template:', e); }
                }

                // Load config info
                if (configId) {
                    try {
                        const cfg = await workflowConfigs.get(configId);
                        if (!cancelled) {
                            setConfigInfo(cfg);
                            setConfigActive(cfg.active || false);
                            if (cfg.startFormId) setStartFormId(cfg.startFormId);
                        }
                    } catch (e) { console.error('Error loading config:', e); }
                }
            } catch (err) {
                console.error('Error loading global settings:', err);
            } finally {
                if (!cancelled) setLoading(false);
            }
        };
        load();
        return () => { cancelled = true; };
    }, [templateId, configId]);

    const handleSave = useCallback(async () => {
        setSaving(true);
        try {
            // Update template
            if (templateId) {
                await processTemplates.update(templateId, {
                    name: processName,
                    description: processDesc
                });
            }
            // Update config
            if (configId && configInfo) {
                await workflowConfigs.update(configId, {
                    ...configInfo,
                    startFormId: startFormId || null
                });
            }
            onSaved?.();
        } catch (err) {
            console.error('Error saving global settings:', err);
        } finally {
            setSaving(false);
        }
    }, [templateId, configId, configInfo, processName, processDesc, startFormId, onSaved]);

    // Keyboard: Esc
    useEffect(() => {
        const handleKey = (e) => { if (e.key === 'Escape') onClose(); };
        document.addEventListener('keydown', handleKey);
        return () => document.removeEventListener('keydown', handleKey);
    }, [onClose]);

    if (loading) {
        return (
            <div className="property-panel" role="complementary" aria-label="Global Settings">
                <div className="property-panel__header">
                    <div className="property-panel__icon">⚙️</div>
                    <div className="property-panel__title-group">
                        <div className="property-panel__type">GLOBAL SETTINGS</div>
                        <div style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-gray-400)' }}>Loading...</div>
                    </div>
                    <button className="property-panel__close" onClick={onClose} title="Close">×</button>
                </div>
                <div className="property-panel__body" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <span style={{ color: 'var(--color-gray-400)' }}>Loading settings...</span>
                </div>
            </div>
        );
    }

    return (
        <div className="property-panel" role="complementary" aria-label="Global Settings">
            {/* Header */}
            <div className="property-panel__header">
                <div className="property-panel__icon">⚙️</div>
                <div className="property-panel__title-group">
                    <div className="property-panel__type">GLOBAL SETTINGS</div>
                    <div style={{ fontSize: 'var(--font-size-base)', fontWeight: 'var(--font-weight-semibold)' }}>
                        {processName || 'Workflow Settings'}
                    </div>
                </div>
                <button className="property-panel__close" onClick={onClose} title="Close (Esc)">×</button>
            </div>

            {/* Body */}
            <div className="property-panel__body">
                {/* Process Name */}
                <div className="panel-section">
                    <label className="panel-section__label">Process Name</label>
                    <input className="panel-input" value={processName}
                        onChange={(e) => setProcessName(e.target.value)} placeholder="Workflow name" />
                </div>

                {/* Description */}
                <div className="panel-section">
                    <label className="panel-section__label">Description</label>
                    <textarea className="panel-textarea" value={processDesc}
                        onChange={(e) => setProcessDesc(e.target.value)} placeholder="Describe this workflow..." rows={3} />
                </div>

                {/* Product */}
                <div className="panel-section">
                    <label className="panel-section__label">Product</label>
                    <input className="panel-input panel-input--readonly" value={
                        productsList.find(p => p.code === productCode || p.id === productCode)?.name || productCode || 'Not linked'
                    } readOnly />
                    <p className="panel-section__help">Product is set when the template is created.</p>
                </div>

                {/* Start Form */}
                <div className="panel-section">
                    <label className="panel-section__label">Start Form</label>
                    <select className="panel-select" value={startFormId}
                        onChange={(e) => setStartFormId(e.target.value)}>
                        <option value="">No start form</option>
                        {formsList.map(f => (
                            <option key={f.id} value={f.id}>{f.name} (v{f.version || 1})</option>
                        ))}
                    </select>
                    <p className="panel-section__help">The form shown when initiating this workflow.</p>
                </div>

                {/* Version Info */}
                {templateInfo && (
                    <div className="panel-section">
                        <label className="panel-section__label">Version Info</label>
                        <div style={{
                            fontSize: 'var(--font-size-sm)', color: 'var(--color-gray-600)',
                            padding: 'var(--space-3)', background: 'var(--color-gray-50)',
                            borderRadius: 'var(--radius-md)', border: '1px solid var(--color-gray-200)'
                        }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                                <span>Version</span><span>{templateInfo.version || 1}</span>
                            </div>
                            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                                <span>Status</span>
                                <span style={{ color: templateInfo.status === 'DEPLOYED' ? '#059669' : 'var(--color-gray-500)' }}>
                                    {templateInfo.status || 'DRAFT'}
                                </span>
                            </div>
                            {templateInfo.deployedAt && (
                                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                    <span>Deployed</span>
                                    <span>{new Date(templateInfo.deployedAt).toLocaleDateString()}</span>
                                </div>
                            )}
                        </div>
                    </div>
                )}

                {/* Config Status */}
                {configInfo && (
                    <div className="panel-section">
                        <label className="panel-section__label">Configuration</label>
                        <div style={{
                            display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                            padding: 'var(--space-2) var(--space-3)', background: configActive ? '#d1fae5' : 'var(--color-gray-50)',
                            borderRadius: 'var(--radius-md)', border: '1px solid ' + (configActive ? '#6ee7b7' : 'var(--color-gray-200)'),
                            fontSize: 'var(--font-size-sm)'
                        }}>
                            <span>Config Code: <strong>{configInfo.code}</strong></span>
                            <span style={{ color: configActive ? '#059669' : 'var(--color-gray-500)' }}>
                                {configActive ? '● Active' : '○ Inactive'}
                            </span>
                        </div>
                    </div>
                )}

                {/* Save */}
                <div style={{ paddingTop: 'var(--space-3)' }}>
                    <button className="btn btn-primary" onClick={handleSave} disabled={saving} style={{ width: '100%' }}>
                        {saving ? 'Saving...' : 'Save Settings'}
                    </button>
                </div>
            </div>
        </div>
    );
}
