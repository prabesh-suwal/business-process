import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from './ui/card';
import { Button } from './ui/button';
import { Settings, Save, AlertCircle, CheckCircle } from 'lucide-react';
import AdvancedAssignmentTab from './AdvancedAssignmentTab';
import { MemoApi } from '../lib/api';

/**
 * TopicDefaultAssignee - Configure default assignment rules at the topic level.
 * These defaults apply to any workflow step that doesn't have specific assignment config.
 */
const TopicDefaultAssignee = ({
    topicId,
    initialConfig = {},
    // Organization data
    roles = [],
    groups = [],
    departments = [],
    branches = [],
    regions = [],
    districts = [],
    states = [],
    users = [],
    onPreview
}) => {
    const [config, setConfig] = useState(initialConfig);
    const [saving, setSaving] = useState(false);
    const [saveStatus, setSaveStatus] = useState(null); // 'success', 'error', null
    const [isExpanded, setIsExpanded] = useState(false);

    useEffect(() => {
        setConfig(initialConfig);
    }, [initialConfig]);

    const handleSave = async () => {
        setSaving(true);
        setSaveStatus(null);
        try {
            // Save to topic's defaultAssigneeConfig
            await MemoApi.updateTopicDefaultAssignee(topicId, config);
            setSaveStatus('success');
            setTimeout(() => setSaveStatus(null), 3000);
        } catch (error) {
            console.error('Failed to save default assignee config:', error);
            setSaveStatus('error');
        } finally {
            setSaving(false);
        }
    };

    return (
        <Card className="border-2 border-slate-200 shadow-sm">
            <CardHeader
                className="cursor-pointer bg-gradient-to-r from-slate-50 to-white border-b hover:from-slate-100 hover:to-slate-50 transition-colors"
                onClick={() => setIsExpanded(!isExpanded)}
            >
                <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-lg bg-purple-100 text-purple-600 flex items-center justify-center">
                            <Settings className="w-5 h-5" />
                        </div>
                        <div>
                            <CardTitle className="text-lg">Topic Default Assignment</CardTitle>
                            <CardDescription>
                                Fallback assignment rules when a step has no specific configuration
                            </CardDescription>
                        </div>
                    </div>
                    <div className="flex items-center gap-2">
                        {saveStatus === 'success' && (
                            <span className="flex items-center gap-1 text-green-600 text-sm">
                                <CheckCircle className="w-4 h-4" /> Saved
                            </span>
                        )}
                        {saveStatus === 'error' && (
                            <span className="flex items-center gap-1 text-red-600 text-sm">
                                <AlertCircle className="w-4 h-4" /> Failed
                            </span>
                        )}
                        <Button
                            variant={isExpanded ? "secondary" : "outline"}
                            size="sm"
                            onClick={(e) => { e.stopPropagation(); setIsExpanded(!isExpanded); }}
                        >
                            {isExpanded ? 'Collapse' : 'Configure'}
                        </Button>
                    </div>
                </div>
            </CardHeader>

            {isExpanded && (
                <CardContent className="pt-6">
                    {/* Info box */}
                    <div className="bg-purple-50 border border-purple-200 rounded-lg p-4 mb-6">
                        <p className="text-sm text-purple-700">
                            <strong>Default Rules:</strong> These assignment rules apply to any workflow step
                            that doesn't have its own assignment configuration. If a step has specific rules,
                            those override this default.
                        </p>
                    </div>

                    {/* Use the same AdvancedAssignmentTab component */}
                    <AdvancedAssignmentTab
                        config={config}
                        onChange={setConfig}
                        roles={roles}
                        groups={groups}
                        departments={departments}
                        branches={branches}
                        regions={regions}
                        districts={districts}
                        states={states}
                        users={users}
                        topicId={topicId}
                        onPreview={onPreview}
                    />

                    {/* Save Button */}
                    <div className="flex justify-end pt-6 mt-6 border-t">
                        <Button
                            onClick={handleSave}
                            disabled={saving}
                            className="bg-purple-600 hover:bg-purple-700"
                        >
                            {saving ? (
                                <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2" />
                            ) : (
                                <Save className="w-4 h-4 mr-2" />
                            )}
                            Save Default Assignment
                        </Button>
                    </div>
                </CardContent>
            )}
        </Card>
    );
};

export default TopicDefaultAssignee;
