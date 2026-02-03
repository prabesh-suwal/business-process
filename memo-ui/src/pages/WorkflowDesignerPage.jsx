import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { MemoApi, CasAdminApi, WorkflowConfigApi } from '../lib/api';
import BpmnDesigner from '../components/BpmnDesigner';
import CopyWorkflowModal from '../components/CopyWorkflowModal';
import { Card, CardHeader, CardTitle, CardContent, CardDescription } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';
import { Badge } from '../components/ui/badge';
import { toast } from 'sonner';
import {
    ArrowLeft, Users, Clock, AlertTriangle, CheckCircle,
    FileText, Save, Eye, Workflow, Settings, ChevronRight, ChevronDown,
    Layers, MousePointer2, Rocket, Sparkles, Zap, GitBranch, Unlock, ToggleLeft, ShieldCheck, Copy, Lock, FolderPlus
} from 'lucide-react';
import ViewerConfigPanel from '../components/ViewerConfigPanel';
import AssignmentConfigPanel from '../components/AssignmentConfigPanel';
import ConditionBuilder from '../components/ConditionBuilder';
import { PageContainer } from '../components/PageContainer';
import GatewayConfigPanel from '../components/GatewayConfigPanel';
import { useGatewayConfigs } from '../hooks/useGatewayConfigs';

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

    // Collapsible section states
    const [collapsedSections, setCollapsedSections] = useState({
        overridePermissions: false,
        memoWideViewers: false,
        workflowSteps: false
    });

    const toggleSection = (section) => {
        setCollapsedSections(prev => ({ ...prev, [section]: !prev[section] }));
    };

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
        if (!element) return;

        // Debug logging to see what's being clicked
        console.log('[WorkflowDesigner] Element clicked:', {
            id: element.id,
            type: element.type,
            name: element.businessObject?.name,
            incoming: element.businessObject?.incoming?.length,
            outgoing: element.businessObject?.outgoing?.length
        });

        // Handle User Tasks
        if (element.type === 'bpmn:UserTask') {
            setSelectedGateway(null); // Clear gateway selection
            const taskKey = element.id;
            const step = allSteps.find(s => s.taskKey === taskKey) || {
                taskKey,
                taskName: element.businessObject?.name || taskKey,
                stepOrder: allSteps.length + 1
            };
            setSelectedStep(step);
            console.log('[WorkflowDesigner] Selected UserTask:', step);
        }
        // Handle Parallel Gateways (for completion config)
        else if (element.type === 'bpmn:ParallelGateway' || element.type === 'bpmn:InclusiveGateway') {
            setSelectedStep(null); // Clear step selection
            const gatewayId = element.id;
            const gatewayName = element.businessObject?.name || 'Parallel Gateway';
            const incoming = element.businessObject?.incoming || [];
            const outgoing = element.businessObject?.outgoing || [];

            const gateway = {
                id: gatewayId,
                name: gatewayName,
                type: element.type.replace('bpmn:', ''),
                incomingFlows: incoming.length,
                outgoingFlows: outgoing.length,
                incoming: incoming,
                outgoing: outgoing
            };
            setSelectedGateway(gateway);
            console.log('[WorkflowDesigner] Selected Gateway:', gateway);
        }
        // Also handle ExclusiveGateway (decision point)
        else if (element.type === 'bpmn:ExclusiveGateway') {
            setSelectedStep(null);
            const gatewayId = element.id;
            const gatewayName = element.businessObject?.name || 'Decision Gateway';
            const incoming = element.businessObject?.incoming || [];
            const outgoing = element.businessObject?.outgoing || [];

            setSelectedGateway({
                id: gatewayId,
                name: gatewayName,
                type: 'ExclusiveGateway',
                incomingFlows: incoming.length,
                outgoingFlows: outgoing.length,
                incoming: incoming,
                outgoing: outgoing
            });
            console.log('[WorkflowDesigner] Selected ExclusiveGateway');
        }
        // Clear selections for other element types
        else {
            setSelectedStep(null);
            setSelectedGateway(null);
            console.log('[WorkflowDesigner] Cleared selection for type:', element.type);
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
                {/* Main Content - Tabs View */}
                <div className="flex-1 flex flex-col overflow-hidden bg-gradient-to-b from-slate-50 to-white">
                    <Tabs defaultValue="diagram" className="flex-1 flex flex-col">
                        <div className="border-b border-slate-200 px-6 bg-white/80 backdrop-blur-sm">
                            <TabsList className="bg-transparent p-0 h-12 w-full justify-start gap-1">
                                <TabsTrigger
                                    value="diagram"
                                    className="relative data-[state=active]:bg-transparent data-[state=active]:shadow-none rounded-none px-4 h-12 font-medium text-slate-500 data-[state=active]:text-blue-600 transition-all hover:text-slate-700 group"
                                >
                                    <div className="flex items-center gap-2">
                                        <div className="p-1.5 rounded-lg bg-slate-100 group-data-[state=active]:bg-blue-100 transition-colors">
                                            <Layers className="w-4 h-4 group-data-[state=active]:text-blue-600" />
                                        </div>
                                        <span>Workflow Diagram</span>
                                    </div>
                                    <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-blue-600 scale-x-0 data-[state=active]:scale-x-100 transition-transform origin-left" />
                                </TabsTrigger>
                                <TabsTrigger
                                    value="rules"
                                    className="relative data-[state=active]:bg-transparent data-[state=active]:shadow-none rounded-none px-4 h-12 font-medium text-slate-500 data-[state=active]:text-blue-600 transition-all hover:text-slate-700 group"
                                >
                                    <div className="flex items-center gap-2">
                                        <div className="p-1.5 rounded-lg bg-slate-100 group-data-[state=active]:bg-blue-100 transition-colors">
                                            <Settings className="w-4 h-4 group-data-[state=active]:text-blue-600" />
                                        </div>
                                        <span>Configuration & Rules</span>
                                        {allSteps.length > 0 && (
                                            <span className="ml-1 px-1.5 py-0.5 text-xs font-medium rounded-full bg-slate-200 text-slate-600 group-data-[state=active]:bg-blue-100 group-data-[state=active]:text-blue-600">
                                                {allSteps.length}
                                            </span>
                                        )}
                                    </div>
                                    <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-blue-600 scale-x-0 data-[state=active]:scale-x-100 transition-transform origin-left" />
                                </TabsTrigger>
                            </TabsList>
                        </div>

                        {/* Tab 1: Workflow Diagram */}
                        <TabsContent value="diagram" className="flex-1 flex flex-col m-0 p-0 overflow-hidden data-[state=active]:flex data-[state=inactive]:hidden">
                            <div className="flex-1 bg-white relative min-h-0">
                                <BpmnDesigner
                                    initialXml={bpmnXml}
                                    onXmlChange={handleBpmnChange}
                                    onElementClick={handleElementClick}
                                    height="100%"
                                />
                            </div>
                        </TabsContent>

                        {/* Tab 2: Rules & Configuration */}
                        <TabsContent value="rules" className="flex-1 flex m-0 p-0 overflow-hidden data-[state=active]:flex data-[state=inactive]:hidden bg-slate-50">
                            <div className="w-full flex flex-col lg:flex-row h-full p-4 lg:p-6 gap-4 lg:gap-6 overflow-hidden">
                                {/* Left Side: Step List & Viewers - Unified scrollable container */}
                                <div className="w-full lg:w-[340px] flex-shrink-0 max-h-[calc(100vh-200px)] overflow-y-auto scroll-smooth">
                                    <div className="space-y-4 pb-[50vh]"> {/* Bottom padding for scroll-to-middle */}
                                        {/* Override Permissions Card */}
                                        <div className="bg-white rounded-xl shadow-sm border border-slate-200 mb-4 overflow-hidden">
                                            <button
                                                onClick={() => toggleSection('overridePermissions')}
                                                className="w-full px-4 py-3 bg-gradient-to-r from-emerald-50 to-teal-50 border-b border-slate-100 flex items-center justify-between hover:from-emerald-100 hover:to-teal-100 transition-colors"
                                            >
                                                <div className="flex items-center gap-2">
                                                    <div className="p-1.5 rounded-lg bg-white shadow-sm">
                                                        <Unlock className="h-4 w-4 text-emerald-600" />
                                                    </div>
                                                    <div className="text-left">
                                                        <h3 className="text-sm font-semibold text-slate-800">User Override Permissions</h3>
                                                        <p className="text-xs text-slate-500">Allow memo creators to customize</p>
                                                    </div>
                                                </div>
                                                <ChevronDown className={`w-5 h-5 text-slate-400 transition-transform duration-200 ${collapsedSections.overridePermissions ? '-rotate-90' : ''}`} />
                                            </button>
                                            {!collapsedSections.overridePermissions && (
                                                <div className="p-4 space-y-3">
                                                    {/* Toggle: Assignments */}
                                                    <label className="flex items-center justify-between cursor-pointer group">
                                                        <div className="flex items-center gap-2">
                                                            <Users className="w-4 h-4 text-slate-400" />
                                                            <span className="text-sm text-slate-700">Modify assignments</span>
                                                        </div>
                                                        <button
                                                            type="button"
                                                            onClick={() => setOverridePermissions(p => ({ ...p, allowOverrideAssignments: !p.allowOverrideAssignments }))}
                                                            className={`relative w-11 h-6 rounded-full transition-colors duration-200 ${overridePermissions.allowOverrideAssignments
                                                                ? 'bg-emerald-500'
                                                                : 'bg-slate-200'
                                                                }`}
                                                        >
                                                            <span className={`absolute top-0.5 left-0.5 w-5 h-5 bg-white rounded-full shadow-sm transition-transform duration-200 ${overridePermissions.allowOverrideAssignments ? 'translate-x-5' : ''
                                                                }`} />
                                                        </button>
                                                    </label>

                                                    {/* Toggle: SLA */}
                                                    <label className="flex items-center justify-between cursor-pointer group">
                                                        <div className="flex items-center gap-2">
                                                            <Clock className="w-4 h-4 text-slate-400" />
                                                            <span className="text-sm text-slate-700">Modify time limits</span>
                                                        </div>
                                                        <button
                                                            type="button"
                                                            onClick={() => setOverridePermissions(p => ({ ...p, allowOverrideSLA: !p.allowOverrideSLA }))}
                                                            className={`relative w-11 h-6 rounded-full transition-colors duration-200 ${overridePermissions.allowOverrideSLA
                                                                ? 'bg-emerald-500'
                                                                : 'bg-slate-200'
                                                                }`}
                                                        >
                                                            <span className={`absolute top-0.5 left-0.5 w-5 h-5 bg-white rounded-full shadow-sm transition-transform duration-200 ${overridePermissions.allowOverrideSLA ? 'translate-x-5' : ''
                                                                }`} />
                                                        </button>
                                                    </label>

                                                    {/* Toggle: Escalation */}
                                                    <label className="flex items-center justify-between cursor-pointer group">
                                                        <div className="flex items-center gap-2">
                                                            <AlertTriangle className="w-4 h-4 text-slate-400" />
                                                            <span className="text-sm text-slate-700">Modify escalation rules</span>
                                                        </div>
                                                        <button
                                                            type="button"
                                                            onClick={() => setOverridePermissions(p => ({ ...p, allowOverrideEscalation: !p.allowOverrideEscalation }))}
                                                            className={`relative w-11 h-6 rounded-full transition-colors duration-200 ${overridePermissions.allowOverrideEscalation
                                                                ? 'bg-emerald-500'
                                                                : 'bg-slate-200'
                                                                }`}
                                                        >
                                                            <span className={`absolute top-0.5 left-0.5 w-5 h-5 bg-white rounded-full shadow-sm transition-transform duration-200 ${overridePermissions.allowOverrideEscalation ? 'translate-x-5' : ''
                                                                }`} />
                                                        </button>
                                                    </label>

                                                    {/* Toggle: Viewers */}
                                                    <label className="flex items-center justify-between cursor-pointer group">
                                                        <div className="flex items-center gap-2">
                                                            <Eye className="w-4 h-4 text-slate-400" />
                                                            <span className="text-sm text-slate-700">Add/remove viewers</span>
                                                        </div>
                                                        <button
                                                            type="button"
                                                            onClick={() => setOverridePermissions(p => ({ ...p, allowOverrideViewers: !p.allowOverrideViewers }))}
                                                            className={`relative w-11 h-6 rounded-full transition-colors duration-200 ${overridePermissions.allowOverrideViewers
                                                                ? 'bg-emerald-500'
                                                                : 'bg-slate-200'
                                                                }`}
                                                        >
                                                            <span className={`absolute top-0.5 left-0.5 w-5 h-5 bg-white rounded-full shadow-sm transition-transform duration-200 ${overridePermissions.allowOverrideViewers ? 'translate-x-5' : ''
                                                                }`} />
                                                        </button>
                                                    </label>
                                                </div>
                                            )}
                                        </div>

                                        {/* Memo-Wide Viewers Card */}
                                        <div className="bg-white rounded-xl shadow-sm border border-slate-200 mb-4 overflow-hidden">
                                            <button
                                                onClick={() => toggleSection('memoWideViewers')}
                                                className="w-full px-4 py-3 bg-gradient-to-r from-indigo-50 to-blue-50 border-b border-slate-100 flex items-center justify-between hover:from-indigo-100 hover:to-blue-100 transition-colors"
                                            >
                                                <div className="flex items-center gap-2">
                                                    <div className="p-1.5 rounded-lg bg-white shadow-sm">
                                                        <Eye className="h-4 w-4 text-indigo-600" />
                                                    </div>
                                                    <div className="text-left">
                                                        <h3 className="text-sm font-semibold text-slate-800">Memo-Wide Viewers</h3>
                                                        <p className="text-xs text-slate-500">Read-only access for all steps</p>
                                                    </div>
                                                </div>
                                                <ChevronDown className={`w-5 h-5 text-slate-400 transition-transform duration-200 ${collapsedSections.memoWideViewers ? '-rotate-90' : ''}`} />
                                            </button>
                                            {!collapsedSections.memoWideViewers && (
                                                <div className="p-4">
                                                    <ViewerConfigPanel
                                                        viewers={memoWideViewers}
                                                        onChange={setMemoWideViewers}
                                                        title=""
                                                    />
                                                </div>
                                            )}
                                        </div>

                                        {/* Step List Card */}
                                        <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden flex flex-col">
                                            <button
                                                onClick={() => toggleSection('workflowSteps')}
                                                className="w-full px-4 py-3 bg-gradient-to-r from-slate-800 to-slate-900 flex items-center justify-between hover:from-slate-700 hover:to-slate-800 transition-colors"
                                            >
                                                <h2 className="text-sm font-semibold text-white flex items-center">
                                                    <Layers className="w-4 h-4 mr-2 text-blue-400" />
                                                    Workflow Steps
                                                </h2>
                                                <div className="flex items-center gap-2">
                                                    <span className="px-2 py-0.5 text-xs font-medium rounded-full bg-white/10 text-slate-300">
                                                        {allSteps.length} total
                                                    </span>
                                                    <ChevronDown className={`w-5 h-5 text-slate-400 transition-transform duration-200 ${collapsedSections.workflowSteps ? '-rotate-90' : ''}`} />
                                                </div>
                                            </button>

                                            {!collapsedSections.workflowSteps && (
                                                <div className="p-2">
                                                    {allSteps.length === 0 ? (
                                                        <div className="p-8 text-center">
                                                            <div className="w-16 h-16 mx-auto mb-4 rounded-2xl bg-slate-100 flex items-center justify-center">
                                                                <Layers className="w-8 h-8 text-slate-300" />
                                                            </div>
                                                            <p className="text-slate-500 text-sm font-medium">No steps found</p>
                                                            <p className="text-slate-400 text-xs mt-1">Add User Tasks in the Diagram tab</p>
                                                        </div>
                                                    ) : (
                                                        <div className="space-y-1.5">
                                                            {allSteps.map((step, i) => {
                                                                const isSelected = selectedStep?.taskKey === step.taskKey;
                                                                const isConfigured = stepConfigs[step.taskKey]?.roles?.length > 0 ||
                                                                    stepConfigs[step.taskKey]?.users?.length > 0 ||
                                                                    stepConfigs[step.taskKey]?.departments?.length > 0;
                                                                return (
                                                                    <button
                                                                        key={step.taskKey}
                                                                        className={`w-full p-3 text-left flex items-center rounded-lg transition-all duration-200 group ${isSelected
                                                                            ? 'bg-blue-50 ring-2 ring-blue-500 ring-inset shadow-sm'
                                                                            : 'hover:bg-slate-50 border border-transparent hover:border-slate-200'
                                                                            }`}
                                                                        onClick={() => setSelectedStep(step)}
                                                                    >
                                                                        <div className={`w-9 h-9 rounded-xl flex items-center justify-center text-sm font-semibold mr-3 transition-all ${isSelected
                                                                            ? 'bg-blue-600 text-white shadow-lg shadow-blue-500/30'
                                                                            : 'bg-slate-100 text-slate-500 group-hover:bg-slate-200'
                                                                            }`}>
                                                                            {i + 1}
                                                                        </div>
                                                                        <div className="flex-1 min-w-0">
                                                                            <div className={`font-medium truncate transition-colors ${isSelected ? 'text-blue-900' : 'text-slate-700'
                                                                                }`}>{step.taskName}</div>
                                                                            <div className="text-xs text-slate-400 truncate font-mono">{step.taskKey}</div>
                                                                        </div>
                                                                        {isConfigured && (
                                                                            <div className="ml-2 p-1 rounded-full bg-emerald-100">
                                                                                <CheckCircle className="w-3.5 h-3.5 text-emerald-600" />
                                                                            </div>
                                                                        )}
                                                                    </button>
                                                                );
                                                            })}
                                                        </div>
                                                    )}
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                </div> {/* Close scroll wrapper and left side */}

                                {/* Right Side: Configuration Panel */}
                                <div className="flex-1 bg-white rounded-xl shadow-sm border border-slate-200 min-h-[400px] lg:h-full overflow-hidden flex flex-col">
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
                                            topicId={topicId}
                                            allSteps={allSteps}
                                        />
                                    ) : selectedGateway ? (
                                        /* Gateway Configuration Panel */
                                        <div className="h-full overflow-y-auto p-6">
                                            <GatewayConfigPanel
                                                gateway={selectedGateway}
                                                config={getConfig(selectedGateway.id)}
                                                onUpdate={(newConfig) => saveGatewayConfig(selectedGateway.id, newConfig)}
                                            />
                                        </div>
                                    ) : (
                                        <div className="h-full flex flex-col items-center justify-center p-12 text-center">
                                            <div className="w-20 h-20 rounded-2xl bg-gradient-to-br from-slate-100 to-slate-50 flex items-center justify-center mb-6 shadow-inner">
                                                <MousePointer2 className="w-10 h-10 text-slate-300" />
                                            </div>
                                            <h3 className="text-xl font-semibold text-slate-800">Select an Element</h3>
                                            <p className="max-w-sm mt-3 text-slate-500 leading-relaxed">
                                                Click on a <span className="font-medium text-blue-600">workflow step</span> to configure assignments,
                                                or a <span className="font-medium text-purple-600">parallel gateway</span> to set up approval completion rules.
                                            </p>
                                        </div>
                                    )}
                                </div>
                            </div>
                        </TabsContent>
                    </Tabs>
                </div>
            </PageContainer >

            {/* Copy as New Topic Modal */}
            <CopyWorkflowModal
                isOpen={showCopyModal}
                onClose={() => setShowCopyModal(false)}
                onTopicCreated={handleTopicCreated}
                sourceTopic={topic}
                sourceWorkflowXml={bpmnXml}
            />
        </>
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
    escalationActions,
    topicId,
    allSteps
}) => {
    const [expandedSection, setExpandedSection] = useState('assignment');

    const updateConfig = (key, value) => {
        onConfigChange({ ...config, [key]: value });
    };

    // Get available target steps for branching (exclude current step)
    const availableTargetSteps = allSteps
        ?.filter(s => s.taskKey !== step.taskKey)
        .map(s => ({ id: s.taskKey, name: s.taskName || s.taskKey })) || [];

    return (
        <div className="h-full flex flex-col">
            {/* Header */}
            <div className="px-6 py-4 border-b bg-gradient-to-r from-slate-50 to-white">
                <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center shadow-lg shadow-blue-500/20">
                        <Zap className="w-5 h-5 text-white" />
                    </div>
                    <div>
                        <h2 className="text-lg font-semibold text-slate-900">{step.taskName}</h2>
                        <p className="text-xs text-slate-500">Configure step behavior & assignments</p>
                    </div>
                </div>
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

                {/* Branching Section */}
                <ConfigSection
                    title="Branching"
                    icon={<GitBranch className="w-4 h-4" />}
                    isExpanded={expandedSection === 'branching'}
                    onToggle={() => setExpandedSection(expandedSection === 'branching' ? null : 'branching')}
                    hasConfig={config.conditionConfig?.conditions?.length > 0}
                >
                    <div className="space-y-4">
                        <p className="text-xs text-gray-500">
                            Define conditions to route memos to different steps based on their data.
                        </p>
                        <ConditionBuilder
                            topicId={topicId}
                            conditions={config.conditionConfig?.conditions || []}
                            defaultTarget={config.conditionConfig?.defaultTarget || ''}
                            availableSteps={availableTargetSteps}
                            onChange={(conditions, defaultTarget) => updateConfig('conditionConfig', { conditions, defaultTarget })}
                            label="Route After This Step"
                        />
                        {(!config.conditionConfig?.conditions || config.conditionConfig.conditions.length === 0) && (
                            <div className="p-2 bg-purple-50 border border-purple-200 rounded text-xs text-purple-800">
                                💡 No conditions defined. Workflow will follow the default sequence.
                            </div>
                        )}
                    </div>
                </ConfigSection>
            </div>

            {/* Bottom Status */}
            <div className="px-6 py-3 border-t bg-slate-50 flex items-center gap-2">
                <div className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                <span className="text-xs text-slate-500">Changes save automatically when you deploy</span>
            </div>
        </div>
    );
};

