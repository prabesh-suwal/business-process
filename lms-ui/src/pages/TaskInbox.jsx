import { useState, useEffect } from 'react';
import { tasks } from '../api';

export default function TaskInbox() {
    const [myTasks, setMyTasks] = useState([]);
    const [claimableTasks, setClaimableTasks] = useState([]);
    const [loading, setLoading] = useState(true);
    const [activeTab, setActiveTab] = useState('my'); // 'my' | 'claimable'
    const [selectedTask, setSelectedTask] = useState(null);
    const [actionLoading, setActionLoading] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        loadTasks();
    }, []);

    const loadTasks = async () => {
        try {
            setLoading(true);
            const [my, claimable] = await Promise.all([
                tasks.getMyTasks().catch(() => []),
                tasks.getClaimableTasks().catch(() => [])
            ]);
            setMyTasks(my || []);
            setClaimableTasks(claimable || []);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    const handleClaim = async (taskId) => {
        try {
            setActionLoading(true);
            await tasks.claim(taskId);
            await loadTasks();
            setSelectedTask(null);
        } catch (err) {
            setError(err.message);
        } finally {
            setActionLoading(false);
        }
    };

    const handleUnclaim = async (taskId) => {
        try {
            setActionLoading(true);
            await tasks.unclaim(taskId);
            await loadTasks();
            setSelectedTask(null);
        } catch (err) {
            setError(err.message);
        } finally {
            setActionLoading(false);
        }
    };

    const handleComplete = async (taskId) => {
        try {
            setActionLoading(true);
            await tasks.complete(taskId, {});
            await loadTasks();
            setSelectedTask(null);
        } catch (err) {
            setError(err.message);
        } finally {
            setActionLoading(false);
        }
    };

    const formatDate = (dateString) => {
        if (!dateString) return '-';
        return new Date(dateString).toLocaleString();
    };

    const getPriorityBadge = (priority) => {
        const map = {
            high: 'badge-rejected',
            medium: 'badge-pending',
            low: 'badge-draft'
        };
        return map[priority?.toLowerCase()] || 'badge-draft';
    };

    const currentTasks = activeTab === 'my' ? myTasks : claimableTasks;

    return (
        <div className="main-content">
            <div className="page-header">
                <h1 className="page-title">Task Inbox</h1>
                <button className="btn btn-secondary" onClick={loadTasks} disabled={loading}>
                    🔄 Refresh
                </button>
            </div>

            {error && <div className="form-error" style={{ marginBottom: '1rem' }}>{error}</div>}

            {/* Tabs */}
            <div className="tabs" style={{ marginBottom: 'var(--spacing-4)' }}>
                <button
                    className={`tab-btn ${activeTab === 'my' ? 'active' : ''}`}
                    onClick={() => setActiveTab('my')}
                >
                    📋 My Tasks ({myTasks.length})
                </button>
                <button
                    className={`tab-btn ${activeTab === 'claimable' ? 'active' : ''}`}
                    onClick={() => setActiveTab('claimable')}
                >
                    📥 Available to Claim ({claimableTasks.length})
                </button>
            </div>

            {/* Task List */}
            <div className="card">
                {loading ? (
                    <p className="text-muted">Loading tasks...</p>
                ) : currentTasks.length === 0 ? (
                    <div style={{ textAlign: 'center', padding: 'var(--spacing-8)' }}>
                        <p style={{ fontSize: '3rem', marginBottom: 'var(--spacing-2)' }}>
                            {activeTab === 'my' ? '✅' : '📭'}
                        </p>
                        <p className="text-muted">
                            {activeTab === 'my'
                                ? 'No tasks assigned to you. Check available tasks to claim.'
                                : 'No tasks available to claim at the moment.'}
                        </p>
                    </div>
                ) : (
                    <div className="table-container">
                        <table>
                            <thead>
                                <tr>
                                    <th>Task</th>
                                    <th>Application</th>
                                    <th>Priority</th>
                                    <th>Created</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {currentTasks.map((task) => (
                                    <tr key={task.id}>
                                        <td>
                                            <div>
                                                <strong>{task.name || 'Task'}</strong>
                                                {task.description && (
                                                    <div className="text-muted" style={{ fontSize: '0.75rem' }}>
                                                        {task.description}
                                                    </div>
                                                )}
                                            </div>
                                        </td>
                                        <td>
                                            <span style={{ fontFamily: 'monospace' }}>
                                                {task.processInstanceBusinessKey || task.processInstanceId?.slice(0, 8) || '-'}
                                            </span>
                                        </td>
                                        <td>
                                            <span className={`badge ${getPriorityBadge(task.priority)}`}>
                                                {task.priority || 'Normal'}
                                            </span>
                                        </td>
                                        <td className="text-muted" style={{ fontSize: '0.875rem' }}>
                                            {formatDate(task.createTime)}
                                        </td>
                                        <td>
                                            <div style={{ display: 'flex', gap: 'var(--spacing-2)' }}>
                                                {activeTab === 'claimable' ? (
                                                    <button
                                                        className="btn btn-primary"
                                                        onClick={() => handleClaim(task.id)}
                                                        disabled={actionLoading}
                                                    >
                                                        Claim
                                                    </button>
                                                ) : (
                                                    <>
                                                        <button
                                                            className="btn btn-success"
                                                            onClick={() => setSelectedTask(task)}
                                                        >
                                                            Complete
                                                        </button>
                                                        <button
                                                            className="btn btn-secondary"
                                                            onClick={() => handleUnclaim(task.id)}
                                                            disabled={actionLoading}
                                                        >
                                                            Release
                                                        </button>
                                                    </>
                                                )}
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>

            {/* Complete Task Modal */}
            {selectedTask && (
                <div className="modal-overlay" onClick={() => setSelectedTask(null)}>
                    <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: '500px' }}>
                        <div className="modal-header">
                            <h3>Complete Task</h3>
                            <button className="btn btn-secondary" onClick={() => setSelectedTask(null)}>✕</button>
                        </div>
                        <div className="modal-body">
                            <p><strong>Task:</strong> {selectedTask.name}</p>
                            <p><strong>Application:</strong> {selectedTask.processInstanceBusinessKey || '-'}</p>

                            <div className="form-group" style={{ marginTop: 'var(--spacing-4)' }}>
                                <label className="form-label">Comments (optional)</label>
                                <textarea
                                    className="form-input"
                                    rows={3}
                                    placeholder="Add any notes about this task..."
                                />
                            </div>
                        </div>
                        <div className="modal-footer">
                            <button className="btn btn-secondary" onClick={() => setSelectedTask(null)}>
                                Cancel
                            </button>
                            <button
                                className="btn btn-success"
                                onClick={() => handleComplete(selectedTask.id)}
                                disabled={actionLoading}
                            >
                                {actionLoading ? 'Completing...' : 'Complete Task'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
