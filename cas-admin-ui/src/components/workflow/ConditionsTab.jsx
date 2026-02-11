import { useState, useEffect, useCallback } from 'react';

const OPERATORS = [
    { value: '==', label: '=' },
    { value: '!=', label: '≠' },
    { value: '>', label: '>' },
    { value: '<', label: '<' },
    { value: '>=', label: '≥' },
    { value: '<=', label: '≤' },
    { value: 'contains', label: 'contains' },
    { value: 'in', label: 'in list' }
];

function createEmptyCondition() {
    return { field: '', operator: '==', value: '' };
}

/**
 * Conditions tab — shown for Gateways and Sequence Flows.
 * Visual condition builder with field/operator/value rows.
 */
export default function ConditionsTab({
    element,
    modelerRef,
    topicId,
    gatewayRule,
    onConfigChange,
    onDirty,
    onSaved
}) {
    const isGateway = element?.type?.includes('Gateway');
    const isSequenceFlow = element?.type === 'bpmn:SequenceFlow';

    // For gateways: manage conditions per outgoing flow
    // For sequence flows: manage conditions on this flow
    const [conditions, setConditions] = useState([createEmptyCondition()]);
    const [isDefaultFlow, setIsDefaultFlow] = useState(false);
    const [advancedMode, setAdvancedMode] = useState(false);
    const [expression, setExpression] = useState('');
    const [saving, setSaving] = useState(false);
    const [outgoingFlows, setOutgoingFlows] = useState([]);

    // Load existing config
    useEffect(() => {
        if (isSequenceFlow) {
            const bo = element?.businessObject;
            const condExpr = bo?.conditionExpression?.body || '';
            if (condExpr) {
                setAdvancedMode(true);
                setExpression(condExpr);
            }

            // Check if this is the default flow from the source
            const sourceRef = bo?.sourceRef;
            if (sourceRef?.default === bo) {
                setIsDefaultFlow(true);
            }
        }

        if (isGateway && gatewayRule) {
            // Load gateway rules into conditions
            const rules = gatewayRule.rules || [];
            if (rules.length > 0) {
                const loadedConditions = rules.map(r => ({
                    field: r.field || '',
                    operator: r.operator || '==',
                    value: r.value || '',
                    targetFlow: r.targetFlow || ''
                }));
                setConditions(loadedConditions);
            }
        }
    }, [element, gatewayRule, isSequenceFlow, isGateway]);

    // For gateways, get outgoing sequence flows
    useEffect(() => {
        if (isGateway && element) {
            const flows = element.outgoing || [];
            setOutgoingFlows(flows.map(f => ({
                id: f.id,
                name: f.businessObject?.name || f.id,
                targetName: f.target?.businessObject?.name || f.target?.id || ''
            })));
        }
    }, [element, isGateway]);

    const handleAddCondition = useCallback(() => {
        setConditions(prev => [...prev, createEmptyCondition()]);
        onDirty?.();
    }, [onDirty]);

    const handleRemoveCondition = useCallback((index) => {
        setConditions(prev => prev.filter((_, i) => i !== index));
        onDirty?.();
    }, [onDirty]);

    const handleConditionChange = useCallback((index, field, value) => {
        setConditions(prev => prev.map((c, i) =>
            i === index ? { ...c, [field]: value } : c
        ));
        onDirty?.();
    }, [onDirty]);

    const handleSaveExpression = useCallback(async () => {
        if (!element || !modelerRef?.current) return;

        if (isSequenceFlow) {
            const modeler = modelerRef.current.getModeler();
            const modeling = modelerRef.current.getModeling();
            const moddle = modeler.get('moddle');

            if (advancedMode && expression.trim()) {
                const conditionExpression = moddle.create('bpmn:FormalExpression', {
                    body: expression.trim()
                });
                modeling.updateProperties(element, { conditionExpression });
            } else if (!advancedMode && conditions.some(c => c.field && c.value)) {
                // Build expression from visual conditions
                const expr = conditions
                    .filter(c => c.field && c.value)
                    .map(c => {
                        if (c.operator === 'contains') return `\${${c.field}.contains('${c.value}')}`;
                        if (c.operator === 'in') return `\${${c.value}.contains(${c.field})}`;
                        return `\${${c.field} ${c.operator} ${isNaN(c.value) ? "'" + c.value + "'" : c.value}}`;
                    })
                    .join(' && ');

                if (expr) {
                    const conditionExpression = moddle.create('bpmn:FormalExpression', {
                        body: expr
                    });
                    modeling.updateProperties(element, { conditionExpression });
                }
            }

            onSaved?.();
            return;
        }

        // For gateways, save to backend
        if (isGateway && topicId) {
            setSaving(true);
            try {
                const rules = conditions
                    .filter(c => c.field && c.value)
                    .map(c => ({
                        field: c.field,
                        operator: c.operator,
                        value: c.value,
                        targetFlow: c.targetFlow || ''
                    }));

                await onConfigChange?.('gateway', element.id, {
                    rules,
                    defaultFlow: isDefaultFlow ? outgoingFlows[0]?.id : null
                });
                onSaved?.();
            } catch (err) {
                console.error('Error saving gateway rules:', err);
            } finally {
                setSaving(false);
            }
        }
    }, [element, modelerRef, isSequenceFlow, isGateway, advancedMode, expression, conditions, topicId, isDefaultFlow, outgoingFlows, onConfigChange, onSaved]);

    // Toggle default flow
    const handleDefaultFlowToggle = useCallback(() => {
        if (!element || !modelerRef?.current) return;

        if (isSequenceFlow) {
            const modeling = modelerRef.current.getModeling();
            const sourceElement = element.source;
            if (sourceElement) {
                modeling.updateProperties(sourceElement, {
                    default: isDefaultFlow ? undefined : element
                });
                setIsDefaultFlow(!isDefaultFlow);
                onDirty?.();
            }
        }
    }, [element, modelerRef, isSequenceFlow, isDefaultFlow, onDirty]);

    return (
        <div>
            {/* Sequence Flow Conditions */}
            {isSequenceFlow && (
                <>
                    {/* Default Flow toggle */}
                    <div className="panel-section">
                        <div className="panel-toggle">
                            <span className="panel-toggle__label">Default Flow</span>
                            <button
                                className={`panel-toggle__switch ${isDefaultFlow ? 'panel-toggle__switch--on' : ''}`}
                                onClick={handleDefaultFlowToggle}
                                role="switch"
                                aria-checked={isDefaultFlow}
                            />
                        </div>
                        <p className="panel-section__help">
                            The default flow is taken when no other conditions match.
                        </p>
                    </div>

                    {!isDefaultFlow && (
                        <>
                            {/* Mode Toggle */}
                            <div className="panel-section">
                                <div className="panel-toggle">
                                    <span className="panel-toggle__label">Advanced (Expression Mode)</span>
                                    <button
                                        className={`panel-toggle__switch ${advancedMode ? 'panel-toggle__switch--on' : ''}`}
                                        onClick={() => { setAdvancedMode(!advancedMode); onDirty?.(); }}
                                        role="switch"
                                        aria-checked={advancedMode}
                                    />
                                </div>
                            </div>

                            {advancedMode ? (
                                /* Expression mode */
                                <div className="panel-section">
                                    <label className="panel-section__label">Condition Expression</label>
                                    <textarea
                                        className="panel-textarea"
                                        value={expression}
                                        onChange={(e) => { setExpression(e.target.value); onDirty?.(); }}
                                        placeholder="${amount > 500000}"
                                        rows={4}
                                        style={{ fontFamily: "'SF Mono', 'Fira Code', monospace", fontSize: '13px' }}
                                    />
                                    <p className="panel-section__help">
                                        Use JUEL expressions. Variables are accessed with ${'{'}variableName{'}'}.
                                    </p>
                                </div>
                            ) : (
                                /* Visual condition builder */
                                <div className="panel-section">
                                    <label className="panel-section__label">Conditions</label>
                                    {conditions.map((cond, idx) => (
                                        <div key={idx} className="condition-row">
                                            <span className="condition-row__label">
                                                {idx === 0 ? 'IF' : 'AND'}
                                            </span>
                                            <div className="condition-row__field">
                                                <input
                                                    className="panel-input"
                                                    value={cond.field}
                                                    onChange={(e) => handleConditionChange(idx, 'field', e.target.value)}
                                                    placeholder="Field name"
                                                />
                                            </div>
                                            <div className="condition-row__operator">
                                                <select
                                                    className="panel-select"
                                                    value={cond.operator}
                                                    onChange={(e) => handleConditionChange(idx, 'operator', e.target.value)}
                                                >
                                                    {OPERATORS.map(op => (
                                                        <option key={op.value} value={op.value}>{op.label}</option>
                                                    ))}
                                                </select>
                                            </div>
                                            <div className="condition-row__value">
                                                <input
                                                    className="panel-input"
                                                    value={cond.value}
                                                    onChange={(e) => handleConditionChange(idx, 'value', e.target.value)}
                                                    placeholder="Value"
                                                />
                                            </div>
                                            {conditions.length > 1 && (
                                                <button
                                                    className="condition-row__remove"
                                                    onClick={() => handleRemoveCondition(idx)}
                                                    title="Remove condition"
                                                >
                                                    ✕
                                                </button>
                                            )}
                                        </div>
                                    ))}
                                    <button className="condition-add-btn" onClick={handleAddCondition}>
                                        + Add Condition
                                    </button>
                                </div>
                            )}
                        </>
                    )}
                </>
            )}

            {/* Gateway Conditions */}
            {isGateway && (
                <div className="panel-section">
                    <label className="panel-section__label">
                        Outgoing Flows ({outgoingFlows.length})
                    </label>
                    <p className="panel-section__help" style={{ marginBottom: 'var(--space-3)' }}>
                        Define conditions for each outgoing flow. Click on individual sequence flows for detailed configuration.
                    </p>

                    {outgoingFlows.length === 0 ? (
                        <div style={{
                            padding: 'var(--space-4)',
                            textAlign: 'center',
                            color: 'var(--color-gray-400)',
                            fontSize: 'var(--font-size-sm)',
                            background: 'var(--color-gray-50)',
                            borderRadius: 'var(--radius-md)',
                            border: '1px dashed var(--color-gray-200)'
                        }}>
                            No outgoing flows. Add sequence flows from this gateway.
                        </div>
                    ) : (
                        outgoingFlows.map(flow => (
                            <div key={flow.id} className="flow-card">
                                <div className="flow-card__header">
                                    <span className="flow-card__name">→ {flow.name}</span>
                                    <span className="flow-card__target">to {flow.targetName}</span>
                                </div>
                                <p style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-gray-400)', margin: 0 }}>
                                    Select this flow on the diagram to edit conditions.
                                </p>
                            </div>
                        ))
                    )}
                </div>
            )}

            {/* Save button */}
            <div style={{ paddingTop: 'var(--space-3)' }}>
                <button
                    className="btn btn-primary"
                    onClick={handleSaveExpression}
                    disabled={saving}
                    style={{ width: '100%' }}
                >
                    {saving ? 'Saving...' : 'Apply Conditions'}
                </button>
            </div>
        </div>
    );
}
