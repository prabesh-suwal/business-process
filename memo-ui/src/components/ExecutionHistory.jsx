import React, { useState, useEffect, useMemo } from 'react';
import { HistoryApi } from '../lib/api';
import {
    Clock, CheckCircle, XCircle, CornerUpLeft, User, Play,
    RefreshCw, Filter, Timer, ArrowRight, Ban, FileText,
    Zap, Send, MessageSquare, ChevronsRight, Loader2, GitBranch
} from 'lucide-react';
import { format, formatDistanceToNow } from 'date-fns';

// ─── Filter Config ──────────────────────────────────────────────────────
const FILTERS = [
    { key: 'all', label: 'All' },
    { key: 'decisions', label: 'Decisions' },
    { key: 'sendbacks', label: 'Send-backs' },
    { key: 'system', label: 'System' },
    { key: 'taskflow', label: 'Task Flow' },
];

const FILTER_MAP = {
    all: ['PROCESS_STARTED', 'PROCESS_COMPLETED', 'PROCESS_CANCELLED', 'MEMO_SUBMITTED',
        'TASK_COMPLETED', 'TASK_REJECTED', 'TASK_SENT_BACK', 'TASK_CANCELLED',
        'TASK_ASSIGNED', 'TASK_CLAIMED'],
    decisions: ['TASK_COMPLETED', 'TASK_REJECTED'],
    sendbacks: ['TASK_SENT_BACK'],
    system: ['PROCESS_STARTED', 'PROCESS_COMPLETED', 'PROCESS_CANCELLED', 'MEMO_SUBMITTED'],
    taskflow: ['TASK_CREATED'],
};

// ─── Event Config ───────────────────────────────────────────────────────
const EVENT_CONFIG = {
    MEMO_SUBMITTED: { icon: Send, color: 'text-violet-600', border: 'border-violet-200', badge: 'bg-violet-100 text-violet-700', label: 'Memo Submitted' },
    PROCESS_STARTED: { icon: Play, color: 'text-blue-600', border: 'border-blue-200', badge: 'bg-blue-100 text-blue-700', label: 'Workflow Started' },
    PROCESS_COMPLETED: { icon: CheckCircle, color: 'text-emerald-600', border: 'border-emerald-200', badge: 'bg-emerald-100 text-emerald-700', label: 'Workflow Completed' },
    PROCESS_CANCELLED: { icon: Ban, color: 'text-red-500', border: 'border-red-200', badge: 'bg-red-100 text-red-700', label: 'Workflow Cancelled' },
    TASK_COMPLETED: { icon: CheckCircle, color: 'text-green-600', border: 'border-green-200', badge: 'bg-green-100 text-green-700', label: 'Completed' },
    TASK_SENT_BACK: { icon: CornerUpLeft, color: 'text-orange-500', border: 'border-orange-200', badge: 'bg-orange-100 text-orange-700', label: 'Sent Back' },
    TASK_REJECTED: { icon: XCircle, color: 'text-red-600', border: 'border-red-200', badge: 'bg-red-100 text-red-700', label: 'Rejected' },
    TASK_CANCELLED: { icon: Ban, color: 'text-slate-400', border: 'border-slate-200', badge: 'bg-slate-100 text-slate-500', label: 'Cancelled' },
    TASK_CLAIMED: { icon: User, color: 'text-indigo-500', border: 'border-indigo-200', badge: 'bg-indigo-100 text-indigo-600', label: 'Claimed' },
    TASK_ASSIGNED: { icon: User, color: 'text-indigo-400', border: 'border-indigo-100', badge: 'bg-indigo-50 text-indigo-500', label: 'Assigned' },
    TASK_CREATED: { icon: Zap, color: 'text-slate-400', border: 'border-slate-200', badge: 'bg-slate-100 text-slate-500', label: 'Task Created' },
};
const DEFAULT_CONFIG = { icon: Clock, color: 'text-slate-400', border: 'border-slate-200', badge: 'bg-slate-100 text-slate-500', label: 'Action' };

