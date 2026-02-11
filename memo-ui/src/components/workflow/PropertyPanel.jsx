import React, { useState, useMemo } from 'react';
import { X, Info, Users, GitBranch, Zap, Settings2, ArrowRight } from 'lucide-react';
import GeneralTab from './GeneralTab';
import ConditionsTab from './ConditionsTab';
import AdvancedTab from './AdvancedTab';
import AdvancedAssignmentTab from '../AdvancedAssignmentTab';
import ConditionBuilder from '../ConditionBuilder';

/**
 * PropertyPanel — Context-aware sliding panel for BPMN elements.
 *
 * Shows different tabs depending on the selected element type:
 * - UserTask: General, Assignment, Conditions, Advanced
 * - ExclusiveGateway: General, Conditions
 * - ParallelGateway/InclusiveGateway: General, Advanced
 * - SequenceFlow: General, Conditions
 * - Other: General
 */

// Determine which tabs to show for each element type
function getTabsForElement(type) {
    switch (type) {
        case 'bpmn:UserTask':
            return [
                { id: 'general', label: 'General', icon: Info },
                { id: 'assignment', label: 'Assignment', icon: Users },
                { id: 'conditions', label: 'Branching', icon: GitBranch },
                { id: 'advanced', label: 'Advanced', icon: Settings2 },
            ];
        case 'bpmn:ExclusiveGateway':
            return [
                { id: 'general', label: 'General', icon: Info },
                { id: 'conditions', label: 'Conditions', icon: GitBranch },
            ];
        case 'bpmn:ParallelGateway':
        case 'bpmn:InclusiveGateway':
            return [
                { id: 'general', label: 'General', icon: Info },
                { id: 'advanced', label: 'Advanced', icon: Zap },
            ];
        case 'bpmn:SequenceFlow':
            return [
                { id: 'general', label: 'General', icon: Info },
                { id: 'conditions', label: 'Conditions', icon: GitBranch },
            ];
        default:
            return [
                { id: 'general', label: 'General', icon: Info },
            ];
    }
}

// Header icon/color by element type
function getElementMeta(type) {
    switch (type) {
        case 'bpmn:UserTask':
            return { icon: Users, color: 'from-blue-500 to-indigo-600', shadow: 'shadow-blue-500/20', label: 'User Task' };
        case 'bpmn:ExclusiveGateway':
            return { icon: GitBranch, color: 'from-amber-500 to-orange-600', shadow: 'shadow-amber-500/20', label: 'Decision Gateway' };
        case 'bpmn:ParallelGateway':
            return { icon: Zap, color: 'from-purple-500 to-violet-600', shadow: 'shadow-purple-500/20', label: 'Parallel Gateway' };
        case 'bpmn:InclusiveGateway':
            return { icon: Zap, color: 'from-indigo-500 to-blue-600', shadow: 'shadow-indigo-500/20', label: 'Inclusive Gateway' };
        case 'bpmn:SequenceFlow':
            return { icon: ArrowRight, color: 'from-slate-500 to-slate-700', shadow: 'shadow-slate-500/20', label: 'Sequence Flow' };
        default:
            return { icon: Info, color: 'from-slate-400 to-slate-600', shadow: 'shadow-slate-400/20', label: type?.replace('bpmn:', '') || 'Element' };
    }
}

