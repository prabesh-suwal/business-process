import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { MemoApi, CasAdminApi, WorkflowConfigApi } from '../lib/api';
import BpmnDesigner from '../components/BpmnDesigner';
import CopyWorkflowModal from '../components/CopyWorkflowModal';
import { Button } from '../components/ui/button';
import { toast } from 'sonner';
import {
    ArrowLeft, Save, Workflow, Settings2, ChevronDown,
    Layers, Rocket, GitBranch, Copy, Lock, FolderPlus, Upload, X, FileCode2
} from 'lucide-react';
import { PageContainer } from '../components/PageContainer';
import { useGatewayConfigs } from '../hooks/useGatewayConfigs';
import PropertyPanel from '../components/workflow/PropertyPanel';
import GlobalSettingsModal from '../components/GlobalSettingsModal';

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
    const [copying, setCopying] = useState(false);

    // Copy workflow UI state
    const [showCopyDropdown, setShowCopyDropdown] = useState(false);
    const [showCopyModal, setShowCopyModal] = useState(false);
    const [showSettingsModal, setShowSettingsModal] = useState(false);
    const [showImportModal, setShowImportModal] = useState(false);
    const [importXmlText, setImportXmlText] = useState('');

    // Ref to BpmnDesigner for accessing modeler internals
    const bpmnDesignerRef = useRef(null);

    // Raw BPMN element for PropertyPanel
    const [selectedElement, setSelectedElement] = useState(null);

    // Check if workflow is deployed (locked for editing)
    const isDeployed = topic?.workflowTemplateId != null;

    // Selected step from BPMN
    const [selectedStep, setSelectedStep] = useState(null);
    const [selectedGateway, setSelectedGateway] = useState(null); // For gateway configuration
    const [allSteps, setAllSteps] = useState([]);
    const [stepConfigs, setStepConfigs] = useState({});

    // Gateway configurations hook
    const { configs: gatewayConfigs, saveConfig: saveGatewayConfig, getConfig } = useGatewayConfigs(topicId);

    // Viewer configuration
    const [memoWideViewers, setMemoWideViewers] = useState([]);

    // Override permissions (what users can customize when creating memos)
    const [overridePermissions, setOverridePermissions] = useState({
        allowOverrideAssignments: false,
        allowOverrideSLA: false,
        allowOverrideEscalation: false,
        allowOverrideViewers: false
    });



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

            // Load override permissions
            if (topicData.overridePermissions) {
                setOverridePermissions(topicData.overridePermissions);
            }

            // Extract steps from BPMN
            if (topicData.workflowXml) {
                const steps = extractStepsFromBpmn(topicData.workflowXml);
                setAllSteps(steps);
            }

            // Load saved step configurations from memo-service
            // Use topicId directly (memo-service stores the config per topic)
            try {
                const savedConfigs = await WorkflowConfigApi.getStepConfigs(topicId);
                const configMap = {};
                savedConfigs.forEach(c => {
                    configMap[c.taskKey] = {
                        // New rules-based format (for AdvancedAssignmentTab)
                        rules: c.assignmentConfig?.rules || [],
                        fallbackRoleId: c.assignmentConfig?.fallbackRoleId,
                        completionMode: c.assignmentConfig?.completionMode || 'ANY',
                        // Legacy multi-select format
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
                        viewers: c.viewerConfig?.viewers || [],
                        // Branching/condition config
                        conditionConfig: c.conditionConfig || null
                    };
                });
                setStepConfigs(configMap);
                console.log('Loaded step configs from memo-service:', configMap);
            } catch (e) {
                console.log('No saved configs found in memo-service:', e.message);
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

    // When BPMN changes, re-extract steps and auto-refresh stale conditions
    const handleBpmnChange = (newXml) => {
        setBpmnXml(newXml);
        const newSteps = extractStepsFromBpmn(newXml);

        // Auto-refresh stale condition targetSteps
        // If a condition's targetStep ID no longer exists but a step with matching name does, update it
        const newStepIds = new Set(newSteps.map(s => s.taskKey));
        const stepsByName = Object.fromEntries(newSteps.map(s => [s.taskName, s.taskKey]));

        setStepConfigs(prevConfigs => {
            const updatedConfigs = { ...prevConfigs };
            let hasChanges = false;

            Object.keys(updatedConfigs).forEach(taskKey => {
                const config = updatedConfigs[taskKey];
                if (!config?.conditionConfig?.conditions) return;

                const updatedConditions = config.conditionConfig.conditions.map(cond => {
                    // Check if targetStep is stale (doesn't exist in new BPMN)
                    if (cond.targetStep && !newStepIds.has(cond.targetStep)) {
                        // Try to find a step with matching name
                        const targetName = cond.targetStepName;
                        if (targetName && stepsByName[targetName]) {
                            console.log(`Auto-refreshing condition: ${cond.targetStep} -> ${stepsByName[targetName]} (matched by name: ${targetName})`);
                            hasChanges = true;
                            return { ...cond, targetStep: stepsByName[targetName] };
                        } else {
                            console.warn(`Stale condition: targetStep ${cond.targetStep} no longer exists and no matching name found`);
                        }
                    }
                    return cond;
                });

                if (hasChanges) {
                    updatedConfigs[taskKey] = {
                        ...config,
                        conditionConfig: { ...config.conditionConfig, conditions: updatedConditions }
                    };
                }
            });

            return updatedConfigs;
        });

        setAllSteps(newSteps);
    };

    // When user clicks an element in BPMN viewer
    const handleElementClick = (element) => {
        if (!element) {
            // Clicked empty canvas — close panel
            setSelectedStep(null);
            setSelectedGateway(null);
            setSelectedElement(null);
            return;
        }

        // Store raw element for PropertyPanel
        setSelectedElement(element);

        console.log('[WorkflowDesigner] Element clicked:', {
            id: element.id,
            type: element.type,
            name: element.businessObject?.name,
        });

        // Handle User Tasks
        if (element.type === 'bpmn:UserTask') {
            setSelectedGateway(null);
            const taskKey = element.id;
            const step = allSteps.find(s => s.taskKey === taskKey) || {
                taskKey,
                taskName: element.businessObject?.name || taskKey,
                stepOrder: allSteps.length + 1
            };
            setSelectedStep(step);
        }
        // Handle Gateways
        else if (element.type === 'bpmn:ParallelGateway' || element.type === 'bpmn:InclusiveGateway' || element.type === 'bpmn:ExclusiveGateway') {
            setSelectedStep(null);
            const gatewayId = element.id;
            const gatewayName = element.businessObject?.name || (element.type === 'bpmn:ExclusiveGateway' ? 'Decision Gateway' : 'Parallel Gateway');

            setSelectedGateway({
                id: gatewayId,
                name: gatewayName,
                type: element.type.replace('bpmn:', ''),
                incomingFlows: (element.businessObject?.incoming || []).length,
                outgoingFlows: (element.businessObject?.outgoing || []).length,
                incoming: element.businessObject?.incoming || [],
                outgoing: element.businessObject?.outgoing || [],
            });
        }
        // Handle Sequence Flows (for conditions)
        else if (element.type === 'bpmn:SequenceFlow') {
            setSelectedStep(null);
            setSelectedGateway(null);
            // selectedElement already stored above — PropertyPanel handles the rest
        }
        // Other elements (StartEvent, EndEvent, etc.)
        else {
            setSelectedStep(null);
            setSelectedGateway(null);
            // selectedElement stored — General tab will show
        }
    };

    // Navigate to a specific element by ID (used by ConditionsTab gateway flow list)
    const handleSelectElement = (elementId) => {
        if (!bpmnDesignerRef.current) return;
        const registry = bpmnDesignerRef.current.getElementRegistry();
        if (!registry) return;
        const el = registry.get(elementId);
        if (el) {
            handleElementClick(el);
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

            // Save BPMN XML only if it exists
            if (bpmnXml) {
                await MemoApi.updateTopicWorkflow(topicId, bpmnXml);
            }

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
                    viewerConfig: uiConfig.viewers && uiConfig.viewers.length > 0 ? { viewers: uiConfig.viewers } : {},
                    conditionConfig: uiConfig.conditionConfig || null
                };
                return WorkflowConfigApi.saveStepConfig(topicId, taskKey, apiConfig);
            });

            await Promise.all(savePromises);

            // Save memo-wide viewers
            if (memoWideViewers && memoWideViewers.length > 0) {
                await MemoApi.updateTopicViewers(topicId, { viewers: memoWideViewers });
            }

            // Save override permissions
            await MemoApi.updateTopicOverridePermissions(topicId, overridePermissions);

            toast.success('Workflow saved successfully!');

        } catch (error) {
            console.error('Save error:', error);
            toast.error('Failed to save workflow: ' + (error.response?.data?.message || error.message));
        } finally {
            setSaving(false);
        }
    };

    // Copy as New Version - frontend only until save/deploy
    const handleCopyAsNewVersion = async () => {
        try {
            setCopying(true);
            setShowCopyDropdown(false);

            // Call backend to create a new version (increments version, clears workflowTemplateId)
            const updatedTopic = await MemoApi.copyTopicWorkflow(topicId);

            // Update local state with the new version
            setTopic(updatedTopic);

            toast.success(`Now editing version ${updatedTopic.workflowVersion}. Save or deploy when ready.`);
        } catch (error) {
            console.error('Copy error:', error);
            toast.error('Failed to create new version: ' + (error.response?.data?.message || error.message));
        } finally {
            setCopying(false);
        }
    };

    // Copy as New Topic - show modal to collect details
    const handleCopyAsNewTopic = () => {
        setShowCopyDropdown(false);
        setShowCopyModal(true);
    };

    // Handle when new topic is created from the copy modal
    const handleTopicCreated = (newTopic) => {
        setShowCopyModal(false);
        toast.success(`Topic "${newTopic.name}" created! Navigating to workflow designer...`);
        // Navigate to the new topic's workflow page
        navigate(`/workflow/${newTopic.id}`);
    };

    // Save workflow AND deploy to Flowable engine
    const handleSaveAndDeploy = async () => {
        try {
            setDeploying(true);

            let currentTopicId = topicId;

            console.log('💾 Starting save & deploy...');
            console.log('📋 stepConfigs:', JSON.stringify(stepConfigs, null, 2));
            console.log('🔢 Number of steps to save:', Object.keys(stepConfigs).length);

            // First save BPMN to topic
            await MemoApi.updateTopicWorkflow(currentTopicId, bpmnXml);

            // Save step configs to memo-service (REQUIRED for BpmnEnricher to inject conditions)
            const saveToMemoPromises = Object.keys(stepConfigs).map(taskKey => {
                const uiConfig = stepConfigs[taskKey];

                const assignmentConfig = {
                    // New rules-based format (for AdvancedAssignmentTab)
                    rules: uiConfig.rules || [],
                    fallbackRoleId: uiConfig.fallbackRoleId,
                    completionMode: uiConfig.completionMode || 'ANY',
                    // Legacy multi-select format
                    roles: uiConfig.roles || [],
                    departments: uiConfig.departments || [],
                    users: uiConfig.users || []
                };
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
                    viewerConfig: uiConfig.viewers && uiConfig.viewers.length > 0 ? { viewers: uiConfig.viewers } : {},
                    conditionConfig: uiConfig.conditionConfig || null
                };
                return WorkflowConfigApi.saveStepConfig(currentTopicId, taskKey, apiConfig);
            });
            await Promise.all(saveToMemoPromises);

            // Deploy the workflow to Flowable FIRST (creates ProcessTemplate, returns processTemplateId)
            const deployResult = await MemoApi.deployTopicWorkflow(currentTopicId);
            console.log('🚀 Deployed workflow, result:', deployResult);

            // Refresh topic to get the new workflowTemplateId (processTemplateId)
            const updatedTopic = await MemoApi.getTopic(currentTopicId);
            const processTemplateId = updatedTopic.workflowTemplateId;
            setTopic(updatedTopic);

            console.log('📌 ProcessTemplateId:', processTemplateId);

            // Now save step configs to WORKFLOW-SERVICE using processTemplateId
            if (processTemplateId) {
                const savePromises = Object.keys(stepConfigs).map(taskKey => {
                    const uiConfig = stepConfigs[taskKey];

                    // Build assignment config with new rules-based format
                    const assignmentConfig = {
                        // New rules-based format (for AdvancedAssignmentTab)
                        rules: uiConfig.rules || [],
                        fallbackRoleId: uiConfig.fallbackRoleId,
                        completionMode: uiConfig.completionMode || 'ANY',
                        // Legacy multi-select format
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
                        viewerConfig: uiConfig.viewers && uiConfig.viewers.length > 0 ? { viewers: uiConfig.viewers } : {},
                        conditionConfig: uiConfig.conditionConfig || null
                    };

                    console.log('Saving step config to workflow-service for', taskKey, ':', JSON.stringify(apiConfig, null, 2));

                    // Use NEW workflow-service endpoint with processTemplateId
                    return WorkflowConfigApi.saveTaskConfig(processTemplateId, taskKey, apiConfig);
                });
                await Promise.all(savePromises);
                console.log('✅ Step configs saved to workflow-service');
            } else {
                console.warn('⚠️ No processTemplateId - step configs not saved to workflow-service');
            }

            // Save memo-wide viewers
            if (memoWideViewers && memoWideViewers.length > 0) {
                await MemoApi.updateTopicViewers(currentTopicId, { viewers: memoWideViewers });
                console.log('Memo-wide viewers saved');
            }

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
            <div className="flex items-center justify-center h-screen bg-gradient-to-br from-slate-50 to-blue-50">
                <div className="text-center">
                    <div className="relative">
                        <div className="animate-spin rounded-full h-16 w-16 border-4 border-blue-200 border-t-blue-600 mx-auto"></div>
                        <Workflow className="w-6 h-6 text-blue-600 absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2" />
                    </div>
                    <p className="mt-6 text-slate-600 font-medium">Loading Workflow Designer</p>
                    <p className="mt-1 text-slate-400 text-sm">Preparing your canvas...</p>
                </div>
            </div>
        );
    }

    return (
        <>
            <PageContainer className="h-screen flex flex-col bg-slate-100 space-y-0 p-0">
                {/* Premium Header Bar */}
                <div className="bg-gradient-to-r from-slate-900 via-slate-800 to-slate-900 px-6 py-4 flex items-center justify-between shadow-lg">
                    <div className="flex items-center space-x-5">
                        <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => navigate('/settings')}
                            className="text-slate-300 hover:text-white hover:bg-white/10 transition-all"
                        >
                            <ArrowLeft className="w-4 h-4 mr-2" />
                            Settings
                        </Button>
                        <div className="h-8 w-px bg-slate-600" />
                        <div className="flex items-center gap-3">
                            <div className="h-10 w-10 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center shadow-lg shadow-blue-500/20">
                                <GitBranch className="w-5 h-5 text-white" />
                            </div>
                            <div>
                                <h1 className="text-lg font-semibold text-white flex items-center gap-2">
                                    {topic?.name || 'Workflow Designer'}
                                </h1>
                                <p className="text-xs text-slate-400">Design approval flows & configure automation</p>
                            </div>
                        </div>
                    </div>
                    <div className="flex items-center space-x-3">
                        {/* Import BPMN Button */}
                        {!isDeployed && (
                            <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => { setImportXmlText(''); setShowImportModal(true); }}
                                className="text-slate-300 hover:text-white hover:bg-white/10 transition-all"
                                title="Import BPMN XML"
                            >
                                <Upload className="w-4 h-4" />
                            </Button>
                        )}
                        {/* Global Settings Button */}
                        <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setShowSettingsModal(true)}
                            className="text-slate-300 hover:text-white hover:bg-white/10 transition-all"
                            title="Workflow Settings"
                        >
                            <Settings2 className="w-4 h-4" />
                        </Button>
                        <div className="h-6 w-px bg-slate-600" />
                        {/* Status Badges */}
                        <div className="flex items-center gap-2 mr-2">
                            {/* Version Badge */}
                            <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-white/5 border border-white/10">
                                <span className="text-xs font-medium text-slate-300">v{topic?.workflowVersion || 1}</span>
                            </div>
                            {isDeployed ? (
                                <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-emerald-500/10 border border-emerald-500/20">
                                    <Lock className="w-3 h-3 text-emerald-400" />
                                    <span className="text-xs font-medium text-emerald-400">Deployed (Locked)</span>
                                </div>
                            ) : (
                                <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-amber-500/10 border border-amber-500/20">
                                    <div className="w-2 h-2 rounded-full bg-amber-400" />
                                    <span className="text-xs font-medium text-amber-400">Draft</span>
                                </div>
                            )}
                            <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-white/5 border border-white/10">
                                <Layers className="w-3 h-3 text-slate-400" />
                                <span className="text-xs font-medium text-slate-300">{allSteps.length} steps</span>
                            </div>
                        </div>

                        {/* Action Buttons - Show based on state */}
                        {isDeployed ? (
                            /* Deployed: Show Copy Dropdown */
                            <div className="relative">
                                <Button
                                    size="sm"
                                    onClick={() => setShowCopyDropdown(!showCopyDropdown)}
                                    className="bg-gradient-to-r from-blue-500 to-indigo-600 hover:from-blue-600 hover:to-indigo-700 text-white shadow-lg shadow-blue-500/25 border-0 transition-all"
                                >
                                    <Copy className="w-4 h-4 mr-2" />
                                    Copy
                                    <ChevronDown className="w-4 h-4 ml-2" />
                                </Button>

                                {showCopyDropdown && (
                                    <>
                                        <div
                                            className="fixed inset-0 z-10"
                                            onClick={() => setShowCopyDropdown(false)}
                                        />
                                        <div className="absolute right-0 mt-2 w-56 bg-slate-800 border border-slate-700 rounded-lg shadow-xl z-20 overflow-hidden">
                                            <button
                                                onClick={handleCopyAsNewVersion}
                                                className="w-full flex items-center gap-3 px-4 py-3 text-left hover:bg-slate-700 transition-colors"
                                            >
                                                <div className="p-1.5 rounded-lg bg-blue-500/20">
                                                    <Copy className="w-4 h-4 text-blue-400" />
                                                </div>
                                                <div>
                                                    <div className="text-sm font-medium text-white">Copy as New Version</div>
                                                    <div className="text-xs text-slate-400">Create v{(topic?.workflowVersion || 1) + 1} of this workflow</div>
                                                </div>
                                            </button>
                                            <div className="border-t border-slate-700" />
                                            <button
                                                onClick={handleCopyAsNewTopic}
                                                className="w-full flex items-center gap-3 px-4 py-3 text-left hover:bg-slate-700 transition-colors"
                                            >
                                                <div className="p-1.5 rounded-lg bg-indigo-500/20">
                                                    <FolderPlus className="w-4 h-4 text-indigo-400" />
                                                </div>
                                                <div>
                                                    <div className="text-sm font-medium text-white">Copy as New Topic</div>
                                                    <div className="text-xs text-slate-400">Create a new topic with this workflow</div>
                                                </div>
                                            </button>
                                        </div>
                                    </>
                                )}
                            </div>
                        ) : (
                            /* Draft: Show Save/Deploy */
                            <>
                                <Button
                                    variant="outline"
                                    size="sm"
                                    onClick={handleSaveAll}
                                    disabled={saving || deploying}
                                    className="bg-white/5 border-white/10 text-white hover:bg-white/10 hover:border-white/20 transition-all"
                                >
                                    {saving ? (
                                        <div className="animate-spin rounded-full h-4 w-4 border-2 border-white/30 border-t-white mr-2" />
                                    ) : (
                                        <Save className="w-4 h-4 mr-2" />
                                    )}
                                    Save Draft
                                </Button>

                                <Button
                                    size="sm"
                                    onClick={handleSaveAndDeploy}
                                    disabled={saving || deploying}
                                    className="bg-gradient-to-r from-emerald-500 to-emerald-600 hover:from-emerald-600 hover:to-emerald-700 text-white shadow-lg shadow-emerald-500/25 border-0 transition-all"
                                >
                                    {deploying ? (
                                        <div className="animate-spin rounded-full h-4 w-4 border-2 border-white/30 border-t-white mr-2" />
                                    ) : (
                                        <Rocket className="w-4 h-4 mr-2" />
                                    )}
                                    Deploy
                                </Button>
                            </>
                        )}
                    </div>
                </div>
                {/* Main Content — Full-width BPMN + sliding PropertyPanel */}
                <div className="flex-1 flex overflow-hidden bg-gradient-to-b from-slate-50 to-white">
                    {/* BPMN Diagram — takes remaining width */}
                    <div className="flex-1 bg-white relative min-h-0 min-w-0">
                        <BpmnDesigner
                            ref={bpmnDesignerRef}
                            initialXml={bpmnXml}
                            onXmlChange={handleBpmnChange}
                            onElementClick={handleElementClick}
                            height="100%"
                        />
                    </div>

                    {/* Sliding Property Panel */}
                    <div
                        className={`transition-all duration-300 ease-in-out border-l border-slate-200 bg-white flex-shrink-0 overflow-hidden ${selectedElement ? 'w-[400px] opacity-100' : 'w-0 opacity-0 border-l-0'
                            }`}
                    >
                        {selectedElement && (
                            <PropertyPanel
                                element={selectedElement}
                                onClose={() => {
                                    setSelectedElement(null);
                                    setSelectedStep(null);
                                    setSelectedGateway(null);
                                }}
                                modelerRef={bpmnDesignerRef}
                                // UserTask props
                                stepConfig={selectedStep ? (stepConfigs[selectedStep.taskKey] || {}) : {}}
                                onStepConfigChange={selectedStep ? (config) => updateStepConfig(selectedStep.taskKey, config) : undefined}
                                roles={roles}
                                groups={groups}
                                departments={departments}
                                slaDurations={slaDurations}
                                escalationActions={escalationActions}
                                topicId={topicId}
                                allSteps={allSteps}
                                // Gateway props
                                gatewayConfig={selectedGateway ? getConfig(selectedGateway.id) : undefined}
                                onGatewayUpdate={selectedGateway ? (newConfig) => saveGatewayConfig(selectedGateway.id, newConfig) : undefined}
                                // Flow navigation
                                onSelectElement={handleSelectElement}
                            />
                        )}
                    </div>
                </div>

            </PageContainer>

            {/* Copy as New Topic Modal */}
            <CopyWorkflowModal
                isOpen={showCopyModal}
                onClose={() => setShowCopyModal(false)}
                onTopicCreated={handleTopicCreated}
                sourceTopic={topic}
                sourceWorkflowXml={bpmnXml}
            />

            {/* Global Settings Modal */}
            <GlobalSettingsModal
                isOpen={showSettingsModal}
                onClose={() => setShowSettingsModal(false)}
                memoWideViewers={memoWideViewers}
                onMemoWideViewersChange={setMemoWideViewers}
                overridePermissions={overridePermissions}
                onOverridePermissionsChange={setOverridePermissions}
            />

            {/* Import BPMN XML Modal */}
            {showImportModal && (
                <div className="fixed inset-0 z-50 flex items-center justify-center">
                    {/* Backdrop */}
                    <div
                        className="absolute inset-0 bg-black/60 backdrop-blur-sm"
                        onClick={() => setShowImportModal(false)}
                    />
                    {/* Modal */}
                    <div className="relative bg-slate-900 border border-slate-700 rounded-2xl shadow-2xl w-full max-w-2xl mx-4 overflow-hidden">
                        {/* Header */}
                        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-700">
                            <div className="flex items-center gap-3">
                                <div className="p-2 rounded-lg bg-blue-500/20">
                                    <FileCode2 className="w-5 h-5 text-blue-400" />
                                </div>
                                <div>
                                    <h3 className="text-lg font-semibold text-white">Import BPMN XML</h3>
                                    <p className="text-xs text-slate-400">Paste a BPMN 2.0 XML diagram to replace the current one</p>
                                </div>
                            </div>
                            <button
                                onClick={() => setShowImportModal(false)}
                                className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-white/10 transition-all"
                            >
                                <X className="w-5 h-5" />
                            </button>
                        </div>
                        {/* Body */}
                        <div className="px-6 py-4">
                            <textarea
                                value={importXmlText}
                                onChange={(e) => setImportXmlText(e.target.value)}
                                placeholder={'Paste your BPMN XML here...\n\n<?xml version="1.0" encoding="UTF-8"?>\n<bpmn:definitions ...>'}
                                className="w-full h-72 bg-slate-800 border border-slate-600 rounded-xl p-4 text-sm text-slate-200 font-mono resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent placeholder:text-slate-500"
                                spellCheck={false}
                            />
                            <p className="mt-2 text-xs text-slate-500">The XML must contain a valid BPMN 2.0 process definition.</p>
                        </div>
                        {/* Footer */}
                        <div className="flex items-center justify-end gap-3 px-6 py-4 border-t border-slate-700">
                            <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => setShowImportModal(false)}
                                className="text-slate-400 hover:text-white hover:bg-white/10"
                            >
                                Cancel
                            </Button>
                            <Button
                                size="sm"
                                disabled={!importXmlText.trim()}
                                onClick={async () => {
                                    const xml = importXmlText.trim();
                                    // Basic validation
                                    if (!xml.includes('<bpmn:definitions') && !xml.includes('<definitions')) {
                                        toast.error('Invalid BPMN XML — must contain a <definitions> root element');
                                        return;
                                    }
                                    try {
                                        await bpmnDesignerRef.current.importXML(xml);
                                        setBpmnXml(xml);
                                        setShowImportModal(false);
                                        setImportXmlText('');
                                        toast.success('BPMN diagram imported successfully');
                                    } catch (err) {
                                        console.error('Import error:', err);
                                        toast.error('Failed to import XML: ' + (err.message || 'Invalid BPMN'));
                                    }
                                }}
                                className="bg-gradient-to-r from-blue-500 to-indigo-600 hover:from-blue-600 hover:to-indigo-700 text-white border-0"
                            >
                                <Upload className="w-4 h-4 mr-2" />
                                Import Diagram
                            </Button>
                        </div>
                    </div>
                </div>
            )}
        </>
    );
};

export default WorkflowDesignerPage;

