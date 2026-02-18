import React, { useState, useEffect, useRef } from 'react';
import { MemoApi } from '../lib/api';
import { Card, CardContent } from './ui/card';
import { Button } from './ui/button';
import { Badge } from './ui/badge';
import {
    Plus, Trash2, ArrowDown, GitBranch, Copy, GripVertical,
    ChevronDown, ChevronUp, Zap, ArrowRight, Variable,
    AlertCircle, Sparkles, Settings2
} from 'lucide-react';
import { cn } from '../lib/utils';

/**
 * ConditionBuilder — Premium enterprise condition builder for workflow branching.
 * 
 * Features:
 * - Card-based rules with colored accent borders
 * - WHEN conditions with human-readable operators 
 * - THEN routing to target step
 * - SET output variables (new — replaces DMN)
 * - ELSE default fallback
 * - Clean, professional, enterprise-grade UI
 */

// ─── Operator definitions ────────────────────────────────────────
const OPERATORS = {
    number: [
        { value: 'EQUALS', label: 'is equal to' },
        { value: 'NOT_EQUALS', label: 'is not equal to' },
        { value: 'GREATER_THAN', label: 'is greater than' },
        { value: 'GREATER_THAN_OR_EQUALS', label: 'is at least' },
        { value: 'LESS_THAN', label: 'is less than' },
        { value: 'LESS_THAN_OR_EQUALS', label: 'is at most' },
    ],
    enum: [
        { value: 'EQUALS', label: 'is' },
        { value: 'NOT_EQUALS', label: 'is not' },
        { value: 'IN', label: 'is one of' },
    ],
    text: [
        { value: 'EQUALS', label: 'is' },
        { value: 'NOT_EQUALS', label: 'is not' },
        { value: 'CONTAINS', label: 'contains' },
        { value: 'STARTS_WITH', label: 'starts with' },
    ],
    boolean: [
        { value: 'EQUALS', label: 'is' },
    ],
    default: [
        { value: 'EQUALS', label: 'is' },
        { value: 'NOT_EQUALS', label: 'is not' },
    ],
};

// ─── Rule accent colors (cycle through for visual distinction) ────
const RULE_COLORS = [
    { border: 'border-l-violet-500', bg: 'bg-violet-50', text: 'text-violet-700', badge: 'bg-violet-100 text-violet-700' },
    { border: 'border-l-sky-500', bg: 'bg-sky-50', text: 'text-sky-700', badge: 'bg-sky-100 text-sky-700' },
    { border: 'border-l-amber-500', bg: 'bg-amber-50', text: 'text-amber-700', badge: 'bg-amber-100 text-amber-700' },
    { border: 'border-l-emerald-500', bg: 'bg-emerald-50', text: 'text-emerald-700', badge: 'bg-emerald-100 text-emerald-700' },
    { border: 'border-l-rose-500', bg: 'bg-rose-50', text: 'text-rose-700', badge: 'bg-rose-100 text-rose-700' },
    { border: 'border-l-cyan-500', bg: 'bg-cyan-50', text: 'text-cyan-700', badge: 'bg-cyan-100 text-cyan-700' },
];

const getOperators = (type) => OPERATORS[type] || OPERATORS.default;

// ─── Default fallback variables ──────────────────────────────────
const DEFAULT_VARIABLES = [
    { name: 'memo.amount', label: 'Amount', type: 'number', source: 'memo' },
    { name: 'memo.priority', label: 'Priority', type: 'enum', source: 'memo', options: ['LOW', 'MEDIUM', 'HIGH', 'URGENT'] },
    { name: 'memo.category', label: 'Category', type: 'text', source: 'memo' },
    { name: 'initiator.role', label: 'Initiator Role', type: 'enum', source: 'initiator', options: ['OFFICER', 'MANAGER', 'DIRECTOR', 'BOARD'] },
    { name: 'initiator.department', label: 'Initiator Department', type: 'text', source: 'initiator' },
    { name: 'initiator.branch', label: 'Initiator Branch', type: 'text', source: 'initiator' },
];

