import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { MemoApi, WorkflowConfigApi, CasAdminApi, OrganizationApi } from '../lib/api';
import { Card, CardHeader, CardTitle, CardContent, CardDescription } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';
import { Badge } from '../components/ui/badge';
import { ArrowLeft, Settings, Users, Clock, AlertTriangle, CheckCircle, FileText, Save, Eye, GitBranch } from 'lucide-react';
import SearchableMultiSelect from '../components/ui/SearchableMultiSelect';
import ConditionBuilder from '../components/ConditionBuilder';
import AdvancedAssignmentTab from '../components/AdvancedAssignmentTab';
import TopicDefaultAssignee from '../components/TopicDefaultAssignee';

/**
 * WorkflowConfigPage - Step-centric workflow configuration for business users.
 * Allows admins to configure each workflow step without technical knowledge.
 */
const WorkflowConfigPage = () => {
    const { topicId } = useParams();
    const navigate = useNavigate();

    const [topic, setTopic] = useState(null);
    const [steps, setSteps] = useState([]);
    const [selectedStep, setSelectedStep] = useState(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);

    // Dynamic dropdown data from CAS
    const [roles, setRoles] = useState([]);
    const [groups, setGroups] = useState([]);
    const [departments, setDepartments] = useState([]);
    const [scopes, setScopes] = useState([]);
    const [assignmentTypes, setAssignmentTypes] = useState([]);
    const [slaDurations, setSlaDurations] = useState([]);
    const [escalationActions, setEscalationActions] = useState([]);

    // Organization/geo data for advanced assignment
    const [branches, setBranches] = useState([]);
    const [regions, setRegions] = useState([]);
    const [districts, setDistricts] = useState([]);
    const [states, setStates] = useState([]);
    const [users, setUsers] = useState([]);

    useEffect(() => {
        loadData();
    }, [topicId]);

    /**
     * Parse BPMN XML and extract user tasks
     */
    const extractStepsFromBpmn = (bpmnXml) => {
        if (!bpmnXml) return [];

        try {
            const parser = new DOMParser();
            const xmlDoc = parser.parseFromString(bpmnXml, 'text/xml');

            // Get all userTask elements
            const userTasks = xmlDoc.getElementsByTagName('bpmn:userTask');
            const userTasks2 = xmlDoc.getElementsByTagName('userTask');

            const allTasks = [...userTasks, ...userTasks2];

            const steps = [];
            for (let i = 0; i < allTasks.length; i++) {
                const task = allTasks[i];
                const id = task.getAttribute('id');
                const name = task.getAttribute('name') || id;

                steps.push({
                    taskKey: id,
                    taskName: name,
                    stepOrder: i + 1,
                    assignmentConfig: {},
                    formConfig: {},
                    slaConfig: {},
                    escalationConfig: {}
                });
            }

            return steps;
        } catch (error) {
            console.error('Error parsing BPMN:', error);
            return [];
        }
    };

    const loadData = async () => {
        try {
            setLoading(true);

            // Load topic details (includes workflowXml)
            const topicData = await MemoApi.getTopic(topicId);
            setTopic(topicData);

            // First try to load saved step configs
            let stepConfigs = [];
            try {
                stepConfigs = await WorkflowConfigApi.getStepConfigs(topicId);
            } catch (e) {
                console.log('No saved step configs, will extract from BPMN');
            }

            // If no saved configs, extract from BPMN
            if (stepConfigs.length === 0 && topicData.workflowXml) {
                stepConfigs = extractStepsFromBpmn(topicData.workflowXml);
            }

            setSteps(stepConfigs);

            // Load dynamic dropdown data from CAS - with fallbacks
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
                // Use defaults if CAS not available
                setAssignmentTypes(getDefaultAssignmentTypes());
                setScopes(getDefaultScopes());
                setSlaDurations(getDefaultSlaDurations());
                setEscalationActions(getDefaultEscalationActions());
            }

            // Load organization/geo data for advanced assignment
            try {
                const [branchesData, regionsData, usersData] = await Promise.all([
                    OrganizationApi.getAllBranches().catch(() => []),
                    OrganizationApi.getLocationsByType('PROVINCE').catch(() => []),
                    CasAdminApi.getUsers().catch(() => [])
                ]);
                setBranches(branchesData);
                setRegions(regionsData);
                setUsers(usersData);
            } catch (e) {
                console.log('Organization data not available:', e);
            }

        } catch (error) {
            console.error('Error loading workflow config:', error);
        } finally {
            setLoading(false);
        }
    };

    // Default data when CAS is not available
    const getDefaultAssignmentTypes = () => [
        { id: 'INITIATOR', code: 'INITIATOR', label: 'Initiator' },
        { id: 'ROLE', code: 'ROLE', label: 'Role-based' },
        { id: 'DEPARTMENT', code: 'DEPARTMENT', label: 'Department' },
        { id: 'GROUP', code: 'GROUP', label: 'Group / Committee' },
        { id: 'MANAGER', code: 'MANAGER', label: 'Reporting Manager' },
    ];

    const getDefaultScopes = () => [
        { id: 'INITIATOR_BRANCH', code: 'INITIATOR_BRANCH', label: 'Same Branch as Initiator' },
        { id: 'SAME_DEPARTMENT', code: 'SAME_DEPARTMENT', label: 'Same Department' },
        { id: 'REGION', code: 'REGION', label: 'Region Level' },
        { id: 'HEAD_OFFICE', code: 'HEAD_OFFICE', label: 'Head Office' },
    ];

    const getDefaultSlaDurations = () => [
        { id: 'PT4H', code: 'PT4H', label: '4 Hours' },
        { id: 'PT8H', code: 'PT8H', label: '8 Hours' },
        { id: 'P1D', code: 'P1D', label: '1 Working Day' },
        { id: 'P2D', code: 'P2D', label: '2 Working Days' },
        { id: 'P3D', code: 'P3D', label: '3 Working Days' },
    ];

    const getDefaultEscalationActions = () => [
        { id: 'NOTIFY', code: 'NOTIFY', label: 'Notify Only' },
        { id: 'REASSIGN', code: 'REASSIGN', label: 'Reassign Task' },
        { id: 'ESCALATE_TO_MANAGER', code: 'ESCALATE_TO_MANAGER', label: 'Escalate to Manager' },
    ];

    const handleStepSelect = (step) => {
        setSelectedStep(step);
    };

    const handleSaveStep = async (stepConfig) => {
        try {
            setSaving(true);
            await WorkflowConfigApi.saveStepConfig(topicId, selectedStep.taskKey, stepConfig);
            await loadData(); // Refresh
            // Show success message
        } catch (error) {
            console.error('Error saving step config:', error);
        } finally {
            setSaving(false);
        }
    };

    if (loading) {
        return (
            <div className="flex items-center justify-center h-64">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
            </div>
        );
    }

    return (
        <div className="p-6 space-y-6 bg-gray-50 min-h-screen">
            {/* Header */}
            <div className="flex items-center justify-between">
                <div className="flex items-center space-x-4">
                    <Button variant="ghost" onClick={() => navigate(-1)}>
                        <ArrowLeft className="w-4 h-4 mr-2" />
                        Back
                    </Button>
                    <div>
                        <h1 className="text-2xl font-bold text-gray-900">Workflow Configuration</h1>
                        <p className="text-gray-600">{topic?.name || 'Memo Topic'}</p>
                    </div>
                </div>
            </div>

            {/* Topic-level Default Assignee Configuration */}
            <TopicDefaultAssignee
                topicId={topicId}
                initialConfig={topic?.defaultAssigneeConfig || {}}
                roles={roles}
                groups={groups}
                departments={departments}
                branches={branches}
                regions={regions}
                districts={districts}
                states={states}
                users={users}
                onPreview={async (rules) => {
                    try {
                        const result = await WorkflowConfigApi.previewAssignment(topicId, rules);
                        return result;
                    } catch (error) {
                        console.error('Preview failed:', error);
                        throw error;
                    }
                }}
            />

            <div className="grid grid-cols-12 gap-6 mt-6">
                {/* Steps List - Left Panel */}
                <div className="col-span-4">
                    <Card className="shadow-lg">
                        <CardHeader className="bg-gradient-to-r from-blue-600 to-blue-700 text-white rounded-t-lg">
                            <CardTitle className="text-lg">Workflow Steps</CardTitle>
                            <CardDescription className="text-blue-100">
                                Configure each step of the approval process
                            </CardDescription>
                        </CardHeader>
                        <CardContent className="p-0">
                            <div className="divide-y">
                                {steps.length === 0 ? (
                                    <div className="p-6 text-center text-gray-500">
                                        <Settings className="w-12 h-12 mx-auto mb-3 text-gray-400" />
                                        <p>No workflow steps found.</p>
                                        <p className="text-sm mt-2">
                                            Design a workflow in the BPMN designer first,
                                            then come back here to configure each step.
                                        </p>
                                    </div>
                                ) : (
                                    steps.map((step, index) => (
                                        <StepListItem
                                            key={step.taskKey}
                                            step={step}
                                            index={index + 1}
                                            isSelected={selectedStep?.taskKey === step.taskKey}
                                            onClick={() => handleStepSelect(step)}
                                        />
                                    ))
                                )}
                            </div>
                        </CardContent>
                    </Card>
                </div>

                {/* Step Configuration - Right Panel */}
                <div className="col-span-8">
                    {selectedStep ? (
                        <StepConfigPanel
                            step={selectedStep}
                            roles={roles}
                            groups={groups}
                            departments={departments}
                            branches={branches}
                            regions={regions}
                            districts={districts}
                            states={states}
                            users={users}
                            scopes={scopes}
                            assignmentTypes={assignmentTypes}
                            slaDurations={slaDurations}
                            escalationActions={escalationActions}
                            onSave={handleSaveStep}
                            saving={saving}
                            topicId={topicId}
                            allSteps={steps}
                        />
                    ) : (
                        <Card className="shadow-lg h-full flex items-center justify-center">
                            <div className="text-center p-12">
                                <Settings className="w-16 h-16 mx-auto mb-4 text-gray-300" />
                                <h3 className="text-xl font-medium text-gray-600">Select a Step</h3>
                                <p className="text-gray-500 mt-2">
                                    Choose a workflow step from the left panel to configure it.
                                </p>
                            </div>
                        </Card>
                    )}
                </div>
            </div>
        </div>
    );
};