export default function PropertyPanel({
    element,               // raw BPMN element from modeler
    onClose,
    modelerRef,            // ref to BpmnDesigner modeler
    // UserTask config
    stepConfig = {},
    onStepConfigChange,
    roles = [],
    groups = [],
    departments = [],
    slaDurations = [],
    escalationActions = [],
    topicId,
    allSteps = [],
    // Gateway config
    gatewayConfig,
    onGatewayUpdate,
    // Callback to select a different element
    onSelectElement,
}) {
    const type = element?.type;
    const tabs = useMemo(() => getTabsForElement(type), [type]);
    const meta = useMemo(() => getElementMeta(type), [type]);
    const [activeTab, setActiveTab] = useState(tabs[0]?.id || 'general');

    // Reset tab when element changes
    const elementId = element?.id;
    const [prevElementId, setPrevElementId] = useState(elementId);
    if (elementId !== prevElementId) {
        setPrevElementId(elementId);
        // Default to 'general' unless it's a gateway (then conditions) or usertask (then assignment)
        if (type === 'bpmn:ExclusiveGateway') {
            setActiveTab('conditions');
        } else if (type === 'bpmn:UserTask') {
            setActiveTab('assignment');
        } else {
            setActiveTab(tabs[0]?.id || 'general');
        }
    }

    // Make sure active tab is valid for this element
    const validActiveTab = tabs.find(t => t.id === activeTab) ? activeTab : tabs[0]?.id || 'general';

    const bo = element?.businessObject;
    const elementName = bo?.name || meta.label;

    // Available target steps for UserTask branching
    const availableTargetSteps = allSteps
        ?.filter(s => s.taskKey !== element?.id)
        .map(s => ({ id: s.taskKey, name: s.taskName || s.taskKey })) || [];

    // Handler to select a flow element (when clicking a flow in the gateway conditions overview)
    const handleSelectFlow = (flowId) => {
        onSelectElement?.(flowId);
    };

    return (
        <div className="h-full flex flex-col bg-white">
            {/* Header */}
            <div className="px-5 py-4 border-b border-slate-100 bg-gradient-to-r from-slate-50 to-white flex-shrink-0">
                <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3 min-w-0">
                        <div className={`w-9 h-9 rounded-xl bg-gradient-to-br ${meta.color} flex items-center justify-center shadow-lg ${meta.shadow} flex-shrink-0`}>
                            <meta.icon className="w-4.5 h-4.5 text-white" />
                        </div>
                        <div className="min-w-0">
                            <h2 className="text-sm font-semibold text-slate-900 truncate">{elementName}</h2>
                            <p className="text-[11px] text-slate-400 font-mono truncate">{element?.id}</p>
                        </div>
                    </div>
                    <button
                        onClick={onClose}
                        className="p-1.5 rounded-lg hover:bg-slate-100 text-slate-400 hover:text-slate-600 transition-colors flex-shrink-0"
                    >
                        <X className="w-4 h-4" />
                    </button>
                </div>

                {/* Tabs */}
                {tabs.length > 1 && (
                    <div className="flex items-center gap-1 mt-3 bg-slate-100 rounded-lg p-1">
                        {tabs.map((tab) => {
                            const isActive = tab.id === validActiveTab;
                            return (
                                <button
                                    key={tab.id}
                                    onClick={() => setActiveTab(tab.id)}
                                    className={`flex-1 flex items-center justify-center gap-1.5 px-2 py-1.5 rounded-md text-[11px] font-medium transition-all ${isActive
                                            ? 'bg-white text-slate-900 shadow-sm'
                                            : 'text-slate-500 hover:text-slate-700'
                                        }`}
                                >
                                    <tab.icon className="w-3 h-3" />
                                    {tab.label}
                                </button>
                            );
                        })}
                    </div>
                )}
            </div>

            {/* Tab Content */}
            <div className="flex-1 overflow-y-auto p-5">
                {validActiveTab === 'general' && (
                    <GeneralTab element={element} modelerRef={modelerRef} />
                )}

                {validActiveTab === 'assignment' && type === 'bpmn:UserTask' && (
                    <AdvancedAssignmentTab
                        config={stepConfig}
                        onChange={onStepConfigChange}
                        roles={roles}
                        groups={groups}
                        departments={departments}
                        branches={[]}
                        regions={[]}
                        districts={[]}
                        states={[]}
                        users={[]}
                        topicId={topicId}
                        onPreview={async (rules) => {
                            console.log('Preview not implemented yet', rules);
                            return [];
                        }}
                    />
                )}

                {validActiveTab === 'conditions' && type === 'bpmn:UserTask' && (
                    <div className="space-y-4">
                        <p className="text-xs text-slate-400">
                            Define conditions to route memos to different steps based on their data.
                        </p>
                        <ConditionBuilder
                            topicId={topicId}
                            conditions={stepConfig.conditionConfig?.conditions || []}
                            defaultTarget={stepConfig.conditionConfig?.defaultTarget || ''}
                            availableSteps={availableTargetSteps}
                            onChange={(conditions, defaultTarget) => {
                                onStepConfigChange?.({ ...stepConfig, conditionConfig: { conditions, defaultTarget } });
                            }}
                            label="Route After This Step"
                        />
                        {(!stepConfig.conditionConfig?.conditions || stepConfig.conditionConfig.conditions.length === 0) && (
                            <div className="p-2.5 bg-purple-50 border border-purple-200 rounded-xl text-xs text-purple-800">
                                💡 No conditions defined. Workflow will follow the default sequence.
                            </div>
                        )}
                    </div>
                )}

                {validActiveTab === 'conditions' && (type === 'bpmn:ExclusiveGateway' || type === 'bpmn:SequenceFlow') && (
                    <ConditionsTab
                        element={element}
                        modelerRef={modelerRef}
                        onSelectFlow={handleSelectFlow}
                        topicId={topicId}
                        allSteps={allSteps}
                    />
                )}

                {validActiveTab === 'advanced' && (
                    <AdvancedTab
                        element={element}
                        config={stepConfig}
                        onConfigChange={onStepConfigChange}
                        slaDurations={slaDurations}
                        escalationActions={escalationActions}
                        gateway={element}
                        gatewayConfig={gatewayConfig}
                        onGatewayUpdate={onGatewayUpdate}
                    />
                )}
            </div>

            {/* Bottom Status */}
            <div className="px-5 py-2.5 border-t border-slate-100 bg-slate-50 flex items-center gap-2 flex-shrink-0">
                <div className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
                <span className="text-[11px] text-slate-500">Changes save when you deploy</span>
            </div>
        </div>
    );
}
