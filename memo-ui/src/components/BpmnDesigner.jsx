import { useEffect, useRef, useState, useCallback, forwardRef, useImperativeHandle } from 'react';
import BpmnModeler from 'bpmn-js/lib/Modeler';
import TokenSimulationModule from 'bpmn-js-token-simulation';
import 'bpmn-js/dist/assets/diagram-js.css';
import 'bpmn-js/dist/assets/bpmn-js.css';
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-embedded.css';
import 'bpmn-js-token-simulation/assets/css/bpmn-js-token-simulation.css';

const DEFAULT_BPMN = `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" 
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" 
                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                  xmlns:flowable="http://flowable.org/bpmn"
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

const BpmnDesigner = forwardRef(function BpmnDesigner({
    initialXml,
    onSave,
    onXmlChange,
    onElementClick,
    readOnly = false,
    height = '600px'
}, ref) {
    const containerRef = useRef(null);
    const modelerRef = useRef(null);
    const [selectedElement, setSelectedElement] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);
    const [isModelerReady, setIsModelerReady] = useState(false);
    const isImportingRef = useRef(false);

    // Track assignment configs for each task
    const [taskAssignments, setTaskAssignments] = useState({});
    const [activeTab, setActiveTab] = useState('assignment');

    // Initialize modeler
    useEffect(() => {
        if (!containerRef.current) return;

        const modeler = new BpmnModeler({
            container: containerRef.current,
            keyboard: { bindTo: document },
            additionalModules: [
                TokenSimulationModule
            ]
        });

        modelerRef.current = modeler;

        // Listen for selection changes
        modeler.on('selection.changed', (e) => {
            const element = e.newSelection[0];
            setSelectedElement(element || null);
            if (onElementClick) {
                onElementClick(element || null);
            }
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

        setIsModelerReady(true);

        return () => {
            setIsModelerReady(false);
            modeler.destroy();
        };
    }, []);

    // Expose modeler internals to parent via ref
    useImperativeHandle(ref, () => ({
        getModeler: () => modelerRef.current,
        getModeling: () => modelerRef.current?.get('modeling'),
        getModdle: () => modelerRef.current?.get('moddle'),
        getElementRegistry: () => modelerRef.current?.get('elementRegistry'),
        getCanvas: () => modelerRef.current?.get('canvas'),
        importXML: async (xml) => {
            if (!modelerRef.current) throw new Error('Modeler not ready');
            isImportingRef.current = true;
            try {
                await modelerRef.current.importXML(xml);
                const canvas = modelerRef.current.get('canvas');
                canvas.zoom('fit-viewport');
                extractAssignmentConfigs();
            } finally {
                isImportingRef.current = false;
            }
        },
    }), []);

    // Load diagram when modeler is ready or initialXml changes
    useEffect(() => {
        if (!isModelerReady || !modelerRef.current) return;
        if (isImportingRef.current) return;

        const loadDiagram = async () => {
            try {
                isImportingRef.current = true;
                setIsLoading(true);
                setError(null);

                const xmlToLoad = initialXml || DEFAULT_BPMN;
                await modelerRef.current.importXML(xmlToLoad);

                // Extract existing assignment configs from extensionElements
                extractAssignmentConfigs();

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

    // Extract assignment configs from BPMN extensionElements
    const extractAssignmentConfigs = () => {
        if (!modelerRef.current) return;

        const elementRegistry = modelerRef.current.get('elementRegistry');
        const configs = {};

        elementRegistry.forEach(element => {
            if (element.type === 'bpmn:UserTask') {
                const bo = element.businessObject;

                // Try to parse config from documentation (where we store the JSON)
                const documentation = bo.documentation;
                if (documentation && documentation.length > 0) {
                    const docText = documentation[0].text;
                    if (docText) {
                        try {
                            const parsed = JSON.parse(docText);
                            if (parsed && parsed.strategy) {
                                configs[element.id] = {
                                    strategy: parsed.strategy || 'ROLE',
                                    role: parsed.role || '',
                                    scope: parsed.scope || 'ORIGINATING_BRANCH',
                                    limitField: parsed.limitField || 'amount',
                                    selection: parsed.selection || 'LOWEST_MATCH',
                                    department: parsed.department || '',
                                    group: parsed.group || '',
                                    committeeCode: parsed.committeeCode || '',
                                    decisionRule: parsed.decisionRule || 'MAJORITY',
                                    conditions: parsed.conditions || [],
                                    escalationLevels: parsed.escalationLevels || [],
                                };
                                return;
                            }
                        } catch (e) {
                            console.log('Could not parse assignment config from documentation:', e);
                        }
                    }
                }

                // Fallback: try to read from candidateGroups
                if (bo.candidateGroups) {
                    const role = bo.candidateGroups.replace('ROLE_', '').replace('COMMITTEE_', '').replace('DEPT_', '');
                    configs[element.id] = {
                        strategy: bo.candidateGroups.startsWith('COMMITTEE_') ? 'COMMITTEE' :
                            bo.candidateGroups.startsWith('DEPT_') ? 'DEPARTMENT' : 'ROLE',
                        role: role,
                        scope: 'ORIGINATING_BRANCH',
                        conditions: [],
                        escalationLevels: [],
                    };
                }
            }
        });

        setTaskAssignments(prev => ({ ...prev, ...configs }));
    };

    // Handle assignment config change
    const handleAssignmentChange = (config) => {
        if (!selectedElement) return;

        const taskId = selectedElement.id;
        setTaskAssignments(prev => ({ ...prev, [taskId]: config }));

        // Update BPMN element with assignment info
        updateBpmnElement(selectedElement, config);
    };

    // Update BPMN element with assignment config
    const updateBpmnElement = (element, config) => {
        if (!modelerRef.current || !element) return;

        const modeling = modelerRef.current.get('modeling');
        const moddle = modelerRef.current.get('moddle');
        const bo = element.businessObject;

        // Convert our config to BPMN properties
        let candidateGroups = '';

        if (config.strategy === 'ROLE' && config.role) {
            candidateGroups = `ROLE_${config.role}`;
        } else if (config.strategy === 'COMMITTEE' && config.committeeCode) {
            candidateGroups = `COMMITTEE_${config.committeeCode}`;
        } else if (config.strategy === 'DEPARTMENT' && config.department) {
            candidateGroups = `DEPT_${config.department}`;
        } else if (config.strategy === 'AUTHORITY') {
            candidateGroups = 'ROLE_APPROVER'; // fallback group
        }

        // Store the full config as JSON in documentation
        const assignmentDoc = JSON.stringify({
            strategy: config.strategy,
            role: config.role,
            scope: config.scope,
            limitField: config.limitField,
            selection: config.selection,
            department: config.department,
            group: config.group,
            committeeCode: config.committeeCode,
            decisionRule: config.decisionRule,
            conditions: config.conditions,
            escalationLevels: config.escalationLevels,
        });

        try {
            // Create a new documentation element with the config JSON
            const documentation = moddle.create('bpmn:Documentation', {
                text: assignmentDoc
            });

            // Update the business object with candidateGroups and documentation
            modeling.updateProperties(element, {
                'flowable:candidateGroups': candidateGroups,
                documentation: [documentation]
            });

            console.log('Saved assignment config for', element.id, config);
        } catch (err) {
            console.error('Error updating element:', err);
            // Fallback: try simpler update
            try {
                modeling.updateProperties(element, {
                    'flowable:candidateGroups': candidateGroups
                });
            } catch (e) {
                console.error('Fallback also failed:', e);
            }
        }
    };

    // Get current XML with assignment configs embedded
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

    const handleSave = useCallback(async () => {
        const xml = await getXml();
        if (xml && onSave) {
            // Include assignment configs with the save
            onSave(xml, taskAssignments);
        }
    }, [getXml, onSave, taskAssignments]);

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

    const isUserTask = selectedElement?.type === 'bpmn:UserTask';
    const selectedTaskName = selectedElement?.businessObject?.name || 'Selected Task';

    return (
        <div className="bpmn-designer border rounded-lg overflow-hidden flex flex-col" style={{ height }}>
            {/* Toolbar */}
            <div className="flex gap-2 p-2 bg-muted border-b items-center">
                <button onClick={handleZoomIn} className="px-2 py-1 bg-white border rounded hover:bg-gray-50 text-sm" title="Zoom In">
                    ➕
                </button>
                <button onClick={handleZoomOut} className="px-2 py-1 bg-white border rounded hover:bg-gray-50 text-sm" title="Zoom Out">
                    ➖
                </button>
                <button onClick={handleZoomReset} className="px-2 py-1 bg-white border rounded hover:bg-gray-50 text-sm" title="Fit to View">
                    ⛶
                </button>
                <div className="flex-1" />
                <button onClick={handleDownloadSvg} className="px-2 py-1 bg-white border rounded hover:bg-gray-50 text-sm">
                    Export SVG
                </button>
                {onSave && (
                    <button onClick={handleSave} className="px-3 py-1 bg-primary text-primary-foreground rounded hover:bg-primary/90 text-sm font-medium">
                        Save Workflow
                    </button>
                )}
            </div>

            {/* Main Content Area */}
            <div className="flex-1 relative flex">
                {/* Designer Canvas */}
                <div className="flex-1 relative bg-white">
                    {isLoading && (
                        <div className="absolute inset-0 flex items-center justify-center text-muted-foreground z-10 bg-white/80">
                            Loading designer...
                        </div>
                    )}
                    {error && (
                        <div className="absolute inset-0 flex items-center justify-center text-destructive z-10 bg-white/80 p-4 text-center">
                            <div>
                                <div className="font-bold">Error loading diagram</div>
                                <small>{error}</small>
                            </div>
                        </div>
                    )}
                    <div
                        ref={containerRef}
                        className={`w-full h-full ${isLoading ? 'invisible' : 'visible'}`}
                    />
                </div>
            </div>

            {/* Status Bar */}
            <div className="p-2 bg-muted/50 border-t text-xs text-muted-foreground flex items-center justify-between">
                <div>
                    {selectedElement ? (
                        <>
                            Selected: <strong>{selectedElement.type.replace('bpmn:', '')}</strong>
                            {selectedElement.businessObject?.name && (
                                <> — {selectedElement.businessObject.name}</>
                            )}
                        </>
                    ) : (
                        'No element selected'
                    )}
                </div>
            </div>
        </div>
    );
});

export default BpmnDesigner;
