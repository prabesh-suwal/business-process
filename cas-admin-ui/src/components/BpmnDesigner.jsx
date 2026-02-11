import { useEffect, useRef, useState, useCallback, useImperativeHandle, forwardRef } from 'react';
import BpmnModeler from 'bpmn-js/lib/Modeler';
import 'bpmn-js/dist/assets/diagram-js.css';
import 'bpmn-js/dist/assets/bpmn-js.css';
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-embedded.css';

const DEFAULT_BPMN = `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" 
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" 
                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                  id="Definitions_1" 
                  targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="Process_1" name="New Process" isExecutable="true">
    <bpmn:startEvent id="StartEvent_1" name="Start">
      <bpmn:outgoing>Flow_1</bpmn:outgoing>
    </bpmn:startEvent>
    <bpmn:userTask id="Task_1" name="First Task">
      <bpmn:incoming>Flow_1</bpmn:incoming>
      <bpmn:outgoing>Flow_2</bpmn:outgoing>
    </bpmn:userTask>
    <bpmn:endEvent id="EndEvent_1" name="End">
      <bpmn:incoming>Flow_2</bpmn:incoming>
    </bpmn:endEvent>
    <bpmn:sequenceFlow id="Flow_1" sourceRef="StartEvent_1" targetRef="Task_1" />
    <bpmn:sequenceFlow id="Flow_2" sourceRef="Task_1" targetRef="EndEvent_1" />
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_1">
      <bpmndi:BPMNShape id="StartEvent_1_di" bpmnElement="StartEvent_1">
        <dc:Bounds x="180" y="160" width="36" height="36" />
        <bpmndi:BPMNLabel><dc:Bounds x="186" y="203" width="24" height="14" /></bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Task_1_di" bpmnElement="Task_1">
        <dc:Bounds x="280" y="138" width="100" height="80" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="EndEvent_1_di" bpmnElement="EndEvent_1">
        <dc:Bounds x="452" y="160" width="36" height="36" />
        <bpmndi:BPMNLabel><dc:Bounds x="460" y="203" width="20" height="14" /></bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="Flow_1_di" bpmnElement="Flow_1">
        <di:waypoint x="216" y="178" /><di:waypoint x="280" y="178" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_2_di" bpmnElement="Flow_2">
        <di:waypoint x="380" y="178" /><di:waypoint x="452" y="178" />
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`;

/**
 * Helper to get a human-readable type label and icon from a BPMN element.
 */
export function getElementMeta(element) {
    if (!element) return { type: 'Process', label: 'Process', icon: '📋' };
    const t = element.type;
    const bo = element.businessObject;

    const map = {
        'bpmn:UserTask': { label: 'User Task', icon: '👤' },
        'bpmn:ServiceTask': { label: 'Service Task', icon: '⚙️' },
        'bpmn:ScriptTask': { label: 'Script Task', icon: '📜' },
        'bpmn:SendTask': { label: 'Send Task', icon: '📨' },
        'bpmn:ReceiveTask': { label: 'Receive Task', icon: '📩' },
        'bpmn:ManualTask': { label: 'Manual Task', icon: '🤲' },
        'bpmn:BusinessRuleTask': { label: 'Business Rule Task', icon: '📏' },
        'bpmn:Task': { label: 'Task', icon: '📄' },
        'bpmn:SubProcess': { label: 'Sub-Process', icon: '🔲' },
        'bpmn:ExclusiveGateway': { label: 'Exclusive Gateway', icon: '◇' },
        'bpmn:InclusiveGateway': { label: 'Inclusive Gateway', icon: '◈' },
        'bpmn:ParallelGateway': { label: 'Parallel Gateway', icon: '⊞' },
        'bpmn:EventBasedGateway': { label: 'Event Gateway', icon: '⊡' },
        'bpmn:ComplexGateway': { label: 'Complex Gateway', icon: '✳️' },
        'bpmn:StartEvent': { label: 'Start Event', icon: '▶' },
        'bpmn:EndEvent': { label: 'End Event', icon: '⏹' },
        'bpmn:IntermediateCatchEvent': { label: 'Intermediate Event', icon: '⏸' },
        'bpmn:IntermediateThrowEvent': { label: 'Intermediate Event', icon: '⏸' },
        'bpmn:BoundaryEvent': { label: 'Boundary Event', icon: '⚡' },
        'bpmn:SequenceFlow': { label: 'Sequence Flow', icon: '→' },
        'bpmn:DataStoreReference': { label: 'Data Store', icon: '🗄' },
        'bpmn:DataObjectReference': { label: 'Data Object', icon: '📊' },
        'bpmn:TextAnnotation': { label: 'Annotation', icon: '💬' },
        'bpmn:Participant': { label: 'Pool', icon: '🏊' },
        'bpmn:Lane': { label: 'Lane', icon: '🏊' },
    };

    return map[t] || { label: t?.replace('bpmn:', '') || 'Element', icon: '📄' };
}

