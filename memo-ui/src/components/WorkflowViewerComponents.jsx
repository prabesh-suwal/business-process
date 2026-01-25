import React from 'react';
import { Eye } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent, CardDescription } from './ui/card';
import { TabsContent, TabsTrigger } from './ui/tabs';
import ViewerConfigPanel from './ViewerConfigPanel';

/**
 * Memo-Wide Viewer Card - Add this to the right panel above the step list
 */
export const MemoWideViewerCard = ({ viewers, onChange }) => {
    return (
        <Card className="m-4 mb-0">
            <CardHeader className="bg-gradient-to-r from-blue-50 to-indigo-50 border-b pb-3">
                <div className="flex items-center gap-2">
                    <Eye className="h-5 w-5 text-blue-600" />
                    <div className="flex-1">
                        <CardTitle className="text-sm">Memo-Wide Viewers</CardTitle>
                        <CardDescription className="text-xs mt-0.5">
                            Who can view ALL memos (read-only)
                        </CardDescription>
                    </div>
                </div>
            </CardHeader>
            <CardContent className="pt-3 pb-3">
                <ViewerConfigPanel
                    viewers={viewers}
                    onChange={onChange}
                    title=""
                />
                <div className="mt-2 p-2 bg-blue-50/50 border border-blue-200 rounded text-xs text-blue-800">
                    💡 These viewers can see all memos created under this workflow
                </div>
            </CardContent>
        </Card>
    );
};

/**
 * Step Viewer Tab Trigger - Add this to the TabsList in step configuration
 */
export const StepViewerTabTrigger = () => {
    return (
        <TabsTrigger value="viewers" className="gap-2">
            <Eye className="h-4 w-4" />
            Viewers
        </TabsTrigger>
    );
};

/**
 * Step Viewer Tab Content - Add this to the TabsContent sections in step configuration
 */
export const StepViewerTabContent = ({ viewers, onChange }) => {
    return (
        <TabsContent value="viewers" className="space-y-4 mt-4">
            <ViewerConfigPanel
                viewers={viewers}
                onChange={onChange}
                title="Who can view tasks for this step?"
            />
            <div className="p-3 bg-amber-50 border border-amber-200 rounded-lg">
                <p className="text-xs text-amber-800">
                    <strong>⚠️ Note:</strong> These viewers can only see tasks for THIS specific step.
                </p>
            </div>
        </TabsContent>
    );
};
