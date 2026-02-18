import React, { useState, useEffect } from 'react';
import { MemoApi } from '../../lib/api';
import { Table2, ExternalLink, AlertCircle, CheckCircle2, RefreshCw } from 'lucide-react';
import { Button } from '../ui/button';
import { cn } from '../../lib/utils';

/**
 * BusinessRuleTaskTab — PropertyPanel tab for bpmn:BusinessRuleTask elements.
 * Allows selecting a deployed DMN decision table key to reference.
 *
 * Sets the flowable:decisionTableReferenceKey extension attribute on the element.
 */
export default function BusinessRuleTaskTab({ element, modelerRef }) {
    const [keys, setKeys] = useState([]);
    const [loading, setLoading] = useState(false);
    const [selectedKey, setSelectedKey] = useState('');

    // Read current decisionTableReferenceKey from modeler
    useEffect(() => {
        if (!element) return;
        const bo = element.businessObject;
        const extensionElements = bo?.extensionElements?.values || [];
        // Check for flowable:field or direct attribute
        const existingKey = bo?.$attrs?.['flowable:decisionTableReferenceKey'] || '';
        setSelectedKey(existingKey);
    }, [element?.id]);

    // Load available keys
    useEffect(() => {
        const load = async () => {
            try {
                setLoading(true);
                const data = await MemoApi.listDecisionTableKeys();
                setKeys(data);
            } catch (err) {
                console.error('Failed to load DMN keys:', err);
            } finally {
                setLoading(false);
            }
        };
        load();
    }, []);

    // Update BPMN element when key changes
    const handleKeyChange = (key) => {
        setSelectedKey(key);
        if (!modelerRef?.current || !element) return;

        try {
            modelerRef.current.suppressSelection?.();
            const modeler = modelerRef.current;
            const activeViewer = modeler.getActiveViewer?.() || modeler;
            const modeling = activeViewer.get('modeling');
            const bo = element.businessObject;

            // Set the extension attribute
            modeling.updateProperties(element, {
                'flowable:decisionTableReferenceKey': key,
            });
        } catch (err) {
            console.error('Failed to update BPMN element:', err);
        } finally {
            setTimeout(() => modelerRef.current?.resumeSelection?.(), 50);
        }
    };

    return (
        <div className="space-y-4">
            {/* Info */}
            <div className="p-3 bg-indigo-50 border border-indigo-200 rounded-xl text-xs text-indigo-800 flex items-start gap-2">
                <Table2 className="h-4 w-4 mt-0.5 flex-shrink-0" />
                <div>
                    <p className="font-medium">Business Rule Task</p>
                    <p className="mt-0.5 text-indigo-600">
                        Select a deployed DMN decision table. At runtime, the engine will evaluate the table
                        and set output variables for gateway routing.
                    </p>
                </div>
            </div>

            {/* Decision Table Selector */}
            <div>
                <label className="block text-xs font-medium text-slate-700 mb-1.5">
                    Decision Table Key
                </label>
                <div className="flex gap-2">
                    <select
                        value={selectedKey}
                        onChange={(e) => handleKeyChange(e.target.value)}
                        className="flex-1 px-3 py-2 bg-white border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                    >
                        <option value="">— Select a decision table —</option>
                        {keys.map(k => (
                            <option key={k.key} value={k.key}>
                                {k.name} ({k.key})
                            </option>
                        ))}
                    </select>
                    <Button
                        size="sm"
                        variant="ghost"
                        className="rounded-lg h-9 w-9 p-0"
                        onClick={() => {
                            setLoading(true);
                            MemoApi.listDecisionTableKeys().then(setKeys).catch(() => { }).finally(() => setLoading(false));
                        }}
                    >
                        <RefreshCw className={cn("h-3.5 w-3.5", loading && "animate-spin")} />
                    </Button>
                </div>
            </div>

            {/* Status */}
            {selectedKey ? (
                <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-xl text-xs flex items-center gap-2">
                    <CheckCircle2 className="h-4 w-4 text-emerald-600" />
                    <div>
                        <p className="font-medium text-emerald-800">
                            Linked to: <span className="font-mono">{selectedKey}</span>
                        </p>
                        <p className="text-emerald-600 mt-0.5">
                            This task will evaluate the decision table at runtime and set output variables.
                        </p>
                    </div>
                </div>
            ) : (
                <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl text-xs flex items-center gap-2">
                    <AlertCircle className="h-4 w-4 text-amber-600" />
                    <p className="text-amber-800">
                        No decision table selected. Choose one above or{' '}
                        <a href="/dmn" target="_blank" className="text-indigo-600 hover:underline inline-flex items-center gap-0.5">
                            create one <ExternalLink className="h-3 w-3" />
                        </a>
                    </p>
                </div>
            )}

            {/* Manual Key Input */}
            <div>
                <label className="block text-xs font-medium text-slate-500 mb-1.5">
                    Or enter key manually
                </label>
                <input
                    type="text"
                    value={selectedKey}
                    onChange={(e) => handleKeyChange(e.target.value)}
                    placeholder="my-decision-table"
                    className="w-full px-3 py-2 bg-white border border-slate-200 rounded-xl text-xs font-mono focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                />
            </div>
        </div>
    );
}
