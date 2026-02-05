import React, { useState, useCallback } from 'react';
import { Button } from '../components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import AssignmentRuleCard from '../components/AssignmentRuleCard';
import { Plus, Eye, AlertCircle, Info, Users, Shield, X } from 'lucide-react';

/**
 * AdvancedAssignmentTab - Multi-rule assignment configuration
 * 
 * Rules Logic:
 * - Multiple rules are combined with OR logic (match ANY rule)
 * - Within each rule, criteria are combined with AND logic (match ALL criteria)
 */
const AdvancedAssignmentTab = ({
    config = {},
    onChange,
    // Organization data from CAS
    roles = [],
    groups = [],
    departments = [],
    branches = [],
    // Geo data
    regions = [],
    districts = [],
    states = [],
    // Users for direct assignment
    users = [],
    // API functions
    onPreview,
    topicId
}) => {
    const [expandedRules, setExpandedRules] = useState({});
    const [previewResult, setPreviewResult] = useState(null);
    const [previewLoading, setPreviewLoading] = useState(false);
    const [previewError, setPreviewError] = useState(null);

    // Initialize rules from config or create empty array
    const rules = config.rules || [];
    const fallbackRoleId = config.fallbackRoleId;
    const completionMode = config.completionMode || 'ANY';

    // Add a new empty rule
    const addRule = useCallback(() => {
        const newRule = {
            id: crypto.randomUUID(),
            name: `Rule ${rules.length + 1}`,
            criteria: {}
        };
        onChange({
            ...config,
            rules: [...rules, newRule]
        });
        // Expand the new rule
        setExpandedRules(prev => ({ ...prev, [newRule.id]: true }));
    }, [config, rules, onChange]);

    // Update a specific rule
    const updateRule = useCallback((ruleId, updatedRule) => {
        onChange({
            ...config,
            rules: rules.map(r => r.id === ruleId ? { ...r, ...updatedRule } : r)
        });
    }, [config, rules, onChange]);

    // Remove a rule
    const removeRule = useCallback((ruleId) => {
        onChange({
            ...config,
            rules: rules.filter(r => r.id !== ruleId)
        });
    }, [config, rules, onChange]);

    // Toggle rule expansion
    const toggleExpand = useCallback((ruleId) => {
        setExpandedRules(prev => ({ ...prev, [ruleId]: !prev[ruleId] }));
    }, []);

    // Handle fallback role change
    const handleFallbackChange = useCallback((roleId) => {
        onChange({ ...config, fallbackRoleId: roleId });
    }, [config, onChange]);

    // Handle completion mode change
    const handleCompletionModeChange = useCallback((mode) => {
        onChange({ ...config, completionMode: mode });
    }, [config, onChange]);

    // Preview assignment
    const handlePreview = useCallback(async () => {
        if (!onPreview) return;

        setPreviewLoading(true);
        setPreviewError(null);

        try {
            const result = await onPreview(config);
            setPreviewResult(result);
        } catch (error) {
            console.error('Preview failed:', error);
            setPreviewError(error.message || 'Preview failed');
        } finally {
            setPreviewLoading(false);
        }
    }, [config, onPreview]);

    return (
        <div className="space-y-4">
            {/* Header - Clean and minimal */}
            <div className="flex items-center gap-2 text-slate-600 pb-2 border-b border-slate-100">
                <Users className="w-4 h-4" />
                <span className="text-xs font-semibold uppercase tracking-wider">Assignment Rules</span>
            </div>

            {/* Info banner - Simplified */}
            {rules.length > 1 && (
                <div className="flex items-center gap-2 text-xs text-amber-700 bg-amber-50 border border-amber-100 rounded-md px-3 py-2">
                    <Info className="w-3.5 h-3.5 flex-shrink-0" />
                    <span>Rules use <strong>OR</strong> logic — matching any rule qualifies a user.</span>
                </div>
            )}

            {/* Rules Section */}
            <div className="space-y-3">
                {/* Add Rule Button */}
                <div className="flex justify-end">
                    <Button
                        onClick={addRule}
                        size="sm"
                        className="bg-blue-600 hover:bg-blue-700 text-xs h-8"
                    >
                        <Plus className="w-3.5 h-3.5 mr-1.5" />
                        Add Rule
                    </Button>
                </div>

                {/* Empty State */}
                {rules.length === 0 ? (
                    <div className="text-center py-8 bg-slate-50 border border-dashed border-slate-200 rounded-lg">
                        <Users className="w-8 h-8 mx-auto text-slate-300 mb-2" />
                        <p className="text-sm text-slate-500 mb-3">No assignment rules configured</p>
                        <Button onClick={addRule} variant="outline" size="sm" className="text-xs">
                            <Plus className="w-3.5 h-3.5 mr-1.5" />
                            Add First Rule
                        </Button>
                    </div>
                ) : (
                    <div className="space-y-2">
                        {rules.map((rule, index) => (
                            <div key={rule.id} className="relative">
                                {/* OR indicator between rules */}
                                {index > 0 && (
                                    <div className="flex items-center justify-center -my-1 z-10 relative">
                                        <span className="px-2 py-0.5 bg-amber-100 text-amber-700 text-[10px] font-semibold rounded-full">
                                            OR
                                        </span>
                                    </div>
                                )}
                                <AssignmentRuleCard
                                    rule={rule}
                                    ruleIndex={index}
                                    onUpdate={(updated) => updateRule(rule.id, updated)}
                                    onRemove={() => removeRule(rule.id)}
                                    geoOptions={{ regions, districts, states }}
                                    orgOptions={{ branches, departments, groups, roles, users }}
                                    isExpanded={expandedRules[rule.id] !== false}
                                    onToggleExpand={() => toggleExpand(rule.id)}
                                />
                            </div>
                        ))}
                    </div>
                )}
            </div>

            {/* Fallback Configuration - Compact */}
            <div className="border border-slate-200 rounded-lg p-3">
                <div className="flex items-center gap-2 text-slate-600 mb-2">
                    <Shield className="w-4 h-4" />
                    <span className="text-xs font-semibold uppercase tracking-wider">Fallback</span>
                </div>
                <p className="text-xs text-slate-500 mb-2">
                    If no users match the rules, assign to:
                </p>
                <select
                    className="w-full text-sm p-2 border border-slate-200 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    value={fallbackRoleId || ''}
                    onChange={(e) => handleFallbackChange(e.target.value || null)}
                >
                    <option value="">No fallback (leave unassigned)</option>
                    {roles.map((role) => (
                        <option key={role.id} value={role.id}>
                            {role.name || role.code || role.label}
                        </option>
                    ))}
                </select>
            </div>

            {/* Completion Mode - Compact radio buttons */}
            <div className="border border-slate-200 rounded-lg p-3">
                <div className="flex items-center gap-2 text-slate-600 mb-2">
                    <Users className="w-4 h-4" />
                    <span className="text-xs font-semibold uppercase tracking-wider">Completion Mode</span>
                </div>
                <div className="grid grid-cols-2 gap-2">
                    <label className={`flex items-center p-2 border rounded-md cursor-pointer text-xs transition-all ${completionMode === 'ANY'
                            ? 'border-blue-500 bg-blue-50 text-blue-700'
                            : 'border-slate-200 hover:border-slate-300'
                        }`}>
                        <input
                            type="radio"
                            name="completionMode"
                            value="ANY"
                            checked={completionMode === 'ANY'}
                            onChange={() => handleCompletionModeChange('ANY')}
                            className="w-3.5 h-3.5 text-blue-600 mr-2"
                        />
                        <div>
                            <span className="font-medium">Pool</span>
                            <p className="text-[10px] text-slate-500 mt-0.5">First to claim</p>
                        </div>
                    </label>
                    <label className={`flex items-center p-2 border rounded-md cursor-pointer text-xs transition-all ${completionMode === 'ALL'
                            ? 'border-blue-500 bg-blue-50 text-blue-700'
                            : 'border-slate-200 hover:border-slate-300'
                        }`}>
                        <input
                            type="radio"
                            name="completionMode"
                            value="ALL"
                            checked={completionMode === 'ALL'}
                            onChange={() => handleCompletionModeChange('ALL')}
                            className="w-3.5 h-3.5 text-blue-600 mr-2"
                        />
                        <div>
                            <span className="font-medium">All Must Approve</span>
                            <p className="text-[10px] text-slate-500 mt-0.5">Parallel approval</p>
                        </div>
                    </label>
                </div>
            </div>

            {/* Preview Section - Compact */}
            <div className="border-t border-slate-100 pt-3">
                <div className="flex items-center justify-between mb-2">
                    <span className="text-xs font-semibold text-slate-600 uppercase tracking-wider">Preview</span>
                    <Button
                        variant="outline"
                        size="sm"
                        onClick={handlePreview}
                        disabled={previewLoading || rules.length === 0}
                        className="text-xs h-7"
                    >
                        <Eye className="w-3.5 h-3.5 mr-1.5" />
                        {previewLoading ? 'Loading...' : 'Preview'}
                    </Button>
                </div>

                {previewError && (
                    <div className="p-2 bg-red-50 border border-red-100 rounded-md flex items-center gap-2 text-red-700 text-xs">
                        <AlertCircle className="w-3.5 h-3.5" />
                        <span>{previewError}</span>
                    </div>
                )}

                {previewResult && (
                    <div className="p-2 bg-green-50 border border-green-100 rounded-md text-xs">
                        <p className="font-medium text-green-800 mb-1">
                            Matched Users ({previewResult.matchedUsers?.length || 0})
                        </p>
                        {previewResult.matchedUsers?.length > 0 ? (
                            <div className="flex flex-wrap gap-1">
                                {previewResult.matchedUsers.map((user) => (
                                    <span key={user.userId} className="px-2 py-0.5 bg-green-100 text-green-800 rounded">
                                        {user.fullName || user.username}
                                    </span>
                                ))}
                            </div>
                        ) : (
                            <p className="text-green-700">No users match the current rules.</p>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};

export default AdvancedAssignmentTab;
