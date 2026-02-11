import React from 'react';
import { Clock, AlertTriangle, Eye, Zap, Users } from 'lucide-react';
import ViewerConfigPanel from '../ViewerConfigPanel';

/**
 * AdvancedTab — Context-aware advanced settings.
 *
 * UserTask: SLA, escalation, per-step viewers
 * ParallelGateway/InclusiveGateway: Completion mode (delegates to GatewayConfigPanel)
 */
export default function AdvancedTab({
    element,
    config = {},
    onConfigChange,
    slaDurations = [],
    escalationActions = [],
    gateway,
    gatewayConfig,
    onGatewayUpdate,
}) {
    const type = element?.type;

    const updateConfig = (key, value) => {
        onConfigChange?.({ ...config, [key]: value });
    };

    if (type === 'bpmn:UserTask') {
        return (
            <div className="space-y-5">
                {/* SLA / Time Limit */}
                <div>
                    <div className="flex items-center gap-2 mb-2">
                        <div className="p-1.5 rounded-lg bg-blue-100">
                            <Clock className="w-3.5 h-3.5 text-blue-600" />
                        </div>
                        <h4 className="text-sm font-semibold text-slate-700">Time Limit</h4>
                    </div>
                    <select
                        className="w-full p-2.5 border border-slate-200 rounded-lg text-sm bg-white focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                        value={config.duration || ''}
                        onChange={(e) => updateConfig('duration', e.target.value)}
                    >
                        <option value="">No time limit</option>
                        {slaDurations.map(d => (
                            <option key={d.id} value={d.code}>{d.label}</option>
                        ))}
                    </select>
                    <p className="text-xs text-slate-400 mt-1">Must be completed within the selected duration.</p>
                </div>

                {/* Escalation */}
                <div>
                    <div className="flex items-center gap-2 mb-2">
                        <div className="p-1.5 rounded-lg bg-amber-100">
                            <AlertTriangle className="w-3.5 h-3.5 text-amber-600" />
                        </div>
                        <h4 className="text-sm font-semibold text-slate-700">If Delayed</h4>
                    </div>
                    <select
                        className="w-full p-2.5 border border-slate-200 rounded-lg text-sm bg-white focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                        value={config.escalationAction || ''}
                        onChange={(e) => updateConfig('escalationAction', e.target.value)}
                    >
                        <option value="">Do nothing</option>
                        {escalationActions.map(a => (
                            <option key={a.id} value={a.code}>{a.label}</option>
                        ))}
                    </select>
                    <p className="text-xs text-slate-400 mt-1">Action to take when the time limit is exceeded.</p>
                </div>

                {/* Per-Step Viewers */}
                <div>
                    <div className="flex items-center gap-2 mb-2">
                        <div className="p-1.5 rounded-lg bg-indigo-100">
                            <Eye className="w-3.5 h-3.5 text-indigo-600" />
                        </div>
                        <h4 className="text-sm font-semibold text-slate-700">Step Viewers</h4>
                    </div>
                    <p className="text-xs text-slate-400 mb-3">
                        Configure who can view tasks for this step (read-only access).
                    </p>
                    <ViewerConfigPanel
                        viewers={config.viewers || []}
                        onChange={(viewers) => updateConfig('viewers', viewers)}
                        title=""
                    />
                    <div className="mt-2 p-2 bg-amber-50 border border-amber-200 rounded-lg text-xs text-amber-800">
                        ⚠️ These viewers can only see tasks for THIS step
                    </div>
                </div>
            </div>
        );
    }

    if (type === 'bpmn:ParallelGateway' || type === 'bpmn:InclusiveGateway') {
        const incoming = element.businessObject?.incoming || [];
        const isJoin = incoming.length > 1;

        if (!isJoin) {
            return (
                <div className="p-6 text-center">
                    <div className="w-14 h-14 rounded-2xl bg-purple-50 flex items-center justify-center mx-auto mb-3">
                        <Zap className="w-7 h-7 text-purple-400" />
                    </div>
                    <p className="text-sm font-medium text-slate-600">Fork Gateway</p>
                    <p className="text-xs text-slate-400 mt-1">
                        This gateway splits the flow. No advanced configuration needed.
                    </p>
                </div>
            );
        }

        const branchCount = incoming.length;
        const completionMode = gatewayConfig?.completionMode || 'ALL';
        const minRequired = gatewayConfig?.minRequired || 1;

        const modes = [
            {
                value: 'ALL',
                label: 'Wait for everyone',
                description: `All ${branchCount} branches must complete before proceeding.`,
                icon: Users,
                color: 'blue',
            },
            {
                value: 'ANY',
                label: 'First to finish',
                description: 'Proceed as soon as any one branch completes.',
                icon: Zap,
                color: 'emerald',
            },
            {
                value: 'N_OF_M',
                label: 'Minimum required',
                description: `At least N of ${branchCount} branches must complete.`,
                icon: Users,
                color: 'purple',
            },
        ];

        return (
            <div className="space-y-4">
                <div className="flex items-center gap-2 mb-1">
                    <div className="p-1.5 rounded-lg bg-purple-100">
                        <Users className="w-3.5 h-3.5 text-purple-600" />
                    </div>
                    <h4 className="text-sm font-semibold text-slate-700">Completion Mode</h4>
                </div>
                <p className="text-xs text-slate-400">
                    This gateway joins {branchCount} parallel branches. Choose when to proceed.
                </p>

                <div className="space-y-2">
                    {modes.map((m) => (
                        <button
                            key={m.value}
                            onClick={() => onGatewayUpdate?.({ ...gatewayConfig, completionMode: m.value })}
                            className={`w-full text-left p-3 rounded-xl border transition-all ${completionMode === m.value
                                    ? `border-${m.color}-300 bg-${m.color}-50 ring-2 ring-${m.color}-200`
                                    : 'border-slate-200 bg-white hover:border-slate-300'
                                }`}
                        >
                            <div className="flex items-center gap-2">
                                <m.icon className={`w-4 h-4 ${completionMode === m.value ? `text-${m.color}-600` : 'text-slate-400'}`} />
                                <span className={`text-sm font-medium ${completionMode === m.value ? 'text-slate-900' : 'text-slate-600'}`}>
                                    {m.label}
                                </span>
                            </div>
                            <p className="text-xs text-slate-400 mt-1 ml-6">{m.description}</p>
                        </button>
                    ))}
                </div>

                {completionMode === 'N_OF_M' && (
                    <div className="p-3 bg-purple-50 border border-purple-200 rounded-xl">
                        <label className="block text-xs font-semibold text-purple-800 mb-1.5">
                            Minimum required branches
                        </label>
                        <input
                            type="number"
                            min={1}
                            max={branchCount}
                            className="w-full p-2 border border-purple-200 rounded-lg text-sm bg-white focus:ring-2 focus:ring-purple-500"
                            value={minRequired}
                            onChange={(e) => onGatewayUpdate?.({ ...gatewayConfig, completionMode: 'N_OF_M', minRequired: Math.max(1, parseInt(e.target.value) || 1) })}
                        />
                    </div>
                )}
            </div>
        );
    }

    // Fallback for other types
    return (
        <div className="p-6 text-center text-slate-400 text-sm">
            <p>No advanced settings for this element type.</p>
        </div>
    );
}