// ═══════════════════════════════════════════════════════════════════
//  MAIN COMPONENT
// ═══════════════════════════════════════════════════════════════════

const ConditionBuilder = ({
    topicId,
    conditions = [],
    defaultTarget = '',
    availableSteps = [],
    onChange,
    label = 'Routing Rules'
}) => {
    const [variables, setVariables] = useState([]);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (topicId) loadVariables();
    }, [topicId]);

    const loadVariables = async () => {
        try {
            setLoading(true);
            const vars = await MemoApi.getWorkflowVariables(topicId);
            setVariables(vars?.length ? vars : DEFAULT_VARIABLES);
        } catch {
            setVariables(DEFAULT_VARIABLES);
        } finally {
            setLoading(false);
        }
    };

    // ── Rule CRUD ─────────────────────────────────
    const addRule = () => {
        const newRule = {
            id: `rule_${Date.now()}`,
            label: `Rule ${conditions.length + 1}`,
            field: variables[0]?.name || '',
            operator: 'EQUALS',
            value: '',
            targetStep: availableSteps[0]?.id || '',
            targetStepName: availableSteps[0]?.name || '',
            outputVariables: [],
            // Support multiple conditions per rule (AND logic)
            extraConditions: [],
        };
        onChange([...conditions, newRule], defaultTarget);
    };

    const updateRule = (index, updates) => {
        const updated = [...conditions];
        updated[index] = { ...updated[index], ...updates };

        // Reset operator/value when field changes
        if (updates.field) {
            const variable = variables.find(v => v.name === updates.field);
            const ops = getOperators(variable?.type);
            updated[index].operator = ops[0]?.value || 'EQUALS';
            updated[index].value = '';
        }

        onChange(updated, defaultTarget);
    };

    const removeRule = (index) => {
        onChange(conditions.filter((_, i) => i !== index), defaultTarget);
    };

    const duplicateRule = (index) => {
        const original = conditions[index];
        const clone = {
            ...JSON.parse(JSON.stringify(original)),
            id: `rule_${Date.now()}`,
            label: `${original.label || 'Rule'} (copy)`,
        };
        const updated = [...conditions];
        updated.splice(index + 1, 0, clone);
        onChange(updated, defaultTarget);
    };

    const moveRule = (index, direction) => {
        const newIndex = index + direction;
        if (newIndex < 0 || newIndex >= conditions.length) return;
        const updated = [...conditions];
        [updated[index], updated[newIndex]] = [updated[newIndex], updated[index]];
        onChange(updated, defaultTarget);
    };

    // ── Extra conditions within a rule (AND logic) ──
    const addExtraCondition = (ruleIndex) => {
        const updated = [...conditions];
        const extras = updated[ruleIndex].extraConditions || [];
        extras.push({ field: variables[0]?.name || '', operator: 'EQUALS', value: '' });
        updated[ruleIndex].extraConditions = extras;
        onChange(updated, defaultTarget);
    };

    const updateExtraCondition = (ruleIndex, condIndex, updates) => {
        const updated = [...conditions];
        const extras = [...(updated[ruleIndex].extraConditions || [])];
        extras[condIndex] = { ...extras[condIndex], ...updates };

        if (updates.field) {
            const variable = variables.find(v => v.name === updates.field);
            const ops = getOperators(variable?.type);
            extras[condIndex].operator = ops[0]?.value || 'EQUALS';
            extras[condIndex].value = '';
        }

        updated[ruleIndex].extraConditions = extras;
        onChange(updated, defaultTarget);
    };

    const removeExtraCondition = (ruleIndex, condIndex) => {
        const updated = [...conditions];
        updated[ruleIndex].extraConditions = (updated[ruleIndex].extraConditions || []).filter((_, i) => i !== condIndex);
        onChange(updated, defaultTarget);
    };

    // ── Output variables within a rule ──────────────
    const addOutputVariable = (ruleIndex) => {
        const updated = [...conditions];
        const outputs = updated[ruleIndex].outputVariables || [];
        outputs.push({ name: '', value: '' });
        updated[ruleIndex].outputVariables = outputs;
        onChange(updated, defaultTarget);
    };

    const updateOutputVariable = (ruleIndex, outIndex, updates) => {
        const updated = [...conditions];
        const outputs = [...(updated[ruleIndex].outputVariables || [])];
        outputs[outIndex] = { ...outputs[outIndex], ...updates };
        updated[ruleIndex].outputVariables = outputs;
        onChange(updated, defaultTarget);
    };

    const removeOutputVariable = (ruleIndex, outIndex) => {
        const updated = [...conditions];
        updated[ruleIndex].outputVariables = (updated[ruleIndex].outputVariables || []).filter((_, i) => i !== outIndex);
        onChange(updated, defaultTarget);
    };

    if (loading) {
        return (
            <div className="flex items-center justify-center p-12">
                <div className="flex flex-col items-center gap-3">
                    <div className="animate-spin rounded-full h-8 w-8 border-2 border-violet-200 border-t-violet-600" />
                    <p className="text-sm text-slate-400">Loading variables…</p>
                </div>
            </div>
        );
    }

    return (
        <div className="space-y-4">
            {/* ─── Header ─────────────────────────── */}
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-2.5">
                    <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-violet-500 to-indigo-600 flex items-center justify-center shadow-md shadow-violet-500/20">
                        <GitBranch className="w-4 h-4 text-white" />
                    </div>
                    <div>
                        <h3 className="text-sm font-semibold text-slate-800">{label}</h3>
                        <p className="text-[11px] text-slate-400">
                            {conditions.length === 0 ? 'No rules defined' : `${conditions.length} rule${conditions.length !== 1 ? 's' : ''} configured`}
                        </p>
                    </div>
                </div>
                {conditions.length > 0 && (
                    <Badge className="bg-violet-100 text-violet-700 border-violet-200 text-[10px] font-medium px-2 py-0.5 rounded-md">
                        {conditions.length} rule{conditions.length !== 1 ? 's' : ''}
                    </Badge>
                )}
            </div>

            {/* ─── Info Banner ─────────────────────── */}
            <div className="px-3.5 py-2.5 bg-gradient-to-r from-violet-50/80 to-indigo-50/80 border border-violet-100 rounded-xl">
                <div className="flex items-start gap-2.5">
                    <Sparkles className="w-3.5 h-3.5 text-violet-500 mt-0.5 flex-shrink-0" />
                    <p className="text-[11px] text-violet-700 leading-relaxed">
                        Define rules to route memos based on their data. Rules are checked <strong>top to bottom</strong> — the first matching rule wins.
                        You can also <strong>set variables</strong> like approval level or risk category.
                    </p>
                </div>
            </div>

            {/* ─── Rules ──────────────────────────── */}
            {conditions.length === 0 ? (
                <EmptyState onAdd={addRule} />
            ) : (
                <div className="space-y-3">
                    {conditions.map((rule, index) => (
                        <RuleCard
                            key={rule.id}
                            rule={rule}
                            index={index}
                            total={conditions.length}
                            color={RULE_COLORS[index % RULE_COLORS.length]}
                            variables={variables}
                            availableSteps={availableSteps}
                            onUpdate={(updates) => updateRule(index, updates)}
                            onRemove={() => removeRule(index)}
                            onDuplicate={() => duplicateRule(index)}
                            onMove={(dir) => moveRule(index, dir)}
                            onAddExtraCondition={() => addExtraCondition(index)}
                            onUpdateExtraCondition={(ci, u) => updateExtraCondition(index, ci, u)}
                            onRemoveExtraCondition={(ci) => removeExtraCondition(index, ci)}
                            onAddOutput={() => addOutputVariable(index)}
                            onUpdateOutput={(oi, u) => updateOutputVariable(index, oi, u)}
                            onRemoveOutput={(oi) => removeOutputVariable(index, oi)}
                        />
                    ))}
                </div>
            )}

            {/* ─── Default / ELSE ─────────────────── */}
            {conditions.length > 0 && (
                <div className="rounded-xl border border-slate-200 bg-slate-50/50 p-4">
                    <div className="flex items-center gap-3">
                        <span className="text-[11px] font-bold uppercase tracking-wider text-slate-400 bg-slate-200/60 px-2.5 py-1 rounded-md">
                            Otherwise
                        </span>
                        <ArrowRight className="w-3.5 h-3.5 text-slate-300" />
                        <span className="text-xs text-slate-500">go to</span>
                        <select
                            className="flex-1 px-3 py-2 bg-white border border-slate-200 rounded-lg text-sm text-slate-700 font-medium focus:outline-none focus:ring-2 focus:ring-violet-500/20 focus:border-violet-400 transition-all"
                            value={defaultTarget}
                            onChange={(e) => onChange(conditions, e.target.value)}
                        >
                            <option value="">Select default step…</option>
                            {availableSteps.map((step) => (
                                <option key={step.id} value={step.id}>{step.name}</option>
                            ))}
                        </select>
                    </div>
                    <p className="text-[10px] text-slate-400 mt-2 ml-[104px]">
                        Used when no rules above match the memo data.
                    </p>
                </div>
            )}

            {/* ─── Add Rule ───────────────────────── */}
            <button
                onClick={addRule}
                className="w-full flex items-center justify-center gap-2 py-3 px-4 border-2 border-dashed border-violet-200 rounded-xl text-sm font-medium text-violet-600 hover:bg-violet-50 hover:border-violet-300 transition-all duration-200 group"
            >
                <Plus className="w-4 h-4 group-hover:scale-110 transition-transform" />
                Add Rule
            </button>
        </div>
    );
};