const BpmnDesigner = forwardRef(function BpmnDesigner({
    initialXml,
    onSave,
    onXmlChange,
    onElementSelect,
    readOnly = false,
    height = '600px',
    style
}, ref) {
    const containerRef = useRef(null);
    const modelerRef = useRef(null);
    const [selectedElement, setSelectedElement] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);
    const [isModelerReady, setIsModelerReady] = useState(false);
    const isImportingRef = useRef(false);

    // Expose modeler methods to parent via ref
    useImperativeHandle(ref, () => ({
        getModeler: () => modelerRef.current,
        getModeling: () => modelerRef.current?.get('modeling'),
        getElementRegistry: () => modelerRef.current?.get('elementRegistry'),
        getCanvas: () => modelerRef.current?.get('canvas'),
        async getXml() {
            if (!modelerRef.current) return null;
            try {
                const { xml } = await modelerRef.current.saveXML({ format: true });
                return xml;
            } catch (err) {
                console.error('Error saving XML:', err);
                return null;
            }
        },
        async getSvg() {
            if (!modelerRef.current) return null;
            try {
                const { svg } = await modelerRef.current.saveSVG();
                return svg;
            } catch (err) {
                console.error('Error exporting SVG:', err);
                return null;
            }
        }
    }), [isModelerReady]);

    // Initialize modeler
    useEffect(() => {
        if (!containerRef.current) return;

        // Create modeler
        const modeler = new BpmnModeler({
            container: containerRef.current,
            keyboard: { bindTo: document }
        });

        modelerRef.current = modeler;

        // Listen for selection changes
        modeler.on('selection.changed', (e) => {
            const element = e.newSelection[0];
            const selected = element || null;
            setSelectedElement(selected);
            // Fire callback to parent
            onElementSelect?.(selected);
        });

        // Listen for changes
        modeler.on('commandStack.changed', async () => {
            if (onXmlChange) {
                try {
                    const { xml } = await modeler.saveXML({ format: true });
                    onXmlChange(xml);
                } catch (err) {
                    console.error('Error getting XML:', err);
                }
            }
        });

        // Mark modeler as ready
        setIsModelerReady(true);

        return () => {
            setIsModelerReady(false);
            modeler.destroy();
        };
    }, []);

    // Load diagram when modeler is ready or initialXml changes
    useEffect(() => {
        if (!isModelerReady || !modelerRef.current) return;
        if (isImportingRef.current) return; // Prevent concurrent imports

        const loadDiagram = async () => {
            try {
                isImportingRef.current = true;
                setIsLoading(true);
                setError(null);

                const xmlToLoad = initialXml || DEFAULT_BPMN;
                await modelerRef.current.importXML(xmlToLoad);

                // Fit viewport after import
                const canvas = modelerRef.current.get('canvas');
                canvas.zoom('fit-viewport');

                setIsLoading(false);
            } catch (err) {
                console.error('Error loading BPMN:', err);
                setError(err.message);
                setIsLoading(false);
            } finally {
                isImportingRef.current = false;
            }
        };

        loadDiagram();
    }, [isModelerReady, initialXml]);

    // Get current XML
    const getXml = useCallback(async () => {
        if (!modelerRef.current) return null;
        try {
            const { xml } = await modelerRef.current.saveXML({ format: true });
            return xml;
        } catch (err) {
            console.error('Error saving XML:', err);
            return null;
        }
    }, []);

    // Save handler
    const handleSave = useCallback(async () => {
        const xml = await getXml();
        if (xml && onSave) {
            onSave(xml);
        }
    }, [getXml, onSave]);

    // Zoom controls
    const handleZoomIn = () => {
        const canvas = modelerRef.current?.get('canvas');
        if (canvas) canvas.zoom(canvas.zoom() * 1.2);
    };

    const handleZoomOut = () => {
        const canvas = modelerRef.current?.get('canvas');
        if (canvas) canvas.zoom(canvas.zoom() / 1.2);
    };

    const handleZoomReset = () => {
        const canvas = modelerRef.current?.get('canvas');
        if (canvas) canvas.zoom('fit-viewport');
    };

    // Download SVG
    const handleDownloadSvg = async () => {
        if (!modelerRef.current) return;
        try {
            const { svg } = await modelerRef.current.saveSVG();
            const blob = new Blob([svg], { type: 'image/svg+xml' });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = 'workflow.svg';
            a.click();
            URL.revokeObjectURL(url);
        } catch (err) {
            console.error('Error exporting SVG:', err);
        }
    };

    return (
        <div className="bpmn-designer" style={{ display: 'flex', flexDirection: 'column', height, ...style }}>
            {/* Toolbar */}
            <div className="bpmn-toolbar" style={{
                display: 'flex',
                gap: '8px',
                padding: '8px 12px',
                background: 'var(--bg-tertiary)',
                borderBottom: '1px solid var(--border-color)',
                borderRadius: '8px 8px 0 0'
            }}>
                <button onClick={handleZoomIn} className="btn btn-sm btn-secondary" title="Zoom In">
                    🔍+
                </button>
                <button onClick={handleZoomOut} className="btn btn-sm btn-secondary" title="Zoom Out">
                    🔍-
                </button>
                <button onClick={handleZoomReset} className="btn btn-sm btn-secondary" title="Fit to View">
                    ⬜
                </button>
                <div style={{ flex: 1 }} />
                <button onClick={handleDownloadSvg} className="btn btn-sm btn-secondary">
                    📥 Export SVG
                </button>
                {onSave && (
                    <button onClick={handleSave} className="btn btn-sm btn-primary">
                        💾 Save
                    </button>
                )}
            </div>

            {/* Designer Canvas */}
            <div style={{
                flex: 1,
                position: 'relative',
                border: '1px solid var(--border-color)',
                borderTop: 'none',
                borderRadius: '0 0 8px 8px',
                overflow: 'hidden',
                background: '#fff'
            }}>
                {isLoading && (
                    <div style={{
                        position: 'absolute',
                        top: '50%',
                        left: '50%',
                        transform: 'translate(-50%, -50%)',
                        color: '#666'
                    }}>
                        Loading designer...
                    </div>
                )}
                {error && (
                    <div style={{
                        position: 'absolute',
                        top: '50%',
                        left: '50%',
                        transform: 'translate(-50%, -50%)',
                        color: 'var(--danger)',
                        textAlign: 'center'
                    }}>
                        <div>Error loading diagram</div>
                        <small>{error}</small>
                    </div>
                )}
                <div
                    ref={containerRef}
                    style={{
                        width: '100%',
                        height: '100%',
                        visibility: isLoading ? 'hidden' : 'visible'
                    }}
                />
            </div>
        </div>
    );
});

export default BpmnDesigner;
