import { useState, useEffect, useCallback } from 'react';
import { formDefinitions } from '../../api';

export default function AdvancedTab({ element, topicId, stepConfig, onConfigChange, onDirty, onSaved }) {
    const elementType = element?.type;
    const taskKey = element?.id || '';

    return (
        <div>
            {elementType === 'bpmn:UserTask' && (
                <UserTaskAdvanced element={element} topicId={topicId} taskKey={taskKey}
                    stepConfig={stepConfig} onConfigChange={onConfigChange} onDirty={onDirty} onSaved={onSaved} />
            )}
            {elementType === 'bpmn:ParallelGateway' && (
                <ParallelGatewayAdvanced element={element} stepConfig={stepConfig}
                    onConfigChange={onConfigChange} onDirty={onDirty} onSaved={onSaved} />
            )}
            {elementType === 'bpmn:ServiceTask' && <ServiceTaskAdvanced element={element} />}
            {elementType === 'bpmn:SubProcess' && <SubProcessAdvanced element={element} />}
        </div>
    );
}

function UserTaskAdvanced({ element, topicId, taskKey, stepConfig, onConfigChange, onDirty, onSaved }) {
    const [slaHours, setSlaHours] = useState('');
    const [slaPriority, setSlaPriority] = useState('NORMAL');
    const [linkedFormId, setLinkedFormId] = useState('');
    const [escalationEnabled, setEscalationEnabled] = useState(false);
    const [escalationHours, setEscalationHours] = useState('');
    const [escalationAction, setEscalationAction] = useState('NOTIFY');
    const [viewerRoles, setViewerRoles] = useState('');
    const [forms, setForms] = useState([]);
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        if (stepConfig) {
            const sla = stepConfig.slaConfig || {};
            setSlaHours(sla.dueInHours || '');
            setSlaPriority(sla.priority || 'NORMAL');
            const form = stepConfig.formConfig || {};
            setLinkedFormId(form.formId || '');
            const esc = stepConfig.escalationConfig || {};
            setEscalationEnabled(!!esc.enabled);
            setEscalationHours(esc.afterHours || '');
            setEscalationAction(esc.action || 'NOTIFY');
            const viewer = stepConfig.viewerConfig || {};
            setViewerRoles(Array.isArray(viewer.roles) ? viewer.roles.join(', ') : (viewer.roles || ''));
        }
    }, [stepConfig]);

    useEffect(() => {
        let cancelled = false;
        formDefinitions.list(null, true).then(res => {
            if (!cancelled) setForms(res || []);
        }).catch(console.error);
        return () => { cancelled = true; };
    }, []);

    const handleSave = useCallback(async () => {
        if (!topicId || !taskKey) return;
        setSaving(true);
        try {
            await onConfigChange?.('step', taskKey, {
                slaConfig: { dueInHours: slaHours ? parseInt(slaHours) : null, priority: slaPriority },
                formConfig: { formId: linkedFormId || null },
                escalationConfig: { enabled: escalationEnabled, afterHours: escalationHours ? parseInt(escalationHours) : null, action: escalationAction },
                viewerConfig: { roles: viewerRoles ? viewerRoles.split(',').map(r => r.trim()).filter(Boolean) : [] }
            });
            onSaved?.();
        } catch (err) { console.error('Error saving advanced config:', err); }
        finally { setSaving(false); }
    }, [topicId, taskKey, slaHours, slaPriority, linkedFormId, escalationEnabled, escalationHours, escalationAction, viewerRoles, onConfigChange, onSaved]);

    return (
        <>
            <div className="panel-section">
                <label className="panel-section__label">SLA / Due Date</label>
                <div style={{ display: 'flex', gap: 'var(--space-2)', marginBottom: 'var(--space-2)' }}>
                    <input className="panel-input" type="number" value={slaHours} style={{ flex: 1 }}
                        onChange={(e) => { setSlaHours(e.target.value); onDirty?.(); }} placeholder="Hours" min="0" />
                    <select className="panel-select" value={slaPriority} style={{ flex: 1 }}
                        onChange={(e) => { setSlaPriority(e.target.value); onDirty?.(); }}>
                        <option value="LOW">Low</option><option value="NORMAL">Normal</option>
                        <option value="HIGH">High</option><option value="URGENT">Urgent</option>
                    </select>
                </div>
                <p className="panel-section__help">Expected completion time and priority.</p>
            </div>

            <div className="panel-section">
                <label className="panel-section__label">Linked Form</label>
                <select className="panel-select" value={linkedFormId}
                    onChange={(e) => { setLinkedFormId(e.target.value); onDirty?.(); }}>
                    <option value="">No form linked</option>
                    {forms.map(f => <option key={f.id} value={f.id}>{f.name} (v{f.version || 1})</option>)}
                </select>
                <p className="panel-section__help">The form users see when completing this task.</p>
            </div>

            <div className="panel-section">
                <label className="panel-section__label">Escalation</label>
                <div className="panel-toggle" style={{ marginBottom: 'var(--space-2)' }}>
                    <span className="panel-toggle__label">Enable Escalation</span>
                    <button className={`panel-toggle__switch ${escalationEnabled ? 'panel-toggle__switch--on' : ''}`}
                        onClick={() => { setEscalationEnabled(!escalationEnabled); onDirty?.(); }}
                        role="switch" aria-checked={escalationEnabled} />
                </div>
                {escalationEnabled && (
                    <div style={{ display: 'flex', gap: 'var(--space-2)' }}>
                        <input className="panel-input" type="number" value={escalationHours} style={{ flex: 1 }}
                            onChange={(e) => { setEscalationHours(e.target.value); onDirty?.(); }} placeholder="After hours" min="1" />
                        <select className="panel-select" value={escalationAction} style={{ flex: 1 }}
                            onChange={(e) => { setEscalationAction(e.target.value); onDirty?.(); }}>
                            <option value="NOTIFY">Notify</option><option value="REASSIGN">Reassign</option>
                            <option value="ESCALATE">Escalate to Manager</option>
                        </select>
                    </div>
                )}
            </div>

            <div className="panel-section">
                <label className="panel-section__label">Viewer Roles</label>
                <input className="panel-input" value={viewerRoles}
                    onChange={(e) => { setViewerRoles(e.target.value); onDirty?.(); }} placeholder="ADMIN, MANAGER, AUDITOR" />
                <p className="panel-section__help">Comma-separated roles that can view but not act on this task.</p>
            </div>

            <div style={{ paddingTop: 'var(--space-3)' }}>
                <button className="btn btn-primary" onClick={handleSave} disabled={saving} style={{ width: '100%' }}>
                    {saving ? 'Saving...' : 'Save Settings'}
                </button>
            </div>
        </>
    );
}