/**
 * Collapsible config section with premium styling
 */
const ConfigSection = ({ title, icon, isExpanded, onToggle, hasConfig, children }) => (
    <div className="border-b border-slate-100 last:border-b-0">
        <button
            className={`w-full px-6 py-4 flex items-center justify-between transition-colors ${isExpanded ? 'bg-blue-50/50' : 'bg-white hover:bg-slate-50'
                }`}
            onClick={onToggle}
        >
            <div className="flex items-center space-x-3">
                <div className={`p-2 rounded-lg transition-colors ${hasConfig ? 'bg-blue-100 text-blue-600' : 'bg-slate-100 text-slate-400'
                    }`}>
                    {icon}
                </div>
                <span className={`font-medium ${isExpanded ? 'text-blue-900' : 'text-slate-700'
                    }`}>{title}</span>
                {hasConfig && (
                    <span className="px-2 py-0.5 text-xs font-medium rounded-full bg-emerald-100 text-emerald-700">
                        Configured
                    </span>
                )}
            </div>
            <ChevronRight className={`w-5 h-5 text-slate-400 transition-transform duration-200 ${isExpanded ? 'rotate-90' : ''
                }`} />
        </button>
        <div className={`overflow-hidden transition-all duration-200 ${isExpanded ? 'max-h-[500px] opacity-100' : 'max-h-0 opacity-0'
            }`}>
            <div className="px-6 py-5 bg-slate-50/50 border-t border-slate-100">
                {children}
            </div>
        </div>
    </div>
);

export default WorkflowDesignerPage;
