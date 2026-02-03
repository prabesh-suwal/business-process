import { useState, useEffect } from 'react';
import {
    GitBranch,
    Check,
    CheckCircle,
    Clock,
    Loader2,
    ChevronDown,
    ChevronRight,
    User,
    Users,
    AlertCircle,
    Sparkles,
    ArrowRight,
    CircleDot
} from 'lucide-react';

/**
 * ParallelExecutionProgress - Premium visual indicator for parallel approval workflows
 * 
 * Designed for non-technical business users with:
 * - Clear, jargon-free language
 * - Visual progress indicators
 * - Animated effects for engagement
 * - Expandable details for more context
 */
export default function ParallelExecutionProgress({
    parallelStatus,     // ParallelExecutionStatusDTO from API
    currentUserId,      // Current logged-in user ID
    onTaskClick,        // (taskId) => void - Navigate to task
    compact = false,    // Compact mode for sidebar
    className = ''
}) {
    const [expanded, setExpanded] = useState(!compact);

    if (!parallelStatus || !parallelStatus.isInParallelExecution) {
        return null; // Don't render if not in parallel execution
    }

    const {
        totalBranches,
        completedBranches,
        activeBranches,
        activeTasks = [],
        statusMessage,
        progressPercent,
        maxNestingDepth
    } = parallelStatus;

    // Calculate progress percentage
    const progress = totalBranches > 0
        ? Math.round((completedBranches / totalBranches) * 100)
        : 0;

    // Find if current user has a task
    const userTask = activeTasks.find(t => t.assignee === currentUserId);
    const hasUserAction = !!userTask;

    // Generate friendly message
    const getFriendlyMessage = () => {
        if (completedBranches === 0) {
            return `Waiting for ${activeBranches} ${activeBranches === 1 ? 'approval' : 'approvals'} to be completed`;
        }
        if (completedBranches === totalBranches) {
            return 'All approvals completed';
        }
        const remaining = totalBranches - completedBranches;
        return `${completedBranches} of ${totalBranches} approvals done • ${remaining} remaining`;
    };

    if (compact) {
        return (
            <div className={`parallel-progress-compact ${className}`}>
                <div className="flex items-center gap-3 p-3 bg-gradient-to-r from-purple-50 to-indigo-50 rounded-xl border border-purple-100 shadow-sm">
                    <div className="relative">
                        <div className="w-10 h-10 rounded-full bg-gradient-to-br from-purple-500 to-indigo-500 flex items-center justify-center">
                            <GitBranch className="w-5 h-5 text-white" />
                        </div>
                        {hasUserAction && (
                            <span className="absolute -top-1 -right-1 w-4 h-4 bg-red-500 rounded-full flex items-center justify-center animate-pulse">
                                <span className="text-white text-[10px] font-bold">!</span>
                            </span>
                        )}
                    </div>
                    <div className="flex-1 min-w-0">
                        <div className="text-sm font-semibold text-purple-900">
                            Multiple Approvals Required
                        </div>
                        <div className="text-xs text-purple-600 mt-0.5">
                            {getFriendlyMessage()}
                        </div>
                        {/* Mini progress bar */}
                        <div className="mt-2 h-1.5 bg-purple-200 rounded-full overflow-hidden">
                            <div
                                className="h-full bg-gradient-to-r from-purple-500 to-indigo-500 transition-all duration-700 ease-out"
                                style={{ width: `${progress}%` }}
                            />
                        </div>
                    </div>
                    <div className="text-lg font-bold text-purple-700">
                        {completedBranches}/{totalBranches}
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className={`parallel-progress ${className}`}>
            <div className="bg-white rounded-2xl border border-purple-100 shadow-lg shadow-purple-100/50 overflow-hidden">
                {/* Header with gradient */}
                <div
                    className="relative overflow-hidden cursor-pointer"
                    onClick={() => setExpanded(!expanded)}
                >
                    {/* Background gradient */}
                    <div className="absolute inset-0 bg-gradient-to-r from-purple-500 via-indigo-500 to-purple-600 opacity-95" />

                    {/* Decorative patterns */}
                    <div className="absolute inset-0 opacity-10">
                        <div className="absolute top-0 right-0 w-40 h-40 bg-white rounded-full -translate-y-1/2 translate-x-1/2" />
                        <div className="absolute bottom-0 left-0 w-32 h-32 bg-white rounded-full translate-y-1/2 -translate-x-1/2" />
                    </div>

                    <div className="relative flex items-center justify-between p-5">
                        <div className="flex items-center gap-4">
                            <div className="relative">
                                <div className="w-14 h-14 rounded-2xl bg-white/20 backdrop-blur-sm flex items-center justify-center border border-white/30">
                                    <GitBranch className="w-7 h-7 text-white" />
                                </div>
                                {hasUserAction && (
                                    <span className="absolute -top-1 -right-1 w-5 h-5 bg-red-500 rounded-full flex items-center justify-center border-2 border-white animate-bounce">
                                        <span className="text-white text-[10px] font-bold">!</span>
                                    </span>
                                )}
                            </div>
                            <div>
                                <h4 className="text-lg font-bold text-white">
                                    Multiple Approvals in Progress
                                </h4>
                                <p className="text-white/80 text-sm mt-0.5">
                                    {getFriendlyMessage()}
                                </p>
                            </div>
                        </div>

                        <div className="flex items-center gap-5">
                            {/* Circular Progress */}
                            <div className="relative w-16 h-16">
                                <svg className="w-full h-full -rotate-90">
                                    <circle
                                        cx="32"
                                        cy="32"
                                        r="26"
                                        fill="none"
                                        stroke="rgba(255,255,255,0.2)"
                                        strokeWidth="5"
                                    />
                                    <circle
                                        cx="32"
                                        cy="32"
                                        r="26"
                                        fill="none"
                                        stroke="white"
                                        strokeWidth="5"
                                        strokeLinecap="round"
                                        strokeDasharray={`${progress * 1.63} 163`}
                                        className="transition-all duration-700 ease-out"
                                    />
                                </svg>
                                <div className="absolute inset-0 flex flex-col items-center justify-center">
                                    <span className="text-xl font-bold text-white">{completedBranches}</span>
                                    <span className="text-[10px] text-white/70 -mt-0.5">of {totalBranches}</span>
                                </div>
                            </div>

                            <ChevronDown
                                className={`w-6 h-6 text-white/70 transition-transform duration-300 ${expanded ? 'rotate-180' : ''}`}
                            />
                        </div>
                    </div>
                </div>

                {/* Progress Bar */}
                <div className="px-5 py-2 bg-gradient-to-r from-purple-50 via-indigo-50 to-purple-50">
                    <div className="flex items-center gap-4">
                        <div className="flex-1">
                            <div className="h-2.5 bg-purple-100 rounded-full overflow-hidden">
                                <div
                                    className="h-full bg-gradient-to-r from-purple-500 via-indigo-500 to-pink-500 rounded-full transition-all duration-700 ease-out relative"
                                    style={{ width: `${progress}%` }}
                                >
                                    <div className="absolute inset-0 bg-gradient-to-r from-white/0 via-white/30 to-white/0 animate-shimmer" />
                                </div>
                            </div>
                        </div>
                        <span className="text-sm font-bold text-purple-700 tabular-nums min-w-[3rem] text-right">
                            {progress}%
                        </span>
                    </div>
                </div>

                {/* Expanded Content */}
                {expanded && (
                    <div className="p-5 space-y-4 animate-in slide-in-from-top-2 duration-300">
                        {/* Active Approvals Grid */}
                        <div>
                            <h5 className="text-sm font-semibold text-gray-700 mb-3 flex items-center gap-2">
                                <Clock className="w-4 h-4 text-amber-500" />
                                Pending Approvals
                            </h5>
                            <div className="grid gap-3 md:grid-cols-2">
                                {activeTasks.map((task, index) => {
                                    const isUserTask = task.assignee === currentUserId;

                                    return (
                                        <div
                                            key={task.taskId || index}
                                            onClick={() => onTaskClick?.(task.taskId)}
                                            className={`
                                                group relative p-4 rounded-xl border-2 transition-all duration-200 cursor-pointer
                                                ${isUserTask
                                                    ? 'border-purple-300 bg-purple-50 shadow-md shadow-purple-100 ring-2 ring-purple-200'
                                                    : 'border-gray-100 bg-white hover:border-purple-200 hover:shadow-md'}
                                            `}
                                        >
                                            {/* User action indicator */}
                                            {isUserTask && (
                                                <div className="absolute -top-2 -right-2">
                                                    <span className="inline-flex items-center gap-1 px-2 py-0.5 bg-gradient-to-r from-purple-500 to-indigo-500 text-white text-[10px] font-bold rounded-full shadow-lg">
                                                        <Sparkles className="w-3 h-3" />
                                                        YOUR ACTION
                                                    </span>
                                                </div>
                                            )}

                                            <div className="flex items-start gap-3">
                                                <div className={`
                                                    w-10 h-10 rounded-xl flex items-center justify-center text-sm font-bold
                                                    ${isUserTask
                                                        ? 'bg-gradient-to-br from-purple-500 to-indigo-500 text-white'
                                                        : 'bg-gray-100 text-gray-600'}
                                                `}>
                                                    {task.branchName?.charAt(0) || index + 1}
                                                </div>
                                                <div className="flex-1 min-w-0">
                                                    <div className="font-semibold text-gray-900 truncate">
                                                        {task.branchName || task.taskName || `Approval ${index + 1}`}
                                                    </div>
                                                    <div className="text-sm text-gray-500 mt-0.5 truncate">
                                                        {task.taskName}
                                                    </div>
                                                    {task.assigneeName && (
                                                        <div className="flex items-center gap-1.5 mt-2 text-xs text-gray-400">
                                                            <User className="w-3 h-3" />
                                                            Assigned to: <span className="font-medium text-gray-600">{task.assigneeName}</span>
                                                        </div>
                                                    )}
                                                </div>
                                                <ArrowRight className={`
                                                    w-5 h-5 transition-transform group-hover:translate-x-1
                                                    ${isUserTask ? 'text-purple-500' : 'text-gray-300 group-hover:text-purple-400'}
                                                `} />
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                        </div>

                        {/* Completed indicator */}
                        {completedBranches > 0 && (
                            <div className="flex items-center gap-3 p-3 bg-green-50 rounded-xl border border-green-100">
                                <div className="w-8 h-8 rounded-lg bg-green-100 flex items-center justify-center">
                                    <CheckCircle className="w-5 h-5 text-green-600" />
                                </div>
                                <div>
                                    <div className="text-sm font-semibold text-green-800">
                                        {completedBranches} approval{completedBranches > 1 ? 's' : ''} completed
                                    </div>
                                    <div className="text-xs text-green-600">
                                        {totalBranches - completedBranches === 0
                                            ? 'All required approvals are done!'
                                            : `${totalBranches - completedBranches} more needed to proceed`}
                                    </div>
                                </div>
                            </div>
                        )}

                        {/* Helpful tip for business users */}
                        <div className="flex items-start gap-2 p-3 bg-blue-50 rounded-xl text-sm text-blue-700 border border-blue-100">
                            <AlertCircle className="w-4 h-4 mt-0.5 flex-shrink-0 text-blue-500" />
                            <div>
                                <strong>How this works:</strong> This document requires approval from multiple people.
                                Each person can review and approve independently. The workflow continues once all
                                required approvals are received.
                            </div>
                        </div>
                    </div>
                )}
            </div>

            {/* CSS for shimmer animation */}
            <style>{`
                @keyframes shimmer {
                    0% { transform: translateX(-100%); }
                    100% { transform: translateX(100%); }
                }
                .animate-shimmer {
                    animation: shimmer 2s infinite;
                }
            `}</style>
        </div>
    );
}

/**
 * Mini badge for inbox list items
 */
export function ParallelProgressBadge({ parallelStatus, className = '' }) {
    if (!parallelStatus?.isInParallelExecution) return null;

    const { completedBranches, totalBranches } = parallelStatus;

    return (
        <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 bg-gradient-to-r from-purple-100 to-indigo-100 text-purple-700 text-xs font-semibold rounded-full border border-purple-200 ${className}`}>
            <GitBranch className="w-3.5 h-3.5" />
            <span>{completedBranches}/{totalBranches} approvals</span>
        </span>
    );
}

/**
 * Inline status indicator for compact displays
 */
export function ParallelStatusInline({ parallelStatus }) {
    if (!parallelStatus?.isInParallelExecution) return null;

    const { completedBranches, totalBranches, activeBranches } = parallelStatus;

    return (
        <div className="flex items-center gap-2 text-sm">
            <div className="flex items-center gap-1.5">
                <GitBranch className="w-4 h-4 text-purple-500" />
                <span className="text-gray-600">Multiple approvals:</span>
            </div>
            <div className="flex items-center gap-1">
                {[...Array(totalBranches)].map((_, i) => (
                    <CircleDot
                        key={i}
                        className={`w-4 h-4 ${i < completedBranches ? 'text-green-500' : 'text-gray-300'}`}
                    />
                ))}
            </div>
            <span className="text-gray-500 text-xs">
                ({completedBranches}/{totalBranches} done)
            </span>
        </div>
    );
}
