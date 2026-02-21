import React, { useState, useEffect, useCallback } from 'react';
import { Plus, Trash2, GripVertical, Sparkles, Eye, Route, ChevronDown, ChevronRight, Variable, Loader2 } from 'lucide-react';
import { WorkflowConfigApi } from '../../lib/api';

/**
 * OutcomesTab — Predefined action type selection for task outcomes.
 *
 * Admins select from backend-served predefined action types (APPROVE, REJECT,
 * SEND_BACK, ESCALATE, CUSTOM). Each type auto-populates label, style, and
 * process variables (`decision`). Admins can customize the label and style.
 *
 * Data shape:
 * {
 *   "outcomeConfig": {
 *     "options": [
 *       { "actionType": "APPROVE", "label": "Approve Memo", "style": "success", "sets": { "decision": "APPROVED" } },
 *       { "actionType": "REJECT",  "label": "Reject Memo",  "style": "danger",  "sets": { "decision": "REJECTED" }, "requiresComment": true }
 *     ]
 *   }
 * }
 */

const STYLE_OPTIONS = [
    { value: 'success', label: 'Success (Green)', color: 'bg-emerald-500', preview: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
    { value: 'danger', label: 'Danger (Red)', color: 'bg-red-500', preview: 'bg-red-50 text-red-700 border-red-200' },
    { value: 'warning', label: 'Warning (Orange)', color: 'bg-amber-500', preview: 'bg-amber-50 text-amber-700 border-amber-200' },
    { value: 'info', label: 'Info (Blue)', color: 'bg-blue-500', preview: 'bg-blue-50 text-blue-700 border-blue-200' },
    { value: 'default', label: 'Default (Gray)', color: 'bg-slate-400', preview: 'bg-slate-50 text-slate-700 border-slate-200' },
];

function getStylePreview(style) {
    return STYLE_OPTIONS.find(s => s.value === style)?.preview || STYLE_OPTIONS[4].preview;
}

export default function OutcomesTab({ config = {}, onChange }) {
    const outcomeConfig = config?.config?.outcomeConfig || {};
    const options = outcomeConfig.options || [];
    const [showPreview, setShowPreview] = useState(false);
    const [expandedExtras, setExpandedExtras] = useState({});
    const [actionTypes, setActionTypes] = useState([]);
    const [loadingTypes, setLoadingTypes] = useState(true);

    // Fetch predefined action types from backend
    useEffect(() => {
        let cancelled = false;
        setLoadingTypes(true);
        WorkflowConfigApi.getActionTypes()
            .then(types => {
                if (!cancelled) setActionTypes(types || []);
            })
            .catch(err => {
                console.warn('Failed to load action types, using fallback:', err);
                if (!cancelled) {
                    // Fallback hardcoded types if backend is unavailable
                    setActionTypes([
                        { actionType: 'APPROVE', defaultLabel: 'Approve', description: 'Approves and moves to next step', defaultStyle: 'success', defaultSets: { decision: 'APPROVED' }, requiresComment: false, requiresTargetStep: false },
                        { actionType: 'REJECT', defaultLabel: 'Reject', description: 'Rejects the item', defaultStyle: 'danger', defaultSets: { decision: 'REJECTED' }, requiresComment: true, requiresTargetStep: false },
                        { actionType: 'SEND_BACK', defaultLabel: 'Send Back', description: 'Sends back for revision', defaultStyle: 'warning', defaultSets: { decision: 'SENT_BACK' }, requiresComment: false, requiresTargetStep: true },
                        { actionType: 'ESCALATE', defaultLabel: 'Escalate', description: 'Escalates to higher authority', defaultStyle: 'info', defaultSets: { decision: 'ESCALATED' }, requiresComment: false, requiresTargetStep: false },
                        { actionType: 'CUSTOM', defaultLabel: 'Custom Action', description: 'Custom behavior', defaultStyle: 'default', defaultSets: {}, requiresComment: false, requiresTargetStep: false },
                    ]);
                }
            })
            .finally(() => { if (!cancelled) setLoadingTypes(false); });
        return () => { cancelled = true; };
    }, []);

    const updateOutcomeConfig = useCallback((updates) => {
        const newOutcomeConfig = { ...outcomeConfig, ...updates };
        onChange?.({
            ...config,
            config: {
                ...(config.config || {}),
                outcomeConfig: newOutcomeConfig,
            },
        });
    }, [outcomeConfig, config, onChange]);

    const handleAddOption = () => {
        // Add a blank option — user must select action type
        const newOptions = [...options, { actionType: '', label: '', style: 'default', sets: {} }];
        updateOutcomeConfig({ options: newOptions });
    };

    const handleRemoveOption = (index) => {
        const newOptions = options.filter((_, i) => i !== index);
        const newExpanded = { ...expandedExtras };
        delete newExpanded[index];
        setExpandedExtras(newExpanded);
        updateOutcomeConfig({ options: newOptions });
    };

    const handleActionTypeChange = (index, actionTypeValue) => {
        const typeDef = actionTypes.find(t => t.actionType === actionTypeValue);
        if (!typeDef) return;

        const newOptions = options.map((opt, i) => {
            if (i !== index) return opt;
            return {
                actionType: typeDef.actionType,
                label: typeDef.defaultLabel,
                style: typeDef.defaultStyle,
                sets: { ...(typeDef.defaultSets || {}) },
                requiresComment: typeDef.requiresComment || false,
                requiresTargetStep: typeDef.requiresTargetStep || false,
            };
        });
        updateOutcomeConfig({ options: newOptions });
    };

    const handleOptionChange = (index, field, value) => {
        const newOptions = options.map((opt, i) =>
            i === index ? { ...opt, [field]: value } : opt
        );
        updateOutcomeConfig({ options: newOptions });
    };

    const handleToggleField = (index, field) => {
        const newOptions = options.map((opt, i) =>
            i === index ? { ...opt, [field]: !opt[field] } : opt
        );
        updateOutcomeConfig({ options: newOptions });
    };

    const handleExtraVarChange = (optionIndex, key, value, oldKey) => {
        const newOptions = options.map((opt, i) => {
            if (i !== optionIndex) return opt;
            const newSets = { ...(opt.sets || {}) };
            if (oldKey && oldKey !== key) {
                delete newSets[oldKey];
            }
            if (key) {
                newSets[key] = value;
            }
            return { ...opt, sets: newSets };
        });
        updateOutcomeConfig({ options: newOptions });
    };

    const handleRemoveExtraVar = (optionIndex, key) => {
        const newOptions = options.map((opt, i) => {
            if (i !== optionIndex) return opt;
            const newSets = { ...(opt.sets || {}) };
            delete newSets[key];
            return { ...opt, sets: newSets };
        });
        updateOutcomeConfig({ options: newOptions });
    };

    const handleAddExtraVar = (optionIndex) => {
        const newOptions = options.map((opt, i) => {
            if (i !== optionIndex) return opt;
            return { ...opt, sets: { ...(opt.sets || {}), '': '' } };
        });
        updateOutcomeConfig({ options: newOptions });
        setExpandedExtras({ ...expandedExtras, [optionIndex]: true });
    };

    const handleQuickSetup = () => {
        const approve = actionTypes.find(t => t.actionType === 'APPROVE');
        const reject = actionTypes.find(t => t.actionType === 'REJECT');
        const defaults = [];
        if (approve) defaults.push({
            actionType: approve.actionType, label: approve.defaultLabel,
            style: approve.defaultStyle, sets: { ...approve.defaultSets },
            requiresComment: approve.requiresComment, requiresTargetStep: approve.requiresTargetStep,
        });
        if (reject) defaults.push({
            actionType: reject.actionType, label: reject.defaultLabel,
            style: reject.defaultStyle, sets: { ...reject.defaultSets },
            requiresComment: reject.requiresComment, requiresTargetStep: reject.requiresTargetStep,
        });
        updateOutcomeConfig({ options: defaults });
    };

    const getExtraVars = (sets) => {
        if (!sets) return [];
        return Object.entries(sets).filter(([k]) => k !== 'decision' && k !== 'nextStage');
    };

    const hasConfig = options.length > 0;

    if (loadingTypes) {
        return (
            <div className="flex items-center justify-center p-8 text-slate-400">
                <Loader2 className="w-5 h-5 animate-spin mr-2" />
                <span className="text-sm">Loading action types...</span>
            </div>
        );
    }

    return (
        <div className="space-y-5">
            {/* Info Banner */}
            <div className="p-3 bg-gradient-to-r from-violet-50 to-purple-50 border border-violet-200 rounded-xl">
                <div className="flex items-start gap-2">
                    <Route className="w-4 h-4 text-violet-600 mt-0.5 flex-shrink-0" />
                    <div className="text-xs text-violet-800">
                        <p className="font-medium">Action Type Configuration</p>
                        <p className="mt-0.5 text-violet-600">
                            Select predefined actions for this task. Each action sets{' '}
                            <code className="bg-violet-100 px-1 rounded font-semibold">decision</code>{' '}
                            for gateway routing. Use{' '}
                            <code className="bg-violet-100 px-1 rounded">{'${decision == "APPROVED"}'}</code>{' '}
                            in gateways.
                        </p>
                    </div>
                </div>
            </div>

            {/* Quick Setup */}
            {!hasConfig && (
                <button
                    onClick={handleQuickSetup}
                    className="w-full flex items-center justify-center gap-2 p-3 border-2 border-dashed border-violet-300 rounded-xl text-violet-600 hover:bg-violet-50 hover:border-violet-400 transition-all text-sm font-medium"
                >
                    <Sparkles className="w-4 h-4" />
                    Quick Setup: Approve / Reject
                </button>
            )}

            {/* Options */}
            <div className="space-y-2">
                <div className="flex items-center justify-between">
                    <label className="text-xs font-semibold text-slate-700">Action Buttons</label>
                    <button
                        onClick={handleAddOption}
                        className="flex items-center gap-1 text-[11px] text-violet-600 hover:text-violet-700 font-medium transition-colors"
                    >
                        <Plus className="w-3 h-3" />
                        Add Action
                    </button>
                </div>

                {options.length === 0 ? (
                    <div className="p-4 text-center text-xs text-slate-400 border border-dashed border-slate-200 rounded-xl">
                        No actions defined yet. Add actions or use Quick Setup above.
                    </div>
                ) : (
                    <div className="space-y-2">
                        {options.map((option, index) => {
                            const extraVars = getExtraVars(option.sets);
                            const isExpanded = expandedExtras[index];
                            const typeDef = actionTypes.find(t => t.actionType === option.actionType);

                            return (
                                <div
                                    key={index}
                                    className="group p-3 bg-white border border-slate-200 rounded-xl hover:border-slate-300 transition-all"
                                >
                                    <div className="flex items-start gap-2">
                                        <GripVertical className="w-4 h-4 text-slate-300 mt-2 flex-shrink-0" />
                                        <div className="flex-1 space-y-2">
                                            {/* Action Type Dropdown + Label Row */}
                                            <div className="grid grid-cols-2 gap-2">
                                                <div>
                                                    <label className="text-[10px] text-slate-400 font-medium">Action Type</label>
                                                    <select
                                                        value={option.actionType || ''}
                                                        onChange={(e) => handleActionTypeChange(index, e.target.value)}
                                                        className="w-full px-2.5 py-1.5 text-xs border border-slate-200 rounded-lg focus:ring-2 focus:ring-violet-500 focus:border-violet-500 outline-none bg-slate-50 transition-all"
                                                    >
                                                        <option value="">Select type...</option>
                                                        {actionTypes.map(t => (
                                                            <option key={t.actionType} value={t.actionType}>
                                                                {t.defaultLabel} — {t.description}
                                                            </option>
                                                        ))}
                                                    </select>
                                                </div>
                                                <div>
                                                    <label className="text-[10px] text-slate-400 font-medium">Button Label</label>
                                                    <input
                                                        type="text"
                                                        value={option.label || ''}
                                                        onChange={(e) => handleOptionChange(index, 'label', e.target.value)}
                                                        placeholder={typeDef?.defaultLabel || 'Enter label'}
                                                        className="w-full px-2.5 py-1.5 text-xs border border-slate-200 rounded-lg focus:ring-2 focus:ring-violet-500 focus:border-violet-500 outline-none bg-slate-50 transition-all"
                                                    />
                                                </div>
                                            </div>

                                            {/* Decision preview + Style + Toggles */}
                                            {option.actionType && (
                                                <div className="space-y-2">
                                                    {/* Decision variable shown as read-only badge */}
                                                    {option.sets?.decision && (
                                                        <div className="flex items-center gap-1.5">
                                                            <span className="text-[10px] text-slate-400 font-medium">Sets:</span>
                                                            <code className="text-[10px] bg-slate-100 px-1.5 py-0.5 rounded font-mono text-slate-600 border border-slate-200">
                                                                decision = "{option.sets.decision}"
                                                            </code>
                                                        </div>
                                                    )}

                                                    {/* Style + Options Row */}
                                                    <div className="flex items-center justify-between flex-wrap gap-2">
                                                        <div className="flex items-center gap-2">
                                                            <label className="text-[10px] text-slate-400 font-medium whitespace-nowrap">Style:</label>
                                                            <div className="flex gap-1">
                                                                {STYLE_OPTIONS.map((style) => (
                                                                    <button
                                                                        key={style.value}
                                                                        onClick={() => handleOptionChange(index, 'style', style.value)}
                                                                        title={style.label}
                                                                        className={`w-5 h-5 rounded-full ${style.color} transition-all ${option.style === style.value
                                                                            ? 'ring-2 ring-offset-1 ring-violet-400 scale-110'
                                                                            : 'opacity-50 hover:opacity-80'
                                                                            }`}
                                                                    />
                                                                ))}
                                                            </div>
                                                        </div>

                                                        {/* Toggle options */}
                                                        <div className="flex items-center gap-3">
                                                            <label className="flex items-center gap-1 text-[10px] text-slate-500 cursor-pointer">
                                                                <input
                                                                    type="checkbox"
                                                                    checked={option.requiresComment || false}
                                                                    onChange={() => handleToggleField(index, 'requiresComment')}
                                                                    className="w-3 h-3 rounded border-slate-300 text-violet-600 focus:ring-violet-500"
                                                                />
                                                                Requires Comment
                                                            </label>

                                                            <button
                                                                onClick={() => setExpandedExtras({ ...expandedExtras, [index]: !isExpanded })}
                                                                className="flex items-center gap-0.5 text-[10px] text-slate-400 hover:text-violet-600 transition-colors"
                                                            >
                                                                <Variable className="w-3 h-3" />
                                                                +vars
                                                                {isExpanded ? <ChevronDown className="w-3 h-3" /> : <ChevronRight className="w-3 h-3" />}
                                                            </button>
                                                        </div>
                                                    </div>

                                                    {/* Extra Variables (expandable) */}
                                                    {isExpanded && (
                                                        <div className="p-2 bg-slate-50 rounded-lg border border-slate-100 space-y-1.5">
                                                            <div className="flex items-center justify-between">
                                                                <p className="text-[10px] text-slate-400 font-medium">Additional Variables</p>
                                                                <button
                                                                    onClick={() => handleAddExtraVar(index)}
                                                                    className="text-[10px] text-violet-500 hover:text-violet-700 font-medium"
                                                                >
                                                                    + Add
                                                                </button>
                                                            </div>
                                                            {extraVars.length === 0 ? (
                                                                <p className="text-[10px] text-slate-300 italic">No extra variables. Click + Add to set custom process variables.</p>
                                                            ) : (
                                                                extraVars.map(([key, val], vi) => (
                                                                    <div key={vi} className="flex items-center gap-1.5">
                                                                        <input
                                                                            type="text"
                                                                            value={key}
                                                                            onChange={(e) => handleExtraVarChange(index, e.target.value, val, key)}
                                                                            placeholder="key"
                                                                            className="flex-1 px-2 py-1 text-[11px] border border-slate-200 rounded font-mono bg-white"
                                                                        />
                                                                        <span className="text-slate-300">=</span>
                                                                        <input
                                                                            type="text"
                                                                            value={val}
                                                                            onChange={(e) => handleExtraVarChange(index, key, e.target.value)}
                                                                            placeholder="value"
                                                                            className="flex-1 px-2 py-1 text-[11px] border border-slate-200 rounded font-mono bg-white"
                                                                        />
                                                                        <button
                                                                            onClick={() => handleRemoveExtraVar(index, key)}
                                                                            className="p-0.5 text-slate-300 hover:text-red-500"
                                                                        >
                                                                            <Trash2 className="w-3 h-3" />
                                                                        </button>
                                                                    </div>
                                                                ))
                                                            )}
                                                        </div>
                                                    )}
                                                </div>
                                            )}
                                        </div>
                                        <button
                                            onClick={() => handleRemoveOption(index)}
                                            className="p-1 rounded-lg text-slate-300 hover:text-red-500 hover:bg-red-50 transition-all opacity-0 group-hover:opacity-100"
                                        >
                                            <Trash2 className="w-3.5 h-3.5" />
                                        </button>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                )}
            </div>

            {/* Live Preview */}
            {hasConfig && (
                <div className="space-y-2">
                    <button
                        onClick={() => setShowPreview(!showPreview)}
                        className="flex items-center gap-1.5 text-xs font-medium text-slate-500 hover:text-slate-700 transition-colors"
                    >
                        <Eye className="w-3.5 h-3.5" />
                        {showPreview ? 'Hide Preview' : 'Show Preview'}
                    </button>
                    {showPreview && (
                        <div className="p-4 bg-slate-50 border border-slate-200 rounded-xl space-y-3">
                            <p className="text-[10px] text-slate-400 font-medium uppercase tracking-wider">
                                User will see these action buttons:
                            </p>
                            <div className="flex flex-wrap gap-2">
                                {options.filter(o => o.label).map((option, index) => (
                                    <span
                                        key={index}
                                        className={`inline-flex items-center px-3 py-1.5 rounded-lg text-xs font-medium border ${getStylePreview(option.style)}`}
                                    >
                                        {option.label || 'Untitled'}
                                    </span>
                                ))}
                            </div>
                            <div className="pt-2 border-t border-slate-200 space-y-1">
                                {options.filter(o => o.label && o.sets?.decision).map((option, index) => (
                                    <p key={index} className="text-[10px] text-slate-500">
                                        <span className={`inline-block w-2 h-2 rounded-full mr-1.5 ${STYLE_OPTIONS.find(s => s.value === option.style)?.color || 'bg-slate-400'}`} />
                                        <strong>{option.label}</strong> → sets{' '}
                                        <code className="bg-white px-1 py-0.5 rounded border border-slate-200 font-mono">
                                            decision = "{option.sets.decision}"
                                        </code>
                                        {Object.entries(option.sets || {}).filter(([k]) => k !== 'decision').map(([k, v]) => (
                                            <span key={k}>
                                                {', '}
                                                <code className="bg-white px-1 py-0.5 rounded border border-slate-200 font-mono">
                                                    {k} = "{v}"
                                                </code>
                                            </span>
                                        ))}
                                        {option.requiresComment && (
                                            <span className="ml-1.5 text-amber-600 font-medium">(comment required)</span>
                                        )}
                                    </p>
                                ))}
                            </div>
                        </div>
                    )}
                </div>
            )}

            {/* Gateway Hint */}
            {hasConfig && (
                <div className="p-2.5 bg-blue-50 border border-blue-200 rounded-xl">
                    <p className="text-[10px] text-blue-700">
                        💡 <strong>Gateway conditions:</strong> Use{' '}
                        {options.filter(o => o.sets?.decision).map((opt, i) => (
                            <span key={i}>
                                {i > 0 && ' or '}
                                <code className="bg-blue-100 px-1 rounded font-mono">{`\${decision == "${opt.sets.decision}"}`}</code>
                            </span>
                        ))}
                        {' '}in your exclusive gateways.
                    </p>
                </div>
            )}
        </div>
    );
}
