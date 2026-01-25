import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { Label } from './ui/label';
import { Input } from './ui/input';
import { Badge } from './ui/badge';
import { Users, Building2, MapPin, GitBranch, Shield, Clock, Plus, X, CheckCircle } from 'lucide-react';

// Assignment strategy options
const STRATEGIES = [
    { id: 'ROLE', label: 'A specific role', icon: Users, description: 'Branch Manager, Credit Officer, etc.' },
    { id: 'AUTHORITY', label: 'An approval authority', icon: Shield, description: 'Based on approval limits' },
    { id: 'DEPARTMENT', label: 'A department/group', icon: Building2, description: 'Risk, Legal, Audit, etc.' },
    { id: 'COMMITTEE', label: 'A committee', icon: Users, description: 'Multiple approvers required' },
    { id: 'DYNAMIC', label: 'Decide dynamically', icon: GitBranch, description: 'Based on data/rules' },
];

// Location scope options
const SCOPES = [
    { id: 'ORIGINATING_BRANCH', label: 'Originating Branch', description: 'Where the memo was created' },
    { id: 'CUSTOMER_BRANCH', label: "Customer's Home Branch", description: 'Based on customer data' },
    { id: 'DISTRICT', label: 'Same District', description: 'District level offices' },
    { id: 'STATE', label: 'Same State', description: 'State/Region level' },
    { id: 'HEAD_OFFICE', label: 'Head Office', description: 'Central/Corporate office' },
    { id: 'ANY', label: 'Any Location', description: 'No location restriction' },
];

// Common roles (can be fetched from API later)
// Common roles (can be fetched from API later)
const COMMON_ROLES = [];

// Condition operators
const OPERATORS = [
    { id: 'eq', label: '=' },
    { id: 'neq', label: '≠' },
    { id: 'gt', label: '>' },
    { id: 'gte', label: '≥' },
    { id: 'lt', label: '<' },
    { id: 'lte', label: '≤' },
];

// Common fields for conditions
const CONDITION_FIELDS = [
    { id: 'amount', label: 'Loan/Transaction Amount', type: 'number' },
    { id: 'productType', label: 'Product Type', type: 'select' },
    { id: 'customerType', label: 'Customer Type', type: 'select' },
    { id: 'deviationType', label: 'Deviation Type', type: 'select' },
];

import { WorkflowApi } from '../lib/api';

// ... (constants)

