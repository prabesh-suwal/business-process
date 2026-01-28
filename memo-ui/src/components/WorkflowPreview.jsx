import React, { useState, useEffect } from 'react';
import BpmnDesigner from './BpmnDesigner';
import { Users, Clock, AlertTriangle, Eye, ChevronDown, ChevronRight, Layers, GitBranch, Pencil, Lock, UserCircle, Building2, User } from 'lucide-react';
import { WorkflowConfigApi } from '../lib/api';
import SearchableMultiSelect from './ui/SearchableMultiSelect';

/**
 * WorkflowPreview - Workflow diagram and configuration preview with override capability
 * 
 * Used in CreateMemo to show the default workflow configuration for a topic.
 * Shows:
 * - BPMN diagram (read-only, large)
 * - Step list with configuration summary and inline override editing
 */
const WorkflowPreview = ({
    workflowXml,
    processTemplateId,
    topicName = "Workflow",
    overridePermissions = null, // Admin-defined permissions
    stepOverrides = {}, // Current step overrides from parent
    onStepOverrideChange = null, // Callback when step override changes
    roles = [],
    departments = [],
    users = [],
    slaDurations = [],
    escalationActions = []
}) => {
    const [activeView, setActiveView] = useState('diagram'); // 'diagram' | 'steps'
    const [steps, setSteps] = useState([]);
    const [stepConfigs, setStepConfigs] = useState({});
    const [loading, setLoading] = useState(false);
    const [expandedStep, setExpandedStep] = useState(null);
    const [editingStep, setEditingStep] = useState(null);

    // Check if override is allowed
    const canOverrideAssignment = overridePermissions?.allowOverrideAssignments && onStepOverrideChange;
    const canOverrideSLA = overridePermissions?.allowOverrideSLA && onStepOverrideChange;
    const canOverrideEscalation = overridePermissions?.allowOverrideEscalation && onStepOverrideChange;

    // Extract steps from BPMN
    useEffect(() => {
        if (workflowXml) {
            const extractedSteps = extractStepsFromBpmn(workflowXml);
            setSteps(extractedSteps);
        }
    }, [workflowXml]);

    // Load step configurations if processTemplateId available
    useEffect(() => {
        if (processTemplateId && steps.length > 0) {
            loadStepConfigs();
        }
    }, [processTemplateId, steps]);

    const extractStepsFromBpmn = (xml) => {
        if (!xml) return [];
        try {
            const parser = new DOMParser();
            const doc = parser.parseFromString(xml, 'text/xml');
            const userTasks = [...doc.getElementsByTagName('bpmn:userTask'), ...doc.getElementsByTagName('userTask')];

            return userTasks.map((task, index) => ({
                taskKey: task.getAttribute('id'),
                taskName: task.getAttribute('name') || `Step ${index + 1}`,
                order: index + 1
            }));
        } catch (e) {
            console.error('Error parsing BPMN:', e);
            return [];
        }
    };

    const loadStepConfigs = async () => {
        try {
            setLoading(true);
            const configs = await WorkflowConfigApi.getTaskConfigs(processTemplateId);
            const configMap = {};
            configs.forEach(c => {
                configMap[c.taskKey] = {
                    roles: c.assignmentConfig?.roles || [],
                    departments: c.assignmentConfig?.departments || [],
                    users: c.assignmentConfig?.users || [],
                    duration: c.slaConfig?.duration,
                    escalationAction: c.escalationConfig?.escalations?.[0]?.action,
                    viewers: c.viewerConfig?.viewers || []
                };
            });
            setStepConfigs(configMap);
        } catch (e) {
            console.log('No configs found:', e.message);
        } finally {
            setLoading(false);
        }
    };

    const formatDuration = (duration) => {
        if (!duration) return null;
        const map = {
            'PT1H': '1 Hour',
            'PT4H': '4 Hours',
            'P1D': '1 Day',
            'P2D': '2 Days',
            'P3D': '3 Days',
            'P5D': '5 Days',
            'P1W': '1 Week'
        };
        return map[duration] || duration;
    };

    // Get effective config (base config + overrides)
    const getEffectiveConfig = (taskKey) => {
        const base = stepConfigs[taskKey] || {};
        const override = stepOverrides[taskKey] || {};
        return { ...base, ...override };
    };

    // Update step override
    const updateStepOverride = (taskKey, field, value) => {
        if (!onStepOverrideChange) return;
        const currentOverride = stepOverrides[taskKey] || {};
        onStepOverrideChange({
            ...stepOverrides,
            [taskKey]: { ...currentOverride, [field]: value }
        });
    };

    return (
        <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
            {/* Tab Header */}
            <div className="border-b border-slate-200 bg-slate-50 px-4 py-2 flex gap-2">
                <button
                    onClick={() => setActiveView('diagram')}
                    className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${activeView === 'diagram'
                        ? 'bg-white text-blue-600 shadow-sm border border-slate-200'
                        : 'text-slate-600 hover:bg-white/50'
                        }`}
                >
                    <GitBranch className="w-4 h-4 inline mr-2" />
                    Diagram
                </button>
                <button
                    onClick={() => setActiveView('steps')}
                    className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${activeView === 'steps'
                        ? 'bg-white text-blue-600 shadow-sm border border-slate-200'
                        : 'text-slate-600 hover:bg-white/50'
                        }`}
                >
                    <Layers className="w-4 h-4 inline mr-2" />
                    Steps ({steps.length})
                </button>
            </div>

            {/* Content */}
            {activeView === 'diagram' ? (
                <div className="h-[600px] relative">
                    <BpmnDesigner
                        initialXml={workflowXml}
                        onXmlChange={() => { }}
                        onElementClick={() => { }}
                        height="600px"
                        readOnly={true}
                    />
                </div>
            ) : (
                <div className="p-4 max-h-[600px] overflow-y-auto">
                    {steps.length === 0 ? (
                        <div className="text-center py-8 text-slate-500">
                            <Layers className="w-10 h-10 mx-auto mb-3 text-slate-300" />
                            <p>No approval steps found in workflow</p>
                        </div>
                    ) : (
                        <div className="space-y-3">
                            {steps.map((step, index) => {
                                const config = getEffectiveConfig(step.taskKey);
                                const baseConfig = stepConfigs[step.taskKey] || {};
                                const override = stepOverrides[step.taskKey] || {};
                                const isExpanded = expandedStep === step.taskKey;
                                const isEditing = editingStep === step.taskKey;
                                const hasConfig = config.roles?.length > 0 || config.departments?.length > 0 || config.users?.length > 0;
                                const hasOverride = Object.keys(override).length > 0;

                                return (
                                    <div
                                        key={step.taskKey}
                                        className={`border rounded-xl overflow-hidden transition-all ${hasOverride ? 'border-emerald-300 bg-emerald-50/30' : 'border-slate-200'}`}
                                    >
                                        <button
                                            onClick={() => setExpandedStep(isExpanded ? null : step.taskKey)}
                                            className="w-full px-4 py-4 flex items-center gap-3 text-left hover:bg-slate-50/50 transition-colors"
                                        >
                                            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center text-white text-sm font-bold shadow-sm">
                                                {index + 1}
                                            </div>
                                            <div className="flex-1">
                                                <div className="flex items-center gap-2">
                                                    <p className="font-semibold text-slate-800">{step.taskName}</p>
                                                    {hasOverride && (
                                                        <span className="px-2 py-0.5 bg-emerald-100 text-emerald-700 rounded-full text-xs font-medium">
                                                            Modified
                                                        </span>
                                                    )}
                                                </div>
                                                <div className="flex items-center gap-3 mt-1 text-xs text-slate-500">
                                                    {config.roles?.length > 0 && (
                                                        <span className="flex items-center gap-1">
                                                            <Users className="w-3 h-3" />
                                                            {config.roles.length} role{config.roles.length > 1 ? 's' : ''}
                                                        </span>
                                                    )}
                                                    {config.duration && (
                                                        <span className="flex items-center gap-1">
                                                            <Clock className="w-3 h-3" />
                                                            {formatDuration(config.duration)}
                                                        </span>
                                                    )}
                                                    {!hasConfig && <span className="text-amber-600">Not configured</span>}
                                                </div>
                                            </div>
                                            {isExpanded ? (
                                                <ChevronDown className="w-5 h-5 text-slate-400" />
                                            ) : (
                                                <ChevronRight className="w-5 h-5 text-slate-400" />
                                            )}
                                        </button>

                                        {isExpanded && (
                                            <div className="px-4 pb-4 pt-2 border-t border-slate-100 bg-slate-50/50 space-y-4">
                                                {/* Assignees Section */}
                                                <div className="space-y-2">
                                                    <div className="flex items-center justify-between">
                                                        <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider flex items-center gap-1">
                                                            <Users className="w-3 h-3" />
                                                            Assignees
                                                            {!canOverrideAssignment && <Lock className="w-3 h-3 text-slate-400" />}
                                                        </p>
                                                        {canOverrideAssignment && (
                                                            <button
                                                                onClick={(e) => { e.stopPropagation(); setEditingStep(isEditing ? null : step.taskKey); }}
                                                                className="text-xs text-blue-600 hover:text-blue-700 flex items-center gap-1"
                                                            >
                                                                <Pencil className="w-3 h-3" />
                                                                {isEditing ? 'Done' : 'Edit'}
                                                            </button>
                                                        )}
                                                    </div>

                                                    {isEditing && canOverrideAssignment ? (
                                                        <div className="space-y-3 p-3 bg-white rounded-lg border border-slate-200">
                                                            {/* Role Multi-select */}
                                                            <SearchableMultiSelect
                                                                label="Roles"
                                                                icon={UserCircle}
                                                                options={roles}
                                                                value={config.roles || []}
                                                                onChange={(selected) => updateStepOverride(step.taskKey, 'roles', selected)}
                                                                valueKey="code"
                                                                labelKey="name"
                                                                placeholder="Search roles..."
                                                            />

                                                            {/* Department Multi-select */}
                                                            <SearchableMultiSelect
                                                                label="Departments"
                                                                icon={Building2}
                                                                options={departments}
                                                                value={config.departments || []}
                                                                onChange={(selected) => updateStepOverride(step.taskKey, 'departments', selected)}
                                                                valueKey="code"
                                                                labelKey="name"
                                                                placeholder="Search departments..."
                                                            />

                                                            {/* Users Multi-select */}
                                                            <SearchableMultiSelect
                                                                label="Specific Users"
                                                                icon={User}
                                                                options={users}
                                                                value={config.users || []}
                                                                onChange={(selected) => updateStepOverride(step.taskKey, 'users', selected)}
                                                                valueKey="username"
                                                                labelKey="displayName"
                                                                placeholder={users.length > 0 ? "Search users..." : "No users available"}
                                                                disabled={users.length === 0}
                                                            />
                                                        </div>
                                                    ) : (
                                                        hasConfig ? (
                                                            <div className="flex flex-wrap gap-1">
                                                                {config.roles?.map(r => (
                                                                    <span key={r} className="px-2 py-0.5 bg-blue-100 text-blue-700 rounded text-xs">
                                                                        {r}
                                                                    </span>
                                                                ))}
                                                                {config.departments?.map(d => (
                                                                    <span key={d} className="px-2 py-0.5 bg-purple-100 text-purple-700 rounded text-xs">
                                                                        {d}
                                                                    </span>
                                                                ))}
                                                                {config.users?.map(u => (
                                                                    <span key={u} className="px-2 py-0.5 bg-emerald-100 text-emerald-700 rounded text-xs">
                                                                        {u}
                                                                    </span>
                                                                ))}
                                                            </div>
                                                        ) : (
                                                            <p className="text-sm text-slate-400 italic">No assignees configured</p>
                                                        )
                                                    )}
                                                </div>

                                                {/* SLA Section */}
                                                <div className="space-y-2">
                                                    <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider flex items-center gap-1">
                                                        <Clock className="w-3 h-3" />
                                                        Time Limit
                                                        {!canOverrideSLA && <Lock className="w-3 h-3 text-slate-400" />}
                                                    </p>
                                                    {canOverrideSLA ? (
                                                        <select
                                                            value={config.duration || ''}
                                                            onChange={(e) => updateStepOverride(step.taskKey, 'duration', e.target.value)}
                                                            className="w-full p-2 border border-slate-200 rounded-lg text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 bg-white"
                                                        >
                                                            <option value="">No time limit</option>
                                                            {slaDurations.map(s => (
                                                                <option key={s.code} value={s.code}>{s.label}</option>
                                                            ))}
                                                        </select>
                                                    ) : (
                                                        <p className="text-sm text-slate-700">
                                                            {config.duration ? formatDuration(config.duration) : <span className="text-slate-400 italic">No time limit</span>}
                                                        </p>
                                                    )}
                                                </div>

                                                {/* Escalation Section */}
                                                <div className="space-y-2">
                                                    <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider flex items-center gap-1">
                                                        <AlertTriangle className="w-3 h-3" />
                                                        If Overdue
                                                        {!canOverrideEscalation && <Lock className="w-3 h-3 text-slate-400" />}
                                                    </p>
                                                    {canOverrideEscalation ? (
                                                        <select
                                                            value={config.escalationAction || ''}
                                                            onChange={(e) => updateStepOverride(step.taskKey, 'escalationAction', e.target.value)}
                                                            className="w-full p-2 border border-slate-200 rounded-lg text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 bg-white"
                                                        >
                                                            <option value="">Do nothing</option>
                                                            {escalationActions.map(a => (
                                                                <option key={a.code} value={a.code}>{a.label}</option>
                                                            ))}
                                                        </select>
                                                    ) : (
                                                        <p className="text-sm text-slate-700">
                                                            {config.escalationAction || <span className="text-slate-400 italic">No escalation</span>}
                                                        </p>
                                                    )}
                                                </div>

                                                {/* Viewers */}
                                                {config.viewers?.length > 0 && (
                                                    <div>
                                                        <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1 flex items-center gap-1">
                                                            <Eye className="w-3 h-3" />
                                                            Step Viewers
                                                        </p>
                                                        <div className="flex flex-wrap gap-1">
                                                            {config.viewers.map((v, i) => (
                                                                <span key={i} className="px-2 py-0.5 bg-slate-100 text-slate-600 rounded text-xs">
                                                                    {v.value}
                                                                </span>
                                                            ))}
                                                        </div>
                                                    </div>
                                                )}
                                            </div>
                                        )}
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </div>
            )}
        </div>
    );
};

export default WorkflowPreview;
