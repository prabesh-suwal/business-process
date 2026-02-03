import { useState, useEffect, useCallback } from 'react';
import { TaskApi } from '../lib/api';

/**
 * Custom hook for fetching and tracking parallel execution status.
 * 
 * Provides:
 * - Status data (branches completed, active tasks, etc.)
 * - Loading state
 * - Auto-refresh capability with configurable interval
 * - Computed properties for UI display
 */
export function useParallelStatus(memoIdOrProcessInstanceId, {
    isMemoId = true,       // Whether the ID is a memo ID (true) or process instance ID (false)
    autoRefresh = true,     // Auto-refresh status
    refreshInterval = 5000  // Refresh every 5 seconds
} = {}) {
    const [status, setStatus] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const fetchStatus = useCallback(async () => {
        if (!memoIdOrProcessInstanceId) {
            setLoading(false);
            return;
        }

        try {
            const data = isMemoId
                ? await TaskApi.getParallelStatusByMemo(memoIdOrProcessInstanceId)
                : await TaskApi.getParallelStatus(memoIdOrProcessInstanceId);

            setStatus(data);
            setError(null);
        } catch (err) {
            console.warn('Error fetching parallel status:', err);
            setError(err);
            // Set a default non-parallel status on error
            setStatus({
                isInParallelExecution: false,
                totalBranches: 0,
                completedBranches: 0,
                activeBranches: 0,
                activeTasks: []
            });
        } finally {
            setLoading(false);
        }
    }, [memoIdOrProcessInstanceId, isMemoId]);

    // Initial fetch
    useEffect(() => {
        fetchStatus();
    }, [fetchStatus]);

    // Auto-refresh
    useEffect(() => {
        if (!autoRefresh || !memoIdOrProcessInstanceId) return;

        const interval = setInterval(fetchStatus, refreshInterval);
        return () => clearInterval(interval);
    }, [autoRefresh, refreshInterval, fetchStatus, memoIdOrProcessInstanceId]);

    // Computed properties
    const isParallel = status?.isInParallelExecution ?? false;
    const progress = status?.totalBranches > 0
        ? Math.round((status.completedBranches / status.totalBranches) * 100)
        : 0;
    const hasActiveTasks = (status?.activeTasks?.length ?? 0) > 0;
    const statusMessage = status?.statusMessage || generateMessage(status);

    return {
        // Raw data
        status,
        loading,
        error,

        // Computed
        isParallel,
        progress,
        hasActiveTasks,
        statusMessage,

        // Actions
        refresh: fetchStatus
    };
}

function generateMessage(status) {
    if (!status || !status.isInParallelExecution) {
        return 'Sequential workflow';
    }
    const { completedBranches, totalBranches, activeBranches } = status;
    if (completedBranches === 0) {
        return `Parallel approval: ${activeBranches} branches active`;
    }
    if (completedBranches === totalBranches) {
        return 'All parallel branches completed';
    }
    return `${completedBranches} of ${totalBranches} branches completed`;
}

/**
 * Hook for fetching active tasks in a parallel execution
 */
export function useActiveTasks(processInstanceId, { autoRefresh = true, refreshInterval = 5000 } = {}) {
    const [tasks, setTasks] = useState([]);
    const [loading, setLoading] = useState(true);

    const fetchTasks = useCallback(async () => {
        if (!processInstanceId) {
            setLoading(false);
            return;
        }

        try {
            const data = await TaskApi.getActiveTasks(processInstanceId);
            setTasks(data || []);
        } catch (err) {
            console.warn('Error fetching active tasks:', err);
            setTasks([]);
        } finally {
            setLoading(false);
        }
    }, [processInstanceId]);

    useEffect(() => {
        fetchTasks();
    }, [fetchTasks]);

    useEffect(() => {
        if (!autoRefresh || !processInstanceId) return;
        const interval = setInterval(fetchTasks, refreshInterval);
        return () => clearInterval(interval);
    }, [autoRefresh, refreshInterval, fetchTasks, processInstanceId]);

    return { tasks, loading, refresh: fetchTasks };
}

export default useParallelStatus;
