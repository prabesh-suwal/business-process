import React, { useState, useCallback, useEffect } from 'react';
import {
    GitBranch, Code2, ArrowRight, Plus, Trash2, ChevronRight, ChevronDown, ChevronUp,
    Sparkles, Variable, AlertCircle, Shield, ArrowDown, Eye, EyeOff
} from 'lucide-react';
import { MemoApi } from '../../lib/api';
import { cn } from '../../lib/utils';

/**
 * ConditionsTab — Premium condition editing for gateways and sequence flows.
 *
 * For SequenceFlow: visual builder (WHEN + SET) + expression mode, default flow toggle
 * For ExclusiveGateway: show list of outgoing flows with condition summaries
 */

// ─── Human-readable operators ────────────────────────────────────
const OPERATORS = [
    { value: 'equals', label: 'is equal to', symbol: '==' },
    { value: 'not_equals', label: 'is not equal to', symbol: '!=' },
    { value: 'greater_than', label: 'is greater than', symbol: '>' },
    { value: 'less_than', label: 'is less than', symbol: '<' },
    { value: 'greater_or_equal', label: 'is at least', symbol: '>=' },
    { value: 'less_or_equal', label: 'is at most', symbol: '<=' },
    { value: 'contains', label: 'contains', symbol: 'contains' },
    { value: 'starts_with', label: 'starts with', symbol: 'startsWith' },
    { value: 'is_empty', label: 'is empty', symbol: '== null' },
    { value: 'is_not_empty', label: 'is not empty', symbol: '!= null' },
];

// ─── Default variables if API not available ──────────────────────
const DEFAULT_FIELDS = [
    { id: 'amount', label: 'Amount', type: 'number', source: 'memo' },
    { id: 'category', label: 'Category', type: 'string', source: 'memo' },
    { id: 'priority', label: 'Priority', type: 'string', source: 'memo', options: ['LOW', 'MEDIUM', 'HIGH', 'URGENT'] },
    { id: 'department', label: 'Department', type: 'string', source: 'initiator' },
    { id: 'requestType', label: 'Request Type', type: 'string', source: 'memo' },
    { id: 'status', label: 'Status', type: 'string', source: 'memo' },
    { id: 'level', label: 'Level', type: 'number', source: 'memo' },
    { id: 'creditScore', label: 'Credit Score', type: 'number', source: 'memo' },
];

export default function ConditionsTab({
    element,
    modelerRef,
    onSelectFlow,
    topicId,
    allSteps = [],
}) {
    const type = element?.type;

    if (type === 'bpmn:ExclusiveGateway') {
        return <GatewayConditionsView element={element} modelerRef={modelerRef} onSelectFlow={onSelectFlow} />;
    }

    if (type === 'bpmn:SequenceFlow') {
        return <FlowConditionEditor element={element} modelerRef={modelerRef} topicId={topicId} />;
    }

    return (
        <div className="p-6 text-center text-slate-400 text-sm">
            <p>Conditions are not available for this element type.</p>
        </div>
    );
}

// ═══════════════════════════════════════════════════════════════════
//  GATEWAY CONDITIONS VIEW — Shows outgoing flows with previews
// ═══════════════════════════════════════════════════════════════════

