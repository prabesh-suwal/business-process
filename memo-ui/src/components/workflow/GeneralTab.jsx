import React, { useState, useCallback } from 'react';
import { Copy, Check, ArrowRight, FileText } from 'lucide-react';

/**
 * GeneralTab — Shown for every BPMN element.
 * Displays element ID (copyable), type badge, and documentation.
 * For SequenceFlow, also shows source → target info.
 */
export default function GeneralTab({ element, modelerRef }) {
    const [copied, setCopied] = useState(false);

    const bo = element?.businessObject;
    const elementId = element?.id || '';
    const elementName = bo?.name || '';
    const type = element?.type?.replace('bpmn:', '') || 'Element';

    // Documentation
    const docs = bo?.documentation;
    const docText = docs && docs.length > 0 ? docs[0].text : '';

    // Don't try to parse JSON documentation (assignment configs stored as JSON)
    const isJsonDoc = docText && docText.startsWith('{');
    const displayDoc = isJsonDoc ? '' : docText;

    const handleCopy = useCallback(() => {
        navigator.clipboard.writeText(elementId).then(() => {
            setCopied(true);
            setTimeout(() => setCopied(false), 1500);
        });
    }, [elementId]);

    const handleNameChange = useCallback((value) => {
        if (!element || !modelerRef?.current) return;
        try {
            modelerRef.current.suppressSelection?.();
            const modeler = modelerRef.current.getModeler ? modelerRef.current.getModeler() : modelerRef.current;
            const modeling = modeler.get('modeling');
            modeling.updateProperties(element, { name: value });
        } catch (err) {
            console.error('Error updating name:', err);
        } finally {
            setTimeout(() => modelerRef.current?.resumeSelection?.(), 50);
        }
    }, [element, modelerRef]);

    const handleDocChange = useCallback((value) => {
        if (!element || !modelerRef?.current) return;
        try {
            modelerRef.current.suppressSelection?.();
            const modeler = modelerRef.current.getModeler ? modelerRef.current.getModeler() : modelerRef.current;
            const modeling = modeler.get('modeling');
            const moddle = modeler.get('moddle');

            if (value.trim()) {
                const documentation = moddle.create('bpmn:Documentation', { text: value });
                modeling.updateProperties(element, { documentation: [documentation] });
            } else {
                modeling.updateProperties(element, { documentation: [] });
            }
        } catch (err) {
            console.error('Error updating documentation:', err);
        } finally {
            setTimeout(() => modelerRef.current?.resumeSelection?.(), 50);
        }
    }, [element, modelerRef]);

    // Type badge colors
    const getTypeBadge = (t) => {
        const colors = {
            'UserTask': 'bg-blue-100 text-blue-700 border-blue-200',
            'ExclusiveGateway': 'bg-amber-100 text-amber-700 border-amber-200',
            'ParallelGateway': 'bg-purple-100 text-purple-700 border-purple-200',
            'InclusiveGateway': 'bg-indigo-100 text-indigo-700 border-indigo-200',
            'SequenceFlow': 'bg-slate-100 text-slate-700 border-slate-200',
            'StartEvent': 'bg-emerald-100 text-emerald-700 border-emerald-200',
            'EndEvent': 'bg-red-100 text-red-700 border-red-200',
            'ServiceTask': 'bg-cyan-100 text-cyan-700 border-cyan-200',
        };
        return colors[t] || 'bg-gray-100 text-gray-700 border-gray-200';
    };

    return (
        <div className="space-y-4">
            {/* Element ID */}
            <div>
                <label className="block text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1.5">
                    Element ID
                </label>
                <div className="flex items-center gap-2 p-2.5 bg-slate-50 border border-slate-200 rounded-lg">
                    <code className="flex-1 text-sm font-mono text-slate-700 truncate">{elementId}</code>
                    <button
                        onClick={handleCopy}
                        className="p-1.5 rounded-md hover:bg-slate-200 text-slate-400 hover:text-slate-600 transition-colors flex-shrink-0"
                        title={copied ? 'Copied!' : 'Copy ID'}
                    >
                        {copied ? (
                            <Check className="w-3.5 h-3.5 text-emerald-500" />
                        ) : (
                            <Copy className="w-3.5 h-3.5" />
                        )}
                    </button>
                </div>
            </div>

            {/* Element Type */}
            <div>
                <label className="block text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1.5">
                    Type
                </label>
                <span className={`inline-flex items-center px-3 py-1.5 rounded-lg text-xs font-semibold border ${getTypeBadge(type)}`}>
                    {type}
                </span>
            </div>

            {/* Element Name (editable for UserTask) */}
            {element?.type === 'bpmn:UserTask' && (
                <div>
                    <label className="block text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1.5">
                        Name
                    </label>
                    <input
                        className="w-full p-2.5 border border-slate-200 rounded-lg text-sm bg-white focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all"
                        value={elementName}
                        onChange={(e) => handleNameChange(e.target.value)}
                        placeholder="Enter step name..."
                    />
                </div>
            )}

            {/* Documentation */}
            <div>
                <label className="block text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1.5">
                    <div className="flex items-center gap-1.5">
                        <FileText className="w-3.5 h-3.5" />
                        Documentation
                    </div>
                </label>
                <textarea
                    className="w-full p-2.5 border border-slate-200 rounded-lg text-sm bg-white focus:ring-2 focus:ring-blue-500 focus:border-blue-500 resize-none transition-all"
                    value={displayDoc}
                    onChange={(e) => handleDocChange(e.target.value)}
                    placeholder="Add a description for this element..."
                    rows={3}
                />
                <p className="text-xs text-slate-400 mt-1">
                    Visible in exported documentation and to administrators.
                </p>
            </div>

            {/* Sequence Flow: Source → Target */}
            {element?.type === 'bpmn:SequenceFlow' && (
                <div>
                    <label className="block text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1.5">
                        Connection
                    </label>
                    <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg space-y-2">
                        <div className="flex items-center gap-2 text-sm">
                            <span className="text-slate-400 text-xs font-medium w-12">From</span>
                            <span className="text-slate-700 font-medium">
                                {bo?.sourceRef?.name || bo?.sourceRef?.id || '—'}
                            </span>
                        </div>
                        <div className="flex items-center gap-2 text-sm">
                            <ArrowRight className="w-4 h-4 text-slate-300 ml-3" />
                        </div>
                        <div className="flex items-center gap-2 text-sm">
                            <span className="text-slate-400 text-xs font-medium w-12">To</span>
                            <span className="text-slate-700 font-medium">
                                {bo?.targetRef?.name || bo?.targetRef?.id || '—'}
                            </span>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
