import React, { useState, useEffect } from 'react';
import { TaskApi } from '../lib/api';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../components/ui/table';
import { Loader2, Eye, Search, Filter } from 'lucide-react';
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
            case 'URGENT': return 'bg-red-100 text-red-700 border-red-200';
            case 'HIGH': return 'bg-orange-100 text-orange-700 border-orange-200';
            default: return 'bg-brand-slate-light text-slate-700 border-slate-200';
        }
    };

    const filteredTasks = tasks.filter(t => filterPriority === 'ALL' ? true : t.priority === filterPriority);

    return (
        <PageContainer>
            {/* Header & Filters */}
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-bold tracking-tight text-brand-navy">Task Inbox</h1>
                    <p className="text-slate-500 text-sm">Review/approve pending memos assigned to you</p>
                </div>

                <div className="flex items-center gap-2 bg-white p-1 rounded-md border border-slate-200 shadow-sm">
                    <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => setFilterPriority('ALL')}
                        className={filterPriority === 'ALL' ? 'bg-brand-navy/10 text-brand-navy font-semibold' : 'text-slate-500'}
                    >
                        All
                    </Button>
                    <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => setFilterPriority('HIGH')}
                        className={filterPriority === 'HIGH' ? 'bg-orange-50 text-orange-700 font-semibold' : 'text-slate-500'}
                    >
                        High Priority
                    </Button>
                    <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => setFilterPriority('URGENT')}
                        className={filterPriority === 'URGENT' ? 'bg-red-50 text-red-700 font-semibold' : 'text-slate-500'}
                    >
                        Urgent
                    </Button>
                </div>
            </div>

            {/* Task Table */}
            <div className="bg-white rounded-lg border border-slate-200 shadow-sm overflow-hidden">
                <Table>
                    <TableHeader className="bg-brand-slate-light/30">
                        <TableRow>
                            <TableHead className="w-[100px]">Reference</TableHead>
                            <TableHead className="w-[30%]">Subject / Topic</TableHead>
                            <TableHead>Current Step</TableHead>
                            <TableHead>Assigned On</TableHead>
                            <TableHead>Priority</TableHead>
                            <TableHead className="text-right">Action</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {loading ? (
                            <TableRow>
                                <TableCell colSpan={6} className="h-32 text-center">
                                    <div className="flex justify-center flex-col items-center gap-2 text-slate-400">
                                        <Loader2 className="h-6 w-6 animate-spin" />
                                        <span>Loading your tasks...</span>
                                    </div>
                                </TableCell>
                            </TableRow>
                        ) : filteredTasks.length === 0 ? (
                            <TableRow>
                                <TableCell colSpan={6} className="h-40 text-center text-slate-500">
                                    <div className="flex flex-col items-center gap-3">
                                        <div className="bg-slate-100 p-3 rounded-full">
                                            <Filter className="h-6 w-6 text-slate-400" />
                                        </div>
                                        <p>No pending tasks found requiring your action.</p>
                                    </div>
                                </TableCell>
                            </TableRow>
                        ) : (
                            filteredTasks.map((task, index) => (
                                <TableRow
                                    key={task.id}
                                    className={`group transition-colors cursor-pointer ${index % 2 === 0 ? 'bg-white' : 'bg-slate-50/50'} hover:bg-blue-50/50`}
                                    onClick={() => viewMemo(task)}
                                >
                                    <TableCell className="font-mono text-xs font-semibold text-brand-navy">
                                        {task.businessKey || '-'}
                                    </TableCell>
                                    <TableCell>
                                        <div className="font-semibold text-slate-900 leading-snug">{task.name || task.processTemplateName}</div>
                                        <div className="text-xs text-slate-500 mt-1 flex items-center gap-2">
                                            <span className="bg-slate-100 px-1.5 py-0.5 rounded text-slate-600 font-medium">{task.category || 'Memo'}</span>
                                            {task.description && <span className="truncate max-w-[200px] italic text-slate-400">- {task.description}</span>}
                                        </div>
                                    </TableCell>
                                    <TableCell>
                                        <div className="flex items-center gap-2">
                                            <div className="h-2 w-2 rounded-full bg-brand-blue animate-pulse"></div>
                                            <span className="text-sm font-medium text-slate-700">{task.name}</span>
                                        </div>
                                    </TableCell>
                                    <TableCell className="text-slate-600 text-sm">
                                        {task.createTime ? format(new Date(task.createTime), 'MMM d, h:mm a') : '-'}
                                    </TableCell>
                                    <TableCell>
                                        <div className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border ${getPriorityColor(task.priority)}`}>
                                            {task.priority || 'NORMAL'}
                                        </div>
                                    </TableCell>
                                    <TableCell className="text-right">
                                        <Button size="sm" variant="ghost" className="text-brand-blue hover:bg-blue-50">
                                            Open <Eye className="ml-1 h-3.5 w-3.5" />
                                        </Button>
                                    </TableCell>
                                </TableRow>
                            ))
                        )}
                    </TableBody>
                </Table>
            </div>
        </PageContainer>
    );
}