// ─── Helpers ────────────────────────────────────────────────────────────
function formatDuration(ms) {
    if (ms == null) return null;
    const seconds = Math.floor(ms / 1000);
    if (seconds < 60) return seconds <= 1 ? '< 1 min' : `${seconds}s`;
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) return `${minutes} min${minutes > 1 ? 's' : ''}`;
    const hours = Math.floor(minutes / 60);
    const remainMins = minutes % 60;
    if (hours < 24) return remainMins > 0 ? `${hours}h ${remainMins}m` : `${hours}h`;
    const days = Math.floor(hours / 24);
    const remainHours = hours % 24;
    return remainHours > 0 ? `${days}d ${remainHours}h` : `${days}d`;
}

function formatRelativeTime(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now - date;
    const diffSec = Math.floor(diffMs / 1000);
    if (diffSec < 60) return 'just now';
    if (diffSec < 120) return '1 minute ago';
    const diffMin = Math.floor(diffSec / 60);
    if (diffMin < 60) return `${diffMin} minutes ago`;
    const diffHr = Math.floor(diffMin / 60);
    if (diffHr < 24) return `${diffHr} hour${diffHr > 1 ? 's' : ''} ago`;
    const diffDays = Math.floor(diffHr / 24);
    if (diffDays === 1) return 'yesterday';
    if (diffDays < 7) return `${diffDays} days ago`;
    return formatDistanceToNow(date, { addSuffix: true });
}

function formatAbsoluteDate(dateStr) {
    if (!dateStr) return '';
    return format(new Date(dateStr), 'MMM d, h:mm a');
}

function getConfig(actionType) {
    return EVENT_CONFIG[actionType] || DEFAULT_CONFIG;
}

