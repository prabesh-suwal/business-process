import { useState, useCallback } from 'react';

/**
 * General tab — shown for every BPMN element.
 * Displays name, ID (copyable), type, and documentation.
 */
export default function GeneralTab({ element, modelerRef, onNameChange }) {
    const [copied, setCopied] = useState(false);

    const bo = element?.businessObject;
    const elementId = element?.id || '';
    const elementName = bo?.name || '';
    const type = element?.type?.replace('bpmn:', '') || 'Element';

    // Get documentation if it exists
    const docs = bo?.documentation;
    const docText = docs && docs.length > 0 ? docs[0].text : '';

    const handleCopy = useCallback(() => {
        navigator.clipboard.writeText(elementId).then(() => {
            setCopied(true);
            setTimeout(() => setCopied(false), 1500);
        });
    }, [elementId]);

    const handleDocChange = useCallback((value) => {
        if (!element || !modelerRef?.current) return;
        const modeler = modelerRef.current.getModeler();
        if (!modeler) return;

        const modeling = modelerRef.current.getModeling();
        const moddle = modeler.get('moddle');

        if (value.trim()) {
            const documentation = moddle.create('bpmn:Documentation', { text: value });
            modeling.updateProperties(element, { documentation: [documentation] });
        } else {
            modeling.updateProperties(element, { documentation: [] });
        }
    }, [element, modelerRef]);

    return (
        <div>
            {/* Element ID */}
            <div className="panel-section">
                <label className="panel-section__label">Element ID</label>
                <div className="panel-copyable">
                    <span className="panel-copyable__value" title={elementId}>
                        {elementId}
                    </span>
                    <button
                        className="panel-copyable__btn"
                        onClick={handleCopy}
                        title={copied ? 'Copied!' : 'Copy ID'}
                    >
                        {copied ? '✓' : '📋'}
                    </button>
                </div>
            </div>

            {/* Element Type */}
            <div className="panel-section">
                <label className="panel-section__label">Type</label>
                <input
                    className="panel-input panel-input--readonly"
                    value={type}
                    readOnly
                    tabIndex={-1}
                />
            </div>

            {/* Documentation */}
            <div className="panel-section">
                <label className="panel-section__label">Documentation</label>
                <textarea
                    className="panel-textarea"
                    value={docText}
                    onChange={(e) => handleDocChange(e.target.value)}
                    placeholder="Add a description for this element..."
                    rows={3}
                />
                <p className="panel-section__help">
                    Description visible to administrators and in exported documentation.
                </p>
            </div>

            {/* Additional info based on type */}
            {element?.type === 'bpmn:SequenceFlow' && (
                <div className="panel-section">
                    <label className="panel-section__label">Connection</label>
                    <div style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-gray-600)' }}>
                        <div style={{ marginBottom: '4px' }}>
                            <span style={{ color: 'var(--color-gray-400)', marginRight: '8px' }}>From:</span>
                            {bo?.sourceRef?.name || bo?.sourceRef?.id || '—'}
                        </div>
                        <div>
                            <span style={{ color: 'var(--color-gray-400)', marginRight: '8px' }}>To:</span>
                            {bo?.targetRef?.name || bo?.targetRef?.id || '—'}
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
