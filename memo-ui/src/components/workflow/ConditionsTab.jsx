import React, { useState, useCallback, useEffect } from 'react';
import { GitBranch, Code2, ArrowRight, Plus, Trash2, ChevronRight, ToggleLeft, ToggleRight, Sparkles } from 'lucide-react';

/**
 * ConditionsTab — Condition editing for gateways and sequence flows.
 * 
 * For SequenceFlow: edit conditions (visual builder + expression mode), default flow toggle
 * For ExclusiveGateway: show list of outgoing flows with condition summaries
 * For UserTask: show step-level branching using ConditionBuilder
 */

// Operators available for conditions
const OPERATORS = [
    { value: 'equals', label: 'equals', symbol: '==' },
    { value: 'not_equals', label: 'not equals', symbol: '!=' },
    { value: 'greater_than', label: 'greater than', symbol: '>' },
    { value: 'less_than', label: 'less than', symbol: '<' },
    { value: 'greater_or_equal', label: '≥', symbol: '>=' },
    { value: 'less_or_equal', label: '≤', symbol: '<=' },
    { value: 'contains', label: 'contains', symbol: 'contains' },
    { value: 'starts_with', label: 'starts with', symbol: 'startsWith' },
    { value: 'is_empty', label: 'is empty', symbol: '== null' },
    { value: 'is_not_empty', label: 'is not empty', symbol: '!= null' },
];

export default function ConditionsTab({
    element,
    modelerRef,
    onSelectFlow, // callback to select a different flow element
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

    // Fallback: shouldn't get here, but just in case
    return (
        <div className="p-4 text-center text-slate-400 text-sm">
            <p>Conditions are not available for this element type.</p>
        </div>
    );
}

/**
 * ExclusiveGateway view — shows all outgoing flows with condition previews.
 */
function GatewayConditionsView({ element, modelerRef, onSelectFlow }) {
    const bo = element?.businessObject;
    const outgoing = bo?.outgoing || [];

    if (outgoing.length === 0) {
        return (
            <div className="p-6 text-center">
                <div className="w-14 h-14 rounded-2xl bg-amber-50 flex items-center justify-center mx-auto mb-3">
                    <GitBranch className="w-7 h-7 text-amber-400" />
                </div>
                <p className="text-sm font-medium text-slate-600">No outgoing flows</p>
                <p className="text-xs text-slate-400 mt-1">
                    Add sequence flows from this gateway to define decision paths.
                </p>
            </div>
        );
    }

    const defaultFlowId = bo?.default?.id;

    return (
        <div className="space-y-4">
            <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl">
                <div className="flex items-start gap-2">
                    <Sparkles className="w-4 h-4 text-amber-600 flex-shrink-0 mt-0.5" />
                    <p className="text-xs text-amber-800 leading-relaxed">
                        Click on an outgoing <strong>sequence flow</strong> (arrow) in the diagram to configure its condition.
                        Each path needs a condition expression to determine when it should be followed.
                    </p>
                </div>
            </div>

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
                            className="w-full text-left p-3 bg-white border border-slate-200 rounded-xl hover:border-blue-300 hover:shadow-sm transition-all group"
                        >
                            <div className="flex items-center justify-between">
                                <div className="flex items-center gap-2">
                                    <div className={`w-7 h-7 rounded-lg flex items-center justify-center text-xs font-bold ${isDefault
                                            ? 'bg-slate-100 text-slate-500'
                                            : conditionExpr
                                                ? 'bg-emerald-100 text-emerald-700'
                                                : 'bg-red-50 text-red-400'
                                        }`}>
                                        {index + 1}
                                    </div>
                                    <div className="min-w-0">
                                        <div className="flex items-center gap-1.5">
                                            <ArrowRight className="w-3.5 h-3.5 text-slate-400" />
                                            <span className="text-sm font-medium text-slate-700 truncate">{targetName}</span>
                                        </div>
                                        {isDefault ? (
                                            <span className="text-xs text-slate-400 italic">Default flow (no condition needed)</span>
                                        ) : conditionExpr ? (
                                            <code className="text-xs text-emerald-600 bg-emerald-50 px-1.5 py-0.5 rounded font-mono block mt-0.5 truncate max-w-[220px]">
                                                {conditionExpr}
                                            </code>
                                        ) : (
                                            <span className="text-xs text-red-400">⚠ No condition set</span>
                                        )}
                                    </div>
                                </div>
                                <ChevronRight className="w-4 h-4 text-slate-300 group-hover:text-blue-500 transition-colors" />
                            </div>
                        </button>
                    );
                })}
            </div>
        </div>
    );
}

/**
 * SequenceFlow condition editor — visual builder + expression mode.
 */
