import React, { useState, useEffect } from 'react';
import { Card, CardHeader, CardTitle, CardContent, CardDescription } from './ui/card';
import { Button } from './ui/button';
import { Badge } from './ui/badge';
import {
    GitBranch, Plus, Trash2, ArrowUp, ArrowDown,
    Play, CheckCircle, AlertCircle, Save, TestTube
} from 'lucide-react';

/**
 * GatewayRulesPanel - Decision Rule Builder for Business Users
 * 
 * Allows admins to configure "if this then that" routing rules
 * without writing any technical expressions.
 */
const GatewayRulesPanel = ({
    topicId,
    gatewayKey,
    gatewayName,
    rules = [],
    defaultFlow,
    outgoingFlows = [],
    formFields = [],
    operators = [],
    onSave,
    onTest,
    saving
}) => {
    const [ruleList, setRuleList] = useState(rules);
    const [defaultOutcome, setDefaultOutcome] = useState(defaultFlow || '');
    const [testData, setTestData] = useState({});
    const [testResult, setTestResult] = useState(null);
    const [showTest, setShowTest] = useState(false);

    useEffect(() => {
        setRuleList(rules);
        setDefaultOutcome(defaultFlow || '');
    }, [rules, defaultFlow]);

    const addRule = () => {
        setRuleList([
            ...ruleList,
            {
                conditions: [{ field: '', operator: '==', value: '' }],
                goTo: ''
            }
        ]);
    };

    const removeRule = (index) => {
        setRuleList(ruleList.filter((_, i) => i !== index));
    };

    const moveRule = (index, direction) => {
        const newList = [...ruleList];
        const targetIndex = direction === 'up' ? index - 1 : index + 1;
        if (targetIndex >= 0 && targetIndex < newList.length) {
            [newList[index], newList[targetIndex]] = [newList[targetIndex], newList[index]];
            setRuleList(newList);
        }
    };

    const updateRule = (index, field, value) => {
        const newList = [...ruleList];
        newList[index] = { ...newList[index], [field]: value };
        setRuleList(newList);
    };

    const addCondition = (ruleIndex) => {
        const newList = [...ruleList];
        newList[ruleIndex].conditions.push({ field: '', operator: '==', value: '' });
        setRuleList(newList);
    };

    const updateCondition = (ruleIndex, condIndex, field, value) => {
        const newList = [...ruleList];
        newList[ruleIndex].conditions[condIndex] = {
            ...newList[ruleIndex].conditions[condIndex],
            [field]: value
        };
        setRuleList(newList);
    };

    const removeCondition = (ruleIndex, condIndex) => {
        const newList = [...ruleList];
        newList[ruleIndex].conditions = newList[ruleIndex].conditions.filter((_, i) => i !== condIndex);
        setRuleList(newList);
    };

    const handleSave = () => {
        onSave({
            gatewayName,
            rules: ruleList,
            defaultFlow: defaultOutcome,
            activate: true
        });
    };

    const handleTest = async () => {
        if (onTest) {
            const result = await onTest(testData);
            setTestResult(result);
        }
    };

    return (
        <Card className="shadow-lg">
            <CardHeader className="bg-gradient-to-r from-purple-600 to-purple-700 text-white rounded-t-lg">
                <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-3">
                        <GitBranch className="w-6 h-6" />
                        <div>
                            <CardTitle className="text-lg">{gatewayName || 'Decision Point'}</CardTitle>
                            <CardDescription className="text-purple-100">
                                Configure routing rules for this decision
                            </CardDescription>
                        </div>
                    </div>
                    <div className="flex space-x-2">
                        <Button
                            variant="outline"
                            size="sm"
                            className="bg-white/10 border-white/30 text-white hover:bg-white/20"
                            onClick={() => setShowTest(!showTest)}
                        >
                            <TestTube className="w-4 h-4 mr-2" />
                            Test
                        </Button>
                        <Button
                            size="sm"
                            className="bg-white text-purple-700 hover:bg-gray-100"
                            onClick={handleSave}
                            disabled={saving}
                        >
                            {saving ? (
                                <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-purple-700 mr-2" />
                            ) : (
                                <Save className="w-4 h-4 mr-2" />
                            )}
                            Save Rules
                        </Button>
                    </div>
                </div>
            </CardHeader>

            <CardContent className="p-6 space-y-6">
                {/* Help Text */}
                <div className="p-4 bg-purple-50 border border-purple-200 rounded-lg">
                    <p className="text-purple-800">
                        <strong>How it works:</strong> Rules are checked from top to bottom.
                        The first rule that matches will determine where the workflow goes next.
                    </p>
                </div>

                {/* Rules List */}
                <div className="space-y-4">
                    {ruleList.length === 0 ? (
                        <div className="p-8 bg-gray-50 rounded-lg text-center border-2 border-dashed border-gray-300">
                            <GitBranch className="w-12 h-12 mx-auto text-gray-400 mb-3" />
                            <p className="text-gray-600 font-medium">No routing rules defined</p>
                            <p className="text-gray-500 text-sm mt-1">
                                Add a rule to control where the workflow goes based on conditions.
                            </p>
                        </div>
                    ) : (
                        ruleList.map((rule, ruleIndex) => (
                            <RuleCard
                                key={ruleIndex}
                                rule={rule}
                                index={ruleIndex}
                                total={ruleList.length}
                                formFields={formFields}
                                operators={operators}
                                outgoingFlows={outgoingFlows}
                                onUpdate={(field, value) => updateRule(ruleIndex, field, value)}
                                onAddCondition={() => addCondition(ruleIndex)}
                                onUpdateCondition={(condIndex, field, value) =>
                                    updateCondition(ruleIndex, condIndex, field, value)
                                }
                                onRemoveCondition={(condIndex) => removeCondition(ruleIndex, condIndex)}
                                onMove={(dir) => moveRule(ruleIndex, dir)}
                                onRemove={() => removeRule(ruleIndex)}
                            />
                        ))
                    )}
                </div>

                {/* Add Rule Button */}
                <Button variant="outline" className="w-full" onClick={addRule}>
                    <Plus className="w-4 h-4 mr-2" />
                    Add New Rule
                </Button>

                {/* Default Outcome */}
                <div className="p-4 bg-gray-100 rounded-lg">
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                        If no rule matches, go to:
                    </label>
                    <select
                        className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-purple-500"
                        value={defaultOutcome}
                        onChange={(e) => setDefaultOutcome(e.target.value)}
                    >
                        <option value="">Select default path...</option>
                        {outgoingFlows.map((flow) => (
                            <option key={flow.id} value={flow.id}>{flow.label}</option>
                        ))}
                    </select>
                </div>

                {/* Test Panel */}
                {showTest && (
                    <TestPanel
                        formFields={formFields}
                        testData={testData}
                        setTestData={setTestData}
                        testResult={testResult}
                        onTest={handleTest}
                    />
                )}
            </CardContent>
        </Card>
    );
};

