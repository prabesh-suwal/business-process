import React, { useState, useEffect } from 'react';
import { TaskApi } from '../lib/api';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../components/ui/table';
import { Loader2, Eye, Search, Filter, Inbox, Clock, ArrowRight, Sparkles, GitBranch } from 'lucide-react';
import { toast } from 'sonner';
import { useNavigate } from 'react-router-dom';
import { format } from 'date-fns';
import { PageContainer } from '../components/PageContainer';

export default function TaskInbox() {
    const navigate = useNavigate();
    const [tasks, setTasks] = useState([]);
    const [loading, setLoading] = useState(true);
    const [filterPriority, setFilterPriority] = useState('ALL');

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

    const viewMemo = (task) => {
        if (task.businessKey) {
            navigate(`/memo/${task.businessKey}`);
        }
    };

    const getPriorityColor = (priority) => {
        switch (priority) {
            case 'URGENT': return 'bg-red-50 text-red-600 border-red-200';
            case 'HIGH': return 'bg-amber-50 text-amber-600 border-amber-200';
            default: return 'bg-slate-50 text-slate-600 border-slate-200';
        }
    };

    const filteredTasks = tasks.filter(t => filterPriority === 'ALL' ? true : t.priority === filterPriority);

    return (
        <PageContainer className="p-0">
            {/* Premium Dark Header */}
            <div className="bg-gradient-to-r from-slate-900 via-slate-800 to-slate-900 px-8 py-6">
                <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                    <div className="flex items-center gap-4">
                        <div className="h-12 w-12 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center shadow-lg shadow-blue-500/20">
                            <Inbox className="h-6 w-6 text-white" />
                        </div>
                        <div>
                            <h1 className="text-2xl font-bold text-white">Task Inbox</h1>
                            <p className="text-slate-400 text-sm">Review and approve pending memos assigned to you</p>
                        </div>
                    </div>

                    {/* Filter Buttons */}
                    <div className="flex items-center gap-1 bg-white/5 p-1 rounded-xl border border-white/10">
                        <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setFilterPriority('ALL')}
                            className={`rounded-lg px-4 transition-all ${filterPriority === 'ALL'
                                ? 'bg-white text-slate-900 shadow-sm'
                                : 'text-slate-400 hover:text-white hover:bg-white/10'
                                }`}
                        >
                            All Tasks
                            <span className="ml-2 px-1.5 py-0.5 rounded-full bg-slate-100 text-slate-600 text-xs font-bold">
                                {tasks.length}
                            </span>
                        </Button>
                        <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setFilterPriority('HIGH')}
                            className={`rounded-lg px-4 transition-all ${filterPriority === 'HIGH'
                                ? 'bg-amber-500 text-white shadow-sm'
                                : 'text-slate-400 hover:text-amber-400 hover:bg-amber-500/10'
                                }`}
                        >
                            High Priority
                        </Button>
                        <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setFilterPriority('URGENT')}
                            className={`rounded-lg px-4 transition-all ${filterPriority === 'URGENT'
                                ? 'bg-red-500 text-white shadow-sm'
                                : 'text-slate-400 hover:text-red-400 hover:bg-red-500/10'
                                }`}
                        >
                            Urgent
                        </Button>
                    </div>
                </div>
            </div>

            {/* Task Table Card */}
            <div className="p-6 md:p-8">
                <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
                    <Table>
                        <TableHeader>
                            <TableRow className="bg-slate-50/80 border-b border-slate-100">
                                <TableHead className="w-[100px] font-semibold text-slate-600">Reference</TableHead>
                                <TableHead className="w-[30%] font-semibold text-slate-600">Subject / Topic</TableHead>
                                <TableHead className="font-semibold text-slate-600">Current Step</TableHead>
                                <TableHead className="font-semibold text-slate-600">Assigned On</TableHead>
                                <TableHead className="font-semibold text-slate-600">Priority</TableHead>
                                <TableHead className="text-right font-semibold text-slate-600">Action</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {loading ? (
                                <TableRow>
                                    <TableCell colSpan={6} className="h-40 text-center">
                                        <div className="flex justify-center flex-col items-center gap-3">
                                            <div className="relative">
                                                <div className="animate-spin rounded-full h-10 w-10 border-3 border-blue-200 border-t-blue-600"></div>
                                            </div>
                                            <span className="text-slate-500 font-medium">Loading your tasks...</span>
                                        </div>
                                    </TableCell>
                                </TableRow>
                            ) : filteredTasks.length === 0 ? (
                                <TableRow>
                                    <TableCell colSpan={6} className="h-48 text-center">
                                        <div className="flex flex-col items-center gap-4">
                                            <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-slate-100 to-slate-50 flex items-center justify-center shadow-inner">
                                                <Sparkles className="h-8 w-8 text-slate-300" />
                                            </div>
                                            <div>
                                                <p className="font-semibold text-slate-700">All caught up!</p>
                                                <p className="text-slate-400 text-sm mt-1">No pending tasks require your action.</p>
                                            </div>
                                        </div>
                                    </TableCell>
                                </TableRow>
                            ) : (
                                filteredTasks.map((task, index) => (
                                    <TableRow
                                        key={task.id}
                                        className="group transition-all duration-200 cursor-pointer hover:bg-blue-50/50 border-b border-slate-100 last:border-0"
                                        onClick={() => viewMemo(task)}
                                    >
                                        <TableCell className="font-mono text-xs font-bold text-slate-900">
                                            {task.memoNumber || task.businessKey || '-'}
                                        </TableCell>
                                        <TableCell>
                                            <div className="font-semibold text-slate-900 leading-snug group-hover:text-blue-700 transition-colors">
                                                {task.subject || task.name || task.processTemplateName}
                                            </div>
                                            <div className="text-xs text-slate-500 mt-1 flex items-center gap-2 flex-wrap">
                                                <span className="bg-slate-100 px-2 py-0.5 rounded-md text-slate-600 font-medium">
                                                    {task.topicName || task.category || 'Memo'}
                                                </span>
                                                {/* Parallel workflow indicator */}
                                                {task.isParallelExecution && (
                                                    <span className="inline-flex items-center gap-1 px-2 py-0.5 bg-purple-100 text-purple-700 rounded-md font-medium">
                                                        <GitBranch className="w-3 h-3" />
                                                        {task.parallelProgress || 'Multiple approvals'}
                                                    </span>
                                                )}
                                            </div>
                                        </TableCell>
                                        <TableCell>
                                            <div className="flex items-center gap-2">
                                                <div className="h-2 w-2 rounded-full bg-blue-500 animate-pulse"></div>
                                                <span className="text-sm font-medium text-slate-700">{task.name}</span>
                                            </div>
                                        </TableCell>
                                        <TableCell>
                                            <div className="flex items-center gap-2 text-slate-600 text-sm">
                                                <Clock className="h-3.5 w-3.5 text-slate-400" />
                                                {task.createTime ? format(new Date(task.createTime), 'MMM d, h:mm a') : '-'}
                                            </div>
                                        </TableCell>
                                        <TableCell>
                                            <div className={`inline-flex items-center px-2.5 py-1 rounded-lg text-xs font-semibold border ${getPriorityColor(task.priority)}`}>
                                                {task.priority || 'NORMAL'}
                                            </div>
                                        </TableCell>
                                        <TableCell className="text-right">
                                            <Button
                                                size="sm"
                                                className="bg-blue-50 text-blue-600 hover:bg-blue-100 border-0 shadow-none group-hover:bg-blue-500 group-hover:text-white transition-all"
                                            >
                                                Review <ArrowRight className="ml-1 h-3.5 w-3.5" />
                                            </Button>
                                        </TableCell>
                                    </TableRow>
                                ))
                            )}
                        </TableBody>
                    </Table>
                </div>
            </div>
        </PageContainer>
    );
}

