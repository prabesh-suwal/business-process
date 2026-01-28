import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { MemoApi } from '../lib/api';
import { Card, CardHeader, CardTitle, CardContent } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { toast } from 'sonner';
import {
    FileText, Calendar, User, Plus, Filter, Search,
    ChevronRight, Clock, CheckCircle2, XCircle, AlertCircle,
    Eye, Edit3, UserCheck
} from 'lucide-react';
import { PageContainer } from '../components/PageContainer';

/**
 * Memos - Display all memos the user can access:
 * - Created by the user (initiator)
 * - Involved in workflow (past or current approver)
 * - Has explicit viewer access
 */
const Memos = () => {
    const [memos, setMemos] = useState([]);
    const [loading, setLoading] = useState(true);
    const [filter, setFilter] = useState('all'); // all, mine, assigned, watching
    const [searchQuery, setSearchQuery] = useState('');
    const navigate = useNavigate();

    useEffect(() => {
        loadMemos();
    }, []);

    const loadMemos = async () => {
        try {
            setLoading(true);
            const data = await MemoApi.getAccessibleMemos();
            setMemos(data);
        } catch (error) {
            console.error('Error loading memos:', error);
            toast.error('Failed to load memos');
        } finally {
            setLoading(false);
        }
    };

    const getStatusConfig = (status) => {
        const configs = {
            DRAFT: {
                color: 'bg-slate-100 text-slate-700 border-slate-200',
                icon: Edit3,
                label: 'Draft'
            },
            SUBMITTED: {
                color: 'bg-blue-100 text-blue-700 border-blue-200',
                icon: Clock,
                label: 'Submitted'
            },
            IN_PROGRESS: {
                color: 'bg-amber-100 text-amber-700 border-amber-200',
                icon: AlertCircle,
                label: 'In Progress'
            },
            APPROVED: {
                color: 'bg-emerald-100 text-emerald-700 border-emerald-200',
                icon: CheckCircle2,
                label: 'Approved'
            },
            REJECTED: {
                color: 'bg-red-100 text-red-700 border-red-200',
                icon: XCircle,
                label: 'Rejected'
            },
        };
        return configs[status] || configs.DRAFT;
    };

    const getAccessReasonConfig = (reason) => {
        const configs = {
            INITIATOR: {
                color: 'bg-blue-50 text-blue-600 border-blue-200',
                icon: User,
                label: 'Created by you'
            },
            CURRENT_APPROVER: {
                color: 'bg-amber-50 text-amber-600 border-amber-200',
                icon: UserCheck,
                label: 'Pending your action'
            },
            PAST_APPROVER: {
                color: 'bg-emerald-50 text-emerald-600 border-emerald-200',
                icon: CheckCircle2,
                label: 'You approved'
            },
            VIEWER: {
                color: 'bg-purple-50 text-purple-600 border-purple-200',
                icon: Eye,
                label: 'View access'
            },
        };
        return configs[reason] || configs.VIEWER;
    };

    const formatDate = (dateString) => {
        if (!dateString) return 'N/A';
        const date = new Date(dateString);
        const now = new Date();
        const diffMs = now - date;
        const diffMins = Math.floor(diffMs / 60000);
        const diffHours = Math.floor(diffMins / 60);
        const diffDays = Math.floor(diffHours / 24);

        if (diffMins < 1) return 'Just now';
        if (diffMins < 60) return `${diffMins}m ago`;
        if (diffHours < 24) return `${diffHours}h ago`;
        if (diffDays < 7) return `${diffDays}d ago`;

        return date.toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric',
            year: date.getFullYear() !== now.getFullYear() ? 'numeric' : undefined
        });
    };

    // Filter memos based on selected filter
    const filteredMemos = memos.filter(memo => {
        // Search filter
        if (searchQuery) {
            const query = searchQuery.toLowerCase();
            const matchesSearch =
                memo.subject?.toLowerCase().includes(query) ||
                memo.memoNumber?.toLowerCase().includes(query) ||
                memo.topicName?.toLowerCase().includes(query);
            if (!matchesSearch) return false;
        }

        // Tab filter
        switch (filter) {
            case 'mine':
                return memo.accessReason === 'INITIATOR';
            case 'assigned':
                return memo.accessReason === 'CURRENT_APPROVER';
            case 'watching':
                return memo.accessReason === 'VIEWER' || memo.accessReason === 'PAST_APPROVER';
            default:
                return true;
        }
    });

    // Count for filter tabs
    const counts = {
        all: memos.length,
        mine: memos.filter(m => m.accessReason === 'INITIATOR').length,
        assigned: memos.filter(m => m.accessReason === 'CURRENT_APPROVER').length,
        watching: memos.filter(m => m.accessReason === 'VIEWER' || m.accessReason === 'PAST_APPROVER').length,
    };

    if (loading) {
        return (
            <PageContainer>
                <div className="animate-pulse space-y-4">
                    <div className="h-8 bg-slate-200 rounded w-1/4"></div>
                    <div className="h-12 bg-slate-200 rounded"></div>
                    <div className="h-32 bg-slate-200 rounded"></div>
                    <div className="h-32 bg-slate-200 rounded"></div>
                </div>
            </PageContainer>
        );
    }

    return (
        <PageContainer>
            {/* Header */}
            <div className="flex items-center justify-between mb-6">
                <div>
                    <h1 className="text-2xl font-bold text-slate-900">Memos</h1>
                    <p className="text-sm text-slate-600 mt-1">
                        All memos you have access to
                    </p>
                </div>
                <Button
                    onClick={() => navigate('/create-memo')}
                    className="bg-gradient-to-r from-blue-500 to-indigo-600 hover:from-blue-600 hover:to-indigo-700 text-white shadow-lg shadow-blue-500/25"
                >
                    <Plus className="w-4 h-4 mr-2" />
                    New Memo
                </Button>
            </div>

            {/* Search and Filter Bar */}
            <div className="flex items-center gap-4 mb-6">
                {/* Search */}
                <div className="flex-1 relative">
                    <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                    <input
                        type="text"
                        placeholder="Search memos..."
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        className="w-full pl-10 pr-4 py-2.5 border border-slate-200 rounded-lg text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                    />
                </div>

                {/* Filter Tabs */}
                <div className="flex items-center bg-slate-100 rounded-lg p-1">
                    {[
                        { key: 'all', label: 'All' },
                        { key: 'mine', label: 'Created by Me' },
                        { key: 'assigned', label: 'Assigned to Me' },
                        { key: 'watching', label: 'Watching' },
                    ].map(tab => (
                        <button
                            key={tab.key}
                            onClick={() => setFilter(tab.key)}
                            className={`px-4 py-1.5 text-sm font-medium rounded-md transition-all ${filter === tab.key
                                ? 'bg-white text-slate-800 shadow-sm'
                                : 'text-slate-600 hover:text-slate-800'
                                }`}
                        >
                            {tab.label}
                            {counts[tab.key] > 0 && (
                                <span className={`ml-1.5 px-1.5 py-0.5 text-xs rounded-full ${filter === tab.key
                                    ? 'bg-blue-100 text-blue-600'
                                    : 'bg-slate-200 text-slate-600'
                                    }`}>
                                    {counts[tab.key]}
                                </span>
                            )}
                        </button>
                    ))}
                </div>
            </div>

            {/* Memos Table */}
            {filteredMemos.length === 0 ? (
                <Card>
                    <CardContent className="flex flex-col items-center justify-center py-16">
                        <div className="w-20 h-20 rounded-2xl bg-gradient-to-br from-slate-100 to-slate-50 flex items-center justify-center mb-4 shadow-inner">
                            <FileText className="w-10 h-10 text-slate-300" />
                        </div>
                        <p className="text-slate-600 font-medium">No memos found</p>
                        <p className="text-sm text-slate-500 mt-1 text-center max-w-sm">
                            {searchQuery
                                ? `No memos match "${searchQuery}"`
                                : filter === 'assigned'
                                    ? 'No memos pending your action'
                                    : 'Create a new memo to get started'}
                        </p>
                        {!searchQuery && filter === 'all' && (
                            <Button
                                onClick={() => navigate('/create-memo')}
                                className="mt-4 bg-blue-50 text-blue-600 hover:bg-blue-100"
                            >
                                <Plus className="w-4 h-4 mr-1" />
                                Create Memo
                            </Button>
                        )}
                    </CardContent>
                </Card>
            ) : (
                <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
                    <table className="w-full">
                        <thead>
                            <tr className="bg-slate-50 border-b border-slate-200">
                                <th className="text-left px-4 py-3 text-xs font-semibold text-slate-600 uppercase tracking-wider">Memo #</th>
                                <th className="text-left px-4 py-3 text-xs font-semibold text-slate-600 uppercase tracking-wider">Subject</th>
                                <th className="text-left px-4 py-3 text-xs font-semibold text-slate-600 uppercase tracking-wider">Topic</th>
                                <th className="text-left px-4 py-3 text-xs font-semibold text-slate-600 uppercase tracking-wider">Status</th>
                                <th className="text-left px-4 py-3 text-xs font-semibold text-slate-600 uppercase tracking-wider">Current Stage</th>
                                <th className="text-left px-4 py-3 text-xs font-semibold text-slate-600 uppercase tracking-wider">Access</th>
                                <th className="text-left px-4 py-3 text-xs font-semibold text-slate-600 uppercase tracking-wider">Updated</th>
                                <th className="w-10"></th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                            {filteredMemos.map((memo) => {
                                const statusConfig = getStatusConfig(memo.status);
                                const accessConfig = getAccessReasonConfig(memo.accessReason);
                                const StatusIcon = statusConfig.icon;
                                const AccessIcon = accessConfig.icon;

                                return (
                                    <tr
                                        key={memo.id}
                                        onClick={() => navigate(`/memos/${memo.id}/view`)}
                                        className="hover:bg-slate-50 cursor-pointer group transition-colors"
                                    >
                                        <td className="px-4 py-3">
                                            <span className="font-mono text-sm text-slate-600">
                                                {memo.memoNumber || 'DRAFT'}
                                            </span>
                                        </td>
                                        <td className="px-4 py-3">
                                            <span className="font-medium text-slate-800 group-hover:text-blue-600 transition-colors line-clamp-1">
                                                {memo.subject || 'Untitled Memo'}
                                            </span>
                                        </td>
                                        <td className="px-4 py-3">
                                            <span className="text-sm text-slate-600">
                                                {memo.topicName || '-'}
                                            </span>
                                        </td>
                                        <td className="px-4 py-3">
                                            <Badge className={`text-xs ${statusConfig.color}`}>
                                                <StatusIcon className="w-3 h-3 mr-1" />
                                                {statusConfig.label}
                                            </Badge>
                                        </td>
                                        <td className="px-4 py-3">
                                            <span className="text-sm text-slate-600">
                                                {memo.currentStage || '-'}
                                            </span>
                                        </td>
                                        <td className="px-4 py-3">
                                            <Badge variant="outline" className={`text-xs ${accessConfig.color}`}>
                                                <AccessIcon className="w-3 h-3 mr-1" />
                                                {accessConfig.label}
                                            </Badge>
                                        </td>
                                        <td className="px-4 py-3">
                                            <span className="text-sm text-slate-500">
                                                {formatDate(memo.updatedAt || memo.createdAt)}
                                            </span>
                                        </td>
                                        <td className="px-4 py-3">
                                            <ChevronRight className="w-4 h-4 text-slate-300 group-hover:text-slate-500 group-hover:translate-x-1 transition-all" />
                                        </td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                </div>
            )}
        </PageContainer>
    );
};

export default Memos;