// ═══════════════════════════════════════════════════════════════════
//  EMPTY STATE
// ═══════════════════════════════════════════════════════════════════

const EmptyState = ({ onAdd }) => (
    <div className="py-10 text-center">
        <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-violet-100 to-indigo-100 flex items-center justify-center mx-auto mb-4 shadow-sm">
            <GitBranch className="w-7 h-7 text-violet-400" />
        </div>
        <h4 className="text-sm font-semibold text-slate-700 mb-1">No routing rules yet</h4>
        <p className="text-xs text-slate-400 max-w-[260px] mx-auto mb-5 leading-relaxed">
            Add rules to automatically route memos to different steps based on their data — like amount, priority, or department.
        </p>
        <Button
            onClick={onAdd}
            className="bg-gradient-to-r from-violet-500 to-indigo-600 hover:from-violet-600 hover:to-indigo-700 text-white rounded-xl px-5 h-9 text-sm font-medium shadow-lg shadow-violet-500/25"
        >
            <Plus className="w-4 h-4 mr-1.5" />
            Create First Rule
        </Button>
    </div>
);

// ═══════════════════════════════════════════════════════════════════
//  RULE CARD
// ═══════════════════════════════════════════════════════════════════

const RuleCard = ({
    rule,
    index,
    total,
    color,
    variables,
    availableSteps,
    onUpdate,
    onRemove,
    onDuplicate,
    onMove,
    onAddExtraCondition,
    onUpdateExtraCondition,
    onRemoveExtraCondition,
    onAddOutput,
    onUpdateOutput,
    onRemoveOutput,
}) => {
    const [collapsed, setCollapsed] = useState(false);
    const variable = variables.find(v => v.name === rule.field);
    const operators = getOperators(variable?.type);

    return (
        <div className={cn(
            'rounded-xl border border-slate-200 bg-white overflow-hidden transition-all duration-200 hover:shadow-md hover:border-slate-300',
            'border-l-[3px]',
            color.border,
        )}>
            {/* ── Card Header ──────────────────────── */}
            <div className="flex items-center justify-between px-4 py-3 border-b border-slate-100 bg-slate-50/50">
                <div className="flex items-center gap-2.5">
                    <GripVertical className="w-3.5 h-3.5 text-slate-300 cursor-grab" />
                    <div className="flex items-center gap-2">
                        <span className={cn('text-[10px] font-bold uppercase tracking-wide px-2 py-0.5 rounded-md', color.badge)}>
                            Rule {index + 1}
                        </span>
                        <input
                            type="text"
                            value={rule.label || ''}
                            onChange={(e) => onUpdate({ label: e.target.value })}
                            placeholder="Rule description…"
                            className="text-xs text-slate-500 bg-transparent border-0 outline-none focus:text-slate-700 placeholder:text-slate-300 w-[160px]"
                        />
                    </div>
                </div>
                <div className="flex items-center gap-0.5">
                    {index > 0 && (
                        <button onClick={() => onMove(-1)} className="p-1.5 text-slate-300 hover:text-slate-600 hover:bg-slate-100 rounded-lg transition-colors" title="Move up">
                            <ChevronUp className="w-3.5 h-3.5" />
                        </button>
                    )}
                    {index < total - 1 && (
                        <button onClick={() => onMove(1)} className="p-1.5 text-slate-300 hover:text-slate-600 hover:bg-slate-100 rounded-lg transition-colors" title="Move down">
                            <ChevronDown className="w-3.5 h-3.5" />
                        </button>
                    )}
                    <button onClick={onDuplicate} className="p-1.5 text-slate-300 hover:text-violet-600 hover:bg-violet-50 rounded-lg transition-colors" title="Duplicate rule">
                        <Copy className="w-3.5 h-3.5" />
                    </button>
                    <button onClick={() => setCollapsed(!collapsed)} className="p-1.5 text-slate-300 hover:text-slate-600 hover:bg-slate-100 rounded-lg transition-colors" title={collapsed ? 'Expand' : 'Collapse'}>
                        {collapsed ? <ChevronDown className="w-3.5 h-3.5" /> : <ChevronUp className="w-3.5 h-3.5" />}
                    </button>
                    <button onClick={onRemove} className="p-1.5 text-slate-300 hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors" title="Delete rule">
                        <Trash2 className="w-3.5 h-3.5" />
                    </button>
                </div>
            </div>

            {!collapsed && (
                <div className="p-4 space-y-4">
                    {/* ── WHEN Section ──────────────────── */}
                    <div>
                        <div className="flex items-center gap-2 mb-2.5">
                            <span className="text-[10px] font-bold uppercase tracking-wider text-violet-500 bg-violet-50 px-2 py-0.5 rounded-md">When</span>
                            <div className="h-px flex-1 bg-violet-100" />
                        </div>

                        {/* Primary condition */}
                        <ConditionRow
                            condition={{ field: rule.field, operator: rule.operator, value: rule.value }}
                            variables={variables}
                            operators={operators}
                            variable={variable}
                            onChange={(updates) => onUpdate(updates)}
                        />

                        {/* Extra conditions */}
                        {(rule.extraConditions || []).map((ec, ci) => {
                            const ecVar = variables.find(v => v.name === ec.field);
                            return (
                                <React.Fragment key={ci}>
                                    <div className="flex items-center gap-2 my-2">
                                        <div className="h-px flex-1 bg-slate-100" />
                                        <span className="text-[10px] font-bold uppercase text-slate-400 bg-slate-100 px-2 py-0.5 rounded">AND</span>
                                        <div className="h-px flex-1 bg-slate-100" />
                                    </div>
                                    <ConditionRow
                                        condition={ec}
                                        variables={variables}
                                        operators={getOperators(ecVar?.type)}
                                        variable={ecVar}
                                        onChange={(updates) => onUpdateExtraCondition(ci, updates)}
                                        onRemove={() => onRemoveExtraCondition(ci)}
                                        removable
                                    />
                                </React.Fragment>
                            );
                        })}

                        <button
                            onClick={onAddExtraCondition}
                            className="mt-2 flex items-center gap-1.5 text-[11px] font-medium text-violet-500 hover:text-violet-700 transition-colors px-1"
                        >
                            <Plus className="w-3 h-3" />
                            Add AND condition
                        </button>
                    </div>

                    {/* ── Divider ───────────────────────── */}
                    <div className="flex items-center gap-2">
                        <div className="h-px flex-1 bg-slate-100" />
                        <ArrowDown className="w-3.5 h-3.5 text-slate-300" />
                        <div className="h-px flex-1 bg-slate-100" />
                    </div>

                    {/* ── THEN Section ──────────────────── */}
                    <div>
                        <div className="flex items-center gap-2 mb-2.5">
                            <span className="text-[10px] font-bold uppercase tracking-wider text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-md">Then</span>
                            <div className="h-px flex-1 bg-emerald-100" />
                        </div>

                        <div className="flex items-center gap-3 px-3 py-2.5 bg-emerald-50/50 border border-emerald-100 rounded-lg">
                            <ArrowRight className="w-4 h-4 text-emerald-400 flex-shrink-0" />
                            <span className="text-xs text-emerald-700 font-medium whitespace-nowrap">Route to</span>
                            <select
                                className="flex-1 px-3 py-1.5 bg-white border border-emerald-200 rounded-lg text-sm text-slate-700 font-medium focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-400 transition-all"
                                value={rule.targetStep}
                                onChange={(e) => {
                                    const step = availableSteps.find(s => s.id === e.target.value);
                                    onUpdate({ targetStep: e.target.value, targetStepName: step?.name || '' });
                                }}
                            >
                                <option value="">Select step…</option>
                                {availableSteps.map((step) => (
                                    <option key={step.id} value={step.id}>{step.name}</option>
                                ))}
                            </select>
                        </div>
                    </div>

                    {/* ── SET Section (Output Variables) ── */}
                    <div>
                        <div className="flex items-center gap-2 mb-2.5">
                            <span className="text-[10px] font-bold uppercase tracking-wider text-amber-600 bg-amber-50 px-2 py-0.5 rounded-md">Set Variables</span>
                            <div className="h-px flex-1 bg-amber-100" />
                            <span className="text-[10px] text-slate-400">optional</span>
                        </div>

                        {(rule.outputVariables || []).length > 0 && (
                            <div className="space-y-2 mb-2">
                                {(rule.outputVariables || []).map((ov, oi) => (
                                    <OutputVariableRow
                                        key={oi}
                                        output={ov}
                                        onChange={(updates) => onUpdateOutput(oi, updates)}
                                        onRemove={() => onRemoveOutput(oi)}
                                    />
                                ))}
                            </div>
                        )}

                        <button
                            onClick={onAddOutput}
                            className="flex items-center gap-1.5 text-[11px] font-medium text-amber-600 hover:text-amber-700 transition-colors px-1"
                        >
                            <Variable className="w-3 h-3" />
                            Set a variable (e.g. Approval Level, Risk Category)
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
};

// ═══════════════════════════════════════════════════════════════════
//  CONDITION ROW
// ═══════════════════════════════════════════════════════════════════

const ConditionRow = ({ condition, variables, operators, variable, onChange, onRemove, removable }) => (
    <div className="flex items-center gap-2 flex-wrap">
        {/* Variable */}
        <select
            className="px-3 py-2 bg-white border border-slate-200 rounded-lg text-sm text-slate-700 focus:outline-none focus:ring-2 focus:ring-violet-500/20 focus:border-violet-400 transition-all min-w-[140px]"
            value={condition.field}
            onChange={(e) => onChange({ field: e.target.value })}
        >
            <option value="">Select field…</option>
            {Object.entries(
                variables.reduce((groups, v) => {
                    const src = v.source || 'other';
                    (groups[src] = groups[src] || []).push(v);
                    return groups;
                }, {})
            ).map(([source, vars]) => (
                <optgroup key={source} label={source === 'memo' ? '📝 Memo Fields' : source === 'initiator' ? '👤 Initiator' : source === 'form' ? '📋 Form Fields' : '📦 Other'}>
                    {vars.map((v) => (
                        <option key={v.name} value={v.name}>{v.label}</option>
                    ))}
                </optgroup>
            ))}
        </select>

        {/* Operator */}
        <select
            className="px-3 py-2 bg-white border border-slate-200 rounded-lg text-sm text-slate-600 focus:outline-none focus:ring-2 focus:ring-violet-500/20 focus:border-violet-400 transition-all"
            value={condition.operator}
            onChange={(e) => onChange({ operator: e.target.value })}
        >
            {operators.map((op) => (
                <option key={op.value} value={op.value}>{op.label}</option>
            ))}
        </select>

        {/* Value */}
        {variable?.options?.length > 0 ? (
            <select
                className="px-3 py-2 bg-white border border-slate-200 rounded-lg text-sm text-slate-700 font-medium focus:outline-none focus:ring-2 focus:ring-violet-500/20 focus:border-violet-400 transition-all min-w-[120px]"
                value={condition.value}
                onChange={(e) => onChange({ value: e.target.value })}
            >
                <option value="">Select…</option>
                {variable.options.map((opt) => (
                    <option key={opt} value={opt}>{opt}</option>
                ))}
            </select>
        ) : (
            <input
                type={variable?.type === 'number' ? 'number' : 'text'}
                className="px-3 py-2 bg-white border border-slate-200 rounded-lg text-sm text-slate-700 font-medium focus:outline-none focus:ring-2 focus:ring-violet-500/20 focus:border-violet-400 transition-all min-w-[120px] max-w-[160px]"
                value={condition.value}
                onChange={(e) => onChange({ value: e.target.value })}
                placeholder="Enter value…"
            />
        )}

        {/* Remove (for extra conditions) */}
        {removable && (
            <button onClick={onRemove} className="p-1.5 text-slate-300 hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors">
                <Trash2 className="w-3.5 h-3.5" />
            </button>
        )}
    </div>
);

// ═══════════════════════════════════════════════════════════════════
//  OUTPUT VARIABLE ROW
// ═══════════════════════════════════════════════════════════════════

const OutputVariableRow = ({ output, onChange, onRemove }) => (
    <div className="flex items-center gap-2 px-3 py-2 bg-amber-50/60 border border-amber-100 rounded-lg">
        <Variable className="w-3.5 h-3.5 text-amber-400 flex-shrink-0" />
        <input
            type="text"
            className="flex-1 px-2 py-1.5 bg-white border border-amber-200 rounded-md text-sm text-slate-700 focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-400 transition-all placeholder:text-slate-300"
            value={output.name}
            onChange={(e) => onChange({ name: e.target.value })}
            placeholder="Variable name (e.g. approvalLevel)"
        />
        <span className="text-xs text-amber-500 font-bold">=</span>
        <input
            type="text"
            className="flex-1 px-2 py-1.5 bg-white border border-amber-200 rounded-md text-sm text-slate-700 font-medium focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-400 transition-all placeholder:text-slate-300"
            value={output.value}
            onChange={(e) => onChange({ value: e.target.value })}
            placeholder="Value (e.g. DIRECTOR)"
        />
        <button onClick={onRemove} className="p-1 text-amber-300 hover:text-red-500 hover:bg-red-50 rounded-md transition-colors">
            <Trash2 className="w-3 h-3" />
        </button>
    </div>
);

export default ConditionBuilder;
