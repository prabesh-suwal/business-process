import React, { useState, useEffect } from 'react';
import { TaskApi, MemoApi } from '../lib/api';
import { Button } from '../components/ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../components/ui/table';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '../components/ui/dialog';
import { Label } from '../components/ui/label';
import { Loader2, CheckCircle, XCircle, ArrowLeft, Eye, Clock, User, FileText } from 'lucide-react';
import { toast } from 'sonner';
import { useNavigate } from 'react-router-dom';

export default function TaskInbox() {
    const navigate = useNavigate();
    const [tasks, setTasks] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedTask, setSelectedTask] = useState(null);
    const [completing, setCompleting] = useState(false);
    const [actionDialogOpen, setActionDialogOpen] = useState(false);
    const [actionType, setActionType] = useState(null); // 'approve' | 'reject' | 'sendback'
    const [comment, setComment] = useState('');

    const fetchTasks = async () => {
        setLoading(true);
        try {
            const data = await TaskApi.getInbox();
            setTasks(data || []);
        } catch (error) {
            console.error(error);
            toast.error("Failed to load tasks");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchTasks();
    }, []);

    const openActionDialog = (task, action) => {
        setSelectedTask(task);
        setActionType(action);
        setComment('');
        setActionDialogOpen(true);
    };

    const handleComplete = async () => {
        if (!selectedTask) return;
        setCompleting(true);
        try {
            const decisionMap = {
                'approve': 'APPROVE',
                'reject': 'REJECT',
                'sendback': 'SEND_BACK',
                'recommend': 'RECOMMEND',
                'cleared': 'CLEARED',
                'clarification': 'CLARIFICATION',
                'not_recommended': 'NOT_RECOMMENDED'
            };

            const decision = decisionMap[actionType] || 'APPROVE';

            await TaskApi.completeTask(
                selectedTask.id,
                decision,  // action parameter
                comment,   // comment parameter
                { decision }  // variables parameter
            );

            toast.success(`Task ${actionType === 'approve' ? 'approved' : actionType === 'reject' ? 'rejected' : 'completed'} successfully`);
            setActionDialogOpen(false);
            setSelectedTask(null);
            fetchTasks();
        } catch (error) {
            console.error(error);
            toast.error(error.response?.data?.message || "Failed to complete task");
        } finally {
            setCompleting(false);
        }
    };

    const handleClaim = async (task) => {
        try {
            await TaskApi.claimTask(task.id);
            toast.success("Task claimed");
            fetchTasks();
        } catch (error) {
            toast.error("Failed to claim task");
        }
    };

    const viewMemo = (task) => {
        if (task.businessKey) {
            navigate(`/memo/${task.businessKey}`);
        }
    };

    const formatDate = (dateStr) => {
        if (!dateStr) return '-';
        return new Date(dateStr).toLocaleString();
    };

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-3xl font-bold tracking-tight">Task Inbox</h1>
                    <p className="text-muted-foreground">Pending approval tasks assigned to you or your groups.</p>
                </div>
                <Button variant="outline" onClick={fetchTasks} disabled={loading}>
                    {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : 'Refresh'}
                </Button>
            </div>

            <Card>
                <CardContent className="pt-6">
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead>Task</TableHead>
                                <TableHead>Reference</TableHead>
                                <TableHead>Created</TableHead>
                                <TableHead>Assignee</TableHead>
                                <TableHead className="text-right">Actions</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {loading ? (
                                <TableRow>
                                    <TableCell colSpan={5} className="text-center h-32">
                                        <Loader2 className="h-8 w-8 animate-spin mx-auto text-primary" />
                                    </TableCell>
                                </TableRow>
                            ) : tasks.length === 0 ? (
                                <TableRow>
                                    <TableCell colSpan={5} className="text-center h-32 text-muted-foreground">
                                        <div className="flex flex-col items-center gap-2">
                                            <CheckCircle className="h-10 w-10 opacity-20" />
                                            <p>No pending tasks. You're all caught up!</p>
                                        </div>
                                    </TableCell>
                                </TableRow>
                            ) : (
                                tasks.map(task => (
                                    <TableRow key={task.id} className="group">
                                        <TableCell>
                                            <div className="font-medium">{task.name}</div>
                                            <div className="text-xs text-muted-foreground">{task.processTemplateName || task.processTitle}</div>
                                        </TableCell>
                                        <TableCell>
                                            <div className="flex items-center gap-2">
                                                <FileText className="h-4 w-4 text-muted-foreground" />
                                                <span className="font-mono text-xs">{task.businessKey || '-'}</span>
                                            </div>
                                            {task.processVariables?.subject && (
                                                <div className="text-xs text-muted-foreground mt-1 truncate max-w-[200px]">
                                                    {task.processVariables.subject}
                                                </div>
                                            )}
                                        </TableCell>
                                        <TableCell>
                                            <div className="flex items-center gap-1 text-sm">
                                                <Clock className="h-3 w-3 text-muted-foreground" />
                                                {formatDate(task.createTime)}
                                            </div>
                                        </TableCell>
                                        <TableCell>
                                            {task.assignee ? (
                                                <Badge variant="secondary" className="font-normal">
                                                    <User className="h-3 w-3 mr-1" />
                                                    {task.assigneeName || task.assignee}
                                                </Badge>
                                            ) : (
                                                <Badge variant="outline" className="text-muted-foreground">
                                                    Unassigned
                                                </Badge>
                                            )}
                                        </TableCell>
                                        <TableCell className="text-right">
                                            <div className="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                                                {task.businessKey && (
                                                    <Button variant="ghost" size="sm" onClick={() => viewMemo(task)}>
                                                        <Eye className="h-4 w-4" />
                                                    </Button>
                                                )}
                                                {!task.assignee && (
                                                    <Button variant="outline" size="sm" onClick={() => handleClaim(task)}>
                                                        Claim
                                                    </Button>
                                                )}
                                                {task.assignee && (
                                                    <>
                                                        <Button
                                                            variant="outline"
                                                            size="sm"
                                                            className="text-green-600 hover:text-green-700 hover:bg-green-50"
                                                            onClick={() => openActionDialog(task, 'approve')}
                                                        >
                                                            <CheckCircle className="h-4 w-4 mr-1" />
                                                            Approve
                                                        </Button>
                                                        <Button
                                                            variant="outline"
                                                            size="sm"
                                                            className="text-red-600 hover:text-red-700 hover:bg-red-50"
                                                            onClick={() => openActionDialog(task, 'reject')}
                                                        >
                                                            <XCircle className="h-4 w-4 mr-1" />
                                                            Reject
                                                        </Button>
                                                    </>
                                                )}
                                            </div>
                                        </TableCell>
                                    </TableRow>
                                ))
                            )}
                        </TableBody>
                    </Table>
                </CardContent>
            </Card>

            {/* Action Dialog */}
            <Dialog open={actionDialogOpen} onOpenChange={setActionDialogOpen}>
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>
                            {actionType === 'approve' ? 'Approve Task' : actionType === 'reject' ? 'Reject Task' : 'Complete Task'}
                        </DialogTitle>
                        <DialogDescription>
                            {selectedTask?.name} - {selectedTask?.businessKey}
                        </DialogDescription>
                    </DialogHeader>
                    <div className="space-y-4 py-4">
                        <div className="space-y-2">
                            <Label htmlFor="comment">Comment {actionType === 'reject' && <span className="text-red-500">*</span>}</Label>
                            <textarea
                                id="comment"
                                className="w-full min-h-[100px] px-3 py-2 border rounded-md text-sm"
                                placeholder="Add a comment..."
                                value={comment}
                                onChange={(e) => setComment(e.target.value)}
                            />
                        </div>
                    </div>
                    <DialogFooter>
                        <Button variant="outline" onClick={() => setActionDialogOpen(false)}>Cancel</Button>
                        <Button
                            onClick={handleComplete}
                            disabled={completing || (actionType === 'reject' && !comment)}
                            className={actionType === 'approve' ? 'bg-green-600 hover:bg-green-700' : actionType === 'reject' ? 'bg-red-600 hover:bg-red-700' : ''}
                        >
                            {completing && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                            {actionType === 'approve' ? 'Approve' : actionType === 'reject' ? 'Reject' : 'Complete'}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </div>
    );
}
