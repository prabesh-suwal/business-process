import React, { useState, useEffect } from 'react';
import { MemoApi } from '../lib/api';
import { Card, CardHeader, CardTitle, CardContent } from './ui/card';
import { Button } from './ui/button';
import { Badge } from './ui/badge';
import { Plus, Trash2, ArrowRight, GitBranch } from 'lucide-react';

/**
 * ConditionBuilder - User-friendly condition builder for workflow branching.
 * Supports multiple conditions (if/else if/else) with variable selection.
 */
const ConditionBuilder = ({
    topicId,
    conditions = [],
    defaultTarget = '',
    availableSteps = [],
    onChange,
    label = 'Conditional Branching'
}) => {
    const [variables, setVariables] = useState([]);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (topicId) {
            loadVariables();
        }
    }, [topicId]);

    const loadVariables = async () => {
        try {
            setLoading(true);
            const vars = await MemoApi.getWorkflowVariables(topicId);
            setVariables(vars);
        } catch (error) {
            console.error('Failed to load workflow variables:', error);
            // Use fallback variables
            setVariables(getDefaultVariables());
        } finally {
            setLoading(false);
        }
    };

    const getDefaultVariables = () => [
        { name: 'memo.priority', label: 'Priority', type: 'enum', options: ['LOW', 'MEDIUM', 'HIGH', 'URGENT'] },
        { name: 'initiator.role', label: 'Initiator Role', type: 'enum', options: ['OFFICER', 'MANAGER', 'DIRECTOR'] },
    ];

    const getOperatorsForType = (type) => {
        switch (type) {
            case 'number':
                return [
                    { value: 'EQUALS', label: 'equals' },
                    { value: 'NOT_EQUALS', label: 'does not equal' },
                    { value: 'GREATER_THAN', label: 'is greater than' },
                    { value: 'GREATER_THAN_OR_EQUALS', label: 'is greater than or equal to' },
                    { value: 'LESS_THAN', label: 'is less than' },
                    { value: 'LESS_THAN_OR_EQUALS', label: 'is less than or equal to' },
                ];
            case 'enum':
                return [
                    { value: 'EQUALS', label: 'equals' },
                    { value: 'NOT_EQUALS', label: 'does not equal' },
                    { value: 'IN', label: 'is one of' },
                ];
            case 'text':
                return [
                    { value: 'EQUALS', label: 'equals' },
                    { value: 'NOT_EQUALS', label: 'does not equal' },
                    { value: 'CONTAINS', label: 'contains' },
                    { value: 'STARTS_WITH', label: 'starts with' },
                ];
            case 'boolean':
                return [
                    { value: 'EQUALS', label: 'is' },
                ];
            default:
                return [
                    { value: 'EQUALS', label: 'equals' },
                    { value: 'NOT_EQUALS', label: 'does not equal' },
                ];
        }
    };

    const addCondition = () => {
        const firstStep = availableSteps[0];
        const newCondition = {
            id: `cond_${Date.now()}`,
            field: variables[0]?.name || '',
            operator: 'EQUALS',
            value: '',
            targetStep: firstStep?.id || '',
            targetStepName: firstStep?.name || ''  // Store name for auto-refresh
        };
        onChange([...conditions, newCondition], defaultTarget);
    };

    const updateCondition = (index, updates) => {
        const updated = [...conditions];
        updated[index] = { ...updated[index], ...updates };

        // If field changed, reset operator and value
        if (updates.field) {
            const variable = variables.find(v => v.name === updates.field);
            const operators = getOperatorsForType(variable?.type);
            updated[index].operator = operators[0]?.value || 'EQUALS';
            updated[index].value = '';
        }

        onChange(updated, defaultTarget);
    };

    const removeCondition = (index) => {
        onChange(conditions.filter((_, i) => i !== index), defaultTarget);
    };

    const updateDefaultTarget = (target) => {
        onChange(conditions, target);
    };

    const getVariable = (name) => variables.find(v => v.name === name);

    if (loading) {
        return (
            <div className="flex items-center justify-center p-8">
                <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-600"></div>
            </div>
        );
    }

    return (
        <Card className="border-2 border-purple-100">
            <CardHeader className="bg-gradient-to-r from-purple-50 to-indigo-50 pb-3">
                <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                        <GitBranch className="w-5 h-5 text-purple-600" />
                        <CardTitle className="text-lg text-purple-900">{label}</CardTitle>
                    </div>
                    <Badge variant="outline" className="text-purple-600 border-purple-200">
                        {conditions.length} condition{conditions.length !== 1 ? 's' : ''}
                    </Badge>
                </div>
            </CardHeader>
            <CardContent className="p-4 space-y-4">
                {conditions.length === 0 ? (
                    <div className="text-center py-6 text-gray-500">
                        <GitBranch className="w-10 h-10 mx-auto mb-2 text-gray-300" />
                        <p>No conditions defined.</p>
                        <p className="text-sm">Add conditions to route memos based on their data.</p>
                    </div>
                ) : (
                    <div className="space-y-3">
                        {conditions.map((condition, index) => (
                            <ConditionRow
                                key={condition.id}
                                condition={condition}
                                index={index}
                                variables={variables}
                                availableSteps={availableSteps}
                                operators={getOperatorsForType(getVariable(condition.field)?.type)}
                                variable={getVariable(condition.field)}
                                onUpdate={(updates) => updateCondition(index, updates)}
                                onRemove={() => removeCondition(index)}
                            />
                        ))}
                    </div>
                )}

                {/* Default/Else target */}
                {conditions.length > 0 && (
                    <div className="flex items-center gap-3 p-3 bg-gray-50 rounded-lg border border-gray-200">
                        <span className="font-medium text-gray-700 min-w-[80px]">ELSE</span>
                        <ArrowRight className="w-4 h-4 text-gray-400" />
                        <span className="text-gray-600">go to</span>
                        <select
                            className="flex-1 p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500"
                            value={defaultTarget}
                            onChange={(e) => updateDefaultTarget(e.target.value)}
                        >
                            <option value="">Select default step...</option>
                            {availableSteps.map((step) => (
                                <option key={step.id} value={step.id}>{step.name}</option>
                            ))}
                        </select>
                    </div>
                )}

                {/* Add Condition Button */}
                <Button
                    variant="outline"
                    onClick={addCondition}
                    className="w-full border-dashed border-purple-300 text-purple-600 hover:bg-purple-50"
                >
                    <Plus className="w-4 h-4 mr-2" />
                    Add Condition
                </Button>
            </CardContent>
        </Card>
    );
};