function FlowConditionEditor({ element, modelerRef, topicId }) {
    const bo = element?.businessObject;
    const [mode, setMode] = useState('visual'); // 'visual' | 'expression'

    // Check if this is the default flow of its source gateway
    const sourceRef = bo?.sourceRef;
    const isGatewaySource = sourceRef?.$type === 'bpmn:ExclusiveGateway' || sourceRef?.$type === 'bpmn:InclusiveGateway';
    const isDefaultFlow = sourceRef?.default?.id === element?.id;

    // Parse existing condition expression
    const existingExpression = bo?.conditionExpression?.body || '';

    // Visual builder state
    const [conditions, setConditions] = useState(() => parseExpressionToConditions(existingExpression));
    const [expressionText, setExpressionText] = useState(existingExpression);
    const [availableFields, setAvailableFields] = useState([]);

    // Load available fields from topic
    useEffect(() => {
        // Default workflow fields
        setAvailableFields([
            { id: 'amount', label: 'Amount', type: 'number' },
            { id: 'category', label: 'Category', type: 'string' },
            { id: 'priority', label: 'Priority', type: 'string' },
            { id: 'department', label: 'Department', type: 'string' },
            { id: 'requestType', label: 'Request Type', type: 'string' },
            { id: 'status', label: 'Status', type: 'string' },
            { id: 'level', label: 'Level', type: 'number' },
        ]);
    }, [topicId]);

    const handleToggleDefault = useCallback(() => {
        if (!modelerRef?.current || !element) return;
        try {
            const modeler = modelerRef.current.getModeler ? modelerRef.current.getModeler() : modelerRef.current;
            const modeling = modeler.get('modeling');
            const registry = modeler.get('elementRegistry');

            // Find the source element in the registry
            const sourceElement = registry.get(sourceRef.id);
            if (!sourceElement) return;

            if (isDefaultFlow) {
                // Remove default flow
                modeling.updateProperties(sourceElement, { default: undefined });
            } else {
                // Set as default flow
                modeling.updateProperties(sourceElement, { default: bo });
            }
        } catch (err) {
            console.error('Error toggling default flow:', err);
        }
    }, [element, modelerRef, isDefaultFlow, sourceRef, bo]);

    const writeExpression = useCallback((expr) => {
        if (!modelerRef?.current || !element) return;
        try {
            const modeler = modelerRef.current.getModeler ? modelerRef.current.getModeler() : modelerRef.current;
            const modeling = modeler.get('modeling');
            const moddle = modeler.get('moddle');

            if (expr && expr.trim()) {
                const conditionExpr = moddle.create('bpmn:FormalExpression', {
                    body: expr
                });
                modeling.updateProperties(element, { conditionExpression: conditionExpr });
            } else {
                modeling.updateProperties(element, { conditionExpression: undefined });
            }
        } catch (err) {
            console.error('Error writing condition expression:', err);
        }
    }, [element, modelerRef]);

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

    const handleExpressionSave = () => {
        writeExpression(expressionText);
    };

    return (
        <div className="space-y-4">
            {/* Default Flow Toggle (only for gateway outgoing flows) */}
            {isGatewaySource && (
                <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl">
                    <div className="flex items-center justify-between">
                        <div>
                            <p className="text-sm font-medium text-slate-700">Default Flow</p>
                            <p className="text-xs text-slate-400">Used when no other conditions match</p>
                        </div>
                        <button
                            onClick={handleToggleDefault}
                            className="flex items-center gap-1.5"
                        >
                            {isDefaultFlow ? (
                                <ToggleRight className="w-8 h-8 text-emerald-500" />
                            ) : (
                                <ToggleLeft className="w-8 h-8 text-slate-300" />
                            )}
                        </button>
                    </div>
                </div>
            )}

            {isDefaultFlow && (
                <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-xl">
                    <p className="text-xs text-emerald-700">
                        ✅ This is the <strong>default path</strong>. It will be followed when no other outgoing flow's condition is true. No condition expression is needed.
                    </p>
                </div>
            )}

            {!isDefaultFlow && (
                <>
                    {/* Mode Toggle */}
                    <div className="flex items-center bg-slate-100 rounded-lg p-1">
                        <button
                            onClick={() => setMode('visual')}
                            className={`flex-1 flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-medium transition-all ${mode === 'visual'
                                    ? 'bg-white text-slate-900 shadow-sm'
                                    : 'text-slate-500 hover:text-slate-700'
                                }`}
                        >
                            <GitBranch className="w-3.5 h-3.5" />
                            Visual Builder
                        </button>
                        <button
                            onClick={() => setMode('expression')}
                            className={`flex-1 flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-medium transition-all ${mode === 'expression'
                                    ? 'bg-white text-slate-900 shadow-sm'
                                    : 'text-slate-500 hover:text-slate-700'
                                }`}
                        >
                            <Code2 className="w-3.5 h-3.5" />
                            Expression
                        </button>
                    </div>

                    {mode === 'visual' ? (
                        <div className="space-y-3">
                            {conditions.length === 0 && (
                                <div className="p-4 text-center border-2 border-dashed border-slate-200 rounded-xl">
                                    <p className="text-sm text-slate-400 mb-2">No conditions defined</p>
                                    <button
                                        onClick={handleAddCondition}
                                        className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-blue-600 bg-blue-50 rounded-lg hover:bg-blue-100 transition-colors"
                                    >
                                        <Plus className="w-3.5 h-3.5" />
                                        Add Condition
                                    </button>
                                </div>
                            )}

                            {conditions.map((condition, index) => (
                                <div key={index} className="p-3 bg-white border border-slate-200 rounded-xl space-y-2">
                                    {index > 0 && (
                                        <div className="flex items-center gap-2 -mt-1 mb-1">
                                            <div className="h-px flex-1 bg-slate-200" />
                                            <span className="text-[10px] font-bold text-slate-400 uppercase">AND</span>
                                            <div className="h-px flex-1 bg-slate-200" />
                                        </div>
                                    )}

                                    <div className="grid grid-cols-[1fr,auto] gap-2 items-start">
                                        <div className="space-y-2">
                                            {/* Field */}
                                            <select
                                                className="w-full p-2 border border-slate-200 rounded-lg text-sm bg-white focus:ring-2 focus:ring-blue-500"
                                                value={condition.field}
                                                onChange={(e) => handleConditionChange(index, 'field', e.target.value)}
                                            >
                                                <option value="">Select field...</option>
                                                {availableFields.map(f => (
                                                    <option key={f.id} value={f.id}>{f.label}</option>
                                                ))}
                                            </select>

                                            {/* Operator */}
                                            <select
                                                className="w-full p-2 border border-slate-200 rounded-lg text-sm bg-white focus:ring-2 focus:ring-blue-500"
                                                value={condition.operator}
                                                onChange={(e) => handleConditionChange(index, 'operator', e.target.value)}
                                            >
                                                {OPERATORS.map(op => (
                                                    <option key={op.value} value={op.value}>{op.label}</option>
                                                ))}
                                            </select>

                                            {/* Value (not shown for is_empty / is_not_empty) */}
                                            {!['is_empty', 'is_not_empty'].includes(condition.operator) && (
                                                <input
                                                    className="w-full p-2 border border-slate-200 rounded-lg text-sm bg-white focus:ring-2 focus:ring-blue-500"
                                                    value={condition.value}
                                                    onChange={(e) => handleConditionChange(index, 'value', e.target.value)}
                                                    placeholder="Enter value..."
                                                />
                                            )}
                                        </div>

                                        <button
                                            onClick={() => handleRemoveCondition(index)}
                                            className="p-2 text-slate-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors"
                                        >
                                            <Trash2 className="w-4 h-4" />
                                        </button>
                                    </div>
                                </div>
                            ))}

                            {conditions.length > 0 && (
                                <button
                                    onClick={handleAddCondition}
                                    className="w-full flex items-center justify-center gap-1.5 p-2.5 text-xs font-medium text-blue-600 bg-blue-50 border border-blue-200 rounded-xl hover:bg-blue-100 transition-colors"
                                >
                                    <Plus className="w-3.5 h-3.5" />
                                    Add Another Condition
                                </button>
                            )}
                        </div>
                    ) : (
                        <div className="space-y-3">
                            <div className="p-3 bg-slate-800 rounded-xl">
                                <textarea
                                    className="w-full bg-transparent text-emerald-300 font-mono text-sm p-0 border-0 resize-none focus:ring-0 focus:outline-none placeholder-slate-500"
                                    value={expressionText}
                                    onChange={(e) => setExpressionText(e.target.value)}
                                    onBlur={handleExpressionSave}
                                    placeholder="${amount > 500000}"
                                    rows={3}
                                />
                            </div>
                            <div className="p-3 bg-blue-50 border border-blue-200 rounded-xl">
                                <p className="text-xs text-blue-700">
                                    💡 Use JUEL expressions. Variables are accessible as
                                    <code className="mx-1 px-1 py-0.5 bg-blue-100 rounded text-[11px]">{'${fieldName}'}</code>.
                                    Example: <code className="px-1 py-0.5 bg-blue-100 rounded text-[11px]">{'${amount > 500000 && category == "urgent"}'}</code>
                                </p>
                            </div>
                        </div>
                    )}
                </>
            )}
        </div>
    );
}

// ── Helpers ──────────────────────────────────────────────────

function parseExpressionToConditions(expr) {
    if (!expr || !expr.trim()) return [];

    // Try to parse simple JUEL expressions like: ${amount > 500 && category == "urgent"}
    const cleaned = expr.replace(/^\$\{/, '').replace(/\}$/, '').trim();
    if (!cleaned) return [];

    const parts = cleaned.split(/\s*&&\s*/);
    return parts.map(part => {
        const match = part.match(/^(\w+)\s*(==|!=|>|<|>=|<=)\s*(.+)$/);
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
