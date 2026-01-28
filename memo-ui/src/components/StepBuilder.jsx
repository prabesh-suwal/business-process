import React, { useState } from 'react';
import { Button } from './ui/button';
import SearchableMultiSelect from './ui/SearchableMultiSelect';
import {
    Plus, Trash2, GripVertical, Users, Clock, AlertTriangle, ChevronDown, ChevronUp,
    User, Building2, UserCircle, ArrowDown, Sparkles
} from 'lucide-react';

/**
 * StepBuilder - User-friendly workflow step builder for non-technical users.
 * 
 * Features:
 * - Add/remove/reorder steps via drag-and-drop
 * - Configure each step: assignee, SLA, escalation
 * - Visual flow preview with step connections
 * - Future-ready for parallel gateways, voting, triggers
 * 
 * Props:
 * - steps: Array of step objects
 * - onChange: Callback when steps change
 * - roles: Available roles for assignment
 * - departments: Available departments
 * - users: Available users
 * - slaDurations: Available SLA options
 * - escalationActions: Available escalation options
 * - readOnly: Whether the builder is read-only
 * - allowedOverrides: Which fields user can modify (if overriding admin config)
 */
const StepBuilder = ({
    steps = [],
    onChange,
    roles = [],
    departments = [],
    users = [],
    slaDurations = [],
    escalationActions = [],
    readOnly = false,
    allowedOverrides = null // null = all allowed (general/ad-hoc), object = specific permissions
}) => {
    const [expandedStep, setExpandedStep] = useState(null);
    const [draggedIndex, setDraggedIndex] = useState(null);

    // Check if a specific field can be modified
    const canModify = (field) => {
        if (readOnly) return false;
        if (allowedOverrides === null) return true; // Full access for general memos
        switch (field) {
            case 'assignment': return allowedOverrides.allowOverrideAssignments;
            case 'sla': return allowedOverrides.allowOverrideSLA;
            case 'escalation': return allowedOverrides.allowOverrideEscalation;
            default: return true;
        }
    };

    const addStep = () => {
        const newStep = {
            id: `step_${Date.now()}`,
            name: `Approval Step ${steps.length + 1}`,
            assignmentType: 'role',
            roles: [],
            departments: [],
            users: [],
            slaDuration: '',
            escalationAction: ''
        };
        onChange([...steps, newStep]);
        setExpandedStep(newStep.id);
    };

    const removeStep = (index) => {
        const newSteps = steps.filter((_, i) => i !== index);
        onChange(newSteps);
        if (expandedStep === steps[index]?.id) {
            setExpandedStep(null);
        }
    };

    const updateStep = (index, updates) => {
        const newSteps = [...steps];
        newSteps[index] = { ...newSteps[index], ...updates };
        onChange(newSteps);
    };

    const moveStep = (fromIndex, toIndex) => {
        if (toIndex < 0 || toIndex >= steps.length) return;
        const newSteps = [...steps];
        const [removed] = newSteps.splice(fromIndex, 1);
        newSteps.splice(toIndex, 0, removed);
        onChange(newSteps);
    };

    // Drag and drop handlers
    const handleDragStart = (e, index) => {
        setDraggedIndex(index);
        e.dataTransfer.effectAllowed = 'move';
    };

    const handleDragOver = (e, index) => {
        e.preventDefault();
        if (draggedIndex === null || draggedIndex === index) return;
    };

    const handleDrop = (e, index) => {
        e.preventDefault();
        if (draggedIndex === null || draggedIndex === index) return;
        moveStep(draggedIndex, index);
        setDraggedIndex(null);
    };

    const handleDragEnd = () => {
        setDraggedIndex(null);
    };

    return (
        <div className="space-y-4">
            {/* Header */}
            <div className="flex items-center justify-between">
                <div>
                    <h3 className="text-sm font-semibold text-slate-800">Approval Steps</h3>
                    <p className="text-xs text-slate-500">Define the approval workflow for this memo</p>
                </div>
                {!readOnly && (
                    <Button
                        type="button"
                        size="sm"
                        onClick={addStep}
                        className="bg-gradient-to-r from-blue-500 to-indigo-600 hover:from-blue-600 hover:to-indigo-700 text-white shadow-lg shadow-blue-500/25"
                    >
                        <Plus className="w-4 h-4 mr-1" />
                        Add Step
                    </Button>
                )}
            </div>

            {/* Steps List */}
            {steps.length === 0 ? (
                <div className="border-2 border-dashed border-slate-200 rounded-xl p-8 text-center">
                    <div className="w-16 h-16 mx-auto mb-4 rounded-2xl bg-gradient-to-br from-slate-100 to-slate-50 flex items-center justify-center shadow-inner">
                        <Sparkles className="w-8 h-8 text-slate-300" />
                    </div>
                    <p className="text-slate-600 font-medium">No approval steps defined</p>
                    <p className="text-slate-400 text-sm mt-1">Add steps to create your approval workflow</p>
                    {!readOnly && (
                        <Button
                            type="button"
                            onClick={addStep}
                            className="mt-4 bg-blue-50 text-blue-600 hover:bg-blue-100"
                        >
                            <Plus className="w-4 h-4 mr-1" />
                            Add First Step
                        </Button>
                    )}
                </div>
            ) : (
                <div className="space-y-3">
                    {steps.map((step, index) => {
                        const isExpanded = expandedStep === step.id;
                        const isDragging = draggedIndex === index;

                        return (
                            <React.Fragment key={step.id}>
                                {/* Step Card */}
                                <div
                                    className={`bg-white rounded-xl border transition-all duration-200 ${isDragging
                                        ? 'border-blue-400 shadow-lg opacity-50'
                                        : isExpanded
                                            ? 'border-blue-300 shadow-md ring-2 ring-blue-100'
                                            : 'border-slate-200 shadow-sm hover:shadow-md hover:border-slate-300'
                                        }`}
                                    draggable={!readOnly}
                                    onDragStart={(e) => handleDragStart(e, index)}
                                    onDragOver={(e) => handleDragOver(e, index)}
                                    onDrop={(e) => handleDrop(e, index)}
                                    onDragEnd={handleDragEnd}
                                >
                                    {/* Step Header */}
                                    <div
                                        className="flex items-center gap-3 p-4 cursor-pointer"
                                        onClick={() => setExpandedStep(isExpanded ? null : step.id)}
                                    >
                                        {/* Drag Handle */}
                                        {!readOnly && (
                                            <div className="cursor-grab active:cursor-grabbing text-slate-300 hover:text-slate-500">
                                                <GripVertical className="w-5 h-5" />
                                            </div>
                                        )}

                                        {/* Step Number */}
                                        <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center text-white text-sm font-bold shadow-sm">
                                            {index + 1}
                                        </div>

                                        {/* Step Info */}
                                        <div className="flex-1 min-w-0">
                                            <input
                                                type="text"
                                                value={step.name}
                                                onChange={(e) => updateStep(index, { name: e.target.value })}
                                                onClick={(e) => e.stopPropagation()}
                                                disabled={readOnly}
                                                className="font-semibold text-slate-800 bg-transparent border-0 p-0 focus:outline-none focus:ring-0 w-full truncate"
                                                placeholder="Step name"
                                            />
                                            <div className="flex items-center gap-2 mt-1 text-xs text-slate-500">
                                                {step.roles?.length > 0 && (
                                                    <span className="flex items-center gap-1">
                                                        <Users className="w-3 h-3" />
                                                        {step.roles.length} role{step.roles.length > 1 ? 's' : ''}
                                                    </span>
                                                )}
                                                {step.slaDuration && (
                                                    <span className="flex items-center gap-1">
                                                        <Clock className="w-3 h-3" />
                                                        {slaDurations.find(s => s.code === step.slaDuration)?.label || step.slaDuration}
                                                    </span>
                                                )}
                                            </div>
                                        </div>

                                        {/* Actions */}
                                        <div className="flex items-center gap-2">
                                            {!readOnly && (
                                                <button
                                                    type="button"
                                                    onClick={(e) => {
                                                        e.stopPropagation();
                                                        removeStep(index);
                                                    }}
                                                    className="p-1.5 rounded-lg text-slate-400 hover:text-red-500 hover:bg-red-50 transition-colors"
                                                >
                                                    <Trash2 className="w-4 h-4" />
                                                </button>
                                            )}
                                            <div className={`p-1 rounded transition-transform ${isExpanded ? 'rotate-180' : ''}`}>
                                                <ChevronDown className="w-5 h-5 text-slate-400" />
                                            </div>
                                        </div>
                                    </div>

                                    {/* Expanded Configuration */}
                                    {isExpanded && (
                                        <div className="border-t border-slate-100 p-4 space-y-4 bg-slate-50/50">
                                            {/* Assignment Section */}
                                            <div className="space-y-2">
                                                <label className="text-xs font-semibold text-slate-600 uppercase tracking-wider flex items-center gap-1">
                                                    <Users className="w-3.5 h-3.5" />
                                                    Who should approve?
                                                </label>
                                                {canModify('assignment') ? (
                                                    <>
                                                        {/* Assignment Type Selector */}
                                                        <div className="flex gap-2">
                                                            {[
                                                                { key: 'role', icon: UserCircle, label: 'By Role' },
                                                                { key: 'department', icon: Building2, label: 'By Dept' },
                                                                { key: 'user', icon: User, label: 'Specific User' }
                                                            ].map(({ key, icon: Icon, label }) => (
                                                                <button
                                                                    key={key}
                                                                    type="button"
                                                                    onClick={() => updateStep(index, { assignmentType: key })}
                                                                    className={`flex-1 flex items-center justify-center gap-1.5 px-3 py-2 rounded-lg text-sm font-medium transition-all ${step.assignmentType === key
                                                                        ? 'bg-blue-500 text-white shadow-sm'
                                                                        : 'bg-white border border-slate-200 text-slate-600 hover:border-blue-300'
                                                                        }`}
                                                                >
                                                                    <Icon className="w-4 h-4" />
                                                                    {label}
                                                                </button>
                                                            ))}
                                                        </div>

                                                        {/* Role/Dept/User Selector based on type */}
                                                        {step.assignmentType === 'role' && (
                                                            <SearchableMultiSelect
                                                                options={roles}
                                                                value={step.roles || []}
                                                                onChange={(selected) => updateStep(index, { roles: selected })}
                                                                valueKey="code"
                                                                labelKey="name"
                                                                placeholder="Search and select roles..."
                                                                icon={UserCircle}
                                                            />
                                                        )}
                                                        {step.assignmentType === 'department' && (
                                                            <SearchableMultiSelect
                                                                options={departments}
                                                                value={step.departments || []}
                                                                onChange={(selected) => updateStep(index, { departments: selected })}
                                                                valueKey="code"
                                                                labelKey="name"
                                                                placeholder="Search and select departments..."
                                                                icon={Building2}
                                                            />
                                                        )}
                                                        {step.assignmentType === 'user' && (
                                                            <SearchableMultiSelect
                                                                options={users}
                                                                value={step.users || []}
                                                                onChange={(selected) => updateStep(index, { users: selected })}
                                                                valueKey="username"
                                                                labelKey="displayName"
                                                                placeholder="Search and select users..."
                                                                icon={User}
                                                            />
                                                        )}
                                                    </>
                                                ) : (
                                                    <div className="text-sm text-slate-500 italic">
                                                        Assignment locked by admin
                                                    </div>
                                                )}
                                            </div>

                                            {/* SLA Section */}
                                            <div className="space-y-2">
                                                <label className="text-xs font-semibold text-slate-600 uppercase tracking-wider flex items-center gap-1">
                                                    <Clock className="w-3.5 h-3.5" />
                                                    Time Limit
                                                </label>
                                                {canModify('sla') ? (
                                                    <select
                                                        value={step.slaDuration || ''}
                                                        onChange={(e) => updateStep(index, { slaDuration: e.target.value })}
                                                        className="w-full p-2 border border-slate-200 rounded-lg text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                                                    >
                                                        <option value="">No time limit</option>
                                                        {slaDurations.map(s => (
                                                            <option key={s.code} value={s.code}>{s.label}</option>
                                                        ))}
                                                    </select>
                                                ) : (
                                                    <div className="text-sm text-slate-500 italic">
                                                        Time limit locked by admin
                                                    </div>
                                                )}
                                            </div>

                                            {/* Escalation Section */}
                                            <div className="space-y-2">
                                                <label className="text-xs font-semibold text-slate-600 uppercase tracking-wider flex items-center gap-1">
                                                    <AlertTriangle className="w-3.5 h-3.5" />
                                                    If Overdue
                                                </label>
                                                {canModify('escalation') ? (
                                                    <select
                                                        value={step.escalationAction || ''}
                                                        onChange={(e) => updateStep(index, { escalationAction: e.target.value })}
                                                        className="w-full p-2 border border-slate-200 rounded-lg text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                                                    >
                                                        <option value="">Do nothing</option>
                                                        {escalationActions.map(a => (
                                                            <option key={a.code} value={a.code}>{a.label}</option>
                                                        ))}
                                                    </select>
                                                ) : (
                                                    <div className="text-sm text-slate-500 italic">
                                                        Escalation locked by admin
                                                    </div>
                                                )}
                                            </div>
                                        </div>
                                    )}
                                </div>

                                {/* Arrow between steps */}
                                {index < steps.length - 1 && (
                                    <div className="flex justify-center py-1">
                                        <ArrowDown className="w-5 h-5 text-slate-300" />
                                    </div>
                                )}
                            </React.Fragment>
                        );
                    })}
                </div>
            )}

            {/* Add Step at Bottom */}
            {!readOnly && steps.length > 0 && (
                <div className="flex justify-center pt-2">
                    <Button
                        type="button"
                        variant="ghost"
                        onClick={addStep}
                        className="text-slate-500 hover:text-blue-600 hover:bg-blue-50"
                    >
                        <Plus className="w-4 h-4 mr-1" />
                        Add Another Step
                    </Button>
                </div>
            )}
        </div>
    );
};

export default StepBuilder;