function GatewayConditionsView({ element, modelerRef, onSelectFlow }) {
    const bo = element?.businessObject;
    const outgoing = bo?.outgoing || [];

    if (outgoing.length === 0) {
        return (
            <div className="py-10 text-center">
                <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-amber-100 to-orange-100 flex items-center justify-center mx-auto mb-4 shadow-sm">
                    <GitBranch className="w-7 h-7 text-amber-400" />
                </div>
                <h4 className="text-sm font-semibold text-slate-700 mb-1">No outgoing paths</h4>
                <p className="text-xs text-slate-400 max-w-[240px] mx-auto leading-relaxed">
                    Add sequence flows from this gateway to define decision paths.
                </p>
            </div>
        );
    }

    const defaultFlowId = bo?.default?.id;

    return (
        <div className="space-y-4">
            {/* Info Banner */}
            <div className="px-3.5 py-2.5 bg-gradient-to-r from-amber-50/80 to-orange-50/80 border border-amber-100 rounded-xl">
                <div className="flex items-start gap-2.5">
                    <Sparkles className="w-3.5 h-3.5 text-amber-500 mt-0.5 flex-shrink-0" />
                    <p className="text-[11px] text-amber-700 leading-relaxed">
                        Click on an outgoing <strong>sequence flow</strong> (arrow) in the diagram to configure its condition.
                        Each path needs a condition to determine when it should be followed.
                    </p>
                </div>
            </div>

            {/* Flow List */}
            <div className="space-y-2">
                {outgoing.map((flow, index) => {
                    const flowBo = flow;
                    const targetName = flowBo.targetRef?.name || flowBo.targetRef?.id || 'Unknown';
                    const conditionExpr = flowBo.conditionExpression?.body;
                    const isDefault = flowBo.id === defaultFlowId;

                    return (
                        <button
                            key={flowBo.id}
                            onClick={() => onSelectFlow?.(flowBo.id)}
                            className="w-full text-left rounded-xl border border-slate-200 bg-white overflow-hidden transition-all duration-200 hover:shadow-md hover:border-slate-300 group"
                        >
                            <div className="flex items-center gap-3 p-3.5">
                                {/* Number Badge */}
                                <div className={cn(
                                    'w-8 h-8 rounded-lg flex items-center justify-center text-xs font-bold flex-shrink-0',
                                    isDefault
                                        ? 'bg-slate-100 text-slate-500'
                                        : conditionExpr
                                            ? 'bg-emerald-100 text-emerald-700'
                                            : 'bg-red-50 text-red-400'
                                )}>
                                    {index + 1}
                                </div>

                                {/* Flow info */}
                                <div className="min-w-0 flex-1">
                                    <div className="flex items-center gap-1.5">
                                        <ArrowRight className="w-3.5 h-3.5 text-slate-400" />
                                        <span className="text-sm font-medium text-slate-700 truncate">{targetName}</span>
                                    </div>
                                    {isDefault ? (
                                        <div className="flex items-center gap-1.5 mt-1">
                                            <Shield className="w-3 h-3 text-slate-400" />
                                            <span className="text-[11px] text-slate-400 italic">Default path — no condition needed</span>
                                        </div>
                                    ) : conditionExpr ? (
                                        <code className="text-[11px] text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-md font-mono block mt-1 truncate">
                                            {conditionExpr}
                                        </code>
                                    ) : (
                                        <div className="flex items-center gap-1.5 mt-1">
                                            <AlertCircle className="w-3 h-3 text-red-400" />
                                            <span className="text-[11px] text-red-400">No condition set</span>
                                        </div>
                                    )}
                                </div>

                                {/* Arrow */}
                                <ChevronRight className="w-4 h-4 text-slate-300 group-hover:text-violet-500 transition-colors flex-shrink-0" />
                            </div>
                        </button>
                    );
                })}
            </div>
        </div>
    );
}

// ═══════════════════════════════════════════════════════════════════
//  SEQUENCE FLOW CONDITION EDITOR — Premium visual builder
// ═══════════════════════════════════════════════════════════════════

