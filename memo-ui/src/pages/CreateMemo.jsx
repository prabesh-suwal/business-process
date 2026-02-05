import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { MemoApi, CasAdminApi, OrganizationApi } from '../lib/api';
import { PageContainer } from '../components/PageContainer';
import { Button } from '../components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import RichTextEditor from '../components/RichTextEditor';
import MemoAttachments from '../components/MemoAttachments';
import StepBuilder from '../components/StepBuilder';
import WorkflowPreview from '../components/WorkflowPreview';
import { Loader2, Save, Send, Workflow, FileText, Paperclip, Settings, Info } from 'lucide-react';
import { toast } from 'sonner';

export default function CreateMemo() {
    const navigate = useNavigate();
    const [activeTab, setActiveTab] = useState('content');
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);

    // Form State
    const [title, setTitle] = useState('');
    const [referenceNumber, setReferenceNumber] = useState('');
    const [priority, setPriority] = useState('NORMAL');
    const [content, setContent] = useState('');

    // Selection State
    const [categories, setCategories] = useState([]);
    const [topics, setTopics] = useState([]);
    const [selectedCategory, setSelectedCategory] = useState('');
    const [selectedTopic, setSelectedTopic] = useState('');

    // Created Memo
    const [memoId, setMemoId] = useState(null);

    // Workflow/Step Builder State
    const [selectedTopicDetails, setSelectedTopicDetails] = useState(null);
    const [workflowSteps, setWorkflowSteps] = useState([]);
    const [useCustomWorkflow, setUseCustomWorkflow] = useState(false);
    const [stepOverrides, setStepOverrides] = useState({}); // Inline step overrides for WorkflowPreview

    // Dropdown data for StepBuilder/AdvancedAssignmentTab
    const [roles, setRoles] = useState([]);
    const [groups, setGroups] = useState([]);
    const [departments, setDepartments] = useState([]);
    const [branches, setBranches] = useState([]);
    const [users, setUsers] = useState([]);
    // Geo data
    const [regions, setRegions] = useState([]);
    const [districts, setDistricts] = useState([]);
    const [states, setStates] = useState([]);
    const [slaDurations, setSlaDurations] = useState([
        { code: 'PT1H', label: '1 Hour' },
        { code: 'PT4H', label: '4 Hours' },
        { code: 'P1D', label: '1 Day' },
        { code: 'P2D', label: '2 Days' },
        { code: 'P3D', label: '3 Days' },
        { code: 'P5D', label: '5 Days' },
        { code: 'P1W', label: '1 Week' }
    ]);
    const [escalationActions, setEscalationActions] = useState([
        { code: 'NOTIFY_SUPERVISOR', label: 'Notify Supervisor' },
        { code: 'AUTO_ESCALATE', label: 'Auto-Escalate' },
        { code: 'SEND_REMINDER', label: 'Send Reminder' }
    ]);

    // Fetch Categories and dropdown data on load
    useEffect(() => {
        setLoading(true);
        Promise.all([
            MemoApi.getCategories(),
            CasAdminApi.getRoles().catch(() => []),
            CasAdminApi.getGroups().catch(() => []),
            CasAdminApi.getDepartments().catch(() => []),
            CasAdminApi.getUsers().catch(() => []),
            OrganizationApi.getAllBranches().catch(() => []),
            OrganizationApi.getLocationsByType('PROVINCE').catch(() => []),
            OrganizationApi.getLocationsByType('DISTRICT').catch(() => [])
        ]).then(([cats, rolesData, groupsData, deptsData, usersData, branchesData, provincesData, districtsData]) => {
            setCategories(cats);
            setRoles(rolesData);
            setGroups(groupsData);
            setDepartments(deptsData);
            setUsers(usersData);
            setBranches(branchesData);
            setRegions(provincesData); // Provinces as regions
            setDistricts(districtsData);
        }).catch(err => {
            console.error(err);
            toast.error("Failed to load initial data");
        }).finally(() => setLoading(false));
    }, []);

    // Preview assignment handler - resolves which users match the assignment rules
    const handlePreviewAssignment = async (config) => {
        return CasAdminApi.previewAssignment(config);
    };

    // Fetch Topics when Category changes
    useEffect(() => {
        if (selectedCategory) {
            MemoApi.getTopics(selectedCategory)
                .then(setTopics)
                .catch(err => {
                    console.error(err);
                    toast.error("Failed to load topics");
                });
        } else {
            setTopics([]);
        }
    }, [selectedCategory]);

    // Load Topic Details when Topic is selected (to check workflow & override permissions)
    useEffect(() => {
        if (selectedTopic) {
            MemoApi.getTopic(selectedTopic)
                .then(topicData => {
                    setSelectedTopicDetails(topicData);

                    // Check if topic has workflow configured
                    const hasWorkflow = topicData.workflowXml && topicData.workflowXml.trim().length > 0;

                    if (!hasWorkflow) {
                        // General topic - user creates their own workflow
                        setUseCustomWorkflow(true);
                        setWorkflowSteps([]);
                    } else {
                        // Topic has workflow - user can override if permitted
                        setUseCustomWorkflow(false);
                        // Pre-load steps from topic workflow (simplified for now)
                        // In full implementation, we'd extract steps from BPMN
                        setWorkflowSteps([]);
                    }
                })
                .catch(err => {
                    console.error(err);
                    setSelectedTopicDetails(null);
                });
        } else {
            setSelectedTopicDetails(null);
            setWorkflowSteps([]);
            setUseCustomWorkflow(false);
        }
    }, [selectedTopic]);

    const handleSaveDraft = async () => {
        if (!selectedTopic) {
            toast.error("Please select a topic first.");
            return;
        }
        if (!title.trim()) {
            toast.error("Please enter a title.");
            return;
        }

        setSaving(true);
        try {
            let currentMemoId = memoId;

            // 1. Create Draft if doesn't exist
            if (!currentMemoId) {
                const draft = await MemoApi.createDraft({
                    topicId: selectedTopic,
                    subject: title,
                    priority: priority
                });
                currentMemoId = draft.id;
                setMemoId(currentMemoId);
                toast.success("Draft created successfully");
            }

            // 2. Build update payload
            const updatePayload = {
                subject: title,
                priority: priority,
                content: content,
                referenceNumber: referenceNumber
            };

            // 3. Include workflow overrides if user customized
            if (useCustomWorkflow && workflowSteps.length > 0) {
                updatePayload.workflowOverrides = {
                    customWorkflow: true,
                    steps: workflowSteps.map(step => ({
                        id: step.id,
                        name: step.name,
                        // New rules-based format
                        rules: step.rules || [],
                        fallbackRoleId: step.fallbackRoleId || null,
                        completionMode: step.completionMode || 'ANY',
                        // Legacy format for backward compatibility
                        assignmentType: step.assignmentType,
                        roles: step.roles || [],
                        departments: step.departments || [],
                        users: step.users || [],
                        slaDuration: step.slaDuration || null,
                        escalationAction: step.escalationAction || null
                    }))
                };
            } else if (Object.keys(stepOverrides).length > 0) {
                // Include inline step overrides when using default workflow
                // Transform step overrides to include rules-based format
                const transformedOverrides = {};
                Object.entries(stepOverrides).forEach(([taskKey, override]) => {
                    transformedOverrides[taskKey] = {
                        ...override,
                        // Include rules if they exist
                        rules: override.rules || [],
                        fallbackRoleId: override.fallbackRoleId || null,
                        completionMode: override.completionMode || 'ANY'
                    };
                });
                updatePayload.workflowOverrides = {
                    customWorkflow: false,
                    stepOverrides: transformedOverrides
                };
            }

            // 4. Update memo
            await MemoApi.updateMemo(currentMemoId, updatePayload);

            toast.success("Memo saved");

        } catch (error) {
            console.error("Error saving memo:", error);
            toast.error("Failed to save memo");
        } finally {
            setSaving(false);
        }
    };

    const handleSubmit = async () => {
        if (!memoId) {
            // Try saving first
            await handleSaveDraft();
            if (!memoId) return; // Save failed
        }

        try {
            await MemoApi.submitMemo(memoId);
            toast.success("Memo submitted successfully");
            navigate('/memos'); // Redirect to memos list after submit
        } catch (error) {
            console.error(error);
            toast.error("Failed to submit memo");
        }
    };

    return (
        <PageContainer>
            <div className="mb-6">
                <h1 className="text-3xl font-bold tracking-tight">Create New Memo</h1>
                <p className="text-muted-foreground mt-2">Complete the tabbed sections below to submit your corporate memo.</p>
            </div>

            <div className="bg-background rounded-lg border shadow-sm">
                <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
                    <div className="border-b px-4 bg-muted/20">
                        <TabsList className="bg-transparent h-14 w-full justify-start space-x-6 p-0">
                            <TabsTrigger
                                value="content"
                                className="data-[state=active]:bg-transparent data-[state=active]:shadow-none data-[state=active]:border-b-2 data-[state=active]:border-primary data-[state=active]:text-primary rounded-none px-2 h-full font-semibold"
                            >
                                Content
                            </TabsTrigger>
                            <TabsTrigger
                                value="attachments"
                                className="data-[state=active]:bg-transparent data-[state=active]:shadow-none data-[state=active]:border-b-2 data-[state=active]:border-primary data-[state=active]:text-primary rounded-none px-2 h-full font-semibold"
                            >
                                Attachments
                            </TabsTrigger>
                            <TabsTrigger
                                value="workflow"
                                className="data-[state=active]:bg-transparent data-[state=active]:shadow-none data-[state=active]:border-b-2 data-[state=active]:border-primary data-[state=active]:text-primary rounded-none px-2 h-full font-semibold"
                            >
                                Workflow
                            </TabsTrigger>
                        </TabsList>
                    </div>

                    <div className="p-6">
                        <TabsContent value="content" className="space-y-6 mt-0">
                            {/* Title */}
                            <div>
                                <Label htmlFor="title" className="text-sm font-semibold mb-1.5 block">Memo Title</Label>
                                <Input
                                    id="title"
                                    placeholder="e.g., Credit Facility Revision - Enterprise Client Corp"
                                    className="h-12 text-lg"
                                    value={title}
                                    onChange={(e) => setTitle(e.target.value)}
                                />
                            </div>

                            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                {/* Category & Topic Selection (Added per requirement) */}
                                <div>
                                    <Label htmlFor="category" className="text-sm font-semibold mb-1.5 block">Category</Label>
                                    <select
                                        id="category"
                                        className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                                        value={selectedCategory}
                                        onChange={(e) => setSelectedCategory(e.target.value)}
                                        disabled={!!memoId} // Disable if already created? Maybe allow changing if draft?
                                    >
                                        <option value="">Select Category</option>
                                        {categories.map(cat => (
                                            <option key={cat.id} value={cat.id}>{cat.name}</option>
                                        ))}
                                    </select>
                                </div>
                                <div>
                                    <Label htmlFor="topic" className="text-sm font-semibold mb-1.5 block">Topic</Label>
                                    <select
                                        id="topic"
                                        className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                                        value={selectedTopic}
                                        onChange={(e) => setSelectedTopic(e.target.value)}
                                        disabled={!selectedCategory || !!memoId}
                                    >
                                        <option value="">Select Topic</option>
                                        {topics.map(topic => (
                                            <option key={topic.id} value={topic.id}>{topic.name}</option>
                                        ))}
                                    </select>
                                </div>

                                {/* Ref & Priority */}
                                <div>
                                    <Label htmlFor="ref" className="text-sm font-semibold mb-1.5 block">Reference Number</Label>
                                    <Input
                                        id="ref"
                                        placeholder="REF-2023-KB82"
                                        value={referenceNumber}
                                        onChange={(e) => setReferenceNumber(e.target.value)}
                                    />
                                </div>
                                <div>
                                    <Label htmlFor="priority" className="text-sm font-semibold mb-1.5 block">Priority Level</Label>
                                    <select
                                        id="priority"
                                        className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                                        value={priority}
                                        onChange={(e) => setPriority(e.target.value)}
                                    >
                                        <option value="NORMAL">Standard</option>
                                        <option value="HIGH">High</option>
                                        <option value="URGENT">Urgent</option>
                                    </select>
                                </div>
                            </div>

                            {/* Editor */}
                            <div>
                                <div className="flex justify-between items-center mb-1.5">
                                    <Label className="text-sm font-semibold">Executive Summary</Label>
                                    <span className="text-xs text-muted-foreground">Formal document editor</span>
                                </div>
                                <RichTextEditor
                                    content={content}
                                    onChange={setContent}
                                    outputFormat="json"
                                />
                            </div>
                        </TabsContent>

                        <TabsContent value="attachments" className="mt-0">
                            {memoId ? (
                                <MemoAttachments
                                    memoId={memoId}
                                    isEditable={true}
                                    onUpload={() => { }} // Refresh?
                                />
                            ) : (
                                <div className="text-center py-12 text-muted-foreground bg-muted/10 rounded-lg border border-dashed">
                                    <p>Please save the draft first to add attachments.</p>
                                    <Button variant="outline" onClick={handleSaveDraft} className="mt-4">Save Draft</Button>
                                </div>
                            )}
                        </TabsContent>

                        <TabsContent value="workflow" className="mt-0">
                            {!selectedTopic ? (
                                <div className="text-center py-12 text-muted-foreground bg-muted/10 rounded-lg border border-dashed">
                                    <Workflow className="w-12 h-12 mx-auto mb-4 text-slate-300" />
                                    <p className="font-medium text-slate-600">Select a topic first</p>
                                    <p className="text-sm mt-1">Choose a category and topic to configure the approval workflow</p>
                                </div>
                            ) : !selectedTopicDetails ? (
                                <div className="flex items-center justify-center py-12">
                                    <Loader2 className="w-6 h-6 animate-spin text-slate-400" />
                                </div>
                            ) : (
                                <div className="space-y-6">
                                    {/* Workflow Info Banner */}
                                    {selectedTopicDetails.workflowXml ? (
                                        // Topic has configured workflow
                                        <div className="bg-blue-50 border border-blue-200 rounded-xl p-4">
                                            <div className="flex items-start gap-3">
                                                <div className="p-2 rounded-lg bg-blue-100">
                                                    <Info className="w-5 h-5 text-blue-600" />
                                                </div>
                                                <div className="flex-1">
                                                    <h4 className="font-semibold text-blue-900">Pre-configured Workflow</h4>
                                                    <p className="text-sm text-blue-700 mt-1">
                                                        This topic has a default approval workflow configured by admin.
                                                    </p>

                                                    {/* Check if any overrides allowed */}
                                                    {(selectedTopicDetails.overridePermissions?.allowOverrideAssignments ||
                                                        selectedTopicDetails.overridePermissions?.allowOverrideSLA ||
                                                        selectedTopicDetails.overridePermissions?.allowOverrideEscalation) && (
                                                            <div className="mt-3">
                                                                <label className="flex items-center gap-2 cursor-pointer">
                                                                    <input
                                                                        type="checkbox"
                                                                        checked={useCustomWorkflow}
                                                                        onChange={(e) => setUseCustomWorkflow(e.target.checked)}
                                                                        className="w-4 h-4 rounded border-blue-300 text-blue-600 focus:ring-blue-500"
                                                                    />
                                                                    <span className="text-sm font-medium text-blue-900">
                                                                        Customize workflow for this memo
                                                                    </span>
                                                                </label>
                                                            </div>
                                                        )}
                                                </div>
                                            </div>
                                        </div>
                                    ) : (
                                        // General topic - no workflow configured
                                        <div className="bg-amber-50 border border-amber-200 rounded-xl p-4">
                                            <div className="flex items-start gap-3">
                                                <div className="p-2 rounded-lg bg-amber-100">
                                                    <Settings className="w-5 h-5 text-amber-600" />
                                                </div>
                                                <div>
                                                    <h4 className="font-semibold text-amber-900">General Memo Topic</h4>
                                                    <p className="text-sm text-amber-700 mt-1">
                                                        This topic doesn't have a pre-configured workflow.
                                                        Please define the approval steps below.
                                                    </p>
                                                </div>
                                            </div>
                                        </div>
                                    )}

                                    {/* Step Builder - Show when customizing or general topic */}
                                    {(useCustomWorkflow || !selectedTopicDetails.workflowXml) && (
                                        <div className="bg-white rounded-xl border border-slate-200 p-6">
                                            <StepBuilder
                                                steps={workflowSteps}
                                                onChange={setWorkflowSteps}
                                                roles={roles}
                                                groups={groups}
                                                departments={departments}
                                                branches={branches}
                                                users={users}
                                                regions={regions}
                                                districts={districts}
                                                states={states}
                                                slaDurations={slaDurations}
                                                escalationActions={escalationActions}
                                                allowedOverrides={selectedTopicDetails.workflowXml ? selectedTopicDetails.overridePermissions : null}
                                                onPreviewAssignment={handlePreviewAssignment}
                                            />
                                        </div>
                                    )}

                                    {/* Workflow Preview - Show when using default workflow (not customizing) */}
                                    {!useCustomWorkflow && selectedTopicDetails.workflowXml && (
                                        <WorkflowPreview
                                            workflowXml={selectedTopicDetails.workflowXml}
                                            processTemplateId={selectedTopicDetails.workflowTemplateId}
                                            topicName={selectedTopicDetails.name}
                                            overridePermissions={selectedTopicDetails.overridePermissions}
                                            stepOverrides={stepOverrides}
                                            onStepOverrideChange={setStepOverrides}
                                            roles={roles}
                                            groups={groups}
                                            departments={departments}
                                            branches={branches}
                                            users={users}
                                            regions={regions}
                                            districts={districts}
                                            states={states}
                                            slaDurations={slaDurations}
                                            escalationActions={escalationActions}
                                            onPreviewAssignment={handlePreviewAssignment}
                                        />
                                    )}
                                </div>
                            )}
                        </TabsContent>
                    </div>

                    {/* Footer Actions */}
                    <div className="border-t p-6 bg-muted/10 flex justify-end gap-3 rounded-b-lg">
                        <Button variant="outline" size="lg" onClick={handleSaveDraft} disabled={saving}>
                            {saving ? <Loader2 className="w-4 h-4 mr-2 animate-spin" /> : <Save className="w-4 h-4 mr-2" />}
                            Save Draft
                        </Button>
                        <Button size="lg" onClick={handleSubmit} disabled={saving || !memoId} className="bg-primary hover:bg-primary/90">
                            Submit Memo
                            <Send className="w-4 h-4 ml-2" />
                        </Button>
                    </div>
                </Tabs>
            </div>
        </PageContainer>
    );
}
