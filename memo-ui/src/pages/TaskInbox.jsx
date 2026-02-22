import React, { useState, useEffect, useCallback } from 'react';
import { TaskApi } from '../lib/api';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../components/ui/table';
import { Loader2, Eye, Search, Filter, Inbox, Clock, ArrowRight, Sparkles, GitBranch, ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight, ArrowUpDown, ArrowUp, ArrowDown } from 'lucide-react';
import { toast } from 'sonner';
import { useNavigate } from 'react-router-dom';
import { format } from 'date-fns';
import { PageContainer } from '../components/PageContainer';

const PAGE_SIZES = [10, 20, 50];

export default function TaskInbox() {
    const navigate = useNavigate();

    // Pagination state
    const [page, setPage] = useState(0);
    const [size, setSize] = useState(10);
    const [sortBy, setSortBy] = useState('createTime');
    const [sortDir, setSortDir] = useState('desc');
    const [filterPriority, setFilterPriority] = useState('ALL');
    const [searchTerm, setSearchTerm] = useState('');
    const [searchInput, setSearchInput] = useState('');

    // Data state
    const [tasks, setTasks] = useState([]);
    const [totalElements, setTotalElements] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [loading, setLoading] = useState(true);

    const fetchTasks = useCallback(async () => {
        setLoading(true);
        try {
            const response = await TaskApi.getInbox({
                page,
                size,
                sortBy,
                sortDir,
                priority: filterPriority === 'ALL' ? undefined : filterPriority,
                search: searchTerm || undefined,
            });

            // Response is ApiResponse<PagedData<TaskDTO>>
            if (response?.success && response?.data) {
                setTasks(response.data.content || []);
                setTotalElements(response.data.totalElements || 0);
                setTotalPages(response.data.totalPages || 0);
            } else {
                // Fallback for unexpected shape
                setTasks([]);
                setTotalElements(0);
                setTotalPages(0);
            }
        } catch (error) {
            console.error(error);
            toast.error("Failed to load tasks");
            setTasks([]);
        } finally {
            setLoading(false);
        }
    }, [page, size, sortBy, sortDir, filterPriority, searchTerm]);

    useEffect(() => {
        fetchTasks();
    }, [fetchTasks]);

    // Reset to first page when filters change
    const handlePriorityChange = (p) => {
        setFilterPriority(p);
        setPage(0);
    };

    const handleSearch = (e) => {
        e.preventDefault();
        setSearchTerm(searchInput);
        setPage(0);
    };

    const handleSort = (field) => {
        if (sortBy === field) {
            setSortDir(d => d === 'asc' ? 'desc' : 'asc');
        } else {
            setSortBy(field);
            setSortDir('desc');
        }
        setPage(0);
    };

    const handlePageSizeChange = (newSize) => {
        setSize(newSize);
        setPage(0);
    };

    const viewMemo = (task) => {
        if (task.businessKey) {
            navigate(`/memo/${task.businessKey}`);
        }
    };

    const getPriorityColor = (priority) => {
        switch (priority) {
            case 100: return 'bg-red-50 text-red-600 border-red-200';
            case 75: return 'bg-amber-50 text-amber-600 border-amber-200';
            default: return 'bg-slate-50 text-slate-600 border-slate-200';
        }
    };

    const getPriorityLabel = (priority) => {
        switch (priority) {
            case 100: return 'URGENT';
            case 75: return 'HIGH';
            case 50: return 'NORMAL';
            default: return priority || 'NORMAL';
        }
    };

    const SortIcon = ({ field }) => {
        if (sortBy !== field) return <ArrowUpDown className="h-3 w-3 opacity-40" />;
        return sortDir === 'asc'
            ? <ArrowUp className="h-3 w-3 text-blue-600" />
            : <ArrowDown className="h-3 w-3 text-blue-600" />;
    };

    const fromItem = page * size + 1;
    const toItem = Math.min((page + 1) * size, totalElements);

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
                            <p className="text-slate-400 text-sm">
                                {totalElements} pending {totalElements === 1 ? 'task' : 'tasks'}
                            </p>
                        </div>
                    </div>

                    <div className="flex items-center gap-3">
                        {/* Search */}
                        <form onSubmit={handleSearch} className="relative">
                            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
                            <input
                                type="text"
                                placeholder="Search tasks..."
                                value={searchInput}
                                onChange={(e) => setSearchInput(e.target.value)}
                                onBlur={() => { if (searchInput !== searchTerm) { setSearchTerm(searchInput); setPage(0); } }}
                                className="h-9 w-56 pl-9 pr-3 rounded-lg bg-white/10 border border-white/10 text-white placeholder:text-slate-500 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500/50 transition-all"
                            />
                        </form>

                        {/* Priority Filters */}
                        <div className="flex items-center gap-1 bg-white/5 p-1 rounded-xl border border-white/10">
                            <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => handlePriorityChange('ALL')}
                                className={`rounded-lg px-4 transition-all ${filterPriority === 'ALL'
                                    ? 'bg-white text-slate-900 shadow-sm'
                                    : 'text-slate-400 hover:text-white hover:bg-white/10'
                                    }`}
                            >
                                All
                            </Button>
                            <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => handlePriorityChange('HIGH')}
                                className={`rounded-lg px-4 transition-all ${filterPriority === 'HIGH'
                                    ? 'bg-amber-500 text-white shadow-sm'
                                    : 'text-slate-400 hover:text-amber-400 hover:bg-amber-500/10'
                                    }`}
                            >
                                High
                            </Button>
                            <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => handlePriorityChange('URGENT')}
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
            </div>

            {/* Task Table Card */}
            <div className="p-6 md:p-8">
                <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
                    <Table>
                        <TableHeader>
                            <TableRow className="bg-slate-50/80 border-b border-slate-100">
                                <TableHead className="w-[120px] font-semibold text-slate-600">
                                    Reference
                                </TableHead>
                                <TableHead className="w-[30%] font-semibold text-slate-600">
                                    Subject / Topic
                                </TableHead>
                                <TableHead
                                    className="font-semibold text-slate-600 cursor-pointer select-none hover:text-blue-600 transition-colors"
                                    onClick={() => handleSort('name')}
                                >
                                    <div className="flex items-center gap-1.5">
                                        Current Step <SortIcon field="name" />
                                    </div>
                                </TableHead>
                                <TableHead
                                    className="font-semibold text-slate-600 cursor-pointer select-none hover:text-blue-600 transition-colors"
                                    onClick={() => handleSort('createTime')}
                                >
                                    <div className="flex items-center gap-1.5">
                                        Assigned On <SortIcon field="createTime" />
                                    </div>
                                </TableHead>
                                <TableHead
                                    className="font-semibold text-slate-600 cursor-pointer select-none hover:text-blue-600 transition-colors"
                                    onClick={() => handleSort('priority')}
                                >
                                    <div className="flex items-center gap-1.5">
                                        Priority <SortIcon field="priority" />
                                    </div>
                                </TableHead>
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
                            ) : tasks.length === 0 ? (
                                <TableRow>
                                    <TableCell colSpan={6} className="h-48 text-center">
                                        <div className="flex flex-col items-center gap-4">
                                            <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-slate-100 to-slate-50 flex items-center justify-center shadow-inner">
                                                <Sparkles className="h-8 w-8 text-slate-300" />
                                            </div>
                                            <div>
                                                <p className="font-semibold text-slate-700">All caught up!</p>
                                                <p className="text-slate-400 text-sm mt-1">
                                                    {searchTerm || filterPriority !== 'ALL'
                                                        ? 'No tasks match your filters.'
                                                        : 'No pending tasks require your action.'}
                                                </p>
                                            </div>
                                        </div>
                                    </TableCell>
                                </TableRow>
                            ) : (
                                tasks.map((task) => (
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
                                                {getPriorityLabel(task.priority)}
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

                    {/* Pagination Bar */}
                    {!loading && totalElements > 0 && (
                        <div className="flex items-center justify-between border-t border-slate-100 px-6 py-3 bg-slate-50/50">
                            {/* Left: Info + page size */}
                            <div className="flex items-center gap-4 text-sm text-slate-600">
                                <span>
                                    Showing <strong>{fromItem}</strong>–<strong>{toItem}</strong> of{' '}
                                    <strong>{totalElements}</strong>
                                </span>
                                <div className="flex items-center gap-1.5">
                                    <span className="text-slate-400">|</span>
                                    <span className="text-slate-500">Per page:</span>
                                    {PAGE_SIZES.map((s) => (
                                        <button
                                            key={s}
                                            onClick={() => handlePageSizeChange(s)}
                                            className={`px-2 py-0.5 rounded text-xs font-medium transition-colors ${size === s
                                                ? 'bg-blue-100 text-blue-700'
                                                : 'text-slate-500 hover:bg-slate-200'
                                                }`}
                                        >
                                            {s}
                                        </button>
                                    ))}
                                </div>
                            </div>

                            {/* Right: Page navigation */}
                            <div className="flex items-center gap-1">
                                <Button
                                    variant="ghost"
                                    size="icon"
                                    className="h-8 w-8"
                                    onClick={() => setPage(0)}
                                    disabled={page === 0}
                                >
                                    <ChevronsLeft className="h-4 w-4" />
                                </Button>
                                <Button
                                    variant="ghost"
                                    size="icon"
                                    className="h-8 w-8"
                                    onClick={() => setPage(p => Math.max(0, p - 1))}
                                    disabled={page === 0}
                                >
                                    <ChevronLeft className="h-4 w-4" />
                                </Button>

                                {/* Page numbers */}
                                {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                                    let pageNum;
                                    if (totalPages <= 5) {
                                        pageNum = i;
                                    } else if (page < 3) {
                                        pageNum = i;
                                    } else if (page > totalPages - 4) {
                                        pageNum = totalPages - 5 + i;
                                    } else {
                                        pageNum = page - 2 + i;
                                    }
                                    return (
                                        <button
                                            key={pageNum}
                                            onClick={() => setPage(pageNum)}
                                            className={`h-8 w-8 rounded-md text-sm font-medium transition-colors ${page === pageNum
                                                ? 'bg-blue-600 text-white shadow-sm'
                                                : 'text-slate-600 hover:bg-slate-200'
                                                }`}
                                        >
                                            {pageNum + 1}
                                        </button>
                                    );
                                })}

                                <Button
                                    variant="ghost"
                                    size="icon"
                                    className="h-8 w-8"
                                    onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                                    disabled={page >= totalPages - 1}
                                >
                                    <ChevronRight className="h-4 w-4" />
                                </Button>
                                <Button
                                    variant="ghost"
                                    size="icon"
                                    className="h-8 w-8"
                                    onClick={() => setPage(totalPages - 1)}
                                    disabled={page >= totalPages - 1}
                                >
                                    <ChevronsRight className="h-4 w-4" />
                                </Button>
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </PageContainer>
    );
}
