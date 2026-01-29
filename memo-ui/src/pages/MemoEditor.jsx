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
import AuditHistoryPanel from '../components/AuditHistoryPanel';
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
                    const tasks = await TaskApi.getTasksForMemo(id);
                    if (tasks && tasks.length > 0) {
                        setActiveTask(tasks[0]);
                    }
                } catch (e) {
                    console.log("No active tasks or failed to check", e);
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

    const openActionDialog = (type) => {
        setActionType(type);
        setComment('');
        setActionDialogOpen(true);
    };

    const handleAction = async () => {
        if (!activeTask) return;
        setSubmitting(true);
        try {
            const decisionMap = {
                'approve': 'APPROVE',
                'reject': 'REJECT',
                'sendback': 'SEND_BACK'
            };
            const decision = decisionMap[actionType] || 'APPROVE';

            await TaskApi.completeTask(
                activeTask.workflowTaskId || activeTask.id, // Prefer workflowTaskId if available
                decision,
                comment,
                { decision }
            );

            toast.success("Action processed successfully");
            setActionDialogOpen(false);
            loadMemo();
            setActiveTask(null);
        } catch (error) {
            console.error(error);
            toast.error("Failed to process action");
        } finally {
            setSubmitting(false);
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
                                Audit Trail
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

                        {/* Audit Trail Tab */}
                        <TabsContent value="audit" className="mt-0 animate-in fade-in duration-300">
                            {memo.processInstanceId ? (
                                <AuditHistoryPanel processInstanceId={memo.processInstanceId} className="border-0 shadow-none px-0" />
                            ) : (
                                <div className="text-center py-12 text-slate-400 italic">No audit history available (Local Draft)</div>
                            )}
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
                        <DialogTitle className="capitalize">{actionType} Memo</DialogTitle>
                        <DialogDescription>
                            Please provide a comment for this action.
                        </DialogDescription>
                    </DialogHeader>
                    <div className="py-4">
                        <Label>Comment {actionType === 'reject' && <span className="text-red-500">*</span>}</Label>
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
                            disabled={submitting || (actionType === 'reject' && !comment)}
                            className={actionType === 'approve' ? 'bg-green-600 hover:bg-green-700 text-white' : actionType === 'reject' ? 'bg-red-600 hover:bg-red-700 text-white' : 'bg-orange-600 hover:bg-orange-700 text-white'}
                        >
                            {submitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                            Confirm {actionType === 'sendback' ? 'Revision Request' : actionType}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </PageContainer>
    );
}