function FlowConditionEditor({ element, modelerRef, topicId }) {
    const bo = element?.businessObject;
    const [mode, setMode] = useState('visual'); // 'visual' | 'expression'

    // Default flow detection
    const sourceRef = bo?.sourceRef;
    const isGatewaySource = sourceRef?.$type === 'bpmn:ExclusiveGateway' || sourceRef?.$type === 'bpmn:InclusiveGateway';
    const isDefaultFlow = sourceRef?.default?.id === element?.id;

    // Target info
    const targetName = bo?.targetRef?.name || bo?.targetRef?.id || 'Unknown';

    // Parse existing condition expression
    const existingExpression = bo?.conditionExpression?.body || '';

    // State
    const [conditions, setConditions] = useState(() => parseExpressionToConditions(existingExpression));
    const [expressionText, setExpressionText] = useState(existingExpression);
    const [availableFields, setAvailableFields] = useState([]);
    const [outputVariables, setOutputVariables] = useState(() => parseOutputVariables(bo));

    // Re-sync state when the selected element changes (e.g., after page refresh or switching flows)
    useEffect(() => {
        const expr = bo?.conditionExpression?.body || '';
        console.log('[ConditionsTab] Element changed:', element?.id, 'conditionExpression body:', JSON.stringify(expr));
        console.log('[ConditionsTab] bo?.conditionExpression:', bo?.conditionExpression);
        setConditions(parseExpressionToConditions(expr));
        setExpressionText(expr);
        setOutputVariables(parseOutputVariables(bo));
    }, [element?.id]);

    // Load available fields from topic
    useEffect(() => {
        if (topicId) {
            MemoApi.getWorkflowVariables?.(topicId)
                .then(vars => {
                    if (vars?.length) {
                        setAvailableFields(vars.map(v => ({
                            id: v.name || v.id,
                            label: v.label || v.name || v.id,
                            type: v.type || 'string',
                            source: v.source || 'other',
                            options: v.options,
                        })));
                    } else {
                        setAvailableFields(DEFAULT_FIELDS);
                    }
                })
                .catch(() => setAvailableFields(DEFAULT_FIELDS));
        } else {
            setAvailableFields(DEFAULT_FIELDS);
        }
    }, [topicId]);

    // ── Write expression to BPMN model ───────────
    const writeExpression = useCallback((expr) => {
        console.log('[writeExpression] Called with expr:', JSON.stringify(expr), 'element:', element?.id);
        if (!modelerRef?.current || !element) {
            console.warn('[writeExpression] SKIPPED: modelerRef=', !!modelerRef?.current, 'element=', !!element);
            return;
        }
        try {
            // Suppress selection changes to prevent panel from closing
            modelerRef.current.suppressSelection?.();
            const modeler = modelerRef.current.getModeler ? modelerRef.current.getModeler() : modelerRef.current;
            const modeling = modeler.get('modeling');
            const moddle = modeler.get('moddle');

            if (expr && expr.trim()) {
                const conditionExpr = moddle.create('bpmn:FormalExpression', { body: expr });
                console.log('[writeExpression] Writing conditionExpression:', conditionExpr);
                modeling.updateProperties(element, { conditionExpression: conditionExpr });
                // Verify it was written
                console.log('[writeExpression] After write, bo.conditionExpression:', element.businessObject?.conditionExpression);
            } else {
                modeling.updateProperties(element, { conditionExpression: undefined });
            }
        } catch (err) {
            console.error('Error writing condition expression:', err);
        } finally {
            // Resume after microtask to let event queue settle
            setTimeout(() => modelerRef.current?.resumeSelection?.(), 50);
        }
    }, [element, modelerRef]);

    // ── Write output variables to extension elements ──
    const writeOutputVariables = useCallback((outputs) => {
        if (!modelerRef?.current || !element) return;
        try {
            // Suppress selection changes to prevent panel from closing
            modelerRef.current.suppressSelection?.();
            const modeler = modelerRef.current.getModeler ? modelerRef.current.getModeler() : modelerRef.current;
            const moddle = modeler.get('moddle');
            const modeling = modeler.get('modeling');

            // Store output variables as a custom property on the flow's documentation or extensionElements
            // For now, store as a JSON string in the flow's documentation field
            const existingDoc = bo?.documentation?.[0]?.text || '';
            let docData = {};
            try { docData = JSON.parse(existingDoc); } catch { docData = {}; }
            docData.outputVariables = outputs.filter(o => o.name && o.value);

            const doc = moddle.create('bpmn:Documentation', { text: JSON.stringify(docData) });
            modeling.updateProperties(element, { documentation: [doc] });
        } catch (err) {
            console.error('Error writing output variables:', err);
        } finally {
            setTimeout(() => modelerRef.current?.resumeSelection?.(), 50);
        }
    }, [element, modelerRef, bo]);

    // Toggle default flow
    const handleToggleDefault = useCallback(() => {
        if (!modelerRef?.current || !element) return;
        try {
            // Suppress selection changes to prevent panel from closing
            modelerRef.current.suppressSelection?.();
            const modeler = modelerRef.current.getModeler ? modelerRef.current.getModeler() : modelerRef.current;
            const modeling = modeler.get('modeling');
            const registry = modeler.get('elementRegistry');
            const sourceElement = registry.get(sourceRef.id);
            if (!sourceElement) return;

            if (isDefaultFlow) {
                modeling.updateProperties(sourceElement, { default: undefined });
            } else {
                modeling.updateProperties(sourceElement, { default: bo });
            }
        } catch (err) {
            console.error('Error toggling default flow:', err);
        } finally {
            setTimeout(() => modelerRef.current?.resumeSelection?.(), 50);
        }
    }, [element, modelerRef, isDefaultFlow, sourceRef, bo]);

    // ── Condition CRUD ────────────────────────────
    const handleAddCondition = () => {
        const updated = [...conditions, { field: '', operator: 'equals', value: '' }];
        setConditions(updated);
    };

    const handleRemoveCondition = (index) => {
        const updated = conditions.filter((_, i) => i !== index);
        setConditions(updated);
        const expr = conditionsToExpression(updated);
        setExpressionText(expr);
        writeExpression(expr);
    };

    const handleConditionChange = (index, key, value) => {
        const updated = conditions.map((c, i) => i === index ? { ...c, [key]: value } : c);
        setConditions(updated);
        const expr = conditionsToExpression(updated);
        setExpressionText(expr);
        writeExpression(expr);
    };

    // ── Output variable CRUD ─────────────────────
    const handleAddOutput = () => {
        const updated = [...outputVariables, { name: '', value: '' }];
        setOutputVariables(updated);
    };

    const handleOutputChange = (index, key, value) => {
        const updated = outputVariables.map((o, i) => i === index ? { ...o, [key]: value } : o);
        setOutputVariables(updated);
        writeOutputVariables(updated);
    };

    const handleRemoveOutput = (index) => {
        const updated = outputVariables.filter((_, i) => i !== index);
        setOutputVariables(updated);
        writeOutputVariables(updated);
    };

    // ── Expression save (manual mode) ────────────
    const handleExpressionSave = () => {
        writeExpression(expressionText);
    };

    return (
        <div className="space-y-4">

            {/* ── Route indicator ────────────────── */}
            <div className="flex items-center gap-3 px-3.5 py-3 bg-slate-50 border border-slate-200 rounded-xl">
                <ArrowRight className="w-4 h-4 text-slate-400 flex-shrink-0" />
                <div className="min-w-0">
                    <p className="text-[11px] text-slate-400 uppercase tracking-wide font-medium">This flow goes to</p>
                    <p className="text-sm font-semibold text-slate-700 truncate">{targetName}</p>
                </div>
            </div>

            {/* ── Default Flow Toggle ────────────── */}
            {isGatewaySource && (
                <div className="rounded-xl border border-slate-200 bg-white p-3.5">
                    <div className="flex items-center justify-between">
                        <div>
                            <p className="text-sm font-medium text-slate-700">Default Path</p>
                            <p className="text-[11px] text-slate-400">Followed when no other conditions match</p>
                        </div>
                        <button
                            onClick={handleToggleDefault}
                            className={cn(
                                'relative inline-flex h-6 w-11 items-center rounded-full transition-colors',
                                isDefaultFlow ? 'bg-emerald-500' : 'bg-slate-200'
                            )}
                        >
                            <span className={cn(
                                'inline-block h-4 w-4 transform rounded-full bg-white shadow-sm transition-transform',
                                isDefaultFlow ? 'translate-x-6' : 'translate-x-1'
                            )} />
                        </button>
                    </div>
                </div>
            )}

            {isDefaultFlow && (
                <div className="px-3.5 py-2.5 bg-emerald-50 border border-emerald-200 rounded-xl">
                    <div className="flex items-start gap-2.5">
                        <Shield className="w-3.5 h-3.5 text-emerald-500 mt-0.5 flex-shrink-0" />
                        <p className="text-[11px] text-emerald-700 leading-relaxed">
                            This is the <strong>default path</strong>. It will be followed when no other outgoing flow's condition matches. No condition expression needed.
                        </p>
                    </div>
                </div>
            )}

            {/* ── Condition Builder (non-default flows) ── */}
            {!isDefaultFlow && (
                <>
                    {/* Mode Toggle */}
                    <div className="flex items-center bg-slate-100 rounded-lg p-1">
                        <button
                            onClick={() => setMode('visual')}
                            className={cn(
                                'flex-1 flex items-center justify-center gap-1.5 px-3 py-2 rounded-md text-xs font-medium transition-all',
                                mode === 'visual'
                                    ? 'bg-white text-slate-900 shadow-sm'
                                    : 'text-slate-500 hover:text-slate-700'
                            )}
                        >
                            <Eye className="w-3.5 h-3.5" />
                            Visual Builder
                        </button>
                        <button
                            onClick={() => setMode('expression')}
                            className={cn(
                                'flex-1 flex items-center justify-center gap-1.5 px-3 py-2 rounded-md text-xs font-medium transition-all',
                                mode === 'expression'
                                    ? 'bg-white text-slate-900 shadow-sm'
                                    : 'text-slate-500 hover:text-slate-700'
                            )}
                        >
                            <Code2 className="w-3.5 h-3.5" />
                            Expression
                        </button>
                    </div>

                    {mode === 'visual' ? (
                        <div className="space-y-4">

                            {/* ── WHEN Section ────────────────── */}
                            <div>
                                <div className="flex items-center gap-2 mb-3">
                                    <span className="text-[10px] font-bold uppercase tracking-wider text-violet-500 bg-violet-50 px-2.5 py-0.5 rounded-md">
                                        When
                                    </span>
                                    <div className="h-px flex-1 bg-violet-100" />
                                </div>

                                {conditions.length === 0 ? (
                                    <div className="py-6 text-center border-2 border-dashed border-slate-200 rounded-xl">
                                        <p className="text-xs text-slate-400 mb-2.5">No conditions defined yet</p>
                                        <button
                                            onClick={handleAddCondition}
                                            className="inline-flex items-center gap-1.5 px-3.5 py-2 text-xs font-medium text-violet-600 bg-violet-50 rounded-lg hover:bg-violet-100 transition-colors"
                                        >
                                            <Plus className="w-3.5 h-3.5" />
                                            Add Condition
                                        </button>
                                    </div>
                                ) : (
                                    <div className="space-y-2">
                                        {conditions.map((condition, index) => (
                                            <React.Fragment key={index}>
                                                {/* AND separator */}
                                                {index > 0 && (
                                                    <div className="flex items-center gap-2 py-0.5">
                                                        <div className="h-px flex-1 bg-slate-100" />
                                                        <span className="text-[10px] font-bold uppercase text-slate-400 bg-slate-100 px-2 py-0.5 rounded">AND</span>
                                                        <div className="h-px flex-1 bg-slate-100" />
                                                    </div>
                                                )}

                                                {/* Condition Row */}
                                                <div className="p-3 bg-white border border-slate-200 rounded-xl transition-all hover:border-violet-200">
                                                    <div className="flex items-start gap-2">
                                                        <div className="flex-1 space-y-2">
                                                            {/* Field */}
                                                            <select
                                                                className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-700 focus:outline-none focus:ring-2 focus:ring-violet-500/20 focus:border-violet-400 transition-all"
                                                                value={condition.field}
                                                                onChange={(e) => handleConditionChange(index, 'field', e.target.value)}
                                                            >
                                                                <option value="">Select field…</option>
                                                                {Object.entries(
                                                                    availableFields.reduce((groups, f) => {
                                                                        const src = f.source || 'other';
                                                                        (groups[src] = groups[src] || []).push(f);
                                                                        return groups;
                                                                    }, {})
                                                                ).map(([source, fields]) => (
                                                                    <optgroup key={source} label={
                                                                        source === 'memo' ? '📝 Memo Fields'
                                                                            : source === 'initiator' ? '👤 Initiator'
                                                                                : source === 'form' ? '📋 Form Fields'
                                                                                    : '📦 Other'
                                                                    }>
                                                                        {fields.map(f => (
                                                                            <option key={f.id} value={f.id}>{f.label}</option>
                                                                        ))}
                                                                    </optgroup>
                                                                ))}
                                                            </select>

                                                            {/* Operator */}
                                                            <select
                                                                className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-600 focus:outline-none focus:ring-2 focus:ring-violet-500/20 focus:border-violet-400 transition-all"
                                                                value={condition.operator}
                                                                onChange={(e) => handleConditionChange(index, 'operator', e.target.value)}
                                                            >
                                                                {OPERATORS.map(op => (
                                                                    <option key={op.value} value={op.value}>{op.label}</option>
                                                                ))}
                                                            </select>

                                                            {/* Value */}
                                                            {!['is_empty', 'is_not_empty'].includes(condition.operator) && (() => {
                                                                const field = availableFields.find(f => f.id === condition.field);
                                                                if (field?.options?.length > 0) {
                                                                    return (
                                                                        <select
                                                                            className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-700 font-medium focus:outline-none focus:ring-2 focus:ring-violet-500/20 focus:border-violet-400 transition-all"
                                                                            value={condition.value}
                                                                            onChange={(e) => handleConditionChange(index, 'value', e.target.value)}
                                                                        >
                                                                            <option value="">Select…</option>
                                                                            {field.options.map(opt => (
                                                                                <option key={opt} value={opt}>{opt}</option>
                                                                            ))}
                                                                        </select>
                                                                    );
                                                                }
                                                                return (
                                                                    <input
                                                                        className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-700 font-medium focus:outline-none focus:ring-2 focus:ring-violet-500/20 focus:border-violet-400 transition-all placeholder:text-slate-300"
                                                                        value={condition.value}
                                                                        onChange={(e) => handleConditionChange(index, 'value', e.target.value)}
                                                                        placeholder="Enter value…"
                                                                        type={field?.type === 'number' ? 'number' : 'text'}
                                                                    />
                                                                );
                                                            })()}
                                                        </div>

                                                        <button
                                                            onClick={() => handleRemoveCondition(index)}
                                                            className="p-2 text-slate-300 hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors mt-1"
                                                        >
                                                            <Trash2 className="w-3.5 h-3.5" />
                                                        </button>
                                                    </div>
                                                </div>
                                            </React.Fragment>
                                        ))}

                                        {/* Add another condition */}
                                        <button
                                            onClick={handleAddCondition}
                                            className="w-full flex items-center justify-center gap-1.5 py-2.5 text-[11px] font-medium text-violet-500 bg-violet-50 border border-violet-200 rounded-xl hover:bg-violet-100 transition-colors"
                                        >
                                            <Plus className="w-3 h-3" />
                                            Add AND condition
                                        </button>
                                    </div>
                                )}
                            </div>

                            {/* ── Divider ─────────────────────── */}
                            {conditions.length > 0 && (
                                <div className="flex items-center gap-2">
                                    <div className="h-px flex-1 bg-slate-100" />
                                    <ArrowDown className="w-3.5 h-3.5 text-slate-300" />
                                    <div className="h-px flex-1 bg-slate-100" />
                                </div>
                            )}

                            {/* ── SET Variables Section ────────── */}
                            <div>
                                <div className="flex items-center gap-2 mb-3">
                                    <span className="text-[10px] font-bold uppercase tracking-wider text-amber-600 bg-amber-50 px-2.5 py-0.5 rounded-md">
                                        Set Variables
                                    </span>
                                    <div className="h-px flex-1 bg-amber-100" />
                                    <span className="text-[10px] text-slate-400">optional</span>
                                </div>

                                {outputVariables.length > 0 && (
                                    <div className="space-y-2 mb-2.5">
                                        {outputVariables.map((ov, oi) => (
                                            <div key={oi} className="flex items-center gap-2 p-2.5 bg-amber-50/60 border border-amber-100 rounded-lg">
                                                <Variable className="w-3.5 h-3.5 text-amber-400 flex-shrink-0" />
                                                <input
                                                    type="text"
                                                    className="flex-1 px-2.5 py-1.5 bg-white border border-amber-200 rounded-md text-sm text-slate-700 focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-400 transition-all placeholder:text-slate-300"
                                                    value={ov.name}
                                                    onChange={(e) => handleOutputChange(oi, 'name', e.target.value)}
                                                    placeholder="Variable name"
                                                />
                                                <span className="text-xs text-amber-500 font-bold">=</span>
                                                <input
                                                    type="text"
                                                    className="flex-1 px-2.5 py-1.5 bg-white border border-amber-200 rounded-md text-sm text-slate-700 font-medium focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-400 transition-all placeholder:text-slate-300"
                                                    value={ov.value}
                                                    onChange={(e) => handleOutputChange(oi, 'value', e.target.value)}
                                                    placeholder="Value"
                                                />
                                                <button
                                                    onClick={() => handleRemoveOutput(oi)}
                                                    className="p-1.5 text-amber-300 hover:text-red-500 hover:bg-red-50 rounded-md transition-colors"
                                                >
                                                    <Trash2 className="w-3 h-3" />
                                                </button>
                                            </div>
                                        ))}
                                    </div>
                                )}

                                <button
                                    onClick={handleAddOutput}
                                    className="flex items-center gap-1.5 text-[11px] font-medium text-amber-600 hover:text-amber-700 transition-colors px-1"
                                >
                                    <Variable className="w-3 h-3" />
                                    Set a variable (e.g. Approval Level, Risk Category)
                                </button>
                            </div>

                            {/* ── Expression preview ──────────── */}
                            {conditions.length > 0 && (
                                <div className="px-3.5 py-2.5 bg-slate-800 rounded-xl">
                                    <div className="flex items-center gap-2 mb-1.5">
                                        <Code2 className="w-3 h-3 text-slate-400" />
                                        <span className="text-[10px] text-slate-400 uppercase tracking-wide">Generated Expression</span>
                                    </div>
                                    <code className="text-[12px] text-emerald-300 font-mono break-all leading-relaxed">
                                        {conditionsToExpression(conditions)}
                                    </code>
                                </div>
                            )}
                        </div>

                    ) : (
                        /* ── Expression Mode ─────────────── */
                        <div className="space-y-3">
                            <div className="rounded-xl overflow-hidden border border-slate-700">
                                <div className="px-3 py-2 bg-slate-800 border-b border-slate-700 flex items-center gap-2">
                                    <Code2 className="w-3 h-3 text-slate-400" />
                                    <span className="text-[10px] text-slate-400 uppercase tracking-wide">JUEL Expression</span>
                                </div>
                                <textarea
                                    className="w-full bg-slate-900 text-emerald-300 font-mono text-sm p-3.5 border-0 resize-none focus:ring-0 focus:outline-none placeholder-slate-500"
                                    value={expressionText}
                                    onChange={(e) => setExpressionText(e.target.value)}
                                    onBlur={handleExpressionSave}
                                    placeholder="${amount > 500000}"
                                    rows={4}
                                />
                            </div>
                            <div className="px-3.5 py-2.5 bg-gradient-to-r from-blue-50/80 to-indigo-50/80 border border-blue-100 rounded-xl">
                                <p className="text-[11px] text-blue-700 leading-relaxed">
                                    💡 Use JUEL expressions. Variables: <code className="mx-0.5 px-1.5 py-0.5 bg-blue-100 rounded text-[10px]">{'${fieldName}'}</code>
                                    <br />
                                    Example: <code className="px-1.5 py-0.5 bg-blue-100 rounded text-[10px]">{'${amount > 500000 && category == "urgent"}'}</code>
                                </p>
                            </div>
                        </div>
                    )}
                </>
            )}
        </div>
    );
}