// ─── BPMN-like Task Flow Diagram ─────────────────────────────────────────
function TaskFlowDiagram({ events }) {
    // Build flow levels from timeline events:
    // Group TASK_CREATED events into "levels" — tasks created within 2s of each other = same level (parallel)
    const flowLevels = useMemo(() => {
        const taskCreated = events.filter(e => e.actionType === 'TASK_CREATED');
        const decisions = events.filter(e =>
            ['TASK_COMPLETED', 'TASK_REJECTED', 'TASK_SENT_BACK', 'TASK_CANCELLED'].includes(e.actionType)
        );
        const processCompleted = events.find(e => e.actionType === 'PROCESS_COMPLETED');
        const processCancelled = events.find(e => e.actionType === 'PROCESS_CANCELLED');

        // For each TASK_CREATED, find its matching decision event:
        // The FIRST decision for the same taskName that occurs AFTER the creation time
        function findDecisionForInstance(createdEvent) {
            const createTime = new Date(createdEvent.createdAt).getTime();
            const name = createdEvent.taskName;
            return decisions.find(d =>
                d.taskName === name && new Date(d.createdAt).getTime() >= createTime
            );
        }

        // Group created tasks by time proximity (within 2s = same parallel level)
        const levels = [];
        let currentLevel = [];
        let lastTime = null;

        taskCreated.forEach(tc => {
            const t = new Date(tc.createdAt).getTime();
            if (lastTime !== null && (t - lastTime) > 2000) {
                if (currentLevel.length > 0) levels.push(currentLevel);
                currentLevel = [];
            }

            // Find the specific decision event for THIS task instance
            const decision = findDecisionForInstance(tc);
            let status = 'active'; // default = waiting
            let details = null;

            if (decision) {
                if (decision.actionType === 'TASK_COMPLETED') status = 'completed';
                else if (decision.actionType === 'TASK_CANCELLED') status = 'cancelled';
                else if (decision.actionType === 'TASK_SENT_BACK') status = 'sent-back';
                else if (decision.actionType === 'TASK_REJECTED') status = 'rejected';
                details = {
                    action: decision.metadata?.action || decision.actionLabel || decision.actionType?.replace('TASK_', ''),
                    actor: decision.actorName,
                    date: decision.createdAt,
                };
            }

            currentLevel.push({
                name: tc.taskName || 'Unknown',
                status,
                createdAt: tc.createdAt,
                details,
            });
            lastTime = t;
        });
        if (currentLevel.length > 0) levels.push(currentLevel);

        return { levels, processCompleted: !!processCompleted, processCancelled: !!processCancelled };
    }, [events]);

    const { levels, processCompleted, processCancelled } = flowLevels;

    if (levels.length === 0) {
        return (
            <div className="text-center py-8 text-slate-400 text-xs">
                <GitBranch className="w-6 h-6 mx-auto mb-2 text-slate-300" />
                No task flow data available
            </div>
        );
    }

    const statusStyles = {
        'completed': { border: 'border-green-400', bg: 'bg-green-50', text: 'text-green-700', ring: 'ring-green-200', icon: CheckCircle, iconColor: 'text-green-500' },
        'active': { border: 'border-blue-400', bg: 'bg-blue-50', text: 'text-blue-700', ring: 'ring-blue-200 ring-2 animate-pulse', icon: Clock, iconColor: 'text-blue-500' },
        'cancelled': { border: 'border-slate-300', bg: 'bg-slate-50', text: 'text-slate-400 line-through', ring: '', icon: Ban, iconColor: 'text-slate-400' },
        'sent-back': { border: 'border-orange-400', bg: 'bg-orange-50', text: 'text-orange-700', ring: 'ring-orange-200', icon: CornerUpLeft, iconColor: 'text-orange-500' },
        'rejected': { border: 'border-red-400', bg: 'bg-red-50', text: 'text-red-700', ring: 'ring-red-200', icon: XCircle, iconColor: 'text-red-500' },
    };

    return (
        <div className="overflow-x-auto py-6 px-2">
            <div className="flex items-center gap-0 min-w-max mx-auto justify-center">

                {/* ── Start Circle ── */}
                <div className="flex flex-col items-center">
                    <div className="w-10 h-10 rounded-full bg-green-500 border-2 border-green-600 flex items-center justify-center shadow-md">
                        <Play className="w-4 h-4 text-white ml-0.5" />
                    </div>
                    <span className="text-[10px] text-slate-500 mt-1.5 font-medium">Start</span>
                </div>

                {levels.map((level, levelIdx) => (
                    <React.Fragment key={levelIdx}>
                        {/* ── Connector arrow ── */}
                        <div className="flex items-center">
                            <div className="w-8 h-[2px] bg-slate-300" />
                            {level.length > 1 && (
                                /* Parallel gateway diamond */
                                <div className="relative">
                                    <div className="w-7 h-7 bg-amber-100 border-2 border-amber-400 rotate-45 shadow-sm" />
                                    <span className="absolute inset-0 flex items-center justify-center text-amber-600 text-[10px] font-bold">+</span>
                                </div>
                            )}
                            <div className="w-8 h-[2px] bg-slate-300" />
                            {level.length > 1 && (
                                <svg className="absolute" style={{ left: 0, top: 0, width: 0, height: 0, overflow: 'visible' }}>
                                </svg>
                            )}
                        </div>

                        {/* ── Task nodes (stacked vertically if parallel) ── */}
                        <div className={`flex flex-col items-center ${level.length > 1 ? 'gap-3' : ''}`}>
                            {level.map((task, taskIdx) => {
                                const style = statusStyles[task.status];
                                const StatusIcon = style.icon;
                                return (
                                    <div key={taskIdx} className="flex flex-col items-center group relative">
                                        {/* Vertical connector for parallel tasks */}
                                        {level.length > 1 && taskIdx > 0 && (
                                            <div className="w-[2px] h-3 bg-slate-300 mb-0" />
                                        )}
                                        {/* Task node */}
                                        <div className={`
                                            relative px-5 py-3 rounded-lg border-2 ${style.border} ${style.bg} ${style.ring}
                                            shadow-sm min-w-[120px] text-center transition-all
                                            hover:shadow-md cursor-default
                                        `}>
                                            <div className="flex items-center justify-center gap-1.5">
                                                <StatusIcon className={`w-3.5 h-3.5 ${style.iconColor}`} />
                                                <span className={`text-sm font-semibold ${style.text}`}>
                                                    {task.name}
                                                </span>
                                            </div>
                                            {task.details && (
                                                <div className="mt-1.5 text-[10px] text-slate-500 space-y-0.5">
                                                    {task.details.actor && (
                                                        <div className="flex items-center justify-center gap-1">
                                                            <User className="w-2.5 h-2.5" />
                                                            <span>{task.details.actor}</span>
                                                        </div>
                                                    )}
                                                    <div className="text-[9px] text-slate-400">
                                                        {task.details.action}
                                                    </div>
                                                </div>
                                            )}
                                            {task.status === 'active' && (
                                                <div className="absolute -top-1.5 -right-1.5 w-3 h-3 rounded-full bg-blue-500 border-2 border-white animate-pulse" />
                                            )}
                                        </div>
                                        {/* Vertical connector for parallel tasks */}
                                        {level.length > 1 && taskIdx < level.length - 1 && (
                                            <div className="w-[2px] h-3 bg-slate-300 mt-0" />
                                        )}
                                    </div>
                                );
                            })}
                        </div>

                        {/* ── Closing gateway for parallel ── */}
                        {level.length > 1 && (
                            <div className="flex items-center">
                                <div className="w-8 h-[2px] bg-slate-300" />
                                <div className="relative">
                                    <div className="w-7 h-7 bg-amber-100 border-2 border-amber-400 rotate-45 shadow-sm" />
                                    <span className="absolute inset-0 flex items-center justify-center text-amber-600 text-[10px] font-bold">+</span>
                                </div>
                                {levelIdx < levels.length - 1 && <div className="w-8 h-[2px] bg-slate-300" />}
                            </div>
                        )}
                    </React.Fragment>
                ))}

                {/* ── End connector + End Circle ── */}
                <div className="flex items-center">
                    <div className="w-8 h-[2px] bg-slate-300" />
                </div>
                <div className="flex flex-col items-center">
                    {processCompleted ? (
                        <>
                            <div className="w-10 h-10 rounded-full bg-red-500 border-[3px] border-red-600 flex items-center justify-center shadow-md">
                                <div className="w-4 h-4 rounded-full border-2 border-white" />
                            </div>
                            <span className="text-[10px] text-slate-500 mt-1.5 font-medium">End</span>
                        </>
                    ) : processCancelled ? (
                        <>
                            <div className="w-10 h-10 rounded-full bg-slate-400 border-[3px] border-slate-500 flex items-center justify-center shadow-md">
                                <XCircle className="w-4 h-4 text-white" />
                            </div>
                            <span className="text-[10px] text-slate-500 mt-1.5 font-medium">Cancelled</span>
                        </>
                    ) : (
                        <>
                            <div className="w-10 h-10 rounded-full border-2 border-dashed border-slate-300 flex items-center justify-center bg-white">
                                <Clock className="w-4 h-4 text-slate-300" />
                            </div>
                            <span className="text-[10px] text-slate-400 mt-1.5 font-medium">In Progress</span>
                        </>
                    )}
                </div>
            </div>

            {/* Legend */}
            <div className="flex items-center justify-center gap-4 mt-6 text-[10px] text-slate-400">
                <div className="flex items-center gap-1"><div className="w-3 h-3 rounded bg-green-50 border border-green-400" /><span>Completed</span></div>
                <div className="flex items-center gap-1"><div className="w-3 h-3 rounded bg-blue-50 border border-blue-400" /><span>Active</span></div>
                <div className="flex items-center gap-1"><div className="w-3 h-3 rounded bg-orange-50 border border-orange-400" /><span>Sent Back</span></div>
                <div className="flex items-center gap-1"><div className="w-3 h-3 rounded bg-slate-50 border border-slate-300" /><span>Cancelled</span></div>
                <div className="flex items-center gap-1"><div className="w-5 h-5 bg-amber-100 border border-amber-400 rotate-45 scale-75" /><span>Gateway</span></div>
            </div>
        </div>
    );
}

