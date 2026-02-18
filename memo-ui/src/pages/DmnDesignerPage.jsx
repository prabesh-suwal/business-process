import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { MemoApi } from '../lib/api';
import { toast } from 'sonner';
import {
    ArrowLeft, Save, Rocket, Table2, Play, ChevronDown,
    ChevronUp, AlertCircle, CheckCircle2, Info
} from 'lucide-react';
import { Button } from '../components/ui/button';
import { cn } from '../lib/utils';

const statusConfig = {
    DRAFT: { label: 'Draft', color: 'bg-amber-100 text-amber-700 border-amber-200', dot: 'bg-amber-400' },
    ACTIVE: { label: 'Active', color: 'bg-emerald-100 text-emerald-700 border-emerald-200', dot: 'bg-emerald-400' },
    DEPRECATED: { label: 'Deprecated', color: 'bg-slate-100 text-slate-500 border-slate-200', dot: 'bg-slate-400' },
};

export default function DmnDesignerPage() {
    const { decisionId } = useParams();
    const navigate = useNavigate();
    const containerRef = useRef(null);
    const modelerRef = useRef(null);

    const [table, setTable] = useState(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [deploying, setDeploying] = useState(false);
    const [dirty, setDirty] = useState(false);

    // Test evaluation state
    const [showTestPanel, setShowTestPanel] = useState(false);
    const [testVars, setTestVars] = useState('{\n  "decision": "APPROVED"\n}');
    const [testResult, setTestResult] = useState(null);
    const [testing, setTesting] = useState(false);

    // Load decision table
    useEffect(() => {
        const load = async () => {
            try {
                setLoading(true);
                const data = await MemoApi.getDecisionTable(decisionId);
                setTable(data);
            } catch (err) {
                toast.error('Failed to load decision table');
                navigate('/dmn');
            } finally {
                setLoading(false);
            }
        };
        load();
    }, [decisionId, navigate]);

    // Initialize dmn-js modeler once the table is loaded
    useEffect(() => {
        if (!table || !containerRef.current) return;

        let modeler = null;
        let destroyed = false;

        const init = async () => {
            try {
                // Dynamic import to avoid SSR issues
                const DmnJS = (await import('dmn-js/lib/Modeler')).default;

                if (destroyed) return;

                modeler = new DmnJS({
                    container: containerRef.current,
                });

                modelerRef.current = modeler;

                await modeler.importXML(table.dmnXml);

                // Track changes
                const views = modeler.getViews();
                views.forEach(view => {
                    try {
                        const viewer = modeler.getActiveViewer();
                        if (viewer) {
                            const eventBus = viewer.get('eventBus');
                            if (eventBus) {
                                eventBus.on('elements.changed', () => setDirty(true));
                                eventBus.on('commandStack.changed', () => setDirty(true));
                            }
                        }
                    } catch (e) {
                        // Some views may not have event bus
                    }
                });
            } catch (err) {
                console.error('Failed to initialize DMN editor:', err);
                toast.error('Failed to initialize DMN editor');
            }
        };

        init();

        return () => {
            destroyed = true;
            if (modeler) {
                try { modeler.destroy(); } catch (e) { /* ignore */ }
            }
        };
    }, [table?.id]); // Only re-init when table ID changes

    // Save DMN XML
    const handleSave = useCallback(async () => {
        if (!modelerRef.current || !table) return;
        try {
            setSaving(true);
            const { xml } = await modelerRef.current.saveXML({ format: true });
            await MemoApi.updateDecisionTable(table.id, { dmnXml: xml });
            setDirty(false);
            toast.success('Saved');
        } catch (err) {
            toast.error('Failed to save');
        } finally {
            setSaving(false);
        }
    }, [table]);

    // Deploy
    const handleDeploy = async () => {
        // Save first if dirty
        if (dirty && modelerRef.current) {
            try {
                const { xml } = await modelerRef.current.saveXML({ format: true });
                await MemoApi.updateDecisionTable(table.id, { dmnXml: xml });
                setDirty(false);
            } catch (err) {
                toast.error('Failed to save before deploy');
                return;
            }
        }
        try {
            setDeploying(true);
            const result = await MemoApi.deployDecisionTable(table.id);
            setTable(result);
            toast.success('Deployed to Flowable DMN engine');
        } catch (err) {
            toast.error(err.response?.data?.message || 'Deploy failed');
        } finally {
            setDeploying(false);
        }
    };

    // Test evaluate
    const handleTest = async () => {
        if (!table?.flowableDecisionKey) {
            toast.error('Decision table must be deployed before testing');
            return;
        }
        try {
            setTesting(true);
            const vars = JSON.parse(testVars);
            const result = await MemoApi.evaluateDecisionTable(table.flowableDecisionKey || table.key, vars);
            setTestResult(result);
            toast.success(`Evaluated: ${result.results?.length || 0} result(s)`);
        } catch (err) {
            if (err instanceof SyntaxError) {
                toast.error('Invalid JSON in test variables');
            } else {
                toast.error(err.response?.data?.message || 'Evaluation failed');
            }
        } finally {
            setTesting(false);
        }
    };

    // Keyboard shortcuts
    useEffect(() => {
        const handleKeyDown = (e) => {
            if ((e.metaKey || e.ctrlKey) && e.key === 's') {
                e.preventDefault();
                handleSave();
            }
        };
        window.addEventListener('keydown', handleKeyDown);
        return () => window.removeEventListener('keydown', handleKeyDown);
    }, [handleSave]);

    if (loading) {
        return (
            <div className="flex items-center justify-center h-screen">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600" />
            </div>
        );
    }

    if (!table) return null;

    const sc = statusConfig[table.status] || statusConfig.DRAFT;
    const isDraft = table.status === 'DRAFT';

    return (
        <div className="h-[calc(100vh-4rem)] flex flex-col">
            {/* Toolbar */}
            <div className="bg-white border-b border-slate-200 px-4 py-2.5 flex items-center justify-between flex-shrink-0">
                <div className="flex items-center gap-3">
                    <Button
                        size="sm"
                        variant="ghost"
                        className="rounded-lg"
                        onClick={() => navigate('/dmn')}
                    >
                        <ArrowLeft className="h-4 w-4" />
                    </Button>
                    <div className="flex items-center gap-2">
                        <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center shadow shadow-indigo-500/20">
                            <Table2 className="h-4 w-4 text-white" />
                        </div>
                        <div>
                            <h1 className="text-sm font-semibold text-slate-900">{table.name}</h1>
                            <div className="flex items-center gap-2">
                                <span className="text-[10px] text-slate-400 font-mono">{table.key}</span>
                                <span className={cn('text-[10px] px-1.5 py-0.5 rounded-full border font-medium', sc.color)}>
                                    {sc.label} v{table.version}
                                </span>
                                {dirty && (
                                    <span className="text-[10px] px-1.5 py-0.5 rounded bg-amber-50 text-amber-600 font-medium">
                                        Unsaved
                                    </span>
                                )}
                            </div>
                        </div>
                    </div>
                </div>

                <div className="flex items-center gap-2">
                    <Button
                        size="sm"
                        variant="outline"
                        className={cn("rounded-lg text-xs h-8", showTestPanel && "bg-indigo-50 border-indigo-200")}
                        onClick={() => setShowTestPanel(!showTestPanel)}
                    >
                        <Play className="mr-1.5 h-3 w-3" />
                        Test
                        {showTestPanel ? <ChevronUp className="ml-1 h-3 w-3" /> : <ChevronDown className="ml-1 h-3 w-3" />}
                    </Button>

                    {isDraft && (
                        <>
                            <Button
                                size="sm"
                                variant="outline"
                                className="rounded-lg text-xs h-8"
                                onClick={handleSave}
                                disabled={saving || !dirty}
                            >
                                <Save className="mr-1.5 h-3 w-3" />
                                {saving ? 'Saving...' : 'Save'}
                            </Button>
                            <Button
                                size="sm"
                                className="rounded-lg text-xs h-8 bg-gradient-to-r from-emerald-500 to-green-600 hover:from-emerald-600 hover:to-green-700 text-white"
                                onClick={handleDeploy}
                                disabled={deploying}
                            >
                                <Rocket className="mr-1.5 h-3 w-3" />
                                {deploying ? 'Deploying...' : 'Deploy'}
                            </Button>
                        </>
                    )}

                    {!isDraft && (
                        <div className="flex items-center gap-1.5 text-xs text-slate-500 bg-slate-50 px-3 py-1.5 rounded-lg">
                            <Info className="h-3 w-3" />
                            Read-only — create a new version to edit
                        </div>
                    )}
                </div>
            </div>

            {/* Test Panel */}
            {showTestPanel && (
                <div className="bg-slate-50 border-b border-slate-200 p-4 flex gap-4 flex-shrink-0">
                    <div className="flex-1">
                        <label className="block text-xs font-medium text-slate-700 mb-1.5">
                            Input Variables (JSON)
                        </label>
                        <textarea
                            value={testVars}
                            onChange={(e) => setTestVars(e.target.value)}
                            rows={3}
                            className="w-full px-3 py-2 bg-white border border-slate-200 rounded-xl text-xs font-mono focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 resize-none"
                        />
                        <Button
                            size="sm"
                            className="mt-2 rounded-lg text-xs bg-indigo-600 hover:bg-indigo-700 text-white"
                            onClick={handleTest}
                            disabled={testing || !table.flowableDecisionKey}
                        >
                            <Play className="mr-1.5 h-3 w-3" />
                            {testing ? 'Evaluating...' : 'Evaluate'}
                        </Button>
                        {!table.flowableDecisionKey && (
                            <p className="text-[10px] text-amber-600 mt-1">⚠ Deploy first to enable testing</p>
                        )}
                    </div>
                    <div className="flex-1">
                        <label className="block text-xs font-medium text-slate-700 mb-1.5">
                            Result
                        </label>
                        {testResult ? (
                            <div className="bg-white border border-slate-200 rounded-xl p-3 text-xs font-mono overflow-auto max-h-[120px]">
                                <div className="flex items-center gap-1.5 mb-2">
                                    <CheckCircle2 className="h-3.5 w-3.5 text-emerald-500" />
                                    <span className="text-emerald-700 font-semibold">
                                        {testResult.results?.length || 0} result(s) — Hit Policy: {testResult.hitPolicy}
                                    </span>
                                </div>
                                <pre className="text-slate-600 whitespace-pre-wrap">
                                    {JSON.stringify(testResult.results, null, 2)}
                                </pre>
                            </div>
                        ) : (
                            <div className="bg-white border border-dashed border-slate-200 rounded-xl p-3 text-xs text-slate-400 flex items-center gap-2">
                                <AlertCircle className="h-3.5 w-3.5" />
                                Run evaluation to see results
                            </div>
                        )}
                    </div>
                </div>
            )}

            {/* DMN Editor Container */}
            <div className="flex-1 relative">
                <div
                    ref={containerRef}
                    className="absolute inset-0"
                    style={{ minHeight: '400px' }}
                />
            </div>
        </div>
    );
}
