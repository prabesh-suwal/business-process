import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { MemoApi } from '../lib/api';
import { toast } from 'sonner';
import {
    Plus, Search, Table2, Rocket, Trash2, Copy, Archive,
    ChevronRight, Filter, MoreHorizontal
} from 'lucide-react';
import { Button } from '../components/ui/button';
import { cn } from '../lib/utils';

const statusConfig = {
    DRAFT: { label: 'Draft', color: 'bg-amber-100 text-amber-700 border-amber-200' },
    ACTIVE: { label: 'Active', color: 'bg-emerald-100 text-emerald-700 border-emerald-200' },
    DEPRECATED: { label: 'Deprecated', color: 'bg-slate-100 text-slate-500 border-slate-200' },
};

export default function DmnListPage() {
    const navigate = useNavigate();
    const [tables, setTables] = useState([]);
    const [loading, setLoading] = useState(true);
    const [search, setSearch] = useState('');
    const [statusFilter, setStatusFilter] = useState('ALL');
    const [showCreateModal, setShowCreateModal] = useState(false);

    // Create form state
    const [createForm, setCreateForm] = useState({ name: '', key: '', description: '' });
    const [creating, setCreating] = useState(false);

    const load = async () => {
        try {
            setLoading(true);
            const data = await MemoApi.listDecisionTables();
            setTables(data);
        } catch (err) {
            toast.error('Failed to load decision tables');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { load(); }, []);

    const filtered = tables.filter(t => {
        const matchSearch = !search ||
            t.name.toLowerCase().includes(search.toLowerCase()) ||
            t.key.toLowerCase().includes(search.toLowerCase());
        const matchStatus = statusFilter === 'ALL' || t.status === statusFilter;
        return matchSearch && matchStatus;
    });

    const handleCreate = async (e) => {
        e.preventDefault();
        if (!createForm.name.trim() || !createForm.key.trim()) {
            toast.error('Name and Key are required');
            return;
        }
        try {
            setCreating(true);
            const result = await MemoApi.createDecisionTable(createForm);
            toast.success(`Created "${result.name}"`);
            setShowCreateModal(false);
            setCreateForm({ name: '', key: '', description: '' });
            navigate(`/dmn/${result.id}`);
        } catch (err) {
            toast.error(err.response?.data?.message || 'Failed to create decision table');
        } finally {
            setCreating(false);
        }
    };

    const handleDeploy = async (id, name) => {
        try {
            await MemoApi.deployDecisionTable(id);
            toast.success(`Deployed "${name}"`);
            load();
        } catch (err) {
            toast.error(err.response?.data?.message || 'Failed to deploy');
        }
    };

    const handleDelete = async (id, name) => {
        if (!confirm(`Delete "${name}"? This cannot be undone.`)) return;
        try {
            await MemoApi.deleteDecisionTable(id);
            toast.success(`Deleted "${name}"`);
            load();
        } catch (err) {
            toast.error(err.response?.data?.message || 'Failed to delete');
        }
    };

    const handleNewVersion = async (id, name) => {
        try {
            const result = await MemoApi.createDecisionTableVersion(id);
            toast.success(`Created v${result.version} of "${name}"`);
            navigate(`/dmn/${result.id}`);
        } catch (err) {
            toast.error(err.response?.data?.message || 'Failed to create version');
        }
    };

    const handleDeprecate = async (id, name) => {
        if (!confirm(`Deprecate "${name}"? It will no longer be available for new processes.`)) return;
        try {
            await MemoApi.deprecateDecisionTable(id);
            toast.success(`Deprecated "${name}"`);
            load();
        } catch (err) {
            toast.error(err.response?.data?.message || 'Failed to deprecate');
        }
    };

    // Auto-generate key from name
    const handleNameChange = (name) => {
        setCreateForm(prev => ({
            ...prev,
            name,
            key: prev.key === '' || prev.key === toKey(prev.name)
                ? toKey(name)
                : prev.key
        }));
    };

    const toKey = (name) => name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');

    return (
        <div className="p-6 max-w-6xl mx-auto space-y-6">
            {/* Header */}
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-bold text-slate-900 flex items-center gap-2">
                        <Table2 className="h-6 w-6 text-indigo-600" />
                        Decision Tables
                    </h1>
                    <p className="text-sm text-slate-500 mt-1">
                        Manage DMN decision tables for complex business rule routing
                    </p>
                </div>
                <Button
                    onClick={() => setShowCreateModal(true)}
                    className="bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-600 hover:to-purple-700 text-white font-medium shadow-lg shadow-indigo-500/25 rounded-xl px-5 h-10"
                >
                    <Plus className="mr-2 h-4 w-4" />
                    New Decision Table
                </Button>
            </div>

            {/* Filters */}
            <div className="flex items-center gap-3">
                <div className="relative flex-1 max-w-sm">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
                    <input
                        type="text"
                        placeholder="Search by name or key..."
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        className="w-full pl-10 pr-4 py-2 bg-white border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                    />
                </div>
                <div className="flex gap-1 bg-slate-100 rounded-lg p-1">
                    {['ALL', 'DRAFT', 'ACTIVE', 'DEPRECATED'].map(s => (
                        <button
                            key={s}
                            onClick={() => setStatusFilter(s)}
                            className={cn(
                                'px-3 py-1.5 text-xs font-medium rounded-md transition-all',
                                statusFilter === s
                                    ? 'bg-white text-slate-900 shadow-sm'
                                    : 'text-slate-500 hover:text-slate-700'
                            )}
                        >
                            {s === 'ALL' ? 'All' : statusConfig[s]?.label || s}
                        </button>
                    ))}
                </div>
            </div>

            {/* Table List */}
            {loading ? (
                <div className="flex items-center justify-center py-20">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600" />
                </div>
            ) : filtered.length === 0 ? (
                <div className="text-center py-20 space-y-3">
                    <Table2 className="h-12 w-12 text-slate-300 mx-auto" />
                    <p className="text-slate-500">No decision tables found</p>
                    {tables.length === 0 && (
                        <Button
                            variant="outline"
                            onClick={() => setShowCreateModal(true)}
                            className="rounded-xl"
                        >
                            <Plus className="mr-2 h-4 w-4" />
                            Create your first decision table
                        </Button>
                    )}
                </div>
            ) : (
                <div className="space-y-3">
                    {filtered.map(table => {
                        const sc = statusConfig[table.status] || statusConfig.DRAFT;
                        return (
                            <div
                                key={table.id}
                                className="bg-white border border-slate-200 rounded-xl p-4 hover:shadow-md hover:border-indigo-200 transition-all group cursor-pointer"
                                onClick={() => navigate(`/dmn/${table.id}`)}
                            >
                                <div className="flex items-center justify-between">
                                    <div className="flex items-center gap-4 min-w-0">
                                        <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center shadow-lg shadow-indigo-500/20 flex-shrink-0">
                                            <Table2 className="h-5 w-5 text-white" />
                                        </div>
                                        <div className="min-w-0">
                                            <div className="flex items-center gap-2">
                                                <h3 className="text-sm font-semibold text-slate-900 truncate">
                                                    {table.name}
                                                </h3>
                                                <span className={cn('text-[10px] px-2 py-0.5 rounded-full border font-medium', sc.color)}>
                                                    {sc.label}
                                                </span>
                                                <span className="text-[10px] px-1.5 py-0.5 rounded bg-slate-100 text-slate-500 font-mono">
                                                    v{table.version}
                                                </span>
                                            </div>
                                            <div className="flex items-center gap-3 mt-0.5">
                                                <span className="text-xs text-slate-400 font-mono">{table.key}</span>
                                                {table.description && (
                                                    <span className="text-xs text-slate-400 truncate max-w-[300px]">
                                                        — {table.description}
                                                    </span>
                                                )}
                                            </div>
                                        </div>
                                    </div>
                                    <div className="flex items-center gap-2" onClick={e => e.stopPropagation()}>
                                        {table.status === 'DRAFT' && (
                                            <>
                                                <Button
                                                    size="sm"
                                                    variant="outline"
                                                    className="rounded-lg text-xs h-8"
                                                    onClick={() => handleDeploy(table.id, table.name)}
                                                >
                                                    <Rocket className="mr-1.5 h-3 w-3 text-emerald-600" />
                                                    Deploy
                                                </Button>
                                                <Button
                                                    size="sm"
                                                    variant="ghost"
                                                    className="rounded-lg text-xs h-8 text-red-500 hover:bg-red-50"
                                                    onClick={() => handleDelete(table.id, table.name)}
                                                >
                                                    <Trash2 className="h-3 w-3" />
                                                </Button>
                                            </>
                                        )}
                                        {table.status === 'ACTIVE' && (
                                            <>
                                                <Button
                                                    size="sm"
                                                    variant="outline"
                                                    className="rounded-lg text-xs h-8"
                                                    onClick={() => handleNewVersion(table.id, table.name)}
                                                >
                                                    <Copy className="mr-1.5 h-3 w-3" />
                                                    New Version
                                                </Button>
                                                <Button
                                                    size="sm"
                                                    variant="ghost"
                                                    className="rounded-lg text-xs h-8 text-amber-600 hover:bg-amber-50"
                                                    onClick={() => handleDeprecate(table.id, table.name)}
                                                >
                                                    <Archive className="h-3 w-3" />
                                                </Button>
                                            </>
                                        )}
                                        <ChevronRight className="h-4 w-4 text-slate-300 group-hover:text-indigo-500 transition-colors" />
                                    </div>
                                </div>
                            </div>
                        );
                    })}
                </div>
            )}

            {/* Create Modal */}
            {showCreateModal && (
                <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50"
                    onClick={() => setShowCreateModal(false)}>
                    <div className="bg-white rounded-2xl shadow-2xl p-6 w-full max-w-md mx-4"
                        onClick={e => e.stopPropagation()}>
                        <h2 className="text-lg font-bold text-slate-900 mb-4">New Decision Table</h2>
                        <form onSubmit={handleCreate} className="space-y-4">
                            <div>
                                <label className="block text-xs font-medium text-slate-700 mb-1.5">Name</label>
                                <input
                                    type="text"
                                    value={createForm.name}
                                    onChange={(e) => handleNameChange(e.target.value)}
                                    placeholder="Memo Routing Rules"
                                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                                    autoFocus
                                />
                            </div>
                            <div>
                                <label className="block text-xs font-medium text-slate-700 mb-1.5">Key</label>
                                <input
                                    type="text"
                                    value={createForm.key}
                                    onChange={(e) => setCreateForm(prev => ({ ...prev, key: e.target.value }))}
                                    placeholder="memo-routing-rules"
                                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm font-mono focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                                />
                                <p className="text-[10px] text-slate-400 mt-1">
                                    Used to reference this table from BPMN Business Rule Tasks. Must be lowercase with hyphens.
                                </p>
                            </div>
                            <div>
                                <label className="block text-xs font-medium text-slate-700 mb-1.5">Description</label>
                                <textarea
                                    value={createForm.description}
                                    onChange={(e) => setCreateForm(prev => ({ ...prev, description: e.target.value }))}
                                    placeholder="Decision logic for routing memos based on..."
                                    rows={2}
                                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 resize-none"
                                />
                            </div>
                            <div className="flex gap-3">
                                <Button
                                    type="button"
                                    variant="outline"
                                    className="flex-1 rounded-xl"
                                    onClick={() => setShowCreateModal(false)}
                                >
                                    Cancel
                                </Button>
                                <Button
                                    type="submit"
                                    disabled={creating}
                                    className="flex-1 bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-600 hover:to-purple-700 text-white rounded-xl"
                                >
                                    {creating ? 'Creating...' : 'Create & Edit'}
                                </Button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