function ParallelGatewayAdvanced({ element, stepConfig, onConfigChange, onDirty, onSaved }) {
    const [completionMode, setCompletionMode] = useState('ALL');
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        const bo = element?.businessObject;
        const exts = bo?.extensionElements?.values || [];
        for (const ext of exts) {
            const props = ext.values || [];
            for (const p of props) {
                if (p.name === 'completionMode') setCompletionMode(p.value || 'ALL');
            }
        }
    }, [element]);

    const handleSave = useCallback(async () => {
        setSaving(true);
        try {
            await onConfigChange?.('gateway', element?.id, { completionMode });
            onSaved?.();
        } catch (err) { console.error('Error saving:', err); }
        finally { setSaving(false); }
    }, [element, completionMode, onConfigChange, onSaved]);

    return (
        <div className="panel-section">
            <label className="panel-section__label">Completion Mode</label>
            <select className="panel-select" value={completionMode}
                onChange={(e) => { setCompletionMode(e.target.value); onDirty?.(); }}>
                <option value="ALL">ALL — Wait for all branches</option>
                <option value="ANY">ANY — Continue when first completes</option>
            </select>
            <p className="panel-section__help">
                "ALL" waits for every parallel branch. "ANY" proceeds when one branch completes and cancels the rest.
            </p>
            <div style={{ paddingTop: 'var(--space-3)' }}>
                <button className="btn btn-primary" onClick={handleSave} disabled={saving} style={{ width: '100%' }}>
                    {saving ? 'Saving...' : 'Save Settings'}
                </button>
            </div>
        </div>
    );
}

function ServiceTaskAdvanced({ element }) {
    const bo = element?.businessObject;
    return (
        <>
            <div className="panel-section">
                <label className="panel-section__label">Implementation Type</label>
                <input className="panel-input panel-input--readonly" readOnly
                    value={bo?.['flowable:delegateExpression'] ? 'Delegate Expression' : (bo?.['flowable:class'] ? 'Java Class' : 'Not configured')} />
            </div>
            {bo?.['flowable:delegateExpression'] && (
                <div className="panel-section">
                    <label className="panel-section__label">Delegate Expression</label>
                    <input className="panel-input panel-input--readonly" value={bo['flowable:delegateExpression']} readOnly />
                </div>
            )}
        </>
    );
}

function SubProcessAdvanced({ element }) {
    const bo = element?.businessObject;
    const isMulti = !!bo?.loopCharacteristics;
    return (
        <div className="panel-section">
            <label className="panel-section__label">Sub-Process Configuration</label>
            <div style={{
                padding: 'var(--space-3)', background: 'var(--color-gray-50)', borderRadius: 'var(--radius-md)',
                border: '1px solid var(--color-gray-200)', fontSize: 'var(--font-size-sm)', color: 'var(--color-gray-600)'
            }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                    <span>Multi-Instance</span>
                    <span>{isMulti ? '✓ Enabled' : 'Disabled'}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span>Triggered Start</span>
                    <span>{bo?.triggeredByEvent ? '✓ Yes' : 'No'}</span>
                </div>
            </div>
            <p className="panel-section__help">Sub-process properties are configured in the BPMN model directly.</p>
        </div>
    );
}
