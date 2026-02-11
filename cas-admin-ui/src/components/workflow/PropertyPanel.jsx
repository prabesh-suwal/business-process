import { useState, useEffect, useCallback } from 'react';
import { getElementMeta } from '../BpmnDesigner';
import GeneralTab from './GeneralTab';
import AssignmentTab from './AssignmentTab';
import ConditionsTab from './ConditionsTab';
import AdvancedTab from './AdvancedTab';

/**
 * Determine which tabs are available for a given BPMN element type.
 */
function getTabsForElement(element) {
    if (!element) return [];
    const type = element.type;
    const tabs = [{ id: 'general', label: 'General' }];

    // User Tasks get assignment & advanced (SLA, form, escalation)
    if (type === 'bpmn:UserTask') {
        tabs.push({ id: 'assignment', label: 'Assignment' });
        tabs.push({ id: 'advanced', label: 'Advanced' });
    }

    // Gateways get conditions tab
    if (['bpmn:ExclusiveGateway', 'bpmn:InclusiveGateway'].includes(type)) {
        tabs.push({ id: 'conditions', label: 'Conditions' });
    }

    // Parallel gateways get advanced (completion mode)
    if (type === 'bpmn:ParallelGateway') {
        tabs.push({ id: 'advanced', label: 'Advanced' });
    }

    // Sequence flows get conditions
    if (type === 'bpmn:SequenceFlow') {
        tabs.push({ id: 'conditions', label: 'Conditions' });
    }

    // Service tasks get advanced (delegate expression)
    if (type === 'bpmn:ServiceTask') {
        tabs.push({ id: 'advanced', label: 'Advanced' });
    }

    // Sub-processes get advanced
    if (type === 'bpmn:SubProcess') {
        tabs.push({ id: 'advanced', label: 'Advanced' });
    }

    return tabs;
}

export default function PropertyPanel({
    element,
    modelerRef,
    topicId,
    stepConfigs,
    gatewayRules,
    onConfigChange,
    onClose
}) {
    const [activeTab, setActiveTab] = useState('general');
    const [isDirty, setIsDirty] = useState(false);
    const [showSaveToast, setShowSaveToast] = useState(false);

    const meta = getElementMeta(element);
    const tabs = getTabsForElement(element);
    const elementName = element?.businessObject?.name || '';
    const elementId = element?.id || '';

    // Reset active tab when element changes
    useEffect(() => {
        setActiveTab('general');
        setIsDirty(false);
    }, [elementId]);

    // Keyboard: Esc to close
    useEffect(() => {
        const handleKeyDown = (e) => {
            if (e.key === 'Escape') {
                onClose();
            }
        };
        document.addEventListener('keydown', handleKeyDown);
        return () => document.removeEventListener('keydown', handleKeyDown);
    }, [onClose]);

    // Handle name change (syncs to BPMN model)
    const handleNameChange = useCallback((newName) => {
        if (!element || !modelerRef?.current) return;
        const modeling = modelerRef.current.getModeling();
        if (modeling) {
            modeling.updateProperties(element, { name: newName });
        }
    }, [element, modelerRef]);

    // Mark dirty
    const markDirty = useCallback(() => {
        setIsDirty(true);
    }, []);

    // Show saved toast
    const showSaved = useCallback(() => {
        setIsDirty(false);
        setShowSaveToast(true);
        setTimeout(() => setShowSaveToast(false), 2000);
    }, []);

    // Find the step config for the current element's task key
    const currentStepConfig = stepConfigs?.find(
        s => s.taskKey === elementId
    ) || null;

    // Find gateway rule for the current element
    const currentGatewayRule = gatewayRules?.find(
        g => g.gatewayKey === elementId
    ) || null;

    // Ensure activeTab is valid for current element
    useEffect(() => {
        const validIds = tabs.map(t => t.id);
        if (!validIds.includes(activeTab)) {
            setActiveTab(validIds[0] || 'general');
        }
    }, [tabs, activeTab]);

    // Render active tab content
    const renderTabContent = () => {
        switch (activeTab) {
            case 'general':
                return (
                    <GeneralTab
                        element={element}
                        modelerRef={modelerRef}
                        onNameChange={handleNameChange}
                    />
                );
            case 'assignment':
                return (
                    <AssignmentTab
                        element={element}
                        topicId={topicId}
                        stepConfig={currentStepConfig}
                        onConfigChange={onConfigChange}
                        onDirty={markDirty}
                        onSaved={showSaved}
                    />
                );
            case 'conditions':
                return (
                    <ConditionsTab
                        element={element}
                        modelerRef={modelerRef}
                        topicId={topicId}
                        gatewayRule={currentGatewayRule}
                        onConfigChange={onConfigChange}
                        onDirty={markDirty}
                        onSaved={showSaved}
                    />
                );
            case 'advanced':
                return (
                    <AdvancedTab
                        element={element}
                        topicId={topicId}
                        stepConfig={currentStepConfig}
                        onConfigChange={onConfigChange}
                        onDirty={markDirty}
                        onSaved={showSaved}
                    />
                );
            default:
                return null;
        }
    };

    return (
        <>
            <div
                className="property-panel"
                role="complementary"
                aria-label="Element Properties"
            >
                {/* Header */}
                <div className="property-panel__header">
                    <div className="property-panel__icon">
                        {meta.icon}
                    </div>
                    <div className="property-panel__title-group">
                        <div className="property-panel__type">{meta.label}</div>
                        <input
                            className="property-panel__name-input"
                            value={elementName}
                            onChange={(e) => handleNameChange(e.target.value)}
                            placeholder="Unnamed element"
                            aria-label="Element name"
                        />
                    </div>
                    {isDirty && <span className="dirty-indicator" title="Unsaved changes" />}
                    <button
                        className="property-panel__close"
                        onClick={onClose}
                        title="Close panel (Esc)"
                        aria-label="Close properties panel"
                    >
                        ×
                    </button>
                </div>

                {/* Tabs */}
                {tabs.length > 1 && (
                    <div className="property-panel__tabs" role="tablist">
                        {tabs.map(tab => (
                            <button
                                key={tab.id}
                                className={`property-panel__tab ${activeTab === tab.id ? 'property-panel__tab--active' : ''}`}
                                onClick={() => setActiveTab(tab.id)}
                                role="tab"
                                aria-selected={activeTab === tab.id}
                            >
                                {tab.label}
                            </button>
                        ))}
                    </div>
                )}

                {/* Body */}
                <div className="property-panel__body" role="tabpanel">
                    {renderTabContent()}
                </div>
            </div>

            {/* Save Toast */}
            {showSaveToast && (
                <div className="save-toast">
                    ✓ Saved
                </div>
            )}
        </>
    );
}
