import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { MemoApi, TaskApi } from '../lib/api';
import RichTextEditor from '../components/RichTextEditor';
import { Button } from '../components/ui/button';
import { Card, CardContent, CardHeader } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';
import {
    Save, Send, ArrowLeft, FileIcon, Download, Loader2, Paperclip,
    MessageSquare, CheckCircle, XCircle, CornerUpLeft, User, Clock,
    AlertTriangle, History, FileText, ChevronDown, Mail, Printer
} from 'lucide-react';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from "../components/ui/dropdown-menu"

import { toast } from 'sonner';
import Form from '@rjsf/core';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "../components/ui/dialog";
import validator from '@rjsf/validator-ajv8';
import ExecutionHistory from '../components/ExecutionHistory';
import { Label } from '../components/ui/label';
import { PageContainer } from '../components/PageContainer';
import MemoComments from '../components/MemoComments';
import MemoAttachments from '../components/MemoAttachments';

export default function MemoEditor() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [memo, setMemo] = useState(null);
    const [topic, setTopic] = useState(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [submitDialogOpen, setSubmitDialogOpen] = useState(false);
    const [attachments, setAttachments] = useState([]);

    // Action Dialog State
    const [actionDialogOpen, setActionDialogOpen] = useState(false);
    const [actionType, setActionType] = useState(null);
    const [comment, setComment] = useState('');
    const [activeTask, setActiveTask] = useState(null);
    const [outcomeConfig, setOutcomeConfig] = useState(null);
    const [actionMeta, setActionMeta] = useState(null);

    // Send-Back Dialog State
    const [sendBackDialogOpen, setSendBackDialogOpen] = useState(false);
    const [returnPoints, setReturnPoints] = useState([]);
    const [selectedReturnPoint, setSelectedReturnPoint] = useState(null);
    const [sendBackReason, setSendBackReason] = useState('');
    const [loadingReturnPoints, setLoadingReturnPoints] = useState(false);

    // Delegate Dialog State
    const [delegateDialogOpen, setDelegateDialogOpen] = useState(false);
    const [delegateCandidates, setDelegateCandidates] = useState([]);
    const [selectedDelegateUser, setSelectedDelegateUser] = useState(null);
    const [delegateComment, setDelegateComment] = useState('');
    const [loadingDelegateCandidates, setLoadingDelegateCandidates] = useState(false);
    const [delegateSearch, setDelegateSearch] = useState('');

    useEffect(() => {
        loadMemo();
        loadAttachments();
    }, [id]);

    const loadMemo = async () => {
        try {
            const data = await MemoApi.getMemo(id);
            setMemo(data);

            if (data.status !== 'DRAFT') {
                try {
                    console.log(`[MemoEditor] Fetching tasks for memo ${id}, status: ${data.status}`);
                    const tasks = await TaskApi.getTasksForMemo(id);
                    console.log(`[MemoEditor] getTasksForMemo returned:`, tasks);
                    if (tasks && tasks.length > 0) {
                        console.log(`[MemoEditor] Setting activeTask:`, tasks[0]);
                        setActiveTask(tasks[0]);
                        // Use inline outcomeConfig from the task DTO (no separate API call needed)
                        if (tasks[0].outcomeConfig) {
                            console.log(`[MemoEditor] Inline outcome config:`, tasks[0].outcomeConfig);
                            setOutcomeConfig(tasks[0].outcomeConfig);
                        } else {
                            // Fallback: fetch outcome config separately (backward compat)
                            try {
                                const taskId = tasks[0].workflowTaskId || tasks[0].id;
                                const config = await TaskApi.getOutcomeConfig(taskId);
                                if (config) {
                                    console.log(`[MemoEditor] Fetched outcome config:`, config);
                                    setOutcomeConfig(config);
                                }
                            } catch (e) {
                                console.debug('[MemoEditor] No outcome config:', e);
                            }
                        }
                    } else {
                        console.log(`[MemoEditor] No actionable tasks found for this user`);
                    }
                } catch (e) {
                    console.error("[MemoEditor] getTasksForMemo failed:", e);
                }
            }

            if (data.topicId) {
                try {
                    const topicData = await MemoApi.getTopic(data.topicId);
                    setTopic(topicData);
                } catch (e) {
                    console.warn("Failed to load topic details", e);
                }
            }
        } catch (error) {
            console.error(error);
            toast.error("Failed to load memo");
        } finally {
            setLoading(false);
        }
    };

    const loadAttachments = async () => {
        try {
            const data = await MemoApi.getAttachments(id);
            setAttachments(data);
        } catch (error) {
            console.error("Failed to load attachments", error);
        }
    };

    const handleSave = async () => {
        setSaving(true);
        try {
            await MemoApi.updateMemo(id, {
                subject: memo.subject,
                priority: memo.priority,
                content: memo.content,
                formData: memo.formData
            });
            toast.success("Memo saved successfully");
        } catch (error) {
            console.error(error);
            toast.error("Failed to save memo");
        } finally {
            setSaving(false);
        }
    };

    const handleSubmit = () => {
        setSubmitDialogOpen(true);
    };

    const confirmSubmit = async () => {
        setSubmitDialogOpen(false);
        setSubmitting(true);
        try {
            const result = await MemoApi.submitMemo(id);
            setMemo(result);
            toast.success("Memo submitted for approval!");
            navigate('/tasks');
        } catch (error) {
            console.error(error);
            toast.error(error.response?.data?.message || "Failed to submit memo");
        } finally {
            setSubmitting(false);
        }
    };

    const handlePrint = () => {
        window.print();
    };

    const handleEmail = () => {
        toast.info("Email feature coming soon!");
    };

    const handleFormChange = ({ formData }) => {
        setMemo({ ...memo, formData });
    };

    const openActionDialog = (type, meta = null) => {
        // Only BACK_TO_STEP opens the step-picker dialog (user chooses which step)
        // Legacy: 'sendback'/'SENDBACK' also open it for backward compat
        if (meta?.actionType === 'BACK_TO_STEP' ||
            type === 'sendback' || type === 'SENDBACK') {
            openSendBackDialog();
            return;
        }
        // DELEGATE opens the user-picker dialog
        if (meta?.actionType === 'DELEGATE') {
            openDelegateDialog();
            return;
        }
        // All other action types (APPROVE, REJECT, ESCALATE, SEND_BACK, BACK_TO_INITIATOR)
        // open the comment dialog — SEND_BACK auto-determines the previous step
        setActionType(type);
        setActionMeta(meta); // { actionType, label, style, sets, requiresComment } for dynamic options
        setComment('');
        setActionDialogOpen(true);
    };

    const openSendBackDialog = async () => {
        if (!activeTask) return;
        setLoadingReturnPoints(true);
        setSendBackReason('');
        setSelectedReturnPoint(null);
        setSendBackDialogOpen(true);
        try {
            const taskId = activeTask.workflowTaskId || activeTask.id;
            const points = await TaskApi.getReturnPoints(taskId);
            console.log('[MemoEditor] Return points:', points);
            setReturnPoints(points || []);
        } catch (e) {
            console.error('[MemoEditor] Failed to load return points:', e);
            toast.error('Failed to load return points');
            setReturnPoints([]);
        } finally {
            setLoadingReturnPoints(false);
        }
    };

    const handleSendBack = async () => {
        if (!activeTask || !selectedReturnPoint) return;
        setSubmitting(true);
        try {
            const taskId = activeTask.workflowTaskId || activeTask.id;
            // Unified flow: route through completeTask with BACK_TO_STEP actionType
            await TaskApi.completeTask(
                taskId,
                'BACK_TO_STEP',           // actionType (stable identifier)
                sendBackReason,            // comment
                {
                    decision: 'SENT_BACK',
                    targetStep: selectedReturnPoint.taskKey  // user-selected step
                }
            );
            toast.success('Task sent back successfully');
            setSendBackDialogOpen(false);
            loadMemo();
            setActiveTask(null);
            setOutcomeConfig(null);
        } catch (error) {
            console.error(error);
            toast.error('Failed to send back task');
        } finally {
            setSubmitting(false);
        }
    };

    const openDelegateDialog = async () => {
        if (!activeTask) return;
        setLoadingDelegateCandidates(true);
        setDelegateComment('');
        setSelectedDelegateUser(null);
        setDelegateSearch('');
        setDelegateDialogOpen(true);
        try {
            const taskId = activeTask.workflowTaskId || activeTask.id;
            const candidates = await TaskApi.getDelegateCandidates(taskId);
            const groups = candidates.candidateGroups || [];
            let users = [];
            if (groups.length > 0) {
                // Resolve users from candidate groups via CAS
                users = await TaskApi.getUsersByRoles(groups);
            }
            // Also add any explicit candidate users
            if (candidates.candidateUsers?.length > 0) {
                const existingIds = new Set(users.map(u => u.id));
                candidates.candidateUsers.forEach(userId => {
                    if (!existingIds.has(userId)) {
                        users.push({ id: userId, code: userId, label: userId });
                    }
                });
            }
            setDelegateCandidates(users);
        } catch (e) {
            console.error('[MemoEditor] Failed to load delegate candidates:', e);
            toast.error('Failed to load delegate candidates');
            setDelegateCandidates([]);
        } finally {
            setLoadingDelegateCandidates(false);
        }
    };

    const handleDelegate = async () => {
        if (!activeTask || !selectedDelegateUser) return;
        setSubmitting(true);
        try {
            const taskId = activeTask.workflowTaskId || activeTask.id;
            await TaskApi.completeTask(
                taskId,
                'DELEGATE',
                delegateComment,
                {
                    delegateTo: selectedDelegateUser.id
                }
            );
            toast.success(`Task delegated to ${selectedDelegateUser.label}`);
            setDelegateDialogOpen(false);
            loadMemo();
            setActiveTask(null);
            setOutcomeConfig(null);
        } catch (error) {
            console.error(error);
            toast.error('Failed to delegate task');
        } finally {
            setSubmitting(false);
        }
    };

    const handleAction = async () => {
        if (!activeTask) return;
        setSubmitting(true);
        try {
            // Use actionType (stable identifier) not label (user-editable)
            const resolvedActionType = actionMeta?.actionType || actionType?.toUpperCase() || 'APPROVE';
            const variables = { decision: resolvedActionType };

            // Enterprise pattern: inject all variables from option.sets map
            if (actionMeta?.sets) {
                Object.entries(actionMeta.sets).forEach(([key, value]) => {
                    variables[key] = value;
                });
                console.log('[MemoEditor] Injecting sets:', actionMeta.sets);
            }

            await TaskApi.completeTask(
                activeTask.workflowTaskId || activeTask.id,
                resolvedActionType,
                comment,
                variables
            );

            toast.success("Action processed successfully");
            setActionDialogOpen(false);
            loadMemo();
            setActiveTask(null);
            setOutcomeConfig(null);
            setActionMeta(null);
        } catch (error) {
            console.error(error);
            toast.error("Failed to process action");
        } finally {
            setSubmitting(false);
        }
    };

    // Style helpers for dynamic outcome buttons
    const getOutcomeStyle = (style) => {
        switch (style) {
            case 'success': return 'text-green-600 focus:text-green-700 focus:bg-green-50';
            case 'danger': return 'text-red-600 focus:text-red-700 focus:bg-red-50';
            case 'warning': return 'text-orange-600 focus:text-orange-700 focus:bg-orange-50';
            case 'info': return 'text-blue-600 focus:text-blue-700 focus:bg-blue-50';
            default: return 'text-slate-600 focus:text-slate-700 focus:bg-slate-50';
        }
    };

    const getOutcomeIcon = (style) => {
        switch (style) {
            case 'success': return CheckCircle;
            case 'danger': return XCircle;
            case 'warning': return CornerUpLeft;
            default: return CheckCircle;
        }
    };

    const getOutcomeButtonStyle = (style) => {
        switch (style) {
            case 'success': return 'bg-green-600 hover:bg-green-700 text-white';
            case 'danger': return 'bg-red-600 hover:bg-red-700 text-white';
            case 'warning': return 'bg-orange-600 hover:bg-orange-700 text-white';
            case 'info': return 'bg-blue-600 hover:bg-blue-700 text-white';
            default: return 'bg-slate-600 hover:bg-slate-700 text-white';
        }
    };

    const getStatusColor = (status) => {
        switch (status) {
            case 'APPROVED': return 'bg-green-100 text-green-700 border-green-200';
            case 'REJECTED': return 'bg-red-100 text-red-700 border-red-200';
            case 'SUBMITTED':
            case 'IN_PROGRESS': return 'bg-amber-100 text-amber-700 border-amber-200';
            default: return 'bg-slate-100 text-slate-700 border-slate-200';
        }
    };

    const getTimeAgo = (dateStr) => {
        if (!dateStr) return '';
        const date = new Date(dateStr);
        const now = new Date();
        const diffInSeconds = Math.floor((now - date) / 1000);

        if (diffInSeconds < 60) return 'just now';
        if (diffInSeconds < 3600) return `${Math.floor(diffInSeconds / 60)} minutes ago`;
        if (diffInSeconds < 86400) return `${Math.floor(diffInSeconds / 3600)} hours ago`;
        return `${Math.floor(diffInSeconds / 86400)} days ago`;
    };

    if (loading || !memo) return <div className="flex h-screen items-center justify-center text-slate-400 font-medium">Loading Document...</div>;

    const isEditable = memo.status === 'DRAFT' || memo.status === 'SENT_BACK';

    return (
        <PageContainer>
            {/* Header Section */}
            <div className="space-y-4">
                {/* Breadcrumbs */}
                <div className="flex items-center text-sm text-slate-500 gap-2">
                    <span className="hover:text-slate-900 cursor-pointer" onClick={() => navigate('/tasks')}>Memos</span>
                    <span>/</span>
                    <span>{topic?.name || 'Internal Memo'}</span>
                    <span>/</span>
                    <span className="text-brand-blue font-medium">{memo.memoNumber}</span>
                </div>

                {/* Title & Metadata */}
                <div>
                    <div className="flex items-start justify-between">
                        <h1 className="text-3xl font-bold text-slate-900 tracking-tight">{memo.subject}</h1>
                        <Badge variant="outline" className={`px-3 py-1 text-xs font-semibold uppercase tracking-wider ${getStatusColor(memo.status)}`}>
                            {activeTask ? 'Pending Action' : memo.status}
                        </Badge>
                    </div>

                    <div className="flex items-center gap-6 mt-3 text-sm text-slate-500">
                        <div className="flex items-center gap-2">
                            <div className="w-2 h-2 rounded-full bg-slate-300"></div>
                            <span>Memo ID: <span className="font-mono">{memo.memoNumber}</span></span>
                        </div>
                        <div className="flex items-center gap-2">
                            <User className="w-4 h-4" />
                            <span>Author: <span className="text-slate-700 font-medium">{memo.createdBy || 'Unknown'}</span></span>
                        </div>
                        <div className="flex items-center gap-2">
                            <AlertTriangle className={`w-4 h-4 ${memo.priority === 'High' ? 'text-red-500' : 'text-slate-400'}`} />
                            <span>Priority: <span className={`font-medium ${memo.priority === 'High' ? 'text-red-600' : 'text-slate-700'}`}>{memo.priority}</span></span>
                        </div>
                        <div className="flex items-center gap-2">
                            <Clock className="w-4 h-4" />
                            <span>Last updated {getTimeAgo(memo.updatedAt || memo.createdAt)}</span>
                        </div>
                    </div>
                </div>
            </div>

            {/* Main Tabs Card */}
            <Card className="border shadow-sm bg-white mt-6">
                <Tabs defaultValue="content" className="w-full">
                    <CardHeader className="border-b bg-slate-50/50 px-6 py-3 flex flex-row items-center justify-between space-y-0">
                        {/* Tabs List */}
                        <TabsList className="bg-transparent p-0 gap-6 h-auto">
                            <TabsTrigger
                                value="content"
                                className="data-[state=active]:bg-transparent data-[state=active]:shadow-none data-[state=active]:border-b-2 data-[state=active]:border-brand-blue data-[state=active]:text-brand-blue rounded-none px-0 py-2 bg-transparent text-slate-500 shadow-none border-b-2 border-transparent transition-all"
                            >
                                <FileText className="w-4 h-4 mr-2" />
                                Content
                            </TabsTrigger>
                            <TabsTrigger
                                value="comments"
                                className="data-[state=active]:bg-transparent data-[state=active]:shadow-none data-[state=active]:border-b-2 data-[state=active]:border-brand-blue data-[state=active]:text-brand-blue rounded-none px-0 py-2 bg-transparent text-slate-500 shadow-none border-b-2 border-transparent transition-all"
                            >
                                <MessageSquare className="w-4 h-4 mr-2" />
                                Comments
                            </TabsTrigger>
                            <TabsTrigger
                                value="audit"
                                className="data-[state=active]:bg-transparent data-[state=active]:shadow-none data-[state=active]:border-b-2 data-[state=active]:border-brand-blue data-[state=active]:text-brand-blue rounded-none px-0 py-2 bg-transparent text-slate-500 shadow-none border-b-2 border-transparent transition-all"
                            >
                                <History className="w-4 h-4 mr-2" />
                                Execution History
                            </TabsTrigger>
                            <TabsTrigger
                                value="attachments"
                                className="data-[state=active]:bg-transparent data-[state=active]:shadow-none data-[state=active]:border-b-2 data-[state=active]:border-brand-blue data-[state=active]:text-brand-blue rounded-none px-0 py-2 bg-transparent text-slate-500 shadow-none border-b-2 border-transparent transition-all"
                            >
                                <Paperclip className="w-4 h-4 mr-2" />
                                Attachments ({attachments.length})
                            </TabsTrigger>
                        </TabsList>

                        {/* Action Buttons */}
                        <div className="flex items-center gap-2">
                            {/* Draft Actions */}
                            {isEditable && (
                                <>
                                    <Button variant="outline" size="sm" onClick={handleSave} disabled={saving} className="bg-white hover:bg-slate-50 text-brand-blue border-brand-blue/30">
                                        {saving ? <Loader2 className="mr-2 h-3.5 w-3.5 animate-spin" /> : <Save className="mr-2 h-3.5 w-3.5" />}
                                        Save Draft
                                    </Button>
                                    <Button size="sm" onClick={handleSubmit} disabled={submitting} className="bg-brand-blue hover:bg-brand-blue-hover text-white">
                                        {submitting ? <Loader2 className="mr-2 h-3.5 w-3.5 animate-spin" /> : <Send className="mr-2 h-3.5 w-3.5" />}
                                        Submit
                                    </Button>
                                </>
                            )}

                            {/* Approver Actions - Dropdown */}
                            {!isEditable && (
                                <DropdownMenu>
                                    <DropdownMenuTrigger asChild>
                                        <Button className="bg-brand-blue hover:bg-brand-blue-hover text-white">
                                            Actions <ChevronDown className="ml-2 h-4 w-4" />
                                        </Button>
                                    </DropdownMenuTrigger>
                                    <DropdownMenuContent align="end" className="w-56">
                                        {activeTask && (
                                            <>
                                                {outcomeConfig?.options?.length > 0 ? (
                                                    /* Dynamic outcome buttons from config (enterprise sets pattern) */
                                                    outcomeConfig.options.map((option, idx) => {
                                                        const Icon = getOutcomeIcon(option.style);
                                                        return (
                                                            <DropdownMenuItem
                                                                key={idx}
                                                                onClick={() => openActionDialog(option.label, option)}
                                                                className={`${getOutcomeStyle(option.style)} cursor-pointer`}
                                                            >
                                                                <Icon className="mr-2 h-4 w-4" />
                                                                <span>{option.label}</span>
                                                            </DropdownMenuItem>
                                                        );
                                                    })
                                                ) : (
                                                    /* Default hardcoded buttons */
                                                    <>
                                                        <DropdownMenuItem onClick={() => openActionDialog('approve')} className="text-green-600 focus:text-green-700 focus:bg-green-50 cursor-pointer">
                                                            <CheckCircle className="mr-2 h-4 w-4" />
                                                            <span>Approve Memo</span>
                                                        </DropdownMenuItem>
                                                        <DropdownMenuItem onClick={() => openActionDialog('sendback')} className="text-orange-600 focus:text-orange-700 focus:bg-orange-50 cursor-pointer">
                                                            <CornerUpLeft className="mr-2 h-4 w-4" />
                                                            <span>Request Revision</span>
                                                        </DropdownMenuItem>
                                                        <DropdownMenuItem onClick={() => openActionDialog('reject')} className="text-red-600 focus:text-red-700 focus:bg-red-50 cursor-pointer">
                                                            <XCircle className="mr-2 h-4 w-4" />
                                                            <span>Reject Memo</span>
                                                        </DropdownMenuItem>
                                                    </>
                                                )}
                                                <DropdownMenuSeparator />
                                            </>
                                        )}
                                        <DropdownMenuItem onClick={handleEmail} className="cursor-pointer">
                                            <Mail className="mr-2 h-4 w-4 text-slate-500" />
                                            <span>Email URL</span>
                                        </DropdownMenuItem>
                                        <DropdownMenuItem onClick={handlePrint} className="cursor-pointer">
                                            <Printer className="mr-2 h-4 w-4 text-slate-500" />
                                            <span>Print Memo</span>
                                        </DropdownMenuItem>
                                    </DropdownMenuContent>
                                </DropdownMenu>
                            )}
                        </div>
                    </CardHeader>

                    <CardContent className="p-8 min-h-[500px]">

                        {/* Content Tab */}
                        <TabsContent value="content" className="mt-0 space-y-8 animate-in fade-in duration-300">
                            {/* Executive Summary / Subject Section */}
                            <div>
                                <h2 className="text-xl font-bold text-slate-800 mb-4">Executive Summary</h2>
                                {isEditable ? (
                                    <div className="space-y-2">
                                        <label className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Subject</label>
                                        <input
                                            value={memo.subject}
                                            onChange={(e) => setMemo({ ...memo, subject: e.target.value })}
                                            className="w-full text-lg p-2 border rounded-md focus:border-brand-blue focus:ring-1 focus:ring-brand-blue outline-none transition-all"
                                            placeholder="Enter memo subject..."
                                        />
                                    </div>
                                ) : (
                                    <p className="text-slate-600 leading-relaxed text-lg pt-1">{memo.subject}</p>
                                )}
                            </div>

                            {/* Rich Text Body */}
                            <div>
                                <h2 className="text-xl font-bold text-slate-800 mb-4">Detailed Analysis</h2>
                                <RichTextEditor
                                    content={memo.content}
                                    onChange={(content) => setMemo({ ...memo, content })}
                                    editable={isEditable}
                                    outputFormat="json"
                                    className="prose max-w-none prose-slate prose-headings:font-bold prose-h3:text-lg prose-p:text-slate-600 prose-img:rounded-md"
                                />
                            </div>

                            {/* Dynamic Forms */}
                            {topic?.formSchema && (
                                <div className="pt-6 border-t border-slate-100">
                                    <h2 className="text-xl font-bold text-slate-800 mb-4">Structured Data</h2>
                                    <div className={`p-6 bg-slate-50 rounded-lg border border-slate-100 ${!isEditable ? 'pointer-events-none opacity-90' : ''}`}>
                                        <Form
                                            schema={topic.formSchema}
                                            formData={memo.formData || {}}
                                            onChange={handleFormChange}
                                            validator={validator}
                                            disabled={!isEditable}
                                            uiSchema={{ "ui:submitButtonOptions": { norender: true } }}
                                            className="rjsf-clean"
                                        />
                                    </div>
                                </div>
                            )}

                            {/* Confidential Footer */}
                            <div className="mt-12 p-4 bg-blue-50 border border-blue-100 rounded-lg text-blue-800 text-sm italic text-center">
                                Confidential Information: Do not distribute outside of the Corporate Lending and Risk Committee and Risk Committee members.
                            </div>
                        </TabsContent>

                        {/* Comments Tab */}
                        <TabsContent value="comments" className="mt-0 animate-in fade-in duration-300">
                            <MemoComments memoId={id} />
                        </TabsContent>

                        {/* Execution History Tab */}
                        <TabsContent value="audit" className="mt-0 animate-in fade-in duration-300">
                            <ExecutionHistory memoId={id} />
                        </TabsContent>

                        {/* Attachments Tab */}
                        <TabsContent value="attachments" className="mt-0 animate-in fade-in duration-300">
                            <MemoAttachments
                                memoId={id}
                                attachments={attachments}
                                onUpload={loadAttachments}
                                isEditable={isEditable}
                            />
                        </TabsContent>

                    </CardContent>
                </Tabs>
            </Card>

            {/* Submit Confirmation Dialog */}
            <Dialog open={submitDialogOpen} onOpenChange={setSubmitDialogOpen}>
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>Confirm Submission</DialogTitle>
                        <DialogDescription>
                            Are you sure you want to submit this memo? It will be routed to the workflow engine.
                        </DialogDescription>
                    </DialogHeader>
                    <DialogFooter>
                        <Button variant="outline" onClick={() => setSubmitDialogOpen(false)}>Cancel</Button>
                        <Button onClick={confirmSubmit} disabled={submitting} className="bg-brand-blue hover:bg-brand-blue-hover text-white">
                            {submitting ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
                            Confirm Submit
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            {/* Workflow Action Dialog */}
            <Dialog open={actionDialogOpen} onOpenChange={setActionDialogOpen}>
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle className="capitalize">{actionMeta?.label || actionType} Memo</DialogTitle>
                        <DialogDescription>
                            {actionMeta?.confirmationMessage
                                ? actionMeta.confirmationMessage
                                : 'Please provide a comment for this action.'}
                        </DialogDescription>
                    </DialogHeader>
                    <div className="py-4">
                        <Label>
                            Comment
                            {(actionMeta?.requiresComment || actionType === 'reject') && (
                                <span className="text-red-500 ml-1">* Required</span>
                            )}
                        </Label>
                        <textarea
                            className="w-full mt-2 p-2 border rounded-md text-sm min-h-[100px] focus:ring-2 focus:ring-brand-blue focus:border-brand-blue outline-none"
                            placeholder="Type your comment here..."
                            value={comment}
                            onChange={(e) => setComment(e.target.value)}
                        />
                    </div>
                    <DialogFooter>
                        <Button variant="outline" onClick={() => setActionDialogOpen(false)}>Cancel</Button>
                        <Button
                            onClick={handleAction}
                            disabled={
                                submitting ||
                                ((actionMeta?.requiresComment || actionType === 'reject') && !comment.trim())
                            }
                            className={actionMeta ? getOutcomeButtonStyle(actionMeta.style || 'default')
                                : actionType === 'approve' ? 'bg-green-600 hover:bg-green-700 text-white' : actionType === 'reject' ? 'bg-red-600 hover:bg-red-700 text-white' : 'bg-orange-600 hover:bg-orange-700 text-white'}
                        >
                            {submitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                            Confirm {actionMeta?.label || (actionType === 'sendback' ? 'Revision Request' : actionType)}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            {/* Send-Back Dialog with Return Points */}
            <Dialog open={sendBackDialogOpen} onOpenChange={setSendBackDialogOpen}>
                <DialogContent className="sm:max-w-lg">
                    <DialogHeader>
                        <DialogTitle>Send Back for Revision</DialogTitle>
                        <DialogDescription>
                            Select which step to return this memo to.
                        </DialogDescription>
                    </DialogHeader>
                    <div className="py-4 space-y-4">
                        {/* Return Points List */}
                        <div>
                            <Label className="mb-2 block">Return to step:</Label>
                            {loadingReturnPoints ? (
                                <div className="flex items-center justify-center py-6 text-slate-500">
                                    <Loader2 className="mr-2 h-5 w-5 animate-spin" />
                                    <span>Loading return points...</span>
                                </div>
                            ) : returnPoints.length === 0 ? (
                                <div className="text-center py-6 text-slate-400 text-sm">
                                    No previous steps available to return to.
                                </div>
                            ) : (
                                <div className="space-y-2 max-h-48 overflow-y-auto">
                                    {returnPoints.map((point, idx) => (
                                        <label
                                            key={idx}
                                            className={`flex items-center p-3 border rounded-lg cursor-pointer transition-all ${selectedReturnPoint?.taskKey === point.taskKey
                                                ? 'border-orange-500 bg-orange-50 ring-1 ring-orange-300'
                                                : 'border-slate-200 hover:border-slate-300 hover:bg-slate-50'
                                                }`}
                                        >
                                            <input
                                                type="radio"
                                                name="returnPoint"
                                                checked={selectedReturnPoint?.taskKey === point.taskKey}
                                                onChange={() => setSelectedReturnPoint(point)}
                                                className="mr-3 text-orange-600 focus:ring-orange-500"
                                            />
                                            <div>
                                                <div className="font-medium text-sm text-slate-800">
                                                    {point.taskName || point.taskKey}
                                                </div>
                                                {point.completedAt && (
                                                    <div className="text-xs text-slate-400 mt-0.5">
                                                        Completed {new Date(point.completedAt).toLocaleDateString()}
                                                    </div>
                                                )}
                                            </div>
                                        </label>
                                    ))}
                                </div>
                            )}
                        </div>

                        {/* Reason */}
                        <div>
                            <Label>Reason <span className="text-red-500 ml-1">* Required</span></Label>
                            <textarea
                                className="w-full mt-2 p-2 border rounded-md text-sm min-h-[80px] focus:ring-2 focus:ring-orange-500 focus:border-orange-500 outline-none"
                                placeholder="Explain why this memo needs to be sent back..."
                                value={sendBackReason}
                                onChange={(e) => setSendBackReason(e.target.value)}
                            />
                        </div>
                    </div>
                    <DialogFooter>
                        <Button variant="outline" onClick={() => setSendBackDialogOpen(false)}>Cancel</Button>
                        <Button
                            onClick={handleSendBack}
                            disabled={submitting || !selectedReturnPoint || !sendBackReason.trim()}
                            className="bg-orange-600 hover:bg-orange-700 text-white"
                        >
                            {submitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                            Send Back
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            {/* ==================== DELEGATE DIALOG ==================== */}
            <Dialog open={delegateDialogOpen} onOpenChange={setDelegateDialogOpen}>
                <DialogContent className="max-w-md">
                    <DialogHeader>
                        <DialogTitle>Delegate Task</DialogTitle>
                        <DialogDescription>
                            Select a user to delegate this task to.
                        </DialogDescription>
                    </DialogHeader>
                    <div className="py-4 space-y-4">
                        {/* Search */}
                        <div>
                            <input
                                type="text"
                                className="w-full p-2 border rounded-md text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
                                placeholder="Search users..."
                                value={delegateSearch}
                                onChange={(e) => setDelegateSearch(e.target.value)}
                            />
                        </div>

                        {/* User List */}
                        <div>
                            <Label className="mb-2 block">Delegate to:</Label>
                            {loadingDelegateCandidates ? (
                                <div className="flex items-center justify-center py-6 text-slate-500">
                                    <Loader2 className="mr-2 h-5 w-5 animate-spin" />
                                    <span>Loading eligible users...</span>
                                </div>
                            ) : delegateCandidates.length === 0 ? (
                                <div className="text-center py-6 text-slate-400 text-sm">
                                    No eligible users found for this task.
                                </div>
                            ) : (
                                <div className="space-y-2 max-h-56 overflow-y-auto">
                                    {delegateCandidates
                                        .filter(u => {
                                            if (!delegateSearch.trim()) return true;
                                            const q = delegateSearch.toLowerCase();
                                            return (u.label || '').toLowerCase().includes(q)
                                                || (u.code || '').toLowerCase().includes(q);
                                        })
                                        .map((user) => (
                                            <label
                                                key={user.id}
                                                className={`flex items-center p-3 border rounded-lg cursor-pointer transition-all ${selectedDelegateUser?.id === user.id
                                                    ? 'border-blue-500 bg-blue-50 ring-1 ring-blue-300'
                                                    : 'border-slate-200 hover:border-slate-300 hover:bg-slate-50'
                                                    }`}
                                            >
                                                <input
                                                    type="radio"
                                                    name="delegateUser"
                                                    checked={selectedDelegateUser?.id === user.id}
                                                    onChange={() => setSelectedDelegateUser(user)}
                                                    className="mr-3 text-blue-600 focus:ring-blue-500"
                                                />
                                                <div>
                                                    <div className="font-medium text-sm text-slate-800">
                                                        {user.label}
                                                    </div>
                                                    <div className="text-xs text-slate-400">
                                                        {user.code}
                                                    </div>
                                                </div>
                                            </label>
                                        ))}
                                </div>
                            )}
                        </div>

                        {/* Comment */}
                        <div>
                            <Label>Comment <span className="text-slate-400 text-xs">(optional)</span></Label>
                            <textarea
                                className="w-full mt-2 p-2 border rounded-md text-sm min-h-[60px] focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
                                placeholder="Add a note for the delegate..."
                                value={delegateComment}
                                onChange={(e) => setDelegateComment(e.target.value)}
                            />
                        </div>
                    </div>
                    <DialogFooter>
                        <Button variant="outline" onClick={() => setDelegateDialogOpen(false)}>Cancel</Button>
                        <Button
                            onClick={handleDelegate}
                            disabled={submitting || !selectedDelegateUser}
                            className="bg-blue-600 hover:bg-blue-700 text-white"
                        >
                            {submitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                            Delegate
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </PageContainer>
    );
}