export default function AssignmentPanel({
    config = null,
    onChange,
    taskName = 'This Task',
    processVariables = {}
}) {
    // Merge initial static roles with fetched ones
    const [availableRoles, setAvailableRoles] = useState([]);
    const [availableCommittees, setAvailableCommittees] = useState([]);

    useEffect(() => {
        const fetchVariables = async () => {
            try {
                const variables = await WorkflowApi.getVariables();

                // Process Roles
                const dynamicRoles = variables
                    .filter(v => v.type === 'ROLE')
                    .map(v => ({ id: '${' + v.key + '}', label: v.label }));

                if (dynamicRoles.length > 0) {
                    // Prepend dynamic roles to static ones, removing duplicates if needed
                    setAvailableRoles(prev => [...dynamicRoles, ...prev]);
                }

                // Process Committees
                const dynamicCommittees = variables
                    .filter(v => v.type === 'COMMITTEE')
                    .map(v => ({ id: '${' + v.key + '}', label: v.label }));

                if (dynamicCommittees.length > 0) {
                    setAvailableCommittees(prev => [...dynamicCommittees, ...prev]);
                }

            } catch (error) {
                console.error("Failed to fetch workflow variables", error);
            }
        };
        fetchVariables();
    }, []);

    const [step, setStep] = useState(1);
    const [assignment, setAssignment] = useState({
        strategy: config?.strategy || 'ROLE',
        role: config?.role || '',
        scope: config?.scope || 'ORIGINATING_BRANCH',
        limitField: config?.limitField || 'amount',
        selection: config?.selection || 'LOWEST_MATCH',
        department: config?.department || '',
        group: config?.group || '',
        committeeCode: config?.committeeCode || '',
        decisionRule: config?.decisionRule || 'MAJORITY',
        conditions: config?.conditions || [],
        escalationLevels: config?.escalationLevels || [],
    });

    useEffect(() => {
        if (onChange) {
            onChange(assignment);
        }
    }, [assignment]);

    const updateAssignment = (updates) => {
        setAssignment(prev => ({ ...prev, ...updates }));
    };

    const addCondition = () => {
        updateAssignment({
            conditions: [...assignment.conditions, { field: 'amount', operator: 'gte', value: '' }]
        });
    };

    const removeCondition = (index) => {
        updateAssignment({
            conditions: assignment.conditions.filter((_, i) => i !== index)
        });
    };

    const updateCondition = (index, updates) => {
        const newConditions = [...assignment.conditions];
        newConditions[index] = { ...newConditions[index], ...updates };
        updateAssignment({ conditions: newConditions });
    };

    const addEscalation = () => {
        updateAssignment({
            escalationLevels: [...assignment.escalationLevels, { duration: 'P1D', role: '', scope: 'HEAD_OFFICE' }]
        });
    };

    const removeEscalation = (index) => {
        updateAssignment({
            escalationLevels: assignment.escalationLevels.filter((_, i) => i !== index)
        });
    };

    const updateEscalation = (index, updates) => {
        const newLevels = [...assignment.escalationLevels];
        newLevels[index] = { ...newLevels[index], ...updates };
        updateAssignment({ escalationLevels: newLevels });
    };

    // Generate preview text
    const getPreviewText = () => {
        let who = '';
        let where = '';

        if (assignment.strategy === 'ROLE' && assignment.role) {
            who = availableRoles.find(r => r.id === assignment.role)?.label || assignment.role;
        } else if (assignment.strategy === 'AUTHORITY') {
            who = `${assignment.selection === 'LOWEST_MATCH' ? 'Lowest sufficient' : 'Highest'} authority`;
        } else if (assignment.strategy === 'DEPARTMENT') {
            who = assignment.department || 'Department';
            if (assignment.group) who += ` (${assignment.group})`;
        } else if (assignment.strategy === 'COMMITTEE') {
            who = availableCommittees.find(c => c.id === assignment.committeeCode)?.label || assignment.committeeCode || 'Committee';
        } else {
            who = STRATEGIES.find(s => s.id === assignment.strategy)?.label || 'Assignee';
        }

        where = SCOPES.find(s => s.id === assignment.scope)?.label || '';

        let conditions = '';
        if (assignment.conditions.length > 0) {
            conditions = ' when ' + assignment.conditions
                .filter(c => c.value)
                .map(c => {
                    const field = CONDITION_FIELDS.find(f => f.id === c.field)?.label || c.field;
                    const op = OPERATORS.find(o => o.id === c.operator)?.label || c.operator;
                    return `${field} ${op} ${c.value}`;
                })
                .join(' AND ');
        }

        return `${who} at ${where}${conditions}`;
    };

    return (
        <div className="space-y-4 p-4 bg-gray-50 rounded-lg">
            {/* Step Indicator */}
            <div className="flex items-center gap-2 mb-6">
                {[1, 2, 3].map(s => (
                    <button
                        key={s}
                        onClick={() => setStep(s)}
                        className={`flex items-center gap-2 px-3 py-1.5 rounded-full text-sm font-medium transition-colors ${step === s
                            ? 'bg-primary text-primary-foreground'
                            : step > s
                                ? 'bg-green-100 text-green-700'
                                : 'bg-muted text-muted-foreground'
                            }`}
                    >
                        {step > s ? <CheckCircle className="h-4 w-4" /> : s}
                        <span className="hidden sm:inline">
                            {s === 1 ? 'Who' : s === 2 ? 'Where' : 'Conditions'}
                        </span>
                    </button>
                ))}
            </div>

            {/* Step 1: WHO */}
            {step === 1 && (
                <Card>
                    <CardHeader className="pb-3">
                        <CardTitle className="text-base flex items-center gap-2">
                            <Users className="h-4 w-4" />
                            Who should handle this task?
                        </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        {/* Strategy Selection */}
                        <div className="grid grid-cols-1 gap-2">
                            {STRATEGIES.map(strategy => (
                                <button
                                    key={strategy.id}
                                    onClick={() => updateAssignment({ strategy: strategy.id })}
                                    className={`flex items-start gap-3 p-3 rounded-lg border text-left transition-all ${assignment.strategy === strategy.id
                                        ? 'border-primary bg-primary/5 ring-1 ring-primary'
                                        : 'border-border hover:border-primary/50 hover:bg-muted/50'
                                        }`}
                                >
                                    <div className={`p-2 rounded-md ${assignment.strategy === strategy.id ? 'bg-primary text-primary-foreground' : 'bg-muted'}`}>
                                        <strategy.icon className="h-4 w-4" />
                                    </div>
                                    <div>
                                        <div className="font-medium text-sm">{strategy.label}</div>
                                        <div className="text-xs text-muted-foreground">{strategy.description}</div>
                                    </div>
                                </button>
                            ))}
                        </div>

                        {/* Strategy-specific Config */}
                        {assignment.strategy === 'ROLE' && (
                            <div className="pt-4 border-t space-y-2">
                                <Label>Select Role</Label>
                                <select
                                    value={assignment.role}
                                    onChange={(e) => updateAssignment({ role: e.target.value })}
                                    className="w-full h-10 px-3 rounded-md border bg-background text-sm"
                                >
                                    <option value="">-- Select a role --</option>
                                    {availableRoles.map(role => (
                                        <option key={role.id} value={role.id}>{role.label}</option>
                                    ))}
                                </select>
                            </div>
                        )}

                        {assignment.strategy === 'AUTHORITY' && (
                            <div className="pt-4 border-t space-y-3">
                                <div>
                                    <Label>Authority based on field</Label>
                                    <select
                                        value={assignment.limitField}
                                        onChange={(e) => updateAssignment({ limitField: e.target.value })}
                                        className="w-full h-10 px-3 rounded-md border bg-background text-sm mt-1"
                                    >
                                        <option value="amount">Loan/Transaction Amount</option>
                                        <option value="deviationLimit">Deviation Limit</option>
                                    </select>
                                </div>
                                <div>
                                    <Label>Selection Method</Label>
                                    <div className="flex gap-2 mt-1">
                                        <button
                                            onClick={() => updateAssignment({ selection: 'LOWEST_MATCH' })}
                                            className={`flex-1 py-2 px-3 rounded-md text-sm border ${assignment.selection === 'LOWEST_MATCH'
                                                ? 'border-primary bg-primary/5'
                                                : 'border-border'
                                                }`}
                                        >
                                            Lowest Sufficient
                                        </button>
                                        <button
                                            onClick={() => updateAssignment({ selection: 'HIGHEST' })}
                                            className={`flex-1 py-2 px-3 rounded-md text-sm border ${assignment.selection === 'HIGHEST'
                                                ? 'border-primary bg-primary/5'
                                                : 'border-border'
                                                }`}
                                        >
                                            Highest Authority
                                        </button>
                                    </div>
                                </div>
                            </div>
                        )}

                        {assignment.strategy === 'DEPARTMENT' && (
                            <div className="pt-4 border-t space-y-3">
                                <div>
                                    <Label>Department</Label>
                                    <Input
                                        value={assignment.department}
                                        onChange={(e) => updateAssignment({ department: e.target.value })}
                                        placeholder="e.g., Credit Risk, Legal, Audit"
                                        className="mt-1"
                                    />
                                </div>
                                <div>
                                    <Label>Group (optional)</Label>
                                    <Input
                                        value={assignment.group}
                                        onChange={(e) => updateAssignment({ group: e.target.value })}
                                        placeholder="e.g., Retail, Corporate"
                                        className="mt-1"
                                    />
                                </div>
                            </div>
                        )}

                        {assignment.strategy === 'COMMITTEE' && (
                            <div className="pt-4 border-t space-y-3">
                                <div>
                                    <Label>Committee</Label>
                                    <select
                                        value={assignment.committeeCode}
                                        onChange={(e) => updateAssignment({ committeeCode: e.target.value })}
                                        className="w-full h-10 px-3 rounded-md border bg-background text-sm mt-1"
                                    >
                                        <option value="">-- Select committee --</option>
                                        {availableCommittees.map(c => (
                                            <option key={c.id} value={c.id}>{c.label}</option>
                                        ))}
                                    </select>
                                </div>
                                <div>
                                    <Label>Decision Rule</Label>
                                    <div className="flex gap-2 mt-1">
                                        <button
                                            onClick={() => updateAssignment({ decisionRule: 'MAJORITY' })}
                                            className={`flex-1 py-2 px-3 rounded-md text-sm border ${assignment.decisionRule === 'MAJORITY'
                                                ? 'border-primary bg-primary/5'
                                                : 'border-border'
                                                }`}
                                        >
                                            Majority
                                        </button>
                                        <button
                                            onClick={() => updateAssignment({ decisionRule: 'UNANIMOUS' })}
                                            className={`flex-1 py-2 px-3 rounded-md text-sm border ${assignment.decisionRule === 'UNANIMOUS'
                                                ? 'border-primary bg-primary/5'
                                                : 'border-border'
                                                }`}
                                        >
                                            Unanimous
                                        </button>
                                    </div>
                                </div>
                            </div>
                        )}

                        <div className="pt-4 flex justify-end">
                            <Button onClick={() => setStep(2)}>
                                Next: Location →
                            </Button>
                        </div>
                    </CardContent>
                </Card>
            )}

            {/* Step 2: WHERE */}
            {step === 2 && (
                <Card>
                    <CardHeader className="pb-3">
                        <CardTitle className="text-base flex items-center gap-2">
                            <MapPin className="h-4 w-4" />
                            From which location?
                        </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        <div className="grid grid-cols-1 gap-2">
                            {SCOPES.map(scope => (
                                <button
                                    key={scope.id}
                                    onClick={() => updateAssignment({ scope: scope.id })}
                                    className={`flex items-center gap-3 p-3 rounded-lg border text-left transition-all ${assignment.scope === scope.id
                                        ? 'border-primary bg-primary/5 ring-1 ring-primary'
                                        : 'border-border hover:border-primary/50 hover:bg-muted/50'
                                        }`}
                                >
                                    <div className={`w-4 h-4 rounded-full border-2 ${assignment.scope === scope.id
                                        ? 'border-primary bg-primary'
                                        : 'border-muted-foreground'
                                        }`}>
                                        {assignment.scope === scope.id && (
                                            <div className="w-full h-full flex items-center justify-center">
                                                <div className="w-1.5 h-1.5 bg-white rounded-full" />
                                            </div>
                                        )}
                                    </div>
                                    <div>
                                        <div className="font-medium text-sm">{scope.label}</div>
                                        <div className="text-xs text-muted-foreground">{scope.description}</div>
                                    </div>
                                </button>
                            ))}
                        </div>

                        <div className="pt-4 flex justify-between">
                            <Button variant="outline" onClick={() => setStep(1)}>
                                ← Back
                            </Button>
                            <Button onClick={() => setStep(3)}>
                                Next: Conditions →
                            </Button>
                        </div>
                    </CardContent>
                </Card>
            )}

            {/* Step 3: CONDITIONS + ESCALATION */}
            {step === 3 && (
                <Card>
                    <CardHeader className="pb-3">
                        <CardTitle className="text-base flex items-center gap-2">
                            <GitBranch className="h-4 w-4" />
                            Conditions & Escalation <span className="text-muted-foreground font-normal">(optional)</span>
                        </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-6">
                        {/* Conditions Section */}
                        <div>
                            <Label className="text-sm font-medium">Conditions</Label>
                            <p className="text-xs text-muted-foreground mb-3">
                                Apply this assignment only when certain criteria are met.
                            </p>

                            {assignment.conditions.length === 0 && (
                                <div className="p-4 border-2 border-dashed rounded-lg text-center text-muted-foreground">
                                    <p className="text-sm">No conditions set. This assignment applies to all cases.</p>
                                </div>
                            )}

                            {assignment.conditions.map((condition, idx) => (
                                <div key={idx} className="flex items-center gap-2 p-3 bg-background rounded-lg border mb-2">
                                    <select
                                        value={condition.field}
                                        onChange={(e) => updateCondition(idx, { field: e.target.value })}
                                        className="h-9 px-2 rounded-md border bg-background text-sm"
                                    >
                                        {CONDITION_FIELDS.map(f => (
                                            <option key={f.id} value={f.id}>{f.label}</option>
                                        ))}
                                    </select>
                                    <select
                                        value={condition.operator}
                                        onChange={(e) => updateCondition(idx, { operator: e.target.value })}
                                        className="h-9 px-2 rounded-md border bg-background text-sm w-16"
                                    >
                                        {OPERATORS.map(o => (
                                            <option key={o.id} value={o.id}>{o.label}</option>
                                        ))}
                                    </select>
                                    <Input
                                        value={condition.value}
                                        onChange={(e) => updateCondition(idx, { value: e.target.value })}
                                        placeholder="Value"
                                        className="flex-1 h-9"
                                    />
                                    <Button
                                        variant="ghost"
                                        size="icon"
                                        onClick={() => removeCondition(idx)}
                                        className="h-9 w-9 text-muted-foreground hover:text-destructive"
                                    >
                                        <X className="h-4 w-4" />
                                    </Button>
                                </div>
                            ))}

                            <Button variant="outline" size="sm" onClick={addCondition}>
                                <Plus className="h-4 w-4 mr-2" /> Add Condition
                            </Button>
                        </div>

                        {/* Escalation Section */}
                        <div className="pt-4 border-t">
                            <div className="flex items-center gap-2 mb-3">
                                <Clock className="h-4 w-4 text-amber-600" />
                                <Label className="text-sm font-medium">Auto-Escalation</Label>
                            </div>
                            <p className="text-xs text-muted-foreground mb-3">
                                Automatically escalate if not completed within the timeout period.
                            </p>

                            {assignment.escalationLevels.length === 0 && (
                                <div className="p-4 border-2 border-dashed rounded-lg text-center text-muted-foreground mb-3">
                                    <p className="text-sm">No escalation configured. Task will wait indefinitely.</p>
                                </div>
                            )}

                            {assignment.escalationLevels.map((level, idx) => (
                                <div key={idx} className="flex items-center gap-2 p-3 bg-amber-50 rounded-lg border border-amber-200 mb-2">
                                    <span className="text-sm text-amber-700">After</span>
                                    <select
                                        value={level.duration || 'P1D'}
                                        onChange={(e) => updateEscalation(idx, { duration: e.target.value })}
                                        className="h-9 px-2 rounded-md border bg-background text-sm"
                                    >
                                        <option value="PT2H">2 Hours</option>
                                        <option value="PT4H">4 Hours</option>
                                        <option value="PT8H">8 Hours</option>
                                        <option value="P1D">1 Day</option>
                                        <option value="P2D">2 Days</option>
                                        <option value="P3D">3 Days</option>
                                        <option value="P7D">1 Week</option>
                                    </select>
                                    <span className="text-sm text-amber-700">escalate to</span>
                                    <select
                                        value={level.role || ''}
                                        onChange={(e) => updateEscalation(idx, { role: e.target.value })}
                                        className="h-9 px-2 rounded-md border bg-background text-sm flex-1"
                                    >
                                        <option value="">-- Select role --</option>
                                        {availableRoles.map(role => (
                                            <option key={role.id} value={role.id}>{role.label}</option>
                                        ))}
                                    </select>
                                    <Button
                                        variant="ghost"
                                        size="icon"
                                        onClick={() => removeEscalation(idx)}
                                        className="h-9 w-9 text-muted-foreground hover:text-destructive"
                                    >
                                        <X className="h-4 w-4" />
                                    </Button>
                                </div>
                            ))}

                            <Button variant="outline" size="sm" onClick={addEscalation} className="text-amber-700 border-amber-300 hover:bg-amber-50">
                                <Clock className="h-4 w-4 mr-2" /> Add Escalation Level
                            </Button>
                        </div>

                        <div className="pt-4 flex justify-between">
                            <Button variant="outline" onClick={() => setStep(2)}>
                                ← Back
                            </Button>
                        </div>
                    </CardContent>
                </Card>
            )}

            {/* Preview */}
            <Card className="bg-gradient-to-r from-primary/5 to-primary/10 border-primary/20">
                <CardContent className="py-4">
                    <div className="flex items-start gap-3">
                        <div className="p-2 bg-primary/10 rounded-full">
                            <CheckCircle className="h-5 w-5 text-primary" />
                        </div>
                        <div>
                            <p className="text-sm font-medium text-muted-foreground">This task will be assigned to:</p>
                            <p className="text-base font-semibold text-foreground mt-1">
                                {getPreviewText()}
                            </p>
                        </div>
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}