/**
 * Step List Item - Clean visual representation of each workflow step
 */
const StepListItem = ({ step, index, isSelected, onClick }) => {
    const hasAssignment = step.assignmentConfig && Object.keys(step.assignmentConfig).length > 0;
    const hasSla = step.slaConfig && Object.keys(step.slaConfig).length > 0;
    const hasEscalation = step.escalationConfig && Object.keys(step.escalationConfig).length > 0;

    return (
        <div
            className={`p-4 cursor-pointer transition-all hover:bg-blue-50 ${isSelected ? 'bg-blue-50 border-l-4 border-blue-600' : ''
                }`}
            onClick={onClick}
        >
            <div className="flex items-start justify-between">
                <div className="flex items-center space-x-3">
                    <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium ${isSelected ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-600'
                        }`}>
                        {index}
                    </div>
                    <div>
                        <h4 className="font-medium text-gray-900">
                            {step.taskName || step.taskKey}
                        </h4>
                        <p className="text-sm text-gray-500">{step.taskKey}</p>
                    </div>
                </div>
                <div className="flex space-x-1">
                    {hasAssignment && (
                        <Badge variant="outline" className="text-green-600 border-green-200 bg-green-50">
                            <Users className="w-3 h-3 mr-1" />
                        </Badge>
                    )}
                    {hasSla && (
                        <Badge variant="outline" className="text-blue-600 border-blue-200 bg-blue-50">
                            <Clock className="w-3 h-3 mr-1" />
                        </Badge>
                    )}
                    {hasEscalation && (
                        <Badge variant="outline" className="text-orange-600 border-orange-200 bg-orange-50">
                            <AlertTriangle className="w-3 h-3 mr-1" />
                        </Badge>
                    )}
                </div>
            </div>
        </div>
    );
};

/**
 * Step Configuration Panel - Tabbed interface for step settings
 */
const StepConfigPanel = ({
    step,
    roles,
    groups,
    departments,
    branches,
    regions,
    districts,
    states,
    users,
    scopes,
    assignmentTypes,
    slaDurations,
    escalationActions,
    onSave,
    saving,
    topicId,
    allSteps
}) => {
    const [activeTab, setActiveTab] = useState('assignment');
    const [config, setConfig] = useState({
        taskName: step.taskName || '',
        assignmentConfig: step.assignmentConfig || {},
        formConfig: step.formConfig || {},
        slaConfig: step.slaConfig || {},
        escalationConfig: step.escalationConfig || {},
        conditionConfig: step.conditionConfig || { conditions: [], defaultTarget: '' }
    });

    useEffect(() => {
        setConfig({
            taskName: step.taskName || '',
            assignmentConfig: step.assignmentConfig || {},
            formConfig: step.formConfig || {},
            slaConfig: step.slaConfig || {},
            escalationConfig: step.escalationConfig || {},
            conditionConfig: step.conditionConfig || { conditions: [], defaultTarget: '' }
        });
    }, [step]);

    const handleSave = () => {
        onSave(config);
    };

    return (
        <Card className="shadow-lg">
            <CardHeader className="border-b bg-white">
                <div className="flex items-center justify-between">
                    <div>
                        <CardTitle className="text-xl">{config.taskName || step.taskKey}</CardTitle>
                        <CardDescription>Configure how this step works</CardDescription>
                    </div>
                    <Button onClick={handleSave} disabled={saving} className="bg-blue-600 hover:bg-blue-700">
                        {saving ? (
                            <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2" />
                        ) : (
                            <Save className="w-4 h-4 mr-2" />
                        )}
                        Save Changes
                    </Button>
                </div>
            </CardHeader>
            <CardContent className="p-0">
                <Tabs value={activeTab} onValueChange={setActiveTab}>
                    <TabsList className="w-full justify-start border-b rounded-none bg-gray-50 p-0 h-auto">
                        <TabsTrigger
                            value="assignment"
                            className="rounded-none border-b-2 border-transparent data-[state=active]:border-blue-600 data-[state=active]:bg-white py-3 px-6"
                        >
                            <Users className="w-4 h-4 mr-2" />
                            Who Handles This?
                        </TabsTrigger>
                        <TabsTrigger
                            value="form"
                            className="rounded-none border-b-2 border-transparent data-[state=active]:border-blue-600 data-[state=active]:bg-white py-3 px-6"
                        >
                            <FileText className="w-4 h-4 mr-2" />
                            Form Settings
                        </TabsTrigger>
                        <TabsTrigger
                            value="sla"
                            className="rounded-none border-b-2 border-transparent data-[state=active]:border-blue-600 data-[state=active]:bg-white py-3 px-6"
                        >
                            <Clock className="w-4 h-4 mr-2" />
                            Time Limits
                        </TabsTrigger>
                        <TabsTrigger
                            value="escalation"
                            className="rounded-none border-b-2 border-transparent data-[state=active]:border-blue-600 data-[state=active]:bg-white py-3 px-6"
                        >
                            <AlertTriangle className="w-4 h-4 mr-2" />
                            If Delayed
                        </TabsTrigger>
                        <TabsTrigger
                            value="branching"
                            className="rounded-none border-b-2 border-transparent data-[state=active]:border-purple-600 data-[state=active]:bg-white py-3 px-6"
                        >
                            <GitBranch className="w-4 h-4 mr-2" />
                            Branching
                        </TabsTrigger>
                    </TabsList>

                    <div className="p-6">
                        <TabsContent value="assignment" className="mt-0">
                            <AdvancedAssignmentTab
                                config={config.assignmentConfig}
                                onChange={(c) => setConfig({ ...config, assignmentConfig: c })}
                                roles={roles}
                                groups={groups}
                                departments={departments}
                                branches={branches}
                                regions={regions}
                                districts={districts}
                                states={states}
                                users={users}
                                topicId={topicId}
                                onPreview={async (rules) => {
                                    // Preview API call
                                    try {
                                        const result = await WorkflowConfigApi.previewAssignment(topicId, rules);
                                        return result;
                                    } catch (error) {
                                        console.error('Preview failed:', error);
                                        throw error;
                                    }
                                }}
                            />
                        </TabsContent>

                        <TabsContent value="form" className="mt-0">
                            <FormTab
                                config={config.formConfig}
                                onChange={(c) => setConfig({ ...config, formConfig: c })}
                            />
                        </TabsContent>

                        <TabsContent value="sla" className="mt-0">
                            <SlaTab
                                config={config.slaConfig}
                                onChange={(c) => setConfig({ ...config, slaConfig: c })}
                                slaDurations={slaDurations}
                            />
                        </TabsContent>

                        <TabsContent value="escalation" className="mt-0">
                            <EscalationTab
                                config={config.escalationConfig}
                                onChange={(c) => setConfig({ ...config, escalationConfig: c })}
                                escalationActions={escalationActions}
                                roles={roles}
                                slaDurations={slaDurations}
                            />
                        </TabsContent>

                        <TabsContent value="branching" className="mt-0">
                            <BranchingTab
                                config={config.conditionConfig}
                                onChange={(c) => setConfig({ ...config, conditionConfig: c })}
                                topicId={topicId}
                                allSteps={allSteps}
                                currentStepKey={step.taskKey}
                            />
                        </TabsContent>
                    </div>
                </Tabs>
            </CardContent>
        </Card>
    );
};

/**
 * Assignment Tab - "Who handles this step?"
 */
const AssignmentTab = ({ config, onChange, assignmentTypes, roles, groups, departments, scopes }) => {
    const [showPreview, setShowPreview] = useState(false);
    const [previewUsers, setPreviewUsers] = useState([]);

    const handleTypeChange = (type) => {
        onChange({ ...config, type });
    };

    const handlePreview = async () => {
        try {
            const result = await CasAdminApi.previewAssignment(config);
            setPreviewUsers(result);
            setShowPreview(true);
        } catch (error) {
            console.error('Preview failed:', error);
        }
    };

    return (
        <div className="space-y-6">
            {/* Assignment Type Selection */}
            <div>
                <label className="block text-sm font-medium text-gray-700 mb-3">
                    Assign this step to:
                </label>
                <div className="grid grid-cols-2 gap-3">
                    {assignmentTypes.map((type) => (
                        <div
                            key={type.id}
                            className={`p-4 border-2 rounded-lg cursor-pointer transition-all ${config.type === type.code
                                ? 'border-blue-600 bg-blue-50'
                                : 'border-gray-200 hover:border-gray-300'
                                }`}
                            onClick={() => handleTypeChange(type.code)}
                        >
                            <div className="font-medium text-gray-900">{type.label}</div>
                        </div>
                    ))}
                </div>
            </div>

            {/* Role-based configuration */}
            {config.type === 'ROLE' && (
                <div className="space-y-4 p-4 bg-gray-50 rounded-lg">
                    <div>
                        <SearchableMultiSelect
                            options={roles.map(r => ({ value: r.code, label: r.label }))}
                            selected={config.roles || []}
                            onChange={(selected) => onChange({ ...config, roles: selected })}
                            placeholder="Select roles..."
                            label="Select Roles"
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                            Location Scope
                        </label>
                        <select
                            className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                            value={config.scope || ''}
                            onChange={(e) => onChange({ ...config, scope: e.target.value })}
                        >
                            <option value="">Any location...</option>
                            {scopes.map((scope) => (
                                <option key={scope.id} value={scope.code}>{scope.label}</option>
                            ))}
                        </select>
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                            If no one available, assign to:
                        </label>
                        <select
                            className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                            value={config.fallbackRole || ''}
                            onChange={(e) => onChange({ ...config, fallbackRole: e.target.value })}
                        >
                            <option value="">No fallback</option>
                            {roles.map((role) => (
                                <option key={role.id} value={role.code}>{role.label}</option>
                            ))}
                        </select>
                    </div>
                </div>
            )}

            {/* Group/Committee configuration */}
            {config.type === 'GROUP' && (
                <div className="space-y-4 p-4 bg-gray-50 rounded-lg">
                    <div>
                        <SearchableMultiSelect
                            options={groups.map(g => ({ value: g.code, label: g.label }))}
                            selected={config.groupCodes || []}
                            onChange={(selected) => onChange({ ...config, groupCodes: selected })}
                            placeholder="Select groups..."
                            label="Select Committee / Groups"
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                            Approval Required From
                        </label>
                        <div className="space-y-2">
                            <label className="flex items-center space-x-3 p-3 border rounded-lg cursor-pointer hover:bg-white">
                                <input
                                    type="radio"
                                    name="completionRule"
                                    value="ANY_ONE"
                                    checked={config.completionRule === 'ANY_ONE'}
                                    onChange={() => onChange({ ...config, completionRule: 'ANY_ONE' })}
                                    className="w-4 h-4 text-blue-600"
                                />
                                <span>Any one member</span>
                            </label>
                            <label className="flex items-center space-x-3 p-3 border rounded-lg cursor-pointer hover:bg-white">
                                <input
                                    type="radio"
                                    name="completionRule"
                                    value="MAJORITY"
                                    checked={config.completionRule === 'MAJORITY'}
                                    onChange={() => onChange({ ...config, completionRule: 'MAJORITY' })}
                                    className="w-4 h-4 text-blue-600"
                                />
                                <span>Majority of members</span>
                            </label>
                            <label className="flex items-center space-x-3 p-3 border rounded-lg cursor-pointer hover:bg-white">
                                <input
                                    type="radio"
                                    name="completionRule"
                                    value="ALL"
                                    checked={config.completionRule === 'ALL'}
                                    onChange={() => onChange({ ...config, completionRule: 'ALL' })}
                                    className="w-4 h-4 text-blue-600"
                                />
                                <span>All members</span>
                            </label>
                        </div>
                    </div>
                </div>
            )}

            {/* Preview Button */}
            <div className="flex justify-end pt-4 border-t">
                <Button variant="outline" onClick={handlePreview}>
                    <Eye className="w-4 h-4 mr-2" />
                    Preview Who Will Receive This
                </Button>
            </div>

            {/* Preview Results */}
            {showPreview && (
                <div className="p-4 bg-green-50 border border-green-200 rounded-lg">
                    <h4 className="font-medium text-green-800 mb-2">People who will receive this step:</h4>
                    {previewUsers.length > 0 ? (
                        <ul className="space-y-1">
                            {previewUsers.map((user) => (
                                <li key={user.id} className="flex items-center text-green-700">
                                    <CheckCircle className="w-4 h-4 mr-2" />
                                    {user.name} ({user.role} - {user.branch})
                                </li>
                            ))}
                        </ul>
                    ) : (
                        <p className="text-green-700">No users match this configuration yet.</p>
                    )}
                </div>
            )}
        </div>
    );
};

/**
 * Form Tab - Form field configuration
 */
const FormTab = ({ config, onChange }) => {
    return (
        <div className="space-y-6">
            <div className="p-6 bg-gray-50 rounded-lg border-2 border-dashed border-gray-300">
                <div className="text-center">
                    <FileText className="w-12 h-12 mx-auto text-gray-400 mb-3" />
                    <h3 className="text-lg font-medium text-gray-700">Form Configuration</h3>
                    <p className="text-gray-500 mt-2">
                        Configure which form fields can be edited at this step.
                    </p>
                    <p className="text-sm text-gray-400 mt-4">
                        (Form field selection will be available when connected to form-service)
                    </p>
                </div>
            </div>
        </div>
    );
};

/**
 * SLA Tab - Time limit configuration
 */
const SlaTab = ({ config, onChange, slaDurations }) => {
    return (
        <div className="space-y-6">
            <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg">
                <p className="text-blue-800">
                    <strong>Time Limits</strong> help ensure tasks are completed promptly.
                    Set how long someone has to complete this step.
                </p>
            </div>

            <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                    Maximum time to complete this step
                </label>
                <select
                    className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    value={config.duration || ''}
                    onChange={(e) => onChange({ ...config, duration: e.target.value })}
                >
                    <option value="">No time limit</option>
                    {slaDurations.map((sla) => (
                        <option key={sla.id} value={sla.code}>{sla.label}</option>
                    ))}
                </select>
            </div>

            <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                    Send warning reminder before deadline
                </label>
                <select
                    className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    value={config.warningBefore || ''}
                    onChange={(e) => onChange({ ...config, warningBefore: e.target.value })}
                >
                    <option value="">Don't send warning</option>
                    <option value="PT1H">1 hour before</option>
                    <option value="PT2H">2 hours before</option>
                    <option value="PT4H">4 hours before</option>
                    <option value="PT8H">8 hours before</option>
                </select>
            </div>
        </div>
    );
};

/**
 * Escalation Tab - "What happens if delayed?"
 */
const EscalationTab = ({ config, onChange, escalationActions, roles, slaDurations }) => {
    const escalations = config.escalations || [];

    const addEscalation = () => {
        onChange({
            ...config,
            escalations: [...escalations, { level: escalations.length + 1, after: 'PT2H', action: 'NOTIFY' }]
        });
    };

    const updateEscalation = (index, field, value) => {
        const updated = [...escalations];
        updated[index] = { ...updated[index], [field]: value };
        onChange({ ...config, escalations: updated });
    };

    const removeEscalation = (index) => {
        onChange({
            ...config,
            escalations: escalations.filter((_, i) => i !== index)
        });
    };

    return (
        <div className="space-y-6">
            <div className="p-4 bg-orange-50 border border-orange-200 rounded-lg">
                <p className="text-orange-800">
                    <strong>Escalation Rules</strong> define what happens if someone doesn't complete their task on time.
                    You can set multiple levels of escalation.
                </p>
            </div>

            {escalations.length === 0 ? (
                <div className="p-6 bg-gray-50 rounded-lg text-center">
                    <AlertTriangle className="w-12 h-12 mx-auto text-gray-400 mb-3" />
                    <p className="text-gray-600">No escalation rules configured.</p>
                    <p className="text-gray-500 text-sm mt-1">Add a rule to ensure tasks don't get stuck.</p>
                </div>
            ) : (
                <div className="space-y-4">
                    {escalations.map((esc, index) => (
                        <div key={index} className="p-4 bg-white border rounded-lg">
                            <div className="flex items-center justify-between mb-4">
                                <h4 className="font-medium text-gray-900">Level {esc.level}</h4>
                                <Button
                                    variant="ghost"
                                    size="sm"
                                    className="text-red-600 hover:text-red-700"
                                    onClick={() => removeEscalation(index)}
                                >
                                    Remove
                                </Button>
                            </div>

                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-2">
                                        Trigger after deadline by
                                    </label>
                                    <select
                                        className="w-full p-2 border border-gray-300 rounded-lg"
                                        value={esc.after || ''}
                                        onChange={(e) => updateEscalation(index, 'after', e.target.value)}
                                    >
                                        <option value="PT1H">1 hour</option>
                                        <option value="PT2H">2 hours</option>
                                        <option value="PT4H">4 hours</option>
                                        <option value="PT8H">8 hours</option>
                                        <option value="P1D">1 day</option>
                                    </select>
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-2">
                                        Action to take
                                    </label>
                                    <select
                                        className="w-full p-2 border border-gray-300 rounded-lg"
                                        value={esc.action || ''}
                                        onChange={(e) => updateEscalation(index, 'action', e.target.value)}
                                    >
                                        {escalationActions.map((action) => (
                                            <option key={action.id} value={action.code}>{action.label}</option>
                                        ))}
                                    </select>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}

            <Button variant="outline" onClick={addEscalation} className="w-full">
                + Add Escalation Level
            </Button>
        </div>
    );
};

/**
 * Branching Tab - "Where does this step route to?"
 * Uses ConditionBuilder to define conditional routing based on memo/form data.
 */
const BranchingTab = ({ config, onChange, topicId, allSteps, currentStepKey }) => {
    // Filter out current step from available targets
    const availableTargetSteps = allSteps
        .filter(s => s.taskKey !== currentStepKey)
        .map(s => ({ id: s.taskKey, name: s.taskName || s.taskKey }));

    const handleConditionsChange = (conditions, defaultTarget) => {
        onChange({
            ...config,
            conditions,
            defaultTarget
        });
    };

    return (
        <div className="space-y-4">
            <div className="bg-purple-50 border border-purple-200 rounded-lg p-4 mb-4">
                <div className="flex items-start gap-3">
                    <GitBranch className="w-5 h-5 text-purple-600 mt-0.5" />
                    <div>
                        <h4 className="font-medium text-purple-900">Conditional Routing</h4>
                        <p className="text-sm text-purple-700">
                            Define conditions to route memos to different steps based on their data.
                            If no conditions match, the default step will be used.
                        </p>
                    </div>
                </div>
            </div>

            <ConditionBuilder
                topicId={topicId}
                conditions={config?.conditions || []}
                defaultTarget={config?.defaultTarget || ''}
                availableSteps={availableTargetSteps}
                onChange={handleConditionsChange}
                label="Route After This Step"
            />

            {config?.conditions?.length === 0 && (
                <div className="text-center py-4 text-gray-500 text-sm">
                    <p>No conditions defined. The workflow will follow the default sequence.</p>
                    <p className="mt-1">Add conditions above to create branching logic.</p>
                </div>
            )}
        </div>
    );
};

export default WorkflowConfigPage;

