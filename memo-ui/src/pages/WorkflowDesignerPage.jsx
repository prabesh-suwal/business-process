import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { MemoApi, CasAdminApi, WorkflowConfigApi } from '../lib/api';
import BpmnDesigner from '../components/BpmnDesigner';
import { Card, CardHeader, CardTitle, CardContent, CardDescription } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';
import { Badge } from '../components/ui/badge';
import { toast } from 'sonner';
import {
    ArrowLeft, Users, Clock, AlertTriangle, CheckCircle,
    FileText, Save, Eye, Workflow, Settings, ChevronRight,
    Layers, MousePointer2, Rocket
} from 'lucide-react';
import ViewerConfigPanel from '../components/ViewerConfigPanel';
import AssignmentConfigPanel from '../components/AssignmentConfigPanel';

/**
 * WorkflowDesignerPage - Unified workflow design experience.
 * BPMN designer + step configuration in one seamless interface.
 * 
 * Flow:
 * 1. User designs/views BPMN on left side
 * 2. When clicking a User Task, right panel shows step configuration
 * 3. Everything saves together - no back and forth
 */
const WorkflowDesignerPage = () => {
    const { topicId } = useParams();
    const navigate = useNavigate();

    const [topic, setTopic] = useState(null);
    const [bpmnXml, setBpmnXml] = useState(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [deploying, setDeploying] = useState(false);

    // Selected step from BPMN
    const [selectedStep, setSelectedStep] = useState(null);
    const [allSteps, setAllSteps] = useState([]);
    const [stepConfigs, setStepConfigs] = useState({});

    // Viewer configuration
    const [memoWideViewers, setMemoWideViewers] = useState([]);

    // Dropdown data
    const [assignmentTypes, setAssignmentTypes] = useState([]);
    const [roles, setRoles] = useState([]);
    const [groups, setGroups] = useState([]);
    const [departments, setDepartments] = useState([]);
    const [scopes, setScopes] = useState([]);
    const [slaDurations, setSlaDurations] = useState([]);
    const [escalationActions, setEscalationActions] = useState([]);

    useEffect(() => {
        loadData();
    }, [topicId]);

    const loadData = async () => {
        try {
            setLoading(true);

            // Load topic with workflow
            const topicData = await MemoApi.getTopic(topicId);
            setTopic(topicData);
            setBpmnXml(topicData.workflowXml || null);

            // Load memo-wide viewers
            if (topicData.viewerConfig && topicData.viewerConfig.viewers) {
                setMemoWideViewers(topicData.viewerConfig.viewers);
            }

            // Extract steps from BPMN
            if (topicData.workflowXml) {
                const steps = extractStepsFromBpmn(topicData.workflowXml);
                setAllSteps(steps);
            }

            // Load saved step configurations
            try {
                const savedConfigs = await WorkflowConfigApi.getStepConfigs(topicId);
                const configMap = {};
                savedConfigs.forEach(c => {
                    configMap[c.taskKey] = {
                        // New multi-select format
                        roles: c.assignmentConfig?.roles || [],
                        departments: c.assignmentConfig?.departments || [],
                        users: c.assignmentConfig?.users || [],
                        // Legacy single-select format
                        assignmentType: c.assignmentConfig?.type,
                        role: c.assignmentConfig?.role,
                        scope: c.assignmentConfig?.scope,
                        group: c.assignmentConfig?.groupCode,
                        duration: c.slaConfig?.duration,
                        escalationAction: c.escalationConfig?.escalations?.[0]?.action,
                        viewers: c.viewerConfig?.viewers || []
                    };
                });
                setStepConfigs(configMap);
            } catch (e) {
                console.log('No saved configs found');
            }

            // Load dropdown data with fallbacks
            loadDropdownData();

        } catch (error) {
            console.error('Error loading:', error);
            toast.error('Failed to load workflow');
        } finally {
            setLoading(false);
        }
    };

    const loadDropdownData = async () => {
        try {
            const [rolesData, groupsData, deptsData, scopesData, typesData, slaData, escalationData] = await Promise.all([
                CasAdminApi.getRoles().catch(() => []),
                CasAdminApi.getGroups().catch(() => []),
                CasAdminApi.getDepartments().catch(() => []),
                CasAdminApi.getScopes().catch(() => getDefaultScopes()),
                CasAdminApi.getAssignmentTypes().catch(() => getDefaultAssignmentTypes()),
                CasAdminApi.getSlaDurations().catch(() => getDefaultSlaDurations()),
                CasAdminApi.getEscalationActions().catch(() => getDefaultEscalationActions())
            ]);

            setRoles(rolesData);
            setGroups(groupsData);
            setDepartments(deptsData);
            setScopes(scopesData);
            setAssignmentTypes(typesData);
            setSlaDurations(slaData);
            setEscalationActions(escalationData);
        } catch (e) {
            setAssignmentTypes(getDefaultAssignmentTypes());
            setScopes(getDefaultScopes());
            setSlaDurations(getDefaultSlaDurations());
            setEscalationActions(getDefaultEscalationActions());
        }
    };

    const extractStepsFromBpmn = (xml) => {
        if (!xml) return [];
        try {
            const parser = new DOMParser();
            const doc = parser.parseFromString(xml, 'text/xml');

            // Collect all task types: userTask, task, serviceTask, sendTask, etc.
            const userTasks = [...doc.getElementsByTagName('bpmn:userTask'), ...doc.getElementsByTagName('userTask')];
            const genericTasks = [...doc.getElementsByTagName('bpmn:task'), ...doc.getElementsByTagName('task')];
            const serviceTasks = [...doc.getElementsByTagName('bpmn:serviceTask'), ...doc.getElementsByTagName('serviceTask')];
            const sendTasks = [...doc.getElementsByTagName('bpmn:sendTask'), ...doc.getElementsByTagName('sendTask')];
            const manualTasks = [...doc.getElementsByTagName('bpmn:manualTask'), ...doc.getElementsByTagName('manualTask')];

            // Combine all - user tasks first, then others
            const allTasks = [...userTasks, ...genericTasks, ...serviceTasks, ...sendTasks, ...manualTasks];

            // Dedupe by ID (in case same element matched multiple times)
            const seenIds = new Set();
            const uniqueTasks = allTasks.filter(t => {
                const id = t.getAttribute('id');
                if (seenIds.has(id)) return false;
                seenIds.add(id);
                return true;
            });

            return uniqueTasks.map((t, i) => ({
                taskKey: t.getAttribute('id'),
                taskName: t.getAttribute('name') || t.getAttribute('id'),
                stepOrder: i + 1,
                taskType: t.tagName.replace('bpmn:', '')
            }));
        } catch (e) {
            console.error('Error parsing BPMN:', e);
            return [];
        }
    };

    // When BPMN changes, re-extract steps
    const handleBpmnChange = (newXml) => {
        setBpmnXml(newXml);
        const steps = extractStepsFromBpmn(newXml);
        setAllSteps(steps);
    };

    // When user clicks a task in BPMN viewer
    const handleElementClick = (element) => {
        if (element && element.type === 'bpmn:UserTask') {
            const taskKey = element.id;
            const step = allSteps.find(s => s.taskKey === taskKey) || {
                taskKey,
                taskName: element.businessObject?.name || taskKey,
                stepOrder: allSteps.length + 1
            };
            setSelectedStep(step);
        }
    };

    // Update step configuration
    const updateStepConfig = (taskKey, config) => {
        setStepConfigs(prev => ({
            ...prev,
            [taskKey]: config
        }));
    };

    // Save everything
    const handleSaveAll = async () => {
        try {
            setSaving(true);

            // Save BPMN XML
            await MemoApi.updateTopicWorkflow(topicId, bpmnXml);

            // Save step configs
            const savePromises = Object.keys(stepConfigs).map(taskKey => {
                const uiConfig = stepConfigs[taskKey];

                // Build assignment config with new multi-select format
                const assignmentConfig = {
                    roles: uiConfig.roles || [],
                    departments: uiConfig.departments || [],
                    users: uiConfig.users || []
                };

                // Add legacy fields if they exist (backward compatibility)
                if (uiConfig.assignmentType) assignmentConfig.type = uiConfig.assignmentType;
                if (uiConfig.role) assignmentConfig.role = uiConfig.role;
                if (uiConfig.scope) assignmentConfig.scope = uiConfig.scope;
                if (uiConfig.group) assignmentConfig.groupCode = uiConfig.group;
                if (uiConfig.department) assignmentConfig.department = uiConfig.department;
                if (uiConfig.groupApproval) assignmentConfig.groupApproval = uiConfig.groupApproval;

                const apiConfig = {
                    taskName: allSteps.find(s => s.taskKey === taskKey)?.taskName || taskKey,
                    assignmentConfig,
                    slaConfig: uiConfig.duration ? { duration: uiConfig.duration } : {},
                    escalationConfig: uiConfig.escalationAction ? {
                        escalations: [{
                            level: 1,
                            after: uiConfig.duration,
                            action: uiConfig.escalationAction
                        }]
                    } : {},
                    viewerConfig: uiConfig.viewers && uiConfig.viewers.length > 0 ? { viewers: uiConfig.viewers } : {}
                };
                return WorkflowConfigApi.saveStepConfig(topicId, taskKey, apiConfig);
            });

            await Promise.all(savePromises);

            // Save memo-wide viewers
            if (memoWideViewers && memoWideViewers.length > 0) {
                await MemoApi.updateTopicViewers(topicId, { viewers: memoWideViewers });
            }

            toast.success('Workflow saved successfully!');
        } catch (error) {
            console.error('Save error:', error);
            toast.error('Failed to save workflow');
        } finally {
            setSaving(false);
        }
    };

    // Save workflow AND deploy to Flowable engine
    const handleSaveAndDeploy = async () => {
        try {
            setDeploying(true);

            console.log('💾 Starting save & deploy...');
            console.log('📋 stepConfigs:', JSON.stringify(stepConfigs, null, 2));
            console.log('🔢 Number of steps to save:', Object.keys(stepConfigs).length);

            // First save everything (BPMN + configs)
            await MemoApi.updateTopicWorkflow(topicId, bpmnXml);

            // Save step configs
            const savePromises = Object.keys(stepConfigs).map(taskKey => {
                const uiConfig = stepConfigs[taskKey];

                // Build assignment config with new multi-select format
                const assignmentConfig = {
                    roles: uiConfig.roles || [],
                    departments: uiConfig.departments || [],
                    users: uiConfig.users || []
                };

                // Add legacy fields if they exist (backward compatibility)
                if (uiConfig.assignmentType) assignmentConfig.type = uiConfig.assignmentType;
                if (uiConfig.role) assignmentConfig.role = uiConfig.role;
                if (uiConfig.scope) assignmentConfig.scope = uiConfig.scope;
                if (uiConfig.group) assignmentConfig.groupCode = uiConfig.group;
                if (uiConfig.department) assignmentConfig.department = uiConfig.department;
                if (uiConfig.groupApproval) assignmentConfig.groupApproval = uiConfig.groupApproval;

                const apiConfig = {
                    taskName: allSteps.find(s => s.taskKey === taskKey)?.taskName || taskKey,
                    assignmentConfig,
                    slaConfig: uiConfig.duration ? { duration: uiConfig.duration } : {},
                    escalationConfig: uiConfig.escalationAction ? {
                        escalations: [{
                            level: 1,
                            after: uiConfig.duration,
                            action: uiConfig.escalationAction
                        }]
                    } : {},
                    viewerConfig: uiConfig.viewers && uiConfig.viewers.length > 0 ? { viewers: uiConfig.viewers } : {}
                };

                console.log('Saving step config for', taskKey, ':', JSON.stringify(apiConfig, null, 2));

                return WorkflowConfigApi.saveStepConfig(topicId, taskKey, apiConfig);
            });
            await Promise.all(savePromises);

            // Save memo-wide viewers
            if (memoWideViewers && memoWideViewers.length > 0) {
                await MemoApi.updateTopicViewers(topicId, { viewers: memoWideViewers });
                console.log('Memo-wide viewers saved');
            }

            // Now deploy the workflow to Flowable
            const deployResult = await MemoApi.deployTopicWorkflow(topicId);

            // Refresh topic data to get the new workflowTemplateId
            const updatedTopic = await MemoApi.getTopic(topicId);
            setTopic(updatedTopic);

            toast.success('Workflow deployed successfully! Ready to use.');
        } catch (error) {
            console.error('Deploy error:', error);
            toast.error('Failed to deploy workflow: ' + (error.response?.data?.message || error.message));
        } finally {
            setDeploying(false);
        }
    };

    // Defaults
    const getDefaultAssignmentTypes = () => [
        { id: 'INITIATOR', code: 'INITIATOR', label: 'Initiator (who created the memo)' },
        { id: 'ROLE', code: 'ROLE', label: 'Anyone with a specific role' },
        { id: 'MANAGER', code: 'MANAGER', label: 'Reporting manager' },
        { id: 'GROUP', code: 'GROUP', label: 'Committee / Group' },
    ];
    const getDefaultScopes = () => [
        { id: 'INITIATOR_BRANCH', code: 'INITIATOR_BRANCH', label: 'Same branch as initiator' },
        { id: 'REGION', code: 'REGION', label: 'Same region' },
        { id: 'HEAD_OFFICE', code: 'HEAD_OFFICE', label: 'Head office' },
    ];
    const getDefaultSlaDurations = () => [
        { id: 'PT4H', code: 'PT4H', label: '4 hours' },
        { id: 'P1D', code: 'P1D', label: '1 day' },
        { id: 'P2D', code: 'P2D', label: '2 days' },
        { id: 'P3D', code: 'P3D', label: '3 days' },
    ];
    const getDefaultEscalationActions = () => [
        { id: 'NOTIFY', code: 'NOTIFY', label: 'Send reminder' },
        { id: 'ESCALATE_TO_MANAGER', code: 'ESCALATE_TO_MANAGER', label: 'Escalate to manager' },
    ];

    if (loading) {
        return (
            <div className="flex items-center justify-center h-screen bg-gray-50">
                <div className="text-center">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
                    <p className="mt-4 text-gray-600">Loading workflow designer...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="h-screen flex flex-col bg-gray-100">
            {/* Top Header Bar */}
            <div className="bg-white border-b px-6 py-3 flex items-center justify-between shadow-sm">
                <div className="flex items-center space-x-4">
                    <Button variant="ghost" size="sm" onClick={() => navigate('/settings')}>
                        <ArrowLeft className="w-4 h-4 mr-2" />
                        Back to Settings
                    </Button>
                    <div className="h-6 w-px bg-gray-300" />
                    <div>
                        <h1 className="text-lg font-semibold text-gray-900 flex items-center">
                            <Workflow className="w-5 h-5 mr-2 text-blue-600" />
                            {topic?.name || 'Workflow Designer'}
                        </h1>
                        <p className="text-xs text-gray-500">Design the approval flow and configure each step</p>
                    </div>
                </div>
                <div className="flex items-center space-x-3">
                    {/* Deployment Status Badge */}
                    {topic?.workflowTemplateId ? (
                        <Badge variant="outline" className="bg-green-50 text-green-700 border-green-200">
                            <CheckCircle className="w-3 h-3 mr-1" />
                            Deployed
                        </Badge>
                    ) : (
                        <Badge variant="outline" className="bg-amber-50 text-amber-700 border-amber-200">
                            <AlertTriangle className="w-3 h-3 mr-1" />
                            Not Deployed
                        </Badge>
                    )}
                    <Badge variant="outline" className="bg-gray-50 text-gray-700 border-gray-200">
                        {allSteps.length} steps
                    </Badge>

                    {/* Save Draft Button */}
                    <Button
                        variant="outline"
                        onClick={handleSaveAll}
                        disabled={saving || deploying}
                    >
                        {saving ? (
                            <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-gray-600 mr-2" />
                        ) : (
                            <Save className="w-4 h-4 mr-2" />
                        )}
                        Save Draft
                    </Button>

                    {/* Save & Deploy Button */}
                    <Button
                        onClick={handleSaveAndDeploy}
                        disabled={saving || deploying}
                        className="bg-green-600 hover:bg-green-700"
                    >
                        {deploying ? (
                            <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2" />
                        ) : (
                            <Rocket className="w-4 h-4 mr-2" />
                        )}
                        Save & Deploy
                    </Button>
                </div>
            </div>

            {/* Main Content - Split View */}
            <div className="flex-1 flex overflow-hidden">
                {/* Left: BPMN Designer */}
                <div className="flex-1 flex flex-col border-r bg-white">
                    <div className="px-4 py-2 border-b bg-gray-50 flex items-center justify-between">
                        <div className="flex items-center text-sm text-gray-600">
                            <Layers className="w-4 h-4 mr-2" />
                            <span className="font-medium">Workflow Diagram</span>
                        </div>
                        <div className="text-xs text-gray-500 flex items-center">
                            <MousePointer2 className="w-3 h-3 mr-1" />
                            Click on a step to configure it
                        </div>
                    </div>
                    <div className="flex-1">
                        <BpmnDesigner
                            initialXml={bpmnXml}
                            onXmlChange={handleBpmnChange}
                            onElementClick={handleElementClick}
                            height="100%"
                        />
                    </div>
                </div>

                {/* Right: Step List + Configuration Panel */}
                <div className="w-[420px] flex flex-col bg-gray-50 border-l">
                    {/* Memo-Wide Viewers Card */}
                    <div className="p-4 bg-white border-b">
                        <div className="flex items-center gap-2 mb-3">
                            <Eye className="h-5 w-5 text-blue-600" />
                            <div>
                                <h3 className="text-sm font-semibold text-gray-900">Memo-Wide Viewers</h3>
                                <p className="text-xs text-gray-500">Who can view ALL memos (read-only)</p>
                            </div>
                        </div>
                        <ViewerConfigPanel
                            viewers={memoWideViewers}
                            onChange={setMemoWideViewers}
                            title=""
                        />
                    </div>

                    {/* Step List - Always visible at top */}
                    <div className="border-b bg-white">
                        <div className="px-4 py-2 border-b bg-gradient-to-r from-blue-600 to-blue-700">
                            <h2 className="text-sm font-semibold text-white flex items-center">
                                <Layers className="w-4 h-4 mr-2" />
                                Workflow Steps ({allSteps.length})
                            </h2>
                        </div>
                        <div className="max-h-48 overflow-y-auto">
                            {allSteps.length === 0 ? (
                                <div className="p-4 text-center text-gray-500 text-sm">
                                    <p>No steps yet. Add User Tasks to the diagram.</p>
                                </div>
                            ) : (
                                <div className="divide-y">
                                    {allSteps.map((step, i) => (
                                        <button
                                            key={step.taskKey}
                                            className={`w-full p-3 text-left flex items-center hover:bg-blue-50 transition-colors ${selectedStep?.taskKey === step.taskKey
                                                ? 'bg-blue-50 border-l-4 border-blue-600'
                                                : ''
                                                }`}
                                            onClick={() => setSelectedStep(step)}
                                        >
                                            <div className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-medium mr-3 ${selectedStep?.taskKey === step.taskKey
                                                ? 'bg-blue-600 text-white'
                                                : 'bg-gray-200 text-gray-600'
                                                }`}>
                                                {i + 1}
                                            </div>
                                            <div className="flex-1 min-w-0">
                                                <div className="font-medium text-gray-900 truncate">{step.taskName}</div>
                                            </div>
                                            {stepConfigs[step.taskKey]?.assignmentType && (
                                                <CheckCircle className="w-4 h-4 text-green-500 ml-2" />
                                            )}
                                        </button>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>

                    {/* Configuration Panel - Below the list */}
                    <div className="flex-1 overflow-y-auto">
                        {selectedStep ? (
                            <StepConfigurationPanel
                                step={selectedStep}
                                config={stepConfigs[selectedStep.taskKey] || {}}
                                onConfigChange={(config) => updateStepConfig(selectedStep.taskKey, config)}
                                assignmentTypes={assignmentTypes}
                                roles={roles}
                                groups={groups}
                                departments={departments}
                                scopes={scopes}
                                slaDurations={slaDurations}
                                escalationActions={escalationActions}
                            />
                        ) : (
                            <div className="h-full flex items-center justify-center p-6 text-center">
                                <div>
                                    <MousePointer2 className="w-12 h-12 mx-auto text-gray-300 mb-3" />
                                    <p className="text-gray-500">Select a step above to configure it</p>
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

/**
 * Prompt shown when no step is selected
 */
const StepSelectionPrompt = ({ steps, onSelectStep }) => (
    <div className="h-full flex flex-col">
        <div className="px-4 py-3 border-b bg-white">
            <h2 className="text-lg font-semibold text-gray-900 flex items-center">
                <Settings className="w-5 h-5 mr-2 text-gray-500" />
                Step Configuration
            </h2>
        </div>
        <div className="flex-1 p-6">
            {steps.length === 0 ? (
                <div className="h-full flex items-center justify-center text-center">
                    <div>
                        <Workflow className="w-16 h-16 mx-auto text-gray-300 mb-4" />
                        <h3 className="text-lg font-medium text-gray-600">Start Designing</h3>
                        <p className="text-gray-500 mt-2 max-w-xs">
                            Add User Task elements to your workflow diagram.
                            Each task becomes a step that you can configure.
                        </p>
                    </div>
                </div>
            ) : (
                <div className="space-y-3">
                    <p className="text-sm text-gray-600 mb-4">
                        Click a step in the diagram or select from the list below:
                    </p>
                    {steps.map((step, i) => (
                        <div
                            key={step.taskKey}
                            className="p-3 bg-white rounded-lg border hover:border-blue-300 hover:shadow cursor-pointer transition-all flex items-center"
                            onClick={() => onSelectStep(step)}
                        >
                            <div className="w-8 h-8 rounded-full bg-blue-100 text-blue-700 flex items-center justify-center text-sm font-medium mr-3">
                                {i + 1}
                            </div>
                            <div className="flex-1">
                                <div className="font-medium text-gray-900">{step.taskName}</div>
                                <div className="text-xs text-gray-500">{step.taskKey}</div>
                            </div>
                            <ChevronRight className="w-5 h-5 text-gray-400" />
                        </div>
                    ))}
                </div>
            )}
        </div>
    </div>
);

/**
 * Step Configuration Panel - Simplified accordion style
 */
const StepConfigurationPanel = ({
    step,
    config,
    onConfigChange,
    assignmentTypes,
    roles,
    groups,
    departments,
    scopes,
    slaDurations,
    escalationActions
}) => {
    const [expandedSection, setExpandedSection] = useState('assignment');

    const updateConfig = (key, value) => {
        onConfigChange({ ...config, [key]: value });
    };

    return (
        <div className="h-full flex flex-col">
            {/* Header */}
            <div className="px-4 py-3 border-b bg-white">
                <h2 className="text-lg font-semibold text-gray-900">{step.taskName}</h2>
                <p className="text-xs text-gray-500">Configure how this step behaves</p>
            </div>

            {/* Config Sections - Accordion Style */}
            <div className="flex-1 overflow-y-auto">
                {/* Assignment Section */}
                <ConfigSection
                    title="Who handles this?"
                    icon={<Users className="w-4 h-4" />}
                    isExpanded={expandedSection === 'assignment'}
                    onToggle={() => setExpandedSection(expandedSection === 'assignment' ? null : 'assignment')}
                    hasConfig={config.roles?.length > 0 || config.departments?.length > 0 || config.users?.length > 0}
                >
                    <AssignmentConfigPanel
                        config={config}
                        onChange={onConfigChange}
                        title=""
                    />
                </ConfigSection>

                {/* Time Limit Section */}
                <ConfigSection
                    title="Time limit"
                    icon={<Clock className="w-4 h-4" />}
                    isExpanded={expandedSection === 'sla'}
                    onToggle={() => setExpandedSection(expandedSection === 'sla' ? null : 'sla')}
                    hasConfig={!!config.duration}
                >
                    <div className="space-y-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                Must be completed within
                            </label>
                            <select
                                className="w-full p-2 border rounded-lg text-sm"
                                value={config.duration || ''}
                                onChange={(e) => updateConfig('duration', e.target.value)}
                            >
                                <option value="">No time limit</option>
                                {slaDurations.map(d => (
                                    <option key={d.id} value={d.code}>{d.label}</option>
                                ))}
                            </select>
                        </div>
                    </div>
                </ConfigSection>

                {/* Escalation Section */}
                <ConfigSection
                    title="If delayed"
                    icon={<AlertTriangle className="w-4 h-4" />}
                    isExpanded={expandedSection === 'escalation'}
                    onToggle={() => setExpandedSection(expandedSection === 'escalation' ? null : 'escalation')}
                    hasConfig={!!config.escalationAction}
                >
                    <div className="space-y-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                When time limit is exceeded
                            </label>
                            <select
                                className="w-full p-2 border rounded-lg text-sm"
                                value={config.escalationAction || ''}
                                onChange={(e) => updateConfig('escalationAction', e.target.value)}
                            >
                                <option value="">Do nothing</option>
                                {escalationActions.map(a => (
                                    <option key={a.id} value={a.code}>{a.label}</option>
                                ))}
                            </select>
                        </div>
                    </div>
                </ConfigSection>

                {/* Viewers Section */}
                <ConfigSection
                    title="Viewers"
                    icon={<Eye className="w-4 h-4" />}
                    isExpanded={expandedSection === 'viewers'}
                    onToggle={() => setExpandedSection(expandedSection === 'viewers' ? null : 'viewers')}
                    hasConfig={config.viewers && config.viewers.length > 0}
                >
                    <div className="space-y-4">
                        <p className="text-xs text-gray-500">
                            Configure who can view tasks for this step (read-only access)
                        </p>
                        <ViewerConfigPanel
                            viewers={config.viewers || []}
                            onChange={(viewers) => updateConfig('viewers', viewers)}
                            title=""
                        />
                        <div className="p-2 bg-amber-50 border border-amber-200 rounded text-xs text-amber-800">
                            ⚠️ These viewers can only see tasks for THIS step
                        </div>
                    </div>
                </ConfigSection>
            </div>

            {/* Bottom Status */}
            <div className="px-4 py-3 border-t bg-white text-xs text-gray-500">
                Changes are saved automatically when you click "Save Workflow"
            </div>
        </div>
    );
};

/**
 * Collapsible config section
 */
const ConfigSection = ({ title, icon, isExpanded, onToggle, hasConfig, children }) => (
    <div className="border-b">
        <button
            className="w-full px-4 py-3 flex items-center justify-between bg-white hover:bg-gray-50 transition-colors"
            onClick={onToggle}
        >
            <div className="flex items-center space-x-2">
                <span className={hasConfig ? 'text-blue-600' : 'text-gray-400'}>{icon}</span>
                <span className="font-medium text-gray-900">{title}</span>
                {hasConfig && (
                    <CheckCircle className="w-4 h-4 text-green-500" />
                )}
            </div>
            <ChevronRight className={`w-5 h-5 text-gray-400 transition-transform ${isExpanded ? 'rotate-90' : ''}`} />
        </button>
        {isExpanded && (
            <div className="px-4 py-4 bg-gray-50 border-t">
                {children}
            </div>
        )}
    </div>
);

export default WorkflowDesignerPage;