/**
 * Individual Rule Card
 */
const RuleCard = ({
    rule,
    index,
    total,
    formFields,
    operators,
    outgoingFlows,
    onUpdate,
    onAddCondition,
    onUpdateCondition,
    onRemoveCondition,
    onMove,
    onRemove
}) => {
    return (
        <div className="p-4 bg-white border-2 border-gray-200 rounded-lg hover:border-purple-300 transition-all">
            {/* Rule Header */}
            <div className="flex items-center justify-between mb-4">
                <div className="flex items-center space-x-2">
                    <Badge variant="outline" className="bg-purple-100 text-purple-700 border-purple-300">
                        Rule {index + 1}
                    </Badge>
                </div>
                <div className="flex items-center space-x-1">
                    <Button
                        variant="ghost"
                        size="sm"
                        disabled={index === 0}
                        onClick={() => onMove('up')}
                    >
                        <ArrowUp className="w-4 h-4" />
                    </Button>
                    <Button
                        variant="ghost"
                        size="sm"
                        disabled={index === total - 1}
                        onClick={() => onMove('down')}
                    >
                        <ArrowDown className="w-4 h-4" />
                    </Button>
                    <Button
                        variant="ghost"
                        size="sm"
                        className="text-red-600 hover:text-red-700 hover:bg-red-50"
                        onClick={onRemove}
                    >
                        <Trash2 className="w-4 h-4" />
                    </Button>
                </div>
            </div>

            {/* Conditions */}
            <div className="space-y-3 mb-4">
                <div className="text-sm font-medium text-gray-600">IF</div>
                {rule.conditions.map((condition, condIndex) => (
                    <ConditionRow
                        key={condIndex}
                        condition={condition}
                        index={condIndex}
                        isFirst={condIndex === 0}
                        formFields={formFields}
                        operators={operators}
                        onUpdate={(field, value) => onUpdateCondition(condIndex, field, value)}
                        onRemove={() => onRemoveCondition(condIndex)}
                        canRemove={rule.conditions.length > 1}
                    />
                ))}
                <Button
                    variant="ghost"
                    size="sm"
                    className="text-purple-600"
                    onClick={onAddCondition}
                >
                    <Plus className="w-4 h-4 mr-1" />
                    Add condition (AND)
                </Button>
            </div>

            {/* Outcome */}
            <div className="pt-4 border-t">
                <div className="flex items-center space-x-4">
                    <span className="text-sm font-medium text-gray-600">THEN go to:</span>
                    <select
                        className="flex-1 p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500"
                        value={rule.goTo || ''}
                        onChange={(e) => onUpdate('goTo', e.target.value)}
                    >
                        <option value="">Select next step...</option>
                        {outgoingFlows.map((flow) => (
                            <option key={flow.id} value={flow.id}>{flow.label}</option>
                        ))}
                    </select>
                </div>
            </div>
        </div>
    );
};