// ─── Timeline Row Component ────────────────────────────────────────────
function TimelineRow({ event, isLast, index, isLastDecision }) {
    const config = getConfig(event.actionType);
    const IconComponent = config.icon;

    const isDecision = ['TASK_COMPLETED', 'TASK_REJECTED', 'TASK_SENT_BACK'].includes(event.actionType);
    const isSystemEvent = ['PROCESS_STARTED', 'PROCESS_COMPLETED', 'PROCESS_CANCELLED', 'MEMO_SUBMITTED'].includes(event.actionType);
    const showNextStep = isDecision || isSystemEvent;

    const commentText = event.metadata?.comment || event.metadata?.reason || null;
    const eventTitle = isSystemEvent ? config.label : (event.taskName || config.label);
    const decisionLabel = event.actionLabel || config.label;

    const sentBackTo = event.metadata?.sentBackTo;
    const cancelledTasks = event.metadata?.cancelledTasks;

    const nextSteps = event.nextSteps;
    const hasNextSteps = nextSteps && nextSteps.length > 0;

    return (
        <div className="group">
            <div className={`relative flex items-stretch transition-colors ${isDecision ? 'hover:bg-slate-50/80' : ''}`}>
                <div className="relative flex flex-col items-center w-10 shrink-0">
                    {index > 0 && <div className="w-[2px] flex-1 bg-slate-200" />}
                    {index === 0 && <div className="flex-1" />}
                    <div className={`relative z-10 flex items-center justify-center w-7 h-7 rounded-full border-2 ${config.border} bg-white shrink-0 shadow-sm`}>
                        <IconComponent className={`w-3.5 h-3.5 ${config.color}`} />
                    </div>
                    {!isLast ? <div className="w-[2px] flex-1 bg-slate-200" /> : <div className="flex-1" />}
                </div>

                <div className={`flex-1 grid grid-cols-12 gap-2 items-start py-2.5 px-3 border-b border-slate-100 ${isSystemEvent ? 'bg-slate-50/50' : ''}`}>
                    <div className="col-span-4 min-w-0">
                        <div className="flex items-center gap-2 flex-wrap">
                            <span className={`text-sm font-medium truncate ${isSystemEvent ? 'text-slate-500 italic' : 'text-slate-800'}`}>{eventTitle}</span>
                            {isDecision && (
                                <span className={`inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-semibold uppercase tracking-wide ${config.badge}`}>{decisionLabel}</span>
                            )}
                        </div>
                        {commentText && (
                            <div className="flex items-start gap-1.5 mt-1">
                                <MessageSquare className="w-3 h-3 text-slate-400 mt-0.5 shrink-0" />
                                <p className="text-[11px] text-slate-500 italic line-clamp-2">{commentText}</p>
                            </div>
                        )}
                        {sentBackTo && (
                            <div className="flex items-center gap-1.5 mt-1 text-[10px]">
                                <ArrowRight className="w-3 h-3 text-orange-400 shrink-0" />
                                <span className="text-slate-400">To:</span>
                                <span className="text-slate-600 font-medium truncate">{Array.isArray(sentBackTo) ? sentBackTo.join(', ') : sentBackTo}</span>
                            </div>
                        )}
                        {cancelledTasks && (
                            <div className="flex items-center gap-1.5 mt-1 text-[10px]">
                                <Ban className="w-3 h-3 text-slate-400 shrink-0" />
                                <span className="text-slate-400">Cancelled:</span>
                                <span className="text-slate-500 truncate">{Array.isArray(cancelledTasks) ? cancelledTasks.join(', ') : cancelledTasks}</span>
                            </div>
                        )}
                    </div>

                    <div className="col-span-2 min-w-0 pt-0.5">
                        {event.actorName ? (
                            <div className="flex items-center gap-1.5">
                                <div className="w-5 h-5 rounded-full bg-slate-200 flex items-center justify-center text-[9px] font-bold text-slate-500 shrink-0">{event.actorName.charAt(0).toUpperCase()}</div>
                                <span className="text-xs text-slate-600 truncate">{event.actorName}</span>
                            </div>
                        ) : (
                            <span className="text-xs text-slate-300 italic">System</span>
                        )}
                    </div>

                    <div className="col-span-2 min-w-0 pt-0.5">
                        {showNextStep && (
                            hasNextSteps ? (
                                <div className="flex items-center gap-1 text-[11px] text-slate-600">
                                    <ChevronsRight className="w-3 h-3 text-slate-400 shrink-0" />
                                    <span className="truncate font-medium" title={nextSteps.join(', ')}>{nextSteps.join(', ')}</span>
                                </div>
                            ) : isLastDecision ? (
                                <div className="flex items-center gap-1 text-[11px] text-amber-500">
                                    <Loader2 className="w-3 h-3 animate-spin shrink-0" />
                                    <span className="font-medium">Waiting…</span>
                                </div>
                            ) : null
                        )}
                    </div>

                    <div className="col-span-2 text-right pt-0.5">
                        {event.createdAt && (
                            <div>
                                <div className="text-[11px] font-medium text-slate-700">{formatAbsoluteDate(event.createdAt)}</div>
                                <div className="text-[10px] text-slate-400">{formatRelativeTime(event.createdAt)}</div>
                            </div>
                        )}
                    </div>

                    <div className="col-span-2 text-right pt-0.5">
                        {event.durationMs != null ? (
                            <div className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded bg-slate-100 text-slate-600">
                                <Timer className="w-3 h-3" />
                                <span className="text-[11px] font-medium">{formatDuration(event.durationMs)}</span>
                            </div>
                        ) : (
                            isDecision ? <span className="text-[10px] text-slate-300">—</span> : null
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}

// ─── Main Component ─────────────────────────────────────────────────────
const ExecutionHistory = ({ memoId, className = "" }) => {
    const [events, setEvents] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [activeFilter, setActiveFilter] = useState('all');

    useEffect(() => {
        if (memoId) { loadHistory(); }
    }, [memoId]);

    const loadHistory = async () => {
        try {
            setLoading(true);
            setError(null);
            const data = await HistoryApi.getTimeline(memoId);
            setEvents(data || []);
        } catch (err) {
            console.error('Failed to load execution history:', err);
            setError('Failed to load execution history');
        } finally {
            setLoading(false);
        }
    };

    const filteredEvents = useMemo(() => {
        const allowedTypes = FILTER_MAP[activeFilter];
        if (!allowedTypes) return events;
        return events.filter(e => allowedTypes.includes(e.actionType));
    }, [events, activeFilter]);

    const lastDecisionIndex = useMemo(() => {
        for (let i = events.length - 1; i >= 0; i--) {
            if (['TASK_COMPLETED', 'TASK_REJECTED', 'TASK_SENT_BACK'].includes(events[i].actionType)) return i;
        }
        return -1;
    }, [events]);

    const stats = useMemo(() => {
        const decisions = events.filter(e => ['TASK_COMPLETED', 'TASK_REJECTED', 'TASK_SENT_BACK'].includes(e.actionType));
        const totalDurationMs = decisions.reduce((sum, e) => sum + (e.durationMs || 0), 0);
        let elapsedMs = null;
        if (events.length >= 2) {
            elapsedMs = new Date(events[events.length - 1].createdAt) - new Date(events[0].createdAt);
        }
        return {
            totalSteps: decisions.length,
            totalDurationMs,
            elapsedMs,
            sendBacks: events.filter(e => e.actionType === 'TASK_SENT_BACK').length,
            isCompleted: events.some(e => e.actionType === 'PROCESS_COMPLETED'),
        };
    }, [events]);

    const isTaskFlowView = activeFilter === 'taskflow';

    if (!memoId) {
        return (
            <div className={`text-center py-16 ${className}`}>
                <FileText className="w-12 h-12 text-slate-300 mx-auto mb-3" />
                <p className="text-slate-400 text-sm">This memo hasn't been submitted yet.</p>
                <p className="text-slate-300 text-xs mt-1">Execution history will appear here once the workflow begins.</p>
            </div>
        );
    }

    return (
        <div className={className}>
            <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2.5">
                    <h3 className="text-sm font-semibold text-slate-800">Execution History</h3>
                    {!loading && events.length > 0 && (
                        <span className="bg-slate-100 text-slate-500 text-[10px] px-1.5 py-0.5 rounded-full font-medium">{filteredEvents.length} events</span>
                    )}
                </div>
                <button onClick={loadHistory} disabled={loading} className="p-1.5 rounded-md hover:bg-slate-100 text-slate-400 hover:text-slate-600 transition-colors disabled:opacity-50" title="Refresh">
                    <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
                </button>
            </div>

            {!loading && events.length > 0 && !isTaskFlowView && (
                <div className="flex items-center gap-3 mb-3 px-3 py-2 bg-gradient-to-r from-slate-50 to-white rounded-lg border border-slate-100 text-[11px] text-slate-500">
                    <div className="flex items-center gap-1.5">
                        <CheckCircle className="w-3 h-3 text-green-500" />
                        <span><strong className="text-slate-700">{stats.totalSteps}</strong> decision{stats.totalSteps !== 1 ? 's' : ''}</span>
                    </div>
                    {stats.totalDurationMs > 0 && (
                        <><div className="w-px h-3.5 bg-slate-200" /><div className="flex items-center gap-1.5"><Timer className="w-3 h-3 text-blue-500" /><span>Processing: <strong className="text-slate-700">{formatDuration(stats.totalDurationMs)}</strong></span></div></>
                    )}
                    {stats.elapsedMs != null && (
                        <><div className="w-px h-3.5 bg-slate-200" /><div className="flex items-center gap-1.5"><Clock className="w-3 h-3 text-slate-400" /><span>Elapsed: <strong className="text-slate-700">{formatDuration(stats.elapsedMs)}</strong></span></div></>
                    )}
                    {stats.sendBacks > 0 && (
                        <><div className="w-px h-3.5 bg-slate-200" /><div className="flex items-center gap-1.5"><CornerUpLeft className="w-3 h-3 text-orange-500" /><span><strong className="text-slate-700">{stats.sendBacks}</strong> send-back{stats.sendBacks > 1 ? 's' : ''}</span></div></>
                    )}
                    {stats.isCompleted && (<><div className="w-px h-3.5 bg-slate-200" /><span className="text-emerald-600 font-semibold">✓ Completed</span></>)}
                </div>
            )}

            {!loading && events.length > 0 && (
                <div className="flex items-center gap-1.5 mb-3">
                    <Filter className="w-3 h-3 text-slate-400 mr-0.5" />
                    {FILTERS.map(f => (
                        <button key={f.key} onClick={() => setActiveFilter(f.key)}
                            className={`px-2 py-0.5 rounded text-[10px] font-medium transition-all ${activeFilter === f.key ? 'bg-slate-800 text-white' : 'bg-white text-slate-500 border border-slate-200 hover:border-slate-300'}`}
                        >
                            {f.key === 'taskflow' && <GitBranch className="w-3 h-3 inline mr-0.5 -mt-px" />}
                            {f.label}
                        </button>
                    ))}
                </div>
            )}

            {!loading && filteredEvents.length > 0 && !isTaskFlowView && (
                <div className="flex">
                    <div className="w-10 shrink-0" />
                    <div className="flex-1 grid grid-cols-12 gap-2 px-3 py-1.5 border-b border-slate-200 bg-slate-50 rounded-t-lg">
                        <div className="col-span-4 text-[10px] font-semibold text-slate-400 uppercase tracking-wider">Step</div>
                        <div className="col-span-2 text-[10px] font-semibold text-slate-400 uppercase tracking-wider">Actor</div>
                        <div className="col-span-2 text-[10px] font-semibold text-slate-400 uppercase tracking-wider">Active Steps</div>
                        <div className="col-span-2 text-[10px] font-semibold text-slate-400 uppercase tracking-wider text-right">Date / Time</div>
                        <div className="col-span-2 text-[10px] font-semibold text-slate-400 uppercase tracking-wider text-right">Duration</div>
                    </div>
                </div>
            )}

            {loading && (
                <div className="space-y-3 animate-pulse mt-2">
                    {[1, 2, 3, 4].map(i => (
                        <div key={i} className="flex gap-3 items-center">
                            <div className="w-7 h-7 rounded-full bg-slate-200 shrink-0 ml-1.5" />
                            <div className="flex-1 h-12 bg-slate-100 rounded" />
                        </div>
                    ))}
                </div>
            )}

            {error && (
                <div className="flex items-center gap-2 p-3 bg-red-50 border border-red-200 rounded-lg text-xs text-red-600 mt-2">
                    <XCircle className="w-4 h-4 shrink-0" /><span>{error}</span>
                    <button onClick={loadHistory} className="ml-auto text-[10px] underline hover:no-underline">Retry</button>
                </div>
            )}

            {!loading && !error && (
                isTaskFlowView ? (
                    <div className="border border-slate-200 rounded-lg bg-white overflow-hidden">
                        <TaskFlowDiagram events={events} />
                    </div>
                ) : (
                    <div className="border border-slate-200 rounded-b-lg rounded-t-none overflow-hidden">
                        {filteredEvents.length > 0 ? (
                            filteredEvents.map((event, index) => {
                                const originalIndex = events.indexOf(event);
                                return (
                                    <TimelineRow key={event.id || index} event={event} isLast={index === filteredEvents.length - 1} index={index} isLastDecision={originalIndex === lastDecisionIndex} />
                                );
                            })
                        ) : (
                            <div className="text-center py-8">
                                <Clock className="w-7 h-7 text-slate-300 mx-auto mb-2" />
                                <p className="text-slate-400 text-xs">
                                    {activeFilter === 'all' ? 'No history events recorded yet' : `No ${FILTERS.find(f => f.key === activeFilter)?.label.toLowerCase()} events`}
                                </p>
                            </div>
                        )}
                    </div>
                )
            )}
        </div>
    );
};

export default ExecutionHistory;