// ═══════════════════════════════════════════════════════════════════
//  HELPERS
// ═══════════════════════════════════════════════════════════════════

function parseExpressionToConditions(expr) {
    if (!expr || !expr.trim()) return [];

    const cleaned = expr.replace(/^\$\{/, '').replace(/\}$/, '').trim();
    if (!cleaned) return [];

    const parts = cleaned.split(/\s*&&\s*/);
    return parts.map(part => {
        // Support dotted field names like memo.subject, initiator.department
        const match = part.match(/^([\w.]+)\s*(==|!=|>|<|>=|<=)\s*(.+)$/);
        if (match) {
            const [, field, op, val] = match;
            const operator = {
                '==': 'equals',
                '!=': 'not_equals',
                '>': 'greater_than',
                '<': 'less_than',
                '>=': 'greater_or_equal',
                '<=': 'less_or_equal',
            }[op] || 'equals';
            return { field, operator, value: val.replace(/^["']|["']$/g, '') };
        }
        // Handle contains
        const containsMatch = part.match(/^([\w.]+)\.contains\("(.+)"\)$/);
        if (containsMatch) {
            return { field: containsMatch[1], operator: 'contains', value: containsMatch[2] };
        }
        // Handle startsWith
        const startsMatch = part.match(/^([\w.]+)\.startsWith\("(.+)"\)$/);
        if (startsMatch) {
            return { field: startsMatch[1], operator: 'starts_with', value: startsMatch[2] };
        }
        // Handle is_empty
        const emptyMatch = part.match(/^([\w.]+)\s*==\s*null$/);
        if (emptyMatch) {
            return { field: emptyMatch[1], operator: 'is_empty', value: '' };
        }
        // Handle is_not_empty
        const notEmptyMatch = part.match(/^([\w.]+)\s*!=\s*null$/);
        if (notEmptyMatch) {
            return { field: notEmptyMatch[1], operator: 'is_not_empty', value: '' };
        }
        return { field: '', operator: 'equals', value: '' };
    }).filter(c => c.field || c.value);
}

function conditionsToExpression(conditions) {
    if (!conditions || conditions.length === 0) return '';

    const parts = conditions
        .filter(c => c.field)
        .map(c => {
            const op = OPERATORS.find(o => o.value === c.operator);
            const symbol = op?.symbol || '==';

            if (c.operator === 'is_empty') return `${c.field} == null`;
            if (c.operator === 'is_not_empty') return `${c.field} != null`;
            if (c.operator === 'contains') return `${c.field}.contains("${c.value}")`;
            if (c.operator === 'starts_with') return `${c.field}.startsWith("${c.value}")`;

            // Quote strings, leave numbers raw
            const value = isNaN(c.value) ? `"${c.value}"` : c.value;
            return `${c.field} ${symbol} ${value}`;
        });

    if (parts.length === 0) return '';
    return '${' + parts.join(' && ') + '}';
}

function parseOutputVariables(bo) {
    try {
        const docText = bo?.documentation?.[0]?.text || '';
        const docData = JSON.parse(docText);
        return docData.outputVariables || [];
    } catch {
        return [];
    }
}
