import React, { useState } from 'react';
import { Plus, Trash2, GripVertical, Sparkles, Eye, Route, ChevronDown, ChevronRight, Variable } from 'lucide-react';

/**
 * OutcomesTab — Enterprise routing pattern for task outcomes.
 *
 * Each outcome option defines:
 * - label: What the user sees on the button
 * - style: Visual style (success, danger, warning, info, default)
 * - sets: A map of process variables to set when this option is chosen
 *         The standard routing variable is `nextStage`.
 *
 * BPMN gateways should always use: ${nextStage == "STAGE_NAME"}
 * This decouples UI labels from routing logic — designers rename buttons
 * freely without breaking gateway expressions.
 *
 * Example config:
 * {
 *   "outcomeConfig": {
 *     "options": [
 *       { "label": "Approve", "style": "success", "sets": { "nextStage": "CREDIT" } },
 *       { "label": "Reject",  "style": "danger",  "sets": { "nextStage": "REJECT" } },
 *       { "label": "Escalate","style": "warning", "sets": { "nextStage": "RISK", "escalated": "true" } }
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

const DEFAULT_OPTIONS = [
    { label: 'Approve', style: 'success', sets: { nextStage: 'APPROVED' } },
    { label: 'Reject', style: 'danger', sets: { nextStage: 'REJECTED' } },
];

function getStylePreview(style) {
    return STYLE_OPTIONS.find(s => s.value === style)?.preview || STYLE_OPTIONS[4].preview;
}

export default function OutcomesTab({ config = {}, onChange }) {
    const outcomeConfig = config?.config?.outcomeConfig || {};
    const options = outcomeConfig.options || [];
    const [showPreview, setShowPreview] = useState(false);
    const [expandedExtras, setExpandedExtras] = useState({});

    const updateOutcomeConfig = (updates) => {
        const newOutcomeConfig = { ...outcomeConfig, ...updates };
        onChange?.({
            ...config,
            config: {
                ...(config.config || {}),
                outcomeConfig: newOutcomeConfig,
            },
        });
    };

    const handleAddOption = () => {
        const newOptions = [...options, { label: '', style: 'default', sets: { nextStage: '' } }];
        updateOutcomeConfig({ options: newOptions });
    };

    const handleRemoveOption = (index) => {
        const newOptions = options.filter((_, i) => i !== index);
        const newExpanded = { ...expandedExtras };
        delete newExpanded[index];
        setExpandedExtras(newExpanded);
        updateOutcomeConfig({ options: newOptions });
    };

    const handleOptionChange = (index, field, value) => {
        const newOptions = options.map((opt, i) =>
            i === index ? { ...opt, [field]: value } : opt
        );
        updateOutcomeConfig({ options: newOptions });
    };

    const handleNextStageChange = (index, value) => {
        const newOptions = options.map((opt, i) =>
            i === index ? { ...opt, sets: { ...(opt.sets || {}), nextStage: value } } : opt
        );
        updateOutcomeConfig({ options: newOptions });
    };

    const handleExtraVarChange = (optionIndex, key, value, oldKey) => {
        const newOptions = options.map((opt, i) => {
            if (i !== optionIndex) return opt;
            const newSets = { ...(opt.sets || {}) };
            // If key was renamed, remove old key
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

    const handleLoadDefaults = () => {
        updateOutcomeConfig({ options: [...DEFAULT_OPTIONS.map(o => ({ ...o, sets: { ...o.sets } }))] });
    };

    const getExtraVars = (sets) => {
        if (!sets) return [];
        return Object.entries(sets).filter(([k]) => k !== 'nextStage');
    };

    const hasConfig = options.length > 0;

    return (
        <div className="space-y-5">
            {/* Info Banner */}
            <div className="p-3 bg-gradient-to-r from-violet-50 to-purple-50 border border-violet-200 rounded-xl">
                <div className="flex items-start gap-2">
                    <Route className="w-4 h-4 text-violet-600 mt-0.5 flex-shrink-0" />
                    <div className="text-xs text-violet-800">
                        <p className="font-medium">Enterprise Routing Pattern</p>
                        <p className="mt-0.5 text-violet-600">
                            Each outcome sets <code className="bg-violet-100 px-1 rounded font-semibold">nextStage</code> for gateway routing.
                            Gateways use: <code className="bg-violet-100 px-1 rounded">{'${nextStage == "STAGE_NAME"}'}</code>
                        </p>
                    </div>
                </div>
            </div>

            {/* Quick Setup */}
            {!hasConfig && (
                <button
                    onClick={handleLoadDefaults}
                    className="w-full flex items-center justify-center gap-2 p-3 border-2 border-dashed border-violet-300 rounded-xl text-violet-600 hover:bg-violet-50 hover:border-violet-400 transition-all text-sm font-medium"
                >
                    <Sparkles className="w-4 h-4" />
                    Quick Setup: Approve / Reject
                </button>
            )}

            {/* Options */}
            <div className="space-y-2">
                <div className="flex items-center justify-between">
                    <label className="text-xs font-semibold text-slate-700">Outcome Options</label>
                    <button
                        onClick={handleAddOption}
                        className="flex items-center gap-1 text-[11px] text-violet-600 hover:text-violet-700 font-medium transition-colors"
                    >
                        <Plus className="w-3 h-3" />
                        Add Option
                    </button>
                </div>

                {options.length === 0 ? (
                    <div className="p-4 text-center text-xs text-slate-400 border border-dashed border-slate-200 rounded-xl">
                        No options defined yet. Add options or use Quick Setup above.
                    </div>
                ) : (
                    <div className="space-y-2">
                        {options.map((option, index) => {
                            const extraVars = getExtraVars(option.sets);
                            const isExpanded = expandedExtras[index];

                            return (
                                <div
                                    key={index}
                                    className="group p-3 bg-white border border-slate-200 rounded-xl hover:border-slate-300 transition-all"
                                >
                                    <div className="flex items-start gap-2">
                                        <GripVertical className="w-4 h-4 text-slate-300 mt-2 flex-shrink-0" />
                                        <div className="flex-1 space-y-2">
                                            {/* Label and nextStage Row */}
                                            <div className="grid grid-cols-2 gap-2">
                                                <div>
                                                    <label className="text-[10px] text-slate-400 font-medium">Button Label</label>
                                                    <input
                                                        type="text"
                                                        value={option.label || ''}
                                                        onChange={(e) => handleOptionChange(index, 'label', e.target.value)}
                                                        placeholder="Approve"
                                                        className="w-full px-2.5 py-1.5 text-xs border border-slate-200 rounded-lg focus:ring-2 focus:ring-violet-500 focus:border-violet-500 outline-none bg-slate-50 transition-all"
                                                    />
                                                </div>
                                                <div>
                                                    <label className="text-[10px] text-slate-400 font-medium flex items-center gap-1">
                                                        <Route className="w-2.5 h-2.5" />
                                                        nextStage →
                                                    </label>
                                                    <input
                                                        type="text"
                                                        value={option.sets?.nextStage || ''}
                                                        onChange={(e) => handleNextStageChange(index, e.target.value)}
                                                        placeholder="APPROVED"
                                                        className="w-full px-2.5 py-1.5 text-xs border border-slate-200 rounded-lg focus:ring-2 focus:ring-violet-500 focus:border-violet-500 outline-none bg-slate-50 transition-all font-mono"
                                                    />
                                                </div>
                                            </div>

                                            {/* Style + Extra Vars Toggle Row */}
                                            <div className="flex items-center justify-between">
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

                                                <button
                                                    onClick={() => setExpandedExtras({ ...expandedExtras, [index]: !isExpanded })}
                                                    className="flex items-center gap-0.5 text-[10px] text-slate-400 hover:text-violet-600 transition-colors"
                                                >
                                                    <Variable className="w-3 h-3" />
                                                    +vars
                                                    {isExpanded ? <ChevronDown className="w-3 h-3" /> : <ChevronRight className="w-3 h-3" />}
                                                </button>
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
                                                        <p className="text-[10px] text-slate-300 italic">No extra variables.</p>
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
                                {options.map((option, index) => (
                                    <span
                                        key={index}
                                        className={`inline-flex items-center px-3 py-1.5 rounded-lg text-xs font-medium border ${getStylePreview(option.style)}`}
                                    >
                                        {option.label || 'Untitled'}
                                    </span>
                                ))}
                            </div>
                            <div className="pt-2 border-t border-slate-200 space-y-1">
                                {options.filter(o => o.label && o.sets?.nextStage).map((option, index) => (
                                    <p key={index} className="text-[10px] text-slate-500">
                                        <span className={`inline-block w-2 h-2 rounded-full mr-1.5 ${STYLE_OPTIONS.find(s => s.value === option.style)?.color || 'bg-slate-400'}`} />
                                        <strong>{option.label}</strong> → sets{' '}
                                        <code className="bg-white px-1 py-0.5 rounded border border-slate-200 font-mono">
                                            nextStage = "{option.sets.nextStage}"
                                        </code>
                                        {Object.entries(option.sets || {}).filter(([k]) => k !== 'nextStage').map(([k, v]) => (
                                            <span key={k}>
                                                {', '}
                                                <code className="bg-white px-1 py-0.5 rounded border border-slate-200 font-mono">
                                                    {k} = "{v}"
                                                </code>
                                            </span>
                                        ))}
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
                        {options.filter(o => o.sets?.nextStage).map((opt, i) => (
                            <span key={i}>
                                {i > 0 && ' or '}
                                <code className="bg-blue-100 px-1 rounded font-mono">{`\${nextStage == "${opt.sets.nextStage}"}`}</code>
                            </span>
                        ))}
                        {' '}in your exclusive gateways.
                    </p>
                </div>
            )}
        </div>
    );
}