/**
 * Individual condition row component
 */
const ConditionRow = ({
    condition,
    index,
    variables,
    availableSteps,
    operators,
    variable,
    onUpdate,
    onRemove
}) => {
    const prefix = index === 0 ? 'IF' : 'ELSE IF';

    return (
        <div className="flex items-center gap-2 p-3 bg-white rounded-lg border border-gray-200 shadow-sm">
            <span className="font-medium text-purple-700 min-w-[60px]">{prefix}</span>

            {/* Variable selector */}
            <select
                className="p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 min-w-[140px]"
                value={condition.field}
                onChange={(e) => onUpdate({ field: e.target.value })}
            >
                <option value="">Select variable...</option>
                <optgroup label="Memo Fields">
                    {variables.filter(v => v.source === 'memo').map((v) => (
                        <option key={v.name} value={v.name}>{v.label}</option>
                    ))}
                </optgroup>
                <optgroup label="Initiator">
                    {variables.filter(v => v.source === 'initiator').map((v) => (
                        <option key={v.name} value={v.name}>{v.label}</option>
                    ))}
                </optgroup>
                <optgroup label="Form Fields">
                    {variables.filter(v => v.source === 'form').map((v) => (
                        <option key={v.name} value={v.name}>{v.label}</option>
                    ))}
                </optgroup>
            </select>

            {/* Operator selector */}
            <select
                className="p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500"
                value={condition.operator}
                onChange={(e) => onUpdate({ operator: e.target.value })}
            >
                {operators.map((op) => (
                    <option key={op.value} value={op.value}>{op.label}</option>
                ))}
            </select>

            {/* Value input - dropdown for enum, text for others */}
            {variable?.options && variable.options.length > 0 ? (
                <select
                    className="p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 min-w-[120px]"
                    value={condition.value}
                    onChange={(e) => onUpdate({ value: e.target.value })}
                >
                    <option value="">Select value...</option>
                    {variable.options.map((opt) => (
                        <option key={opt} value={opt}>{opt}</option>
                    ))}
                </select>
            ) : (
                <input
                    type={variable?.type === 'number' ? 'number' : 'text'}
                    className="p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 min-w-[120px]"
                    value={condition.value}
                    onChange={(e) => onUpdate({ value: e.target.value })}
                    placeholder="Enter value..."
                />
            )}

            <ArrowRight className="w-4 h-4 text-gray-400" />
            <span className="text-gray-600 text-sm">go to</span>

            {/* Target step selector */}
            <select
                className="p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 min-w-[140px]"
                value={condition.targetStep}
                onChange={(e) => {
                    const selectedStep = availableSteps.find(s => s.id === e.target.value);
                    onUpdate({
                        targetStep: e.target.value,
                        targetStepName: selectedStep?.name || ''  // Store name for auto-refresh
                    });
                }}
            >
                <option value="">Select step...</option>
                {availableSteps.map((step) => (
                    <option key={step.id} value={step.id}>{step.name}</option>
                ))}
            </select>

            {/* Remove button */}
            <Button
                variant="ghost"
                size="icon"
                onClick={onRemove}
                className="text-red-500 hover:text-red-700 hover:bg-red-50"
            >
                <Trash2 className="w-4 h-4" />
            </Button>
        </div>
    );
};

export default ConditionBuilder;