/**
 * Condition Row - Single condition in a rule
 */
const ConditionRow = ({
    condition,
    index,
    isFirst,
    formFields,
    operators,
    onUpdate,
    onRemove,
    canRemove
}) => {
    const selectedField = formFields.find(f => f.id === condition.field);
    const fieldType = selectedField?.type || 'STRING';
    const filteredOperators = operators.filter(op =>
        !op.types || op.types.includes(fieldType)
    );

    return (
        <div className="flex items-center space-x-2">
            {!isFirst && (
                <span className="text-sm text-gray-500 w-10">AND</span>
            )}
            <div className={`flex items-center space-x-2 flex-1 ${isFirst ? '' : 'ml-10'}`}>
                {/* Field Selector */}
                <select
                    className="flex-1 p-2 border border-gray-300 rounded-lg text-sm"
                    value={condition.field || ''}
                    onChange={(e) => onUpdate('field', e.target.value)}
                >
                    <option value="">Select field...</option>
                    {formFields.map((field) => (
                        <option key={field.id} value={field.id}>{field.label}</option>
                    ))}
                </select>

                {/* Operator Selector */}
                <select
                    className="w-32 p-2 border border-gray-300 rounded-lg text-sm"
                    value={condition.operator || '=='}
                    onChange={(e) => onUpdate('operator', e.target.value)}
                >
                    {filteredOperators.map((op) => (
                        <option key={op.id} value={op.id}>{op.label}</option>
                    ))}
                </select>

                {/* Value Input */}
                <input
                    type="text"
                    className="flex-1 p-2 border border-gray-300 rounded-lg text-sm"
                    placeholder="Enter value..."
                    value={condition.value || ''}
                    onChange={(e) => onUpdate('value', e.target.value)}
                />

                {/* Remove Button */}
                {canRemove && (
                    <Button
                        variant="ghost"
                        size="sm"
                        className="text-gray-400 hover:text-red-600"
                        onClick={onRemove}
                    >
                        <Trash2 className="w-4 h-4" />
                    </Button>
                )}
            </div>
        </div>
    );
};

/**
 * Test Panel - Test rules with sample data
 */
const TestPanel = ({ formFields, testData, setTestData, testResult, onTest }) => {
    return (
        <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg space-y-4">
            <h4 className="font-medium text-blue-800 flex items-center">
                <TestTube className="w-4 h-4 mr-2" />
                Test Your Rules
            </h4>
            <p className="text-sm text-blue-700">
                Enter sample values to see where the workflow would go.
            </p>

            <div className="grid grid-cols-2 gap-4">
                {formFields.slice(0, 6).map((field) => (
                    <div key={field.id}>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                            {field.label}
                        </label>
                        <input
                            type="text"
                            className="w-full p-2 border border-gray-300 rounded-lg text-sm"
                            placeholder={`Enter ${field.label}...`}
                            value={testData[field.id] || ''}
                            onChange={(e) => setTestData({ ...testData, [field.id]: e.target.value })}
                        />
                    </div>
                ))}
            </div>

            <Button onClick={onTest} className="bg-blue-600 hover:bg-blue-700">
                <Play className="w-4 h-4 mr-2" />
                Run Test
            </Button>

            {testResult && (
                <div className={`p-3 rounded-lg ${testResult.targetFlow
                        ? 'bg-green-100 border border-green-300'
                        : 'bg-yellow-100 border border-yellow-300'
                    }`}>
                    {testResult.targetFlow ? (
                        <p className="text-green-800 flex items-center">
                            <CheckCircle className="w-4 h-4 mr-2" />
                            Result: Workflow will go to <strong className="ml-1">{testResult.targetFlow}</strong>
                        </p>
                    ) : (
                        <p className="text-yellow-800 flex items-center">
                            <AlertCircle className="w-4 h-4 mr-2" />
                            No matching rule - will use default path
                        </p>
                    )}
                </div>
            )}
        </div>
    );
};

export default GatewayRulesPanel;
